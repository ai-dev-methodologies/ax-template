package com.ax.template.authblueprint.apikey;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * REST surface for the api-key management domain. Trace:
 * <ul>
 *   <li>KEY-AUTHZ-001 — every endpoint here is JWT-only (SecurityConfig matcher +
 *       the auth filter explicitly skips this path).</li>
 *   <li>KEY-AUTHZ-002 — cross-user lookups return 404 (not 403).</li>
 *   <li>KEY-AUTHN-001 — plaintext is returned only from {@link #create} /
 *       {@link #rotate}; never from {@link #list} / {@link #get}.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/api-keys")
public class ApiKeyController {

    private final ApiKeyService service;

    public ApiKeyController(ApiKeyService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CreateApiKeyResponse> create(Authentication auth,
                                                       @Valid @RequestBody(required = false) CreateApiKeyRequest body) {
        CreateApiKeyResponse response = service.create(auth.getName(), body);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .location(URI.create("/api/api-keys/" + response.id()))
            .body(response);
    }

    @GetMapping
    public ApiKeyListResponse list(Authentication auth) {
        return service.list(auth.getName());
    }

    @GetMapping("/{id}")
    public ApiKeyResponse get(Authentication auth, @PathVariable UUID id) {
        return service.get(auth.getName(), id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(Authentication auth, @PathVariable UUID id) {
        service.revoke(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/rotate")
    public ResponseEntity<CreateApiKeyResponse> rotate(Authentication auth, @PathVariable UUID id) {
        CreateApiKeyResponse response = service.rotate(auth.getName(), id);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    // ── Exception → HTTP mapping ─────────────────────────────────────────────

    @ExceptionHandler(ApiKeyNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ApiKeyNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "API_KEY_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(TooManyApiKeysException.class)
    public ResponseEntity<ProblemDetail> handleQuota(TooManyApiKeysException ex) {
        return problem(HttpStatus.BAD_REQUEST, "TOO_MANY_KEYS", ex.getMessage());
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setProperty("code", code);
        return ResponseEntity.status(status).body(pd);
    }
}
