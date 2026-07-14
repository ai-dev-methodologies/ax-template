package com.ax.template.authblueprint.appealindependence;

import org.springframework.http.HttpStatus;

/** Domain exception for appeal-decider-independence-l0. status + RFC 9457 type + machine-readable code. */
public class AppealException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private AppealException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static AppealException notFound() {
        return new AppealException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "DECISION_NOT_FOUND", "Decision or appeal not found");
    }

    /** APPEAL-CHAIN-001 — at most one appeal per decision level. */
    public static AppealException alreadyAppealed() {
        return new AppealException(HttpStatus.CONFLICT,
            "urn:problem:appeal-already-filed", "APPEAL_ALREADY_FILED",
            "This decision already has an appeal filed against it");
    }

    /** APPEAL-DISTINCT-001 / APPEAL-CHAIN-001 — decider reused somewhere in the chain. */
    public static AppealException deciderNotIndependent(String decider) {
        return new AppealException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:appeal-decider-not-independent", "APPEAL_DECIDER_NOT_INDEPENDENT",
            "'" + decider + "' already decided at an earlier level of this appeal chain "
            + "and cannot decide this appeal (nemo iudex in causa sua)");
    }
}
