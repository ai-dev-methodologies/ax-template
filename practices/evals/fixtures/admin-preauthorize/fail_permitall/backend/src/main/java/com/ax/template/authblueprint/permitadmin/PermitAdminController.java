package com.ax.template.authblueprint.permitadmin;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * admin_preauthorize_guard.sh fail fixture — BYPASS (c): a method-level
 * @PreAuthorize("permitAll()") is NOT an effective authorization check, and it
 * OVERRIDES the class-level ROLE_ADMIN (Spring method-security precedence). No
 * covering SecurityConfig matcher exists in this fixture root, so the export
 * endpoint is reachable by any caller. The pre-hardening guard counted the
 * mere PRESENCE of an @PreAuthorize as coverage (false PASS); the hardened
 * guard inspects the SpEL, rejects permitAll(), and honors the override →
 * BLOCKED.
 */
@RestController
@RequestMapping("/api/admin/permit")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class PermitAdminController {

    @GetMapping
    public List<String> list() {
        return List.of("a", "b");
    }

    @PostMapping("/export")
    @PreAuthorize("permitAll()")
    public String export() {
        return "exported";
    }
}
