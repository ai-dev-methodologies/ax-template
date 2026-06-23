package com.ax.template.authblueprint.timedoffer;

import org.springframework.http.HttpStatus;

/** Domain exception for timed-offer-exclusive-assignment. status + RFC 9457 type + machine code. */
public class TimedOfferException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private TimedOfferException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static TimedOfferException notFound() {
        return new TimedOfferException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Timed offer not found");
    }

    /** TIMEDOFFER-LIFECYCLE-001 — acting on a non-OPEN (terminal) offer. */
    public static TimedOfferException notOpen(String currentStatus) {
        return new TimedOfferException(HttpStatus.CONFLICT,
            "urn:problem:timed-offer-not-open", "TIMEDOFFER_NOT_OPEN",
            "This offer is no longer open (status " + currentStatus + ") — it cannot be acted on");
    }

    /** TIMEDOFFER-LIFECYCLE-001 — the deadline passed before the candidate accepted. */
    public static TimedOfferException offerExpired() {
        return new TimedOfferException(HttpStatus.CONFLICT,
            "urn:problem:timed-offer-expired", "TIMEDOFFER_EXPIRED",
            "This offer's deadline has passed — it can no longer be accepted");
    }

    /** TIMEDOFFER-EXCLUSIVE/CONCURRENT-001 — the subject is already assigned (the loser's 409). */
    public static TimedOfferException subjectAlreadyAssigned() {
        return new TimedOfferException(HttpStatus.CONFLICT,
            "urn:problem:timed-offer-subject-already-assigned", "TIMEDOFFER_SUBJECT_ALREADY_ASSIGNED",
            "That subject is already assigned — at most one offer per subject can be accepted");
    }

    /** TIMEDOFFER-LADDER-001 — re-offering an offer that is still OPEN (not declined/expired). */
    public static TimedOfferException notReofferable(String currentStatus) {
        return new TimedOfferException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:timed-offer-not-reofferable", "TIMEDOFFER_NOT_REOFFERABLE",
            "Only a declined or expired offer can be re-offered (status " + currentStatus + ")");
    }
}
