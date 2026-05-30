package com.ax.template.authblueprint.dsr;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request + response payloads for the DSR surface. Grouped in one file because
 * each is a small boundary record with no behavior.
 */
public final class DsrDtos {

    private DsrDtos() {}

    /** DSR-RECTIFY-001 body. */
    public record RectifyRequest(
        @NotBlank String fieldPath,
        String currentValue,
        @NotBlank String correctedValue,
        @NotBlank String justification
    ) {}

    /** Common tracking envelope returned by every open/get endpoint (DSR-SLA-001). */
    public record DsrRequestResponse(
        UUID requestId,
        String type,
        String status,
        Instant receivedAt,
        Instant dueAt,
        Instant closedAt,
        int extensionDays,
        boolean slaBreached
    ) {
        public static DsrRequestResponse from(DsrRequest r) {
            return new DsrRequestResponse(
                r.getId(),
                r.getType().metricType(),
                r.getStatus().name(),
                r.getReceivedAt(),
                r.getDueAt(),
                r.getClosedAt(),
                r.getExtensionDays(),
                r.isSlaBreached());
        }
    }

    /** DSR-ACCESS-001 / DSR-PORTABILITY-001 bundle: per-module personal data. */
    public record AccessBundle(
        DsrRequestResponse request,
        Map<String, Map<String, Object>> modules
    ) {}

    /**
     * DSR-ERASURE-001 result. {@code retained} is empty for a full erasure and
     * non-empty when a legal-hold yields a partial-erasure manifest. The shape is
     * STABLE across re-requests so the idempotent re-call returns the prior manifest.
     */
    public record ErasureManifest(
        UUID requestId,
        Instant erasedAt,
        String legalBasis,
        boolean fullyErased,
        List<RetainedCategory> retained
    ) {
        public record RetainedCategory(String category, String legalBasis) {}
    }

    /** DSR-RESTRICT-001 lift body. */
    public record LiftRequest(@NotBlank String justification) {}

    /** DSR-SLA-001 extension body. */
    public record ExtendRequest(int extensionDays, @NotBlank String extensionReason) {}
}
