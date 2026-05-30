package com.ax.template.authblueprint.dsr;

import com.ax.template.authblueprint.dsr.DsrDtos.AccessBundle;
import com.ax.template.authblueprint.dsr.DsrDtos.DsrRequestResponse;
import com.ax.template.authblueprint.dsr.DsrDtos.ErasureManifest;
import com.ax.template.authblueprint.dsr.DsrDtos.ExtendRequest;
import com.ax.template.authblueprint.dsr.DsrDtos.LiftRequest;
import com.ax.template.authblueprint.dsr.DsrDtos.RectifyRequest;

import jakarta.validation.Valid;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Thin REST surface for data-subject-rights. Delegates to {@link DsrService} ONLY —
 * it never injects or calls a repository (ArchitectureLayerBoundaryTest).
 *
 * <p>The data-subject is always {@link Authentication#getName()}; no path/body param
 * carries a subject id. Cross-subject admin access is the separate
 * {@code /api/admin/dsr/**} surface (already gated ROLE_ADMIN by SecurityConfig).
 *
 * <p>Responses that return personal data carry {@code Cache-Control: no-store}
 * (access / portability bundles) so personal data never lands in a browser or proxy
 * cache. A stable {@code DSR-Schema-Version} header pins the portability contract
 * (DSR-PORTABILITY-001).
 */
@RestController
@RequestMapping("/api")
public class DsrController {

    /** Stable export schema contract (DSR-PORTABILITY-001 schema_version header). */
    static final String SCHEMA_VERSION = "1.0";

    private final DsrService service;

    public DsrController(DsrService service) {
        this.service = service;
    }

    // ── DSR-ACCESS-001 ────────────────────────────────────────────────────────

    @PostMapping("/me/dsr/access")
    public ResponseEntity<AccessBundle> access(Authentication auth) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .cacheControl(CacheControl.noStore())
            .body(service.openAccess(auth));
    }

    // ── DSR-RECTIFY-001 ───────────────────────────────────────────────────────

    @PatchMapping("/me/dsr/rectify")
    public ResponseEntity<DsrRequestResponse> rectify(Authentication auth,
                                                      @Valid @RequestBody RectifyRequest body) {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(service.rectify(auth, body));
    }

    // ── DSR-ERASURE-001 ───────────────────────────────────────────────────────

    @PostMapping("/me/dsr/erasure")
    public ResponseEntity<ErasureManifest> erasure(Authentication auth) {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(service.erase(auth));
    }

    // ── DSR-PORTABILITY-001 ───────────────────────────────────────────────────

    @PostMapping("/me/dsr/portability")
    public ResponseEntity<AccessBundle> portability(Authentication auth,
                                                    @RequestParam(required = false) String format) {
        AccessBundle bundle = service.producePortableCopy(auth, format);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .cacheControl(CacheControl.noStore())
            .header("DSR-Schema-Version", SCHEMA_VERSION)
            .body(bundle);
    }

    // ── DSR-RESTRICT-001 ──────────────────────────────────────────────────────

    @PostMapping("/me/dsr/restrict")
    public ResponseEntity<DsrRequestResponse> restrict(Authentication auth) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .cacheControl(CacheControl.noStore())
            .body(service.restrict(auth));
    }

    @PostMapping("/me/dsr/restrict/lift")
    public ResponseEntity<DsrRequestResponse> lift(Authentication auth,
                                                   @Valid @RequestBody LiftRequest body) {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(service.lift(auth, body.justification()));
    }

    // ── DSR-SLA-001 ───────────────────────────────────────────────────────────

    @GetMapping("/me/dsr/requests/{requestId}")
    public ResponseEntity<DsrRequestResponse> get(Authentication auth, @PathVariable UUID requestId) {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(service.get(auth, requestId));
    }

    @PostMapping("/me/dsr/requests/{requestId}/extend")
    public ResponseEntity<DsrRequestResponse> extend(Authentication auth,
                                                     @PathVariable UUID requestId,
                                                     @Valid @RequestBody ExtendRequest body) {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(service.extend(auth, requestId, body.extensionDays(), body.extensionReason()));
    }

    /** Admin cross-subject lookup — gated ROLE_ADMIN by the /api/admin/** matcher. */
    @GetMapping("/admin/dsr/requests/{requestId}")
    public ResponseEntity<DsrRequestResponse> adminGet(Authentication auth,
                                                       @PathVariable UUID requestId) {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(service.get(auth, requestId));
    }

    // ── domain exception → RFC 9457 ProblemDetail ─────────────────────────────

    @ExceptionHandler(DsrException.class)
    public ResponseEntity<ProblemDetail> handleDsr(DsrException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalTransition(IllegalStateException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setProperty("code", "DSR_ILLEGAL_TRANSITION");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }
}
