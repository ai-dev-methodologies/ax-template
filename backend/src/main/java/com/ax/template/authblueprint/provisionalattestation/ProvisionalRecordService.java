package com.ax.template.authblueprint.provisionalattestation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * provisional-attestation-l0 sole orchestrator. PATT-LIFECYCLE-001's 2-state transition is
 * delegated to {@link ProvisionalRecordStateMachine}; PATT-DISTINCT-002 (attestor != author) is
 * checked here BEFORE the state machine is invoked, so a self-attestation never reaches even a
 * partial transition. PATT-FREEZE-003: an edit is rejected once ATTESTED (409) and only the
 * author may edit while PROVISIONAL (403); the attest action computes the content-hash of what
 * is being attested at that instant.
 */
@Service
public class ProvisionalRecordService {

    private final ProvisionalRecordRepository repository;
    private final ProvisionalRecordStateMachine stateMachine;
    private final Clock clock;

    public ProvisionalRecordService(ProvisionalRecordRepository repository,
                                    ProvisionalRecordStateMachine stateMachine, Clock clock) {
        this.repository = repository;
        this.stateMachine = stateMachine;
        this.clock = clock;
    }

    @Transactional
    public ProvisionalRecord author(String authoredBy, String content) {
        return repository.save(ProvisionalRecord.author(authoredBy, content, Instant.now(clock)));
    }

    /** PATT-FREEZE-003 — only the author may edit, and only while PROVISIONAL. */
    @Transactional
    public ProvisionalRecord editContent(UUID id, String callerId, String newContent) {
        ProvisionalRecord record = getOrThrow(id);
        if (record.getStatus() == ProvisionalRecordStatus.ATTESTED) {
            throw ProvisionalAttestationException.illegalTransition();  // frozen — 409
        }
        if (!record.getAuthoredBy().equals(callerId)) {
            throw ProvisionalAttestationException.editNotAuthor();      // 403
        }
        record.editContent(newContent);
        return record;
    }

    /** PATT-DISTINCT-002 — the attestor MUST differ from the author, checked BEFORE any
     *  transition is attempted (fail-closed, no partial attestation). */
    @Transactional
    public ProvisionalRecord attest(UUID id, String attestorId) {
        ProvisionalRecord record = getOrThrow(id);
        if (attestorId.equals(record.getAuthoredBy())) {
            throw ProvisionalAttestationException.attestorMustDifferFromAuthor();
        }
        String contentHash = ContentHasher.sha256Hex(record.getContent());
        stateMachine.attest(record, attestorId, contentHash, Instant.now(clock));
        return record;
    }

    /** PATT-FREEZE-003 — recompute the current content-hash and compare to the attested one;
     *  a mismatch can only arise from an out-of-band write that bypassed the frozen-content
     *  guard, so it is reported as tamper-detected rather than silently trusted. */
    @Transactional(readOnly = true)
    public boolean verifyIntegrity(UUID id) {
        ProvisionalRecord record = getOrThrow(id);
        if (record.getStatus() != ProvisionalRecordStatus.ATTESTED) {
            throw ProvisionalAttestationException.notYetAttested();
        }
        String currentHash = ContentHasher.sha256Hex(record.getContent());
        return !currentHash.equals(record.getAttestedContentHash());
    }

    /** PATT-DOWNSTREAM-004 — attested-only (default) vs include-provisional filter. */
    @Transactional(readOnly = true)
    public Page<ProvisionalRecord> list(boolean includeProvisional, Pageable pageable) {
        if (includeProvisional) {
            return repository.findAll(pageable);
        }
        return repository.findByStatus(ProvisionalRecordStatus.ATTESTED, pageable);
    }

    @Transactional(readOnly = true)
    public ProvisionalRecord getOrThrow(UUID id) {
        return repository.findById(id).orElseThrow(ProvisionalAttestationException::notFound);
    }
}
