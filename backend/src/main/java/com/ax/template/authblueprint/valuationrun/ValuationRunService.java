package com.ax.template.authblueprint.valuationrun;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * valuation-run-projection-l0 sole orchestrator. Every write path takes the subject row's
 * PESSIMISTIC_WRITE lock first (VALRUN-CONCURRENT-001), so the read-current-version /
 * write-next-version sequence cannot interleave; the uq(subject_id, run_version) backstop makes
 * the loser of any residual race a deterministic 409. A run is IMMUTABLE once computed — a
 * correction is a NEW run (recompute appends version+1) or a rebase (a new baseline with a
 * forward pointer); nothing in this domain rewrites or deletes. Fan-out conservation is checked
 * TWICE: the run's persisted output_sum carries a DB @Check (= total_value), AND the service
 * derives Σ a SECOND way via {@code runs.sumOutputValues} before commit. ValuationOutput rows
 * are members: {@link MemberWriter} writes, root-JPQL reads.
 */
@Service
public class ValuationRunService {

    /** VALRUN-FALLBACK-001 — the implicit source every plain recompute/rebase is tagged with. */
    public static final String PRIMARY_SOURCE = "PRIMARY";

    private final ValuationSubjectRepository subjects;
    private final ValuationRunRepository runs;
    private final MemberWriter members;
    private final ValuationRunMetrics metrics;
    private final Clock clock;

