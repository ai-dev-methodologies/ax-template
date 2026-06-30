package com.ax.template.authblueprint.tokenizedsecurities;

import org.springframework.http.HttpStatus;

public class TokenizedSecuritiesException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private TokenizedSecuritiesException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static TokenizedSecuritiesException notFound() {
        return new TokenizedSecuritiesException(HttpStatus.NOT_FOUND,
                "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Security token not found");
    }

    public static TokenizedSecuritiesException duplicateTokenCode() {
        return new TokenizedSecuritiesException(HttpStatus.CONFLICT,
                "urn:problem:ts-duplicate-token", "TS_DUPLICATE_TOKEN",
                "A security token with this code already exists");
    }

    public static TokenizedSecuritiesException duplicateUnderlyingAsset() {
        return new TokenizedSecuritiesException(HttpStatus.CONFLICT,
                "urn:problem:ts-duplicate-underlying-asset", "TS_DUPLICATE_UNDERLYING_ASSET",
                "Underlying asset is already securitized by another security token");
    }

    public static TokenizedSecuritiesException ineligibleRecipient() {
        return new TokenizedSecuritiesException(HttpStatus.UNPROCESSABLE_ENTITY,
                "urn:problem:ts-ineligible-recipient", "TS_INELIGIBLE_RECIPIENT",
                "Recipient is not an eligible investor for this security token");
    }

    public static TokenizedSecuritiesException lockupActive() {
        return new TokenizedSecuritiesException(HttpStatus.UNPROCESSABLE_ENTITY,
                "urn:problem:ts-lockup-active", "TS_LOCKUP_ACTIVE",
                "The security token is within its lock-up period; transfers are not permitted yet");
    }

    public static TokenizedSecuritiesException holdingLimitExceeded() {
        return new TokenizedSecuritiesException(HttpStatus.UNPROCESSABLE_ENTITY,
                "urn:problem:ts-holding-limit-exceeded", "TS_HOLDING_LIMIT_EXCEEDED",
                "Transfer would push the recipient over the per-investor holding limit");
    }

    public static TokenizedSecuritiesException insufficientUnits() {
        return new TokenizedSecuritiesException(HttpStatus.UNPROCESSABLE_ENTITY,
                "urn:problem:ts-insufficient-units", "TS_INSUFFICIENT_UNITS",
                "Sender does not hold enough units for this transfer");
    }

    public static TokenizedSecuritiesException invalidUnits() {
        return new TokenizedSecuritiesException(HttpStatus.UNPROCESSABLE_ENTITY,
                "urn:problem:ts-invalid-units", "TS_INVALID_UNITS",
                "units must be a positive whole number; totalUnits/limit must be > 0");
    }

    public static TokenizedSecuritiesException holderAlreadyOwned() {
        return new TokenizedSecuritiesException(HttpStatus.CONFLICT,
                "urn:problem:ts-holder-already-owned", "TS_HOLDER_ALREADY_OWNED",
                "Holder is already owned by a different principal");
    }

    public static TokenizedSecuritiesException notHolderController() {
        return new TokenizedSecuritiesException(HttpStatus.FORBIDDEN,
                "urn:problem:ts-not-holder-controller", "TS_NOT_HOLDER_CONTROLLER",
                "Caller does not control the debited holder");
    }

    public static TokenizedSecuritiesException notIssued() {
        return new TokenizedSecuritiesException(HttpStatus.CONFLICT,
                "urn:problem:ts-not-issued", "TS_NOT_ISSUED",
                "Security token is still in DRAFT; it must be issued before any transfer");
    }

    public static TokenizedSecuritiesException alreadyIssued() {
        return new TokenizedSecuritiesException(HttpStatus.CONFLICT,
                "urn:problem:ts-already-issued", "TS_ALREADY_ISSUED",
                "Security token is already ISSUED; issuance is a one-way operation");
    }
}
