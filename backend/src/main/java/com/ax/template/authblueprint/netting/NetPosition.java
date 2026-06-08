package com.ax.template.authblueprint.netting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * collection-conservation-l0 computed net position: one signed net per member of a netting run
 * ({@code netAmount} = Σ owed-to-member − Σ owed-by-member, a net credit if positive / net debit if
 * negative). Immutable output (every column {@code @Column(updatable=false)}, no setter). The unique
 * (run_id, member_id) constraint makes a double-persist (a concurrent re-net) unrepresentable — the
 * structural backstop for NET-ONCE-001. The set of a run's positions sums to exactly 0.
 */
@AggregateMember(root = NettingRun.class)
@Entity
@Table(name = "netting_net_positions",
    uniqueConstraints = @UniqueConstraint(name = "uq_net_position_run_member",
        columnNames = {"run_id", "member_id"}))
public class NetPosition {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "run_id", nullable = false, updatable = false)
    private UUID runId;

    @Column(name = "member_id", nullable = false, updatable = false, length = 120)
    private String member;

    /** Signed net — positive = net creditor (owed), negative = net debtor (owes). */
    @Column(name = "net_amount", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal netAmount;

    protected NetPosition() {}

    public NetPosition(UUID id, UUID runId, String member, BigDecimal netAmount) {
        this.id = id;
        this.runId = runId;
        this.member = member;
        this.netAmount = netAmount;
    }

    public UUID getId() { return id; }
    public UUID getRunId() { return runId; }
    public String getMember() { return member; }
    public BigDecimal getNetAmount() { return netAmount; }
}
