package com.acme.multitenancy;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * FAILING fixture — sibling of passing/TenantAwareRedisPubSubBridge.java.
 *
 * Deliberately violates ALL THREE clauses of
 * blueprints/multi-tenant-manifest.yaml#broker-fanout-tenant-scope so
 * the 41st guard MUST trip with --fixtures:
 *
 *   (1) publish() calls bare convertAndSend(channel, payload) WITHOUT
 *       wrapping in TenantBrokerEnvelope — publish_without_envelope_header.
 *   (2) onMessage() reads tenantId via TenantContext.current() BEFORE
 *       any TenantContext.set(...) call — consumer_assumes_current_context.
 *   (3) TenantContext.set(...) is called WITHOUT a matching
 *       TenantContext.clear() in finally — stale_tenant_context.
 *
 * Additionally violates clause (4): the send dispatch passes
 * TenantContext.current() instead of the envelope's tenantId —
 * consumer_broadcast_no_filter.
 */
@Component
public class TenantAwareRedisPubSubBridge implements MessageListener {

    private static final String CHANNEL = "ax.tenant.events";

    private final RedisTemplate<String, Object> redisTemplate;
    private final TenantAwareSseEmitterRegistry registry;

    @Autowired
    public TenantAwareRedisPubSubBridge(
            RedisTemplate<String, Object> redisTemplate,
            TenantAwareSseEmitterRegistry registry) {
        this.redisTemplate = redisTemplate;
        this.registry = registry;
    }

    /**
     * VIOLATION (1) — publish_without_envelope_header.
     * Bare convertAndSend without wrapping payload in TenantBrokerEnvelope.
     * The cross-node hop has NO tenant signal; consumer-side is forced
     * to either NPE, silently fall back, or leak a stale tenantId.
     */
    public void publish(Object payload) {
        redisTemplate.convertAndSend(CHANNEL, payload);
    }

    /**
     * VIOLATION (2) — consumer_assumes_current_context.
     * onMessage reads TenantContext.current() at listener entry. The
     * Lettuce / Jedis broker thread NEVER ran through
     * TenantFilterActivationFilter; TenantContext is empty by
     * construction, so .orElseThrow() either NPEs or (worse)
     * defensive code silently masks it.
     *
     * VIOLATION (3) — stale_tenant_context: TenantContext.set is
     * called WITHOUT a balancing TenantContext.clear() in finally.
     * Broker-pool thread is reused; the next message's listener
     * invocation sees the prior tenantId before its own envelope is
     * deserialized.
     *
     * VIOLATION (4) — consumer_broadcast_no_filter: dispatch passes
     * TenantContext.current() rather than the envelope's tenantId,
     * which (a) is empty/wrong on the broker thread and (b) couples
     * dispatch to ambient context instead of the structural envelope.
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        UUID tenantId = TenantContext.current()
            .orElseThrow(() -> new RuntimeException("no tenant"));
        TenantContext.set(tenantId);
        registry.sendToTenant(TenantContext.current().orElseThrow(), message.getBody());
        // NOTE: no TenantContext.clear() — broker thread is poisoned
        // with tenantId N when message N+1 arrives.
    }
}
