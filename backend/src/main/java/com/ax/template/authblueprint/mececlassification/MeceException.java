package com.ax.template.authblueprint.mececlassification;

import org.springframework.http.HttpStatus;

/** Domain exception for mece-classification-l0. status + RFC 9457 type + machine-readable code. */
public class MeceException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private MeceException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static MeceException missingResidual() {
        return new MeceException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:mece-scheme-missing-residual", "MECE_SCHEME_MISSING_RESIDUAL",
            "A scheme must declare a non-blank residual category at config time");
    }

    public static MeceException duplicateScheme() {
        return new MeceException(HttpStatus.CONFLICT,
            "urn:problem:mece-duplicate-scheme", "MECE_DUPLICATE_SCHEME", "A scheme with that key already exists");
    }

    public static MeceException schemeNotFound() {
        return new MeceException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "MECE_SCHEME_NOT_FOUND", "No scheme found for that key");
    }

    public static MeceException alreadyClassified() {
        return new MeceException(HttpStatus.CONFLICT,
            "urn:problem:mece-already-classified", "MECE_ALREADY_CLASSIFIED",
            "That item already has a category under this scheme — reclassify instead of assigning again");
    }

    public static MeceException notClassified() {
        return new MeceException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "MECE_NOT_CLASSIFIED", "That item has no classification under this scheme yet");
    }

    public static MeceException duplicateRule() {
        return new MeceException(HttpStatus.CONFLICT,
            "urn:problem:mece-duplicate-rule", "MECE_DUPLICATE_RULE",
            "A rule for that match value already exists on this scheme");
    }
}