    public ValuationRunService(ValuationSubjectRepository subjects, ValuationRunRepository runs,
                               MemberWriter members, ValuationRunMetrics metrics, Clock clock) {
        this.subjects = subjects;
        this.runs = runs;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public ValuationSubject createSubject(String subjectRef) {
        ValuationSubject subject = subjects.save(new ValuationSubject(UUID.randomUUID(), subjectRef,
            Instant.now(clock)));
        metrics.record("create", "ok");
        return subject;
    }

    /** VALRUN-ASOF/FANOUT/IMMUTABLE/CONCURRENT-001 — append a new immutable version pinned to an
     *  as-of instant. {@code expectedHeadVersion} is the head the caller OBSERVED; under the
     *  subject's PESSIMISTIC_WRITE lock the read-head / write-next sequence is serialized, so
     *  across N concurrent recomputes naming the SAME observed head EXACTLY ONE advances (the
     *  rest find the head already moved → 409 VALRUN_VERSION_CONFLICT); the uq(subject_id,
     *  run_version) backstop makes the loser deterministic even if a residual race slipped the
     *  lock (CWE-362). {@code declaredTotal} MUST equal Σ positions (checked an independent second
     *  way after persistence) or the fan-out is rejected (422) and nothing lands. */
    @Transactional
    public ValuationRun recompute(UUID subjectId, int expectedHeadVersion, BigDecimal declaredTotal,
                                  String basis, Map<String, BigDecimal> positions) {
        return recomputeForSource(subjectId, PRIMARY_SOURCE, expectedHeadVersion, declaredTotal, basis, positions);
    }

    /** VALRUN-FALLBACK-001 — recompute tagged with a NAMED source, so a fallback as-of read can
     *  later try sources in a configured priority order. Otherwise identical to {@link #recompute}. */
    @Transactional
    public ValuationRun recomputeForSource(UUID subjectId, String sourceRef, int expectedHeadVersion,
                                           BigDecimal declaredTotal, String basis,
                                           Map<String, BigDecimal> positions) {
        ValuationSubject subject = subjects.findByIdForUpdate(subjectId)
            .orElseThrow(ValuationRunException::notFound);
        if (subject.getHeadRunVersion() != expectedHeadVersion) {
            metrics.record("recompute", "conflict");
            throw ValuationRunException.versionConflict();    // the head already moved — loser of the race
        }
        ValuationRun run = appendVersion(subject, declaredTotal, basis, positions, null, sourceRef);
        metrics.record("recompute", "ok");
        return run;
    }

    /** VALRUN-REBASE-001 — reset the basis as a NEW baseline run while retaining prior runs verbatim. */
    @Transactional
    public ValuationRun rebase(UUID subjectId, int fromRunVersion, BigDecimal declaredTotal,
                               String newBasis, Map<String, BigDecimal> positions) {
        ValuationSubject subject = subjects.findByIdForUpdate(subjectId)
            .orElseThrow(ValuationRunException::notFound);
        if (subject.getHeadRunVersion() != fromRunVersion) {
            metrics.record("rebase", "not_current");
            throw ValuationRunException.notCurrent();         // rebase only from the current head — chain stays linear
        }
        ValuationRun baseline = appendVersion(subject, declaredTotal, newBasis, positions, fromRunVersion,
            PRIMARY_SOURCE);
        metrics.record("rebase", "ok");
        return baseline;
    }

    /**
     * The single locked write core (the caller already holds the subject's PESSIMISTIC_WRITE lock).
     * Reads the current head version, writes version+1 (the uq backstop catches a residual race),
     * fans out to N output rows in the same transaction, then cross-checks conservation an
     * INDEPENDENT second way (a repo SUM) against the caller's declared total before advancing the
     * head. {@code rebasedFrom} is null on a plain recompute and the rebased-from version on a
     * rebase baseline.
     */
    private ValuationRun appendVersion(ValuationSubject subject, BigDecimal declaredTotal, String basis,
                                       Map<String, BigDecimal> positions, Integer rebasedFrom, String sourceRef) {
        String op = rebasedFrom == null ? "recompute" : "rebase";
        if (positions == null || positions.isEmpty()) {
            metrics.record(op, "empty");
            throw ValuationRunException.emptyFanOut();
        }
        int nextVersion = subject.getHeadRunVersion() + 1;
        Instant now = Instant.now(clock);
        ValuationRun run;
        try {
            // uq(subject_id, run_version) — two writers at the same head collide here. The DB
            // @Check (output_sum = total_value) ties the persisted columns; the meaningful
            // conservation proof is the independent repo SUM below against the caller's total.
            run = runs.saveAndFlush(new ValuationRun(UUID.randomUUID(), subject.getId(), nextVersion,
                now, basis, declaredTotal, declaredTotal, rebasedFrom, sourceRef, now));
        } catch (DataIntegrityViolationException dup) {
            metrics.record(op, "conflict");
            throw ValuationRunException.versionConflict();    // loser of the concurrent advance → 409
        }
        for (Map.Entry<String, BigDecimal> e : positions.entrySet()) {
            members.persist(new ValuationOutput(UUID.randomUUID(), run.getId(), e.getKey(), e.getValue()));
        }
        // INDEPENDENT conservation derivation — a repo SUM over the persisted rows, NOT the
        // caller's declared total. A by-construction total := Σ would be tautological.
        BigDecimal crossCheck = runs.sumOutputValues(run.getId());
        if (crossCheck.compareTo(run.getTotalValue()) != 0) {
            metrics.record(op, "not_conserved");
            throw ValuationRunException.fanOutNotConserved(); // 422; the whole tx rolls back
        }
        subject.advanceHead(run.getRunVersion());
        return run;
    }

    /** VALRUN-ASOF-001 — the run that was current AS OF T: the greatest as-of ≤ T (404 if none). */
    @Transactional(readOnly = true)
    public ValuationRun asOf(UUID subjectId, Instant asOf) {
        getSubject(subjectId);                                // 404 before a no-run-as-of
        ValuationRun run = runs.findAsOf(subjectId, asOf, Limit.of(1))
            .orElseThrow(ValuationRunException::noRunAsOf);
        metrics.record("as_of_read", "ok");
        return run;
    }

    /** VALRUN-FALLBACK-001 — try {@code sourcePriority} in order; the FIRST source with a
     *  qualifying as-of ≤ T run wins (priority order, not most-recent-across-sources). The
     *  returned run's {@code sourceRef} tells the caller which source actually served the read
     *  (provenance). No source in the list qualifying is fail-closed (404), never a silent
     *  default. */
    @Transactional(readOnly = true)
    public ValuationRun asOfWithFallback(UUID subjectId, Instant asOf, List<String> sourcePriority) {
        getSubject(subjectId);                                // 404 before a no-run-as-of
        for (String source : sourcePriority) {
            Optional<ValuationRun> hit = runs.findAsOfBySource(subjectId, source, asOf, Limit.of(1));
            if (hit.isPresent()) {
                metrics.record("as_of_fallback_read", "ok");
                return hit.get();
            }
        }
        metrics.record("as_of_fallback_read", "no_qualifying_source");
        throw ValuationRunException.noQualifyingSource();
    }

    /** VALRUN-REBASE-001 — resolve the subject's CURRENT run (the latest head version). */
    @Transactional(readOnly = true)
    public ValuationRun current(UUID subjectId) {
        getSubject(subjectId);
        return runs.findTopBySubjectIdOrderByRunVersionDesc(subjectId)
            .orElseThrow(ValuationRunException::noRunAsOf);
    }

    @Transactional(readOnly = true)
    public ValuationSubject getSubject(UUID subjectId) {
        return subjects.findById(subjectId).orElseThrow(ValuationRunException::notFound);
    }

    @Transactional(readOnly = true)
    public ValuationRun getRun(UUID subjectId, int runVersion) {
        return runs.findBySubjectIdAndRunVersion(subjectId, runVersion)
            .orElseThrow(ValuationRunException::notFound);
    }

    @Transactional(readOnly = true)
    public List<ValuationRun> runHistory(UUID subjectId) {
        getSubject(subjectId);                                // 404 before an empty list
        return runs.findBySubjectIdOrderByRunVersionAsc(subjectId);
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> outputs(UUID subjectId, int runVersion) {
        ValuationRun run = getRun(subjectId, runVersion);
        Map<String, BigDecimal> byPosition = new LinkedHashMap<>();
        for (ValuationOutput o : runs.findOutputs(run.getId())) {
            byPosition.put(o.getPositionRef(), o.getPositionValue());
        }
        return byPosition;
    }
}
