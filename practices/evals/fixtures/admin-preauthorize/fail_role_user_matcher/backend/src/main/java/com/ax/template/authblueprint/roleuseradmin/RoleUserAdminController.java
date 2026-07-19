package com.ax.template.authblueprint.roleuseradmin;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * admin_preauthorize_guard.sh fail fixture — BYPASS (a) part 1.
 *
 * This admin controller carries NO class-level or method-level @PreAuthorize.
 * It relies entirely on a SecurityConfig requestMatcher — but that matcher
 * (see security/SecurityConfig.java) grants "/api/admin/**" to ROLE_USER, not
 * ROLE_ADMIN. A non-admin authority on an admin surface is NOT protection, so
 * the hardened guard MUST reject this (the pre-hardening guard credited ANY
 * hasAuthority/hasRole matcher regardless of the authority granted → BLOCKED).
 */
@RestController
@RequestMapping("/api/admin/roleuser")
public class RoleUserAdminController {

    @GetMapping("/secrets")
    public List<String> listSecrets() {
        return List.of("s1", "s2");
    }
}
