package com.ax.template.authblueprint.governedrecord;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * attested-change-record-l0 sole orchestrator. A governed datum's value is mutated ONLY through
 * {@link #changeValue}, which (in one transaction, under the datum's row lock) requires a non-blank
 * reason (422 before any change), reads the pre-edit value as oldValue, appends an immutable
 * {@link ChangeRecord} with a monotonic per-field sequence, then advances the value. There is no
 * other mutation path (the field has no public setter).
 */
@Service
public class GovernedRecordService {

    /** The single governed field of the reference workload. The change sequence is per (datum, field),
     *  so this constant is the field key for both the sequence allocation and the appended record. */
    static final String FIELD_VALUE = "value";

    /** Upper bound on a single history page — the trail is fully retrievable by paging, but one
     *  request cannot pull an unbounded slice. */
    static final int MAX_HISTORY_PAGE_SIZE = 200;

    private final GovernedDatumRepository datumRepo;
    private final ChangeRecordRepository changeRepo;
    private final GovernedRecordMetrics metrics;
    private final Clock clock;
    private final Set<String> reasonVocabulary;       // empty => free-text reasons allowed
    private final String reasonVocabularyVersion;     // null => not pinned (only valid when vocab empty)

    public GovernedRecordService(GovernedDatumRepository datumRepo, ChangeRecordRepository changeRepo,
                                 GovernedRecordMetrics metrics, Clock clock,
                                 @Value("${governed-record.reason-vocabulary:}") String reasonVocabularyCsv,
                                 @Value("${governed-record.reason-vocabulary-version:}") String reasonVocabularyVersion) {
        this.datumRepo = datumRepo;
        this.changeRepo = changeRepo;
        this.metrics = metrics;
        this.clock = clock;
        this.reasonVocabulary = reasonVocabularyCsv == null || reasonVocabularyCsv.isBlank()
            ? Set.of()
            : Arrays.stream(reasonVocabularyCsv.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        // ACR-VOCAB-001 — a pinned vocabulary version is meaningful ONLY when a vocabulary is configured.
        // Normalize blank -> null so a missing version can never masquerade as a present-but-empty pin, and
        // fail FAST if a vocabulary is configured without a version (otherwise every change row would pin "").
        this.reasonVocabularyVersion =
            reasonVocabularyVersion == null || reasonVocabularyVersion.isBlank() ? null : reasonVocabularyVersion.strip();
        if (!this.reasonVocabulary.isEmpty() && this.reasonVocabularyVersion == null) {
            throw new IllegalStateException(
                "governed-record.reason-vocabulary-version must be set (non-blank) when "
                    + "governed-record.reason-vocabulary is configured — the pinned version makes a reason "
                    + "code reproducible after the vocabulary evolves (ACR-VOCAB-001).");
        }
    }

    @Transactional
    public GovernedDatum createDatum(String createdBy, String name, String value) {
        if (datumRepo.existsByName(name)) {
            metrics.record("create", "rejected");
            throw AttestedException.duplicateName();
        }
        try {
            // saveAndFlush so a create-create race on the same name resolves to the SAME deterministic 409
            // as the pre-check (the unique index is the backstop), never an opaque 500.
            GovernedDatum d = datumRepo.saveAndFlush(
                new GovernedDatum(UUID.randomUUID(), name, value, createdBy, Instant.now(clock)));
            metrics.record("create", "ok");
            return d;
        } catch (DataIntegrityViolationException e) {
            metrics.record("create", "rejected");
            throw AttestedException.duplicateName();
        }
    }

    /** ACR-ENVELOPE-001 / ACR-PREIMAGE-001 / ACR-APPEND-ONLY-001 — the sole governed mutator. */
    @Transactional
    public GovernedDatum changeValue(UUID id, String newValue, String reasonRaw, String actor) {
        if (reasonRaw == null || reasonRaw.isBlank()) {     // reject BEFORE any change
            metrics.record("change", "reason_required");
            throw AttestedException.reasonRequired();
        }
        // Normalize the reason symmetrically with the configured vocabulary (which is trimmed): a member
        // submitted with surrounding whitespace must match, and the immutable record stores the clean form.
        String reason = reasonRaw.strip();
        if (!ReasonVocabulary.isAllowed(reason, reasonVocabulary)) {   // ACR-VOCAB-001 (when configured)
            metrics.record("change", "unknown_reason");
            throw AttestedException.unknownReason();
        }
        GovernedDatum d = datumRepo.findByIdForUpdate(id).orElseThrow(AttestedException::notFound);
        String oldValue = d.getValue();                     // pre-image, read under the lock
        long seq = changeRepo.maxSequence(id, FIELD_VALUE) + 1;   // monotonic per (datum, field), under the lock
        String vocabVersion = reasonVocabulary.isEmpty() ? null : reasonVocabularyVersion;
        try {
            // saveAndFlush so the uq_governed_change_seq backstop (if the row lock failed to serialize)
            // surfaces as a deterministic, retryable 409 rather than an opaque 500.
            changeRepo.saveAndFlush(new ChangeRecord(UUID.randomUUID(), id, FIELD_VALUE, seq, oldValue, newValue,
                reason, vocabVersion, actor, Instant.now(clock)));
        } catch (DataIntegrityViolationException e) {
            metrics.record("change", "conflict");
            throw AttestedException.sequenceConflict();
        }
        d.setValueInternal(newValue);                       // package-private; no public setter
        metrics.record("change", "ok");
        return d;
    }

    @Transactional(readOnly = true)
    public GovernedDatum get(UUID id) {
        return datumRepo.findById(id).orElseThrow(AttestedException::notFound);
    }

    /** Paginated append-only history (causal/sequence order). The {@link Page} carries totalElements +
     *  hasNext so a consumer can retrieve EVERY record and can tell when more exist — the trail is never
     *  silently truncated (which would itself obscure recorded changes, inverting 21 CFR 11.10(e)). */
    @Transactional(readOnly = true)
    public Page<ChangeRecord> history(UUID id, int page, int size) {
        if (!datumRepo.existsById(id)) {
            throw AttestedException.notFound();
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_HISTORY_PAGE_SIZE);
        return changeRepo.findByDatumIdOrderBySequenceNoAsc(id, PageRequest.of(safePage, safeSize));
    }
}
