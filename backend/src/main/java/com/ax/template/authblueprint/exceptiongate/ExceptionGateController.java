package com.ax.template.authblueprint.exceptiongate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * orthogonal-exception-gate-l0 thin controller. Delegates to {@link ExceptionGateService}.
 * {@code /probe} simulates a caller ATTEMPTING an operation against the subject — the fixture
 * a fork-receiver's real domain replaces with its own protected endpoint's pre-check.
 */
@RestController
public class ExceptionGateController {

    public record ReasonReq(@Size(max = 500) String reason) {}
    public record PrimaryStateReq(@NotBlank @Size(max = 100) String newState) {}
    public record ProbeReq(@NotBlank @Size(max = 50) String operation) {}

    public record GateDto(UUID id, String subjectType, String subjectId, boolean raised,
                          String reason, String primaryState) {
        static GateDto of(ExceptionGate g) {
            return new GateDto(g.getId(), g.getSubjectType(), g.getSubjectId(), g.isRaised(),
                g.getReason(), g.getPrimaryState());
        }
    }
    public record AuditDto(UUID id, String action, String reason, String actor, Instant occurredAt) {
        static AuditDto of(ExceptionAuditEntry a) {
            return new AuditDto(a.getId(), a.getAction(), a.getReason(), a.getActor(), a.getOccurredAt());
        }
    }
    public record ProbeDto(boolean allowed, String operation) {}

    private final ExceptionGateService service;

    public ExceptionGateController(ExceptionGateService service) {
        this.service = service;
    }

    @PostMapping("/api/exception-gate/{subjectType}/{subjectId}/raise")
    public GateDto raise(@PathVariable String subjectType, @PathVariable String subjectId,
                         @Valid @RequestBody ReasonReq req, Authentication auth) {
        return GateDto.of(service.raise(subjectType, subjectId, req.reason(), auth.getName()));
    }

    @PostMapping("/api/exception-gate/{subjectType}/{subjectId}/lift")
    public GateDto lift(@PathVariable String subjectType, @PathVariable String subjectId,
                        @Valid @RequestBody ReasonReq req, Authentication auth) {
        return GateDto.of(service.lift(subjectType, subjectId, req.reason(), auth.getName()));
    }

    @PostMapping("/api/exception-gate/{subjectType}/{subjectId}/primary-state")
    public GateDto advancePrimary(@PathVariable String subjectType, @PathVariable String subjectId,
                                  @Valid @RequestBody PrimaryStateReq req) {
        return GateDto.of(service.advancePrimary(subjectType, subjectId, req.newState()));
    }

    @PostMapping("/api/exception-gate/{subjectType}/{subjectId}/probe")
    public ProbeDto probe(@PathVariable String subjectType, @PathVariable String subjectId,
                          @Valid @RequestBody ProbeReq req) {
        service.checkAllowed(subjectType, subjectId, req.operation());
        return new ProbeDto(true, req.operation());
    }

    @GetMapping("/api/exception-gate/{subjectType}/{subjectId}")
    public GateDto get(@PathVariable String subjectType, @PathVariable String subjectId) {
        return GateDto.of(service.get(subjectType, subjectId));
    }

    @GetMapping("/api/exception-gate/{subjectType}/{subjectId}/audit")
    public List<AuditDto> audit(@PathVariable String subjectType, @PathVariable String subjectId) {
        return service.auditTrail(subjectType, subjectId).stream().map(AuditDto::of).toList();
    }

    @ExceptionHandler(ExceptionGateException.class)
    public ResponseEntity<ProblemDetail> handle(ExceptionGateException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
