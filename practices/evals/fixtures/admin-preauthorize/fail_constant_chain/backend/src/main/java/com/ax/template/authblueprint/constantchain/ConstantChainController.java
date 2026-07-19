package com.ax.template.authblueprint.constantchain;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * admin_preauthorize_guard.sh fail fixture — round-8 codex Finding (constant
 * CHAIN, as distinct from round-7's Finding 3-simple single-literal constant).
 *
 * {@code ok()}'s ordinary bare {@code @PostMapping("/api/admin/ok")} (correctly
 * gated) makes this class an admin surface via the WIDENED method-path
 * detector — same establishing shape codex used in its round-8 repro.
 *
 * {@code create()} is the deliberate miss: its mapping argument folds through
 * a CHAIN of two same-file {@code static final String} constants, where the
 * SECOND constant's own initializer is an EXPRESSION referencing the FIRST —
 *   {@code static final String API = "/api";}
 *   {@code static final String ADMIN = API + "/admin";}
 *   {@code @PostMapping(ADMIN + "/x")}   // → "/api/admin/x"
 * PRE-FIX, {@code build_const_map} only captured a constant whose initializer
 * was a single bare {@code "literal"} string — it never resolved an
 * initializer that was itself an EXPRESSION referencing another same-file
 * constant. So {@code ADMIN} was never entered into the constant map at all,
 * and {@code @PostMapping(ADMIN + "/x")} folded to nothing: the required admin
 * mutation (ZERO @PreAuthorize) went uncounted and the pre-fix guard exited 0
 * (MISS). The FIXED guard resolves same-file constant declarations to a FIXED
 * POINT — reusing the exact literal/identifier/{@code +} subset already
 * supported by {@code fold_scalar} — so {@code ADMIN} folds to
 * {@code "/api/admin"} first, then {@code ADMIN + "/x"} folds to
 * {@code "/api/admin/x"}, requires authz, finds none → BLOCKS (exit 1).
 * (Imported/opaque constants and {@code ${...}}/{@code #{...}} placeholders
 * remain controller-locally undecidable and OUT OF SCOPE — deferred to the
 * domain 403 tests + runtime.)
 */
@RestController
public class ConstantChainController {

    static final String API = "/api";
    static final String ADMIN = API + "/admin";

    @PostMapping("/api/admin/ok")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String ok() {
        return "ok";
    }

    @PostMapping(ADMIN + "/x")
    public String create() {
        return "created";
    }
}
