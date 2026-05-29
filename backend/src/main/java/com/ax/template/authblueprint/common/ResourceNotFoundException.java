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
 * <h2>Mapping — 404 RFC 9457 problem+json, built-in</h2>
 * {@link GlobalProblemDetailAdvice} (the {@code LOWEST_PRECEDENCE} fallback) registers
 * an {@code @ExceptionHandler(ResourceNotFoundException.class)} that maps this signal
 * to {@code 404 application/problem+json} (code {@code NOT_FOUND}, with the neutral
 * not-found message) by default — a fork-receiver inherits a uniform RFC 9457 body the
 * moment they throw it, no extra wiring required.
 *
 * <p>This explicit handler is also what makes the IDOR-safe 404 actually <em>be</em> a
 * 404: relying on the {@link ResponseStatus @ResponseStatus(NOT_FOUND)} annotation
 * alone is a trap under the reference {@code SecurityConfig}. The annotation drives a
 * {@code sendError(404)} which re-dispatches the request to {@code /error}; that
 * re-entry passes back through the filter chain and is caught by
 * {@code anyRequest().denyAll()}, turning the intended 404 into a misleading
 * {@code 403}. A global {@code @ExceptionHandler} returns the response DIRECTLY (no
 * {@code sendError} re-dispatch), so it sidesteps the trap. The {@code @ResponseStatus}
 * annotation is kept as a belt-and-braces default for any path that bypasses the
 * advice.
 *
 * <p>Because the advice is {@code LOWEST_PRECEDENCE}, a domain that wants a richer body
 * can still override it with its own controller-local or {@code basePackages}-scoped
 * {@code @ExceptionHandler} (mirroring how {@code ActivityController} maps its domain
 * {@code NotFound} to a {@link org.springframework.http.ProblemDetail}); the built-in
 * 404 is the safe default, not a ceiling.
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
