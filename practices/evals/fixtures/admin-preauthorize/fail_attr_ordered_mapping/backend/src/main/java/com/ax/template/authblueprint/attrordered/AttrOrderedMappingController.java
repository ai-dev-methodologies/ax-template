package com.ax.template.authblueprint.attrordered;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * admin_preauthorize_guard.sh fail fixture — round-5 codex finding: the
 * attribute-ordered mapping-path parse gap.
 *
 * This class is NOT *AdminController-named and has NO class-level '/admin'
 * @RequestMapping; it is detected as an admin surface purely via the WIDENED
 * method-level-path detector, triggered here by {@code rotate()}'s ordinary
 * bare-shorthand {@code @PostMapping("/api/admin/x/rotate")} (already
 * correctly gated by @PreAuthorize).
 *
 * {@code create()} is the deliberate miss: its @PostMapping lists {@code
 * produces} BEFORE {@code path} —
 *   {@code @PostMapping(produces = "application/json", path = "/api/admin/x")}
 * The OLD guard extracted the mapping path by taking the FIRST quoted string
 * in the annotation's argument list ({@code QUOTED_RE.search(args)}), which
 * misread {@code "application/json"} as the endpoint path. Because that
 * misread path does not match {@code /api/admin/**}, the OLD guard silently
 * did NOT require this endpoint to carry any authorization at all — a
 * required admin mutation with ZERO @PreAuthorize went completely uncounted
 * (this is the shape behind the real-repo "required endpoints silently
 * dropped 25→24" reproduction). The FIXED guard reads the {@code path=}
 * attribute explicitly regardless of its position among the annotation's
 * other named attributes, correctly detects this endpoint as a required
 * /api/admin mutation, and — since it carries no @PreAuthorize at all —
 * BLOCKS (exit 1).
 */
@RestController
public class AttrOrderedMappingController {

    @PostMapping(produces = "application/json", path = "/api/admin/x")
    public String create() {
        return "created";
    }

    @PostMapping("/api/admin/x/rotate")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String rotate() {
        return "rotated";
    }
}
