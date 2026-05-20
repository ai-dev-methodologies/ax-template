package com.acme.multitenancy;

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
 * FAILING fixture — intentionally trips the 43rd guard
 * (kafka_streams_tenant_scope_guard.sh) by violating four
 * detectable clauses in #kafka-streams-tenant-scope:
 *
 *   (1) groupByKey() without an upstream selectKey that
 *       tenant-prefixes the key → cross_tenant_state_store_collision.
 *   (2) Punctuator body calls context.forward(...) without
 *       wrapping in TenantContext.set / clear →
 *       punctuator_silent_context_loss.
 *   (3) (Composite — same surface as (2): no set/clear at
 *       all means count=0/count=0 with forward present,
 *       which is the detection rule for clause 2.)
 *   (4) .join(...) without an upstream tenant-prefix
 *       selectKey on the joined stream → cross_tenant_join_key.
 *   (5) Materialized.as("tenant-metrics-" + tenantId.toString())
 *       — per_tenant_state_store_naming (dynamic tenant
 *       interpolation in store name).
 *
 * The 43rd guard MUST detect all four clauses and exit
 * non-zero on --fixtures mode.
 */
@Component
public class TenantAwareKafkaStreamsTopology {

    static final String EVENTS_TOPIC = "ax.tenant.events";
    static final String CUSTOMERS_TOPIC = "ax.tenant.customers";

    /**
     * VIOLATING topology:
     *   - groupByKey directly on source key (no upstream
     *     tenant-prefix selectKey) → cross_tenant_state_store_collision.
     *   - join with bare key on both sides → cross_tenant_join_key.
     *   - per-tenant Materialized.as name → per_tenant_state_store_naming.
     */
    public KTable<String, Long> build(StreamsBuilder builder,
                                       UUID tenantId) {
        KStream<String, String> source = builder.stream(
            EVENTS_TOPIC,
            Consumed.with(Serdes.String(), Serdes.String()));

        KStream<String, String> customerStream = builder.stream(
            CUSTOMERS_TOPIC,
            Consumed.with(Serdes.String(), Serdes.String()));

        // VIOLATION clause (4): join with NO upstream
        // tenant-prefix selectKey. Tenant A's order joins
        // Tenant B's customer because the join key is the
        // bare business key on both sides.
        KStream<String, String> joined = source.join(
            customerStream.toTable(),
            (orderValue, customerValue) -> orderValue + "|" + customerValue);

        // VIOLATION clause (1) + (5): groupByKey with no
        // upstream tenant-prefix selectKey, AND Materialized.as
        // interpolates tenantId into the store name (which is
        // impossible at static topology build time and breaks
        // rebalance/standby).
        KTable<String, Long> metrics = joined
            .groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
            .count(Materialized.<String, Long,
                KeyValueStore<org.apache.kafka.common.utils.Bytes, byte[]>>
                as("tenant-metrics-" + tenantId.toString())
                .withKeySerde(Serdes.String())
                .withValueSerde(Serdes.Long()));

        return metrics;
    }

    /**
     * VIOLATING punctuator:
     *   - calls context.forward(...) WITHOUT TenantContext.set
     *     / clear around it → punctuator_silent_context_loss.
     *   - count(set)==0, count(clear)==0 while forward( is
     *     present (clause 2 + 3).
     */
    public static class MetricsPunctuator
            implements Processor<String, Long, String, Long> {

        private KeyValueStore<String, Long> store;

        @Override
        public void init(ProcessorContext<String, Long> context) {
            this.store = context.getStateStore("tenant-metrics-by-key");
            context.schedule(
                Duration.ofSeconds(60),
                PunctuationType.WALL_CLOCK_TIME,
                timestamp -> {
                    try (KeyValueIterator<String, Long> it = store.all()) {
                        while (it.hasNext()) {
                            KeyValue<String, Long> kv = it.next();
                            // VIOLATION clause (2): forward without
                            // any TenantContext.set/clear. The
                            // StreamThread has empty TenantContext
                            // by construction; downstream sinks
                            // calling TenantContext.current() NPE
                            // or fall back to the LAST iteration's
                            // stale tenant (silent cross-tenant
                            // write).
                            context.forward(new Record<>(
                                kv.key, kv.value, timestamp));
                        }
                    }
                });
        }

        @Override
        public void process(Record<String, Long> record) {
            // No-op.
        }
    }
}
