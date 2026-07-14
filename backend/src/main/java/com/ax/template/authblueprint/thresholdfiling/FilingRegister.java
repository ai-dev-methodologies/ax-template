package com.ax.template.authblueprint.thresholdfiling;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * threshold-filing-obligation-l0 root: a per-subject cumulative {@code accruedValue} with a
 * mandatory immutable {@code threshold}. Composes threshold-terminal-derivation's atomic-crossing
 * shape (TFO-TRIGGER-001): the accrual that makes the value reach/cross the threshold flips
 * {@code status} to the irreversible {@link FilingRegisterStatus#TRIGGERED} in the SAME
 * transaction that binds its {@link FilingObligation} member — one aggregate, one transaction
 * (HG-ANTI-GODSERVICE-TX). The @Check backstops the implication even under ddl-auto. The lifecycle
 * moves ONLY via the package-private {@link #markTriggered()} called by
 * {@link FilingRegisterStateMachine} (sole mutator); the accrued value moves ONLY via the
 * package-private {@link #advanceAccrual} called by {@link FilingService} under a
 * PESSIMISTIC_WRITE row lock.
 */
@AggregateRoot
@Entity
@Table(name = "filing_registers")
@Check(constraints = "threshold_value > 0 AND accrued_value >= 0 "
    + "AND (accrued_value < threshold_value OR status = 'TRIGGERED')")
public class FilingRegister {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "subject_key", nullable = false, updatable = false, length = 200, unique = true)
    private String subjectKey;

    @Column(name = "threshold_value", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal threshold;

    @Column(name = "accrued_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal accruedValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FilingRegisterStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FilingRegister() {}

    public FilingRegister(UUID id, String subjectKey, BigDecimal threshold, Instant createdAt) {
        this.id = id;
        this.subjectKey = subjectKey;
        this.threshold = threshold;
        this.accruedValue = BigDecimal.ZERO.setScale(4);
        this.status = FilingRegisterStatus.ACTIVE;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook (service, under the row lock) — advance the accrued value. */
    void advanceAccrual(BigDecimal next) {
        this.accruedValue = next;
    }

    /** Sole-mutator hook ({@link FilingRegisterStateMachine} only) — the one-way crossing edge. */
    void markTriggered() {
        this.status = FilingRegisterStatus.TRIGGERED;
    }

    public UUID getId() { return id; }
    public String getSubjectKey() { return subjectKey; }
    public BigDecimal getThreshold() { return threshold; }
    public BigDecimal getAccruedValue() { return accruedValue; }
    public FilingRegisterStatus getStatus() { return status; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
