package com.ax.template.authblueprint.consumerproof;

// Plain domain POJO (deliberately NOT a JPA @Entity) — support type for the
// money + repository fixtures. Structural only; does not need to compile.
public class Order {
    private long totalAmount;   // minor units (long)
    private String currency;    // ISO-4217

    public long getTotalAmount() { return totalAmount; }
    public String getCurrency() { return currency; }
}
