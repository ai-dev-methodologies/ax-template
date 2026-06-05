package com.ax.template.authblueprint.softdelete;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * SOFTDELETE-OBSERVABILITY-001 — exactly 3 canonical Micrometer counters with bounded-cardinality
 * labels:
 * <ul>
 *   <li>{@code soft_delete_total{tenant, entity}} — tombstone events;</li>
 *   <li>{@code soft_delete_restore_total{tenant, entity, outcome}} — outcome ∈ {restored,
 *       window_expired, not_deleted};</li>
 *   <li>{@code soft_delete_purge_total{tenant, entity, reason}} — reason ∈ {retention, erasure_request}.</li>
 * </ul>
 * {@code entity} is the table name (fixed, low-cardinality); row id / natural key (email) / payload
 * are NEVER labels. Spec: specs/soft-delete-l0.yaml#SOFTDELETE-OBSERVABILITY-001.
 */
@Component
public class SoftDeleteMetrics {

    public static final String DELETED = "soft_delete_total";
    public static final String RESTORED = "soft_delete_restore_total";
    public static final String PURGED = "soft_delete_purge_total";

    static final String TAG_TENANT = "tenant";
    static final String TAG_ENTITY = "entity";
    static final String TAG_OUTCOME = "outcome";
    static final String TAG_REASON = "reason";

    /** The only entity value this reference emits (fixed table-name enum). */
    static final String ENTITY = "account";

    private final MeterRegistry registry;

    public SoftDeleteMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void deleted(String tenant) {
        Counter.builder(DELETED).tag(TAG_TENANT, tenant).tag(TAG_ENTITY, ENTITY)
                .register(registry).increment();
    }

    /** outcome ∈ {restored, window_expired, not_deleted}. */
    public void restore(String tenant, String outcome) {
        Counter.builder(RESTORED).tag(TAG_TENANT, tenant).tag(TAG_ENTITY, ENTITY).tag(TAG_OUTCOME, outcome)
                .register(registry).increment();
    }

    /** reason ∈ {retention, erasure_request}. */
    public void purge(String tenant, String reason) {
        Counter.builder(PURGED).tag(TAG_TENANT, tenant).tag(TAG_ENTITY, ENTITY).tag(TAG_REASON, reason)
                .register(registry).increment();
    }
}
