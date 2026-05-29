package com.ax.template.authblueprint.common;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit coverage for {@link BulkResult} / {@link BulkItemResult} / {@link BulkItemError}
 * — closes the prose-only gap of the {@code bulk-operation-l0} spec
 * (specs/bulk-operation-l0.yaml) that IDW2 proved all three personas hand-rolled.
 *
 * <p>Pins the contract surfaces a fork-receiver relies on: derived counts +
 * order-preserving per-item array (BULK-PARTIAL-001), the 200-vs-207 aggregate
 * decision, and the best-effort {@code collect} that traps a per-item failure
 * without aborting the batch (BULK-ATOMICITY-001 best_effort branch).
 * Framework-clean: no Spring context, runs under the default {@code test} task.
 */
@Tag("COMMON_BULK_RESULT")
class BulkResultTest {

    // ─── all-ok → 200, every item succeeded (BULK-PARTIAL-001) ────────────

    @Test
    void allOk_reports200AndAllSucceeded() {
        BulkResult<String> result = BulkResult.partial(List.of(
            BulkItemResult.ok(0, "a"),
            BulkItemResult.ok(1, "b"),
            BulkItemResult.ok(2, "c")));

        assertThat(result.total()).isEqualTo(3);
        assertThat(result.succeeded()).isEqualTo(3);
        assertThat(result.failed()).isZero();
        assertThat(result.httpStatus()).isEqualTo(BulkResult.STATUS_OK);
        assertThat(result.httpStatus()).isEqualTo(200);
    }

    @Test
    void collect_allOk_returns200WithEachPayload() {
        BulkResult<Integer> result = BulkResult.collect(List.of(1, 2, 3), n -> n * 10);

        assertThat(result.httpStatus()).isEqualTo(200);
        assertThat(result.succeeded()).isEqualTo(3);
        assertThat(result.items())
            .extracting(BulkItemResult::result)
            .containsExactly(10, 20, 30);
    }

    // ─── mixed → 207 with correct index/error, others ok (BULK-PARTIAL-001) ─

    @Test
    void oneFailingItem_reports207WithCorrectIndexAndError() {
        // item at index 1 fails; 0 and 2 succeed
        BulkResult<Integer> result = BulkResult.collect(List.of(2, 0, 5), n -> 100 / n);

        assertThat(result.httpStatus()).isEqualTo(BulkResult.STATUS_MULTI);
        assertThat(result.httpStatus()).isEqualTo(207);
        assertThat(result.total()).isEqualTo(3);
        assertThat(result.succeeded()).isEqualTo(2);
        assertThat(result.failed()).isEqualTo(1);

        BulkItemResult<Integer> ok0 = result.items().get(0);
        assertThat(ok0.index()).isEqualTo(0);
        assertThat(ok0.status()).isEqualTo("ok");
        assertThat(ok0.result()).isEqualTo(50);
        assertThat(ok0.error()).isNull();

        BulkItemResult<Integer> failed1 = result.items().get(1);
        assertThat(failed1.index()).isEqualTo(1);
        assertThat(failed1.status()).isEqualTo("error");
        assertThat(failed1.result()).isNull();
        assertThat(failed1.error()).isNotNull();
        assertThat(failed1.error().code()).isEqualTo("ArithmeticException");
        assertThat(failed1.error().message()).contains("by zero");

        BulkItemResult<Integer> ok2 = result.items().get(2);
        assertThat(ok2.index()).isEqualTo(2);
        assertThat(ok2.result()).isEqualTo(20);
    }

    @Test
    void allFailing_stillReports207NotASingle4xx() {
        // BULK-PARTIAL-001: a batch where every item fails STILL returns 207
        BulkResult<Integer> result = BulkResult.collect(List.of(0, 0), n -> 1 / n);

        assertThat(result.httpStatus()).isEqualTo(207);
        assertThat(result.succeeded()).isZero();
        assertThat(result.failed()).isEqualTo(2);
    }

    @Test
    void partial_preservesInputOrder() {
        BulkResult<String> result = BulkResult.partial(List.of(
            BulkItemResult.error(0, new BulkItemError("E1", "first failed")),
            BulkItemResult.ok(1, "second"),
            BulkItemResult.error(2, new BulkItemError("E3", "third failed"))));

        assertThat(result.items())
            .extracting(BulkItemResult::index)
            .containsExactly(0, 1, 2);
        assertThat(result.httpStatus()).isEqualTo(207);
        assertThat(result.succeeded()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(2);
    }

    // ─── derived counts are trustworthy, items list is immutable ──────────

    @Test
    void partial_derivesCountsFromList() {
        BulkResult<String> result = BulkResult.partial(List.of(
            BulkItemResult.ok(0, "x"),
            BulkItemResult.error(1, new BulkItemError("BAD", "nope"))));

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.succeeded()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
    }

    @Test
    void items_areDefensivelyCopiedAndUnmodifiable() {
        BulkResult<String> result = BulkResult.partial(List.of(BulkItemResult.ok(0, "x")));
        assertThatThrownBy(() -> result.items().add(BulkItemResult.ok(1, "y")))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    // ─── collect null-message exception falls back to class name ──────────

    @Test
    void collect_nullMessageException_usesClassNameForMessage() {
        BulkResult<String> result = BulkResult.collect(
            List.of("only"),
            s -> { throw new IllegalStateException(); });

        BulkItemError error = result.items().get(0).error();
        assertThat(error.code()).isEqualTo("IllegalStateException");
        assertThat(error.message()).isEqualTo("IllegalStateException");
    }

    // ─── factory + validation invariants ──────────────────────────────────

    @Test
    void bulkItemResult_factories_setStatusAndNullability() {
        BulkItemResult<String> ok = BulkItemResult.ok(0, "payload");
        assertThat(ok.status()).isEqualTo("ok");
        assertThat(ok.succeeded()).isTrue();
        assertThat(ok.error()).isNull();

        BulkItemResult<String> err = BulkItemResult.error(1, new BulkItemError("C", "m"));
        assertThat(err.status()).isEqualTo("error");
        assertThat(err.succeeded()).isFalse();
        assertThat(err.result()).isNull();
    }

    @Test
    void bulkItemResult_inconsistentStatusRejected() {
        // "ok" carrying an error, and "error" without one, are caller bugs
        assertThatThrownBy(() -> new BulkItemResult<>(0, "ok", "x", new BulkItemError("C", "m")))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BulkItemResult<>(0, "error", null, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BulkItemResult<>(0, "bogus", null, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BulkItemResult<>(-1, "ok", "x", null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bulkItemError_blankCodeRejected() {
        assertThatThrownBy(() -> new BulkItemError("  ", "m"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BulkItemError(null, "m"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void partial_rejectsNullList() {
        assertThatThrownBy(() -> BulkResult.partial(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void collect_rejectsNullArguments() {
        assertThatThrownBy(() -> BulkResult.collect(null, x -> x))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BulkResult.collect(List.of("a"), null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
