package com.acme.multitenancy;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Generated from blueprints/multi-tenant-manifest.yaml#kafka-consumer-tenant-scope.shared_topic_header_per_record_set_clear.canonical_skeleton
 * with <root> = acme.
 *
 * Tenant-scoped Kafka business-event consumer for interleaved
 * multi-tenant traffic on a shared topic. Five guard-checked
 * properties:
 *   (1) X-Tenant-Id header read INSIDE the batch for-loop —
 *       NOT once at method entry. Each ConsumerRecord carries
 *       its own header; the batch may interleave tenants.
 *   (2) TenantContext.set + clear wrapped in try/finally
 *       INSIDE the for-loop. Record N's context MUST NOT leak
 *       into record N+1's handler.
 *   (3) Rebalance callbacks (onPartitionsRevokedBeforeCommit /
 *       onPartitionsAssigned) NEVER touch TenantContext — the
 *       poll thread has no tenant signal between batches.
 *   (4) Manual Acknowledgment runs OUTSIDE the per-record
 *       set/clear span — batch-level ack after the for-loop.
 *   (5) Partition key is a producer-side ordering decision;
 *       the consumer trusts ONLY the X-Tenant-Id header.
 *
 * Distinct from TenantAwareRedisPubSubBridge (R8): that bridge
 * dispatches into the local SSE registry (push-out direction);
 * this consumer processes domain events and invokes
 * service-layer business code under per-record tenant context
 * (in-direction).
 */
@Component
public class TenantAwareKafkaConsumer implements ConsumerAwareRebalanceListener {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String TOPIC = "ax.tenant.events";

    private final TenantEventHandler handler;

    public TenantAwareKafkaConsumer(TenantEventHandler handler) {
        this.handler = handler;
    }

    /**
     * Batch poll listener. Each invocation receives a list of
     * ConsumerRecords that MAY interleave tenants — the poll
     * returns whatever is buffered on the assigned partitions,
     * and a shared topic carries all tenants' events.
     *
     * The X-Tenant-Id header is the structural tenant signal;
     * setting TenantContext once before the loop is the
     * batch_set_once anti-pattern (every record after the
     * first runs under the wrong tenant).
     */
    @KafkaListener(topics = TOPIC, containerFactory = "kafkaListenerContainerFactory")
    public void onBatch(List<ConsumerRecord<String, Object>> records,
                        Acknowledgment ack) {
        for (ConsumerRecord<String, Object> record : records) {
            Header tenantHeader = record.headers().lastHeader(TENANT_HEADER);
            if (tenantHeader == null || tenantHeader.value() == null) {
                // Reject malformed records — falling through to
                // handler invocation would either NPE in business
                // code or (worse) run under the previous record's
                // stale tenantId (silent cross-tenant write).
                throw new TenantContextMissingException(
                    "Kafka record lacks " + TENANT_HEADER + " header at offset "
                    + record.offset() + " partition " + record.partition());
            }
            UUID tenantId = UUID.fromString(
                new String(tenantHeader.value(), StandardCharsets.UTF_8));
            try {
                // MUST set: business-layer handlers may call
                // TenantContext.current() during repository
                // queries, @AuthorizedTenant-annotated getters,
                // and audit-event MDC enrichment.
                TenantContext.set(tenantId);
                handler.handle(record.value());
            } finally {
                // MUST clear: poll thread is reused across
                // records and batches. Leaving record N's
                // context set for record N+1's iteration is
                // the stale-per-record context leak.
                TenantContext.clear();
            }
        }
        // Batch-level ack AFTER all per-record set/clear cycles.
        // ack.acknowledge() interacts with the broker client and
        // MUST NOT run inside any record's TenantContext span
        // (#failure_modes.ack_inside_tenant_span).
        ack.acknowledge();
    }

    /**
     * Rebalance callback — runs on the consumer poll thread
     * BETWEEN batches with NO tenant signal. The poll thread
     * is shared across every tenant in the partition
     * assignment; TenantContext.set here would leak into the
     * first record of the next batch (which may belong to a
     * different tenant).
     *
     * Rebalance callbacks are tenant-free by contract
     * (#failure_modes.rebalance_carries_tenant_context).
     */
    @Override
    public void onPartitionsRevokedBeforeCommit(
            Consumer<?, ?> consumer,
            Collection<TopicPartition> partitions) {
        // no-op — no TenantContext interaction.
    }

    @Override
    public void onPartitionsAssigned(
            Consumer<?, ?> consumer,
            Collection<TopicPartition> partitions) {
        // no-op — no TenantContext interaction.
    }

    /**
     * Business-event handler abstraction. Fork-receivers
     * implement this with their domain-event dispatch logic.
     * The handler is invoked under TenantContext set to the
     * record's tenantId; the handler is FORBIDDEN from
     * resolving tenantId from any other source (record key,
     * payload field, config default). TenantContext.current()
     * is the single source of truth inside the handler.
     */
    public interface TenantEventHandler {
        void handle(Object payload);
    }
}
