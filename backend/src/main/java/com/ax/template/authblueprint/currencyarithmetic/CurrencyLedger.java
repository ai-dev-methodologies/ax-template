package com.ax.template.authblueprint.currencyarithmetic;

import com.ax.template.authblueprint.common.AggregateRoot;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A small persisted surface for the currency-arithmetic invariant: a single-currency balance that
 * amounts can be added to / subtracted from. Its {@code currencyCode} tag is fixed at creation
 * ({@code @Column(updatable=false)}, no public setter) — so a cross-currency add can NEVER be
 * retroactively legitimized by mutating the tag, and the only way the balance moves is through the
 * sole-mutator {@link CurrencyArithmeticService}, which performs the fail-closed
 * {@link CurrencyMoney} arithmetic.
 *
 * <p>The balance is exposed as a {@link CurrencyMoney} ({@link #balance()}) so every operation runs
 * through the currency-tagged value type's guard. Cross-currency combinations are recorded in the
 * {@link ConversionRecord} trail ({@code @ElementCollection}, owned through this root) — making the
 * sanctioned conversion path auditable, never implicit.
 */
@AggregateRoot
@Entity(name = "CurrencyLedger")
@Table(name = "currency_ledgers")
public class CurrencyLedger {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** The ledger's fixed ISO-4217 currency tag — immutable, so the fail-closed guard cannot be evaded. */
    @Column(name = "currency_code", updatable = false, nullable = false)
    private String currencyCode;

    /** The current balance in integer minor units of {@link #currencyCode}. */
    @Column(name = "balance_minor", nullable = false)
    private long balanceMinor;

    /** The recorded cross-currency conversions applied to this ledger (audit trail). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "currency_ledger_conversions",
        joinColumns = @JoinColumn(name = "ledger_id"))
    private List<ConversionRecord> conversions = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CurrencyLedger() {}

    public CurrencyLedger(UUID id, String currencyCode, long balanceMinor, Instant createdAt) {
        this.id = id;
        this.currencyCode = currencyCode;
        this.balanceMinor = balanceMinor;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public Long getVersion() { return version; }
    public String getCurrencyCode() { return currencyCode; }
    public long getBalanceMinor() { return balanceMinor; }
    public Instant getCreatedAt() { return createdAt; }

    /** Defensive copy — the conversion trail is read-only to callers. */
    public List<ConversionRecord> getConversions() { return List.copyOf(conversions); }

    /** The balance as a currency-tagged value — every operation runs through the fail-closed guard. */
    public CurrencyMoney balance() {
        return new CurrencyMoney(balanceMinor, currencyCode);
    }

    /**
     * Sole-mutator entry point for the balance. Package-private: only {@link CurrencyArithmeticService}
     * may move it, and only with a value already in this ledger's currency (the service obtains it via
     * the fail-closed {@link CurrencyMoney} arithmetic). The currency tag is never re-pointed.
     */
    void applyBalance(CurrencyMoney newBalance) {
        this.balanceMinor = newBalance.minorUnits();
    }

    /** Append a recorded conversion to the audit trail (package-private; through the root). */
    void recordConversion(ConversionRecord record) {
        this.conversions.add(record);
    }
}
