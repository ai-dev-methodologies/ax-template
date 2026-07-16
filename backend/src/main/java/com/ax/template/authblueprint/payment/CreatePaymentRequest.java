package com.ax.template.authblueprint.payment;

import tools.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request body for POST /api/payments.
 *
 * <p>{@code amount} is deserialized via {@link MoneyDeserializer} so JSON floats are
 * rejected (PAYMENT-MONEY-002). Currency / scale validation is performed in
 * {@link PaymentService}; we deliberately keep this DTO minimal so that scale
 * violations surface as 400 RFC 7807 ProblemDetails rather than bean-validation
 * messages (which leak field paths).
 *
 * <p>{@code paymentMethodToken} is an opaque tokenized payment method reference;
 * raw PAN is never accepted (SAQ-A scope).
 *
 * <p>{@code mockFailureMode} allows tests to pre-set a mock failure mode in the
 * request body (alternative to the X-Test-Provider-Mode header).
 */
public record CreatePaymentRequest(
    @NotNull @JsonDeserialize(using = MoneyDeserializer.class) BigDecimal amount,
    // ISO-4217 currency codes are exactly 3 chars. The tight @Size(max=3) fast-rejects an
    // oversized (~1MB) currency at bean-validation BEFORE it can reach PaymentService and be
    // echoed into an error message (response-amplification defense — Jackson 3 raised the
    // stream max-string-length default 20M→100M, so an unbounded field could otherwise be huge).
    @NotBlank @Size(max = 3) String currency,
    // orderId / paymentMethodToken / mockFailureMode are free-text; generous upper bounds keep any
    // rejected value from being an amplification vector while comfortably fitting real inputs.
    @NotBlank @Size(max = 200) String orderId,
    @Size(max = 512) String paymentMethodToken,
    @Size(max = 64) String mockFailureMode
) {
}
