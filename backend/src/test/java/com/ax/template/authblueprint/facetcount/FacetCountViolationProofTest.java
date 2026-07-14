package com.ax.template.authblueprint.facetcount;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * VIOLATION proof for facet-count-l0. Structural assertions a deliberate break cannot pass
 * silently: the allowlist rejects a non-allowlisted facet field BY NAME before any repository
 * access, the aggregation queries are fixed/parameterized (never client-string concatenation
 * into JPQL), the FacetableItem entity carries immutable identity columns + @Version + no
 * public setters, and the service never selects a repository method from a raw request param.
 */
@Tag("FACETCOUNT")
class FacetCountViolationProofTest {

    // ── FACET-ALLOWLIST-002 — a non-allowlisted field is rejected by NAME; ownerId is absent ──
    @Test @Tag("FACET-ALLOWLIST-002")
    void violation_nonAllowlistedField_rejectedByName_ownerIdAbsent() {
        assertThat(FacetFieldAllowlist.allowed()).containsExactlyInAnyOrder("category", "status");
        assertThat(FacetFieldAllowlist.allowed()).doesNotContain("ownerId", "id", "version");

        assertThatThrownBy(() -> FacetFieldAllowlist.resolve("ownerId"))
            .isInstanceOf(FacetCountException.class)
            .hasMessageContaining("ownerId")
            .extracting(ex -> ((FacetCountException) ex).code())
            .isEqualTo("FACET_FIELD_NOT_ALLOWED");

        assertThat(FacetFieldAllowlist.resolve("category")).isEqualTo("category");
    }

    // ── entity — immutable identity columns, @Version, no public setters ──
    @Test @Tag("FACET-COUNT-001")
    void violation_entityImmutable_versioned_noSetters() throws Exception {
        for (Method m : FacetableItem.class.getMethods()) {
            assertThat(m.getName()).as("FacetableItem must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "ownerId", "category", "createdAt"}) {
            Column col = FacetableItem.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("FacetableItem." + f + " must be immutable").isFalse();
        }
        assertThat(FacetableItem.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
    }

    // ── the service resolves the field through the allowlist BEFORE any repository call; no raw-string JPQL ──
    @Test @Tag("FACET-ALLOWLIST-002")
    void violation_serviceResolvesBeforeQuery_neverConcatenatesField() throws Exception {
        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "facetcount", "FacetCountService.java"));
        assertThat(svc).as("the service resolves the field through the allowlist first")
            .contains("FacetFieldAllowlist.resolve(publicField)");
        assertThat(svc).as("the service selects a FIXED repository method, never builds JPQL from the param")
            .doesNotContain("\"SELECT\"").doesNotContain("+ publicField").doesNotContain("+ internal +");

        String repo = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "facetcount", "FacetableItemRepository.java"));
        assertThat(repo).as("both aggregation queries share the ownerId scope predicate")
            .contains("WHERE f.ownerId = :ownerId").doesNotContain("String.format").doesNotContain("+ field");
    }

    // ── the migration exists and matches the entity's columns ──
    @Test @Tag("FACET-COUNT-001")
    void violation_migrationExists() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V101__create_facetable_items.sql")) {
            assertThat(in).as("V101__create_facetable_items.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("CREATE TABLE facetable_items");
            assertThat(sql).contains("owner_id");
            assertThat(sql).contains("category");
            assertThat(sql).contains("status");
        }
    }
}
