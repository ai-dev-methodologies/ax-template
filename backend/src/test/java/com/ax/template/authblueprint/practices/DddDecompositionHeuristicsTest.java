package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * TIER-1 DDD heuristic hard guards (spec §6, shallow heuristic with explicit honest limits).
 * Thin caller over {@link DddRules}.
 *
 * <ul>
 *   <li>HG-ANTI-GODSERVICE-TX (PRACTICES-DDD-008) — a @Transactional method directly mutating
 *       (save/delete) >=2 distinct @AggregateRoot entities. Honest limit: helper-delegated
 *       mutation is NOT caught (TIER-2 review). Members do not count. Governed: governed_god_service.</li>
 *   <li>HG-STATE-SOLE-MUTATOR (PRACTICES-DDD-009) — for entity X with a matching XStateMachine,
 *       setStatus/setState must be called only by that machine (+ the entity). The name-match
 *       avoids the Product public-setter overblock. Governed: governed_state_mutators.</li>
 * </ul>
 */
@Tag("PRACTICES")
class DddDecompositionHeuristicsTest {

    private static final JavaClasses CLASSES = DddRules.authblueprint();

    private static final Path SRC_ROOT =
            Path.of(System.getProperty("user.dir"), "src", "main", "java").normalize();
    private static final Path ALLOWLIST = Path.of(System.getProperty("user.dir"), "..",
            "practices", "evals", "aggregate_boundary_allowlist.yaml").normalize();

    @Test
    @Tag("PRACTICES-DDD-008")
    void practices_DDD_008_noGodServiceTransaction() {
        assertThat(DddRules.godService(CLASSES, SRC_ROOT, DddRules.loadAllowlist(ALLOWLIST)))
                .as("HG-ANTI-GODSERVICE-TX: a @Transactional method must not directly mutate >=2 distinct "
                        + "aggregate roots — one aggregate per transaction; coordinate the rest via published "
                        + "services/events, or record the method in governed_god_service. (Helper-delegated "
                        + "mutation is a documented limit → TIER-2 review.)")
                .isEmpty();
    }

    @Test
    @Tag("PRACTICES-DDD-009")
    void practices_DDD_009_stateMutatorRoutedThroughStateMachine() {
        assertThat(DddRules.stateMutator(CLASSES, DddRules.loadAllowlist(ALLOWLIST)))
                .as("HG-STATE-SOLE-MUTATOR: a state-machine-governed entity's setStatus/setState must be "
                        + "called only by its <Entity>StateMachine — route the transition through the machine, "
                        + "or record the caller in governed_state_mutators.")
                .isEmpty();
    }
}
