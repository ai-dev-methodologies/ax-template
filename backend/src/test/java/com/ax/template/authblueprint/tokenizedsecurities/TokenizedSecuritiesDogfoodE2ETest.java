package com.ax.template.authblueprint.tokenizedsecurities;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Dogfood E2E composition test — exercises all 5 seams of the tokenized-securities domain
 * in a single realistic STO lifecycle flow:
 *
 * <pre>
 *   ISSUE-LIFECYCLE → REG/ISSUE → HOLDER-AUTHZ → TRANSFER-eligibility → TRANSFER+ANCHOR
 * </pre>
 *
 * <p>Persona: fork-receiver building a Korean STO product who tries to USE all 5 seams
 * composed together in one realistic sequence. Every friction/gap discovered during this
 * composition pass is logged in docs/dogfood-ledger/sto-generic-seams-iter1.md.
 *
 * <p>Steps exercised:
 * <ol>
 *   <li>createToken → status DRAFT (ISSUE-LIFECYCLE)</li>
 *   <li>transfer on DRAFT → 409 TS_NOT_ISSUED (issue-gate is first, before caller-authz)</li>
 *   <li>claim issuerHolder BEFORE issue (F3 closure: pre-claim wins, auto-claim skipped)</li>
 *   <li>issue (ADMIN) → ISSUED, issuer holds totalUnits (REG/ISSUE)</li>
 *   <li>claim investorHolder (HOLDER-AUTHZ)</li>
 *   <li>grant eligibility (ADMIN) for investorHolder (TRANSFER eligibility)</li>
 *   <li>transfer issuer→investor → 200, balances move, Σ conserved, anchorRef present (TRANSFER+ANCHOR)</li>
 *   <li>reconcile → converged=true (ANCHOR-002)</li>
 *   <li>Negatives (in same flow): uncontrolled-holder→403, ungranted-holder→422, limit-exceeded→422</li>
 * </ol>
 *
 * Run: {@code ./gradlew testTokenizedSecurities}
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Tag("TOKENIZED_SECURITIES")
class TokenizedSecuritiesDogfoodE2ETest {

    @LocalServerPort int port;

    /** Member who claims issuerHolder and initiates transfers. */
    String issuerMember;
    /** Member who claims investorHolder; used for the AUTHZ negative (does NOT control issuerHolder). */
    String investorMember;
    String admin;
    String issuerHolder;
    String investorHolder;

    @BeforeEach
    void setup() {
        TokenizedSecuritiesTestSupport.useRandomPort(port);
        String prefix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        issuerMember   = TokenizedSecuritiesTestSupport.obtainToken(
                TokenizedSecuritiesTestSupport.freshEmail("e2e-iss-" + prefix), "MEMBER");
        investorMember = TokenizedSecuritiesTestSupport.obtainToken(
                TokenizedSecuritiesTestSupport.freshEmail("e2e-inv-" + prefix), "MEMBER");
        admin          = TokenizedSecuritiesTestSupport.obtainToken(
                TokenizedSecuritiesTestSupport.freshEmail("e2e-adm-" + prefix), "ADMIN");
        issuerHolder   = "ISS-E2E-" + prefix;
        investorHolder = "INV-E2E-" + prefix;
    }

