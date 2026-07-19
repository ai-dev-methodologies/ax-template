package com.ax.template.authblueprint.disjadmin;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * admin_preauthorize_guard.sh fail fixture — round-4 codex Fix 3b (obviously
 * ineffective SpEL: trivial always-true disjunction). The mutating admin
 * endpoint carries {@code @PreAuthorize("hasAuthority('ROLE_ADMIN') or true")},
 * which short-circuits to always-true — every caller passes. A naive check would
 * see the admin predicate and PASS; the hardened guard detects the bare
 * {@code true} disjunct and rejects it → BLOCKED (exit 1).
 */
@RestController
public class DisjunctionTrueController {

    @GetMapping("/api/admin/disj")
    public List<String> list() {
        return List.of("a", "b");
    }

    @PostMapping("/api/admin/disj/export")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or true")
    public String export() {
        return "exported";
    }
}
