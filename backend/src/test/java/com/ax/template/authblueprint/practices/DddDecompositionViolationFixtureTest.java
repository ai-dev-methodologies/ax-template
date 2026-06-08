package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Non-vacuity proof for the DDD guards (spec §11: "reference 위반 fixture로 증명"). Imports the
 * deliberately-violating fixtures under {@code ddd.fixtures} (outside {@code com.ax.template}
 * so the real guards / Spring component scan / Hibernate entity scan never see them) and runs
 * the SAME {@link DddRules} predicate code the production guards use (ralplan CM2/AM2 — the
 * proof exercises real code, not a copy). If a guard regressed to vacuous-green on the real
 * tree, these assertions fail.
 *
 * <p>The allowlist-aware guards (HG-FEAT-ISOLATION, HG-AGG-REPO, HG-ANTI-GODSERVICE-TX,
 * HG-STATE-SOLE-MUTATOR) are additionally proven load-bearing on the real tree by the
 * allowlist bijection check (ralplan CM1) and were each caught pre-grandfathering.
 */
@Tag("PRACTICES")
@Tag("PRACTICES-DDD-FIXTURE")
class DddDecompositionViolationFixtureTest {

    private static final JavaClasses FIX = new ClassFileImporter().importPackages("ddd.fixtures");
    private static final DddRules.Allowlist EMPTY =
            new DddRules.Allowlist(Set.of(), Set.of(), Set.of());

    @Test
    @Tag("PRACTICES-DDD-FIXTURE")
    void fixtureProvesZeroViolationGuardsAreNonVacuous() {
        assertThat(DddRules.topLevelTech(FIX))
                .as("HG-FEAT-TOPLEVEL-TECH must detect the by-layer package fixture").isNotEmpty();
        assertThat(DddRules.antiSplitEndpoint(FIX))
                .as("HG-ANTI-SPLIT-ENDPOINT must detect the verb-prefixed controller fixture").isNotEmpty();
        assertThat(DddRules.kernelFeatureDep(FIX))
                .as("HG-KERNEL-NO-FEATURE-DEP must detect the kernel->feature fixture").isNotEmpty();
        assertThat(DddRules.aggRef(FIX, EMPTY))
                .as("HG-AGG-REF must detect the cross-aggregate object pointer fixture").isNotEmpty();
        assertThat(DddRules.memberEncap(FIX))
                .as("HG-AGG-MEMBER-ENCAP must detect the cross-feature member reference fixture").isNotEmpty();
    }

    @Test
    @Tag("PRACTICES-DDD-FIXTURE")
    void fixtureProvesCrossFeatureIsolationFires() {
        // ddd.fixtures.intruder.Outsider references ddd.fixtures.widget.WidgetPart (@Entity) across
        // features — HG-FEAT-ISOLATION must flag it (entity reference, not import-text dependent:
        // ArchUnit reads bytecode, so an FQN-without-import reference is caught the same way).
        assertThat(DddRules.featIsolation(FIX, EMPTY))
                .as("HG-FEAT-ISOLATION must detect a cross-feature @Entity reference fixture")
                .anyMatch(s -> s.contains("Outsider") && s.contains("WidgetPart"));
    }

    @Test
    @Tag("PRACTICES-DDD-FIXTURE")
    void fixtureProvesCrossAggregatePointerCaughtInCollection() {
        // CollectorRoot holds List<WidgetRoot> (a different aggregate's root) — HG-AGG-REF must flag
        // the COLLECTION element type, not just single-valued fields (getAllInvolvedRawTypes).
        assertThat(DddRules.aggRef(FIX, EMPTY))
                .as("HG-AGG-REF must detect a cross-aggregate pointer inside a collection field")
                .anyMatch(s -> s.contains("CollectorRoot") && s.contains("WidgetRoot"));
    }

    @Test
    @Tag("PRACTICES-DDD-FIXTURE")
    void fixtureProvesGodServiceTransactionFires() {
        // FixtureGodService.mutateTwoAggregates is @Transactional and directly saves TWO distinct
        // roots (WidgetRoot + GadgetRoot via saveAll-family `save`) — HG-ANTI-GODSERVICE-TX must flag it.
        Path fixturesSrc = Path.of(System.getProperty("user.dir"), "src", "test", "java", "ddd", "fixtures")
                .normalize();
        assertThat(DddRules.godService(FIX, fixturesSrc, EMPTY))
                .as("HG-ANTI-GODSERVICE-TX must detect a @Transactional method mutating 2 distinct roots")
                .anyMatch(s -> s.contains("FixtureGodService"));
    }

    @Test
    @Tag("PRACTICES-DDD-FIXTURE")
    void fixtureProvesMemberRepoCaughtThroughCustomBaseAndLineBreak() {
        // AM1 regression: WidgetPartRepo extends a CUSTOM base (BaseRepo, not *Repository) with a
        // LINE-BROKEN type parameter. The hardened DddRules.repoTargetMap must still resolve it to
        // the WidgetPart member, so HG-AGG-REPO fires. The pre-AM1 regex would have missed this.
        Path fixturesSrc = Path.of(System.getProperty("user.dir"), "src", "test", "java", "ddd", "fixtures")
                .normalize();
        assertThat(DddRules.memberRepo(FIX, fixturesSrc, EMPTY))
                .as("HG-AGG-REPO must detect a member repository declared via a custom base interface "
                        + "and a line-broken type parameter (AM1)")
                .anyMatch(s -> s.contains("WidgetPart"));
    }
}
