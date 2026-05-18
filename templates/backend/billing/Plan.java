/**
 * @ax-template-meta
 * template_id: backend/billing/Plan
 * layer: backend-domain
 * domain: billing
 * anchors_rule: subscription-state-machine-explicit.md
 * provenance_class: internal_design
 * evidence:
 *   - source_type: upstream_id
 *     upstream_id: stripe-billing-2026-05
 *     section: "Plan / Price model"
 *     quote: "A Price (formerly Plan) defines the recurring amount, currency, and interval."
 *   - source_type: upstream_id
 *     upstream_id: toss-billing-2026-05
 *     section: "정기결제 구독 상태 매핑"
 *     quote: "ax-template 자체 관리"
 *   - source_type: external
 *     citation: "ISO 4217 — KRW has 0 decimal places; amounts stored as integer won"
 *     url: "https://www.iso.org/iso-4217-currency-codes.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Plan entity stores recurring billing plan definitions (price, interval, trial days).
 *   All amounts are integer minor units. Do NOT use BigDecimal or float.
 *   Per soft-delete contract: @SQLDelete on each @Entity subclass, not @MappedSuperclass.
 */
package com.example.app.billing;

import com.example.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.SQLDelete;

/**
 * Plan entity — recurring billing plan definition.
 *
 * <p>Amount is always stored and transmitted as integer minor units:
 * <ul>
 *   <li>KRW: integer won (e.g., 9900 = ₩9,900).</li>
 *   <li>USD: integer cents (e.g., 999 = $9.99).</li>
 * </ul>
 *
 * <p>Soft-delete: {@code @SQLDelete} sets {@code deleted_at} instead of hard-deleting.
 * Active plans are queried via {@code @Where(clause = "deleted_at IS NULL")} on BaseEntity.
 *
 * <p>Boundary: Plan belongs to billing domain. No import from payment domain.
 */
@Entity
@SQLDelete(sql = "UPDATE billing_plans SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Table(
    name = "billing_plans",
    indexes = {
        @Index(name = "idx_billing_plans_active", columnList = "active"),
    }
)
public class Plan extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Price in integer minor currency units.
     * KRW: won (no decimals). USD: cents.
     * Rule: currency-amount-precision-explicit — never store as BigDecimal or float.
     */
    @Column(name = "amount", nullable = false)
    private long amount;

    /** ISO 4217 currency code (e.g., "KRW", "USD"). */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /** Billing interval in days (30 = monthly, 365 = annual). */
    @Column(name = "interval_days", nullable = false)
    private int intervalDays;

    /** Trial period in days (0 = no trial). */
    @Column(name = "trial_days", nullable = false)
    private int trialDays = 0;

    /** Whether this plan is publicly visible and subscribable. */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Feature bullet points shown in PricingTable. */
    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "feature")
    private List<String> features = new ArrayList<>();

    protected Plan() {}

    /**
     * Factory constructor — creates an active Plan.
     */
    public static Plan create(
            String name,
            String description,
            long amount,
            String currency,
            int intervalDays,
            int trialDays,
            List<String> features) {
        var p = new Plan();
        p.name = name;
        p.description = description;
        p.amount = amount;
        p.currency = currency;
        p.intervalDays = intervalDays;
        p.trialDays = trialDays;
        p.features = features != null ? new ArrayList<>(features) : new ArrayList<>();
        p.active = true;
        return p;
    }

    /** Deactivate this plan (soft-hide; existing subscriptions unaffected). */
    public void deactivate() {
        this.active = false;
    }

    public String getName()          { return name; }
    public String getDescription()   { return description; }
    public long getAmount()          { return amount; }
    public String getCurrency()      { return currency; }
    public int getIntervalDays()     { return intervalDays; }
    public int getTrialDays()        { return trialDays; }
    public boolean isActive()        { return active; }
    public List<String> getFeatures(){ return List.copyOf(features); }
}
