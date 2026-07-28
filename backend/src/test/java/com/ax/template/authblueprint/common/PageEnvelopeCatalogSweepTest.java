package com.ax.template.authblueprint.common;

import io.restassured.RestAssured;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * BACKLOG P3-54 — page-envelope parity beyond scenario-local.
 * <p>
 * {@link PageEnvelopeContractParityTest} pins ONE hand-built envelope's wire
 * shape (S2.QUERY-BOUNDS.XB). It says nothing about the OTHER 20 real list
 * endpoints across the catalog whose contracts also declare a page/list
 * envelope. This test closes that scenario-local gap for the subset of those
 * endpoints that are actually LIVE-HTTP reachable today (an existing
 * RestAssured signup+login test-support fixture exists for the domain) —
 * driving each one as a real authorized principal and asserting the emitted
 * body's top-level member SET equals that endpoint's OWN contract-declared
 * shape (envelope shapes differ per contract: {@code content/totalElements/
 * totalPages/page/size} vs {@code items/totalElements} vs
 * {@code items/page/size/totalElements} — this is per-contract parity, not
 * one fixed shape).
 *
 * <h2>Binding table (disk-verified 2026-07-28, docs/PRD-remaining-work.md §W10)</h2>
 * <ul>
 *   <li>GET /api/audit-logs — audit-log-openapi.yaml#listAuditLogs —
 *       content,totalElements,totalPages,page,size — testAuditLog</li>
 *   <li>GET /api/notifications — notification-openapi.yaml#listNotifications —
 *       content,totalElements,totalPages,page,size — testNotification</li>
 *   <li>GET /api/sessions — session-management-openapi.yaml#listSessions —
 *       items,totalElements — testSessionManagement</li>
 *   <li>GET /api/favorites — favorites-bookmarks-openapi.yaml#listFavorites —
 *       items,totalElements — testFavorites</li>
 *   <li>GET /api/activities — activity-feed-openapi.yaml#listActivities —
 *       items,page,size,totalElements — testActivityFeed</li>
 *   <li>GET /api/api-keys — api-key-openapi.yaml#listApiKeys —
 *       items,totalElements — testApiKey</li>
 *   <li>GET /api/approvals — approval-workflow-openapi.yaml#listMyApprovalRequests —
 *       items,totalElements — testApprovalWorkflow</li>
 *   <li>GET /api/approvals/inbox — approval-workflow-openapi.yaml#listApprovalInbox —
 *       items,totalElements — testApprovalWorkflow</li>
 *   <li>GET /api/admin/identity-verification —
 *       identity-verification-openapi.yaml#listVerifiedIdentities —
 *       content,page,size,totalElements,totalPages — testIdentityVerification</li>
 * </ul>
 * 8 domains / 9 endpoints swept out of the 20 contracts that declare a
 * page/list envelope on disk (ratio 8/20; disk truth =
 * `grep -lE '^ *(totalElements|totalPages):' contracts/*.yaml` -> 20 files.
 * The PRD's "21" counted an items-only edge case a mechanical grep cannot
 * reproduce). GREEN is confirmed by the wave's R25 run, not asserted here.
 * The remaining 12 are FULLY partitioned across the three pinned sets below:
 * NOT_REACHABLE (2) + REACHABLE_BUT_PREEXISTING_DRIFT (2) +
 * DECLARING_NOT_IN_SCOPE (8).
 *
 * <p><b>How "fully partitioned" is proven (2026-07-28 correction).</b> The
 * original assertion summed one int and three set sizes — arithmetic only. It
 * could not see a SWAP: moving one domain from one allowlist into another
 * keeps every size (and therefore the sum) identical while one domain silently
 * vanishes from the universe and another is double-counted. The partition is
 * now asserted SET-WISE against the declaring universe DERIVED FROM DISK:
 * pairwise disjointness of the four sets, then union == universe. Every member
 * of every set is a CONTRACT STEM (the `contracts/<stem>-openapi.yaml`
 * basename), so the comparison is against real filenames, not prose labels.
 *
 * <h2>Allowlist A — NOT reachable (no live-HTTP test-support fixture exists
 * for the domain today; a full sweep needs per-domain auth+seed, the
 * artifact class this PRD wave defers)</h2>
 * {@code email-outbox} (EmailOutboxPage — contracts/email-outbox-openapi.yaml:189)
 * and {@code scheduled-task} (ScheduledTaskPage —
 * contracts/scheduled-task-openapi.yaml:183): every existing test class in
 * both domains is a plain Mockito unit test with no
 * {@code @SpringBootTest}/RestAssured harness — see
 * {@code EmailOutboxComplianceTest} / {@code ScheduledTaskRegisterTest} et
 * al. Pinned count: {@link #NOT_REACHABLE}.size() == 2.
 *
 * <h2>Allowlist B — reachable, but the sweep found a REAL pre-existing
 * contract/implementation drift the item does not fix (out of this test's
 * file-ownership lane; registered for a follow-up closure, not papered
 * over)</h2>
 * <ul>
 *   <li><b>{@code GET /api/admin/webhook-deliveries}</b> — the contract
 *       declares {@code WebhookDeliveryPage} (content/totalElements/
 *       totalPages/page/size — contracts/webhook-openapi.yaml:355-369), but
 *       {@code WebhookAdminController#listDeliveries}
 *       (backend/src/main/.../webhook/WebhookAdminController.java:93) returns
 *       a bare {@code List<WebhookDto.DeliveryResponse>}. The RUNNING system
 *       is self-consistent — {@code WebhookAdminListStatusTest}'s own
 *       assertions type-check the root as an array, and the FE client
 *       ({@code webhookClient.ts:72}, {@code rawFetch<WebhookDelivery[]>})
 *       already expects a bare array, not an envelope. Per the SAME
 *       precedent already recorded for THIS controller/contract pair
 *       (P2-30's "reconcile the contract to the code" decision, this file's
 *       {@code WebhookAdminListStatusTest} javadoc), the contract's
 *       {@code WebhookDeliveryPage} response schema is the stale side, not
 *       the running code — but changing {@code contracts/*.yaml} is outside
 *       this lane's file ownership.</li>
 *   <li><b>{@code GET /api/v1/admin/feature-flags}</b> — the contract
 *       declares {@code FeatureFlagPage} with FIVE members including
 *       {@code totalPages} (contracts/feature-flags-openapi.yaml:197-210),
 *       but BOTH sides of the running system agree on four:
 *       {@code FeatureFlagDto.FlagPage}
 *       (backend/src/main/.../featureflags/FeatureFlagDto.java:76-81) and the
 *       FE's own {@code FeatureFlagPage} interface
 *       (frontend/apps/enterprise/.../featureFlagClient.ts:16-20) both omit
 *       {@code totalPages}. Same class of finding as the webhook row above —
 *       the contract over-declares a member neither producer nor consumer
 *       implements.</li>
 * </ul>
 * Both are genuine disk-verified findings surfaced BY this sweep — contract
 * fidelity gaps, not live functional breaks (both running sides already
 * agree with each other) — reported for BACKLOG registration + a follow-up
 * contract fix rather than asserted-and-red or silently dropped. Pinned
 * count: {@link #REACHABLE_BUT_PREEXISTING_DRIFT}.size() == 2.
 *
 * <p>Escape hatch stated in the PRD (reachable set &lt; 8 domains ⇒
 * decision-record fallback, precedent P3-41/42) does NOT apply here: the
 * reachable set is 8 domains (≥ 8), so this is the real sweep, not the
 * fallback.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("PAGINATION")
@Tag("PAGE-OFFSET-001")
class PageEnvelopeCatalogSweepTest {

    /**
     * Domains actually swept by this class — one entry per endpoint-owning
     * contract stem in the class javadoc's binding table (9 endpoints, 8
     * contracts: the two approval endpoints share
     * {@code approval-workflow-openapi.yaml}).
     */
    static final Set<String> SWEPT = Set.of(
            "audit-log", "notification", "session-management", "favorites-bookmarks",
            "activity-feed", "api-key", "approval-workflow", "identity-verification");

    /** Allowlist A — see class javadoc. */
    static final Set<String> NOT_REACHABLE = Set.of("email-outbox", "scheduled-task");

    /**
     * Allowlist B — see class javadoc. Keyed by CONTRACT STEM (the drift lives
     * in {@code webhook-openapi.yaml} / {@code feature-flags-openapi.yaml}), not
     * by endpoint path, so that the partition below compares like with like.
     */
    static final Set<String> REACHABLE_BUT_PREEXISTING_DRIFT =
            Set.of("webhook", "feature-flags");

    /**
     * Allowlist C — declaring contracts this wave did not sweep. Reachable in
     * principle; simply outside the right-sized 8-domain lane. Listed so the
     * partition is COMPLETE and the pin below is load-bearing rather than a stub.
     */
    static final Set<String> DECLARING_NOT_IN_SCOPE = Set.of(
            "billing", "comment-thread", "crud", "file-storage",
            "payment", "report-export", "tag-categorization", "tokenized-securities");

    /** Disk truth: contracts/*.yaml files declaring totalElements|totalPages. */
    static final int DECLARING_CONTRACTS = 20;

    /**
     * The declaring universe, DERIVED FROM DISK — the Java equivalent of
     * {@code grep -lE '^ *(totalElements|totalPages):' contracts/*.yaml},
     * reduced to contract stems. Derived rather than pinned so a new list
     * contract cannot join the catalog without landing in exactly one of the
     * four sets below.
     *
     * <p>Path convention follows the existing precedent in this test tree
     * ({@code WebhookIdempotentTest}: {@code Path.of("..", "templates", …)}) —
     * gradle runs tests with {@code user.dir} == {@code backend/}.
     */
    static Set<String> declaringUniverseFromDisk() throws IOException {
        Path contracts = Path.of("..", "contracts");
        assertThat(Files.isDirectory(contracts))
                .as("contracts/ must be reachable from the backend module (user.dir=%s)",
                        System.getProperty("user.dir"))
                .isTrue();
        Pattern declares = Pattern.compile("(?m)^ *(totalElements|totalPages):");
        try (Stream<Path> yamls = Files.list(contracts)) {
            return yamls
                    .filter(p -> p.getFileName().toString().endsWith("-openapi.yaml"))
                    .filter(p -> {
                        try {
                            return declares.matcher(Files.readString(p)).find();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    })
                    .map(p -> p.getFileName().toString().replace("-openapi.yaml", ""))
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    /**
     * The partition is a SET property, not an arithmetic one. Sizes alone are
     * blind to a swap between two allowlists (constant sum, one domain lost and
     * another double-counted), which is exactly the escape a cross-family
     * reviewer demonstrated against the previous sum-only assertion.
     */
    @Test
    void declaringUniverseIsExactlyPartitionedAcrossTheFourSets() throws IOException {
        Set<String> universe = declaringUniverseFromDisk();
        assertThat(universe)
                .as("disk truth: contracts declaring a page/list envelope")
                .hasSize(DECLARING_CONTRACTS);

        Map<String, Set<String>> parts = new LinkedHashMap<>();
        parts.put("SWEPT", SWEPT);
        parts.put("NOT_REACHABLE", NOT_REACHABLE);
        parts.put("REACHABLE_BUT_PREEXISTING_DRIFT", REACHABLE_BUT_PREEXISTING_DRIFT);
        parts.put("DECLARING_NOT_IN_SCOPE", DECLARING_NOT_IN_SCOPE);

        // (1) pairwise disjoint — no domain may be claimed by two sets.
        List<String> names = new ArrayList<>(parts.keySet());
        for (int i = 0; i < names.size(); i++) {
            for (int j = i + 1; j < names.size(); j++) {
                Set<String> overlap = new TreeSet<>(parts.get(names.get(i)));
                overlap.retainAll(parts.get(names.get(j)));
                assertThat(overlap)
                        .as("%s and %s must be disjoint", names.get(i), names.get(j))
                        .isEmpty();
            }
        }

        // (2) union EQUALS the universe — nothing lost, nothing invented.
        Set<String> union = new TreeSet<>();
        parts.values().forEach(union::addAll);
        assertThat(union)
                .as("the four sets must exactly cover the declaring universe")
                .isEqualTo(new TreeSet<>(universe));

        // (3) the documented counts stay pinned (a shrunk lane is a visible edit).
        assertThat(SWEPT).hasSize(8);
        assertThat(NOT_REACHABLE).hasSize(2);
        assertThat(REACHABLE_BUT_PREEXISTING_DRIFT).hasSize(2);
        assertThat(DECLARING_NOT_IN_SCOPE).hasSize(8);
    }

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    private static String freshEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    private static String obtainToken(String email, String role) {
        given()
            .header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\",\"role\":\"" + role + "\"}")
        .when().post("/api/auth/email/signup");

        return given()
            .header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\"}")
        .when().post("/api/auth/email/login")
        .then().extract().path("accessToken");
    }

    private static Set<String> membersOf(Response response) {
        Map<String, Object> root = response.jsonPath().getMap("$");
        return root.keySet();
    }

    @Test
    void auditLogList_envelopeMemberSet_matchesContract() {
        String token = obtainToken(freshEmail("sweep-audit"), "MEMBER");
        Response response = given().header("Authorization", "Bearer " + token)
            .when().get("/api/audit-logs")
            .then().statusCode(200).extract().response();
        assertThat(membersOf(response))
            .isEqualTo(Set.of("content", "totalElements", "totalPages", "page", "size"));
    }

    @Test
    void notificationList_envelopeMemberSet_matchesContract() {
        String token = obtainToken(freshEmail("sweep-notif"), "MEMBER");
        Response response = given().header("Authorization", "Bearer " + token)
            .when().get("/api/notifications")
            .then().statusCode(200).extract().response();
        assertThat(membersOf(response))
            .isEqualTo(Set.of("content", "totalElements", "totalPages", "page", "size"));
    }

    @Test
    void sessionList_envelopeMemberSet_matchesContract() {
        String token = obtainToken(freshEmail("sweep-session"), "MEMBER");
        Response response = given().header("Authorization", "Bearer " + token)
            .when().get("/api/sessions")
            .then().statusCode(200).extract().response();
        assertThat(membersOf(response)).isEqualTo(Set.of("items", "totalElements"));
    }

    @Test
    void favoriteList_envelopeMemberSet_matchesContract() {
        String token = obtainToken(freshEmail("sweep-fav"), "MEMBER");
        Response response = given().header("Authorization", "Bearer " + token)
            .when().get("/api/favorites")
            .then().statusCode(200).extract().response();
        assertThat(membersOf(response)).isEqualTo(Set.of("items", "totalElements"));
    }

    @Test
    void activityFeedList_envelopeMemberSet_matchesContract() {
        String token = obtainToken(freshEmail("sweep-activity"), "MEMBER");
        Response response = given().header("Authorization", "Bearer " + token)
            .when().get("/api/activities")
            .then().statusCode(200).extract().response();
        assertThat(membersOf(response)).isEqualTo(Set.of("items", "page", "size", "totalElements"));
    }

    @Test
    void apiKeyList_envelopeMemberSet_matchesContract() {
        String token = obtainToken(freshEmail("sweep-apikey"), "MEMBER");
        Response response = given().header("Authorization", "Bearer " + token)
            .when().get("/api/api-keys")
            .then().statusCode(200).extract().response();
        assertThat(membersOf(response)).isEqualTo(Set.of("items", "totalElements"));
    }

    @Test
    void approvalOwnList_envelopeMemberSet_matchesContract() {
        String token = obtainToken(freshEmail("sweep-approval-own"), "MEMBER");
        Response response = given().header("Authorization", "Bearer " + token)
            .when().get("/api/approvals")
            .then().statusCode(200).extract().response();
        assertThat(membersOf(response)).isEqualTo(Set.of("items", "totalElements"));
    }

    @Test
    void approvalInbox_envelopeMemberSet_matchesContract() {
        String token = obtainToken(freshEmail("sweep-approval-inbox"), "MEMBER");
        Response response = given().header("Authorization", "Bearer " + token)
            .when().get("/api/approvals/inbox")
            .then().statusCode(200).extract().response();
        assertThat(membersOf(response)).isEqualTo(Set.of("items", "totalElements"));
    }

    @Test
    void identityVerificationAdminList_envelopeMemberSet_matchesContract() {
        String token = obtainToken(freshEmail("sweep-idv-admin"), "ADMIN");
        Response response = given().header("Authorization", "Bearer " + token)
            .when().get("/api/admin/identity-verification")
            .then().statusCode(200).extract().response();
        assertThat(membersOf(response))
            .isEqualTo(Set.of("content", "page", "size", "totalElements", "totalPages"));
    }
}
