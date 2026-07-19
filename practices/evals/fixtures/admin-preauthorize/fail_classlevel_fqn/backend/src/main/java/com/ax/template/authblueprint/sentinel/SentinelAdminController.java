package com.ax.template.authblueprint.sentinel;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * admin_preauthorize_guard.sh shared SENTINEL for the composition / class-FQN
 * fixtures (round-7 codex).
 *
 * A plain *AdminController by NAME (always an admin surface, correctly gated).
 * Its only job is to keep admin_controllers >= 1 in this fixture root so the
 * guard reaches the per-endpoint requirement check (avoiding the ZERO_SCAN
 * exit 2) and isolates the TARGET controller's deliberate miss.
 */
@RestController
public class SentinelAdminController {

    @PostMapping("/api/admin/sentinel")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String sentinel() {
        return "ok";
    }
}
