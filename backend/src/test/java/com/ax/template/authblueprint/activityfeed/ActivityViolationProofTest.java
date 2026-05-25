package com.ax.template.authblueprint.activityfeed;

import jakarta.persistence.Column;
import jakarta.persistence.UniqueConstraint;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof tests — closes METHODOLOGY.md Step 5 for R35 activity-feed.
 * Mirrors the R31..R34 ViolationProofTest convention.
 */
@Tag("ACTIVITY")
class ActivityViolationProofTest {

    @Test
    @Tag("ACT-PUBLISH-001")
    void violation_actorUserIdColumn_isImmutable() throws Exception {
        Field f = ActivityEvent.class.getDeclaredField("actorUserId");
        Column c = f.getAnnotation(Column.class);
        assertThat(c.updatable())
            .as("ActivityEvent.actorUserId MUST be @Column(updatable=false) — re-attributing an event to "
              + "a different actor would falsify the audit trail")
            .isFalse();
        assertThat(c.nullable()).isFalse();
    }

    @Test
    @Tag("ACT-READ-002")
    void violation_createdAtColumn_isImmutable() throws Exception {
        Field f = ActivityEvent.class.getDeclaredField("createdAt");
        Column c = f.getAnnotation(Column.class);
        assertThat(c.updatable())
            .as("createdAt drives the feed-order contract (ACT-READ-002, newest first); silent updates "
              + "would reorder user-visible feeds")
            .isFalse();
        assertThat(c.nullable()).isFalse();
    }

    @Test
    @Tag("ACT-PUBLISH-003")
    void violation_idempotencyUniqueConstraint_isStillDeclared() {
        jakarta.persistence.Table table = ActivityEvent.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table).isNotNull();
        boolean hasIdempotencyConstraint = false;
        for (UniqueConstraint uc : table.uniqueConstraints()) {
            String[] cols = uc.columnNames();
            if (cols.length == 2
                && contains(cols, "actor_user_id")
                && contains(cols, "idempotency_key")) {
                hasIdempotencyConstraint = true;
                break;
            }
        }
        assertThat(hasIdempotencyConstraint)
            .as("UNIQUE(actor_user_id, idempotency_key) backs ACT-PUBLISH-003 idempotent publish at the "
              + "DB layer; dropping it re-opens the duplicate-insert window under concurrent POSTs")
            .isTrue();
    }

    @Test
    @Tag("ACT-MARK-001")
    void violation_activityReadUniqueConstraint_isStillDeclared() {
        jakarta.persistence.Table table = ActivityRead.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table).isNotNull();
        boolean hasUserEventConstraint = false;
        for (UniqueConstraint uc : table.uniqueConstraints()) {
            String[] cols = uc.columnNames();
            if (cols.length == 2 && contains(cols, "event_id") && contains(cols, "user_id")) {
                hasUserEventConstraint = true;
                break;
            }
        }
        assertThat(hasUserEventConstraint)
            .as("UNIQUE(event_id, user_id) on activity_reads backs ACT-MARK-001 idempotent mark-read; "
              + "without it, double-clicking 'mark read' could create duplicate read rows")
            .isTrue();
    }

    @Test
    @Tag("ACT-MARK-001")
    void violation_activityReadKeyColumns_areImmutable() throws Exception {
        for (String name : new String[] { "eventId", "userId", "readAt" }) {
            Field f = ActivityRead.class.getDeclaredField(name);
            Column c = f.getAnnotation(Column.class);
            assertThat(c.updatable())
                .as("ActivityRead." + name + " MUST be immutable — re-pointing a read row would let one "
                  + "user 'inherit' another user's read state")
                .isFalse();
        }
    }

    @Test
    @Tag("ACT-PUBLISH-001")
    void violation_noPublicSetters() {
        for (var m : ActivityEvent.class.getDeclaredMethods()) {
            if (m.getName().startsWith("set")) {
                int mod = m.getModifiers();
                assertThat(java.lang.reflect.Modifier.isPublic(mod))
                    .as("ActivityEvent." + m.getName() + " must NOT be public — service is the only mutator")
                    .isFalse();
            }
        }
    }

    // ── R52 backend-contract wave 1: audience peer leak DTO scoping ─────────

    @Test
    @Tag("ACT-AUTHZ-001")
    void violation_responseDtoMustNotExposeAudienceUserIds() {
        // R44 P2-F7 (Compliance Officer) caught audienceUserIds being serialized
        // on every ActivityEventResponse — every recipient could enumerate every
        // other recipient via DevTools Network tab. R52 closure: the response
        // DTO returns a boolean (youAreInAudience), not the full id set.
        // Re-introducing the field would re-open the peer-leak surface.
        for (var field : ActivityDtos.ActivityEventResponse.class.getDeclaredFields()) {
            String name = field.getName();
            assertThat(name)
                .as("ActivityEventResponse MUST NOT carry an audience id collection — "
                  + "R52 replaced audienceUserIds with the youAreInAudience boolean to "
                  + "close R44 P2-F7 peer-leak surface")
                .isNotEqualTo("audienceUserIds");
        }
    }

    @Test
    @Tag("ACT-AUTHZ-001")
    void violation_responseDtoMustExposeYouAreInAudienceBoolean() throws Exception {
        // Companion invariant for R52: the boolean replacement field MUST be
        // present and be a primitive boolean — null-tristate would re-open
        // the ambiguity (caller cannot tell membership from missing field).
        Field f = ActivityDtos.ActivityEventResponse.class.getDeclaredField("youAreInAudience");
        assertThat(f.getType())
            .as("ActivityEventResponse.youAreInAudience MUST be primitive boolean — "
              + "Boolean wrapper would allow null, re-opening tri-state ambiguity")
            .isEqualTo(boolean.class);
    }

    private static boolean contains(String[] arr, String v) {
        for (String s : arr) if (s.equals(v)) return true;
        return false;
    }
}
