package com.ax.template.authblueprint.softdelete;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * soft-delete-l0 reference workload — a tombstoned, owner-scoped account aggregate (with child
 * notes) exercising the full lifecycle: tombstone delete, default-excluding reads (admin opt-in),
 * cascade, restore-within-window, erasure, and a live-only unique key. Conflicts surface as RFC 9457
 * problem+json via {@link SoftDeleteAdvice} (409) and the global advice (404).
 *
 * <p>Spec: specs/soft-delete-l0.yaml.
 */
@RestController
@RequestMapping("/api/soft-delete/accounts")
public class SoftDeleteController {

    public record CreateAccount(String email, String name) {}
    public record CreateNote(String text) {}

    private final SoftDeleteService service;

    public SoftDeleteController(SoftDeleteService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody CreateAccount req, Authentication auth) {
        SoftDeleteAccount a = service.create(auth.getName(), req.email(), req.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto(a));
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<Map<String, Object>> addNote(
            @PathVariable UUID id, @RequestBody CreateNote req, Authentication auth) {
        SoftDeleteNote n = service.addNote(id, auth.getName(), req.text());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", n.getId().toString(), "text", n.getText()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(dto(service.getLive(id, auth.getName())));   // tombstoned → 404
    }

    /** Live (non-tombstoned) child notes — owner-scoped; lets the owner observe cascade soft-delete/restore. */
    @GetMapping("/{id}/notes")
    public ResponseEntity<List<Map<String, Object>>> notes(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(service.liveNotes(id, auth.getName()).stream()
                .map(n -> Map.<String, Object>of("id", n.getId().toString(), "text", n.getText())).toList());
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(
            @RequestParam(name = "include_deleted", defaultValue = "false") boolean includeDeleted,
            Authentication auth) {
        // include_deleted honored ONLY for ROLE_ADMIN; a non-admin flag is silently ignored (QUERY-001)
        List<SoftDeleteAccount> rows = service.list(auth.getName(), includeDeleted, isAdmin(auth));
        return ResponseEntity.ok(rows.stream().map(SoftDeleteController::dto).toList());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication auth) {
        service.delete(id, auth.getName());                                   // second delete → 404
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<Map<String, Object>> restore(@PathVariable UUID id, Authentication auth) {
        service.restore(id, auth.getName());                                  // 409 / 404 via advice
        return ResponseEntity.ok(dto(service.getLive(id, auth.getName())));
    }

    @DeleteMapping("/{id}/erase")
    public ResponseEntity<Void> erase(@PathVariable UUID id, Authentication auth) {
        service.erase(id, auth.getName());                                    // idempotent: second → 404
        return ResponseEntity.noContent().build();
    }

    private static boolean isAdmin(Authentication auth) {
        for (GrantedAuthority a : auth.getAuthorities()) {
            if ("ROLE_ADMIN".equals(a.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Object> dto(SoftDeleteAccount a) {
        return Map.of("id", a.getId().toString(), "email", a.getEmail(),
                "name", a.getName(), "deleted", a.isDeleted());
    }
}
