package com.ax.template.authblueprint.ratingsummary;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public final class RatingSummaryDtos {

    private RatingSummaryDtos() {}

    public record AddReviewRequest(
        @NotNull UUID productId,
        @Min(1) @Max(5) int stars
    ) {}

    public record ReviewResponse(UUID id, UUID productId, int stars, ReviewStatus status) {
        public static ReviewResponse from(Review r) {
            return new ReviewResponse(r.getId(), r.getProductId(), r.getStars(), r.getStatus());
        }
    }

    public record SummaryResponse(UUID productId, BigDecimal average, int reviewCount) {
        public static SummaryResponse from(RatingSummary s) {
            return new SummaryResponse(s.getProductId(), s.getAverage(), s.getReviewCount());
        }
    }
}
