package com.ax.template.authblueprint.apikey;

import jakarta.persistence.Column;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reflective JPA-shape assertions for {@link ApiKey}.
 *
 * <p>Trace: KEY-STORAGE-003 — the {@code hashedValue} column MUST be both
 * {@code nullable=false} and {@code updatable=false}. Updating an existing
 * row's hash is structurally impossible at the JPA layer (rotation = REVOKE +
 * new INSERT, never UPDATE).
 */
@Tag("API_KEY")
class ApiKeyEntityShapeTest {

    @Test
    @Tag("KEY-STORAGE-003")
    void hashedValueColumn_isNotNullAndImmutable() throws Exception {
        Field f = ApiKey.class.getDeclaredField("hashedValue");
        Column column = f.getAnnotation(Column.class);

        assertThat(column).as("@Column annotation present").isNotNull();
        assertThat(column.nullable()).isFalse();
        assertThat(column.updatable()).isFalse();
        assertThat(column.length()).isEqualTo(64);  // SHA-256 hex
    }

    @Test
    @Tag("KEY-STORAGE-003")
    void hashPrefixColumn_isNotNullAndImmutable() throws Exception {
        Field f = ApiKey.class.getDeclaredField("hashPrefix");
        Column column = f.getAnnotation(Column.class);

        assertThat(column).isNotNull();
        assertThat(column.nullable()).isFalse();
        assertThat(column.updatable()).isFalse();
    }
}
