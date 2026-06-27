package com.ax.template.authblueprint.bundlepricing;

import java.util.UUID;

public final class BundlePricingExceptions {

    private BundlePricingExceptions() {}

    /** Composite item does not exist → 404. */
    public static class CompositeItemNotFoundException extends RuntimeException {
        public CompositeItemNotFoundException(UUID id) {
            super("composite item not found: " + id);
        }
    }

    /** A create request whose shape violates the mode/base-price invariant → 400. */
    public static class InvalidCompositeItemException extends RuntimeException {
        public InvalidCompositeItemException(String message) {
            super(message);
        }
    }
}
