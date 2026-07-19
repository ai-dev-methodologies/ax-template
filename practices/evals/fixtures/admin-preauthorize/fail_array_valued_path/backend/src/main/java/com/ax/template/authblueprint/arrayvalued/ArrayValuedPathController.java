package com.ax.template.authblueprint.arrayvalued;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * admin_preauthorize_guard.sh fail fixture — round-6 codex finding: the
 * ARRAY-valued mapping-path parse gap.
 *
 * This class is NOT *AdminController-named and has NO class-level '/admin'
 * @RequestMapping; it is detected as an admin surface purely via the WIDENED
 * method-level-path detector, triggered here by {@code ok()}'s ordinary
 * bare-shorthand {@code @PostMapping("/api/admin/ok")} (already correctly
 * gated by @PreAuthorize).
 *
 * {@code missed()} is the deliberate miss: its @PostMapping declares an
 * ARRAY of paths via {@code value = {...}}, with the admin path as the
 * SECOND element —
 *   {@code @PostMapping(value = {"/public", "/api/admin/missed"})}
 * Spring mapping annotations legally accept an array of paths (a single
 * handler answering multiple routes). The round-5 extractor
 * (`extract_mapping_path`) only matched a SCALAR `path=`/`value=` attribute
 * (`PATH_ATTR_RE` requires a literal `"` immediately after `=`); against a
 * `{...}` array value it found no match and returned "", so this endpoint's
 * path was silently treated as empty — `ADMIN_PATH_RE.match("")` never
 * matches, so this required admin mutation (with ZERO @PreAuthorize) was
 * dropped from the per-endpoint requirement check just like the round-5
 * attribute-ordering gap, one level down. The FIXED guard
 * (`extract_mapping_paths`) parses ALL quoted strings inside the `{...}`
 * array and correctly detects `/api/admin/missed` among them — since
 * `missed()` carries no @PreAuthorize at all, the FIXED guard BLOCKS
 * (exit 1).
 */
@RestController
public class ArrayValuedPathController {

    @PostMapping("/api/admin/ok")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String ok() {
        return "ok";
    }

    @PostMapping(value = {"/public", "/api/admin/missed"})
    public String missed() {
        return "missed";
    }
}
