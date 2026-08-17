package com.ax.template.authblueprint.ratingsummary;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behavioral compliance tests for derived-aggregate-consistency-l0.yaml (3 invariants).
 *
 * <p>DERIVED-AGG-CONSISTENCY-001, DERIVED-AGG-ELIGIBILITY-001, DERIVED-AGG-EMPTY-001.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("RATING_SUMMARY")
class RatingSummaryComplianceTest {

    @LocalServerPort int port;

    @Autowired ReviewRepository reviewRepository;

    // ─── CONSISTENCY family ─────────────────────────────────────────────────

    /**
     * DERIVED-AGG-CONSISTENCY-001: add 3 approved reviews (5, 3, 4).
     * Stored average MUST equal (5+3+4)/3 = 4.00 AND match independent repo AVG.
     */
    @Test
    @Tag("DERIVED-AGG-CONSISTENCY-001")
    void consistency_001_storedAverageEqualsRepoAvg() {
        String token = obtainToken("cons1");
        UUID productId = UUID.randomUUID();

        addApprovedReview(token, productId, 5);
        addApprovedReview(token, productId, 3);
        addApprovedReview(token, productId, 4);

        // Assert stored aggregate via REST.
        Response resp = given().header("Authorization", "Bearer " + token)
            .when().get("/api/rating/summaries/" + productId)
            .then().statusCode(200)
                .body("reviewCount", Matchers.equalTo(3))
                .extract().response();

        BigDecimal storedAvg = new BigDecimal(resp.jsonPath().getString("average"));
        assertThat(storedAvg).isEqualByComparingTo("4.00");

        // Cross-derivation: repo AVG query (independent code path) must match.
        BigDecimal repoAvg = reviewRepository.avgStarsByProductIdAndStatus(productId, ReviewStatus.APPROVED)
            .setScale(2, RoundingMode.HALF_UP);
        assertThat(repoAvg).isEqualByComparingTo("4.00");
        long repoCount = reviewRepository.countByProductIdAndStatus(productId, ReviewStatus.APPROVED);
        assertThat(repoCount).isEqualTo(3);

        // Stored and repo averages must agree.
        assertThat(storedAvg).isEqualByComparingTo(repoAvg);
    }

    /**
     * DERIVED-AGG-CONSISTENCY-001: adding another approved review triggers recompute.
     * Average of (5, 3, 4, 2) = 14/4 = 3.50.
     */
    @Test
    @Tag("DERIVED-AGG-CONSISTENCY-001")
    void consistency_002_recomputeOnAdditionalApproval() {
        String token = obtainToken("cons2");
        UUID productId = UUID.randomUUID();

        addApprovedReview(token, productId, 5);
        addApprovedReview(token, productId, 3);
        addApprovedReview(token, productId, 4);
        addApprovedReview(token, productId, 2);

        Response resp = given().header("Authorization", "Bearer " + token)
            .when().get("/api/rating/summaries/" + productId)
            .then().statusCode(200)
                .body("reviewCount", Matchers.equalTo(4))
                .extract().response();

        BigDecimal storedAvg = new BigDecimal(resp.jsonPath().getString("average"));
        assertThat(storedAvg).isEqualByComparingTo("3.50");
    }

    // ─── ELIGIBILITY family ─────────────────────────────────────────────────

    /**
     * DERIVED-AGG-ELIGIBILITY-001: a PENDING review (stars=1) MUST NOT change the aggregate.
     */
    @Test
    @Tag("DERIVED-AGG-ELIGIBILITY-001")
    void eligibility_001_pendingReviewExcluded() {
        String token = obtainToken("elig1");
        UUID productId = UUID.randomUUID();

        // Establish baseline: one approved review (stars=5).
        addApprovedReview(token, productId, 5);

        // Add a pending review with low stars.
        addReview(token, productId, 1);

        // Summary must still reflect only the approved review.
        Response resp = given().header("Authorization", "Bearer " + token)
            .when().get("/api/rating/summaries/" + productId)
            .then().statusCode(200)
                .body("reviewCount", Matchers.equalTo(1))
                .extract().response();

        BigDecimal storedAvg = new BigDecimal(resp.jsonPath().getString("average"));
        assertThat(storedAvg).isEqualByComparingTo("5.00");
    }

