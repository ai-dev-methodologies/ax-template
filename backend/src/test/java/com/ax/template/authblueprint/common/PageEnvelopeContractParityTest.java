package com.ax.template.authblueprint.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S2.QUERY-BOUNDS.XB — frontend<->backend contract parity for the canonical
 * {@link PageEnvelope} JSON shape (specs/pagination-l0.yaml PAGE-OFFSET-001,
 * specs/crud-frontend-l0.yaml CRUD-FE-003: "Items list page renders Pagination
 * controls reflecting pagination.totalPages from the listItems PageEnvelope
 * response").
 *
 * <p>{@link PageEnvelopeTest} (same package) already pins the DERIVATION
 * arithmetic (totalPages/hasMore). This test pins something that one does
 * NOT: the exact serialized JSON — field names, nesting, and value shapes —
 * against a golden fixture that {@code frontend/tests/page-envelope-parity.vitest.ts}
 * parses from the SAME file:
 * {@code frontend/tests/_fixtures/page-envelope.golden.json}.
 *
 * <p>One committed golden, two independent consumers (this Jackson test +
 * the FE parser test). A drift in either the record's field names OR the FE
 * parser trips exactly one of the two — never silently, since neither side
 * hand-derives the golden from the other.
 *
 * <p>Plain Jackson unit test — NO {@code @SpringBootTest}, zero ContextCache
 * pressure. Builds the envelope via the same generic {@link PageEnvelope#of}
 * factory that 15+ non-Spring-Data domains call directly (obligation,
 * netmetering, countbudget, reservation, decisiongov, ...); the wrapper shape
 * this locks is the real one every list endpoint in the catalog emits — see
 * {@code ItemController#list()}, which returns {@code PageEnvelope<ItemResponse>}
 * straight off {@link PageEnvelope#from}. The {@code data} element here is a
 * local String-only record (not {@code ItemResponse}) so the fixture carries
 * no dependency on {@code java.time.Instant}'s wire format — that is a
 * separate, unrelated serialization concern this test does not claim to pin.
 */
@Tag("PAGINATION")
@Tag("PAGE-OFFSET-001")
class PageEnvelopeContractParityTest {

    /** Mirrors a real list-item DTO's public surface (id/title/createdBy/createdAt,
     *  c.f. crud.ItemResponse) with String-only fields — unambiguous JSON, no
     *  java.time serialization-format dependency. */
    private record CatalogItem(String id, String title, String createdBy, String createdAt) {}

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode goldenTree() throws IOException {
        Path golden = Path.of(System.getProperty("user.dir"), "..", "frontend", "tests",
                "_fixtures", "page-envelope.golden.json");
        return MAPPER.readTree(Files.readString(golden));
    }

    /**
     * Builds an envelope whose values are IDENTICAL to the golden fixture —
     * same two items, same page/pageSize/totalElements. totalPages (7) and
     * hasMore (true) are DERIVED by {@link PageEnvelope#of}, exactly the
     * ceil(137/20)=7 / (1+1)*20=40 &lt; 137 arithmetic
     * {@link PageEnvelopeTest#from_mapsContentAndFillsPaginationFromSpringPage()}
     * already pins for the Spring {@code Page} path — so this is the SAME
     * real derivation, not a hand-picked pair of numbers.
     */
    private static PageEnvelope<CatalogItem> buildEnvelopeMatchingGolden() {
        List<CatalogItem> data = List.of(
                new CatalogItem("3f9a2b8e-6b7a-4e1a-9c3d-1a2b3c4d5e6f", "Quarterly report draft",
                        "alice@example.com", "2026-03-01T09:15:00Z"),
                new CatalogItem("7c1d4e2f-9a8b-4c6d-8e2f-5b6c7d8e9f0a", "Vendor onboarding checklist",
                        "bob@example.com", "2026-03-03T11:00:00Z"));
        return PageEnvelope.of(data, 1, 20, 137L);
    }

    @Test
    void pageEnvelope_serializesFieldWiseEqualToTheGoldenFixture() throws IOException {
        PageEnvelope<CatalogItem> envelope = buildEnvelopeMatchingGolden();
        JsonNode actual = MAPPER.readTree(MAPPER.writeValueAsString(envelope));
        JsonNode expected = goldenTree();

        // Whole-tree structural equality — a renamed field, an added/removed
        // pagination member, a data[] shape change, or a type change (e.g.
        // hasMore becoming a string) all trip this single assertion.
        assertThat(actual).as("PageEnvelope JSON must match the FE-shared golden fixture field-for-field")
                .isEqualTo(expected);
    }

    @Test
    void pageEnvelope_pagination_hasExactlyTheFiveCanonicalFieldsWithCorrectTypes() throws IOException {
        JsonNode pagination = goldenTree().get("pagination");
        assertThat(pagination).as("golden fixture must carry a pagination object").isNotNull();
        assertThat(pagination.size()).as("no extra/missing pagination members").isEqualTo(5);

        assertThat(pagination.get("page").isInt()).isTrue();
        assertThat(pagination.get("pageSize").isInt()).isTrue();
        assertThat(pagination.get("totalElements").isNumber()).isTrue();
        assertThat(pagination.get("totalPages").isInt()).isTrue();
        assertThat(pagination.get("hasMore").isBoolean()).isTrue();

        assertThat(pagination.get("page").asInt()).isEqualTo(1);
        assertThat(pagination.get("pageSize").asInt()).isEqualTo(20);
        assertThat(pagination.get("totalElements").asLong()).isEqualTo(137L);
        assertThat(pagination.get("totalPages").asInt()).isEqualTo(7);
        assertThat(pagination.get("hasMore").asBoolean()).isTrue();
    }

    @Test
    void pageEnvelope_data_isAnArrayAtTheTopLevelAlongsidePagination() throws IOException {
        JsonNode root = goldenTree();
        assertThat(root.size()).as("envelope must have exactly {data, pagination}").isEqualTo(2);
        assertThat(root.get("data").isArray()).isTrue();
        assertThat(root.get("data").size()).isEqualTo(2);
        assertThat(root.get("pagination").isObject()).isTrue();
    }
}
