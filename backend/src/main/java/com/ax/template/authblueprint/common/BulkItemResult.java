package com.ax.template.authblueprint.common;

/**
 * One entry in a bulk-operation per-item result array — ships REAL reusable code
 * for the {@code bulk-operation-l0} catalog spec (specs/bulk-operation-l0.yaml),
 * BULK-PARTIAL-001 ("a {@code results} array preserving input order, one entry
 * per submitted item, each carrying {@code index} … {@code status} … and on
 * failure an embedded RFC 9457 {@code error} object").
 *
 * <p>The IDW2 dogfood proved all three personas hand-rolled this exact per-item
 * report and drifted on the field set. This record fixes the canonical shape:
 * <ul>
 *   <li>{@code index} — the item's position in the input batch (order-preserving,
 *       so the client can correlate the result back to the submitted item).</li>
 *   <li>{@code status} — {@code "ok"} or {@code "error"}. A coarse two-state
 *       discriminant; the precise per-item HTTP status (201/200/409/422…) and the
 *       RFC 9457 {@code type} live in {@link #error} / are derived by the caller.</li>
 *   <li>{@code result} — the success payload (nullable; populated only when
 *       {@code status == "ok"}).</li>
 *   <li>{@code error} — the {@link BulkItemError} (nullable; populated only when
 *       {@code status == "error"}).</li>
 * </ul>
 *
 * <p>Construct via {@link #ok(int, Object)} / {@link #error(int, BulkItemError)}
 * rather than the canonical constructor so the {@code status} string and the
 * {@code result}/{@code error} nullability stay consistent (an "ok" with a
 * non-null error, or vice-versa, is a caller bug). The compact constructor
 * enforces that invariant defensively.
 *
 * <p>Framework-clean and generic over the success payload type {@code R}.
 *
 * @param <R>    the success-payload type (e.g. a created-resource id or DTO)
 * @param index  the item's zero-based position in the input batch
 * @param status {@code "ok"} or {@code "error"}
 * @param result the success payload, or {@code null} on error
 * @param error  the per-item error, or {@code null} on success
 */
public record BulkItemResult<R>(int index, String status, R result, BulkItemError error) {

    /** Canonical per-item success status token. */
    public static final String STATUS_OK = "ok";

    /** Canonical per-item failure status token. */
    public static final String STATUS_ERROR = "error";

    public BulkItemResult {
        if (index < 0) {
            throw new IllegalArgumentException("BulkItemResult.index must be >= 0");
        }
        if (!STATUS_OK.equals(status) && !STATUS_ERROR.equals(status)) {
            throw new IllegalArgumentException(
                "BulkItemResult.status must be \"ok\" or \"error\", was: " + status);
        }
        if (STATUS_OK.equals(status) && error != null) {
            throw new IllegalArgumentException("an \"ok\" result must not carry an error");
        }
        if (STATUS_ERROR.equals(status) && error == null) {
            throw new IllegalArgumentException("an \"error\" result must carry a BulkItemError");
        }
    }

    /** Build a success entry for the item at {@code index}. */
    public static <R> BulkItemResult<R> ok(int index, R result) {
        return new BulkItemResult<>(index, STATUS_OK, result, null);
    }

    /** Build a failure entry for the item at {@code index}. */
    public static <R> BulkItemResult<R> error(int index, BulkItemError error) {
        return new BulkItemResult<>(index, STATUS_ERROR, null, error);
    }

    /** @return {@code true} when this item succeeded. */
    public boolean succeeded() {
        return STATUS_OK.equals(status);
    }
}
