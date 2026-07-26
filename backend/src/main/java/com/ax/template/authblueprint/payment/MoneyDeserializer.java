package com.ax.template.authblueprint.payment;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * PAYMENT-MONEY-002: Jackson deserializer that accepts integer minor units or
 * decimal-string amounts but REJECTS JSON float tokens with a clear error.
 *
 * <p>Accepted JSON shapes — both are preserved as the {@link MoneyWire} SHAPE they arrived in,
 * because the two encodings denote different numbers and this deserializer cannot see the sibling
 * {@code currency} field needed to reconcile them (see {@link MoneyWire}):
 * <ul>
 *   <li>Integer: {@code "amount": 1099} → {@code MoneyWire.ofMinor(1099)} — MINOR units
 *       (USD $10.99), resolved against the currency by {@code MoneyWire.resolveMajor}.</li>
 *   <li>Decimal string: {@code "amount": "10.99"} → {@code MoneyWire.ofMajor(new BigDecimal("10.99"))}
 *       — already MAJOR units.</li>
 * </ul>
 *
 * <p>Rejected:
 * <ul>
 *   <li>Float JSON token: {@code "amount": 10.99} → {@link DatabindException}
 *       translated to HTTP 400 + RFC 7807 detail by Spring's MessageNotReadable handler.</li>
 * </ul>
 *
 * <p>Rationale: IEEE 754 binary floating point cannot represent most decimal fractions
 * exactly; BigDecimal precision only survives integer-minor-units or decimal-string encoding.
 *
 * <p>DoS pre-bound: Jackson 3 raised the default {@code StreamReadConstraints} max string length
 * 20M→100M, so a decimal-STRING amount can now arrive with millions of characters. Such a value is
 * rejected on LENGTH before any regex / {@link BigDecimal} construction touches it (CPU/memory
 * amplification guard), and no rejection message echoes the oversized value back into the 400 body
 * (response-amplification guard). A NUMERIC(19,8) amount is at most ~21 chars; {@link #MAX_AMOUNT_LEN}
 * is generous. The global 20M string pin (application.yml) is the outer defense-in-depth layer.
 */
public class MoneyDeserializer extends ValueDeserializer<MoneyWire> {

    private static final Pattern DECIMAL = Pattern.compile("^[0-9]+(\\.[0-9]{1,8})?$");

    /** Max accepted length of a decimal-string amount. NUMERIC(19,8) ⇒ ≤ ~21 chars; 32 is generous. */
    static final int MAX_AMOUNT_LEN = 32;

    @Override
    public MoneyWire deserialize(JsonParser p, DeserializationContext ctx) throws JacksonException {
        JsonToken token = p.currentToken();
        if (token == JsonToken.VALUE_NUMBER_INT) {
            // MINOR units per contracts/payment-openapi.yaml#MoneyAmount. Jackson raises
            // InputCoercionException for a value beyond long range, keeping the existing 400 path.
            return MoneyWire.ofMinor(p.getLongValue());
        }
        if (token == JsonToken.VALUE_STRING) {
            String text = p.getValueAsString();
            // Reject over-long strings BEFORE regex/BigDecimal — state the length + bound, NEVER echo
            // the oversized value (amplification guard).
            if (text != null && text.length() > MAX_AMOUNT_LEN) {
                throw DatabindException.from(p,
                    "amount string too long (" + text.length() + " chars; max " + MAX_AMOUNT_LEN + ")");
            }
            if (text != null && DECIMAL.matcher(text).matches()) {
                return MoneyWire.ofMajor(new BigDecimal(text));
            }
            throw DatabindException.from(p,
                "invalid amount '" + cap(text) + "'; expected integer minor units or decimal string "
                    + "matching ^[0-9]+(\\.[0-9]{1,8})?$");
        }
        if (token == JsonToken.VALUE_NUMBER_FLOAT) {
            throw DatabindException.from(p,
                "float JSON number not supported for amount; use integer minor units or decimal string. "
                    + "Reason: IEEE 754 binary float cannot losslessly represent most decimal fractions.");
        }
        throw DatabindException.from(p,
            "amount must be an integer minor-units value or a decimal string; got token " + token);
    }

    /**
     * Truncate an echoed value to a short prefix so no rejection message reflects unbounded client
     * input. Over-length strings are already rejected earlier (length pre-bound), so this is a
     * belt-and-suspenders cap for the remaining short-string echo path.
     */
    private static String cap(String s) {
        if (s == null) {
            return "null";
        }
        return s.length() <= MAX_AMOUNT_LEN ? s : s.substring(0, MAX_AMOUNT_LEN) + "…(" + s.length() + " chars)";
    }
}
