package com.ax.template.authblueprint.orderquantization;

import com.ax.template.authblueprint.common.OffsetPageSupport;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * order-multiple-quantization-l0 sole orchestrator. Validates the procurement constraints
 * (ORDERQUANT-CONSTRAINT-001 — moq >= 1, multiple >= 1, required >= 0; an invalid request is 422
 * before any divide-by-zero), quantizes via the pure {@link Quantizer} (ORDERQUANT-QUANTIZE-001),
 * computes the NON-CONSERVING overage = orderQuantity - required (ORDERQUANT-OVERAGE-001), and
 * persists the full reconstructible basis (ORDERQUANT-BASIS-001). The quantizer is a pure function
 * of its three inputs, so the same (required, moq, multiple) always yields the same result
 * (ORDERQUANT-IDEMPOTENT-001).
 *
 * <p>The quantization is DELIBERATELY non-conserving — the placed order exceeds the requirement by
 * the recorded overage, the real procurement cost of a lot constraint that cannot be subdivided.
 * This is the opposite of the catalog's conserving rounded-split.
 */
@Service
public class OrderQuantizationService {

    private final OrderQuantizationRepository records;
    private final OrderQuantizationMetrics metrics;
    private final Clock clock;

    public OrderQuantizationService(OrderQuantizationRepository records,
                                    OrderQuantizationMetrics metrics, Clock clock) {
        this.records = records;
        this.metrics = metrics;
        this.clock = clock;
    }

    /**
     * ORDERQUANT-QUANTIZE/OVERAGE/BASIS-001 — quantize a required net quantity UP to the supplier
     * constraint and record the full non-conserving basis. orderQuantity = max(moq, ceil(required /
     * multiple) * multiple); overage = orderQuantity - required (always >= 0).
     */
    @Transactional
    public OrderQuantization quantize(String itemRef, long required, long moq, long multiple) {
        if (required < 0 || moq < 1 || multiple < 1) {
            metrics.record("invalid_constraint");
            throw OrderQuantizationException.invalidConstraint(
                "required must be >= 0 and MOQ and order multiple must each be >= 1"
                + " (required=" + required + ", moq=" + moq + ", multiple=" + multiple + ")");
        }
        long orderQuantity = Quantizer.quantize(required, moq, multiple);   // pure, deterministic
        long overage = orderQuantity - required;                            // NON-CONSERVING surplus, recorded
        OrderQuantization saved = records.save(new OrderQuantization(UUID.randomUUID(), itemRef,
            required, moq, multiple, orderQuantity, overage, Instant.now(clock)));
        metrics.record("quantized");
        return saved;
    }

    @Transactional(readOnly = true)
    public OrderQuantization get(UUID id) {
        return records.findById(id).orElseThrow(OrderQuantizationException::notFound);
    }

    @Transactional(readOnly = true)
    public Page<OrderQuantization> listForItem(String itemRef, int page, int size) {
        return records.findByItemRefOrderByCreatedAtDesc(itemRef,
            OffsetPageSupport.clamp(page, size, OffsetPageSupport.DEFAULT_MAX_PAGE_SIZE)
                .withSort(Sort.by(Sort.Direction.DESC, "createdAt")));
    }
}
