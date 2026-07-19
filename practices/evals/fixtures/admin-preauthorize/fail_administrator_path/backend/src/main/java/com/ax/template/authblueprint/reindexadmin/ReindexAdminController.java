package com.ax.template.authblueprint.reindexadmin;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * admin_preauthorize_guard.sh fail fixture — BYPASS (a) part 2.
 *
 * This admin controller is mapped under "/api/administrator" (a DIFFERENT path
 * segment) and carries no method authz. SecurityConfig protects
 * "/api/admin/**" with ROLE_ADMIN. A raw startswith check would falsely credit
 * "/api/administrator/reindex" to the "/api/admin/**" matcher — but Spring's
 * Ant matcher "/api/admin/**" does NOT match "/api/administrator...". The
 * hardened guard uses boundary-aware Ant matching → this endpoint is uncovered
 * → BLOCKED.
 */
@RestController
@RequestMapping("/api/administrator")
public class ReindexAdminController {

    @PostMapping("/reindex")
    public String reindex() {
        return "reindex started";
    }
}
