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
 *
 * <p>{@code paymentMethodToken} is an opaque tokenized payment method reference;
 * raw PAN is never accepted (SAQ-A scope).
 *
 * <p>{@code mockFailureMode} allows tests to pre-set a mock failure mode in the
 * request body (alternative to the X-Test-Provider-Mode header).
 */
public record CreatePaymentRequest(
    @NotNull @JsonDeserialize(using = MoneyDeserializer.class) BigDecimal amount,
    @NotBlank String currency,
    @NotBlank String orderId,
    String paymentMethodToken,
    String mockFailureMode
) {
}
