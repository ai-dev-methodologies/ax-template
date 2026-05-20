package com.ax.template.authblueprint.billing;

/**
 * Billing-domain runtime errors mapped to RFC 7807 ProblemDetail at the controller layer.
 */
public class BillingException extends RuntimeException {

    public static class PlanNotFound extends BillingException {
        public PlanNotFound(String planId) { super("plan not found: " + planId); }
    }

    public static class SubscriptionNotFound extends BillingException {
        public SubscriptionNotFound(String subId) { super("subscription not found: " + subId); }
    }

    public static class InvalidWebhookSignature extends BillingException {
        public InvalidWebhookSignature(String reason) { super("invalid webhook signature: " + reason); }
    }

    public BillingException(String msg) { super(msg); }
}
