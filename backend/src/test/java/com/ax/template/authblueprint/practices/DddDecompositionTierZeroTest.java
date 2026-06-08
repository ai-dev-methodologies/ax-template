package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * TIER-0 DDD package-structure hard guards — block mode, evaluated against the whole
 * authblueprint tree. Spec: docs/superpowers/specs/2026-06-08-ddd-decomposition-rules-design.md §6.
 *
 * <p>Thin caller: the predicate logic lives in {@link DddRules} (shared with the
 * non-vacuity fixture proof so the proof exercises real code — ralplan CM2).
 *
 * <ul>
 *   <li>HG-FEAT-TOPLEVEL-TECH (PRACTICES-DDD-001) — no by-layer/by-endpoint package under base.</li>
 *   <li>HG-FEAT-NOCYCLE — kept as {@code ArchitectureNoCyclicPackageTest} (PRACTICES-TEST-003).</li>
 *   <li>HG-KERNEL-NO-FEATURE-DEP (PRACTICES-DDD-002) — kernel must not depend on a leaf feature.</li>
 *   <li>HG-FEAT-ISOLATION (PRACTICES-DDD-003) — no cross-feature @Entity/*Repository (allowlist-aware).</li>
 *   <li>HG-ANTI-SPLIT-ENDPOINT (PRACTICES-DDD-004) — no verb-prefixed controller (loose lexical subset).</li>
 * </ul>
 */
@Tag("PRACTICES")
class DddDecompositionTierZeroTest {

    private static final JavaClasses CLASSES = DddRules.authblueprint();

    private static final Path ALLOWLIST = Path.of(System.getProperty("user.dir"), "..",
            "practices", "evals", "aggregate_boundary_allowlist.yaml").normalize();

    @Test
    @Tag("PRACTICES-DDD-001")
    void practices_DDD_001_noByLayerPackageDirectlyUnderBase() {
        assertThat(DddRules.topLevelTech(CLASSES))
                .as("HG-FEAT-TOPLEVEL-TECH: by-layer/by-endpoint packages %s must not sit directly under "
                        + "the base package — organize by feature (vertical slice), not technical layer.",
                        DddRules.BANNED_TOPLEVEL)
                .isEmpty();
    }

    @Test
    @Tag("PRACTICES-DDD-002")
    void practices_DDD_002_kernelDoesNotDependOnAnyFeature() {
        assertThat(DddRules.kernelFeatureDep(CLASSES))
                .as("HG-KERNEL-NO-FEATURE-DEP: the kernel %s must not depend on any leaf feature.",
                        DddRules.KERNEL)
                .isEmpty();
    }

    @Test
    @Tag("PRACTICES-DDD-003")
    void practices_DDD_003_noCrossFeatureEntityOrRepositoryReference() {
        assertThat(DddRules.featIsolation(CLASSES, DddRules.loadAllowlist(ALLOWLIST)))
                .as("HG-FEAT-ISOLATION: a feature must not reference another feature's @Entity/*Repository. "
                        + "Expose a published service (by id), use the kernel, or record a grandfather/"
                        + "composition exception in practices/evals/aggregate_boundary_allowlist.yaml.")
                .isEmpty();
    }

    @Test
    @Tag("PRACTICES-DDD-004")
    void practices_DDD_004_noVerbPrefixedController() {
        assertThat(DddRules.antiSplitEndpoint(CLASSES))
                .as("HG-ANTI-SPLIT-ENDPOINT: controllers must be resource-oriented (OrderController), not "
                        + "endpoint-per-verb (Create../List../Get..Controller). Cohesion adequacy is TIER-2 review.")
                .isEmpty();
    }
}
