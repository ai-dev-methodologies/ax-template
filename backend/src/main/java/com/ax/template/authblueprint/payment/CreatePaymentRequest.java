package com.ax.template.authblueprint.payment;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request body for POST /api/payments.
 *
 * <p>{@code amount} is deserialized via {@link MoneyDeserializer} so JSON floats are
 * rejected (PAYMENT-MONEY-002). Currency / scale validation is performed in
 * {@link PaymentService}; we deliberately keep this DTO minimal so that scale
 * violations surface as 400 RFC 7807 ProblemDetails rather than bean-validation
 * messages (which leak field paths).
 */
public class CreatePaymentRequest {

    @NotNull
    @JsonDeserialize(using = MoneyDeserializer.class)
    private BigDecimal amount;

    @NotBlank
    private String currency;

    @NotBlank
    private String orderId;

    /** Tokenized payment method reference; raw PAN is never accepted (SAQ-A scope). */
    private String paymentMethodToken;

    /** Allows tests to pre-set a mock failure mode in the request body (alternative to header). */
    private String mockFailureMode;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getPaymentMethodToken() { return paymentMethodToken; }
    public void setPaymentMethodToken(String paymentMethodToken) { this.paymentMethodToken = paymentMethodToken; }

    public String getMockFailureMode() { return mockFailureMode; }
    public void setMockFailureMode(String mockFailureMode) { this.mockFailureMode = mockFailureMode; }
}
