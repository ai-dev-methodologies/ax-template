package com.acme.multitenancy;

import java.util.UUID;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * Generated from blueprints/multi-tenant-manifest.yaml#webclient-async-tenant-scope.tenant_context_propagated_webclient_filter.canonical_skeleton
 * with <root> = acme.
 *
 * Reactive third-party API fan-out OBVERSE of
 * TenantAwareStandbyForwardingService (R12
 * #kafka-streams-standby-rpc-tenant-scope). R12 closes the
 * blocking RestTemplate router; R13 closes the async
 * WebFlux WebClient counterpart where the forward primitive
 * is reactive and the ThreadLocal-backed TenantContext is
 * NOT live on the Reactor scheduler thread that runs the
 * filter and the response handler.
 *
 * The structural problem: TenantContext.CURRENT is a plain
 * ThreadLocal (see TenantContext.java) that
 * TenantFilterActivationFilter populates on the servlet
 * request thread. WebClient subscriptions run on the
 * Reactor parallel / boundedElastic / single scheduler —
 * a DIFFERENT thread from the servlet request thread. By
 * the time the ExchangeFilterFunction runs,
 * TenantContext.current() is empty. The canonical
 * propagation channel is Reactor's Context API
 * (Mono.contextWrite at the controller / Mono.deferContextual
 * inside the filter) — NOT ThreadLocal.
 *
 * Four guard-checked properties:
 *   (1) X-Tenant-Id header on every reactive forward: the
 *       ExchangeFilterFunction MUST set an X-Tenant-Id
 *       header whose value is the tenantId carried in the
 *       Reactor Context. Without it, the receiving service
 *       sees no tenant signal — same surface as R12
 *       clause (1) at the async-router layer. The header
 *       name MUST match #aop-guard.tenant_header_canonical_name.
 *   (2) Reactor Context is the SOLE tenant propagation
 *       channel inside the filter. The filter MUST call
 *       Mono.deferContextual(ctx -> ...) or read a
 *       ContextView to extract the tenantId. Calling
 *       TenantContext.current() DIRECTLY inside the filter
 *       returns Optional.empty (the filter is on a Reactor
 *       scheduler thread; the servlet thread's ThreadLocal
 *       is not visible). The R8 #async-propagation
 *       structural rule ("ThreadLocal does not cross thread
 *       hops without explicit propagation") applied to the
 *       reactive-scheduler case.
 *   (3) tenantId injected into Reactor Context MUST derive
 *       from TenantContext.current() at the request-thread
 *       boundary — NEVER from a @PathVariable /
 *       @RequestParam / @RequestBody field. The
 *       controller's Mono.contextWrite call lives on the
 *       servlet thread where TenantContext.current() IS
 *       populated; that captured value is the only tenant
 *       signal that flows into the subscription. Path /
 *       query / body tenantId is validated for equality
 *       against the context tenantId and discarded — same
 *       IDOR-vector closure as R12 clause (2) at the async
 *       layer.
 *   (4) No ThreadLocal access inside the reactive chain.
 *       The filter MUST NOT call TenantContext.current() /
 *       TenantContext.set() / TenantContext.clear() inside
 *       any Mono.flatMap / Mono.map / Mono.doOnNext /
 *       Mono.subscribe lambda. The chain hops schedulers
 *       (publishOn / subscribeOn) between operators; a
 *       ThreadLocal call inside the chain sees an unrelated
 *       thread's TenantContext (empty in the best case, the
 *       PREVIOUS request's tenant in a pooled-scheduler
 *       worst case → cross-tenant leak). Reactor Context is
 *       the only chain-safe carrier.
 *
 * Distinct surface from TenantAwareStandbyForwardingService
 * (R12): R12 forwards via blocking RestTemplate on the
 * servlet thread where TenantContext.current() IS populated;
 * this filter forwards via reactive WebClient on a Reactor
 * scheduler thread where TenantContext.current() is empty.
 * Both files coexist in the multi-tenant fixture (R12 is
 * the 19th canonical file, R13 is the 20th).
 */
@Component
public class TenantAwareWebClientFilter implements ExchangeFilterFunction {

    /**
     * Canonical Reactor Context key for the tenant
     * identifier. The controller writes this key on the
     * servlet thread (where TenantContext.current() is
     * populated by TenantFilterActivationFilter); this
     * filter reads it on the Reactor scheduler thread via
     * Mono.deferContextual.
     */
    public static final String TENANT_ID_CONTEXT_KEY = "tenantId";

    /**
     * X-Tenant-Id is the canonical cross-process tenant
     * carrier; matches #aop-guard.tenant_header_canonical_name
     * so the receiving service's TenantFilterActivationFilter
     * recognises it.
     */
    static final String TENANT_HEADER = "X-Tenant-Id";

    /**
     * Filter implementation.
     *
     * Property (2): Mono.deferContextual is the ONLY tenant
     *               extraction point. NEVER call
     *               TenantContext.current() inside this
     *               method or any of its lambdas.
     * Property (1): every outbound request gets the
     *               X-Tenant-Id header before next.exchange.
     * Property (4): the request mutation happens in a pure
     *               builder chain — no ThreadLocal access
     *               anywhere in the reactive pipeline.
     */
    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        return Mono.deferContextual(ctx -> {
            if (!ctx.hasKey(TENANT_ID_CONTEXT_KEY)) {
                return Mono.error(new TenantContextMissingException(
                    "WebClient invoked outside a tenant-scoped subscription — "
                    + "controller MUST wrap the publisher with "
                    + ".contextWrite(Context.of(TENANT_ID_CONTEXT_KEY, "
                    + "TenantContext.current().orElseThrow())) on the "
                    + "servlet request thread before the WebClient call."));
            }
            UUID tenantId = ctx.get(TENANT_ID_CONTEXT_KEY);
            ClientRequest stamped = ClientRequest.from(request)
                .header(TENANT_HEADER, tenantId.toString())
                .build();
            return next.exchange(stamped);
        });
    }

    /**
     * Wires this filter into a WebClient.Builder. Static
     * factory keeps the wiring contract explicit; consumers
     * MUST go through {@link WebClient.Builder#filter} so
     * every WebClient instance in the fork-receiver picks
     * up the tenant header automatically.
     */
    public WebClient.Builder register(
            WebClient.Builder builder,
            ClientHttpConnector connector) {
        return builder
            .clientConnector(connector)
            .filter(this);
    }

    /**
     * Controller-side helper: derives the Reactor Context
     * entry from TenantContext.current() on the servlet
     * thread. The controller wraps its publisher chain
     * with this Context so the filter (running on a
     * Reactor scheduler thread) can read the tenantId
     * back via Mono.deferContextual.
     *
     * Property (3): the tenantId source is
     * TenantContext.current() — NEVER a request parameter.
     */
    public static Context tenantContextFor(UUID pathTenantId) {
        UUID contextTenantId = TenantContext.current()
            .orElseThrow(() -> new TenantContextMissingException(
                "Controller invoked without TenantContext — "
                + "TenantFilterActivationFilter must populate the context "
                + "before any WebClient call."));
        if (pathTenantId != null && !pathTenantId.equals(contextTenantId)) {
            throw new TenantBoundaryViolationException(
                "Path tenantId does not match authenticated tenant");
        }
        return Context.of(TENANT_ID_CONTEXT_KEY, contextTenantId);
    }
}
