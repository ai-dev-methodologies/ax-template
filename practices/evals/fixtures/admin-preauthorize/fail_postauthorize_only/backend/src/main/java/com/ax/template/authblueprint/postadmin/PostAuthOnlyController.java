package com.ax.template.authblueprint.postadmin;

import java.util.List;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * admin_preauthorize_guard.sh fail fixture — round-4 codex Fix 1.
 * The mutating admin endpoint is "protected" ONLY by a method-level
 * {@code @PostAuthorize("hasAuthority('ROLE_ADMIN')")}. That is INEFFECTIVE for a
 * MUTATION: @PostAuthorize runs AFTER the handler body (after the side effect),
 * so the write has already happened before authorization is evaluated. Only a
 * @PreAuthorize gates a mutation. Detected as an admin surface via the
 * method-level {@code /api/admin/...} mapping (WIDENED detection), the POST has
 * no @PreAuthorize → BLOCKED (exit 1).
 */
@RestController
public class PostAuthOnlyController {

    @GetMapping("/api/admin/postauth")
    public List<String> list() {
        return List.of("a", "b");
    }

    @PostMapping("/api/admin/postauth/export")
    @PostAuthorize("hasAuthority('ROLE_ADMIN')")
    public String export() {
        return "exported";
    }
}
