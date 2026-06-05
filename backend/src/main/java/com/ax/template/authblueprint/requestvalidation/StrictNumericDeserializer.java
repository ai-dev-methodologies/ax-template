package com.ax.template.authblueprint.requestvalidation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * VALIDATION-TYPE-001 — strict numeric deserialization with NO silent coercion, scoped to
 * the field it annotates (NOT a global {@code ObjectMapper} coercion change, which would
 * alter every other domain's binding). A numeric field declared with
 * {@code @JsonDeserialize(using = StrictNumericDeserializer.class)} rejects a JSON string
 * ({@code "100"}), a boolean, or any non-number token with a binding failure → HTTP 400,
 * never a coerced value reaching business logic.
 *
 * <p>Anchored to RFC 8259 (JSON) + JSON Schema draft 2020-12 {@code type} keyword: "an
 * instance validates successfully if its type matches the type represented by the value of
 * the string." Spec: specs/request-validation-l0.yaml#VALIDATION-TYPE-001.
 */
public class StrictNumericDeserializer extends JsonDeserializer<BigDecimal> {

    @Override
    public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken token = p.currentToken();
        if (token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT) {
            return p.getDecimalValue();
        }
        // VALUE_STRING ("100"), VALUE_TRUE/FALSE, etc. → reject; no coercion.
        throw MismatchedInputException.from(p, BigDecimal.class,
                "numeric field rejects non-number JSON token (" + token + "); no silent coercion");
    }
}
