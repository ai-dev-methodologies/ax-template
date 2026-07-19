package com.ax.template.authblueprint.consumerproof;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// CLEAN — the @PreAuthorize names ROLE_ADMIN, which UserRole.ADMIN grants.
@RestController
public class RoleProbeController {

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/api/probe")
    public String probe() {
        return "ok";
    }
}
