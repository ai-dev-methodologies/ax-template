package com.ax.template.authblueprint.dunning;

import org.springframework.http.HttpStatus;

/** Domain exception for dunning-collections. status + RFC 9457 type + machine-readable code. */
public class DunningException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private DunningException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static DunningException notFound() {
        return new DunningException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Dunning case not found");
    }

    /** DUNNING-LADDER/CONCURRENT-001 — a stage is reached exactly once (the uq backstop loser). */
    public static DunningException stageAlreadyReached() {
        return new DunningException(HttpStatus.CONFLICT,
            "urn:problem:dunning-stage-already-reached", "DUNNING_STAGE_ALREADY_REACHED",
            "That dunning stage was already reached — each stage transitions exactly once");
    }

    /** DUNNING-LADDER-001 — SUSPENDED is terminal; the one-way ladder has no further rung. */
    public static DunningException ladderTerminal() {
        return new DunningException(HttpStatus.CONFLICT,
            "urn:problem:dunning-ladder-terminal", "DUNNING_LADDER_TERMINAL",
            "The dunning ladder is at its terminal stage (SUSPENDED) — it cannot advance further");
    }

    /** DUNNING-CURE-001 — curing requires an open cure window. */
    public static DunningException noCureWindow() {
        return new DunningException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:dunning-no-cure-window", "DUNNING_NO_CURE_WINDOW",
            "No open cure window — record a payment to open one before curing");
    }
}
