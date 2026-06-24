package com.ax.template.authblueprint.quorumresolution;

import org.springframework.http.HttpStatus;

/** Domain exception for quorum-resolution. status + RFC 9457 type + machine-readable code. */
public class QuorumException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private QuorumException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static QuorumException notFound() {
        return new QuorumException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Motion not found");
    }

    /** QR-POLICY-INVALID — 422: the policy snapshot is self-inconsistent. */
    public static QuorumException policyInvalid(String detail) {
        return new QuorumException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:quorum-policy-invalid", "QR_POLICY_INVALID",
            "Motion policy is invalid: " + detail);
    }

    /** QR-MOTION-CLOSED — 409: the motion is no longer OPEN. */
    public static QuorumException motionClosed() {
        return new QuorumException(HttpStatus.CONFLICT,
            "urn:problem:quorum-motion-closed", "QR_MOTION_CLOSED",
            "The motion is no longer accepting ballots — status is not OPEN");
    }

    /** QR-NOT-ELIGIBLE — 403: the caller is not an eligible voter for this motion. */
    public static QuorumException notEligible() {
        return new QuorumException(HttpStatus.FORBIDDEN,
            "urn:problem:quorum-not-eligible", "QR_NOT_ELIGIBLE",
            "You are not an eligible voter for this motion");
    }

    /** QR-DOUBLE-VOTE — 409: the caller already cast a ballot for this motion. */
    public static QuorumException doubleVote() {
        return new QuorumException(HttpStatus.CONFLICT,
            "urn:problem:quorum-double-vote", "QR_DOUBLE_VOTE",
            "You have already cast a ballot for this motion — one voter, one ballot");
    }

    /** QR-NOT-CONVENER — 403: only the convener may resolve the motion. */
    public static QuorumException notConvener() {
        // IDOR-safe: byte-identical to notFound() so a non-convener cannot distinguish
        // "motion exists but you're not the convener" from "no such motion".
        return new QuorumException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND",
            "Motion not found");
    }

    /** State machine illegal edge. */
    public static QuorumException illegalTransition(MotionStatus from, MotionStatus to) {
        return new QuorumException(HttpStatus.CONFLICT,
            "urn:problem:quorum-illegal-transition", "QR_ILLEGAL_TRANSITION",
            "Illegal motion state transition: " + from + " -> " + to);
    }
}
