package com.ax.template.authblueprint.payment;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Routing layer that maps a {@code {provider}} path slug to the matching
 * {@link PaymentCallbackVerifier} bean.
 *
 * <p>Spec anchors:
 * <ul>
 *   <li>specs/payment-l0.yaml#PAYMENT-CALLBACK-001 — signature verifier dispatch
 *       MUST be deterministic per (provider) slug.</li>
 *   <li>blueprints/payment-manifest.yaml#callback — names the registry as the
 *       composition point between the controller and the per-PG verifier
 *       implementations.</li>
 *   <li>blueprints/payment-manifest.yaml#provider.type_allowed — the slug set
 *       this registry's keys MUST be a subset of (enforced at runtime by
 *       {@code practices/evals/payment_provider_type_enum_guard.sh} for the
 *       static enum side; dynamic side ships with redirect-style adapters).</li>
 * </ul>
 *
 * <p>Discovery is via Spring autowiring of the full {@link List} of
 * {@link PaymentCallbackVerifier} beans. Each verifier declares its own slug
 * via {@link PaymentCallbackVerifier#providerName()}. Duplicate slugs cause a
 * fail-fast {@link IllegalStateException} at context initialization so a
 * mis-wired adapter is surfaced at startup, NOT at the first inbound callback.
 *
 * <p>An empty registry (no redirect-style verifier registered) is a valid
 * deployment shape — the catalog ships with tokenization-style flows only.
 * In that case every callback request resolves to {@link Optional#empty()} and
 * the controller returns HTTP 404 without touching any payment state.
 */
@Component
public class PaymentCallbackVerifierRegistry {

    private final Map<String, PaymentCallbackVerifier> byProvider;

    public PaymentCallbackVerifierRegistry(List<PaymentCallbackVerifier> verifiers) {
        Map<String, PaymentCallbackVerifier> index = new HashMap<>();
        for (PaymentCallbackVerifier v : verifiers) {
            String slug = v.providerName();
            if (slug == null || slug.isBlank()) {
                throw new IllegalStateException(
                    "PaymentCallbackVerifier returned null/blank providerName(): "
                    + v.getClass().getName()
                    + " — see blueprints/payment-manifest.yaml#callback");
            }
            PaymentCallbackVerifier previous = index.putIfAbsent(slug, v);
            if (previous != null) {
                throw new IllegalStateException(
                    "Duplicate PaymentCallbackVerifier slug '" + slug + "': "
                    + previous.getClass().getName() + " vs " + v.getClass().getName()
                    + " — providerName() MUST be unique across the registry "
                    + "(blueprints/payment-manifest.yaml#provider.type_allowed)");
            }
        }
        this.byProvider = Map.copyOf(index);
    }

    /**
     * @return verifier registered under the given slug, or empty when none
     *         matches. The controller treats empty as 404 (unknown provider).
     */
    public Optional<PaymentCallbackVerifier> find(String providerSlug) {
        if (providerSlug == null || providerSlug.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byProvider.get(providerSlug));
    }

    /**
     * @return immutable snapshot of registered slugs. Exposed for diagnostics
     *         (e.g. an actuator endpoint or a health probe), NOT for routing.
     */
    public java.util.Set<String> registeredProviders() {
        return java.util.Set.copyOf(byProvider.keySet());
    }
}
