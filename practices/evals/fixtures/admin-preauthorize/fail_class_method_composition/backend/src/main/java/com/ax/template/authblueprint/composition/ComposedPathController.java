package com.ax.template.authblueprint.composition;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * admin_preauthorize_guard.sh fail fixture — round-7 codex Finding 4
 * (class-level + method-level path COMPOSITION).
 *
 * Spring concatenates the class-level {@code @RequestMapping} path with the
 * method-level mapping path to form the EFFECTIVE route. Here:
 *   class {@code @RequestMapping("/api")} × method {@code @PostMapping("/admin/x")}
 *   → effective route {@code /api/admin/x}
 * which is under {@code /api/admin/**} and therefore a required admin mutation.
 *
 * The PRE-FIX guard matched {@code ADMIN_PATH_RE} against the RAW method path
 * ({@code /admin/x}) and the RAW class path ({@code /api}) SEPARATELY — neither
 * raw component matches {@code ^/api/admin}, so the composed admin route was
 * invisible to BOTH the admin-surface detector and the per-endpoint
 * requirement check. {@code create()} carries NO @PreAuthorize, yet the
 * pre-fix guard exited 0 (this target was never even detected as an admin
 * surface; the SentinelAdminController in the same root keeps the scan
 * non-empty). The FIXED guard composes the class×method cross-product,
 * normalises it to {@code /api/admin/x}, detects the admin surface, requires
 * authz, finds none → BLOCKS (exit 1).
 */
@RestController
@RequestMapping("/api")
public class ComposedPathController {

    @PostMapping("/admin/x")
    public String create() {
        return "created";
    }
}
