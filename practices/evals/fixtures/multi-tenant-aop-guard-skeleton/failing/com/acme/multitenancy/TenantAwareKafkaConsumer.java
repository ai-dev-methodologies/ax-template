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
 * FAILING fixture — intentionally trips the 42nd guard
 * (kafka_consumer_tenant_scope_guard.sh) by violating four of
 * the five clauses in #kafka-consumer-tenant-scope:
 *
 *   (1) Reads TenantContext.current() at method entry BEFORE
 *       any X-Tenant-Id header read → consumer_assumes_current_context.
 *   (2) TenantContext.set called ONCE before the batch for-loop
 *       (from the first record's header), then handler invoked
 *       for every record under that single tenantId →
 *       batch_set_once.
 *   (3) TenantContext.clear() missing entirely (count mismatch) →
 *       stale_per_record_context.
 *   (4) onPartitionsAssigned rebalance callback calls
 *       TenantContext.set(defaultTenant) → rebalance_carries_tenant_context.
 *
 * The 42nd guard MUST detect all four clauses and exit non-zero
 * on --fixtures mode.
 */
@Component
public class TenantAwareKafkaConsumer implements ConsumerAwareRebalanceListener {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String TOPIC = "ax.tenant.events";
    private static final UUID DEFAULT_TENANT =
        UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final TenantEventHandler handler;

    public TenantAwareKafkaConsumer(TenantEventHandler handler) {
        this.handler = handler;
    }

    /**
     * VIOLATING listener:
     *   - reads TenantContext.current() BEFORE any header read
     *     (consumer_assumes_current_context)
     *   - sets TenantContext ONCE from the first record outside
     *     the loop (batch_set_once)
     *   - never calls TenantContext.clear() (stale_per_record_context)
     */
    @KafkaListener(topics = TOPIC, containerFactory = "kafkaListenerContainerFactory")
    public void onBatch(List<ConsumerRecord<String, Object>> records,
                        Acknowledgment ack) {
        // VIOLATION clause(1): reads ambient context before any
        // envelope/header read. Poll thread has empty context;
        // .orElseThrow() either NPEs or (with defensive code)
        // silently falls back to a default tenant.
        UUID existing = TenantContext.current().orElse(DEFAULT_TENANT);

        // VIOLATION clause(2): set ONCE from the first record,
        // outside the for-loop. Every subsequent record processes
        // under this single tenantId regardless of its own header.
        if (!records.isEmpty()) {
            Header firstHeader = records.get(0).headers().lastHeader(TENANT_HEADER);
            if (firstHeader != null && firstHeader.value() != null) {
                UUID firstTenant = UUID.fromString(
                    new String(firstHeader.value(), StandardCharsets.UTF_8));
                TenantContext.set(firstTenant);
            }
        }

        for (ConsumerRecord<String, Object> record : records) {
            // No per-record header read; no per-record set/clear.
            handler.handle(record.value());
        }

        // VIOLATION clause(3): no TenantContext.clear() anywhere.
        // count(set)=1, count(clear)=0. Tenant from this batch
        // leaks into the next poll cycle's first record.
        ack.acknowledge();
    }

    /**
     * VIOLATION clause(4): rebalance callback calls
     * TenantContext.set with a default tenant — "restoring"
     * tenant context from config on rebalance. The value
     * leaks into the first record of the next batch (which
     * may belong to a different tenant in the new
     * assignment).
     */
    @Override
    public void onPartitionsRevokedBeforeCommit(
            Consumer<?, ?> consumer,
            Collection<TopicPartition> partitions) {
        // no-op
    }

    @Override
    public void onPartitionsAssigned(
            Consumer<?, ?> consumer,
            Collection<TopicPartition> partitions) {
        // VIOLATION: rebalance callbacks MUST be tenant-free.
        TenantContext.set(DEFAULT_TENANT);
    }

    public interface TenantEventHandler {
        void handle(Object payload);
    }
}
