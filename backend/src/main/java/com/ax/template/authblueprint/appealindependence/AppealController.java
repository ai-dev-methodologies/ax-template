package com.ax.template.authblueprint.appealindependence;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
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
import java.util.UUID;

/**
 * appeal-decider-independence-l0 thin controller. The acting decider is ALWAYS the
 * authenticated caller (caller-authentication-only — never a body/path userId param),
 * mirroring {@code decisiongov.DecisionController}'s posture.
 */
@RestController
public class AppealController {

    public record FileOriginalReq(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,32}") String outcome) {}
    public record FileAppealReq(@NotBlank @Pattern(regexp = "UPHOLD|OVERTURN") String outcome) {}

    public record AppealDto(UUID id, UUID parentDecisionId, UUID chainRootId, int level,
                            AppealDecisionKind kind, String decidedBy, String outcome, Instant decidedAt) {
        static AppealDto of(AppealDecision d) {
            return new AppealDto(d.getId(), d.getParentDecisionId(), d.getChainRootId(), d.getLevel(),
                d.getKind(), d.getDecidedBy(), d.getOutcome(), d.getDecidedAt());
        }
    }

    private final AppealService service;

    public AppealController(AppealService service) {
        this.service = service;
    }

    @PostMapping("/api/appeals")
    public ResponseEntity<AppealDto> fileOriginal(@Valid @RequestBody FileOriginalReq req, Authentication auth) {
        AppealDecision d = service.fileOriginal(auth.getName(), req.outcome());
        return ResponseEntity.status(HttpStatus.CREATED)
            .location(URI.create("/api/appeals/" + d.getId()))
            .body(AppealDto.of(d));
    }

    @PostMapping("/api/appeals/{id}/appeal")
    public ResponseEntity<AppealDto> fileAppeal(@PathVariable UUID id,
                                                @Valid @RequestBody FileAppealReq req,
                                                Authentication auth) {
        AppealDecision d = service.fileAppeal(id, auth.getName(), req.outcome());
        return ResponseEntity.status(HttpStatus.CREATED)
            .location(URI.create("/api/appeals/" + d.getId()))
            .body(AppealDto.of(d));
    }

    @GetMapping("/api/appeals/{id}")
    public AppealDto get(@PathVariable UUID id) {
        return AppealDto.of(service.get(id));
    }

    @GetMapping("/api/appeals/chain/{chainRootId}")
    public List<AppealDto> chain(@PathVariable UUID chainRootId) {
        return service.chain(chainRootId).stream().map(AppealDto::of).toList();
    }

    @ExceptionHandler(AppealException.class)
    public ResponseEntity<ProblemDetail> handle(AppealException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }

    /**
     * APPEAL-CHAIN-001 TOCTOU backstop — {@link AppealService#fileAppeal} checks
     * {@code findByParentDecisionId} BEFORE inserting, but two concurrent appeals against the
     * SAME decision can both pass that check and race to insert; the {@code uq_appeal_parent_decision}
     * UNIQUE constraint lets exactly one commit and the loser raises this exception at flush —
     * without this handler it would surface as an unhandled 500 despite the DB backstop working
     * correctly. Maps to the SAME 409 code the pre-check already returns for the sequential case.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleConcurrentDoubleAppeal(DataIntegrityViolationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
            "This decision already has an appeal filed against it");
        pd.setType(URI.create("urn:problem:appeal-already-filed"));
        pd.setProperty("code", "APPEAL_ALREADY_FILED");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }
}
