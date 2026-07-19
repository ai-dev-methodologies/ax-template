package com.ax.template.authblueprint.mixedadmin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * admin_preauthorize_guard.sh fail fixture — BYPASS (b): @PatchMapping was
 * omitted from the mapping-annotation regex, so a mutating PATCH endpoint was
 * never scanned at all. Here the GET is covered by the "/api/admin/**"
 * ROLE_ADMIN matcher, but the PATCH is mapped at "/api/other/mixed/{id}" —
 * OUTSIDE that matcher and with no authz. The pre-hardening guard silently
 * skipped it (false PASS); the hardened guard scans @PatchMapping → uncovered
 * mutating endpoint → BLOCKED.
 *
 * No class-level @RequestMapping: each method path is absolute, so the PATCH
 * really does resolve outside the admin matcher's coverage.
 */
@RestController
public class MixedAdminController {

    @GetMapping("/api/admin/mixed")
    public String read() {
        return "ok";
    }

    @PatchMapping("/api/other/mixed/{id}")
    public String patch(@PathVariable String id) {
        return "patched " + id;
    }
}
