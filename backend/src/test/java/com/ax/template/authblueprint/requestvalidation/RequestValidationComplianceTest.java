package com.ax.template.authblueprint.requestvalidation;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.restassured.path.json.JsonPath;
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
 * request-validation-l0 compliance — every item verified against the live reference workload.
 * Domain @Tag("REQUEST_VALIDATION") drives ./gradlew testRequestValidation; the per-item @Tag binds
 * the spec item to its test (spec_item_verification_binding guard). Spec: specs/request-validation-l0.yaml.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RequestValidationComplianceTest {

    @LocalServerPort
    int port;

    @Autowired
    MeterRegistry registry;

    String token;

    @BeforeEach
    void setUp() {
        String email = "reqval-" + UUID.randomUUID() + "@example.com";
        given().header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\",\"role\":\"MEMBER\"}")
            .when().post("/api/auth/email/signup");
        token = given().header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\"}")
            .when().post("/api/auth/email/login").then().extract().path("accessToken");
    }

    private io.restassured.specification.RequestSpecification auth() {
        return given().header("Authorization", "Bearer " + token).header("Content-Type", "application/json");
    }

    /** A fully-valid order body; tests mutate one facet at a time. */
    private static String validBody() {
        return "{"
            + "\"customer\":\"Acme\","
            + "\"amount\":100,"
            + "\"priority\":\"HIGH\","
            + "\"startDate\":\"2026-01-01\","
            + "\"endDate\":\"2026-12-31\","
            + "\"address\":{\"postalCode\":\"12345\",\"city\":\"Seoul\"},"
            + "\"items\":[{\"sku\":\"A1\",\"quantity\":2}]"
            + "}";
    }

    @Test
    @Tag("REQUEST_VALIDATION")
    @Tag("VALIDATION-SCHEMA-001")
    void declaredContractRejectsAnEmptyBodyBeforeBusinessLogic() {
        // the controller declares @Valid; an empty body fails the declared schema → 400 (not 201)
        JsonPath body = auth().body("{}").post("/api/request-validation/orders")
            .then().statusCode(400).extract().jsonPath();
        // multiple required-field violations reported declaratively
        assertThat(body.getList("errors")).hasSizeGreaterThanOrEqualTo(3);
        // a fully-valid body is accepted (the contract is real, not reject-everything)
        auth().body(validBody()).post("/api/request-validation/orders").then().statusCode(201);
    }

    @Test
    @Tag("REQUEST_VALIDATION")
    @Tag("VALIDATION-TYPE-001")
    void strictTypingRejectsCoercionUnknownFieldsAndUnlistedEnum() {
        // number field rejects a JSON string — NO silent coercion of "100" → 100
        auth().body(validBody().replace("\"amount\":100", "\"amount\":\"100\""))
            .post("/api/request-validation/orders").then().statusCode(400);
        // unlisted enum token rejected
        auth().body(validBody().replace("\"priority\":\"HIGH\"", "\"priority\":\"URGENT\""))
            .post("/api/request-validation/orders").then().statusCode(400);
        // unknown/typo'd field rejected (reject-unknown), not silently dropped — and proven to be
        // the DECLARATIVE @AssertTrue path (code AssertTrue on the no-unknown-fields check), not a
        // coincidental 400 from another cause
        List<String> codes = auth()
            .body(validBody().replace("\"customer\":\"Acme\",", "\"customer\":\"Acme\",\"customerr\":\"typo\","))
            .post("/api/request-validation/orders").then().statusCode(400)
            .extract().jsonPath().getList("errors.code", String.class);
        assertThat(codes).contains("AssertTrue");
    }

    @Test
    @Tag("REQUEST_VALIDATION")
    @Tag("VALIDATION-CONSTRAINT-001")
    void standardAndCustomCrossFieldConstraintsFireWithStableCodes() {
        // blank customer (@NotBlank) + cross-field DateRange (startDate after endDate)
        String bad = validBody()
            .replace("\"customer\":\"Acme\"", "\"customer\":\"\"")
            .replace("\"startDate\":\"2026-01-01\"", "\"startDate\":\"2026-12-31\"")
            .replace("\"endDate\":\"2026-12-31\"", "\"endDate\":\"2026-01-01\"");
        JsonPath body = auth().body(bad).post("/api/request-validation/orders")
            .then().statusCode(400).extract().jsonPath();
        List<String> codes = body.getList("errors.code");
        assertThat(codes).contains("NotBlank");           // built-in standard constraint
        assertThat(codes).contains("DateRange");          // class-level custom cross-field validator
        // the custom rule reports on a specific node, not the whole object
        assertThat(body.getList("errors.pointer", String.class)).contains("/endDate");
    }

    @Test
    @Tag("REQUEST_VALIDATION")
    @Tag("VALIDATION-NESTED-001")
    void nestedObjectAndEveryCollectionElementValidatedWithIndexedPointers() {
        String bad = validBody()
            .replace("\"address\":{\"postalCode\":\"12345\",\"city\":\"Seoul\"}",
                     "\"address\":{\"postalCode\":\"abc\",\"city\":\"Seoul\"}")
            .replace("\"items\":[{\"sku\":\"A1\",\"quantity\":2}]",
                     "\"items\":[{\"sku\":\"A1\",\"quantity\":2},{\"sku\":\"\",\"quantity\":0},{\"sku\":\"B\",\"quantity\":-1}]");
        JsonPath body = auth().body(bad).post("/api/request-validation/orders")
            .then().statusCode(400).extract().jsonPath();
        List<String> pointers = body.getList("errors.pointer", String.class);
        // nested object member
        assertThat(pointers).contains("/address/postalCode");
        // EVERY violating collection element, located by index (not just the first)
        assertThat(pointers).contains("/items/1/sku", "/items/1/quantity", "/items/2/quantity");
    }

    /**
     * DEFENSE 2 — response-amplification bound. A request carrying MANY violations must not inflate
     * the response: the errors[] array is capped at {@code ValidationErrorBounds.MAX_FIELD_ERRORS}
     * (10) and an {@code errorsTruncated} flag signals the cap. 15 invalid items → 30 element errors
     * → response holds exactly 10, flagged truncated.
     */
    @Test
    @Tag("REQUEST_VALIDATION")
    void manyValidationErrorsAreCountCapped() {
        StringBuilder items = new StringBuilder("[");
        for (int i = 0; i < 15; i++) {
            if (i > 0) items.append(',');
            items.append("{\"sku\":\"\",\"quantity\":0}"); // @NotBlank + @Positive → 2 errors each
        }
        items.append(']');
        String bad = validBody().replace("\"items\":[{\"sku\":\"A1\",\"quantity\":2}]",
                "\"items\":" + items);
        JsonPath body = auth().body(bad).post("/api/request-validation/orders")
            .then().statusCode(400)
            .body("errorsTruncated", org.hamcrest.Matchers.equalTo(true))
            .extract().jsonPath();
        assertThat(body.getList("errors").size())
            .as("errors[] array is count-capped at the amplification bound")
            .isEqualTo(10);
    }

    @Test
    @Tag("REQUEST_VALIDATION")
    @Tag("VALIDATION-SANITIZE-001")
    void rejectsMalformedRatherThanSilentlySanitizingAndNormalizesViaAllowlist() {
        // reject-not-sanitize: a malformed postalCode is REJECTED (400), never stripped/cleaned to pass
        auth().body(validBody().replace("\"postalCode\":\"12345\"", "\"postalCode\":\"12a45\""))
            .post("/api/request-validation/orders").then().statusCode(400);

        // the ONLY transform on accepted input is the explicit allowlist NFC-normalize + trim.
        // Input is GENUINELY decomposed (NFD: 'e' + U+0301 combining acute) with surrounding spaces,
        // so a passing assertion PROVES the bytes were transformed (composed) - not a no-op.
        String decomposed = "  Cafe\u0301  ";          // NFD: 'e' + combining acute
        String composed = "Caf\u00e9";                  // NFC: precomposed e-acute
        assertThat(decomposed.trim()).isNotEqualTo(composed); // guard: the input really is decomposed
        String echoed = auth().body(validBody().replace("\"customer\":\"Acme\"", "\"customer\":\"" + decomposed + "\""))
            .post("/api/request-validation/orders").then().statusCode(201).extract().jsonPath().getString("customer");
        assertThat(echoed).isEqualTo(composed);             // NFC composed the decomposed input
        assertThat(echoed).doesNotStartWith(" ").doesNotEndWith(" "); // trimmed
    }

    @Test
    @Tag("REQUEST_VALIDATION")
    @Tag("VALIDATION-ERROR-001")
    void failuresUseTheSharedRfc9457ErrorsArrayWithCodeNotACompetingEnvelope() {
        var r = auth().body(validBody().replace("\"customer\":\"Acme\"", "\"customer\":\"\""))
            .post("/api/request-validation/orders").then().statusCode(400).extract();
        assertThat(r.contentType()).startsWith("application/problem+json");
        JsonPath body = r.jsonPath();
        assertThat(body.getString("type")).isNotBlank();        // RFC 9457 envelope, not a custom one
        // the SAME errors[] extension array problem-details defines, with an added code per entry
        List<?> errors = body.getList("errors");
        assertThat(errors).isNotEmpty();
        assertThat(body.getList("errors.pointer", String.class)).allSatisfy(p -> assertThat(p).startsWith("/"));
        assertThat(body.getList("errors.detail", String.class)).allSatisfy(d -> assertThat(d).isNotBlank());
        assertThat(body.getList("errors.code", String.class)).allSatisfy(c -> assertThat(c).isNotBlank());
        // NOT a competing envelope: no top-level concatenated 'violations' string field
        assertThat(body.getString("violations")).isNull();
    }

    @Test
    @Tag("REQUEST_VALIDATION")
    @Tag("VALIDATION-OBSERVABILITY-001")
    void exposesBoundedLabelMetersWithCollapsedCollectionIndices() {
        // a collection-element failure at index 2 — its metric label MUST be the template, not /items/2/...
        auth().body(validBody().replace("\"items\":[{\"sku\":\"A1\",\"quantity\":2}]",
                "\"items\":[{\"sku\":\"A1\",\"quantity\":2},{\"sku\":\"A2\",\"quantity\":2},{\"sku\":\"B\",\"quantity\":-1}]"))
            .post("/api/request-validation/orders").then().statusCode(400);

        // index collapsed to {index} — bounded cardinality despite the concrete index 2
        var collapsed = registry.find(RequestValidationMetrics.FAILURES)
            .tag(RequestValidationMetrics.TAG_FIELD, "/items/{index}/quantity")
            .tag(RequestValidationMetrics.TAG_CODE, "Positive").counter();
        assertThat(collapsed).as("validation_failure_total{field=/items/{index}/quantity,code=Positive}").isNotNull();
        assertThat(collapsed.count()).isGreaterThanOrEqualTo(1.0);

        var rejected = registry.find(RequestValidationMetrics.REJECTED)
            .tag(RequestValidationMetrics.TAG_STATUS_CLASS, "4xx").counter();
        assertThat(rejected).as("request_rejected_total{status_class=4xx}").isNotNull();
        assertThat(rejected.count()).isGreaterThanOrEqualTo(1.0);

        // every failures meter carries EXACTLY {field, code}; field bounded to the closed template set,
        // and NO label leaks an offending value or a raw numeric index.
        Set<String> declared = RequestValidationMetrics.DECLARED_FIELD_TEMPLATES;
        for (Meter m : registry.find(RequestValidationMetrics.FAILURES).meters()) {
            Set<String> keys = m.getId().getTags().stream()
                .map(io.micrometer.core.instrument.Tag::getKey).collect(java.util.stream.Collectors.toSet());
            assertThat(keys).containsExactlyInAnyOrder(RequestValidationMetrics.TAG_FIELD, RequestValidationMetrics.TAG_CODE);
            String fieldLabel = m.getId().getTag(RequestValidationMetrics.TAG_FIELD);
            assertThat(declared).as("field label bounded to closed templates").contains(fieldLabel);
            assertThat(fieldLabel).as("no raw numeric index in label").doesNotMatch(".*/\\d+(/|$).*");
        }
    }
}
