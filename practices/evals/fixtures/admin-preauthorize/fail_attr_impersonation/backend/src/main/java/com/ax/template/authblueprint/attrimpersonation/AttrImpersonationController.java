package com.ax.template.authblueprint.attrimpersonation;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * admin_preauthorize_guard.sh fail fixture — round-7 codex Finding 1
 * (non-path attribute contents impersonate path syntax).
 *
 * {@code ok()}'s ordinary bare {@code @PostMapping("/api/admin/ok")} (correctly
 * gated) makes this class an admin surface via the WIDENED method-path
 * detector.
 *
 * {@code create()} is the deliberate miss: a NON-path attribute
 * ({@code name = "path={}"}) contains text that imitates the array-path
 * syntax the extractor scans for. The PRE-FIX extractor searched the WHOLE
 * argument string with {@code PATH_ATTR_ARRAY_RE} ({@code path=\{...\}}) and
 * matched {@code path={}} INSIDE the {@code name} string literal, returning an
 * empty array and never reading the REAL scalar {@code path="/api/admin/x"}.
 * The endpoint's path was silently treated as empty, so this required admin
 * mutation (with ZERO @PreAuthorize) was dropped from the per-endpoint check;
 * the pre-fix guard exited 0. The FIXED guard TOKENIZES the annotation's
 * top-level attributes (splitting on commas outside string literals / braces),
 * identifies the {@code path}/{@code value} attribute by its TOP-LEVEL name,
 * and reads ONLY its value — never quoted content inside {@code name} /
 * {@code produces} / {@code consumes} / {@code headers} / {@code params}. It
 * reads {@code /api/admin/x}, requires authz, finds none → BLOCKS (exit 1).
 */
@RestController
public class AttrImpersonationController {

    @PostMapping("/api/admin/ok")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String ok() {
        return "ok";
    }

    @PostMapping(name = "path={}", path = "/api/admin/x")
    public String create() {
        return "created";
    }
}
