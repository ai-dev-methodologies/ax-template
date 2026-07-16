package com.ax.template.authblueprint.billing;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * BILLING-CUR-001: integer minor-unit amounts only.
 * <p>
 * Accepted:
 * <ul>
 *   <li>Integer JSON token: {@code "amount": 9900} → {@code 9900L}.</li>
 * </ul>
 * Rejected (HTTP 400 via {@link tools.jackson.databind.DatabindException}
 * → {@code HttpMessageNotReadableException} in Spring's ResponseEntityExceptionHandler):
 * <ul>
 *   <li>Float token: {@code "amount": 9.99} — IEEE-754 binary cannot losslessly
 *       represent most decimal fractions.</li>
 *   <li>String token: {@code "amount": "9900"} — billing API requires JSON
 *       numbers to keep type discipline at the wire boundary.</li>
 * </ul>
 */
public class MinorUnitAmountDeserializer extends ValueDeserializer<Long> {

    @Override
    public Long deserialize(JsonParser p, DeserializationContext ctx) {
        JsonToken token = p.currentToken();
        if (token == JsonToken.VALUE_NUMBER_INT) {
            return p.getLongValue();
        }
        if (token == JsonToken.VALUE_NUMBER_FLOAT) {
            throw DatabindException.from(p,
                "amount must be an integer in minor units (e.g. won for KRW, cents for USD); "
                    + "float JSON not accepted (IEEE-754 cannot losslessly represent decimals).");
        }
        throw DatabindException.from(p,
            "amount must be an integer minor-units value; got token " + token);
    }
}
