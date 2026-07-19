package com.ax.template.authblueprint.scadmin;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * admin_preauthorize_guard.sh PASS fixture — the genuine SecurityConfig route
 * (c): this controller carries no @PreAuthorize, but every method resolves
 * under "/api/admin/sc", which the SecurityConfig "/api/admin/**"
 * hasAuthority("ROLE_ADMIN") matcher covers with boundary-aware Ant semantics.
 * This is exactly the shape the real repo's BillingAdminController /
 * PaymentAdminController / WebhookAdminController use → PASS.
 */
@RestController
@RequestMapping("/api/admin/sc")
public class ScAdminController {

    @GetMapping("/reports")
    public List<String> reports() {
        return List.of("r1");
    }

    @PostMapping("/reconcile")
    public String reconcile(Authentication authentication) {
        return "reconciled by " + authentication.getName();
    }
}
