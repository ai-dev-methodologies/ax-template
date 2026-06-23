package com.ax.template.authblueprint.orderquantization;

/**
 * order-multiple-quantization-l0 pure quantizer (ORDERQUANT-QUANTIZE-001 / ORDERQUANT-IDEMPOTENT-001).
 *
 * <p>The single deterministic function the whole domain turns on:
 * {@code orderQuantity = max(MOQ, ceil(required / multiple) * multiple)} in exact long arithmetic.
 * It rounds a required net quantity UP to the next whole order multiple (the lot / pack / case
 * size), then floors the result at the minimum order quantity (MOQ). The result is the SMALLEST
 * placeable quantity that is at least {@code required}, at least {@code moq}, and an exact integer
 * multiple of {@code multiple}.
 *
 * <p>This is NON-CONSERVING by design (ORDERQUANT-OVERAGE-001): {@code orderQuantity >= required}
 * and the surplus {@code overage = orderQuantity - required} is real — the procurement cost of a
 * lot constraint that cannot be subdivided. That is the OPPOSITE of a conserving rounded-split,
 * whose parts sum back to the whole; the rounded-split creates nothing, this deliberately does.
 *
 * <p>A pure function of its three inputs only — no clock, no sequence, no accumulator — so the
 * same {@code (required, moq, multiple)} always yields the same {@code orderQuantity}
 * (ORDERQUANT-IDEMPOTENT-001). Callers MUST have already rejected a non-positive {@code multiple}
 * (ORDERQUANT-CONSTRAINT-001) so {@code ceilDiv} never divides by zero.
 */
final class Quantizer {

    private Quantizer() {}

    /** orderQuantity = max(moq, ceil(required / multiple) * multiple). Inputs assumed validated. */
    static long quantize(long required, long moq, long multiple) {
        long roundedUp = ceilDiv(required, multiple) * multiple;
        return Math.max(moq, roundedUp);
    }

    /** Ceiling of {@code required / multiple} for non-negative {@code required} and positive {@code multiple}. */
    private static long ceilDiv(long required, long multiple) {
        return (required + multiple - 1) / multiple;
    }
}
