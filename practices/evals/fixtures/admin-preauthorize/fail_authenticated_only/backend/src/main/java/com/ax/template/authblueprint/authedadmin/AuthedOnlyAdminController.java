package com.ax.template.authblueprint.authedadmin;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * admin_preauthorize_guard.sh fail fixture — the mutating endpoint carries a
 * method-level {@code @PreAuthorize("isAuthenticated()")}, which is NOT an admin
 * gate: any authenticated non-admin caller passes it. authenticated() /
 * isAuthenticated() are REJECTED as ineffective (they do not require ROLE_ADMIN),
 * so the POST is the BFLA shape → BLOCKED (exit 1). Presence of an @PreAuthorize
 * is not coverage; the SpEL must require admin.
 */
@RestController
@RequestMapping("/api/admin/authed")
public class AuthedOnlyAdminController {

    @GetMapping
    public List<String> list() {
        return List.of("a", "b");
    }

    @PostMapping("/export")
    @PreAuthorize("isAuthenticated()")
    public String export() {
        return "exported";
    }
}
