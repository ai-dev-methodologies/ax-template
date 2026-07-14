package com.ax.template.authblueprint.geofence;

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
 * VIOLATION proof for geofence-transition-l0. Structural assertions a deliberate break cannot pass
 * silently: the tracker carries @Version + no public setter (status mutated only via the
 * package-private startPending/clearPending/confirmTransition hooks); the transition is fully
 * immutable with the dual-timestamp @Check backstop; the transition has NO repository of its own
 * (HG-AGG-REPO — it is a member, written through common/MemberWriter); the uq(subject_id, zone_id)
 * backstop exists; and the migration carries the same @Check.
 */
@Tag("GEOFENCE")
class GeofenceViolationProofTest {

    // ── GEOFENCE-DWELL-001 — the tracker has no public setter; @Version present ──
    @Test @Tag("GEOFENCE-DWELL-001")
    void violation_trackerNoPublicSetter_versionPresent() throws Exception {
        for (Method m : GeofenceTracker.class.getMethods()) {
            assertThat(m.getName()).as("GeofenceTracker must have no public setter").doesNotStartWith("set");
        }
        for (String hook : new String[]{"startPending", "clearPending", "confirmTransition"}) {
            Method m = java.util.Arrays.stream(GeofenceTracker.class.getDeclaredMethods())
                .filter(x -> x.getName().equals(hook)).findFirst().orElseThrow();
            assertThat(java.lang.reflect.Modifier.isPublic(m.getModifiers()))
                .as("GeofenceTracker." + hook + " must be package-private").isFalse();
        }
        assertThat(GeofenceTracker.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
        jakarta.persistence.Table table = GeofenceTracker.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()[0].columnNames())
            .as("uq(subject_id, zone_id) is the one-tracker-per-pair backstop")
            .containsExactly("subject_id", "zone_id");
    }

    // ── GEOFENCE-CONFIRM-001 — the transition is fully immutable, no setter, dual-timestamp @Check ──
    @Test @Tag("GEOFENCE-CONFIRM-001")
    void violation_transitionImmutable_noSetter_dualTimestampCheck() throws Exception {
        for (Method m : GeofenceTransition.class.getMethods()) {
            assertThat(m.getName()).as("GeofenceTransition must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "trackerId", "zoneId", "direction", "observedAt", "confirmedAt", "createdAt"}) {
            Column col = GeofenceTransition.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("GeofenceTransition." + f + " must be immutable").isFalse();
        }
        Check check = GeofenceTransition.class.getAnnotation(Check.class);
        assertThat(check).as("the dual-timestamp backstop must be present").isNotNull();
        assertThat(check.constraints().replaceAll("\\s+", " ")).contains("confirmed_at >= observed_at");
    }

    // ── HG-AGG-REPO — GeofenceTransition owns no repository of its own (a member, not a root) ──
    @Test @Tag("GEOFENCE-CONFIRM-001")
    void violation_noDedicatedTransitionRepository() {
        assertThat(java.util.Arrays.stream(new java.io.File(
                System.getProperty("user.dir"), "src/main/java/com/ax/template/authblueprint/geofence")
            .listFiles())
            .map(java.io.File::getName))
            .as("GeofenceTransition must have no *Repository of its own — it is written through MemberWriter")
            .doesNotContain("GeofenceTransitionRepository.java");
    }

    // ── the migration carries the same dual-timestamp @Check ──
    @Test @Tag("GEOFENCE-CONFIRM-001")
    void violation_migrationCarriesTheSameCheck() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V096__create_geofence.sql")) {
            assertThat(in).as("V096__create_geofence.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("confirmed_at >= observed_at");
            assertThat(sql).contains("UNIQUE INDEX uq_geofence_tracker_subject_zone");
        }
    }
}
