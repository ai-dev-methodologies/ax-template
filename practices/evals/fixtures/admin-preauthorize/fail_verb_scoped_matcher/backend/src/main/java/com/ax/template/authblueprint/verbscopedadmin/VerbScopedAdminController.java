package com.ax.template.authblueprint.verbscopedadmin;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * admin_preauthorize_guard.sh fail fixture — codex round-2 HIGH verb-bypass.
 *
 * This admin controller carries NO @PreAuthorize (class or method). It relies
 * entirely on the SecurityConfig matcher chain. The GET read is genuinely
 * ROLE_ADMIN-gated (verb-specific admin matcher), but the mutating @PostMapping
 * is reachable by any authenticated NON-admin caller because Spring's
 * first-match for a POST is the verb-agnostic .authenticated() fallback, not
 * the GET-scoped ROLE_ADMIN matcher. The hardened guard MUST BLOCK on the POST.
 */
@RestController
@RequestMapping("/api/admin/verbscoped")
public class VerbScopedAdminController {

    @GetMapping("/reports")
    public List<String> reports() {
        return List.of("r1");
    }

    @PostMapping("/mutate")
    public String mutate() {
        return "mutated";
    }
}
