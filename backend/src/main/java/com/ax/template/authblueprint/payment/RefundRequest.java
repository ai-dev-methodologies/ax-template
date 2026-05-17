package com.ax.template.authblueprint.payment;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;

public class RefundRequest {

    @JsonDeserialize(using = MoneyDeserializer.class)
    private BigDecimal amount;

    private String reason;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
