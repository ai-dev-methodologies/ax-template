package com.ax.template.authblueprint.sessionmanagement;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin force-logout surface. Lives under {@code /api/admin/sessions} so the
 * SecurityConfig admin matcher gates it with {@code hasAuthority("ROLE_ADMIN")}
 * before the @PreAuthorize on the method even runs.
 *
 * <p>Trace: SESS-AUTHZ-003.
 */
@RestController
@RequestMapping("/api/admin/sessions")
public class AdminSessionController {

    private final SessionService service;

    public AdminSessionController(SessionService service) {
        this.service = service;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> forceRevoke(Authentication auth, @PathVariable UUID id) {
        service.adminRevoke(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(SessionNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setProperty("code", "SESSION_NOT_FOUND");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }
}
