package com.ax.template.authblueprint.mandate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One declared check in a {@link Mandate}'s battery (MANDATE-BATTERY-001). Declared at issue time
 * with verdict PENDING; recording a verdict is idempotent on {@code (mandate_id, check_key)} — a
 * later verdict supersedes the earlier on the SAME row, never a duplicate. The mandate is
 * SATISFIED only when EVERY check in the battery is recorded PASSED — the gate reads these
 * per-check verdicts, not a bare aggregate. The check key is immutable; the verdict is the only
 * mutable column (the recorded-verdict supersession).
 */
@AggregateMember(root = Mandate.class)
@Entity
@Table(name = "mandate_checks", uniqueConstraints = {
    @UniqueConstraint(name = "uq_mandate_check_key", columnNames = {"mandate_id", "check_key"})
})
public class MandateCheck {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "mandate_id", nullable = false, updatable = false)
    private UUID mandateId;

    @Column(name = "check_key", nullable = false, updatable = false, length = 100)
    private String checkKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", nullable = false, length = 20)
    private MandateCheckVerdict verdict;

    @Column(name = "recorded_by", length = 200)
    private String recordedBy;

    @Column(name = "recorded_at")
    private Instant recordedAt;

    @Column(name = "declared_at", nullable = false, updatable = false)
    private Instant declaredAt;

    protected MandateCheck() {}

    private MandateCheck(UUID id, UUID mandateId, String checkKey, Instant declaredAt) {
        this.id = id;
        this.mandateId = mandateId;
        this.checkKey = checkKey;
        this.verdict = MandateCheckVerdict.PENDING;
        this.declaredAt = declaredAt;
    }

    /** Declare a battery check with no verdict yet (MANDATE-BATTERY-001). */
    public static MandateCheck declared(UUID id, UUID mandateId, String checkKey, Instant declaredAt) {
        return new MandateCheck(id, mandateId, checkKey, declaredAt);
    }

    /** Sole-mutator hook (service) — record/supersede this check's verdict (idempotent on the key). */
    void record(MandateCheckVerdict newVerdict, String by, Instant at) {
        this.verdict = newVerdict;
        this.recordedBy = by;
        this.recordedAt = at;
    }

    public boolean isPassed() {
        return verdict == MandateCheckVerdict.PASSED;
    }

    public UUID getId() { return id; }
    public UUID getMandateId() { return mandateId; }
    public String getCheckKey() { return checkKey; }
    public MandateCheckVerdict getVerdict() { return verdict; }
    public String getRecordedBy() { return recordedBy; }
    public Instant getRecordedAt() { return recordedAt; }
    public Instant getDeclaredAt() { return declaredAt; }
}
