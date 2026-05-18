package ax.template.billing;

/**
 * PASSING FIXTURE — rule: currency-amount-precision-explicit
 *
 * Correct: Plan entity uses long integer minor units for amount.
 * KRW ₩10,000 → 10000L. USD $9.99 → 999L.
 */
class BillingPlanLongAmount {

    private Long id;
    private String name;

    // CORRECT: long integer minor units — no rounding errors
    // KRW: 1원 = 1L (no subdivision), USD: 1¢ = 1L
    private long amount;  // ← CORRECT

    private String currency; // "KRW" / "USD" / "EUR"

    public long getAmount() { return amount; }

    // Factory — no setter; mutation via domain methods only
    static BillingPlanLongAmount create(String name, long amount, String currency) {
        BillingPlanLongAmount plan = new BillingPlanLongAmount();
        plan.name = name;
        plan.amount = amount;
        plan.currency = currency;
        return plan;
    }
}
