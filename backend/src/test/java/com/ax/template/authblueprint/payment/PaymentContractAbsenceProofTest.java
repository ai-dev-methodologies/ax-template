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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

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
 * <h2>Claim 2 — the advertised {@code sortBy} query parameter is unbound</h2>
 * Contract block: {@code /paths/~1payments/get/parameters/2/schema} declares
 * {@code sortBy: [createdAt, updatedAt, amount]}, but {@code PaymentController#list} binds
 * only {@code page}/{@code size} and hard-codes its sort order, so no producer consumes
 * those tokens. Asserted against the RUNNING handler mapping: every {@code @RequestParam}
 * binding of every mapped controller method is enumerated from the live bean graph, so a
 * {@code sortBy} wired under any spelling — an alias on the annotation, a differently
 * named java parameter, a handler registered from configuration — flips it RED. That is
 * strictly more than the source grep can see, and the grep is retained beside it as a
 * cheap secondary floor.
 *
 * <p>Both claims are absence claims, so the assertions are deliberately phrased to fail
 * with the OFFENDING NAME rather than a bare boolean: when one of them goes red the fix is
 * to bind the contract block to the new producer, not to widen the exemption.
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
     * The query parameter {@code contracts/payment-openapi.yaml} advertises on
     * {@code GET /payments} and that nothing in the tree binds.
     */
    private static final String UNBOUND_QUERY_PARAM = "sortBy";

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
    void noControllerBindsTheAdvertisedSortByQueryParameter() {
        Set<String> bound = new TreeSet<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry
                : handlerMapping.getHandlerMethods().entrySet()) {
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

        assertThat(bound)
            .as("this sweep must see the real query-parameter surface for its absence claim "
                + "to mean anything (empty would make the assertion below vacuous)")
            .isNotEmpty();
        assertThat(bound)
            .as("contracts/payment-openapi.yaml advertises a `%s` query parameter on "
                + "GET /payments whose enum block is classified `unproduced` — nothing in "
                + "the tree consumes those tokens. A controller binding `%s` makes the "
                + "block PRODUCED, so it must be bound to that producer's accepted set "
                + "instead of carrying an absence proof",
                UNBOUND_QUERY_PARAM, UNBOUND_QUERY_PARAM)
            .doesNotContain(UNBOUND_QUERY_PARAM);
    }
}
