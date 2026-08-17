package com.ax.template.authblueprint.pagination;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * pagination-l0 compliance — every item verified against the live reference workload. Domain
 * @Tag("PAGINATION") drives ./gradlew testPagination; the per-item @Tag binds the spec item to the
 * test (spec_item_verification_binding guard). Spec: specs/pagination-l0.yaml.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PaginationComplianceTest {

    @LocalServerPort
    int port;

    @Autowired
    MeterRegistry registry;

    String token;

    @BeforeEach
    void setUp() {
        String email = "page-" + UUID.randomUUID() + "@example.com";
        given().header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\",\"role\":\"MEMBER\"}")
            .when().post("/api/auth/email/signup");
        token = given().header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\"}")
            .when().post("/api/auth/email/login").then().extract().path("accessToken");
    }

    private io.restassured.specification.RequestSpecification auth() {
        return given().header("Authorization", "Bearer " + token);
    }

    @Test
    @Tag("PAGINATION")
    @Tag("PAGE-OFFSET-001")
    void offsetModeReturnsTheCanonicalEnvelope() {
        var r = auth().queryParam("page", 0).queryParam("page_size", 10).get("/api/pagination/items")
            .then().statusCode(200).extract();
        assertThat((Integer) r.path("pagination.page")).isEqualTo(0);
        assertThat((Integer) r.path("pagination.pageSize")).isEqualTo(10);
        assertThat((Integer) r.path("pagination.totalPages")).isEqualTo(5);
        assertThat((Boolean) r.path("pagination.hasMore")).isTrue();
        assertThat((List<?>) r.path("data")).hasSize(10);
    }

    @Test
    @Tag("PAGINATION")
    @Tag("PAGE-LIMIT-001")
    void pageSizeIsBounded() {
        auth().queryParam("page_size", 5000).get("/api/pagination/items")
            .then().statusCode(400).body("error", org.hamcrest.Matchers.equalTo("PAGE_SIZE_INVALID"));
        auth().queryParam("page_size", 10).get("/api/pagination/items").then().statusCode(200);
    }

    @Test
    @Tag("PAGINATION")
    @Tag("PAGE-LINK-001")
    void emitsRfc5988LinkHeader() {
        String link = auth().queryParam("page", 1).queryParam("page_size", 10).get("/api/pagination/items")
            .then().statusCode(200).extract().header("Link");
        assertThat(link).contains("rel=\"next\"").contains("rel=\"prev\"");
    }

    @Test
    @Tag("PAGINATION")
    @Tag("PAGE-CURSOR-001")
    void opaqueCursorsAreTamperEvident() {
        String next = auth().queryParam("mode", "cursor").queryParam("page_size", 10).get("/api/pagination/items")
            .then().statusCode(200).extract().path("pagination.next_cursor");
        assertThat(next).isNotBlank();
        // following the cursor returns the NEXT window (ids 11..20)
        Integer firstId = auth().queryParam("mode", "cursor").queryParam("cursor", next).queryParam("page_size", 10)
            .get("/api/pagination/items").then().statusCode(200).extract().path("data[0].id");
        assertThat(firstId).isEqualTo(11);
        // a tampered cursor is rejected (signature mismatch)
        String tampered = next.substring(0, next.length() - 2) + (next.endsWith("A") ? "B" : "A");
        auth().queryParam("mode", "cursor").queryParam("cursor", tampered).get("/api/pagination/items")
            .then().statusCode(400).body("error", org.hamcrest.Matchers.equalTo("PAGE-CURSOR-001"));
    }

    @Test
    @Tag("PAGINATION")
    @Tag("PAGE-STABLE-SORT-001")
    void unstableSortIsRejected() {
        auth().queryParam("sort", "name").get("/api/pagination/items")
            .then().statusCode(400).body("error", org.hamcrest.Matchers.equalTo("PAGE-STABLE-SORT-001"));
        auth().queryParam("sort", "id").get("/api/pagination/items").then().statusCode(200);
        auth().queryParam("sort", "name,id").get("/api/pagination/items").then().statusCode(200);
    }

    @Test
    @Tag("PAGINATION")
    @Tag("PAGE-COUNT-001")
    void totalCountIsOptIn() {
        assertThat((Object) auth().queryParam("page", 0).get("/api/pagination/items")
            .then().statusCode(200).extract().path("pagination.totalElements")).isNull();
        assertThat(((Number) auth().queryParam("include_total", true).get("/api/pagination/items")
            .then().statusCode(200).extract().path("pagination.totalElements")).longValue()).isEqualTo(50L);
    }

    @Test
    @Tag("PAGINATION")
    @Tag("PAGE-OBSERVABILITY-001")
    void exposesBoundedLabelMeters() {
        auth().queryParam("mode", "offset").get("/api/pagination/items").then().statusCode(200);
        auth().queryParam("mode", "cursor").get("/api/pagination/items").then().statusCode(200);
        auth().queryParam("page", 2000).queryParam("page_size", 10).get("/api/pagination/items").then().statusCode(200); // deep offset → drift

        assertThat(registry.find(PaginationMetrics.REQUESTS).counter()).isNotNull();
        assertThat(registry.find(PaginationMetrics.RESPONSE_TIME).timer()).isNotNull();
        assertThat(registry.find(PaginationMetrics.DRIFT).counter()).isNotNull();

        Set<String> allowed = Set.of("tenant", "mode");
        for (String name : List.of(PaginationMetrics.REQUESTS, PaginationMetrics.RESPONSE_TIME, PaginationMetrics.DRIFT)) {
            for (Meter m : registry.find(name).meters()) {
                for (io.micrometer.core.instrument.Tag t : m.getId().getTags()) {
                    assertThat(allowed).as("meter %s tag %s bounded", name, t.getKey()).contains(t.getKey());
                }
            }
        }
    }
}
