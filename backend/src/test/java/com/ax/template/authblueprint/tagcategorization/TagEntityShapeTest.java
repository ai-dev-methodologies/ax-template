package com.ax.template.authblueprint.tagcategorization;

import jakarta.persistence.Column;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("TAGGING")
class TagEntityShapeTest {

    @Test
    @org.junit.jupiter.api.Tag("TAG-CRUD-003")
    void slugColumn_isNotNullAndImmutable() throws Exception {
        Field f = com.ax.template.authblueprint.tagcategorization.Tag.class.getDeclaredField("slug");
        Column column = f.getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.nullable()).isFalse();
        assertThat(column.updatable()).isFalse();
        assertThat(column.length()).isEqualTo(64);
    }

    @Test
    @org.junit.jupiter.api.Tag("TAG-HIER-001")
    void parentTagIdColumn_isImmutable() throws Exception {
        Field f = com.ax.template.authblueprint.tagcategorization.Tag.class.getDeclaredField("parentTagId");
        Column column = f.getAnnotation(Column.class);
        assertThat(column).isNotNull();
        assertThat(column.updatable()).isFalse();
    }
}
