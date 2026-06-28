package com.ax.template.authblueprint.currencyarithmetic;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Currency-arithmetic — the SOLE mutator of the {@link CurrencyLedger} balance. Every balance move
 * runs through the fail-closed {@link CurrencyMoney} arithmetic, so a cross-currency operation
 * throws {@link CurrencyArithmeticException} (CURRENCY_MISMATCH → 422) BEFORE any persisted write:
 * a rejected operation leaves the balance unmutated (fail-closed, no partial write). The exchange
 * RATE is never computed or looked up here — a cross-currency combination is permitted only through
 * an explicit, RECORDED {@link CurrencyConversion} that supplies the converted amount.
 */
@Service
public class CurrencyArithmeticService {

    private final CurrencyLedgerRepository ledgers;
    private final CurrencyArithmeticMetrics metrics;
    private final Clock clock;

    public CurrencyArithmeticService(CurrencyLedgerRepository ledgers,
                                     CurrencyArithmeticMetrics metrics, Clock clock) {
        this.ledgers = ledgers;
        this.metrics = metrics;
        this.clock = clock;
    }

    // ─── Ledger lifecycle ───────────────────────────────────────────────────────────

    @Transactional
    public CurrencyLedger createLedger(String currency, long openingMinor) {
        // Validate the currency tag through the value type (422 CURRENCY_INVALID on a bad code).
        String code = CurrencyMoney.requireIso4217(currency);
        CurrencyLedger ledger = new CurrencyLedger(UUID.randomUUID(), code, openingMinor, Instant.now(clock));
        return ledgers.saveAndFlush(ledger);
    }

    @Transactional(readOnly = true)
    public CurrencyLedger getLedger(UUID id) {
        return ledgers.findById(id)
            .orElseThrow(() -> CurrencyArithmeticException.ledgerNotFound(id.toString()));
    }

    // ─── Fail-closed arithmetic (the sole mutator of the balance) ─────────────────────

    /**
     * Add an amount to the ledger balance. The {@link CurrencyMoney#plus} guard throws
     * CURRENCY_MISMATCH if {@code addend}'s currency differs from the ledger's — and it does so
     * before {@link CurrencyLedger#applyBalance}, so a cross-currency add leaves the balance unchanged.
     */
    @Transactional
    public CurrencyLedger add(UUID ledgerId, CurrencyMoney addend) {
        CurrencyLedger ledger = require(ledgerId);
        CurrencyMoney updated = ledger.balance().plus(addend);   // THROWS on a currency mismatch
        ledger.applyBalance(updated);
        CurrencyLedger saved = ledgers.saveAndFlush(ledger);
        metrics.recordOperation("added");
        return saved;
    }

    /** Subtract an amount — symmetric with {@link #add}: a cross-currency subtrahend fails closed. */
    @Transactional
    public CurrencyLedger subtract(UUID ledgerId, CurrencyMoney subtrahend) {
        CurrencyLedger ledger = require(ledgerId);
        CurrencyMoney updated = ledger.balance().minus(subtrahend);   // THROWS on a currency mismatch
        ledger.applyBalance(updated);
        CurrencyLedger saved = ledgers.saveAndFlush(ledger);
        metrics.recordOperation("subtracted");
        return saved;
    }

    /**
     * Add a FOREIGN-currency amount via an explicit, RECORDED conversion — the only sanctioned
     * cross-currency path. The conversion re-tags {@code foreign} into its {@code toCurrency} (the
     * converted amount is supplied; no rate is computed), after which the same-currency
     * {@link CurrencyMoney#plus} applies. A conversion whose {@code toCurrency} does not match the
     * ledger still fails closed (the plus throws). The conversion is recorded for audit.
     */
    @Transactional
    public CurrencyLedger addConverted(UUID ledgerId, CurrencyMoney foreign, CurrencyConversion conversion) {
        CurrencyLedger ledger = require(ledgerId);
        CurrencyMoney converted = foreign.convertedVia(conversion);  // explicit re-tag (THROWS on from-mismatch)
        CurrencyMoney updated = ledger.balance().plus(converted);    // same-currency add (THROWS if to != ledger)
        ledger.applyBalance(updated);
        ledger.recordConversion(new ConversionRecord(
            conversion.fromCurrency(), conversion.toCurrency(),
            foreign.minorUnits(), converted.minorUnits(), Instant.now(clock)));
        CurrencyLedger saved = ledgers.saveAndFlush(ledger);
        metrics.recordOperation("converted");
        return saved;
    }

    private CurrencyLedger require(UUID ledgerId) {
        return ledgers.findById(ledgerId)
            .orElseThrow(() -> CurrencyArithmeticException.ledgerNotFound(ledgerId.toString()));
    }
}
