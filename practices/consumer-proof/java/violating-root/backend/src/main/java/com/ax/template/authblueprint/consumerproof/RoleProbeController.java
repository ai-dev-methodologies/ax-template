package com.ax.template.authblueprint.consumerproof;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// VIOLATING — role_literal_guard
// The @PreAuthorize names `ROLE_ADMINS` — a typo. No UserRole / ApiKeyScope can
// ever grant it, so the endpoint is permanently un-authorizable (every caller 403s).
@RestController
public class RoleProbeController {

    @PreAuthorize("hasAuthority('ROLE_ADMINS')")
    @GetMapping("/api/probe")
    public String probe() {
        return "ok";
    }
}
