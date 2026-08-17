package com.ax.template.authblueprint.currencyarithmetic;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Behavioral compliance tests for currency-arithmetic-l0.yaml (4 families) — black-box HTTP via
 * RestAssured, real round-trip (signup → login → create ledger → add/subtract/convert → read).
 *
 * <p>CCY-FAILCLOSED-ADD (cross-currency add ⇒ 422 CURRENCY_MISMATCH, balance unchanged),
 * CCY-FAILCLOSED-SUBTRACT (cross-currency subtract ⇒ 422), CCY-SAMECCY-OK (same-currency arithmetic
 * ⇒ exact sum/difference), CCY-EXPLICIT-CONVERT (an explicit recorded conversion makes the
 * cross-currency add succeed and is recorded).
 *
 * <p>BEFORE_CLASS dirties-context: the suite has many @SpringBootTest contexts vs a 32-entry cache;
 * a fresh context avoids LRU-eviction flake.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Tag("CURRENCY_ARITHMETIC")
class CurrencyArithmeticComplianceTest {

    @LocalServerPort int port;


    // ─── FAIL-CLOSED-ADD family ──────────────────────────────────────────────

    /**
     * CCY-FAILCLOSED-ADD: posting an addend of a DIFFERENT currency to a ledger is rejected
     * fail-closed (422 CURRENCY_MISMATCH) and the balance is left UNCHANGED — no silent coercion, no
     * partial mutation. A same-currency addend then adds correctly (200, exact sum). Also exercises
     * the IDOR-safe 404 for an unknown ledger id.
     */
    @Test
    @Tag("CCY-FAILCLOSED-ADD")
    void crossCurrencyAdd_failsClosed_balanceUnchanged() {
        String token = token("add");
        String ledgerId = createLedger(token, "USD", 1099);

        // Cross-currency add (KRW into a USD ledger) → 422 CURRENCY_MISMATCH.
        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{\"minorUnits\":1000,\"currency\":\"KRW\"}")
        .when().post("/api/currency-ledgers/" + ledgerId + "/add")
        .then().statusCode(422)
            .body("code", Matchers.equalTo("CURRENCY_MISMATCH"));

        // The balance is unchanged — the failed cross-currency add mutated nothing.
        read(token, ledgerId)
            .body("balanceMinor", Matchers.equalTo(1099))
            .body("currency", Matchers.equalTo("USD"));

        // Same-currency add succeeds with the exact sum.
        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{\"minorUnits\":250,\"currency\":\"USD\"}")
        .when().post("/api/currency-ledgers/" + ledgerId + "/add")
        .then().statusCode(200)
            .body("balanceMinor", Matchers.equalTo(1349));

        // Unknown ledger id → 404 problem+json (IDOR-safe).
        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{\"minorUnits\":1,\"currency\":\"USD\"}")
        .when().post("/api/currency-ledgers/" + UUID.randomUUID() + "/add")
        .then().statusCode(404)
            .body("code", Matchers.equalTo("CURRENCY_LEDGER_NOT_FOUND"));
    }

    /** CCY-FAILCLOSED-ADD: an unauthenticated caller cannot touch the ledger surface (401/403). */
    @Test
    @Tag("CCY-FAILCLOSED-ADD")
    void unauthenticated_isRejected() {
        given()
        .when().get("/api/currency-ledgers/" + UUID.randomUUID())
        .then().statusCode(Matchers.anyOf(Matchers.is(401), Matchers.is(403)));
    }

    // ─── FAIL-CLOSED-SUBTRACT family ─────────────────────────────────────────

    /**
     * CCY-FAILCLOSED-SUBTRACT: posting a subtrahend of a different currency is rejected fail-closed
     * (422), balance unchanged — subtraction is not a back-door around addition's currency check. A
     * same-currency subtract then yields the exact difference.
     */
    @Test
    @Tag("CCY-FAILCLOSED-SUBTRACT")
    void crossCurrencySubtract_failsClosed_balanceUnchanged() {
        String token = token("sub");
        String ledgerId = createLedger(token, "USD", 5000);

        // Cross-currency subtract (KRW out of a USD ledger) → 422 CURRENCY_MISMATCH.
        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{\"minorUnits\":1000,\"currency\":\"KRW\"}")
        .when().post("/api/currency-ledgers/" + ledgerId + "/subtract")
        .then().statusCode(422)
            .body("code", Matchers.equalTo("CURRENCY_MISMATCH"));

        read(token, ledgerId).body("balanceMinor", Matchers.equalTo(5000));

        // Same-currency subtract succeeds with the exact difference.
        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{\"minorUnits\":1500,\"currency\":\"USD\"}")
        .when().post("/api/currency-ledgers/" + ledgerId + "/subtract")
        .then().statusCode(200)
            .body("balanceMinor", Matchers.equalTo(3500));
    }

