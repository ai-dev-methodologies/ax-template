package com.ax.template.authblueprint.common;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Aggregate result of a best-effort bulk operation — ships REAL reusable code for
 * the {@code bulk-operation-l0} catalog spec (specs/bulk-operation-l0.yaml),
 * BULK-PARTIAL-001 (the 207-style per-item result array) and the
 * {@code best_effort} branch of BULK-ATOMICITY-001.
 *
 * <p>The IDW2 dogfood found all three personas hand-rolled this same aggregate —
 * a total/succeeded/failed tally plus a per-item array — and each recomputed the
 * counts and the 200-vs-207 decision inline, an obvious copy-paste source. This
 * type models that RESULT once: the counts are derived (never trusted from the
 * caller) and the HTTP-status decision is centralised so no endpoint can collapse
 * a mixed batch into a single 200 (which BULK-PARTIAL-001 forbids).
 *
 * <h2>What this type models — and what stays in the caller</h2>
 * This is the <em>result</em> model only. The size/empty checks that BULK-SUBMIT-001
 * mandates stay in the caller (controller / argument resolver), because they must
 * run BEFORE any item is touched and they produce transport-level 4xx, not a
 * per-item outcome:
 * <ul>
 *   <li><b>empty batch</b> → the caller returns <b>400</b> with {@code detail}
 *       "items must not be empty" (do NOT build a {@code BulkResult} for it).</li>
 *   <li><b>oversize batch</b> (count &gt; {@code bulk_max_batch_size}) → the caller
 *       returns <b>413</b> Content Too Large before persisting anything.</li>
 * </ul>
 * Once the batch is admitted, this type carries the per-item outcomes and decides
 * the aggregate status via {@link #httpStatus()}:
 * <ul>
 *   <li>every item ok → <b>200</b> OK,</li>
 *   <li>any item failed (mixed, or all-failed) → <b>207</b> Multi-Status. A batch
 *       where every item fails STILL returns 207, because the request itself was
 *       processed (BULK-PARTIAL-001).</li>
 * </ul>
 *
 * <h2>Atomicity scope (the bug this type's contract exists to prevent)</h2>
 * {@code BulkResult} models the {@code best_effort} mode: each item commits or
 * fails independently. {@link #collect(List, Function)} runs each op and traps a
 * per-item failure into a {@link BulkItemError} instead of aborting the batch —
 * but for that isolation to be REAL, the caller MUST run each {@code op} in its
 * own transaction (Spring {@code @Transactional(propagation = REQUIRES_NEW)} on
 * the per-item service method). Otherwise a single rolled-back item marks the
 * shared transaction rollback-only and silently discards the siblings this type
 * reports as "succeeded" — the exact partial-success bug BULK-ATOMICITY-001 calls
 * out. For {@code all_or_nothing} endpoints do NOT use this type: run the whole
 * batch in one {@code @Transactional} method and let the first failure roll back.
 *
 * <p>Framework-clean and generic over the success-payload type {@code R}: no
 * Spring, no JPA imports, so the result aggregation is unit-testable without a
 * web or persistence context. The {@code items} list is defensively copied to an
 * unmodifiable list so the record stays immutable.
 *
 * @param <R>       the per-item success-payload type
 * @param total     number of items in the batch (== {@code items.size()})
 * @param succeeded count of items whose {@code status} is {@code "ok"}
 * @param failed    count of items whose {@code status} is {@code "error"}
 * @param items     per-item results, in input order
 */
public record BulkResult<R>(int total, int succeeded, int failed, List<BulkItemResult<R>> items) {

    public BulkResult {
        items = (items == null) ? List.of() : List.copyOf(items);
    }

    /**
     * Build a {@code BulkResult} from the per-item outcomes, computing
     * {@code total}/{@code succeeded}/{@code failed} from the list so the counts
     * can never drift from the entries (BULK-PARTIAL-001).
     *
     * @param items the per-item results, in input order (must be non-null)
     * @return an aggregate with derived counts
     */
    public static <R> BulkResult<R> partial(List<BulkItemResult<R>> items) {
        if (items == null) {
            throw new IllegalArgumentException("BulkResult.partial requires a non-null items list");
        }
        int succeeded = 0;
        for (BulkItemResult<R> item : items) {
            if (item.succeeded()) {
                succeeded++;
            }
        }
        return new BulkResult<>(items.size(), succeeded, items.size() - succeeded, items);
    }

    /**
     * Decide the aggregate HTTP status for this best-effort result:
     * <ul>
     *   <li><b>200</b> when every item succeeded (and the batch was non-empty),</li>
     *   <li><b>207</b> Multi-Status when any item failed (mixed or all-failed).</li>
     * </ul>
     * Returns the bare status code as an {@code int} to stay framework-clean;
     * controllers map it to their HTTP layer (e.g. {@code HttpStatus.valueOf(code)}).
     *
     * <p>An empty batch should never reach this type (the caller returns 400 per
     * BULK-SUBMIT-001); defensively, an empty result reports 200.
     *
     * @return {@code 200} for all-ok, {@code 207} for any failure
     */
    public int httpStatus() {
        return failed == 0 ? STATUS_OK : STATUS_MULTI;
    }

    /** HTTP 200 OK — every item in the batch succeeded. */
    public static final int STATUS_OK = 200;

    /** HTTP 207 Multi-Status (RFC 4918 §11.1) — mixed or all-failed per-item outcomes. */
    public static final int STATUS_MULTI = 207;

    /**
     * Run {@code op} against each input independently, trapping a per-item failure
     * into a {@link BulkItemError} so one bad item does not abort the batch, and
     * aggregate the outcomes into a {@code BulkResult} (best-effort mode,
     * BULK-ATOMICITY-001).
     *
     * <p><b>Transaction requirement (REQUIRES_NEW):</b> when {@code op} performs a
     * persistence side effect, the caller MUST ensure each invocation runs in its
     * own transaction — typically by making {@code op} a call to a Spring service
     * method annotated {@code @Transactional(propagation = Propagation.REQUIRES_NEW)}.
     * Without that, a JPA exception on one item marks the surrounding transaction
     * rollback-only and the "succeeded" siblings are silently discarded on commit.
     * This helper traps the throwable but CANNOT undo a poisoned shared
     * transaction — the isolation must come from the propagation boundary.
     *
     * <p>The {@code message} of a trapped error is the throwable's message (or its
     * simple class name when the message is null); the {@code code} is the
     * throwable's simple class name. Callers wanting domain-specific codes should
     * map exceptions to {@link BulkItemError} themselves and use
     * {@link #partial(List)} directly.
     *
     * @param inputs the batch inputs, in order (must be non-null)
     * @param op     the per-item operation; its return value becomes the success
     *               payload, a thrown exception becomes a per-item error
     * @param <I>    the input item type
     * @param <R>    the success-payload type
     * @return the aggregated best-effort result
     */
    public static <I, R> BulkResult<R> collect(List<I> inputs, Function<I, R> op) {
        if (inputs == null) {
            throw new IllegalArgumentException("BulkResult.collect requires a non-null inputs list");
        }
        if (op == null) {
            throw new IllegalArgumentException("BulkResult.collect requires a non-null op");
        }
        List<BulkItemResult<R>> results = new ArrayList<>(inputs.size());
        for (int i = 0; i < inputs.size(); i++) {
            try {
                R value = op.apply(inputs.get(i));
                results.add(BulkItemResult.ok(i, value));
            } catch (RuntimeException ex) {
                String message = (ex.getMessage() != null)
                    ? ex.getMessage()
                    : ex.getClass().getSimpleName();
                results.add(BulkItemResult.error(i, new BulkItemError(ex.getClass().getSimpleName(), message)));
            }
        }
        return partial(results);
    }
}
