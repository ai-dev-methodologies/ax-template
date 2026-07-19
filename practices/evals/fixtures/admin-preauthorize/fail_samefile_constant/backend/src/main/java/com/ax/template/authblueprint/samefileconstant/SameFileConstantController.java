package com.ax.template.authblueprint.samefileconstant;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * admin_preauthorize_guard.sh fail fixture — round-7 codex Finding 3-simple
 * (same-file String constant + literal concatenation).
 *
 * {@code ok()}'s ordinary bare {@code @PostMapping("/api/admin/ok")} (correctly
 * gated) makes this class an admin surface via the WIDENED method-path
 * detector.
 *
 * {@code create()} is the deliberate miss: its mapping argument is a
 * compile-time-constant expression built from a SAME-FILE
 * {@code static final String} plus a literal —
 *   {@code static final String ADMIN_BASE = "/api/admin";}
 *   {@code @PostMapping(ADMIN_BASE + "/x")}   // → "/api/admin/x"
 * The PRE-FIX extractor only recognised a leading {@code "} or {@code {}; a
 * bare identifier / concatenation matched none of its shapes, so it returned
 * an empty path. The required admin mutation (ZERO @PreAuthorize) was uncounted
 * and the pre-fix guard exited 0. The FIXED guard builds a map of same-file
 * {@code static final String IDENT = "literal"} declarations and constant-folds
 * a bare IDENT or a {@code +}-concatenation of literals and/or such same-file
 * IDENTs to the resulting literal, reads {@code /api/admin/x}, requires authz,
 * finds none → BLOCKS (exit 1). (Imported/opaque constants and
 * {@code ${...}}/{@code #{...}} placeholders remain controller-locally
 * undecidable and OUT OF SCOPE — deferred to the domain 403 tests + runtime.)
 */
@RestController
public class SameFileConstantController {

    static final String ADMIN_BASE = "/api/admin";

    @PostMapping("/api/admin/ok")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String ok() {
        return "ok";
    }

    @PostMapping(ADMIN_BASE + "/x")
    public String create() {
        return "created";
    }
}
