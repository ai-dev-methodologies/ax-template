package com.ax.template.authblueprint.secretsmanagement;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

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
    public void serialize(SecretValue value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(SecretValue.MASK);
    }
}
