package com.ax.template.authblueprint.negadmin;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * admin_preauthorize_guard.sh fail fixture — round-4 codex Fix 3a (obviously
 * ineffective SpEL: leading NEGATION of the admin predicate). The mutating
 * admin endpoint carries {@code @PreAuthorize("!hasAuthority('ROLE_ADMIN')")},
 * which requires the admin authority to be ABSENT — the inverse of a gate (every
 * NON-admin caller passes). A naive "does an admin predicate appear?" check would
 * see hasAuthority('ROLE_ADMIN') and PASS; the hardened guard detects the leading
 * {@code !} and rejects it → BLOCKED (exit 1).
 */
@RestController
public class NegatedSpelController {

    @GetMapping("/api/admin/neg")
    public List<String> list() {
        return List.of("a", "b");
    }

    @PostMapping("/api/admin/neg/export")
    @PreAuthorize("!hasAuthority('ROLE_ADMIN')")
    public String export() {
        return "exported";
    }
}
