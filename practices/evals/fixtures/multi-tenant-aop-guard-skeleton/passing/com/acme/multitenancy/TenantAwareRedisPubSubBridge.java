package com.acme.multitenancy;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.stereotype.Component;

/**
 * Generated from blueprints/multi-tenant-manifest.yaml#broker-fanout-tenant-scope.envelope_header_per_message_set_clear.canonical_skeleton
 * with <root> = acme.
 *
 * Tenant-scoped Redis Pub/Sub bridge for multi-node SSE fan-out.
 * Three guard-checked properties:
 *   (1) publish() wraps the payload in a TenantBrokerEnvelope carrying
 *       TenantContext.current() as a STRUCTURAL header — bare
 *       convertAndSend(channel, payload) is the
 *       publish_without_envelope_header anti-pattern.
 *   (2) onMessage() reads tenantId from the deserialized envelope —
 *       NOT from TenantContext.current() (which is empty on the
 *       Lettuce / Jedis broker-client thread by construction; it
 *       never ran through TenantFilterActivationFilter).
 *   (3) Per-message TenantContext.set / TenantContext.clear wraps
 *       the local sendToTenant dispatch in try/finally so downstream
 *       serializers see the receiving tenant's context and the
 *       broker-pool thread is released clean for the next message
 *       (which may belong to a different tenant).
 */
@Component
public class TenantAwareRedisPubSubBridge implements MessageListener {

    private static final String CHANNEL = "ax.tenant.events";

    private final RedisTemplate<String, TenantBrokerEnvelope> redisTemplate;
    private final TenantAwareSseEmitterRegistry registry;
    private final Jackson2JsonRedisSerializer<TenantBrokerEnvelope> envelopeSerializer;

    @Autowired
    public TenantAwareRedisPubSubBridge(
            RedisTemplate<String, TenantBrokerEnvelope> redisTemplate,
            TenantAwareSseEmitterRegistry registry,
            Jackson2JsonRedisSerializer<TenantBrokerEnvelope> envelopeSerializer) {
        this.redisTemplate = redisTemplate;
        this.registry = registry;
        this.envelopeSerializer = envelopeSerializer;
    }

    /**
     * Called from an @EventListener / service-layer publisher.
     * TenantContext is populated by the originating request thread —
     * resolving tenantId from any other source (config default,
     * system tenant) is FORBIDDEN. The envelope IS the wire-format
     * tenant signal across the cross-node hop.
     */
    public void publish(Object payload) {
        UUID tenantId = TenantContext.current()
            .orElseThrow(() -> new TenantContextMissingException(
                "TenantContext empty at broker publish; "
                + "publisher MUST run inside a tenant-scoped request"));
        TenantBrokerEnvelope envelope = new TenantBrokerEnvelope(tenantId, payload);
        redisTemplate.convertAndSend(CHANNEL, envelope);
    }

    /**
     * Redis MessageListener entrypoint — runs on the Lettuce / Jedis
     * broker-client thread which has empty TenantContext by
     * construction.
     *
     * Tenant signal is the ENVELOPE, never TenantContext.current().
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        TenantBrokerEnvelope envelope =
            envelopeSerializer.deserialize(message.getBody());
        if (envelope == null || envelope.tenantId() == null) {
            // Reject malformed messages explicitly — falling through
            // to a bare emitters.forEach would leak across tenants.
            throw new TenantContextMissingException(
                "Broker message lacks tenantId envelope header");
        }
        try {
            // MUST set: downstream serializers / @JsonView resolvers /
            // @AuthorizedTenant-annotated getters reached during
            // sendToTenant may call TenantContext.current().
            TenantContext.set(envelope.tenantId());
            // MUST pass envelope.tenantId() EXPLICITLY — passing
            // TenantContext.current() here would be a tautology that
            // the guard still rejects because the call must also be
            // tenantId-equality-filtered downstream by the registry.
            registry.sendToTenant(envelope.tenantId(), envelope.payload());
        } finally {
            // MUST clear: broker-pool thread is reused across
            // messages. Leaving tenant N's context set for tenant N+1's
            // onMessage is the stale_tenant_context vector.
            TenantContext.clear();
        }
    }

    /**
     * Wire-format envelope. tenantId is a STRUCTURAL header — it
     * travels alongside the payload regardless of payload shape.
     * Tolerated variants:
     *   - Spring Message header / Kafka ProducerRecord header
     *   - envelope record where tenantId precedes payload field
     * Forbidden:
     *   - payload-only messages where tenantId is buried inside the
     *     domain object (parser-dependent, brittle, indistinguishable
     *     from absence on a malformed payload).
     */
    public record TenantBrokerEnvelope(UUID tenantId, Object payload) {}
}
