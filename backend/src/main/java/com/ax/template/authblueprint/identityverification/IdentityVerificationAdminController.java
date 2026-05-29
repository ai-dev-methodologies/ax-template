package com.ax.template.authblueprint.identityverification;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * R54 — IDV-ADMIN-001 admin browse surface.
 *
 * <p>GET /api/admin/identity-verification — paginated VerifiedIdentity list,
 * gated by ROLE_ADMIN. Non-admin caller → 403. Unauthenticated → 401
 * (SecurityConfig already enforces this for /api/admin/**).
 *
 * <p>Response shape is intentionally tight: id / providerName / verifiedAt /
 * name / dob / metadata. CI and DI are NOT exposed in this list view — they
 * are correlation tokens used internally; an explicit lookup endpoint can
 * surface them if a fork-receiver needs that path.
 *
 * <p>R52 lesson preempted day-one: every response uses
 * {@link CacheControl#noStore()} so PII-bearing admin lists never live in
 * shared caches.
 *
 * <p>IMW1-A — this controller routes ALL repository access through
 * {@link IdentityVerificationService} (the domain's sole orchestrator). The
 * page/size clamping, ordering, provider filter and Row projection live in
 * {@link IdentityVerificationService#listAdmin(int, int, String)}; the
 * controller only wires HTTP concerns (RBAC, query params, cache headers).
 */
@RestController
@RequestMapping("/api/admin/identity-verification")
public class IdentityVerificationAdminController {

    private final IdentityVerificationService service;

    public IdentityVerificationAdminController(IdentityVerificationService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<IdentityVerificationService.PageResponse> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "provider", required = false) String provider) {
        IdentityVerificationService.PageResponse body = service.listAdmin(page, size, provider);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Pragma", "no-cache")
                .body(body);
    }
}
