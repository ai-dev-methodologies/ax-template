package com.ax.template.authblueprint.favoritesbookmarks;

import jakarta.persistence.Column;
import jakarta.persistence.UniqueConstraint;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof tests — closes METHODOLOGY.md Step 5 for R34.
 * Mirrors R31 ApprovalViolationProofTest / R32 TagViolationProofTest / R33 SessionDogfoodIter1Test.
 *
 * <p>Each test deliberately re-asserts a structural promise the favorite domain depends on.
 * If a future commit relaxes any of these (intentionally or accidentally), the catalog's
 * idempotency / authz contracts erode — this file is the structural backstop.
 */
@Tag("FAVORITES")
class FavoriteViolationProofTest {

    /**
     * Violation: user_id column annotated mutable. The whole authz model
     * (FAV-AUTHZ-002/003) assumes a Favorite row's user_id is stable — re-pointing
     * a row at a different user would be a stealth transfer of someone's data.
     */
    @Test
    @Tag("FAV-AUTHZ-002")
    void violation_userIdColumn_isImmutable() throws Exception {
        Field f = Favorite.class.getDeclaredField("userId");
        Column c = f.getAnnotation(Column.class);
        assertThat(c.updatable())
            .as("Favorite.userId MUST be @Column(updatable=false) — re-pointing a row across users "
              + "would be a stealth ownership transfer")
            .isFalse();
        assertThat(c.nullable()).isFalse();
    }

    /**
     * Violation: entity_type or entity_id annotated mutable. Re-pointing a favorite at
     * a different entity mid-life would break the UNIQUE(user_id, entity_type, entity_id)
     * promise and silently swap which entity the user actually likes.
     */
    @Test
    @Tag("FAV-CRUD-001")
    void violation_entityColumns_areImmutable() throws Exception {
        for (String name : new String[] { "entityType", "entityId" }) {
            Field f = Favorite.class.getDeclaredField(name);
            Column c = f.getAnnotation(Column.class);
            assertThat(c.updatable())
                .as("Favorite." + name + " MUST be immutable so favorites cannot be silently re-targeted")
                .isFalse();
            assertThat(c.nullable()).isFalse();
        }
    }

    /**
     * Violation: UNIQUE constraint on (user_id, entity_type, entity_id) was dropped.
     * Without it, the idempotent-add contract FAV-CRUD-001 falls back to a TOCTOU race —
     * concurrent POSTs from the same caller would insert duplicates.
     */
    @Test
    @Tag("FAV-CRUD-001")
    void violation_uniqueConstraint_isStillDeclared() {
        jakarta.persistence.Table table = Favorite.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table).isNotNull();
        boolean hasTriple = false;
        for (UniqueConstraint uc : table.uniqueConstraints()) {
            String[] cols = uc.columnNames();
            if (cols.length == 3
                && contains(cols, "user_id")
                && contains(cols, "entity_type")
                && contains(cols, "entity_id")) {
                hasTriple = true;
                break;
            }
        }
        assertThat(hasTriple)
            .as("UNIQUE(user_id, entity_type, entity_id) backs FAV-CRUD-001 idempotent add at the DB layer; "
              + "dropping it re-opens the duplicate-insert window under concurrent POSTs")
            .isTrue();
    }

    /**
     * Violation: favorited_at column annotated mutable. The list ordering contract
     * FAV-CRUD-003 (newest first) assumes this is the original creation timestamp;
     * silent updates would reorder the user-visible feed.
     */
    @Test
    @Tag("FAV-CRUD-003")
    void violation_favoritedAtColumn_isImmutable() throws Exception {
        Field f = Favorite.class.getDeclaredField("favoritedAt");
        Column c = f.getAnnotation(Column.class);
        assertThat(c.updatable()).isFalse();
        assertThat(c.nullable()).isFalse();
    }

    /**
     * Violation: Favorite entity exposes a public setter. The service is the only
     * mutator (mutates only the {@code note} via repository.save → JPA merge);
     * a public setter would let any caller bypass that boundary.
     */
    @Test
    @Tag("FAV-CRUD-001")
    void violation_noPublicSetters() {
        for (var m : Favorite.class.getDeclaredMethods()) {
            if (m.getName().startsWith("set")) {
                int mod = m.getModifiers();
                assertThat(java.lang.reflect.Modifier.isPublic(mod))
                    .as("Favorite." + m.getName() + " must NOT be public — service is the only mutator")
                    .isFalse();
            }
        }
    }

    private static boolean contains(String[] arr, String v) {
        for (String s : arr) if (s.equals(v)) return true;
        return false;
    }
}
