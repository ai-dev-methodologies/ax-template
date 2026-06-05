package com.ax.template.authblueprint.softdelete;

import com.ax.template.authblueprint.common.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * soft-delete-l0 lifecycle core. Every state transition (tombstone, default-excluding read, restore,
 * cascade, purge) lives here so the aggregate stays consistently visible/invisible.
 *
 * <p>Reads use the explicit live finders (default-exclude, SOFTDELETE-QUERY-001); restore/erase/purge
 * use the unfiltered finders (they must see tombstoned rows). Cascade soft-delete + restore run in one
 * {@code @Transactional} unit (SOFTDELETE-CASCADE-001). Uniqueness is checked against LIVE rows only
 * (SOFTDELETE-UNIQUE-001, backed by the partial index). Spec: specs/soft-delete-l0.yaml.
 */
@Service
public class SoftDeleteService {

    static final Duration RECOVERY_WINDOW = Duration.ofDays(30);  // SOFTDELETE-RESTORE-001
    static final Duration RETENTION = Duration.ofDays(90);        // SOFTDELETE-PURGE-001 (>= recovery)

    private final SoftDeleteAccountRepository accounts;
    private final SoftDeleteNoteRepository notes;
    private final SoftDeleteMetrics metrics;

    /** Test seam: a mutable clock lets the restore-window / retention paths be verified deterministically. */
    private volatile Clock clock = Clock.systemUTC();

