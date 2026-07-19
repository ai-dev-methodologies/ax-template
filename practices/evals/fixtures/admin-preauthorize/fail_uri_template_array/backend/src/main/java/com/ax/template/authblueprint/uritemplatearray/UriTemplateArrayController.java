package com.ax.template.authblueprint.uritemplatearray;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * admin_preauthorize_guard.sh fail fixture — round-7 codex Finding 2
 * (URI-template {@code {id}} brace breaks array brace-matching).
 *
 * {@code ok()}'s ordinary bare {@code @PostMapping("/api/admin/ok")} (correctly
 * gated) makes this class an admin surface via the WIDENED method-path
 * detector.
 *
 * {@code rotate()} is the deliberate miss: its {@code @PostMapping} declares an
 * ARRAY of paths whose admin element contains a URI template variable
 * ({@code {id}}):
 *   {@code value = {"/public", "/api/admin/{id}/rotate", "/other"}}
 * The PRE-FIX array extractor used {@code \{([^}]*)\}} — a non-quote-aware
 * brace match that STOPS at the first {@code }} it sees, which is the
 * {@code }} of {@code {id}} INSIDE a string literal, long before the array's
 * real closing brace. It captured only {@code "/public"} and dropped the admin
 * element; the required admin mutation (ZERO @PreAuthorize) was uncounted and
 * the pre-fix guard exited 0. The FIXED guard scans the array initializer with
 * QUOTE/ESCAPE-AWARE brace balancing (only a {@code }} OUTSIDE a string literal
 * closes the array), extracts every element, sees {@code /api/admin/{id}/rotate},
 * requires authz, finds none → BLOCKS (exit 1). Admin element as first, middle,
 * or last is caught identically; this fixture places it in the MIDDLE.
 */
@RestController
public class UriTemplateArrayController {

    @PostMapping("/api/admin/ok")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String ok() {
        return "ok";
    }

    @PostMapping(value = {"/public", "/api/admin/{id}/rotate", "/other"})
    public String rotate() {
        return "rotated";
    }
}
