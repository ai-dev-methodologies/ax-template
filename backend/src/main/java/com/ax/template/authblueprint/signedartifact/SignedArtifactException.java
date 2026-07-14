package com.ax.template.authblueprint.signedartifact;

import org.springframework.http.HttpStatus;

/** Domain exception for signed-artifact-l0. status + RFC 9457 type + machine-readable code. */
public class SignedArtifactException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private SignedArtifactException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static SignedArtifactException notFound() {
        return new SignedArtifactException(HttpStatus.NOT_FOUND,
            "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Signed artifact not found");
    }

    public static SignedArtifactException malformed() {
        return new SignedArtifactException(HttpStatus.BAD_REQUEST,
            "urn:problem:signed-artifact-malformed", "SIGNED_ARTIFACT_MALFORMED",
            "The presented value is not a well-formed JWS compact serialization");
    }

    /** SIGNED-ALG-ALLOWLIST-001 — alg:none, any HS* alg, or any alg outside the server's
     *  configured allow-list for the resolved kid. The alg + key are ALWAYS resolved from
     *  server-side configuration keyed by kid, never from the token's own (attacker-controlled)
     *  header value. */
    public static SignedArtifactException unsupportedAlgorithm() {
        return new SignedArtifactException(HttpStatus.UNAUTHORIZED,
            "urn:problem:signed-artifact-unsupported-algorithm", "SIGNED_ARTIFACT_UNSUPPORTED_ALGORITHM",
            "The token's alg is not the asymmetric algorithm configured for this kid — rejected "
                + "(covers alg:none and HS*-over-public-key algorithm-confusion forgeries)");
    }

    public static SignedArtifactException unknownKid() {
        return new SignedArtifactException(HttpStatus.UNAUTHORIZED,
            "urn:problem:signed-artifact-unknown-kid", "SIGNED_ARTIFACT_UNKNOWN_KID",
            "The token's kid does not resolve to a published verifying key");
    }

    public static SignedArtifactException signatureInvalid() {
        return new SignedArtifactException(HttpStatus.UNAUTHORIZED,
            "urn:problem:signed-artifact-signature-invalid", "SIGNED_ARTIFACT_SIGNATURE_INVALID",
            "Signature verification against the published public key failed");
    }

    /** The presented content's hash does not match the hash the signature covers — tampered. */
    public static SignedArtifactException contentMismatch() {
        return new SignedArtifactException(HttpStatus.UNPROCESSABLE_ENTITY,
            "urn:problem:signed-artifact-content-mismatch", "SIGNED_ARTIFACT_CONTENT_MISMATCH",
            "The presented content's hash does not match the hash the signature covers");
    }
}
