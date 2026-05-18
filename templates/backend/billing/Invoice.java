/**
 * @ax-template-meta
 * template_id: backend/billing/Invoice
 * layer: backend-domain
 * domain: billing
 * anchors_rule: currency-amount-precision-explicit.md
 * provenance_class: internal_design
 * evidence:
 *   - source_type: upstream_id
 *     upstream_id: stripe-billing-2026-05
 *     section: "Subscription lifecycle"
 *     quote: "invoice.payment_succeeded — Invoice paid; renew subscription."
 *   - source_type: external
 *     citation: "ISO 4217 — KRW amounts as integer won; no decimal places"
 *     url: "https://www.iso.org/iso-4217-currency-codes.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   All monetary amounts are integer minor units.
 *   @SQLDelete required on this @Entity subclass.
 */
package com.example.app.billing;

import com.example.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.SQLDelete;

/**
 * Invoice entity — billing invoice for a subscription period.
 *
 * <p>All amounts are integer minor units (KRW: won, USD: cents).
 * BigDecimal or float fields for monetary amounts violate rule
 * {@code currency-amount-precision-explicit}.
 *
 * <p>Boundary: Invoice belongs to billing domain. No import from payment domain.
 */
@Entity
@SQLDelete(sql = "UPDATE billing_invoices SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Table(
    name = "billing_invoices",
    indexes = {
        @Index(name = "idx_billing_invoices_sub_status", columnList = "subscription_id, status"),
        @Index(name = "idx_billing_invoices_user", columnList = "user_id"),
    }
)
public class Invoice extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    /**
     * Amount due in integer minor currency units.
     * KRW: won. USD: cents. Never float or BigDecimal.
     */
    @Column(name = "amount_due", nullable = false)
    private long amountDue;

    /** Amount actually paid (minor units). 0 if not yet paid. */
    @Column(name = "amount_paid", nullable = false)
    private long amountPaid = 0;

    /** ISO 4217 currency code (e.g., "KRW", "USD"). */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    /** When the invoice was issued (sent to customer). */
    @Column(name = "issued_at")
    private Instant issuedAt;

    /** When the invoice was paid. Null if unpaid. */
    @Column(name = "paid_at")
    private Instant paidAt;

    /** Billing period start (date-only). */
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    /** Billing period end (date-only). */
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    /** Provider invoice ID (e.g., Stripe inv_xxx). */
    @Column(name = "provider_invoice_id", length = 255)
    private String providerInvoiceId;

    protected Invoice() {}

    public static Invoice create(
            UUID userId,
            Subscription subscription,
            long amountDue,
            String currency,
            LocalDate periodStart,
            LocalDate periodEnd) {
        var inv = new Invoice();
        inv.userId = userId;
        inv.subscription = subscription;
        inv.amountDue = amountDue;
        inv.currency = currency;
        inv.periodStart = periodStart;
        inv.periodEnd = periodEnd;
        inv.status = InvoiceStatus.OPEN;
        inv.issuedAt = Instant.now();
        return inv;
    }

    public void markPaid(long amountPaid) {
        this.amountPaid = amountPaid;
        this.status = InvoiceStatus.PAID;
        this.paidAt = Instant.now();
    }

    public void void_() {
        this.status = InvoiceStatus.VOID;
    }

    public void setProviderInvoiceId(String id) {
        this.providerInvoiceId = id;
    }

    public UUID getUserId()                   { return userId; }
    public Subscription getSubscription()     { return subscription; }
    public long getAmountDue()                { return amountDue; }
    public long getAmountPaid()               { return amountPaid; }
    public String getCurrency()               { return currency; }
    public InvoiceStatus getStatus()          { return status; }
    public Instant getIssuedAt()              { return issuedAt; }
    public Instant getPaidAt()                { return paidAt; }
    public LocalDate getPeriodStart()         { return periodStart; }
    public LocalDate getPeriodEnd()           { return periodEnd; }
    public String getProviderInvoiceId()      { return providerInvoiceId; }

    public enum InvoiceStatus {
        DRAFT, OPEN, PAID, VOID, UNCOLLECTIBLE
    }
}
