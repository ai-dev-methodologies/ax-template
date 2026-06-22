package com.ax.template.authblueprint.authzparity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * authorization-parity-l0 thin controller. The requester/approver/executor is ALWAYS the
 * authenticated caller (caller-authentication-only-no-userid-param). Delegates to
 * {@link AuthorizationParityService}.
 */
@RestController
public class AuthorizationParityController {

    public record AuthorizeReq(@NotBlank @Size(max = 100) String actionType,
                               @NotNull Map<@NotBlank @Size(max = 100) String,
                                            @Size(max = 500) String> authorizedParams,
                               boolean highValue,
                               Set<@NotBlank @Size(max = 100) String> requiredGates) {}
    public record SatisfyGateReq(@NotBlank @Size(max = 100) String gateKey) {}
    public record ExecuteReq(@NotNull Map<@NotBlank @Size(max = 100) String,
                                          @Size(max = 500) String> executionParams) {}

    public record ActionDto(UUID id, String actionType, String authorizedParams, String parityHash,
                            boolean highValue, String requesterUserId, Set<String> requiredGates,
                            ActionStatus status, Instant executedAt) {
        static ActionDto of(AuthorizedAction a) {
            return new ActionDto(a.getId(), a.getActionType(), a.getAuthorizedParams(), a.getParityHash(),
                a.isHighValue(), a.getRequesterUserId(), a.getRequiredGates(), a.getStatus(), a.getExecutedAt());
        }
    }
    public record SignoffDto(UUID id, String approverUserId, Instant signedAt) {
        static SignoffDto of(ActionSignoff s) {
            return new SignoffDto(s.getId(), s.getApproverUserId(), s.getSignedAt());
        }
    }
    public record GateDto(UUID id, String gateKey, String satisfiedBy, Instant satisfiedAt) {
        static GateDto of(GateSatisfaction g) {
            return new GateDto(g.getId(), g.getGateKey(), g.getSatisfiedBy(), g.getSatisfiedAt());
        }
    }
    public record BlockedDto(UUID id, String offeredHash, String authorizedHash, String attemptedBy,
                             Instant attemptedAt) {
        static BlockedDto of(BlockedAttempt b) {
            return new BlockedDto(b.getId(), b.getOfferedHash(), b.getAuthorizedHash(),
                b.getAttemptedBy(), b.getAttemptedAt());
        }
    }

    private final AuthorizationParityService service;

    public AuthorizationParityController(AuthorizationParityService service) {
        this.service = service;
    }

    @PostMapping("/api/authz-parity/actions")
    public ResponseEntity<ActionDto> authorize(@Valid @RequestBody AuthorizeReq req, Authentication auth) {
        Set<String> gates = req.requiredGates() == null ? Set.of() : new TreeSet<>(req.requiredGates());
        return ResponseEntity.status(HttpStatus.CREATED).body(ActionDto.of(
            service.authorize(req.actionType(), req.authorizedParams(), req.highValue(), gates, auth.getName())));
    }

    @PostMapping("/api/authz-parity/actions/{id}/signoffs")
    public ResponseEntity<SignoffDto> signoff(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(SignoffDto.of(service.signoff(id, auth.getName())));
    }

    @PostMapping("/api/authz-parity/actions/{id}/gates")
    public ResponseEntity<GateDto> satisfyGate(@PathVariable UUID id, @Valid @RequestBody SatisfyGateReq req,
                                               Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(GateDto.of(service.satisfyGate(id, req.gateKey(), auth.getName())));
    }

    /** AUTHZPARITY-EXEC-001 — re-hash actual params; mismatch → 409 + a recorded blocked attempt. */
    @PostMapping("/api/authz-parity/actions/{id}/execute")
    public ActionDto execute(@PathVariable UUID id, @Valid @RequestBody ExecuteReq req, Authentication auth) {
        return ActionDto.of(service.execute(id, req.executionParams(), auth.getName()));
    }

    @GetMapping("/api/authz-parity/actions/{id}")
    public ActionDto get(@PathVariable UUID id) {
        return ActionDto.of(service.get(id));
    }

    @GetMapping("/api/authz-parity/actions/{id}/signoffs")
    public List<SignoffDto> signoffs(@PathVariable UUID id) {
        return service.signoffs(id).stream().map(SignoffDto::of).toList();
    }

    @GetMapping("/api/authz-parity/actions/{id}/gates")
    public List<GateDto> gates(@PathVariable UUID id) {
        return service.gates(id).stream().map(GateDto::of).toList();
    }

    @GetMapping("/api/authz-parity/actions/{id}/blocked-attempts")
    public List<BlockedDto> blockedAttempts(@PathVariable UUID id) {
        return service.blockedAttempts(id).stream().map(BlockedDto::of).toList();
    }

    @ExceptionHandler(AuthorizationParityException.class)
    public ResponseEntity<ProblemDetail> handle(AuthorizationParityException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
