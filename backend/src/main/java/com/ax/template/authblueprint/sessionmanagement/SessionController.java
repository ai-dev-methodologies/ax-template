package com.ax.template.authblueprint.sessionmanagement;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import com.ax.template.authblueprint.sessionmanagement.SessionDtos.RegisterSessionRequest;
import com.ax.template.authblueprint.sessionmanagement.SessionDtos.RevokeOthersResponse;
import com.ax.template.authblueprint.sessionmanagement.SessionDtos.SessionListResponse;
import com.ax.template.authblueprint.sessionmanagement.SessionDtos.SessionResponse;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService service;

    public SessionController(SessionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SessionResponse> register(Authentication auth,
                                                    @Valid @RequestBody RegisterSessionRequest body) {
        SessionService.RegisterResult result = service.register(auth.getName(), body);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.response());
    }

    @GetMapping
    public SessionListResponse list(Authentication auth) {
        return service.listMine(auth.getName());
    }

    @GetMapping("/{id}")
    public SessionResponse get(Authentication auth, @PathVariable UUID id) {
        return service.getMine(auth.getName(), id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(Authentication auth, @PathVariable UUID id) {
        service.revokeMine(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/revoke-others")
    public RevokeOthersResponse revokeOthers(Authentication auth, @RequestParam UUID keep) {
        return service.revokeOthers(auth.getName(), keep);
    }

    @PostMapping("/{id}/heartbeat")
    public ResponseEntity<Void> heartbeat(Authentication auth, @PathVariable UUID id) {
        service.heartbeat(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(SessionNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setProperty("code", "SESSION_NOT_FOUND");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(ExpiresAtInPastException.class)
    public ResponseEntity<ProblemDetail> handleExpiresAt(ExpiresAtInPastException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setProperty("code", "EXPIRES_AT_IN_PAST");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }
}