    public SoftDeleteService(SoftDeleteAccountRepository accounts, SoftDeleteNoteRepository notes,
                             SoftDeleteMetrics metrics) {
        this.accounts = accounts;
        this.notes = notes;
        this.metrics = metrics;
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    private Instant now() {
        return clock.instant();
    }

    // ── create / read ────────────────────────────────────────────────────────

    @Transactional
    public SoftDeleteAccount create(String owner, String email, String name) {
        // SOFTDELETE-UNIQUE-001: collide only with a LIVE row; a tombstoned email is free for reuse.
        // The exists-check is a fast path; the PARTIAL UNIQUE INDEX is the real race backstop — a
        // concurrent insert (or restore) that wins between the check and the flush surfaces here as
        // DataIntegrityViolationException, which we map to the SAME 409 (never a raw 500).
        if (accounts.existsByOwnerIdAndEmailAndDeletedAtIsNull(owner, email)) {
            throw uniqueConflict();
        }
        try {
            return accounts.saveAndFlush(new SoftDeleteAccount(UUID.randomUUID(), owner, email, name));
        } catch (DataIntegrityViolationException raceLostToConcurrentLiveRow) {
            throw uniqueConflict();
        }
    }

    private static SoftDeleteConflictException uniqueConflict() {
        return new SoftDeleteConflictException(SoftDeleteConflictException.UNIQUE_CONFLICT,
                "email already in use by a live account");
    }

    @Transactional
    public SoftDeleteNote addNote(UUID accountId, String owner, String text) {
        getLive(accountId, owner); // 404 if the parent is absent/tombstoned
        return notes.save(new SoftDeleteNote(UUID.randomUUID(), accountId, text));
    }

    @Transactional(readOnly = true)
    public SoftDeleteAccount getLive(UUID id, String owner) {
        return accounts.findByIdAndOwnerIdAndDeletedAtIsNull(id, owner)
                .orElseThrow(() -> new ResourceNotFoundException("account not found"));
    }

    @Transactional(readOnly = true)
    public List<SoftDeleteAccount> list(String owner, boolean includeDeleted, boolean admin) {
        // SOFTDELETE-QUERY-001: only ROLE_ADMIN may opt back in; a non-admin flag is ignored, never 500.
        return (includeDeleted && admin) ? accounts.findByOwnerId(owner)
                : accounts.findByOwnerIdAndDeletedAtIsNull(owner);
    }

    /**
     * Owner-scoped live child notes (IDOR-safe): the account MUST belong to the caller — live OR
     * tombstoned (so the owner can still observe cascade after a delete) — else 404. A non-owner can
     * never read another owner's notes by guessing the account id.
     */
    @Transactional(readOnly = true)
    public List<SoftDeleteNote> liveNotes(UUID accountId, String owner) {
        accounts.findByIdAndOwnerId(accountId, owner)
                .orElseThrow(() -> new ResourceNotFoundException("account not found"));
        return notes.findByAccountIdAndDeletedAtIsNull(accountId);
    }

    // ── delete (cascade tombstone) ─────────────────────────────────────────────

    @Transactional
    public void delete(UUID id, String owner) {
        // a second delete of an already-tombstoned row is a 404 (idempotent observable state, MARK-001)
        SoftDeleteAccount account = getLive(id, owner);
        Instant ts = now();
        account.markDeleted(ts);
        // SOFTDELETE-CASCADE-001: tombstone every LIVE child at the SAME instant, one transaction.
        for (SoftDeleteNote note : notes.findByAccountIdAndDeletedAtIsNull(id)) {
            note.markDeleted(ts);
        }
        metrics.deleted(owner);
    }

    // ── restore (cascade) ──────────────────────────────────────────────────────

    @Transactional
    public void restore(UUID id, String owner) {
        SoftDeleteAccount account = accounts.findByIdAndOwnerId(id, owner)
                .orElseThrow(() -> new ResourceNotFoundException("account not found")); // purged → 404
        if (!account.isDeleted()) {
            metrics.restore(owner, "not_deleted");
            throw new SoftDeleteConflictException(SoftDeleteConflictException.NOT_DELETED,
                    "account is not deleted");
        }
        Instant parentDeletedAt = account.getDeletedAt();
        if (Duration.between(parentDeletedAt, now()).compareTo(RECOVERY_WINDOW) > 0) {
            metrics.restore(owner, "window_expired");
            throw new SoftDeleteConflictException(SoftDeleteConflictException.WINDOW_EXPIRED,
                    "recovery window has expired");
        }
        account.clearDeleted();
        // SOFTDELETE-CASCADE-001: restore ONLY children tombstoned at-or-after the parent (children
        // deleted earlier on their own stay deleted).
        for (SoftDeleteNote note : notes.findByAccountId(id)) {
            if (note.isDeleted() && !note.getDeletedAt().isBefore(parentDeletedAt)) {
                note.clearDeleted();
            }
        }
        metrics.restore(owner, "restored");
    }

    // ── purge (retention + erasure) ────────────────────────────────────────────

    /** SOFTDELETE-PURGE-001 erasure: immediate physical delete bypassing the retention timer; idempotent. */
    @Transactional
    public void erase(UUID id, String owner) {
        SoftDeleteAccount account = accounts.findByIdAndOwnerId(id, owner)
                .orElseThrow(() -> new ResourceNotFoundException("account not found")); // already purged → 404
        physicallyDelete(account);
        metrics.purge(owner, "erasure_request");
    }

    /** SOFTDELETE-PURGE-001 retention: physically delete tombstoned rows older than the cutoff. */
    @Transactional
    public int purgeExpired(Instant cutoff) {
        List<SoftDeleteAccount> expired = accounts.findByDeletedAtIsNotNullAndDeletedAtBefore(cutoff);
        for (SoftDeleteAccount account : expired) {
            physicallyDelete(account);
            metrics.purge(account.getOwnerId(), "retention");
        }
        return expired.size();
    }

    /** Scheduled retention sweep. Fork-receivers tune the interval; retention defaults to 90 days. */
    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT1H")
    public void scheduledRetentionPurge() {
        purgeExpired(now().minus(RETENTION));
    }

    private void physicallyDelete(SoftDeleteAccount account) {
        notes.deleteAll(notes.findByAccountId(account.getId())); // sanitize: no recoverable remnants
        accounts.delete(account);
    }
}
