package com.ax.template.authblueprint.apiversioning;

import org.springframework.http.HttpStatus;

/**
 * The closed set of version-negotiation failures, each carrying the HTTP status + stable {@code code}
 * the spec names (VERSION-NEGOTIATION-001):
 * <ul>
 *   <li>{@link Kind#MALFORMED} — a malformed version token → 400 with an RFC 9457 problem body whose
 *       {@code type} identifies the version-negotiation failure;</li>
 *   <li>{@link Kind#CONFLICT} — two strategies mixed in one request (e.g. a {@code /v1} path with a
 *       conflicting {@code X-API-Version: 2}) → 400, never silently resolved;</li>
 *   <li>{@link Kind#UNSUPPORTED} — a syntactically valid but unsupported version under url-path → 404
 *       Not Found (the spec's url-path branch);</li>
 *   <li>{@link Kind#SUNSET} — a request for a version whose sunset instant has passed → 410 Gone.</li>
 * </ul>
 * The message is ALWAYS value-free and never leaks internals (VERSION-DISCOVERY-001 leak posture).
 *
 * <p>Spec: specs/api-versioning-l0.yaml.
 */
public class ApiVersioningException extends RuntimeException {

    public enum Kind {
        MALFORMED(HttpStatus.BAD_REQUEST, "API_VERSION_MALFORMED"),
        CONFLICT(HttpStatus.BAD_REQUEST, "API_VERSION_STRATEGY_CONFLICT"),
        UNSUPPORTED(HttpStatus.NOT_FOUND, "API_VERSION_UNSUPPORTED"),
        SUNSET(HttpStatus.GONE, "API_VERSION_SUNSET");

        final HttpStatus status;
        final String code;

        Kind(HttpStatus status, String code) {
            this.status = status;
            this.code = code;
        }
    }

    private final transient Kind kind;

    public ApiVersioningException(Kind kind, String neutralMessage) {
        super(neutralMessage);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
