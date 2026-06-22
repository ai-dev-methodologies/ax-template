package com.ax.template.authblueprint.settlement;

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
 * settlement-finality-l0 post-trade delivery-versus-payment instruction (SETTLE-DVP-001).
 *
 * <p>BIS CPMI defines delivery versus payment as "a link between a securities transfer system
 * and a funds transfer system that ensures that delivery occurs if, and only if, payment
 * occurs." This entity makes PARTIAL settlement UNREPRESENTABLE: the two leg-settled flags are
 * @Check-constrained to be equal, so a row can never persist with one leg settled and the other
 * not — DvP is a DB-level invariant, not just service logic.
 *
 * <p>Finality (SETTLE-FINAL-001): {@code status = SETTLED} is the IRREVOCABLE FINAL state
 * (BIS CPMI "final settlement" = "settlement which is irrevocable and unconditional"). The
 * @Check backstops that SETTLED implies both legs settled and a recorded finality instant.
 * After finality novation/cancel/amend are all 409.
 *
 * <p>Lifecycle and leg application move ONLY via the package-private hooks, called by
 * {@link SettlementService} under the instruction's PESSIMISTIC_WRITE row lock. There is NO
 * public setter and NO delete path.
 */
@AggregateRoot
@Entity
@Table(name = "settlement_instructions")
@Check(constraints =
    // DvP — both legs settle atomically or neither (partial settlement unrepresentable)
    "delivery_settled = payment_settled"
    // finality backstop — SETTLED implies both legs settled + a recorded final instant
    + " AND (status <> 'SETTLED' OR (delivery_settled = TRUE AND final_at IS NOT NULL))"
    // a non-final instruction must not carry a finality instant
    + " AND (status = 'SETTLED' OR final_at IS NULL)")
public class SettlementInstruction {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "trade_ref", nullable = false, updatable = false, length = 100)
    private String tradeRef;

    /** The party owing the delivery leg (securities). Replaceable by novation before finality. */
    @Column(name = "delivery_party", nullable = false, length = 200)
    private String deliveryParty;

    /** The party owing the payment leg (funds). Replaceable by novation before finality. */
    @Column(name = "payment_party", nullable = false, length = 200)
    private String paymentParty;

    /** The conserved net obligation — novation replaces a party but NEVER changes this amount. */
    @Column(name = "net_obligation", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal netObligation;

    @Column(name = "delivery_settled", nullable = false)
    private boolean deliverySettled;

    @Column(name = "payment_settled", nullable = false)
    private boolean paymentSettled;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SettlementStatus status;

    /** The recorded instant of finality (SETTLE-FINAL-001) — set exactly when status → SETTLED. */
    @Column(name = "final_at")
    private Instant finalAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SettlementInstruction() {}

    public SettlementInstruction(UUID id, String tradeRef, String deliveryParty, String paymentParty,
                                 BigDecimal netObligation, Instant createdAt) {
        this.id = id;
        this.tradeRef = tradeRef;
        this.deliveryParty = deliveryParty;
        this.paymentParty = paymentParty;
        this.netObligation = netObligation;
        this.deliverySettled = false;
        this.paymentSettled = false;
        this.status = SettlementStatus.PENDING;
        this.createdAt = createdAt;
    }

    /**
     * Sole-mutator hook — settle BOTH legs atomically and reach finality (SETTLE-DVP/FINAL-001).
     * Either both flags flip and the instant is recorded, or nothing happens — there is no
     * code path that flips one leg alone.
     */
    void settleBothLegs(Instant finalAt) {
        this.deliverySettled = true;
        this.paymentSettled = true;
        this.status = SettlementStatus.SETTLED;
        this.finalAt = finalAt;
    }

    /** Sole-mutator hook — walk the fail ladder (driven by {@link SettlementFailLadder}). */
    void moveStatus(SettlementStatus next) {
        this.status = next;
    }

    /**
     * Sole-mutator hook — novation replaces ONE counterparty, conserving the net obligation
     * (SETTLE-NOVATE-001). netObligation is @Column(updatable=false) so it cannot drift here.
     */
    void replaceDeliveryParty(String newParty) {
        this.deliveryParty = newParty;
    }

    void replacePaymentParty(String newParty) {
        this.paymentParty = newParty;
    }

    public UUID getId() { return id; }
    public String getTradeRef() { return tradeRef; }
    public String getDeliveryParty() { return deliveryParty; }
    public String getPaymentParty() { return paymentParty; }
    public BigDecimal getNetObligation() { return netObligation; }
    public boolean isDeliverySettled() { return deliverySettled; }
    public boolean isPaymentSettled() { return paymentSettled; }
    public SettlementStatus getStatus() { return status; }
    public Instant getFinalAt() { return finalAt; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }

    public boolean isFinal() { return status == SettlementStatus.SETTLED; }
}
