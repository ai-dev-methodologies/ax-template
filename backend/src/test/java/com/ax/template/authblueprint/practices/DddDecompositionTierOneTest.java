package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * TIER-1 DDD aggregate hard guards — marker-dependent, flipped to block AFTER the back-tag
 * wave (spec §4 / §10 Phase 2). Spec §6. Thin caller over {@link DddRules}.
 *
 * <ul>
 *   <li>HG-AGG-REPO (PRACTICES-DDD-005) — a @AggregateMember entity must not have its own repository.</li>
 *   <li>HG-AGG-REF (PRACTICES-DDD-006) — no cross-aggregate object pointer (reference by id).</li>
 *   <li>HG-AGG-MEMBER-ENCAP (PRACTICES-DDD-007) — a member must not be referenced outside its feature.</li>
 * </ul>
 */
@Tag("PRACTICES")
class DddDecompositionTierOneTest {

    private static final JavaClasses CLASSES = DddRules.authblueprint();

    private static final Path SRC_ROOT =
            Path.of(System.getProperty("user.dir"), "src", "main", "java").normalize();
    private static final Path ALLOWLIST = Path.of(System.getProperty("user.dir"), "..",
            "practices", "evals", "aggregate_boundary_allowlist.yaml").normalize();

    @Test
    @Tag("PRACTICES-DDD-005")
    void practices_DDD_005_memberEntityHasNoRepository() {
        assertThat(DddRules.memberRepo(CLASSES, SRC_ROOT, DddRules.loadAllowlist(ALLOWLIST)))
                .as("HG-AGG-REPO: a @AggregateMember entity must not have its own repository — mutate/load "
                        + "through its root, promote it to a root, or record a member-repo exception in "
                        + "practices/evals/aggregate_boundary_allowlist.yaml.")
                .isEmpty();
    }

    @Test
    @Tag("PRACTICES-DDD-006")
    void practices_DDD_006_noCrossAggregateObjectPointer() {
        assertThat(DddRules.aggRef(CLASSES, DddRules.loadAllowlist(ALLOWLIST)))
                .as("HG-AGG-REF: an entity must not hold an object pointer to another aggregate's root — "
                        + "reference by identity (id) instead, or record a grandfather exception in the allowlist.")
                .isEmpty();
    }

    @Test
    @Tag("PRACTICES-DDD-007")
    void practices_DDD_007_memberNotReferencedOutsideOwningFeature() {
        assertThat(DddRules.memberEncap(CLASSES))
                .as("HG-AGG-MEMBER-ENCAP: a @AggregateMember entity must not be referenced from outside its "
                        + "owning feature — expose the aggregate root, not its members.")
                .isEmpty();
    }
}