    /**
     * DERIVED-AGG-ELIGIBILITY-001: approving a PENDING review recomputes the aggregate
     * to now include it.
     */
    @Test
    @Tag("DERIVED-AGG-ELIGIBILITY-001")
    void eligibility_002_approvingPendingTriggersRecompute() {
        String token = obtainToken("elig2");
        UUID productId = UUID.randomUUID();

        addApprovedReview(token, productId, 5);

        // Add PENDING review (stars=1).
        String pendingId = addReview(token, productId, 1);

        // Approve it — average must drop to (5+1)/2 = 3.00.
        given().header("Authorization", "Bearer " + token)
        .when().post("/api/rating/reviews/" + pendingId + "/approve")
        .then().statusCode(200)
            .body("status", Matchers.equalTo("APPROVED"));

        Response resp = given().header("Authorization", "Bearer " + token)
            .when().get("/api/rating/summaries/" + productId)
            .then().statusCode(200)
                .body("reviewCount", Matchers.equalTo(2))
                .extract().response();

        BigDecimal storedAvg = new BigDecimal(resp.jsonPath().getString("average"));
        assertThat(storedAvg).isEqualByComparingTo("3.00");
    }

    // ─── EMPTY family ───────────────────────────────────────────────────────

    /**
     * DERIVED-AGG-EMPTY-001: product with zero approved reviews → sentinel (average=0.00, reviewCount=0).
     */
    @Test
    @Tag("DERIVED-AGG-EMPTY-001")
    void empty_001_noApprovedReviewsYieldsSentinel() {
        String token = obtainToken("empty1");
        UUID productId = UUID.randomUUID();

        // Add a review but do NOT approve it.
        addReview(token, productId, 4);

        Response resp = given().header("Authorization", "Bearer " + token)
            .when().get("/api/rating/summaries/" + productId)
            .then().statusCode(200)
                .body("reviewCount", Matchers.equalTo(0))
                .extract().response();

        BigDecimal storedAvg = new BigDecimal(resp.jsonPath().getString("average"));
        assertThat(storedAvg).isEqualByComparingTo("0.00");
    }

    /**
     * DERIVED-AGG-EMPTY-001: rejecting the only approved review returns the aggregate to sentinel.
     * No divide-by-zero / NaN.
     */
    @Test
    @Tag("DERIVED-AGG-EMPTY-001")
    void empty_002_rejectingOnlyApprovedReviewReturnsSentinel() {
        String token = obtainToken("empty2");
        UUID productId = UUID.randomUUID();

        String reviewId = addApprovedReview(token, productId, 4);

        // Confirm it's included.
        given().header("Authorization", "Bearer " + token)
        .when().get("/api/rating/summaries/" + productId)
        .then().statusCode(200)
            .body("reviewCount", Matchers.equalTo(1));

        // Reject the only approved review.
        given().header("Authorization", "Bearer " + token)
        .when().post("/api/rating/reviews/" + reviewId + "/reject")
        .then().statusCode(200)
            .body("status", Matchers.equalTo("REJECTED"));

        // Aggregate must return to sentinel.
        Response resp = given().header("Authorization", "Bearer " + token)
            .when().get("/api/rating/summaries/" + productId)
            .then().statusCode(200)
                .body("reviewCount", Matchers.equalTo(0))
                .extract().response();

        BigDecimal storedAvg = new BigDecimal(resp.jsonPath().getString("average"));
        assertThat(storedAvg).isEqualByComparingTo("0.00");
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private String obtainToken(String prefix) {
        return RatingSummaryTestSupport.obtainToken(
            RatingSummaryTestSupport.freshEmail(prefix), "MEMBER");
    }

    /** Adds a review (PENDING) and returns the review id. */
    private String addReview(String token, UUID productId, int stars) {
        return given().header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body("{\"productId\":\"" + productId + "\",\"stars\":" + stars + "}")
        .when().post("/api/rating/reviews")
        .then().statusCode(201)
            .extract().path("id");
    }

    /** Adds a review and immediately approves it; returns the review id. */
    private String addApprovedReview(String token, UUID productId, int stars) {
        String id = addReview(token, productId, stars);
        given().header("Authorization", "Bearer " + token)
        .when().post("/api/rating/reviews/" + id + "/approve")
        .then().statusCode(200);
        return id;
    }
}
