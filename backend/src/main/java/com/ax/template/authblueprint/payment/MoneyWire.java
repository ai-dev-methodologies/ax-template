package com.ax.template.authblueprint.payment;

import com.ax.template.authblueprint.common.Money;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * The DESERIALIZED SHAPE of a wire {@code amount}, before it is interpreted against a currency.
 *
 * <h2>Why a shape and not a {@link BigDecimal}</h2>
 * {@code contracts/payment-openapi.yaml#MoneyAmount} declares two accepted request encodings —
 * an integer (MINOR units: {@code 1099} == USD $10.99) and a decimal string (MAJOR units:
 * {@code "10.99"}). The two mean different numbers for every currency with a non-zero ISO-4217
 * minor-unit scale, so the branch taken must survive deserialization. {@link MoneyDeserializer}
 * cannot resolve it itself: a Jackson {@code ValueDeserializer} sees only its own token and has no
 * access to the sibling {@code currency} field. So the deserializer records WHICH branch arrived,
 * and {@link #resolveMajor(String)} converts once the currency is known and validated.
 *
 * <p>P1-69 closure: before this type, {@code MoneyDeserializer} mapped the integer token straight to
 * {@code BigDecimal.valueOf(long)}, i.e. it read a MINOR-unit integer as a MAJOR-unit decimal — a
 * contract-compliant USD request for $10.99 ({@code "amount":1099}) was charged $1099.00, a silent
 * 100× overcharge. Zero-decimal currencies (KRW/JPY) coincidentally agreed, which is why the
 * defect survived: every integration test used KRW.
 *
 * <p>The minor→major conversion goes through the canonical {@code common/Money} seam
 * ({@code Money.toMajorUnits(minor, currency)}), never a single-arg {@code BigDecimal.valueOf} —
 * the exact anti-pattern {@code practices/evals/money_boundary_seam_guard.sh} blocks.
 */
public final class MoneyWire {

    /** Integer JSON token branch: the value in MINOR units. Null when the string branch was taken. */
    private final Long minorUnits;

    /** Decimal-string branch: the value already in MAJOR units. Null when the integer branch was taken. */
    private final BigDecimal majorUnits;

    private MoneyWire(Long minorUnits, BigDecimal majorUnits) {
        this.minorUnits = minorUnits;
        this.majorUnits = majorUnits;
    }

    /** Integer JSON token: {@code "amount":1099} — 1099 MINOR units (USD $10.99, KRW ₩1099). */
    public static MoneyWire ofMinor(long minorUnits) {
        return new MoneyWire(minorUnits, null);
    }

    /** Decimal-string JSON token: {@code "amount":"10.99"} — already MAJOR units. */
    public static MoneyWire ofMajor(BigDecimal majorUnits) {
        return new MoneyWire(null, Objects.requireNonNull(majorUnits, "majorUnits"));
    }

    /**
     * Resolve this wire shape to the MAJOR-unit {@link BigDecimal} the payment/PG edge works in.
     *
     * <p>The integer branch is scaled by the currency's ISO-4217 minor-unit count through the
     * {@code common/Money} seam; the decimal-string branch is already major and is returned as-is
     * (its scale is validated separately by {@code PaymentService}'s scale check).
     *
     * <p>MUST be called only AFTER the currency has been validated — {@code Money.fractionDigits}
     * delegates to {@code Currency.getInstance}, which throws {@link IllegalArgumentException} (a
     * 500, not a 400) for a non-ISO code.
     */
    public BigDecimal resolveMajor(String currency) {
        return minorUnits == null ? majorUnits : Money.toMajorUnits(minorUnits, currency);
    }
}
