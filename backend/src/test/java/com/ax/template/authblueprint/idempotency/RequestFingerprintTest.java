package com.ax.template.authblueprint.idempotency;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit proof that {@link RequestFingerprint}'s hand-built standalone mapper pins the FULL Jackson 3
 * {@link tools.jackson.core.StreamReadConstraints} envelope — specifically the {@code maxTokenCount}
 * bound (Jackson 3 default is -1 = UNBOUNDED). Without the bound, a sub-20MB token-dense body would
 * canonicalize into millions of maps → heap exhaustion during fingerprinting.
 *
 * <p>The canonicalizer swallows any parse failure and falls back to hashing the RAW body, so the
 * bound tripping is observed BEHAVIORALLY: canonicalization normalizes JSON key order (order-INsensitive
 * fingerprint) ONLY when the parse succeeds. For a token-dense body over the bound the parse aborts and
 * the raw (order-SENSITIVE) body is hashed instead — so two dense bodies that differ ONLY in key order
 * yield DIFFERENT fingerprints. If the bound were removed, both would canonicalize identically and the
 * fingerprints would collide; this test would then fail. Runs in tens of ms (parse aborts at the bound).
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
    void tokenDenseBodyAbortsCanonicalizationAndFallsBackToRawHash() {
        // Over the 1M token bound: the parse aborts (StreamConstraintsException) → the canonicalizer
        // falls back to hashing the RAW body, which is order-SENSITIVE. So the same-semantics /
        // different-key-order pair yields DIFFERENT fingerprints. Without maxTokenCount bounded, both
        // would canonicalize to the identical sorted-key form and collide — proving the bound is active.
        long start = System.nanoTime();
        String fpDenseB = RequestFingerprint.of("POST", "/api/x", null, dense(true));
        String fpDenseA = RequestFingerprint.of("POST", "/api/x", null, dense(false));
        long ms = (System.nanoTime() - start) / 1_000_000;

        assertThat(fpDenseB)
            .as("token-dense reorder does NOT canonicalize (parse aborted at the token bound → raw hash)")
            .isNotEqualTo(fpDenseA);
        assertThat(ms)
            .as("bound aborts the parse fast — no full materialization of millions of maps")
            .isLessThan(5_000);
    }
}
