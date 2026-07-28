package com.ax.template.authblueprint.payment;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.MethodParameter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * P0-2 round 2 — BYTECODE + RUNTIME proof of the two {@code unproduced} absence claims
 * that {@code contracts/payment-openapi.yaml} rests on.
 *
 * <p><b>Why this class exists.</b> Two enum blocks in that contract are classified
 * {@code wire_only} + {@code wire_source: unproduced} in
 * {@code practices/evals/contract-enum-map.yaml}: nothing in this tree produces them, so
 * there is no literal set to compare against. An absence claim is the one escape from set
 * equality the parity guard allows, and it used to be carried by source regexes alone. A
 * cross-family reviewer showed that is not sound — the probe for the callback SPI was
 * {@code (implements|extends|new)\s+PaymentCallbackVerifier\b}, which requires the
 * interface name IMMEDIATELY after {@code implements}, so an entirely ORDINARY
 * declaration defeats it:
 *
 * <pre>{@code
 * @Component
 * final class AcmeCallbackVerifier implements java.io.Serializable, PaymentCallbackVerifier { … }
 * }</pre>
 *
 * Spring registers that bean and the {@code {provider}} route serves a slug the contract
 * never declared, while the guard reports PASS. Tightening the regex only moves the hole
 * (a generic parameterisation, a nested or anonymous class, a {@code @Bean} factory
 * method, an implementation reached through an intermediate interface). Bytecode and the
 * running bean graph do not have that failure mode, so the claims are asserted there.
 *
 * <h2>Claim 1 — the {@code PaymentCallbackVerifier} SPI ships zero implementations</h2>
 * Contract block: {@code /paths/~1payments~1callback~1{provider}/post/parameters/0/schema}.
 * Asserted twice over:
 * <ul>
 *   <li>ARCHUNIT over the compiled main sources — {@code beAssignableTo} walks the type
 *       hierarchy, so it sees an implementation regardless of how it is DECLARED
 *       (position in the {@code implements} list, generics, nesting, anonymity);</li>
 *   <li>RUNTIME — the autowired {@link PaymentCallbackVerifierRegistry} is asserted empty,
 *       which additionally covers a bean contributed by a {@code @Bean} factory or by
 *       configuration, i.e. a producer with no implementing class of its own in this
 *       package at all.</li>
 * </ul>
 * The test-local {@code stubpg} verifier used by {@code PaymentCallbackSignatureFailIT} is
 * test-scoped: ArchUnit imports main sources only, and this class's context does not
 * register that stub. Registering a REAL verifier flips both assertions RED, which is
 * exactly the intent — the block must then be bound to that verifier's
 * {@code providerName()} instead of exempted.
 *
 * <h2>Claim 2 — {@code GET /payments} advertises no query parameter it does not bind</h2>
 * This claim used to be narrower: {@code /paths/~1payments/get/parameters/2/schema}
 * declared {@code sortBy: [createdAt, updatedAt, amount]} while {@code PaymentController
 * #list} bound only {@code page}/{@code size} and hard-coded {@code Sort.by(DESC,
 * "createdAt")}, so the test asserted that ONE name stayed unbound and the parity guard
 * carried the block as {@code unproduced}.
 *
 * <p>Round 4 of the same review showed that disposal was wrong. An {@code unproduced}
 * block escaped vocabulary checking entirely, so the parameter's tokens could be edited to
 * anything with no code change and nothing noticed — it was a closed vocabulary nobody
 * legislated and nobody enforced, on top of a parameter a client could send and silently
 * have ignored (200, {@code createdAt} ordering, no signal). The parameter was therefore
 * DELETED from the contract rather than exempted.
 *
 * <p>What is asserted here now is the general invariant that makes the deletion stick, and
 * it is strictly stronger than the old one: EVERY {@code in: query} parameter the contract
 * still declares for this operation — read from {@code contracts/payment-openapi.yaml} on
 * disk, not hard-coded here — MUST be bound by the RUNNING handler. The handler's
 * {@code @RequestParam} bindings are enumerated from the live {@link
 * RequestMappingHandlerMapping}, so a parameter wired under any spelling (an alias on the
 * annotation, a differently named java parameter) counts, and re-adding an advertised but
 * unimplemented parameter flips it RED regardless of what it is called.
 *
 * <p>Both claims fail with the OFFENDING NAME rather than a bare boolean: when claim 1
 * goes red the fix is to bind the contract block to the new producer rather than widen the
 * exemption, and when claim 2 goes red the fix is to implement the parameter or delete it
 * from the contract.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// R22 ContextCache lever — see BillingFlowIT / RateLimitComplianceTest for the eviction
