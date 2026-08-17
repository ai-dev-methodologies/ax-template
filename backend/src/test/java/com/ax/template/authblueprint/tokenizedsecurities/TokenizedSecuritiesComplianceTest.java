package com.ax.template.authblueprint.tokenizedsecurities;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("TOKENIZED_SECURITIES")
class TokenizedSecuritiesComplianceTest {

    @LocalServerPort int port;
    String member;
    String admin;
    /** Per-test holder prefix — makes holder IDs unique across test methods so the
     *  global unique(holder_id) ownership table never collides across tests. */
    String hp;

    @BeforeEach
    void setup() {
        member = TokenizedSecuritiesTestSupport.obtainToken(TokenizedSecuritiesTestSupport.freshEmail("ts-member"), "MEMBER");
        admin = TokenizedSecuritiesTestSupport.obtainToken(TokenizedSecuritiesTestSupport.freshEmail("ts-admin"), "ADMIN");
        hp = UUID.randomUUID().toString().replace("-", "").substring(0, 8) + "-";
    }

    // ---- helpers -------------------------------------------------------------
    private String createToken(String issuer, long total, Instant lockupUntil, long limit) {
        return createToken(issuer, "ASSET-" + UUID.randomUUID(), total, lockupUntil, limit);
    }

    private String createToken(String issuer, String underlyingAssetId, long total,
                               Instant lockupUntil, long limit) {
        String code = "TS-" + UUID.randomUUID();
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"tokenCode\":\"" + code + "\",\"underlyingAssetId\":\"" + underlyingAssetId
                + "\",\"securityType\":\"TRUST_BENEFICIARY\",\"totalUnits\":" + total
                + ",\"issuerHolderId\":\"" + issuer + "\",\"lockupUntil\":\"" + lockupUntil + "\""
                + ",\"holdingLimitPerInvestor\":" + limit + "}")
        .when().post("/api/security-tokens")
        .then().statusCode(201);
        return code;
    }

    private void grant(String code, String holder) {
        given().header("Authorization", "Bearer " + admin).header("Content-Type", "application/json")
            .body("{\"holderId\":\"" + holder + "\"}")
        .when().post("/api/security-tokens/" + code + "/eligible-investors")
        .then().statusCode(201);
    }

    private void claimOwnership(String holderId) {
        given().header("Authorization", "Bearer " + member)
                .when().post("/api/security-tokens/holders/" + holderId + "/ownership")
                .then().statusCode(201);
    }

    private io.restassured.response.Response transfer(String code, String from, String to, long units, String tid) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"fromHolderId\":\"" + from + "\",\"toHolderId\":\"" + to + "\",\"units\":" + units
                + ",\"transferId\":\"" + tid + "\"}")
        .when().post("/api/security-tokens/" + code + "/transfers");
    }

    private long heldSum(String code) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/security-tokens/" + code)
            .then().statusCode(200).extract().jsonPath().getLong("heldSum");
    }

    private long unitsOf(String code, String holder) {
        return given().header("Authorization", "Bearer " + member)
            .when().get("/api/security-tokens/" + code)
            .then().statusCode(200).extract().jsonPath()
            .getLong("holdings.find { it.holderId == '" + holder + "' }?.units ?: 0");
    }

    private final Instant past = Instant.now().minus(1, ChronoUnit.DAYS);
    private final Instant future = Instant.now().plus(1, ChronoUnit.DAYS);

    // ---- TS-TRANSFER-001 — recipient eligibility is a hard gate --------------
    @Test @Tag("TS-TRANSFER-001")
    void ungrantedRecipient_isRejected_422_registerUnchanged() {
        String issuer = hp + "ISSUER";
        String code = createToken(issuer, 1000, past, 1000);
        claimOwnership(issuer);
        issue(code);
        long before = heldSum(code);
        transfer(code, issuer, hp + "ALICE", 10, "t1")
                .then().statusCode(422).body("code", equalTo("TS_INELIGIBLE_RECIPIENT"));
        org.assertj.core.api.Assertions.assertThat(heldSum(code)).isEqualTo(before);
        org.assertj.core.api.Assertions.assertThat(unitsOf(code, hp + "ALICE")).isZero();
    }

    // ---- TS-TRANSFER-002 — lock-up -----------------------------------------
    @Test @Tag("TS-TRANSFER-002")
    void transferDuringLockup_isRejected_422() {
        String issuer = hp + "ISSUER";
        String alice = hp + "ALICE";
        String code = createToken(issuer, 1000, future, 1000);
        claimOwnership(issuer);
        issue(code);
        grant(code, alice);
        transfer(code, issuer, alice, 10, "t1").then().statusCode(422).body("code", equalTo("TS_LOCKUP_ACTIVE"));
        org.assertj.core.api.Assertions.assertThat(unitsOf(code, alice)).isZero();
        org.assertj.core.api.Assertions.assertThat(unitsOf(code, issuer)).isEqualTo(1000);
        org.assertj.core.api.Assertions.assertThat(heldSum(code)).isEqualTo(1000);
    }

    // ---- TS-TRANSFER-003 — issuer exemption from holding limit (buyback) ----
    @Test @Tag("TS-TRANSFER-003")
    void issuerIsExemptFromHoldingLimit_buyback() {
        String issuer = hp + "ISSUER";
        String alice = hp + "ALICE";
        String code = createToken(issuer, 1000, past, 50);
        claimOwnership(issuer);
        claimOwnership(alice);  // ALICE transfers back to ISSUER in t2
        issue(code);
        grant(code, alice);
        transfer(code, issuer, alice, 40, "t1").then().statusCode(200);
        // buyback: ALICE returns 40 to ISSUER; issuer would now hold 960 > limit=50 without the exemption
        transfer(code, alice, issuer, 40, "t2").then().statusCode(200);
        org.assertj.core.api.Assertions.assertThat(unitsOf(code, issuer)).isEqualTo(1000);
        org.assertj.core.api.Assertions.assertThat(unitsOf(code, alice)).isZero();
        org.assertj.core.api.Assertions.assertThat(heldSum(code)).isEqualTo(1000);
    }

    // ---- TS-TRANSFER-003 — holding limit ------------------------------------
    @Test @Tag("TS-TRANSFER-003")
    void transferExceedingHoldingLimit_isRejected_422() {
        String issuer = hp + "ISSUER";
        String alice = hp + "ALICE";
        String code = createToken(issuer, 1000, past, 100);
        claimOwnership(issuer);
        issue(code);
        grant(code, alice);
        transfer(code, issuer, alice, 60, "t1").then().statusCode(200);
        transfer(code, issuer, alice, 60, "t2").then().statusCode(422).body("code", equalTo("TS_HOLDING_LIMIT_EXCEEDED"));
        org.assertj.core.api.Assertions.assertThat(unitsOf(code, alice)).isEqualTo(60);
    }

    // ---- TS-TRANSFER-004 — sender balance -----------------------------------
    @Test @Tag("TS-TRANSFER-004")
    void transferExceedingSenderBalance_isRejected_422() {
        String issuer = hp + "ISSUER";
        String alice = hp + "ALICE";
        String bob = hp + "BOB";
        String code = createToken(issuer, 1000, past, 1000);
        claimOwnership(issuer);
        claimOwnership(alice);  // ALICE transfers to BOB in t2
        issue(code);
        grant(code, alice);
        grant(code, bob);
        transfer(code, issuer, alice, 10, "t1").then().statusCode(200);
        transfer(code, alice, bob, 11, "t2").then().statusCode(422).body("code", equalTo("TS_INSUFFICIENT_UNITS"));
        org.assertj.core.api.Assertions.assertThat(unitsOf(code, alice)).isEqualTo(10);
        org.assertj.core.api.Assertions.assertThat(unitsOf(code, bob)).isZero();
    }

    // ---- TS-TRANSFER-005 — atomic settlement + conservation -----------------
    @Test @Tag("TS-TRANSFER-005")
    void fullyGatedTransfer_debitsCreditsAtomically_conservesTotal() {
        String issuer = hp + "ISSUER";
        String alice = hp + "ALICE";
        String code = createToken(issuer, 1000, past, 1000);
        claimOwnership(issuer);
        issue(code);
        grant(code, alice);
        transfer(code, issuer, alice, 40, "t1").then().statusCode(200)
            .body("fromHolderId", equalTo(issuer)).body("toHolderId", equalTo(alice)).body("units", equalTo(40));
        org.assertj.core.api.Assertions.assertThat(unitsOf(code, issuer)).isEqualTo(960);
        org.assertj.core.api.Assertions.assertThat(unitsOf(code, alice)).isEqualTo(40);
        org.assertj.core.api.Assertions.assertThat(heldSum(code)).isEqualTo(1000);  // conservation
    }

    // ---- TS-TRANSFER-006 — idempotency --------------------------------------
    @Test @Tag("TS-TRANSFER-006")
    void replayingSameTransferId_doesNotReMutate() {
        String issuer = hp + "ISSUER";
        String alice = hp + "ALICE";
        String code = createToken(issuer, 1000, past, 1000);
        claimOwnership(issuer);
        issue(code);
        grant(code, alice);
        transfer(code, issuer, alice, 40, "t1").then().statusCode(200);
        transfer(code, issuer, alice, 40, "t1").then().statusCode(200);  // replay
        org.assertj.core.api.Assertions.assertThat(unitsOf(code, alice)).isEqualTo(40);   // not 80
        org.assertj.core.api.Assertions.assertThat(unitsOf(code, issuer)).isEqualTo(960);
        org.assertj.core.api.Assertions.assertThat(heldSum(code)).isEqualTo(1000);
    }

    // ---- TS-TRANSFER-007 — fail-closed default + admin grant flips it --------
    @Test @Tag("TS-TRANSFER-007")
    void eligibilityIsDenyByDefault_adminGrantEnables_nonAdminGrantForbidden() {
        String issuer = hp + "ISSUER";
        String alice = hp + "ALICE";
        String code = createToken(issuer, 1000, past, 1000);
        claimOwnership(issuer);
        issue(code);
        // pre-grant: deny-by-default (eligibility gate fails, HOLDER-AUTHZ already passes)
        transfer(code, issuer, alice, 10, "t1").then().statusCode(422).body("code", equalTo("TS_INELIGIBLE_RECIPIENT"));
        // non-admin grant attempt → 403
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"holderId\":\"" + alice + "\"}")
            .when().post("/api/security-tokens/" + code + "/eligible-investors").then().statusCode(403);
        // admin grant → 201, then transfer succeeds
        grant(code, alice);
        transfer(code, issuer, alice, 10, "t2").then().statusCode(200);
        org.assertj.core.api.Assertions.assertThat(unitsOf(code, alice)).isEqualTo(10);
    }

    // ---- REG-ISSUE-001 — one underlying asset backs at most one security -----
    @Test @Tag("REG-ISSUE-001")
    void oneUnderlyingAsset_backsAtMostOneSecurity() {
        String assetId = "ASSET-FIXED-" + UUID.randomUUID();
        // first registration with this asset → 201
        createToken("ISSUER", assetId, 1000, past, 1000);
        // second registration with the same asset, different tokenCode → 409
        String code2 = "TS-" + UUID.randomUUID();
        given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"tokenCode\":\"" + code2 + "\",\"underlyingAssetId\":\"" + assetId
                + "\",\"securityType\":\"TRUST_BENEFICIARY\",\"totalUnits\":500"
                + ",\"issuerHolderId\":\"ISSUER2\",\"lockupUntil\":\"" + past + "\""
                + ",\"holdingLimitPerInvestor\":500}")
        .when().post("/api/security-tokens")
        .then().statusCode(409).body("code", equalTo("TS_DUPLICATE_UNDERLYING_ASSET"));
        // third registration with a different asset → 201
        createToken("ISSUER3", "ASSET-" + UUID.randomUUID(), 500, past, 500);
    }

    // ---- REG-ISSUE-002 — issued supply is final; Σ holdings == totalUnits ----
    @Test @Tag("REG-ISSUE-002")
    void issuedSupplyIsFinal_conservedAcrossLifecycle() {
        String issuer = hp + "ISSUER";
        String alice = hp + "ALICE";
        String bob = hp + "BOB";
        String code = createToken(issuer, 1000, past, 1000);
        claimOwnership(issuer);
        issue(code);
        // after issue: issuer holds full supply; conservation begins here
        org.assertj.core.api.Assertions.assertThat(heldSum(code)).isEqualTo(1000);
        grant(code, alice);
        grant(code, bob);
        transfer(code, issuer, alice, 40, "t1-ri2").then().statusCode(200);
        transfer(code, issuer, alice, 30, "t2-ri2").then().statusCode(200);
        transfer(code, issuer, bob,   20, "t3-ri2").then().statusCode(200);
        org.assertj.core.api.Assertions.assertThat(heldSum(code)).isEqualTo(1000);
        org.assertj.core.api.Assertions.assertThat(unitsOf(code, issuer)).isEqualTo(910);
    }

    // ---- HOLDER-AUTHZ-001 — uncontrolled holder rejected 403 ------------------
    @Test @Tag("HOLDER-AUTHZ-001")
    void transferFromUncontrolledHolder_isRejected_403_registerUnchanged() {
        String h1 = "H1-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String recipient = "REC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String code = createToken(h1, 1000, past, 1000);
        claimOwnership(h1);               // member (caller A) claims h1
        issue(code);
        grant(code, recipient);           // admin grants eligibility to recipient
        // caller A transfers from h1 → 200
        transfer(code, h1, recipient, 100, "authz1-t1").then().statusCode(200);
        long balH1 = unitsOf(code, h1);
        long balRec = unitsOf(code, recipient);
        // caller B (different member) tries to transfer from h1 → 403
        String memberB = TokenizedSecuritiesTestSupport.obtainToken(
                TokenizedSecuritiesTestSupport.freshEmail("ts-mb"), "MEMBER");
        given().header("Authorization", "Bearer " + memberB).header("Content-Type", "application/json")
                .body("{\"fromHolderId\":\"" + h1 + "\",\"toHolderId\":\"" + recipient
                        + "\",\"units\":50,\"transferId\":\"authz1-t2\"}")
                .when().post("/api/security-tokens/" + code + "/transfers")
                .then().statusCode(403).body("code", equalTo("TS_NOT_HOLDER_CONTROLLER"));
        // register unchanged
        org.assertj.core.api.Assertions.assertThat(unitsOf(code, h1)).isEqualTo(balH1);
        org.assertj.core.api.Assertions.assertThat(unitsOf(code, recipient)).isEqualTo(balRec);
    }

    // ---- ANCHOR-001 — every applied transfer carries a non-null anchorRef -----
    @Test @Tag("TOKENIZED_SECURITIES") @Tag("ANCHOR-001")
    void everyTransferCarriesAnAnchorRef() {
        String issuer = hp + "ISSUER";
        String alice = hp + "ALICE";
        String code = createToken(issuer, 1000, past, 1000);
        claimOwnership(issuer);
        issue(code);
        grant(code, alice);
        transfer(code, issuer, alice, 40, "anchor-t1")
                .then().statusCode(200)
                .body("anchorRef", org.hamcrest.Matchers.notNullValue())
                .body("anchorRef", org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyOrNullString()));
    }

    // ---- ISSUE-001 — DRAFT token cannot be transferred -------------------
    @Test @Tag("TOKENIZED_SECURITIES") @Tag("ISSUE-001")
    void draftTokenCannotBeTransferred() {
        String issuer = hp + "ISSUER";
        String alice = hp + "ALICE";
        String code = createToken(issuer, 1000, past, 1000);
        // DRAFT: any transfer is 409 TS_NOT_ISSUED, register unchanged (no holdings yet)
        transfer(code, issuer, alice, 10, "issue001-t1")
                .then().statusCode(409).body("code", equalTo("TS_NOT_ISSUED"));
        org.assertj.core.api.Assertions.assertThat(heldSum(code)).isZero();
        // issue → ISSUED; full setup → same transfer path now reaches normal gates.
        // claimOwnership(issuer) must run BEFORE issue() so the member controls issuerHolder
        // (after F3, issue() auto-claims issuerHolderId for the calling admin; a prior claim
        // by member takes precedence — fail-safe, no overwrite).
        claimOwnership(issuer);
        issue(code);
        grant(code, alice);
        transfer(code, issuer, alice, 10, "issue001-t2").then().statusCode(200);
        org.assertj.core.api.Assertions.assertThat(unitsOf(code, alice)).isEqualTo(10);
    }

    // ---- ISSUE-002 — issue is ADMIN-only, one-way, seeds issuer holding once --
    @Test @Tag("TOKENIZED_SECURITIES") @Tag("ISSUE-002")
    void issueIsAdminOnly_oneWay_seedsIssuerOnce() {
        String issuer = hp + "ISSUER";
        String code = createToken(issuer, 1000, past, 1000);
        // non-admin issue → 403
        given().header("Authorization", "Bearer " + member)
                .when().post("/api/security-tokens/" + code + "/issue")
                .then().statusCode(403);
        // admin issue → 200, ISSUED, issuer holds totalUnits
        given().header("Authorization", "Bearer " + admin)
                .when().post("/api/security-tokens/" + code + "/issue")
                .then().statusCode(200).body("issuanceStatus", equalTo("ISSUED"));
        org.assertj.core.api.Assertions.assertThat(heldSum(code)).isEqualTo(1000);
        // second issue → 409 TS_ALREADY_ISSUED
        given().header("Authorization", "Bearer " + admin)
                .when().post("/api/security-tokens/" + code + "/issue")
                .then().statusCode(409).body("code", equalTo("TS_ALREADY_ISSUED"));
        // second (rejected) issue must NOT double-seed the issuer holding
        org.assertj.core.api.Assertions.assertThat(heldSum(code)).isEqualTo(1000);
    }

    // ---- ISSUE-002 concurrent keystone — issue() is single-winner under a race --
    @Test @Tag("TOKENIZED_SECURITIES") @Tag("ISSUE-002")
    void concurrentIssue_exactlyOneWins_registerConserved() throws Exception {
        String issuer = hp + "CONC-ISS";
        long total = 1000;
        String code = createToken(issuer, total, past, total);

        int n = 2;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<io.restassured.response.Response> results = new ConcurrentLinkedQueue<>();
        java.util.List<java.util.concurrent.Future<?>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                start.await();
                results.add(given().header("Authorization", "Bearer " + admin)
                        .when().post("/api/security-tokens/" + code + "/issue"));
                return null;
            }));
        }
        // worker-ready barrier: open the gate only once BOTH workers are parked on it,
        // so the two requests genuinely race instead of running back-to-back.
        org.assertj.core.api.Assertions.assertThat(ready.await(30, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        pool.shutdown();
        org.assertj.core.api.Assertions.assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();
        for (java.util.concurrent.Future<?> f : futures) {
            f.get(); // surface any worker exception instead of a confusing count mismatch
        }

        long succeeded = results.stream().filter(r -> r.statusCode() == 200).count();
        long conflicted = results.stream().filter(r -> r.statusCode() == 409).count();
        org.assertj.core.api.Assertions.assertThat(succeeded)
            .as("ISSUE-002 keystone — exactly one concurrent issue() wins the race").isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(conflicted).isEqualTo(1);
        results.stream().filter(r -> r.statusCode() == 409).forEach(r ->
            org.assertj.core.api.Assertions.assertThat(r.jsonPath().getString("code")).isEqualTo("TS_ALREADY_ISSUED"));

        // register invariant survives the race: no double-seed, Σ holdings == totalUnits
        org.assertj.core.api.Assertions.assertThat(heldSum(code)).isEqualTo(total);
    }

    // ---- READ-HOLDER-001 — GET owner returns 200 if claimed, 404 if not ----
    @Test @Tag("READ-HOLDER-001")
    void getOwner_claimed_returns200_unclaimed_returns404() {
        String holder = hp + "OWN-READ";
        // unclaimed → 404
        given().header("Authorization", "Bearer " + member)
                .when().get("/api/security-tokens/holders/" + holder + "/owner")
                .then().statusCode(404);
        // claim → 201
        claimOwnership(holder);
        // claimed → 200 with ownerPrincipal populated
        given().header("Authorization", "Bearer " + member)
                .when().get("/api/security-tokens/holders/" + holder + "/owner")
                .then().statusCode(200)
                .body("holderId", equalTo(holder))
                .body("ownerPrincipal", org.hamcrest.Matchers.notNullValue());
    }

    // ---- READ-ELIGIBLE-001 — GET eligible-investors returns page, 404 for bad tokenCode ---
    @Test @Tag("READ-ELIGIBLE-001")
    void listEligibleInvestors_paged_404ForUnknownToken() {
        // nonexistent tokenCode → 404 (ADMIN-only, so use admin token)
        given().header("Authorization", "Bearer " + admin)
                .when().get("/api/security-tokens/NONEXISTENT-TOKEN/eligible-investors")
                .then().statusCode(404);
        // create a token, grant two investors, list them
        String issuer = hp + "ELG-ISS";
        String a1 = hp + "ELG-A1";
        String a2 = hp + "ELG-A2";
        String code = createToken(issuer, 1000, past, 1000);
        grant(code, a1);
        grant(code, a2);
        given().header("Authorization", "Bearer " + admin)
                .when().get("/api/security-tokens/" + code + "/eligible-investors?page=0&size=20")
                .then().statusCode(200)
                .body("data.size()", equalTo(2))
                .body("pagination.totalElements", equalTo(2));
        // non-admin → 403
        given().header("Authorization", "Bearer " + member)
                .when().get("/api/security-tokens/" + code + "/eligible-investors")
                .then().statusCode(403);
    }

    // ---- ISSUE-003 — issue() auto-claims issuerHolderId → calling principal ----
    @Test @Tag("ISSUE-003")
    void issue_autoClaimsIssuerHolder_forCallingPrincipal() {
        String issuer = hp + "AC-ISS";
        String code = createToken(issuer, 500, past, 500);
        // no prior claim on issuerHolder
        given().header("Authorization", "Bearer " + member)
                .when().get("/api/security-tokens/holders/" + issuer + "/owner")
                .then().statusCode(404);
        // admin issues → issuerHolder auto-claimed for admin principal
        issue(code);
        given().header("Authorization", "Bearer " + member)
                .when().get("/api/security-tokens/holders/" + issuer + "/owner")
                .then().statusCode(200).body("holderId", equalTo(issuer));
        // Scenario A: admin (auto-claimant) can transfer immediately — no separate claimOwnership call needed
        String recipient = hp + "AC-REC";
        grant(code, recipient);
        given().header("Authorization", "Bearer " + admin)
                .header("Content-Type", "application/json")
                .body("{\"fromHolderId\":\"" + issuer + "\",\"toHolderId\":\"" + recipient
                        + "\",\"units\":10,\"transferId\":\"ac-iss-t1\"}")
                .when().post("/api/security-tokens/" + code + "/transfers")
                .then().statusCode(200);
    }

    // ---- ISSUE-003 — pre-claimed by different principal → auto-claim skipped ----
    @Test @Tag("ISSUE-003")
    void issue_doesNotOverwrite_ifIssuerHolderAlreadyClaimed() {
        String issuer = hp + "AC-PRE";
        String code = createToken(issuer, 500, past, 500);
        // member pre-claims issuerHolder BEFORE issue
        claimOwnership(issuer);
        // admin issues → auto-claim is skipped; member still controls issuerHolder
        issue(code);
        // member (who pre-claimed) can transfer; issuerHolder owner unchanged
        grant(code, hp + "REC");
        transfer(code, issuer, hp + "REC", 10, "ac-pre-t1").then().statusCode(200);
    }

    // ---- helper: issue a token as admin --------------------------------
    private void issue(String code) {
        given().header("Authorization", "Bearer " + admin)
                .when().post("/api/security-tokens/" + code + "/issue")
                .then().statusCode(200);
    }

    // ---- ANCHOR-002 — reconcile converges after real write-through transfers --
    @Test @Tag("TOKENIZED_SECURITIES") @Tag("ANCHOR-002")
    void reconcileConvergesAfterRealTransfers() {
        String issuer = hp + "ISSUER";
        String alice = hp + "ALICE";
        String code = createToken(issuer, 1000, past, 1000);
        claimOwnership(issuer);
        issue(code);
        grant(code, alice);
        transfer(code, issuer, alice, 40, "anchor-r1").then().statusCode(200);
        given().header("Authorization", "Bearer " + member)
                .when().get("/api/security-tokens/" + code + "/reconcile")
                .then().statusCode(200)
                .body("converged", equalTo(true))
                .body("breaks", org.hamcrest.Matchers.empty());
    }

    // ---- HOLDER-AUTHZ-002 — deny-by-default, first-claim-wins -----------------
    @Test @Tag("HOLDER-AUTHZ-002")
    void holderOwnership_denyByDefault_firstClaimWins() {
        String h2 = "H2-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        // claim h2 as A (member) → 201
        given().header("Authorization", "Bearer " + member)
                .when().post("/api/security-tokens/holders/" + h2 + "/ownership")
                .then().statusCode(201);
        // claim h2 as B → 409
        String memberB = TokenizedSecuritiesTestSupport.obtainToken(
                TokenizedSecuritiesTestSupport.freshEmail("ts-mb2"), "MEMBER");
        given().header("Authorization", "Bearer " + memberB)
                .when().post("/api/security-tokens/holders/" + h2 + "/ownership")
                .then().statusCode(409).body("code", equalTo("TS_HOLDER_ALREADY_OWNED"));
        // re-claim h2 as A → idempotent 200 OK (HOLDER-AUTHZ-002 / F4 closure)
        given().header("Authorization", "Bearer " + member)
                .when().post("/api/security-tokens/holders/" + h2 + "/ownership")
                .then().statusCode(200);
        // transfer from h2 succeeds for A, rejected for B
        String code = createToken(h2, 1000, past, 1000);
        String alice = "ALICE2-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        grant(code, alice);
        issue(code);
        transfer(code, h2, alice, 100, "authz2-t1").then().statusCode(200);
        given().header("Authorization", "Bearer " + memberB).header("Content-Type", "application/json")
                .body("{\"fromHolderId\":\"" + h2 + "\",\"toHolderId\":\"" + alice
                        + "\",\"units\":50,\"transferId\":\"authz2-t2\"}")
                .when().post("/api/security-tokens/" + code + "/transfers")
                .then().statusCode(403).body("code", equalTo("TS_NOT_HOLDER_CONTROLLER"));
    }
}
