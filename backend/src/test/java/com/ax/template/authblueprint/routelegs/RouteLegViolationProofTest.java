package com.ax.template.authblueprint.routelegs;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for route-leg-contiguity-l0. Structural assertions a deliberate break cannot
 * pass silently: Route has no public setter (mutationSeq is package-private, touched only via
 * touchMutation) + @Version present; RouteLeg has NO setters at all (mutated exclusively via bulk
 * JPQL) + immutable identity columns + the ordinal @Check backstop + uq(route_id, ordinal); the
 * mutation methods always route through the two-phase park-then-land shift, never a naive
 * single-statement renumber; RouteLeg owns no repository of its own (HG-AGG-REPO); and the
 * migration carries the same backstops.
 */
@Tag("ROUTELEGS")
class RouteLegViolationProofTest {

    // ── LEG-MUTATE-001 — Route has no public setter; @Version present; touchMutation is package-private ──
    @Test @Tag("LEG-MUTATE-001")
    void violation_routeNoPublicSetter_versionPresent() throws Exception {
        for (Method m : Route.class.getMethods()) {
            assertThat(m.getName()).as("Route must have no public setter").doesNotStartWith("set");
        }
        Method touch = java.util.Arrays.stream(Route.class.getDeclaredMethods())
            .filter(x -> x.getName().equals("touchMutation")).findFirst().orElseThrow();
        assertThat(java.lang.reflect.Modifier.isPublic(touch.getModifiers()))
            .as("Route.touchMutation must be package-private").isFalse();
        assertThat(Route.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
    }

    // ── LEG-GAP-001 — RouteLeg has NO setters at all; immutable identity columns; ordinal @Check; uq backstop ──
    @Test @Tag("LEG-GAP-001")
    void violation_routeLegNoSetters_immutableIdentity_checkAndUniqueBackstop() throws Exception {
        for (Method m : RouteLeg.class.getMethods()) {
            assertThat(m.getName()).as("RouteLeg must have no setter — mutated only via bulk JPQL")
                .doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "routeId", "createdAt"}) {
            Column col = RouteLeg.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("RouteLeg." + f + " must be immutable").isFalse();
        }
        Check check = RouteLeg.class.getAnnotation(Check.class);
        assertThat(check).as("the ordinal positivity backstop must be present").isNotNull();
        assertThat(check.constraints()).contains("ordinal >= 1");

        jakarta.persistence.Table table = RouteLeg.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()[0].columnNames())
            .as("uq(route_id, ordinal) is the no-duplicate-position backstop")
            .containsExactly("route_id", "ordinal");
    }

    // ── HG-AGG-REPO — RouteLeg owns no repository of its own (a member, written through Route's repo) ──
    @Test @Tag("LEG-GAP-001")
    void violation_noDedicatedRouteLegRepository() {
        assertThat(java.util.Arrays.stream(new java.io.File(
                System.getProperty("user.dir"), "src/main/java/com/ax/template/authblueprint/routelegs")
            .listFiles())
            .map(java.io.File::getName))
            .as("RouteLeg must have no *Repository of its own")
            .doesNotContain("RouteLegRepository.java");
    }

    // ── LEG-GAP/MUTATE-001 — mutation methods always shift ordinals through a disjoint temp zone
    //    first (park), never a naive single-statement renumber that could hit uq(route_id, ordinal) ──
    @Test @Tag("LEG-GAP-001") @Tag("LEG-MUTATE-001")
    void violation_mutationsParkBeforeRenumbering() throws Exception {
        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "routelegs", "RouteLegService.java"));
        for (String method : new String[]{"insertLegAt", "removeLeg"}) {
            int start = svc.indexOf("public Route " + method + "(");
            assertThat(start).as(method + " must exist").isPositive();
            int end = svc.indexOf("\n    public Route ", start + 1);
            String body = end > 0 ? svc.substring(start, end) : svc.substring(start);
            assertThat(body).as(method + " must park before landing (never a naive single-statement shift)")
                .contains("parkOrdinalsFrom").contains("landOrdinalsFrom");
        }
        // reorder parks every leg, then lands each at its final permutation position individually
        int reorderStart = svc.indexOf("public Route reorderLegs(");
        assertThat(reorderStart).as("reorderLegs must exist").isPositive();
        String reorderBody = svc.substring(reorderStart, svc.indexOf("\n    public Route ", reorderStart + 1));
        assertThat(reorderBody).as("reorderLegs must park the whole route before individually landing each leg")
            .contains("parkOrdinalsFrom").contains("setOrdinal");
        // every structural mutation dirties the root so its @Version fires
        assertThat(svc).contains("commitMutation").contains("touchMutation()").contains("saveAndFlush");
    }

    // ── the migration carries the same backstops ──
    @Test @Tag("LEG-GAP-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V095__create_routelegs.sql")) {
            assertThat(in).as("V095__create_routelegs.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("ordinal >= 1");
            assertThat(sql).contains("UNIQUE INDEX uq_route_leg_ordinal");
            assertThat(sql).contains("(route_id, ordinal)");
        }
    }
}
