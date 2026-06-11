package com.ax.template.authblueprint.recordlinkage;

import org.springframework.http.HttpStatus;

/** Domain exception for record-linkage. status + RFC 9457 type + machine-readable code. */
public class LinkageException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private LinkageException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static LinkageException notFound() {
        return new LinkageException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Record or proposal not found");
    }

    /** LINK-BAND-001 — a record cannot be matched against itself. */
    public static LinkageException selfPair() {
        return new LinkageException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:linkage-self-pair", "LINKAGE_SELF_PAIR",
            "A record cannot be proposed as a match with itself");
    }

    /** LINK-BAND/CONCURRENT-001 — a MERGED record participates in no further linkage. */
    public static LinkageException participantMerged() {
        return new LinkageException(HttpStatus.CONFLICT,
            "urn:problem:linkage-participant-merged", "LINKAGE_PARTICIPANT_MERGED",
            "A participant record is already merged — re-propose against its survivor");
    }

    /** LINK-REVIEW-001 — a proposal decides exactly once. */
    public static LinkageException alreadyDecided() {
        return new LinkageException(HttpStatus.CONFLICT,
            "urn:problem:linkage-already-decided", "LINKAGE_ALREADY_DECIDED",
            "The proposal is already decided — a proposal decides once");
    }

    /** LINK-REVIEW-001 — NO_MATCH is not confirmable; re-propose after the records change. */
    public static LinkageException notConfirmable() {
        return new LinkageException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:linkage-not-confirmable", "LINKAGE_NOT_CONFIRMABLE",
            "A NO_MATCH proposal cannot be confirmed — re-propose after the records change");
    }

    /** LINK-RESOLVE-001 — a pointer loop is corruption, surfaced loudly (never spins). */
    public static LinkageException resolutionCycle() {
        return new LinkageException(HttpStatus.INTERNAL_SERVER_ERROR,
            "urn:problem:linkage-resolution-cycle", "LINKAGE_RESOLUTION_CYCLE",
            "Merged-into pointer chain contains a cycle — data corruption");
    }
}
