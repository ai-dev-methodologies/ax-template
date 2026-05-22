package com.ax.template.authblueprint.commentthread;

import jakarta.persistence.Column;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof tests for R36 — closes METHODOLOGY.md Step 5.
 * Mirrors R31..R35 ViolationProofTest convention.
 */
@Tag("COMMENT")
class CommentViolationProofTest {

    @Test
    @Tag("COMMENT-CRUD-001")
    void violation_authorUserIdColumn_isImmutable() throws Exception {
        Field f = Comment.class.getDeclaredField("authorUserId");
        Column c = f.getAnnotation(Column.class);
        assertThat(c.updatable())
            .as("Comment.authorUserId MUST be @Column(updatable=false) — re-attributing a comment "
              + "to a different author would falsify the audit trail")
            .isFalse();
        assertThat(c.nullable()).isFalse();
    }

    @Test
    @Tag("COMMENT-THREAD-002")
    void violation_entityColumns_andParentColumn_areImmutable() throws Exception {
        for (String name : new String[] { "entityType", "entityId", "parentCommentId", "createdAt" }) {
            Field f = Comment.class.getDeclaredField(name);
            Column c = f.getAnnotation(Column.class);
            assertThat(c.updatable())
                .as("Comment." + name + " MUST be immutable so threads cannot be silently re-targeted "
                  + "or re-parented mid-life")
                .isFalse();
        }
    }

    @Test
    @Tag("COMMENT-HISTORY-001")
    void violation_commentEditFields_areAllImmutable() throws Exception {
        for (String name : new String[] { "commentId", "editedAt", "editedByUserId", "previousBody" }) {
            Field f = CommentEdit.class.getDeclaredField(name);
            Column c = f.getAnnotation(Column.class);
            assertThat(c.updatable())
                .as("CommentEdit." + name + " MUST be immutable — edit history is audit-grade")
                .isFalse();
        }
    }

    @Test
    @Tag("COMMENT-CRUD-003")
    void violation_bodyColumn_isNullableForSoftDelete() throws Exception {
        Field f = Comment.class.getDeclaredField("body");
        Column c = f.getAnnotation(Column.class);
        assertThat(c.nullable())
            .as("Comment.body MUST be nullable so soft-delete can clear it (COMMENT-CRUD-003)")
            .isTrue();
    }

    @Test
    @Tag("COMMENT-CRUD-001")
    void violation_noPublicSettersOnComment() {
        for (var m : Comment.class.getDeclaredMethods()) {
            if (m.getName().startsWith("set")) {
                int mod = m.getModifiers();
                assertThat(java.lang.reflect.Modifier.isPublic(mod))
                    .as("Comment." + m.getName() + " must NOT be public — service is the only mutator")
                    .isFalse();
            }
        }
    }

    @Test
    @Tag("COMMENT-HISTORY-001")
    void violation_commentEditHasNoSetters() {
        long settersCount = java.util.Arrays.stream(CommentEdit.class.getDeclaredMethods())
            .filter(m -> m.getName().startsWith("set"))
            .count();
        assertThat(settersCount)
            .as("CommentEdit MUST have zero setters — every field is immutable by construction")
            .isZero();
    }
}
