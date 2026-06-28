package com.ax.template.authblueprint.currencyarithmetic;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;

/**
 * One RECORDED cross-currency conversion applied to a {@link CurrencyLedger} — the audit trail that
 * makes the (otherwise-forbidden) cross-currency combination explicit rather than implicit. Owned by
 * the {@link CurrencyLedger} aggregate root via an {@code @ElementCollection}, so it carries no
 * identity or repository of its own (an {@code @Embeddable}, not an {@code @Entity} — no aggregate
 * tag needed).
 */
@Embeddable
public class ConversionRecord {

    @Column(name = "from_currency", nullable = false)
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false)
    private String toCurrency;

    /** The source amount in the from-currency, integer minor units. */
    @Column(name = "source_minor", nullable = false)
    private long sourceMinor;

    /** The supplied converted amount in the to-currency, integer minor units (no rate stored — out of scope). */
    @Column(name = "converted_minor", nullable = false)
    private long convertedMinor;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected ConversionRecord() {}

    public ConversionRecord(String fromCurrency, String toCurrency,
                            long sourceMinor, long convertedMinor, Instant recordedAt) {
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.sourceMinor = sourceMinor;
        this.convertedMinor = convertedMinor;
        this.recordedAt = recordedAt;
    }

    public String getFromCurrency() { return fromCurrency; }
    public String getToCurrency() { return toCurrency; }
    public long getSourceMinor() { return sourceMinor; }
    public long getConvertedMinor() { return convertedMinor; }
    public Instant getRecordedAt() { return recordedAt; }
}
