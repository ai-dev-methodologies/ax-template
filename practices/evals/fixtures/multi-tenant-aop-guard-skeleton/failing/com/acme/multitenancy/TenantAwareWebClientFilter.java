package com.acme.multitenancy;

import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * FAILING fixture for the 46th hard guard
 * (#webclient-async-tenant-scope). Each of the four
 * clauses is intentionally violated so the guard's
 * detection regex is exercised:
 *
 *   Clause (1) tenant_header_dropped_on_reactive_forward:
 *       the filter passes the request to next.exchange
 *       WITHOUT setting the X-Tenant-Id header.
 *
 *   Clause (2) thread_local_in_reactive_chain: the filter
 *       calls TenantContext.current() DIRECTLY on the
 *       Reactor scheduler thread (where the ThreadLocal is
 *       empty in the best case, or holds a STALE prior
 *       request's tenant in a pooled-scheduler worst case).
 *
 *   Clause (3) reactor_context_missing: there is NO
 *       Mono.deferContextual / ContextView read anywhere in
 *       the file — the canonical Reactor-safe tenant
 *       carrier is bypassed entirely.
 *
 *   Clause (4) thread_local_inside_chain_lambda: the
 *       Mono.flatMap lambda calls TenantContext.set(...)
 *       on the Reactor scheduler thread. Reactor reuses
 *       scheduler workers across subscriptions; the set()
 *       call pollutes the worker's ThreadLocal for the
 *       NEXT subscription that lands on the same worker —
 *       canonical cross-tenant leak.
 */
@Component
public class TenantAwareWebClientFilter implements ExchangeFilterFunction {

    static final String TENANT_HEADER_TYPO = "Tenant-Id";

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        // Clause (2) + (3) violation: reads TenantContext directly
        // on the Reactor scheduler thread (ThreadLocal empty / stale).
        // NO Mono.deferContextual anywhere — the Reactor Context API
        // is bypassed entirely.
        UUID stolenTenantId = TenantContext.current().orElse(null);

        // Clause (4) violation: TenantContext.set inside a chain
        // lambda. Reactor scheduler workers are reused across
        // subscriptions; this leaks the tenantId into the worker's
        // ThreadLocal for the NEXT subscription.
        return next.exchange(request)
            .flatMap(response -> {
                if (stolenTenantId != null) {
                    TenantContext.set(stolenTenantId);
                }
                return Mono.just(response);
            });
        // Clause (1) violation: NO X-Tenant-Id header set on the
        // outbound ClientRequest. The receiving service sees no
        // tenant signal; cross-process tenant scope is lost.
    }
}
