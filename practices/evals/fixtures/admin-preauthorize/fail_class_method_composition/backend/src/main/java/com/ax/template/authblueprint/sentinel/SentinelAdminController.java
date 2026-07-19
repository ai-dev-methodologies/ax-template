package com.ax.template.authblueprint.sentinel;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * admin_preauthorize_guard.sh shared SENTINEL for the composition / class-FQN
 * fixtures (round-7 codex).
 *
 * This class is a plain *AdminController by NAME, so the guard always detects
 * it as an admin surface (admin_by_class), and its single mutating endpoint is
 * correctly gated by an admin @PreAuthorize. Its ONLY job is to guarantee
 * admin_controllers >= 1 in the fixture root so the guard reaches the
 * per-endpoint requirement check and exits 0/1 (NOT the ZERO_SCAN exit 2).
 * It isolates the TARGET controller's behaviour cleanly — the sentinel is
 * always fine; the target is the deliberate miss. This mirrors the two-file
 * (sentinel + target) harness codex itself used to reproduce these findings.
 */
@RestController
public class SentinelAdminController {

    @PostMapping("/api/admin/sentinel")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String sentinel() {
        return "ok";
    }
}
