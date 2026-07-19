package com.ax.template.authblueprint.leadingslash;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * admin_preauthorize_guard.sh fail fixture — round-7 codex Finding 5
 * (Spring's optional leading slash is not normalised).
 *
 * {@code ok()}'s ordinary bare {@code @PostMapping("/api/admin/ok")} (correctly
 * gated) makes this class an admin surface via the WIDENED method-path
 * detector.
 *
 * {@code create()} is the deliberate miss: its path OMITS the leading slash —
 *   {@code @PostMapping("api/admin/x")}
 * Spring's {@code PathPatternParser.initFullPathPattern} prepends {@code /}
 * when absent, so this registers at runtime as {@code /api/admin/x}. The
 * PRE-FIX guard matched {@code ADMIN_PATH_RE} ({@code ^/api/(?:v1/)?admin})
 * against the RAW string {@code api/admin/x}, which lacks the leading slash and
 * so did not match; the required admin mutation (ZERO @PreAuthorize) was
 * uncounted and the pre-fix guard exited 0. The FIXED guard prepends {@code /}
 * to any non-empty extracted/composed path lacking one before matching, reads
 * {@code /api/admin/x}, requires authz, finds none → BLOCKS (exit 1).
 */
@RestController
public class LeadingSlashController {

    @PostMapping("/api/admin/ok")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String ok() {
        return "ok";
    }

    @PostMapping("api/admin/x")
    public String create() {
        return "created";
    }
}
