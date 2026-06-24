package com.ax.template.authblueprint.quorumresolution;

import io.restassured.RestAssured;

import java.util.UUID;

import static io.restassured.RestAssured.given;

/** Shared helpers for quorum-resolution integration tests (mirrors DecisionTestSupport). */
public final class QuorumTestSupport {

    private QuorumTestSupport() {}

    public static String freshEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    public static String obtainToken(String email, String role) {
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

    /** Returns the userId (= JWT subject = what auth.getName() returns) for the given token. */
    public static String resolveUserId(String token) {
        return given()
            .header("Authorization", "Bearer " + token)
        .when().get("/api/auth/me")
        .then().extract().path("userId");
    }

    public static void useRandomPort(int port) {
        RestAssured.port = port;
    }

    /**
     * Build the JSON body for openMotion with a simple majority policy.
     * thresholdNumerator/thresholdDenominator = 1/2 → more than half must vote YES.
     * quorumNumerator/quorumDenominator = 1/2 → at least half of eligible must cast.
     */
    public static String majorityPolicyBody(java.util.List<VoterSpec> voters) {
        return buildOpenBody("MAJORITY", 1, 2, 1, 2, "EXCLUDE_FROM_BASE", "TIE_FAILS", null, voters);
    }

    public static String buildOpenBody(String ruleType, long threshNum, long threshDen,
                                        long quorumNum, long quorumDen,
                                        String abstentionMode, String tieBreakMode,
                                        String tieBreakVoterId,
                                        java.util.List<VoterSpec> voters) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"policy\":{");
        sb.append("\"ruleType\":\"").append(ruleType).append("\",");
        sb.append("\"thresholdNumerator\":").append(threshNum).append(",");
        sb.append("\"thresholdDenominator\":").append(threshDen).append(",");
        sb.append("\"quorumNumerator\":").append(quorumNum).append(",");
        sb.append("\"quorumDenominator\":").append(quorumDen).append(",");
        sb.append("\"abstentionMode\":\"").append(abstentionMode).append("\",");
        sb.append("\"tieBreakMode\":\"").append(tieBreakMode).append("\"");
        if (tieBreakVoterId != null) {
            sb.append(",\"tieBreakVoterId\":\"").append(tieBreakVoterId).append("\"");
        }
        sb.append("},\"roster\":[");
        for (int i = 0; i < voters.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"voterId\":\"").append(voters.get(i).voterId())
              .append("\",\"weight\":").append(voters.get(i).weight()).append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    public record VoterSpec(String voterId, long weight) {}
}
