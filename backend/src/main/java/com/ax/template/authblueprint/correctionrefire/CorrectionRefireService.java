package com.ax.template.authblueprint.correctionrefire;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * correction-refire-l0 sole orchestrator. {@link #publish} is idempotent on content-hash
 * (CRF-IDEMPOTENT-003: an identical re-publish is a no-op — no new version, no new ack); a REAL
 * content change appends a new version (CRF-SUPERSEDE-001) and, in the SAME transaction, creates
 * a brand-new PENDING {@link AckRecord} for that version (CRF-REFIRE-002 / CRF-CHAIN-004) — the
 * prior version's ack row (CLOSED or PENDING) is never read back or mutated by this path.
 */
@Service
public class CorrectionRefireService {

    private final CorrectedRecordRepository records;
    private final MemberWriter members;
    private final Clock clock;

    public CorrectionRefireService(CorrectedRecordRepository records, MemberWriter members, Clock clock) {
        this.records = records;
        this.members = members;
        this.clock = clock;
    }

    /** CRF-SUPERSEDE-001 / CRF-IDEMPOTENT-003 / CRF-REFIRE-002 — publish the first version of a
     *  subject, or a correction to its current version. Identical content is a no-op. */
    @Transactional
    public CorrectedRecord publish(String subjectRef, String content) {
        Optional<CorrectedRecord> current = records.findTopBySubjectRefOrderByVersionDesc(subjectRef);
        String hash = ContentHasher.sha256Hex(content);

        // CRF-IDEMPOTENT-003 — identical content re-publish is a no-op: no new version, no ack spam.
        if (current.isPresent() && current.get().getContentHash().equals(hash)) {
            return current.get();
        }

        int nextVersion = current.map(r -> r.getVersion() + 1).orElse(1);
        Integer correctsVersion = current.map(CorrectedRecord::getVersion).orElse(null);
        Instant now = Instant.now(clock);
        CorrectedRecord record;
        try {
            record = records.saveAndFlush(CorrectedRecord.publish(UUID.randomUUID(), subjectRef, nextVersion,
                content, hash, correctsVersion, now));
        } catch (DataIntegrityViolationException dup) {
            throw CorrectionRefireException.versionConflict();
        }
        // CRF-REFIRE-002 / CRF-CHAIN-004 — a brand-new PENDING ack for THIS version, independent
        // of whatever state the prior version's ack was in.
        members.persist(AckRecord.pending(UUID.randomUUID(), record.getId(), now));
        return record;
    }

    /** Close the ack for a specific published version. */
    @Transactional
    public AckRecord acknowledge(String subjectRef, int version) {
        CorrectedRecord record = getVersionOrThrow(subjectRef, version);
        AckRecord ack = records.findAckByRecordId(record.getId()).orElseThrow(CorrectionRefireException::notFound);
        ack.close(Instant.now(clock));
        return ack;
    }

    /** CRF-CHAIN-004 — the current version, ALWAYS derived as MAX(version), never a stored pointer. */
    @Transactional(readOnly = true)
    public CorrectedRecord current(String subjectRef) {
        return records.findTopBySubjectRefOrderByVersionDesc(subjectRef)
            .orElseThrow(CorrectionRefireException::notFound);
    }

    @Transactional(readOnly = true)
    public CorrectedRecord getVersionOrThrow(String subjectRef, int version) {
        return records.findBySubjectRefAndVersion(subjectRef, version)
            .orElseThrow(CorrectionRefireException::notFound);
    }

    @Transactional(readOnly = true)
    public AckRecord getAck(String subjectRef, int version) {
        CorrectedRecord record = getVersionOrThrow(subjectRef, version);
        return records.findAckByRecordId(record.getId()).orElseThrow(CorrectionRefireException::notFound);
    }

    @Transactional(readOnly = true)
    public List<CorrectedRecord> chain(String subjectRef) {
        List<CorrectedRecord> all = records.findBySubjectRefOrderByVersionAsc(subjectRef);
        if (all.isEmpty()) {
            throw CorrectionRefireException.notFound();
        }
        return all;
    }
}
