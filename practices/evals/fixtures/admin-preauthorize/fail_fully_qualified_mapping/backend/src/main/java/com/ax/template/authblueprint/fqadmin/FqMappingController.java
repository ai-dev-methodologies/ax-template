package com.ax.template.authblueprint.fqadmin;

import java.util.List;

/**
 * admin_preauthorize_guard.sh fail fixture — round-4 codex Fix 2 (recognition
 * completeness). The mutating admin endpoint uses a FULLY-QUALIFIED mapping
 * annotation {@code @org.springframework.web.bind.annotation.PostMapping} (no
 * import). A recogniser that only matched a bare {@code @PostMapping} at line
 * start would treat this handler as INVISIBLE and green the file (false PASS);
 * the hardened guard scans the FQN form, detects the {@code /api/admin/...}
 * surface, and finds NO @PreAuthorize → BLOCKED (exit 1).
 */
@org.springframework.web.bind.annotation.RestController
public class FqMappingController {

    @org.springframework.web.bind.annotation.GetMapping("/api/admin/fq")
    public List<String> list() {
        return List.of("a", "b");
    }

    @org.springframework.web.bind.annotation.PostMapping("/api/admin/fq/export")
    public String export() {
        return "exported";
    }
}
