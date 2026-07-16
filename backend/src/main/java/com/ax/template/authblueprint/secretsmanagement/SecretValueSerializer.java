package com.ax.template.authblueprint.secretsmanagement;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * SECRET-NO-LOG-001 — the JSON egress half of {@link SecretValue}'s masking. Any DTO that
 * (mistakenly) carries a {@link SecretValue} serializes the constant {@link SecretValue#MASK}, never
 * the plaintext, so a secret can never escape over an HTTP response body.
 *
 * <p>Spec: specs/secrets-management-l0.yaml#SECRET-NO-LOG-001.
 */
public class SecretValueSerializer extends StdSerializer<SecretValue> {

    public SecretValueSerializer() {
        super(SecretValue.class);
    }

    @Override
    public void serialize(SecretValue value, JsonGenerator gen, SerializationContext provider) throws JacksonException {
        gen.writeString(SecretValue.MASK);
    }
}