    // ─── SAME-CURRENCY-OK family ─────────────────────────────────────────────

    /**
     * CCY-SAMECCY-OK: same-currency add then subtract converge to the exact integer balance — the
     * fail-closed guard never impedes legitimate same-currency arithmetic.
     */
    @Test
    @Tag("CCY-SAMECCY-OK")
    void sameCurrencyArithmetic_isExact() {
        String token = token("same");
        String ledgerId = createLedger(token, "KRW", 1000);

        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{\"minorUnits\":2500,\"currency\":\"KRW\"}")
        .when().post("/api/currency-ledgers/" + ledgerId + "/add")
        .then().statusCode(200)
            .body("balanceMinor", Matchers.equalTo(3500));

        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{\"minorUnits\":500,\"currency\":\"KRW\"}")
        .when().post("/api/currency-ledgers/" + ledgerId + "/subtract")
        .then().statusCode(200)
            .body("balanceMinor", Matchers.equalTo(3000));
    }

    // ─── EXPLICIT-CONVERT family ─────────────────────────────────────────────

    /**
     * CCY-EXPLICIT-CONVERT: a foreign-currency amount can be added ONLY through an explicit recorded
     * conversion that brings it into the ledger's currency. The balance increases by the supplied
     * converted amount and the conversion is recorded in the audit trail.
     */
    @Test
    @Tag("CCY-EXPLICIT-CONVERT")
    void explicitRecordedConversion_allowsCrossCurrencyAdd() {
        String token = token("conv");
        String ledgerId = createLedger(token, "USD", 1000);

        // Add 130000 KRW via a recorded conversion to USD 1000 (the rate is the caller's; we supply
        // the converted amount). Balance 1000 + 1000 = 2000 USD.
        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("""
                {"amount":{"minorUnits":130000,"currency":"KRW"},
                 "conversion":{"fromCurrency":"KRW","toCurrency":"USD","convertedMinorUnits":1000}}
                """)
        .when().post("/api/currency-ledgers/" + ledgerId + "/add-converted")
        .then().statusCode(200)
            .body("balanceMinor", Matchers.equalTo(2000))
            .body("currency", Matchers.equalTo("USD"))
            .body("conversions.size()", Matchers.equalTo(1))
            .body("conversions[0].fromCurrency", Matchers.equalTo("KRW"))
            .body("conversions[0].toCurrency", Matchers.equalTo("USD"))
            .body("conversions[0].sourceMinor", Matchers.equalTo(130000))
            .body("conversions[0].convertedMinor", Matchers.equalTo(1000));
    }

    /**
     * CCY-EXPLICIT-CONVERT: a conversion whose toCurrency does NOT match the ledger still fails
     * closed — the explicit seam is not a bypass of the currency tag.
     */
    @Test
    @Tag("CCY-EXPLICIT-CONVERT")
    void conversionToWrongCurrency_stillFailsClosed() {
        String token = token("convwrong");
        String ledgerId = createLedger(token, "USD", 1000);

        // Convert KRW → EUR, but the ledger is USD: the subsequent same-currency add throws 422.
        given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("""
                {"amount":{"minorUnits":130000,"currency":"KRW"},
                 "conversion":{"fromCurrency":"KRW","toCurrency":"EUR","convertedMinorUnits":900}}
                """)
        .when().post("/api/currency-ledgers/" + ledgerId + "/add-converted")
        .then().statusCode(422)
            .body("code", Matchers.equalTo("CURRENCY_MISMATCH"));

        // Balance unchanged — the mis-targeted conversion mutated nothing.
        read(token, ledgerId).body("balanceMinor", Matchers.equalTo(1000));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private String token(String prefix) {
        return CurrencyArithmeticTestSupport.obtainToken(
            CurrencyArithmeticTestSupport.freshEmail(prefix), "MEMBER");
    }

    private String createLedger(String token, String currency, long openingMinor) {
        return given().header("Authorization", "Bearer " + token).contentType(ContentType.JSON)
            .body("{\"currency\":\"" + currency + "\",\"openingMinor\":" + openingMinor + "}")
        .when().post("/api/currency-ledgers")
        .then().statusCode(201)
            .extract().path("id");
    }

    private ValidatableResponse read(String token, String ledgerId) {
        return given().header("Authorization", "Bearer " + token)
        .when().get("/api/currency-ledgers/" + ledgerId)
        .then().statusCode(200);
    }
}
