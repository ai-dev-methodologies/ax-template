package com.ax.template.authblueprint.netting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * collection-conservation-l0 directed gross obligation: {@code fromMember} owes {@code toMember}
 * {@code amount} in the run's currency. Append-only INPUT (every column {@code @Column(updatable=false)},
 * no setter) captured while the run is OPEN; once the run is NETTED no obligation may be added or edited
 * (NET-INPUTS-IMMUTABLE-001). {@code @Check} keeps amount positive and forbids a self-obligation.
 */
@AggregateMember(root = NettingRun.class)
@Entity
@Table(name = "netting_gross_obligations")
@Check(constraints = "amount > 0 AND from_member <> to_member")
public class GrossObligation {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "run_id", nullable = false, updatable = false)
    private UUID runId;

    @Column(name = "from_member", nullable = false, updatable = false, length = 120)
    private String fromMember;

    @Column(name = "to_member", nullable = false, updatable = false, length = 120)
    private String toMember;

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected GrossObligation() {}

    public GrossObligation(UUID id, UUID runId, String fromMember, String toMember,
                           BigDecimal amount, String currency, Instant createdAt) {
        this.id = id;
        this.runId = runId;
        this.fromMember = fromMember;
        this.toMember = toMember;
        this.amount = amount;
        this.currency = currency;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getRunId() { return runId; }
    public String getFromMember() { return fromMember; }
    public String getToMember() { return toMember; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public Instant getCreatedAt() { return createdAt; }
}
