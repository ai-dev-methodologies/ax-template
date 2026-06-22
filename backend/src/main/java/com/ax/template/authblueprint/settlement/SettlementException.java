package com.ax.template.authblueprint.settlement;

import org.springframework.http.HttpStatus;

/** Domain exception for settlement-finality. status + RFC 9457 type + machine-readable code. */
public class SettlementException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private SettlementException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static SettlementException notFound() {
        return new SettlementException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Settlement instruction not found");
    }

    /** SETTLE-FINAL-001 — after finality, novation/cancel/amend are all refused (irrevocable). */
    public static SettlementException alreadyFinal() {
        return new SettlementException(HttpStatus.CONFLICT,
            "urn:problem:settlement-already-final", "SETTLEMENT_ALREADY_FINAL",
            "Settlement is final and irrevocable — novation, cancel and amend are no longer permitted");
    }

    /** SETTLE-LADDER-001 — the fail ladder takes each edge at most once; an illegal edge is 409. */
    public static SettlementException illegalLadderEdge(SettlementStatus from, SettlementStatus to) {
        return new SettlementException(HttpStatus.CONFLICT,
            "urn:problem:settlement-illegal-ladder-edge", "SETTLEMENT_ILLEGAL_LADDER_EDGE",
            "Illegal fail-ladder transition " + from + " → " + to
                + " — the ladder is PENDING→FAILED→RETRY→BUYIN, each edge once");
    }

    /** SETTLE-DVP-001 — settle is only meaningful on a not-yet-final, not-terminal instruction. */
    public static SettlementException notSettleable(SettlementStatus status) {
        return new SettlementException(HttpStatus.CONFLICT,
            "urn:problem:settlement-not-settleable", "SETTLEMENT_NOT_SETTLEABLE",
            "Settlement cannot be committed from status " + status
                + " — only PENDING/FAILED/RETRY instructions reach finality");
    }

    /** SETTLE-NOVATE-001 — novating a leg to the party already owing it is rejected. */
    public static SettlementException novationNoChange() {
        return new SettlementException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:settlement-novation-no-change", "SETTLEMENT_NOVATION_NO_CHANGE",
            "Novation must substitute a different counterparty — the assuming party already owes this leg");
    }
}
