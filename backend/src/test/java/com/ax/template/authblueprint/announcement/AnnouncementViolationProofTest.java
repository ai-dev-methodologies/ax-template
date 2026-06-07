package com.ax.template.authblueprint.announcement;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof tests for announcement-l0. Structural + behavioral invariants that would
 * re-open a data-integrity hole if relaxed. Plain reflection / pure logic — no Spring context.
 */
@Tag("ANNOUNCEMENT")
class AnnouncementViolationProofTest {

    @Test
    void violation_noPublicStateSetter() throws Exception {
        // ANN-LIFECYCLE-001: state is advanced ONLY by AnnouncementStateMachine (same package,
        // package-private setState). A public setState would let any caller forge the lifecycle.
        boolean hasPublicSetState = false;
        for (Method m : Announcement.class.getMethods()) {   // getMethods = public only
            if (m.getName().equals("setState")) hasPublicSetState = true;
        }
        assertThat(hasPublicSetState)
            .as("Announcement.state must have no PUBLIC setter (sole-mutator state machine)")
            .isFalse();
    }

    @Test
    void violation_idAndCreatedByAndCreatedAtImmutable() throws Exception {
        for (String fieldName : new String[]{"id", "createdBy", "createdAt"}) {
            Field f = Announcement.class.getDeclaredField(fieldName);
            Column col = f.getAnnotation(Column.class);
            assertThat(col).as(fieldName + " must carry @Column").isNotNull();
            assertThat(col.updatable())
                .as(fieldName + " must be immutable (@Column updatable=false)")
                .isFalse();
        }
    }

    @Test
    void violation_versionFieldIsOptimisticLock() throws Exception {
        Field v = Announcement.class.getDeclaredField("version");
        assertThat(v.isAnnotationPresent(Version.class))
            .as("version must carry @Version (atomic state transitions, ANN-LIFECYCLE-001)")
            .isTrue();
    }

    @Test
    void violation_noStoredVisibilityFlag() {
        // ANN-WINDOW-001: visibility is DERIVED, never stored. There must be no is_active/
        // is_visible/active field on the entity.
        for (Field f : Announcement.class.getDeclaredFields()) {
            String n = f.getName().toLowerCase();
            assertThat(n.contains("active") || n.contains("visible"))
                .as("entity must not store a visibility flag (field: " + f.getName() + ")")
                .isFalse();
        }
    }

    @Test
    void violation_activeWindowIsHalfOpen_exclusiveUpper() {
        // ANN-WINDOW-001: active iff PUBLISHED AND startsAt <= now < endsAt (exclusive upper).
        Instant start = Instant.parse("2026-06-07T00:00:00Z");
        Instant end = start.plus(2, ChronoUnit.HOURS);
        Announcement a = new Announcement(java.util.UUID.randomUUID(), "t", "b", start, end, "admin", start);
        // DRAFT is never active even within the window
        assertThat(a.isActiveAt(start.plus(1, ChronoUnit.HOURS))).isFalse();
        // publish via the state machine (sole mutator), then check window boundaries
        new AnnouncementStateMachine().publish(a);
        assertThat(a.isActiveAt(start.minusSeconds(1))).as("before startsAt -> inactive").isFalse();
        assertThat(a.isActiveAt(start)).as("startsAt inclusive -> active").isTrue();
        assertThat(a.isActiveAt(start.plus(1, ChronoUnit.HOURS))).as("within -> active").isTrue();
        assertThat(a.isActiveAt(end)).as("endsAt exclusive -> inactive").isFalse();
        assertThat(a.isActiveAt(end.plusSeconds(1))).as("after endsAt -> inactive").isFalse();
    }
}
