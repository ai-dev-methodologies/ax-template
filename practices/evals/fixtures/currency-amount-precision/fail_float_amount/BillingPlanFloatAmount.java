package ax.template.billing;

/**
 * FAILING FIXTURE — rule: currency-amount-precision-explicit
 *
 * Violation: Plan entity uses double for the amount field.
 * 10.1 KRW stored as 10.09999942779541 (IEEE 754 rounding).
 */
class BillingPlanFloatAmount {

    private Long id;
    private String name;

    // VIOLATION: double causes IEEE 754 rounding errors on non-exact binary fractions
    // ArchUnit: fields named *amount* in billing package must have type long
    private double amount;  // ← VIOLATION

    // VIOLATION: BigDecimal is also prohibited (verbose, mutation-prone)
    // private BigDecimal price;  // ← also VIOLATION

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}
