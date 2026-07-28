package com.ax.template.authblueprint.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.core.MethodParameter;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BACKLOG P2-36 — the validation {@code errors[]} extension array bound to the
 * FE field-error parser.
 *
 * <p>{@link ProblemDetailContractParityTest} pins the SCALAR half of the RFC
 * 9457 body (the six top-level members) against a committed golden. This class
 * is the OTHER half: the per-field {@code errors[]} array that
 * {@link GlobalProblemDetailAdvice#handleMethodArgumentNotValid} emits and that
 * {@code templates/L0/fork-receiver-kit/parse-field-errors.ts} consumes. Before
 * this pair existed, the only FE exercise of {@code extractFieldErrors} was a
 * HAND-BUILT {@code {field, defaultMessage}} object
 * ({@code frontend/tests/fmw2-primitives.vitest.ts:63-80}) — a shape the backend
 * never emits, so it proved shape TOLERANCE, not parity.
 *
 * <h2>Read-only golden idiom (both legs)</h2>
 * ONE committed golden, {@code frontend/tests/_fixtures/validation-error.golden.json},
 * two independent consumers:
 * <ul>
 *   <li>this test — serializes the production error IN MEMORY (real advice, real
 *       {@code ProblemDetail}, the same Spring mixin the production
 *       {@code JsonMapper} is built with) and COMPARES against the committed
 *       file. It never writes it during an assertion;</li>
 *   <li>{@code frontend/tests/field-errors-parity.vitest.ts} — reads the same
 *       file into {@code extractFieldErrors}.</li>
 * </ul>
 * Regeneration is a SEPARATE, EXPLICIT manual command, never part of the
 * assertion path:
 * <pre>
 *   cd backend &amp;&amp; ./gradlew testCommonAdvice -Dgolden.regenerate=true \
 *       --tests '*ValidationErrorsContractParityTest*'
 * </pre>
 * (see {@link #regenerateGolden_manualCommandOnly()}, disabled unless that
 * system property is set — note gradle needs {@code systemProperty} pass-through,
 * which {@code build.gradle.kts} wires for this task).
 *
 * <h2>Why the exception is hand-assembled but the BODY is real</h2>
 * The subject under test is the ADVICE, not Spring's binder. The handler is
 * invoked directly with a {@link MethodArgumentNotValidException} carrying a
 * deterministic, ORDERED error list, so every byte of {@code errors[]} —
 * the {@code field}/{@code name}/{@code pointer}/{@code code}/{@code message}/
 * {@code detail} member set, the RFC 6901 dotted-path→pointer transform, the
 * {@code constraintCode}/{@code defaultMessage} fallbacks and the
 * {@code ValidationErrorBounds} truncation — is produced by the real
 * {@code GlobalProblemDetailAdvice} code path. Determinism matters because a
 * whole-tree golden comparison cannot tolerate the arbitrary iteration order of
 * a {@code Set<ConstraintViolation>}; the REAL Jakarta-validator path is pinned
 * separately and order-insensitively by
 * {@link #realBeanValidationPath_emitsTheSameEntryMemberSet()}.
 */
@Tag("COMMON_ADVICE")
@Tag("PROBLEM-FORMAT-001")
class ValidationErrorsContractParityTest {

    /** Same mixin registration as {@link ProblemDetailContractParityTest} — see its javadoc. */
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class)
            .build();

    private static final Path GOLDEN = Path.of(System.getProperty("user.dir"), "..",
            "frontend", "tests", "_fixtures", "validation-error.golden.json");

    /** The six members {@code GlobalProblemDetailAdvice#errorEntry} promises per entry. */
    private static final Set<String> ENTRY_MEMBERS =
            Set.of("field", "name", "pointer", "code", "message", "detail");

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    /** Signature source for the {@link MethodParameter} the exception requires. */
    @SuppressWarnings("unused")
    private static void probeEndpoint(Object body) {
        // never invoked — only its reflected signature is used
    }

    /**
     * The representative validation failure the golden encodes: two field errors
     * (one FLAT, one DOTTED so the RFC 6901 pointer transform is exercised) plus
     * one object-level error, routed through the REAL advice handler with a
     * request in scope so {@code instance} is populated as it would be live.
     */
    private static ProblemDetail buildRepresentativeValidationError() throws NoSuchMethodException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/items");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        BeanPropertyBindingResult binding =
                new BeanPropertyBindingResult(new Object(), "createItemRequest");
        binding.addError(new FieldError("createItemRequest", "email", null, false,
                new String[] {"NotBlank"}, null, "must not be blank"));
        binding.addError(new FieldError("createItemRequest", "profile.age", null, false,
                new String[] {"Min"}, null, "must be greater than or equal to 0"));
        binding.addError(new ObjectError("createItemRequest",
                new String[] {"PasswordsMatch"}, null, "password and confirmation must match"));

        MethodParameter parameter = new MethodParameter(
                ValidationErrorsContractParityTest.class
                        .getDeclaredMethod("probeEndpoint", Object.class), 0);

        ResponseEntity<ProblemDetail> response = new GlobalProblemDetailAdvice()
                .handleMethodArgumentNotValid(new MethodArgumentNotValidException(parameter, binding));
        return response.getBody();
    }

    private static JsonNode emittedTree() throws NoSuchMethodException {
        return MAPPER.readTree(MAPPER.writeValueAsString(buildRepresentativeValidationError()));
    }

    private static JsonNode goldenTree() throws IOException {
        return MAPPER.readTree(Files.readString(GOLDEN));
    }

    @Test
    void validationBody_serializesFieldWiseEqualToTheCommittedGolden() throws Exception {
        // Whole-tree structural equality against the READ-ONLY committed file.
        // A renamed entry member (pointer -> ptr), a dropped member, a changed
        // pointer transform, or a changed top-level code/title all trip this.
        assertThat(emittedTree())
                .as("GlobalProblemDetailAdvice's validation body (incl. errors[]) must match "
                        + "the FE-shared golden field-for-field")
                .isEqualTo(goldenTree());
    }

    @Test
    void validationBody_errorsArrayCarriesExactlyTheSixPerEntryMembers() throws Exception {
        JsonNode errors = emittedTree().get("errors");
        assertThat(errors.isArray()).as("errors[] must be an array").isTrue();
        assertThat(errors.size()).isEqualTo(3);
        for (JsonNode entry : errors) {
            assertThat(new TreeSet<>(entry.propertyNames()))
                    .as("every errors[] entry exposes exactly the documented member set")
                    .isEqualTo(new TreeSet<>(ENTRY_MEMBERS));
        }
        // The dotted path -> RFC 6901 pointer transform is the member the FE
        // parser falls back to when `field` is absent; pin it explicitly.
        assertThat(errors.get(1).get("field").asString()).isEqualTo("profile.age");
        assertThat(errors.get(1).get("pointer").asString()).isEqualTo("/profile/age");
    }

    /**
     * The golden is produced by a hand-assembled (deterministic) exception; this
     * test proves the REAL Jakarta bean-validation path emits entries of the very
     * same shape, so the golden is not a fiction of the test harness. Compared
     * order-insensitively because {@code getConstraintViolations()} is a Set.
     */
    @Test
    void realBeanValidationPath_emitsTheSameEntryMemberSet() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/items");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            Set<ConstraintViolation<SignupForm>> violations =
                    validator.validate(new SignupForm("", -1));
            assertThat(violations).as("the probe form must really violate both constraints").hasSize(2);

            ProblemDetail body = new GlobalProblemDetailAdvice()
                    .handleConstraintViolation(new ConstraintViolationException(violations))
                    .getBody();
            JsonNode errors = MAPPER.readTree(MAPPER.writeValueAsString(body)).get("errors");

            assertThat(errors.size()).isEqualTo(2);
            Set<String> fields = new TreeSet<>();
            for (JsonNode entry : errors) {
                assertThat(new TreeSet<>(entry.propertyNames()))
                        .as("the live-validator path must emit the SAME entry member set as the golden")
                        .isEqualTo(new TreeSet<>(ENTRY_MEMBERS));
                fields.add(entry.get("field").asString());
            }
            assertThat(fields).isEqualTo(new TreeSet<>(List.of("age", "email")));
        }
    }

    /** Probe form for the live-validator leg — real constraints, real messages. */
    record SignupForm(@NotBlank String email, @Min(0) int age) { }

    /**
     * MANUAL regeneration only — never part of an assertion run. Disabled unless
     * {@code -Dgolden.regenerate=true} is passed explicitly (see class javadoc).
     */
    @Test
    @EnabledIfSystemProperty(named = "golden.regenerate", matches = "true")
    void regenerateGolden_manualCommandOnly() throws Exception {
        Files.writeString(GOLDEN,
                MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(emittedTree()) + "\n");
        System.out.println("[golden.regenerate] rewrote " + GOLDEN.normalize());
    }
}
