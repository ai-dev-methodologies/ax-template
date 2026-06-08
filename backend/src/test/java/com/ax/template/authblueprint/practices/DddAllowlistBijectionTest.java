package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Allowlist BIJECTION proof (ralplan re-verification CM1 — Critic must-fix #1).
 *
 * <p>Principle 3: every allowlist exception / governed entry must be LOAD-BEARING — it
 * must suppress an exact violation the guard would otherwise fire on, and there must be no
 * unguarded violation. This test runs each DDD predicate against an EMPTY allowlist on the
 * real tree, then asserts the full violation set equals the live allowlist EXACTLY (set
 * equality, not sampling):
 *
 * <ul>
 *   <li>every allowlist entry is matched by a real current violation (no stale/over-broad entry);</li>
 *   <li>every current violation is allowlisted (no hole).</li>
 * </ul>
 *
 * If anyone adds an allowlist entry that no longer (or never) corresponds to a live
 * violation, or removes a real violation's entry, this test fails.
 */
@Tag("PRACTICES")
@Tag("PRACTICES-DDD-BIJECTION")
class DddAllowlistBijectionTest {

    private static final JavaClasses CLASSES = DddRules.authblueprint();

    private static final Path SRC_ROOT =
            Path.of(System.getProperty("user.dir"), "src", "main", "java").normalize();
    private static final Path ALLOWLIST = Path.of(System.getProperty("user.dir"), "..",
            "practices", "evals", "aggregate_boundary_allowlist.yaml").normalize();

    private static final DddRules.Allowlist EMPTY =
            new DddRules.Allowlist(Set.of(), Set.of(), Set.of());

    /** "FROM -> TO (suffix)" -> "FROM->TO" (allowlist pair form). */
    private static String pairOf(String violation) {
        String[] parts = violation.split(" -> ", 2);
        String from = parts[0].trim();
        String to = parts[1].trim().split(" ", 2)[0]; // drop any trailing " (suffix)"
        return from + "->" + to;
    }

    @Test
    @Tag("PRACTICES-DDD-BIJECTION")
    void everyExceptionPairIsLoadBearingAndNoHole() {
        DddRules.Allowlist live = DddRules.loadAllowlist(ALLOWLIST);

        Set<String> rawViolations = new TreeSet<>();
        rawViolations.addAll(DddRules.featIsolation(CLASSES, EMPTY));
        rawViolations.addAll(DddRules.aggRef(CLASSES, EMPTY));
        rawViolations.addAll(DddRules.memberRepo(CLASSES, SRC_ROOT, EMPTY));

        Set<String> violationPairs = new TreeSet<>();
        rawViolations.forEach(v -> violationPairs.add(pairOf(v)));

        // BIJECTION: the set of real entity/repo/ref violations == the set of allowlist exception pairs.
        assertThat(violationPairs)
                .as("Allowlist exception pairs must be exactly the set of real cross-feature/cross-aggregate/"
                        + "member-repo violations (no stale/over-broad entry, no unguarded hole).")
                .isEqualTo(new TreeSet<>(live.pairs()));
    }

    @Test
    @Tag("PRACTICES-DDD-BIJECTION")
    void everyGovernedGodServiceIsLoadBearing() {
        DddRules.Allowlist live = DddRules.loadAllowlist(ALLOWLIST);
        Set<String> keys = new TreeSet<>();
        DddRules.godService(CLASSES, SRC_ROOT, EMPTY)
                .forEach(v -> keys.add(v.split(" directly mutates", 2)[0].trim()));
        assertThat(keys)
                .as("governed_god_service must be exactly the set of real god-service @Transactional methods.")
                .isEqualTo(new TreeSet<>(live.godService()));
    }

    @Test
    @Tag("PRACTICES-DDD-BIJECTION")
    void everyGovernedStateMutatorIsLoadBearing() {
        DddRules.Allowlist live = DddRules.loadAllowlist(ALLOWLIST);
        Set<String> keys = new TreeSet<>(DddRules.stateMutator(CLASSES, EMPTY));
        assertThat(keys)
                .as("governed_state_mutators must be exactly the set of real direct state-mutator callers.")
                .isEqualTo(new TreeSet<>(live.stateMutators()));
    }
}
