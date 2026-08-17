package com.ax.template.authblueprint.problemdetails;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
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
 * problem-details-l0 compliance — every item verified against the live reference workload.
 * Domain @Tag("PROBLEM_DETAILS") drives ./gradlew testProblemDetails; the per-item @Tag binds the
 * spec item to its test (spec_item_verification_binding guard). Spec: specs/problem-details-l0.yaml.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProblemDetailsComplianceTest {

    @LocalServerPort
    int port;

    @Autowired
    MeterRegistry registry;

    String token;

    @BeforeEach
    void setUp() {
        String email = "problem-" + UUID.randomUUID() + "@example.com";
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
    @Tag("PROBLEM_DETAILS")
    @Tag("PROBLEM-FORMAT-001")
    void errorBodyCarriesAllFiveRfc9457Members() {
        var r = auth().post("/api/problem-demo/insufficient-funds").then().statusCode(402).extract();

        // a 4xx body is application/problem+json, NEVER bare application/json
        assertThat(r.contentType()).startsWith("application/problem+json");

        JsonPath body = r.jsonPath();
        assertThat(body.getString("type")).isNotBlank();          // member 1
        assertThat(body.getString("title")).isNotBlank();         // member 2
        assertThat(body.getInt("status")).isEqualTo(402);         // member 3 == HTTP status line
        assertThat(body.getString("detail")).isNotBlank();        // member 4
        assertThat(body.getString("instance")).isNotBlank();      // member 5 (this-occurrence URI)
        assertThat(body.getString("instance")).contains("/api/problem-demo/insufficient-funds");
    }

    @Test
    @Tag("PROBLEM_DETAILS")
    @Tag("PROBLEM-TYPE-001")
    void distinctConditionsOwnDistinctStableDereferenceableTypeUris() {
        String fundsType = auth().post("/api/problem-demo/insufficient-funds")
            .then().statusCode(402).extract().jsonPath().getString("type");
        String serverType = auth().post("/api/problem-demo/boom")
            .then().statusCode(500).extract().jsonPath().getString("type");

        // dereferenceable https locators, enumerated in the closed registry
        assertThat(fundsType).startsWith("https://").isEqualTo(
            ProblemTypeRegistry.uri(ProblemTypeRegistry.INSUFFICIENT_FUNDS).toString());
        assertThat(serverType).startsWith("https://").isEqualTo(
            ProblemTypeRegistry.uri(ProblemTypeRegistry.SERVER_ERROR).toString());
        // distinct conditions MUST NOT share one generic type
        assertThat(fundsType).isNotEqualTo(serverType);
        // the type is STABLE across occurrences (clients key off it, not title/detail)
        String again = auth().post("/api/problem-demo/insufficient-funds")
            .then().statusCode(402).extract().jsonPath().getString("type");
        assertThat(again).isEqualTo(fundsType);
        // the registry enumerates every type the API can emit
        assertThat(ProblemTypeRegistry.slugs())
            .contains(ProblemTypeRegistry.INSUFFICIENT_FUNDS, ProblemTypeRegistry.SERVER_ERROR);
    }

    @Test
    @Tag("PROBLEM_DETAILS")
    @Tag("PROBLEM-EXTENSION-001")
    void domainContextIsConveyedAsTopLevelExtensionMembers() {
        JsonPath body = auth().post("/api/problem-demo/insufficient-funds")
            .then().statusCode(402).extract().jsonPath();

        // top-level siblings of type/title/status/detail — not nested under a wrapper
        assertThat(body.getDouble("balance")).isEqualTo(12.50);
        assertThat(body.getList("accounts")).containsExactly("acct-1001", "acct-2002");
        // structured context is NOT stuffed into the free-text detail prose
        assertThat(body.getString("detail")).doesNotContain("12.5").doesNotContain("acct-1001");
    }

    @Test
    @Tag("PROBLEM_DETAILS")
    @Tag("PROBLEM-VALIDATION-001")
    void everyFieldErrorIsReportedInOneArrayWithLocators() {
        JsonPath body = auth().header("Content-Type", "application/json")
            .body("{\"fromAccount\":\"\",\"amount\":-5}")
            .post("/api/problem-demo/validate")
            .then().statusCode(400).extract().jsonPath();

        List<?> errors = body.getList("errors");
        // ALL field errors, not fail-fast on the first
        assertThat(errors).hasSizeGreaterThanOrEqualTo(2);
        List<String> pointers = body.getList("errors.pointer");
        List<String> details = body.getList("errors.detail");
        assertThat(pointers).contains("/fromAccount", "/amount"); // RFC 6901 JSON Pointers
        assertThat(details).allSatisfy(d -> assertThat(d).isNotBlank());
    }

    @Test
    @Tag("PROBLEM_DETAILS")
    @Tag("PROBLEM-TRACE-001")
    void carriesTraceIdFromTraceparentAndLeaksNoStackTrace() {
        String traceId = "0af7651916cd43dd8448eb211c80319c";
        var r = auth().header("traceparent", "00-" + traceId + "-b7ad6b7169203331-01")
            .post("/api/problem-demo/boom").then().statusCode(500).extract();

        // trace_id populated from the W3C traceparent trace-id, echoed as a response header
        assertThat(r.jsonPath().getString("trace_id")).isEqualTo(traceId);
        assertThat(r.header("Trace-Id")).isEqualTo(traceId);
        // detail leaks NO stack frame, SQL, or internal exception text
        String detail = r.jsonPath().getString("detail");
        assertThat(detail).doesNotContain("at com.").doesNotContain("Exception")
            .doesNotContain("SQLSTATE").doesNotContain("PostingEngine");

        // with no trace context, a server-generated correlation id is still present (never blank)
        String generated = auth().post("/api/problem-demo/boom")
            .then().statusCode(500).extract().jsonPath().getString("trace_id");
        assertThat(generated).isNotBlank();
    }

    @Test
    @Tag("PROBLEM_DETAILS")
    @Tag("PROBLEM-I18N-001")
    void detailIsLocalizedWhileTypeUriStaysByteIdentical() {
        ExtractableResponse<Response> ko = auth().header("Accept-Language", "ko-KR")
            .post("/api/problem-demo/insufficient-funds").then().statusCode(402).extract();
        ExtractableResponse<Response> en = auth().header("Accept-Language", "en-US")
            .post("/api/problem-demo/insufficient-funds").then().statusCode(402).extract();

        String koDetail = ko.jsonPath().getString("detail");
        String enDetail = en.jsonPath().getString("detail");
        // detail is localized by Accept-Language content negotiation
        assertThat(koDetail).isNotEqualTo(enDetail);
        assertThat(koDetail).contains("부족"); // Korean
        assertThat(enDetail).contains("insufficient"); // English
        // the chosen language is advertised in Content-Language
        assertThat(ko.header("Content-Language")).startsWith("ko");
        assertThat(en.header("Content-Language")).startsWith("en");
        // the type URI is the stable machine key — byte-identical across locales
        assertThat(ko.jsonPath().getString("type")).isEqualTo(en.jsonPath().getString("type"));
        // title stays a stable per-type label even when detail is localized
        assertThat(ko.jsonPath().getString("title")).isEqualTo(en.jsonPath().getString("title"));
    }

    @Test
    @Tag("PROBLEM_DETAILS")
    @Tag("PROBLEM-OBSERVABILITY-001")
    void exposesBoundedLabelMeters() {
        auth().post("/api/problem-demo/insufficient-funds").then().statusCode(402);
        auth().header("Content-Type", "application/json").body("{\"fromAccount\":\"\",\"amount\":-5}")
            .post("/api/problem-demo/validate").then().statusCode(400);
        auth().post("/api/problem-demo/boom").then().statusCode(500);

        // (1) + (2): the total counter carries BOTH dimensions TOGETHER and is actually recorded —
        // a dimension-drop or zero-observation regression must NOT pass here.
        var fundsCounter = registry.find(ProblemMetrics.RESPONSES)
            .tag(ProblemMetrics.TAG_PROBLEM_TYPE, ProblemTypeRegistry.INSUFFICIENT_FUNDS)
            .tag(ProblemMetrics.TAG_STATUS_CLASS, "4xx").counter();
        assertThat(fundsCounter).as("problem_response_total{problem_type=insufficient-funds,status_class=4xx}").isNotNull();
        assertThat(fundsCounter.count()).isGreaterThanOrEqualTo(1.0);
        var serverCounter = registry.find(ProblemMetrics.RESPONSES)
            .tag(ProblemMetrics.TAG_PROBLEM_TYPE, ProblemTypeRegistry.SERVER_ERROR)
            .tag(ProblemMetrics.TAG_STATUS_CLASS, "5xx").counter();
        assertThat(serverCounter).as("problem_response_total{problem_type=server-error,status_class=5xx}").isNotNull();
        assertThat(serverCounter.count()).isGreaterThanOrEqualTo(1.0);

        // (3): the timing histogram carries status_class and is recorded.
        var timer4xx = registry.find(ProblemMetrics.RESPONSE_TIME).tag(ProblemMetrics.TAG_STATUS_CLASS, "4xx").timer();
        assertThat(timer4xx).as("problem_response_seconds{status_class=4xx}").isNotNull();
        assertThat(timer4xx.count()).isGreaterThanOrEqualTo(1L);

        Set<String> registeredSlugs = ProblemTypeRegistry.slugs();
        Set<String> statusClasses = Set.of("4xx", "5xx");

        // every RESPONSES meter MUST carry EXACTLY {problem_type, status_class} (no dropped dim,
        // no extra high-cardinality key); values bounded to the closed registry + status classes.
        for (Meter m : registry.find(ProblemMetrics.RESPONSES).meters()) {
            Set<String> keys = m.getId().getTags().stream()
                .map(io.micrometer.core.instrument.Tag::getKey).collect(java.util.stream.Collectors.toSet());
            assertThat(keys).as("RESPONSES meter tag keys")
                .containsExactlyInAnyOrder(ProblemMetrics.TAG_PROBLEM_TYPE, ProblemMetrics.TAG_STATUS_CLASS);
            for (io.micrometer.core.instrument.Tag t : m.getId().getTags()) {
                if (t.getKey().equals(ProblemMetrics.TAG_PROBLEM_TYPE)) {
                    assertThat(registeredSlugs).as("problem_type bounded to closed registry").contains(t.getValue());
                } else {
                    assertThat(statusClasses).as("status_class bounded").contains(t.getValue());
                }
            }
        }
        // every RESPONSE_TIME meter MUST carry EXACTLY {status_class}.
        for (Meter m : registry.find(ProblemMetrics.RESPONSE_TIME).meters()) {
            Set<String> keys = m.getId().getTags().stream()
                .map(io.micrometer.core.instrument.Tag::getKey).collect(java.util.stream.Collectors.toSet());
            assertThat(keys).as("RESPONSE_TIME meter tag keys").containsExactly(ProblemMetrics.TAG_STATUS_CLASS);
            assertThat(statusClasses).contains(m.getId().getTag(ProblemMetrics.TAG_STATUS_CLASS));
        }
    }
}
