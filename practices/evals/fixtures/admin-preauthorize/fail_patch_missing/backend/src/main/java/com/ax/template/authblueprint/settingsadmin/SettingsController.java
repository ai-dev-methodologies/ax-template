package com.ax.template.authblueprint.settingsadmin;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * admin_preauthorize_guard.sh fail fixture — proves THREE things at once:
 *   1. admin-surface detection via the class-level {@code @RequestMapping(path = ...)}
 *      ALIAS. This class is NOT named *AdminController; it is detected purely by
 *      its '/admin' path — the decoupling from the naming convention that the
 *      iter2-G1 dogfood expiry-trigger demanded.
 *   2. {@code @PatchMapping} is treated as a MUTATING verb (not just POST/DELETE).
 *   3. a mutating admin endpoint with NO @PreAuthorize anywhere is the BFLA shape.
 * There is no SecurityConfig in this fixture root; the guard does not read one
 * anyway → BLOCKED (exit 1).
 */
@RestController
@RequestMapping(path = "/api/admin/settings")
public class SettingsController {

    @PatchMapping("/{id}")
    public String update(@PathVariable String id) {
        return "updated " + id;
    }
}
