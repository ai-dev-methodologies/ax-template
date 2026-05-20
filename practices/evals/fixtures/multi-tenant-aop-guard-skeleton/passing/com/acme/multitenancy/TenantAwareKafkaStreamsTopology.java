package com.acme.multitenancy;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.stereotype.Component;

/**
 * Generated from blueprints/multi-tenant-manifest.yaml#kafka-streams-tenant-scope.tenant_prefixed_state_store_with_punctuator_key_decode.canonical_skeleton
 * with <root> = acme.
 *
 * Tenant-scoped Kafka Streams (KStream / KTable) real-time
 * aggregation topology. Five guard-checked properties:
 *   (1) Upstream selectKey rewrites every record key to
 *       tenantId + "#" + originalKey BEFORE any groupBy /
 *       join / aggregate, using the per-record X-Tenant-Id
 *       header as the tenant signal. RocksDB-backed state
 *       stores partition per-tenant by key prefix.
 *   (2) Punctuator body iterates the state store, decodes
 *       the tenant prefix from each key, and wraps per-key
 *       context.forward(...) in try { TenantContext.set; ... }
 *       finally { TenantContext.clear; }. StreamThread has
 *       no inherited TenantContext by construction.
 *   (3) count(TenantContext.set) == count(TenantContext.clear)
 *       inside the punctuator body.
 *   (4) Join keys are tenant-namespaced on every side via
 *       upstream selectKey (no cross-tenant join match).
 *   (5) State store names are static literal ("tenant-metrics-
 *       by-key"); per-tenant store naming is rejected.
 *
 * Distinct surface from TenantAwareKafkaConsumer (R9): R9
 * processes records statelessly via @KafkaListener; this
 * topology builds durable state stores (KTables), runs
 * wall-clock punctuators, and performs key-namespaced joins.
 */
@Component
public class TenantAwareKafkaStreamsTopology {

    static final String TENANT_HEADER = "X-Tenant-Id";
    static final String KEY_SEPARATOR = "#";
    static final String EVENTS_TOPIC = "ax.tenant.events";
    // Property (5): static literal name. No tenantId
    // interpolation; the single RocksDB store partitions
    // per-tenant by key prefix (property 1), not by store
    // name.
    static final String METRICS_STORE = "tenant-metrics-by-key";

    /**
     * Build the topology. Called once at StreamsBuilder
     * wiring time.
     *
     * Property (1): selectKey runs FIRST, rewriting every
     * record key to "tenantId#originalKey" from the
     * X-Tenant-Id header. Downstream stateful operators
     * (groupBy, aggregate, join) see the tenant-namespaced
     * key.
     */
    public KTable<String, Long> build(StreamsBuilder builder) {
        KStream<String, String> source = builder.stream(
            EVENTS_TOPIC,
            Consumed.with(Serdes.String(), Serdes.String()));

        // Property (1) + (4): tenant-prefixed selectKey BEFORE
        // any groupBy / join. Both sides of any subsequent
        // join inherit this tenant-namespaced key.
        KStream<String, String> tenantNamespaced = source
            .selectKey((key, value, recordContext) -> {
                var header = recordContext.headers()
                    .lastHeader(TENANT_HEADER);
                if (header == null || header.value() == null) {
                    throw new TenantContextMissingException(
                        "Streams record lacks " + TENANT_HEADER
                        + " header at offset "
                        + recordContext.offset());
                }
                String tenantId = new String(
                    header.value(), StandardCharsets.UTF_8);
                return tenantId + KEY_SEPARATOR + key;
            });

        KTable<String, Long> metrics = tenantNamespaced
            .groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
            .count(Materialized.<String, Long,
                KeyValueStore<org.apache.kafka.common.utils.Bytes, byte[]>>
                as(METRICS_STORE)
                .withKeySerde(Serdes.String())
                .withValueSerde(Serdes.Long()));

        return metrics;
    }

    /**
     * Punctuator-bearing processor. Registered into the
     * topology via .process(() -> new MetricsPunctuator(),
     * METRICS_STORE) so the punctuator body has access to
     * the materialized state store.
     *
     * Property (2): inside the punctuator, decode the
     * tenant prefix from each key and wrap per-key
     * context.forward(...) in TenantContext set/clear.
     * Property (3): count(set)==count(clear) inside the
     * punctuator body.
     */
    public static class MetricsPunctuator
            implements Processor<String, Long, String, Long> {

        private KeyValueStore<String, Long> store;

        @Override
        public void init(ProcessorContext<String, Long> context) {
            this.store = context.getStateStore(METRICS_STORE);
            // Punctuator fires every 60 seconds wall-clock on
            // the StreamThread. StreamThread has NO inherited
            // TenantContext — same failure-class as the consumer
            // poll thread (R9 #kafka-consumer-tenant-scope).
            context.schedule(
                Duration.ofSeconds(60),
                PunctuationType.WALL_CLOCK_TIME,
                timestamp -> {
                    try (KeyValueIterator<String, Long> it = store.all()) {
                        while (it.hasNext()) {
                            KeyValue<String, Long> kv = it.next();
                            // Property (2): decode the tenant
                            // prefix from the composite key.
                            int sep = kv.key.indexOf(KEY_SEPARATOR);
                            if (sep <= 0) {
                                // Defensive: skip malformed keys
                                // rather than processing them
                                // under empty TenantContext.
                                continue;
                            }
                            UUID tenantId = UUID.fromString(
                                kv.key.substring(0, sep));
                            try {
                                // MUST set: downstream sinks may
                                // call TenantContext.current()
                                // during metric emission.
                                TenantContext.set(tenantId);
                                context.forward(new Record<>(
                                    kv.key, kv.value, timestamp));
                            } finally {
                                // MUST clear: punctuator iteration
                                // reuses the same StreamThread;
                                // leaking tenant N into the next
                                // key (which may belong to tenant
                                // N+1) is punctuator_silent_context_loss.
                                TenantContext.clear();
                            }
                        }
                    }
                });
        }

        @Override
        public void process(Record<String, Long> record) {
            // Stream-time forwarding: the key is already
            // tenant-namespaced from property (1). Per-record
            // set/clear at process() entry is OPTIONAL because
            // the upstream selectKey ran in the same
            // StreamThread invocation chain. Punctuator-driven
            // forwarding above is the surface that REQUIRES
            // explicit set/clear.
        }
    }
}
