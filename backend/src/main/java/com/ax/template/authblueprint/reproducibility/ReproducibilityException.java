package com.ax.template.authblueprint.reproducibility;

import org.springframework.http.HttpStatus;

/** Domain exception for reproducible-procedure. status + RFC 9457 type + machine-readable code. */
public class ReproducibilityException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private ReproducibilityException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static ReproducibilityException notFound() {
        return new ReproducibilityException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Procedure not found");
    }

    /** PROC-REPLAY-001 — a replay re-derived a result different from the recorded one; fail closed. */
    public static ReproducibilityException replayDiverged() {
        return new ReproducibilityException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:proc-replay-diverged", "PROC_REPLAY_DIVERGED",
            "Replay diverged from the recorded result — the procedure is not reproducible");
    }

    /** PROC-REPLAY-001 — only a DRAW procedure can be replayed. */
    public static ReproducibilityException notReplayable() {
        return new ReproducibilityException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:proc-not-replayable", "PROC_NOT_REPLAYABLE",
            "Only a draw procedure can be replayed");
    }
}
