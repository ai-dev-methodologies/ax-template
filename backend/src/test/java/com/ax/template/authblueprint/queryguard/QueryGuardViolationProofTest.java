package com.ax.template.authblueprint.queryguard;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.data.domain.Sort;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * VIOLATION proof for query-field-allowlist-l0. Structural assertions a deliberate break cannot
 * pass silently: the allowlist rejects a non-allowlisted sort/filter field BY NAME (and a bad
 * direction/operator), maps only PUBLIC names, the Sort/Specification is built only from an
 * allowlisted internal property (never a raw client string), the CatalogItem entity carries
 * immutable identity columns + @Version + the @Check backstop + no public setters, and the
 * service/controller never call Sort.by on a raw request param.
 */
@Tag("QUERYGUARD")
class QueryGuardViolationProofTest {

    private static final QueryFieldAllowlist ALLOWLIST = QueryGuardService.CATALOG_ITEM_ALLOWLIST;

    // ── QUERY-ALLOWLIST-SORT-001 — a non-allowlisted sort field is rejected by NAME; direction closed ──
    @Test @Tag("QUERY-ALLOWLIST-SORT-001")
    void violation_nonAllowlistedSort_rejectedByName_directionClosed() {
        // allowlisted field + valid direction → a Sort over the mapped internal property
        Sort sort = ALLOWLIST.toSort("name", "asc");
        assertThat(sort.getOrderFor("name")).isNotNull();
        assertThat(sort.getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.ASC);

        // a non-allowlisted field → 422 QUERY_FIELD_NOT_SORTABLE naming the field
        assertThatThrownBy(() -> ALLOWLIST.toSort("password", "asc"))
            .isInstanceOf(QueryGuardException.class)
            .hasMessageContaining("password")
            .extracting(ex -> ((QueryGuardException) ex).code())
            .isEqualTo("QUERY_FIELD_NOT_SORTABLE");

        // a direction outside {asc,desc} → 422 QUERY_DIRECTION_INVALID
        assertThatThrownBy(() -> ALLOWLIST.toSort("name", "sideways"))
            .isInstanceOf(QueryGuardException.class)
            .extracting(ex -> ((QueryGuardException) ex).code())
            .isEqualTo("QUERY_DIRECTION_INVALID");
    }

    // ── QUERY-ALLOWLIST-FILTER-001 — a non-allowlisted filter field rejected; operator closed ──
    @Test @Tag("QUERY-ALLOWLIST-FILTER-001")
    void violation_nonAllowlistedFilter_rejectedByName_operatorClosed() {
        // allowlisted field → maps to the internal property
        assertThat(ALLOWLIST.filterProperty("status")).isEqualTo("status");

        // a non-allowlisted field → 422 QUERY_FIELD_NOT_FILTERABLE naming the field
        assertThatThrownBy(() -> ALLOWLIST.filterProperty("internalNotes"))
            .isInstanceOf(QueryGuardException.class)
            .hasMessageContaining("internalNotes")
            .extracting(ex -> ((QueryGuardException) ex).code())
            .isEqualTo("QUERY_FIELD_NOT_FILTERABLE");

        // the operator set is closed: only the safe enum tokens parse
        for (String safe : new String[]{"eq", "ne", "gt", "gte", "lt", "lte", "like"}) {
            assertThat(FilterOperator.parse(safe)).as(safe + " is a safe operator").isPresent();
        }
        assertThat(FilterOperator.parse("DROP TABLE")).as("a SQL fragment is not an operator").isEmpty();
        assertThat(FilterOperator.parse("contains")).isEmpty();
    }

    // ── QUERY-ALLOWLIST-MAPPING-001 — only PUBLIC names are declared; internalNotes is absent ──
    @Test @Tag("QUERY-ALLOWLIST-MAPPING-001")
    void violation_allowlistDeclaresOnlyPublicSurface() {
        assertThat(ALLOWLIST.sortableFields()).containsExactlyInAnyOrder("name", "createdAt", "status", "priceMinor");
        assertThat(ALLOWLIST.filterableFields()).containsExactlyInAnyOrder("name", "status", "priceMinor");
        assertThat(ALLOWLIST.sortableFields()).doesNotContain("internalNotes", "password", "version");
        assertThat(ALLOWLIST.filterableFields()).doesNotContain("internalNotes", "password", "version");
    }

    // ── entity — immutable identity columns, @Version, @Check backstop, no public setters ──
    @Test @Tag("QUERY-ALLOWLIST-PAGE-001")
    void violation_entityImmutable_versioned_checkBackstop_noSetters() throws Exception {
        for (Method m : CatalogItem.class.getMethods()) {
            assertThat(m.getName()).as("CatalogItem must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "createdAt"}) {
            Column col = CatalogItem.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("CatalogItem." + f + " must be immutable").isFalse();
        }
        assertThat(CatalogItem.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();

        Check check = CatalogItem.class.getAnnotation(Check.class);
        assertThat(check).as("CatalogItem must carry a @Check backstop").isNotNull();
        assertThat(check.constraints().replaceAll("\\s+", " ")).contains("price_minor >= 0");
    }

    // ── the service/controller never hand a raw request param to Sort.by; the service gates first ──
    @Test @Tag("QUERY-ALLOWLIST-KEYSTONE-001")
    void violation_rawParamNeverReachesSortBy() throws Exception {
        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "queryguard", "QueryGuardService.java"));
        // the Sort is built by the allowlist, not from a raw param
        assertThat(svc).as("the service builds Sort through the allowlist").contains("CATALOG_ITEM_ALLOWLIST.toSort(");
        // the only Sort.by in the service lives inside the allowlist component, not here
        assertThat(svc).as("the service never calls Sort.by directly on a request param").doesNotContain("Sort.by(");

        String ctrl = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "queryguard", "QueryGuardController.java"));
        assertThat(ctrl).as("the controller never builds a Sort or Specification itself")
            .doesNotContain("Sort.by(").doesNotContain("Specification");

        // the allowlist component is where Sort.by lives, fed an internal property only
        String allow = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "queryguard", "QueryFieldAllowlist.java"));
        assertThat(allow).contains("Sort.by(springDirection, internalProperty)");
    }

    // ── the migration carries the same backstop ──
    @Test @Tag("QUERY-ALLOWLIST-PAGE-001")
    void violation_migrationCarriesTheSameBackstop() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V056__create_queryguard.sql")) {
            assertThat(in).as("V056__create_queryguard.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("CREATE TABLE catalog_items");
            assertThat(sql).contains("price_minor >= 0");
            assertThat(sql).contains("internal_notes");
        }
    }
}
