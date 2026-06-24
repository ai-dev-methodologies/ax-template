package com.ax.template.authblueprint.commercecatalog;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Bounded-cardinality metrics for commercecatalog (domain-metrics-bounded-cardinality):
 * {@code catalog_sku_resolve_total{result}} — result is one of: ok | no_match | ambiguous.
 * No high-cardinality labels (no product ids, SKU ids, or option values).
 */
@Component
public class CatalogMetrics {

    private final MeterRegistry registry;

    public CatalogMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * @param result ok | no_match | ambiguous | not_purchasable | price_required
     */
    public void recordSkuResolve(String result) {
        registry.counter("catalog_sku_resolve_total", "result", result).increment();
    }

    /**
     * @param op     create_product | add_variant | link_category
     * @param result ok | rejected
     */
    public void recordOp(String op, String result) {
        registry.counter("catalog_op_total", "op", op, "result", result).increment();
    }
}
