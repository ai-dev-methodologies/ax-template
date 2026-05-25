package com.ax.template.authblueprint.identityverification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
 */
@RestController
@RequestMapping("/api/admin/identity-verification")
public class IdentityVerificationAdminController {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 200;

    private final VerifiedIdentityRepository repository;

    public IdentityVerificationAdminController(VerifiedIdentityRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<PageResponse> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "provider", required = false) String provider) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "verifiedAt"));

        Page<VerifiedIdentity> result = (provider == null || provider.isBlank())
            ? repository.findAll(pageable)
            : repository.findAllByProviderName(provider, pageable);

        List<Row> rows = result.getContent().stream().map(Row::from).toList();
        PageResponse body = new PageResponse(
                rows,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Pragma", "no-cache")
                .body(body);
    }

    public record Row(UUID id, String providerName, Instant verifiedAt,
                      String name, String dob) {
        static Row from(VerifiedIdentity v) {
            return new Row(v.getId(), v.getProviderName(), v.getVerifiedAt(),
                           v.getName(), v.getDob());
        }
    }

    public record PageResponse(List<Row> content, int page, int size,
                                long totalElements, int totalPages) {}
}
