package com.ax.template.authblueprint.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Cross-cutting 404 signal raised by {@link CallerScope#ownerScopedOrThrow(java.util.Optional)}
 * — ships the REAL reusable code for the owner-scope / IDOR-safe-404 contract that
 * IDW2 (ecommerce-seller-admin dogfood, 2026-05-29) found was prose-only: every
 * persona re-derived "row exists but is not mine → 404, never 403" by hand, and the
 * pattern is exactly the kind of subtle authorization rule that drifts when copied.
 *
 * <h2>IDOR-safe 404 — why this is NOT a 403</h2>
 * When a caller asks for a resource they do not own, the server must answer
 * {@code 404 Not Found}, NOT {@code 403 Forbidden}. A 403 confirms the row
 * <em>exists</em> (the caller merely lacks access), which leaks the existence of
 * other users' resources and lets an attacker enumerate ids — the classic IDOR /
 * BOLA disclosure (OWASP API1:2023 Broken Object Level Authorization). Returning
 * the same 404 for "does not exist" and "exists but not yours" makes the two cases
 * indistinguishable to the caller. This exception therefore carries NO owner
 * identity and NO "you are not the owner" wording — only a neutral not-found
 * message — so a leak cannot creep back in through the response body.
 *
 * <h2>Mapping</h2>
 * Annotated {@link ResponseStatus @ResponseStatus(NOT_FOUND)} so it maps to HTTP 404
 * with zero wiring — a fork-receiver inherits the correct status the moment they
 * throw it. {@link GlobalProblemDetailAdvice} deliberately does NOT register a
 * catch-all {@code Exception} handler, so this signal is free to surface as a plain
 * 404 by default. A fork-receiver that wants an RFC 9457 {@code application/problem+json}
 * body can add a single {@code @ExceptionHandler(ResourceNotFoundException.class)} to
 * their own {@code @RestControllerAdvice} (mirroring how {@code ActivityController}
 * maps its domain {@code NotFound} to a {@link org.springframework.http.ProblemDetail});
 * the annotation here is the safe default, not a ceiling.
 *
 * <p>Framework-light: the only Spring touchpoint is the {@code @ResponseStatus}
 * mapping annotation; it carries no domain types.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException() {
        super("resource not found");
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