// root cause. This class reads the bean graph rather than driving HTTP, but a fresh boot
// keeps it independent of whatever the aggregate evicted before it.
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class PaymentContractAbsenceProofTest {

    /** Main sources only — a test-scoped stub verifier must not satisfy (or break) the claim. */
    private static final JavaClasses MAIN_CLASSES = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("com.ax.template.authblueprint");

    /**
     * The contract this test reads its claims out of. Parsing the contract from disk rather
     * than restating it here is what keeps the assertion honest when the contract changes;
     * precedent in this tree: {@code RateLimitPingWireVocabularyTest},
     * {@code PageEnvelopeCatalogSweepTest}.
     */
    private static final Path CONTRACT = Path.of("..", "contracts", "payment-openapi.yaml");

    /** The runtime path of the operation the contract spells {@code /payments} (servers: /api). */
    private static final String LIST_PAYMENTS_PATH = "/api/payments";

    @Autowired
    PaymentCallbackVerifierRegistry callbackVerifierRegistry;

    // Qualified explicitly: actuator contributes a second RequestMappingHandlerMapping
    // (controllerEndpointHandlerMapping). The MVC one is the application's own surface.
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping handlerMapping;

    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-CALLBACK-ABSENCE-001")
    void noMainSourceClassImplementsThePaymentCallbackVerifierSpi() {
        // beAssignableTo (not `implement`) so the rule holds however the implementation is
        // declared: second in the implements list, generic, nested, anonymous, or reached
        // through an intermediate interface.
        ArchRule rule = noClasses()
            .that().areNotInterfaces()
            .should().beAssignableTo(PaymentCallbackVerifier.class)
            .because("contracts/payment-openapi.yaml classifies the callback {provider} enum "
                + "as `unproduced`: the public catalog ships ZERO PaymentCallbackVerifier "
                + "implementations by design (R26 — a fork-receiver registers its own PG). "
                + "An implementation here means the contract must be BOUND to its "
                + "providerName(), not exempted");
        rule.check(MAIN_CLASSES);
    }

    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-CALLBACK-ABSENCE-001")
    void runningRegistryPublishesNoCallbackProviderSlug() {
        assertThat(callbackVerifierRegistry.registeredProviders())
            .as("the RUNNING PaymentCallbackVerifier registry must publish no slug — a bean "
                + "contributed by a @Bean factory or by configuration has no implementing "
                + "class the bytecode rule above would name, but it still routes "
                + "POST /api/payments/callback/{provider} for a slug "
                + "contracts/payment-openapi.yaml does not declare")
            .isEmpty();
    }

    @Test
    @Tag("PAYMENT")
    @Tag("PAYMENT-SORT-ABSENCE-001")
    void everyAdvertisedListQueryParameterIsBoundByTheRunningHandler() {
        Set<String> advertised = contractQueryParametersOfListPayments();
        Set<String> bound = boundQueryParametersOf(LIST_PAYMENTS_PATH, RequestMethod.GET);

        assertThat(advertised)
            .as("contracts/payment-openapi.yaml must still declare query parameters for "
                + "GET /payments — an empty set would make the comparison below vacuous")
            .isNotEmpty();
        assertThat(bound)
            .as("the running handler for %s must expose its @RequestParam bindings — an "
                + "empty set would make the comparison below vacuous", LIST_PAYMENTS_PATH)
            .isNotEmpty();
        assertThat(bound)
            .as("every query parameter contracts/payment-openapi.yaml advertises on "
                + "GET /payments MUST be bound by the running handler. An advertised "
                + "parameter nothing consumes is a promise the server does not keep: the "
                + "client sends it, gets 200, and is silently ignored — which is exactly "
                + "what the deleted `sortBy` parameter did (PaymentController#list binds "
                + "page/size and hard-codes Sort.by(DESC, createdAt)). Implement the "
                + "parameter or delete it from the contract")
            .containsAll(advertised);
    }

    /** The `in: query` parameter names the contract declares for GET /payments, from disk. */
    @SuppressWarnings("unchecked")
    private static Set<String> contractQueryParametersOfListPayments() {
        Map<String, Object> doc;
        try (Reader reader = Files.newBufferedReader(CONTRACT, StandardCharsets.UTF_8)) {
            doc = new Yaml().load(reader);
        } catch (IOException ex) {
            throw new IllegalStateException("cannot read " + CONTRACT.toAbsolutePath(), ex);
        }
        Map<String, Object> paths = (Map<String, Object>) doc.get("paths");
        Map<String, Object> payments = (Map<String, Object>) paths.get("/payments");
        Map<String, Object> get = (Map<String, Object>) payments.get("get");
        Object parameters = get.get("parameters");
        Set<String> names = new TreeSet<>();
        if (parameters instanceof List<?> list) {
            for (Object raw : list) {
                Map<String, Object> parameter = (Map<String, Object>) raw;
                if ("query".equals(parameter.get("in"))) {
                    names.add(String.valueOf(parameter.get("name")));
                }
            }
        }
        return names;
    }

    /**
     * The @RequestParam names the RUNNING handler for {@code path} + {@code method} binds.
     *
     * <p>Read from the live {@link RequestMappingHandlerMapping} rather than from source, so
     * an alias on the annotation or a differently named java parameter still counts.
     */
    private Set<String> boundQueryParametersOf(String path, RequestMethod method) {
        Set<String> bound = new TreeSet<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry
                : handlerMapping.getHandlerMethods().entrySet()) {
            RequestMappingInfo info = entry.getKey();
            if (!info.getPatternValues().contains(path)
                || !info.getMethodsCondition().getMethods().contains(method)) {
                continue;
            }
            for (MethodParameter parameter : entry.getValue().getMethodParameters()) {
                RequestParam annotation = parameter.getParameterAnnotation(RequestParam.class);
                if (annotation == null) {
                    continue;
                }
                String declared = annotation.name().isEmpty() ? annotation.value() : annotation.name();
                if (!declared.isEmpty()) {
                    bound.add(declared);
                    continue;
                }
                parameter.initParameterNameDiscovery(
                    new org.springframework.core.DefaultParameterNameDiscoverer());
                String inferred = parameter.getParameterName();
                if (inferred != null) {
                    bound.add(inferred);
                }
            }
        }
        return bound;
    }
}
