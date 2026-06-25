package com.ax.template.authblueprint.ratingsummary;

import java.util.UUID;

public final class RatingSummaryExceptions {

    private RatingSummaryExceptions() {}

    public static class ReviewNotFoundException extends RuntimeException {
        public ReviewNotFoundException(UUID id) { super("review not found: " + id); }
    }

    public static class InvalidStarsException extends RuntimeException {
        public InvalidStarsException(int stars) {
            super("stars must be between 1 and 5, got: " + stars);
        }
    }
}
