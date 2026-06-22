package com.ax.template.authblueprint.settlement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * One immutable novation record (SETTLE-NOVATE-001): the audit trail of a counterparty
 * replacement before finality. BIS CPMI defines novation as "satisfaction and discharge of
 * existing contractual obligations by means of their replacement by new obligations... there
 * may additionally be substitution of parties."
 *
 * <p>OBLIGATION CONSERVED: a novation records the released (old) party, the assuming (new)
 * party, and the net obligation — and the @Check backstops that the conserved-obligation column
 * is recorded verbatim per row. The original instruction's {@code net_obligation} is
 * {@code @Column(updatable=false)}; this row is the append-only proof that the amount the new
 * party assumed is identical to the amount the old party was released from.
 *
 * <p>Append-only: every column is {@code @Column(updatable=false)} and there is no public setter.
 * {@code @AggregateMember} of {@link SettlementInstruction} — root-JPQL reads,
 * {@code common/MemberWriter} writes.
 */
@AggregateMember(root = SettlementInstruction.class)
@Entity
@Table(name = "novation_records")
@Check(constraints = "released_party <> assuming_party AND assumed_obligation >= 0")
public class NovationRecord {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "instruction_id", nullable = false, updatable = false)
    private UUID instructionId;

    /** DELIVERY or PAYMENT — which leg's counterparty was replaced. */
    @Enumerated(EnumType.STRING)
    @Column(name = "leg", nullable = false, updatable = false, length = 20)
    private SettlementLeg leg;

    @Column(name = "released_party", nullable = false, updatable = false, length = 200)
    private String releasedParty;

    @Column(name = "assuming_party", nullable = false, updatable = false, length = 200)
    private String assumingParty;

    /** The conserved obligation the new party assumed — identical to what the old was released from. */
    @Column(name = "assumed_obligation", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal assumedObligation;

    @Column(name = "novated_by", nullable = false, updatable = false, length = 200)
    private String novatedBy;

    @Column(name = "novated_at", nullable = false, updatable = false)
    private Instant novatedAt;

    protected NovationRecord() {}

    public NovationRecord(UUID id, UUID instructionId, SettlementLeg leg, String releasedParty,
                          String assumingParty, BigDecimal assumedObligation, String novatedBy,
                          Instant novatedAt) {
        this.id = id;
        this.instructionId = instructionId;
        this.leg = leg;
        this.releasedParty = releasedParty;
        this.assumingParty = assumingParty;
        this.assumedObligation = assumedObligation;
        this.novatedBy = novatedBy;
        this.novatedAt = novatedAt;
    }

    public UUID getId() { return id; }
    public UUID getInstructionId() { return instructionId; }
    public SettlementLeg getLeg() { return leg; }
    public String getReleasedParty() { return releasedParty; }
    public String getAssumingParty() { return assumingParty; }
    public BigDecimal getAssumedObligation() { return assumedObligation; }
    public String getNovatedBy() { return novatedBy; }
    public Instant getNovatedAt() { return novatedAt; }
}
