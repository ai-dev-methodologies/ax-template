package com.ax.template.authblueprint.copresence;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * negative-copresence-gate-l0 sole orchestrator. {@link #addMember} is THE GATE: under the subject's
 * PESSIMISTIC_WRITE lock it fails closed on an unassessable concept (GATE-FAILCLOSED-001), intersects
 * the candidate's normalized concept against the subject's ACTIVE member set via the conflict knowledge
 * base (GATE-SET-EVAL-001), grades findings ABSOLUTE/RELATIVE (GATE-GRADED-001), rejects ABSOLUTE
 * unconditionally and RELATIVE unless an atomic non-blank override reason is supplied (GATE-OVERRIDE-001),
 * and is the SOLE activation path. The active-set read is in the locked transaction (GATE-CONCURRENT-001).
 */
@Service
public class CopresenceService {

    private final SubjectRepository subjects;
    private final SubjectMemberRepository members;
    private final ConflictRuleRepository conflicts;
    private final KnownConceptRepository knownConcepts;
    private final CopresenceMetrics metrics;
    private final Clock clock;

    public CopresenceService(SubjectRepository subjects, SubjectMemberRepository members,
                             ConflictRuleRepository conflicts, KnownConceptRepository knownConcepts,
                             CopresenceMetrics metrics, Clock clock) {
        this.subjects = subjects;
        this.members = members;
        this.conflicts = conflicts;
        this.knownConcepts = knownConcepts;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public Subject createSubject(String subjectKey) {
        if (subjectKey == null || subjectKey.isBlank()) throw CopresenceException.invalidInput();
        try {
            Subject s = subjects.saveAndFlush(new Subject(UUID.randomUUID(), subjectKey.strip(), Instant.now(clock)));
            metrics.record("subject", "ok");
            return s;
        } catch (DataIntegrityViolationException e) {
            metrics.record("subject", "rejected");
            throw CopresenceException.duplicate("Subject");
        }
    }

    @Transactional
    public KnownConcept registerConcept(String concept) {
        if (concept == null || concept.isBlank()) throw CopresenceException.invalidInput();
        try {
            KnownConcept k = knownConcepts.saveAndFlush(new KnownConcept(UUID.randomUUID(), concept.strip()));
            metrics.record("concept", "ok");
            return k;
        } catch (DataIntegrityViolationException e) {
            metrics.record("concept", "rejected");
            throw CopresenceException.duplicate("Concept");
        }
    }

    @Transactional
    public ConflictRule addConflict(String a, String b, ConflictSeverity severity, String reason) {
        if (a == null || b == null || a.isBlank() || b.isBlank() || a.strip().equals(b.strip())
                || severity == null || reason == null || reason.isBlank()) {
            metrics.record("conflict", "rejected");
            throw CopresenceException.invalidInput();
        }
        String x = a.strip();
        String y = b.strip();
        // Referential integrity: a conflict rule MUST reference registered concepts — a rule on a typo'd/
        // unregistered concept would silently never fire, disabling a real contraindication (fail-OPEN).
        if (!knownConcepts.existsByConcept(x) || !knownConcepts.existsByConcept(y)) {
            metrics.record("conflict", "rejected");
            throw CopresenceException.unknownConcept();
        }
        // Canonical UNORDERED storage (lo,hi): the ordered unique index then enforces true unordered-pair
        // uniqueness, so a reverse-order duplicate is a 409 and findConflict can never match two
        // contradictory directional rows (no NonUniqueResultException).
        String lo = x.compareTo(y) <= 0 ? x : y;
        String hi = x.compareTo(y) <= 0 ? y : x;
        try {
            ConflictRule r = conflicts.saveAndFlush(
                new ConflictRule(UUID.randomUUID(), lo, hi, severity, reason.strip()));
            metrics.record("conflict", "ok");
            return r;
        } catch (DataIntegrityViolationException e) {
            metrics.record("conflict", "rejected");
            throw CopresenceException.duplicate("Conflict rule");
        }
    }

    /** GATE-SET-EVAL/GRADED/FAILCLOSED/OVERRIDE/CONCURRENT-001 — the contraindication gate. */
    @Transactional
    public SubjectMember addMember(String subjectKey, String conceptRaw, String labelRaw, String overrideReason) {
        if (conceptRaw == null || conceptRaw.isBlank() || labelRaw == null || labelRaw.isBlank()) {
            metrics.record("member", "rejected");
            throw CopresenceException.invalidInput();
        }
        String concept = conceptRaw.strip();
        String label = labelRaw.strip();
        Subject s = subjects.findBySubjectKeyForUpdate(subjectKey)        // lock — in-tx set re-read
            .orElseThrow(CopresenceException::subjectNotFound);
        if (!knownConcepts.existsByConcept(concept)) {                    // FAIL CLOSED (fail-safe default)
            metrics.record("member", "unassessable");
            throw CopresenceException.unassessable();
        }
        List<SubjectMember> active = members.findBySubjectIdAndStatus(s.getId(), MemberStatus.ACTIVE);
        List<String> absolute = new ArrayList<>();
        List<String> relative = new ArrayList<>();
        for (SubjectMember m : active) {                                  // set-intersection on normalized concept
            if (m.getConcept().equals(concept)) {                         // duplicate-therapy: same concept active
                relative.add("DUPLICATE:" + concept);                     // overridable (a deliberate re-add)
                continue;
            }
            Optional<ConflictRule> r = conflicts.findConflict(concept, m.getConcept());
            if (r.isPresent()) {
                if (r.get().getSeverity() == ConflictSeverity.ABSOLUTE) absolute.add(m.getConcept());
                else relative.add(m.getConcept());
            }
        }
        if (!absolute.isEmpty()) {                                        // hard-stop — no override path
            metrics.record("member", "absolute");
            throw CopresenceException.absolute(String.join(",", absolute));
        }
        String overridden = null;
        String reason = null;
        if (!relative.isEmpty()) {                                        // soft-stop — override-with-reason only
            if (overrideReason == null || overrideReason.isBlank()) {
                metrics.record("member", "relative");
                throw CopresenceException.relative(String.join(",", relative));
            }
            reason = overrideReason.strip();
            overridden = "RELATIVE:" + String.join(",", relative);        // recorded by reference, bound to row
        }
        SubjectMember added = members.save(new SubjectMember(UUID.randomUUID(), s.getId(), concept, label,
            MemberStatus.ACTIVE, reason, overridden, Instant.now(clock)));
        metrics.record("member", overridden == null ? "ok" : "overridden");
        return added;
    }

    @Transactional
    public SubjectMember removeMember(String subjectKey, UUID memberId) {
        Subject s = subjects.findBySubjectKeyForUpdate(subjectKey).orElseThrow(CopresenceException::subjectNotFound);
        SubjectMember m = members.findById(memberId).orElseThrow(CopresenceException::subjectNotFound);
        if (!m.getSubjectId().equals(s.getId())) throw CopresenceException.subjectNotFound();  // IDOR-safe 404
        m.markRemoved();
        metrics.record("member", "ok");
        return m;
    }

    @Transactional(readOnly = true)
    public Page<SubjectMember> listMembers(String subjectKey, int page, int size) {
        Subject s = subjects.findBySubjectKey(subjectKey).orElseThrow(CopresenceException::subjectNotFound);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        return members.findBySubjectIdOrderByCreatedAtAsc(s.getId(), PageRequest.of(safePage, safeSize));
    }
}
