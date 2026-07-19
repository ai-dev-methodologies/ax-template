package com.ax.template.authblueprint.classfqn;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * admin_preauthorize_guard.sh fail fixture — round-7 codex Finding 7
 * (class-level FULLY-QUALIFIED mapping recognition).
 *
 * The class-level mapping is written as a fully-qualified annotation
 * {@code @org.springframework.web.bind.annotation.RequestMapping("/api/admin")}
 * (no import). METHOD-level mappings were already FQN-tolerant
 * ({@code MAPPING_START_RE}), but the class-level discovery scanned only for a
 * LITERAL {@code @RequestMapping(} — so the pre-fix guard read this class's
 * paths as EMPTY: {@code admin_by_class} was false and the {@code /api/admin}
 * class prefix vanished. {@code create()} ({@code /x}, no @PreAuthorize) was
 * therefore never required (its effective route {@code /api/admin/x} was
 * invisible); the pre-fix guard exited 0 (the SentinelAdminController in the
 * same root keeps the scan non-empty). The FIXED guard recognises the
 * class-level mapping with the SAME FQN-tolerant recogniser, reads the
 * {@code /api/admin} class prefix, marks the whole class an admin surface,
 * requires authz on {@code create()}, finds none → BLOCKS (exit 1).
 */
@org.springframework.web.bind.annotation.RequestMapping("/api/admin")
@RestController
public class ClassLevelFqnController {

    @PostMapping("/x")
    public String create() {
        return "created";
    }
}
