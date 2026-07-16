package com.ax.template.authblueprint.idempotency;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit proof that {@link RequestFingerprint}'s hand-built standalone mapper pins the FULL Jackson 3
 * {@link tools.jackson.core.StreamReadConstraints} envelope — specifically the {@code maxTokenCount}
 * bound (Jackson 3 default is -1 = UNBOUNDED). Without the bound, a sub-20MB token-dense body would
 * canonicalize into millions of maps → heap exhaustion during fingerprinting.
 *
 * <p>The bound tripping is observed by REJECTION: a body over the streaming constraints is abusive,
 * so the canonicalizer propagates a {@link RequestBodyConstraintViolationException} instead of
 * silently degrading to an order-sensitive raw hash (which would give a same-key reorder retry a
 * false 422 instead of a replay). The positive control below proves canonicalization is live for a
 * legitimate under-bound body. Runs in tens of ms (parse aborts at the bound).
 */
@Tag("IDEMPOTENCY")
class RequestFingerprintTest {

    /** ~250k keyed objects (~5MB, well over the 1M token bound) — key order flipped between the two. */
    private static String dense(boolean bFirst) {
        StringBuilder sb = new StringBuilder("[");
        String obj = bFirst ? "{\"bbbb\":1,\"aaaa\":2}" : "{\"aaaa\":2,\"bbbb\":1}";
        for (int i = 0; i < 250_000; i++) {
            sb.append(obj).append(',');
        }
        return sb.append(obj).append(']').toString();
    }

    @Test
    void smallReorderCanonicalizesToSameFingerprint() {
        // Positive control (non-vacuity): for an under-bound body the mapper DOES canonicalize, so a
        // pure key-order reorder produces the SAME fingerprint. This proves canonicalization is live —
        // the dense divergence below is the bound tripping, not a broken mapper.
        String fp1 = RequestFingerprint.of("POST", "/api/x", null, "{\"bbbb\":1,\"aaaa\":2}");
        String fp2 = RequestFingerprint.of("POST", "/api/x", null, "{\"aaaa\":2,\"bbbb\":1}");
        assertThat(fp1).as("under-bound key reorder canonicalizes identically").isEqualTo(fp2);
    }

    @Test
    void tokenDenseBodyRejectedNotDegradedToRawHash() {
        // Over the 1M token bound: the parse aborts (StreamConstraintsException). The canonicalizer
        // must REJECT (propagate RequestBodyConstraintViolationException) rather than silently fall
        // back to an order-SENSITIVE raw hash — a degraded fingerprint would give a same-key reorder
        // retry a false 422 instead of a replay. If maxTokenCount were removed, the body would
        // materialize into millions of maps (heap risk) instead of aborting; this bound prevents that.
        long start = System.nanoTime();
        assertThatThrownBy(() -> RequestFingerprint.of("POST", "/api/x", null, dense(true)))
            .as("over-constraint body is rejected, not accepted with a degraded raw hash")
            .isInstanceOf(RequestBodyConstraintViolationException.class);
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertThat(ms)
            .as("bound aborts the parse fast — no full materialization of millions of maps")
            .isLessThan(5_000);
    }
}
