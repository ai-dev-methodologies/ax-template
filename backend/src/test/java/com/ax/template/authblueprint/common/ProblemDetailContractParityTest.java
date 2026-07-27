package com.ax.template.authblueprint.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S2.ERROR-CONTRACT.XB — frontend<->backend contract parity for the RFC 9457
 * {@code application/problem+json} body that {@link GlobalProblemDetailAdvice}
 * emits, mirroring the {@code S2.QUERY-BOUNDS.XB} pair
 * ({@link PageEnvelopeContractParityTest} +
 * {@code frontend/tests/page-envelope-parity.vitest.ts}): ONE committed golden
 * ({@code frontend/tests/_fixtures/problem-detail.golden.json}), two
 * independent consumers — this Jackson test (BE emission) and
 * {@code frontend/tests/problem-detail-parity.vitest.ts} (FE
 * {@code parseError()} consumption). Planted for exactly this gap by
 * CANARY-013.
 *
 * <h2>Why a raw {@code new ObjectMapper()} would have been a VACUOUS test</h2>
 * {@link ProblemDetail} is a Spring Framework class with bespoke wire
 * behaviour that a plain Jackson mapper does NOT reproduce on its own:
 * <ul>
 *   <li>its extension members ({@code code}, {@code errors}, ...) live in a
 *       {@code Map<String,Object>} behind {@link ProblemDetail#getProperties()}
 *       — without help, Jackson would serialize that as a NESTED
 *       {@code "properties": {...}} object, not flatten it onto the body;</li>
 *   <li>{@code type}/{@code instance} are omitted entirely when unset (both
 *       default to {@code null} — see {@code ProblemDetail}'s bytecode: its
 *       package-private {@code ProblemDetail(int)} constructor only assigns
 *       {@code status}, and {@code getType()}/{@code getInstance()} return the
 *       raw field with no {@code about:blank} fallback), which requires a
 *       {@code NON_EMPTY}/{@code NON_NULL} inclusion policy to actually omit
 *       them rather than emit {@code null}.</li>
 * </ul>
 * Spring Boot supplies BOTH behaviours to the app's real, shared
 * {@code JsonMapper} via one customizer — decompiled and confirmed present in
 * this project's resolved {@code spring-boot-jackson-4.1.0.jar}:
 * {@code JacksonAutoConfiguration$JsonProblemDetailsConfiguration
 * $ProblemDetailJsonMapperBuilderCustomizer.customize(JsonMapper.Builder)}
 * does exactly one thing —
 * {@code builder.addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class)}
 * (verified via {@code javap} on the shipped class file: the bytecode is a
 * single {@code ldc ProblemDetail; ldc ProblemDetailJacksonMixin; invokevirtual
 * addMixIn}). {@link ProblemDetailJacksonMixin} itself carries a class-level
 * {@code @JsonInclude(NON_EMPTY)} plus {@code @JsonAnyGetter}/
 * {@code @JsonAnySetter} on the properties map — the two behaviours above.
 * Registering that SAME public Spring mixin (below) — instead of hand-rolling
 * an approximation — is what makes this parity test reproduce the REAL
 * production wire shape rather than a plausible-looking guess.
 *
 * <p>Representative error chosen: {@link GlobalProblemDetailAdvice
 * #handleResourceNotFound} — the IDOR-safe-404 primitive every domain shares
 * (spec {@code problem-details-l0} PROBLEM-FORMAT-001). It is one of the few
 * {@code common} handlers that sets ALL SIX RFC 9457 members at once
 * ({@code type}/{@code title}/{@code status}/{@code detail}/{@code instance}/
 * {@code code}), so the golden exercises every field {@code parseError()}
 * could plausibly read. (Contrast: {@code ApprovalController}'s local 409
 * handler builds its {@code ProblemDetail} via
 * {@code ProblemDetail.forStatusAndDetail} WITHOUT calling {@code setType} —
 * {@code type} stays {@code null} and is correctly OMITTED from that body,
 * per {@link com.ax.template.authblueprint.approvalworkflow.ApprovalFlowIT}'s
 * {@code outOfOrder_directorBeforeTeamLead_returns409ProblemDetailShape} —
 * confirming the omit-when-unset behaviour this test's mixin registration
 * also relies on, just for the opposite case: a handler that DOES set every
 * member.)
 *
 * <p>{@code instance} requires a real servlet request in scope — supplied
 * here via a plain {@link MockHttpServletRequest} pushed onto
 * {@link RequestContextHolder} (no {@code @SpringBootTest}, no context boot,
 * zero ContextCache pressure; the same technique
 * {@code RequestBodySizeLimitFilterTest} already uses in this package).
 */
@Tag("COMMON_ADVICE")
@Tag("PROBLEM-FORMAT-001")
class ProblemDetailContractParityTest {

    /**
     * The exact mixin the production {@code JsonMapper} is built with — see
     * class javadoc. Reusing Spring's own public
     * {@link ProblemDetailJacksonMixin} (rather than re-deriving its
     * {@code @JsonInclude}/{@code @JsonAnyGetter} shape by hand) is what
     * anchors this test to the real emission path instead of an assumption
     * about it.
     */
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class)
            .build();

    private static JsonNode goldenTree() throws IOException {
        Path golden = Path.of(System.getProperty("user.dir"), "..", "frontend", "tests",
                "_fixtures", "problem-detail.golden.json");
        return MAPPER.readTree(Files.readString(golden));
    }

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * Builds the SAME representative error the golden fixture encodes: a
     * {@link ResourceNotFoundException} routed through the real
     * {@link GlobalProblemDetailAdvice#handleResourceNotFound} handler, with a
     * mocked request in scope so {@code instance} is populated exactly as it
     * would be for a live {@code GET /api/items/probe/not-found} call (see
     * {@code ProblemDetailProbeController}).
     */
    private static ProblemDetail buildRepresentativeError() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/items/probe/not-found");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        GlobalProblemDetailAdvice advice = new GlobalProblemDetailAdvice();
        ResponseEntity<ProblemDetail> response =
                advice.handleResourceNotFound(new ResourceNotFoundException("probe resource not found"));
        return response.getBody();
    }

    @Test
    void problemDetail_serializesFieldWiseEqualToTheGoldenFixture() throws IOException {
        ProblemDetail actualBody = buildRepresentativeError();
        JsonNode actual = MAPPER.readTree(MAPPER.writeValueAsString(actualBody));
        JsonNode expected = goldenTree();

        // Whole-tree structural equality — a renamed field, a re-nested
        // extension member (e.g. the vacuous-mapper "properties" wrapper this
        // test's mixin registration specifically prevents), an omitted member
        // that should be present (or vice versa), or a type change all trip
        // this single assertion.
        assertThat(actual)
                .as("GlobalProblemDetailAdvice's ProblemDetail JSON must match the FE-shared golden fixture field-for-field")
                .isEqualTo(expected);
    }

    @Test
    void problemDetail_hasExactlyTheSixRfc9457MembersWithCorrectTypes() throws IOException {
        JsonNode root = goldenTree();
        assertThat(root.size()).as("no extra/missing ProblemDetail members").isEqualTo(6);

        assertThat(root.get("type").isTextual()).isTrue();
        assertThat(root.get("title").isTextual()).isTrue();
        assertThat(root.get("status").isInt()).isTrue();
        assertThat(root.get("detail").isTextual()).isTrue();
        assertThat(root.get("instance").isTextual()).isTrue();
        assertThat(root.get("code").isTextual()).isTrue();

        assertThat(root.get("status").asInt()).isEqualTo(404);
        assertThat(root.get("code").asText()).isEqualTo("NOT_FOUND");
    }
}
