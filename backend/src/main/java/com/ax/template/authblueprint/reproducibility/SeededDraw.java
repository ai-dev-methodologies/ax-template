package com.ax.template.authblueprint.reproducibility;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * reproducible-procedure-l0 deterministic draw (PROC-DRAW-001 / PROC-REPLAY-001). A draw of k of n
 * candidates is computed deterministically from (canonically-sorted candidate list, k, recorded
 * seed) via a partial Fisher-Yates shuffle over a {@link Random} seeded with the recorded long.
 *
 * <p>This is the reproducibility seam: the recorded seed is the COMPLETE basis to reconstruct the
 * selection — no wall-clock, no fresh entropy. {@link Random} is deterministic given its seed
 * (NIST SP 800-90A: a DRBG reproduces its sequence from a recorded seed), so {@link #select} called
 * twice with the same arguments yields the byte-identical result. The reference PRNG is a
 * fork-receiver swap behind the recorded-seed-replays governance contract.
 */
final class SeededDraw {

    /** The recorded algorithm identifier — pinned per draw so a fork-receiver swap is auditable. */
    static final String ALGORITHM = "FISHER_YATES_JAVA_RANDOM_V1";

    private SeededDraw() {}

    /**
     * Select {@code k} of {@code sortedCandidates} deterministically from {@code seed}. The input
     * MUST already be canonically sorted by the caller so the selection is stable. Returns the
     * picked ids in selection order. {@code k} is clamped to the candidate count.
     */
    static List<String> select(List<String> sortedCandidates, int k, long seed) {
        List<String> pool = new ArrayList<>(sortedCandidates);
        int take = Math.min(Math.max(k, 0), pool.size());
        Random rng = new Random(seed);                          // deterministic given the recorded seed
        List<String> picked = new ArrayList<>(take);
        for (int i = 0; i < take; i++) {
            int j = i + rng.nextInt(pool.size() - i);           // partial Fisher-Yates
            String tmp = pool.get(i);
            pool.set(i, pool.get(j));
            pool.set(j, tmp);
            picked.add(pool.get(i));
        }
        return picked;
    }
}
