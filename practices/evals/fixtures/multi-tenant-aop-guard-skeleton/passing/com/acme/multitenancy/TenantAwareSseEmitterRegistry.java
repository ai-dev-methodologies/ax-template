package com.acme.multitenancy;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Generated from blueprints/multi-tenant-manifest.yaml#realtime-connection-tenant-scope.registry_per_message_set_clear.canonical_skeleton
 * with <root> = acme.
 *
 * Tenant-scoped SseEmitter registry. Three guard-checked properties:
 *   (1) register() reads tenantId from TenantContext.current() at HTTP-request
 *       thread time — NOT from a query parameter / client header.
 *   (2) sendToTenant filters the registry by tenantId equality before each
 *       send() — broadcast without filter is the cross-tenant push leak.
 *   (3) Per-message TenantContext.set / TenantContext.clear wraps each send()
 *       in try/finally so downstream serializers see the receiving tenant's
 *       context and the broker-pool thread is released clean.
 */
@Component
public class TenantAwareSseEmitterRegistry {

    private final ConcurrentHashMap<UUID, TenantBoundEmitter> emitters =
        new ConcurrentHashMap<>();

    /**
     * Called from a controller @GetMapping("/api/v1/events") handler.
     * TenantContext is populated by TenantFilterActivationFilter at this
     * point — resolving tenantId from any other source is the
     * client_supplied_tenant anti-pattern.
     */
    public SseEmitter register(SseEmitter emitter) {
        UUID tenantId = TenantContext.current()
            .orElseThrow(() -> new TenantContextMissingException(
                "TenantContext empty at SseEmitter register; "
                + "TenantFilterActivationFilter not in chain"));
        UUID emitterId = UUID.randomUUID();
        emitters.put(emitterId, new TenantBoundEmitter(tenantId, emitter));
        emitter.onCompletion(() -> emitters.remove(emitterId));
        emitter.onTimeout(()    -> emitters.remove(emitterId));
        emitter.onError(ex      -> emitters.remove(emitterId));
        return emitter;
    }

    /**
     * Broadcast filter — ONLY emitters bound to tenantId receive the payload.
     * Iterating emitters.values() and calling send() without filtering is the
     * broadcast_no_tenant_filter anti-pattern.
     */
    public void sendToTenant(UUID tenantId, Object payload) {
        for (TenantBoundEmitter bound : emitters.values()) {
            if (!bound.tenantId().equals(tenantId)) {
                continue;
            }
            try {
                TenantContext.set(bound.tenantId());
                bound.emitter().send(payload);
            } catch (IOException ex) {
                emitters.values().remove(bound);
            } finally {
                TenantContext.clear();
            }
        }
    }

    public record TenantBoundEmitter(UUID tenantId, SseEmitter emitter) {}
}