    @Test
    void stoLifecycle_allSeamsComposed() {
        // ── params ────────────────────────────────────────────────────────────
        long   totalUnits    = 1_000L;
        long   holdingLimit  = 200L;
        long   transferAmt   = 150L;   // first transfer; investor will hold 150 (< limit 200)
        long   overLimitAmt  = 60L;    // 150 + 60 = 210 > limit=200 → must be 422
        Instant lockupPast   = Instant.now().minus(1, ChronoUnit.DAYS);
        String tokenCode     = "STO-E2E-" + UUID.randomUUID();
        String ungrantedHolder = "UNGRANTED-" + UUID.randomUUID();

        // ── step 1: createToken → status DRAFT (ISSUE-LIFECYCLE) ─────────────
        given().header("Authorization", "Bearer " + issuerMember)
                .header("Content-Type", "application/json")
                .body("{\"tokenCode\":\"" + tokenCode + "\","
                        + "\"underlyingAssetId\":\"ASSET-" + UUID.randomUUID() + "\","
                        + "\"securityType\":\"TRUST_BENEFICIARY\","
                        + "\"totalUnits\":" + totalUnits + ","
                        + "\"issuerHolderId\":\"" + issuerHolder + "\","
                        + "\"lockupUntil\":\"" + lockupPast + "\","
                        + "\"holdingLimitPerInvestor\":" + holdingLimit + "}")
                .when().post("/api/security-tokens")
                .then().statusCode(201)
                .body("issuanceStatus", equalTo("DRAFT"))
                .body("tokenCode", equalTo(tokenCode));

        // ── step 2: transfer on DRAFT → 409 TS_NOT_ISSUED ────────────────────
        // ISSUE-001 gate fires first (before HOLDER-AUTHZ); even though issuerMember has
        // not yet claimed issuerHolder, the ISSUED check is the outermost gate.
        given().header("Authorization", "Bearer " + issuerMember)
                .header("Content-Type", "application/json")
                .body("{\"fromHolderId\":\"" + issuerHolder + "\","
                        + "\"toHolderId\":\"" + investorHolder + "\","
                        + "\"units\":10,"
                        + "\"transferId\":\"draft-block-t1\"}")
                .when().post("/api/security-tokens/" + tokenCode + "/transfers")
                .then().statusCode(409)
                .body("code", equalTo("TS_NOT_ISSUED"));

        // ── step 3: claim issuerHolder BEFORE issue (HOLDER-AUTHZ seam, F3 closure) ──────
        // F3 closure: issue() now auto-claims issuerHolder for the calling admin principal.
        // issuerMember claims first so they (not admin) control the issuer holder post-issue.
        // issue() sees it already claimed → skips auto-claim (fail-safe, no overwrite).
        given().header("Authorization", "Bearer " + issuerMember)
                .when().post("/api/security-tokens/holders/" + issuerHolder + "/ownership")
                .then().statusCode(201);

        // ── step 4: issue (ADMIN) → ISSUED; issuer holds totalUnits (REG/ISSUE) ──
        given().header("Authorization", "Bearer " + admin)
                .when().post("/api/security-tokens/" + tokenCode + "/issue")
                .then().statusCode(200)
                .body("issuanceStatus", equalTo("ISSUED"));

        long heldAfterIssue = given().header("Authorization", "Bearer " + issuerMember)
                .when().get("/api/security-tokens/" + tokenCode)
                .then().statusCode(200).extract().jsonPath().getLong("heldSum");
        assertThat(heldAfterIssue).isEqualTo(totalUnits);

        // ── step 5: claim investorHolder (HOLDER-AUTHZ seam) ─────────────────
        given().header("Authorization", "Bearer " + investorMember)
                .when().post("/api/security-tokens/holders/" + investorHolder + "/ownership")
                .then().statusCode(201);

        // ── step 6: grant eligibility (ADMIN) for investorHolder ─────────────
        given().header("Authorization", "Bearer " + admin)
                .header("Content-Type", "application/json")
                .body("{\"holderId\":\"" + investorHolder + "\"}")
                .when().post("/api/security-tokens/" + tokenCode + "/eligible-investors")
                .then().statusCode(201);

        // ── step 7: transfer issuer→investor → 200, balances move, Σ conserved,
        //            anchorRef present (TRANSFER+ANCHOR seams composed) ─────────
        given().header("Authorization", "Bearer " + issuerMember)
                .header("Content-Type", "application/json")
                .body("{\"fromHolderId\":\"" + issuerHolder + "\","
                        + "\"toHolderId\":\"" + investorHolder + "\","
                        + "\"units\":" + transferAmt + ","
                        + "\"transferId\":\"e2e-t1\"}")
                .when().post("/api/security-tokens/" + tokenCode + "/transfers")
                .then().statusCode(200)
                .body("fromHolderId", equalTo(issuerHolder))
                .body("toHolderId",   equalTo(investorHolder))
                .body("units",        equalTo((int) transferAmt))
                .body("anchorRef",    not(emptyOrNullString())); // ANCHOR-001

        // assert balances and conservation invariant
        var snapshot = given().header("Authorization", "Bearer " + issuerMember)
                .when().get("/api/security-tokens/" + tokenCode)
                .then().statusCode(200).extract().jsonPath();
        long issuerBal   = snapshot.getLong(
                "holdings.find { it.holderId == '" + issuerHolder + "' }?.units ?: 0");
        long investorBal = snapshot.getLong(
                "holdings.find { it.holderId == '" + investorHolder + "' }?.units ?: 0");
        long heldSum     = snapshot.getLong("heldSum");
        assertThat(issuerBal).isEqualTo(totalUnits - transferAmt);
        assertThat(investorBal).isEqualTo(transferAmt);
        assertThat(heldSum).isEqualTo(totalUnits); // Σ always conserved

        // ── step 8: reconcile → converged=true (ANCHOR-002) ──────────────────
        given().header("Authorization", "Bearer " + issuerMember)
                .when().get("/api/security-tokens/" + tokenCode + "/reconcile")
                .then().statusCode(200)
                .body("converged", equalTo(true))
                .body("breaks",    empty());

        // ── negative: transfer from uncontrolled holder → 403 ────────────────
        // investorMember has NOT claimed issuerHolder; HOLDER-AUTHZ fires → 403
        given().header("Authorization", "Bearer " + investorMember)
                .header("Content-Type", "application/json")
                .body("{\"fromHolderId\":\"" + issuerHolder + "\","
                        + "\"toHolderId\":\"" + investorHolder + "\","
                        + "\"units\":10,"
                        + "\"transferId\":\"neg-authz-t1\"}")
                .when().post("/api/security-tokens/" + tokenCode + "/transfers")
                .then().statusCode(403)
                .body("code", equalTo("TS_NOT_HOLDER_CONTROLLER"));

        // ── negative: transfer to ungranted holder → 422 ─────────────────────
        // ungrantedHolder has no eligibility grant; eligibility gate fires → 422
        given().header("Authorization", "Bearer " + issuerMember)
                .header("Content-Type", "application/json")
                .body("{\"fromHolderId\":\"" + issuerHolder + "\","
                        + "\"toHolderId\":\"" + ungrantedHolder + "\","
                        + "\"units\":10,"
                        + "\"transferId\":\"neg-eligibility-t1\"}")
                .when().post("/api/security-tokens/" + tokenCode + "/transfers")
                .then().statusCode(422)
                .body("code", equalTo("TS_INELIGIBLE_RECIPIENT"));

        // ── negative: transfer exceeding holding limit → 422 ─────────────────
        // investorHolder currently holds 150; transferring 60 more → 210 > limit=200
        given().header("Authorization", "Bearer " + issuerMember)
                .header("Content-Type", "application/json")
                .body("{\"fromHolderId\":\"" + issuerHolder + "\","
                        + "\"toHolderId\":\"" + investorHolder + "\","
                        + "\"units\":" + overLimitAmt + ","
                        + "\"transferId\":\"neg-limit-t1\"}")
                .when().post("/api/security-tokens/" + tokenCode + "/transfers")
                .then().statusCode(422)
                .body("code", equalTo("TS_HOLDING_LIMIT_EXCEEDED"));

        // final conservation: all negative paths must not mutate the register
        long finalSum = given().header("Authorization", "Bearer " + issuerMember)
                .when().get("/api/security-tokens/" + tokenCode)
                .then().statusCode(200).extract().jsonPath().getLong("heldSum");
        assertThat(finalSum).isEqualTo(totalUnits);
    }
}
