package com.ax.template.authblueprint.quorumresolution;

import com.ax.template.authblueprint.common.PageEnvelope;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * quorum-resolution-l0 thin controller. The acting identity (caller) is ALWAYS
 * Authentication.getName() — never from body or path. Delegates to {@link QuorumService}.
 */
@RestController
public class QuorumController {

    // ── Request DTOs ────────────────────────────────────────────────────────

    public record VoterEntryReq(@NotBlank @Size(max = 200) String voterId, @Positive long weight) {}

    public record PolicyReq(
        @NotNull RuleType ruleType,
        @Min(0) long thresholdNumerator,
        @Positive long thresholdDenominator,
        @Min(0) long quorumNumerator,
        @Positive long quorumDenominator,
        @NotNull AbstentionMode abstentionMode,
        @NotNull TieBreakMode tieBreakMode,
        @Size(max = 200) String tieBreakVoterId
    ) {}

    public record OpenMotionReq(@Valid @NotNull PolicyReq policy,
                                 @NotEmpty @Valid List<VoterEntryReq> roster) {}

    public record CastBallotReq(@NotNull Choice choice) {}

    // ── Response DTOs ────────────────────────────────────────────────────────

    public record MotionDto(UUID id, String convenerId, MotionStatus status,
                             long totalEligibleWeight, RuleType ruleType,
                             long thresholdNumerator, long thresholdDenominator,
                             long quorumNumerator, long quorumDenominator,
                             AbstentionMode abstentionMode, TieBreakMode tieBreakMode,
                             String tieBreakVoterId, Instant createdAt) {
        static MotionDto of(Motion m) {
            return new MotionDto(m.getId(), m.getConvenerId(), m.getStatus(),
                m.getTotalEligibleWeight(), m.getRuleType(),
                m.getThresholdNumerator(), m.getThresholdDenominator(),
                m.getQuorumNumerator(), m.getQuorumDenominator(),
                m.getAbstentionMode(), m.getTieBreakMode(), m.getTieBreakVoterId(),
                m.getCreatedAt());
        }
    }

    public record BallotDto(UUID id, UUID motionId, String voterId, Choice choice,
                             long weightAtCast, Instant castAt) {
        static BallotDto of(Ballot b) {
            return new BallotDto(b.getId(), b.getMotionId(), b.getVoterId(),
                b.getChoice(), b.getWeightAtCast(), b.getCastAt());
        }
    }

    public record ResolutionDto(UUID id, UUID motionId, Outcome outcome,
                                 long yesWeight, long noWeight, long abstainWeight,
                                 long castEligibleWeight, long totalEligibleWeight,
                                 Instant resolvedAt) {
        static ResolutionDto of(Resolution r) {
            return new ResolutionDto(r.getId(), r.getMotionId(), r.getOutcome(),
                r.getYesWeight(), r.getNoWeight(), r.getAbstainWeight(),
                r.getCastEligibleWeight(), r.getTotalEligibleWeight(), r.getResolvedAt());
        }
    }

    private final QuorumService service;

    public QuorumController(QuorumService service) {
        this.service = service;
    }

    @PostMapping("/api/quorum/motions")
    public ResponseEntity<MotionDto> openMotion(@Valid @RequestBody OpenMotionReq req,
                                                 Authentication auth) {
        PolicyReq p = req.policy();
        QuorumService.PolicySnapshot policy = new QuorumService.PolicySnapshot(
            p.ruleType(), p.thresholdNumerator(), p.thresholdDenominator(),
            p.quorumNumerator(), p.quorumDenominator(),
            p.abstentionMode(), p.tieBreakMode(), p.tieBreakVoterId());

        List<QuorumService.VoterEntry> roster = req.roster().stream()
            .map(e -> new QuorumService.VoterEntry(e.voterId(), e.weight()))
            .toList();

        Motion motion = service.openMotion(auth.getName(), policy, roster);
        return ResponseEntity.status(HttpStatus.CREATED).body(MotionDto.of(motion));
    }

    @PostMapping("/api/quorum/motions/{id}/ballots")
    public ResponseEntity<BallotDto> castBallot(@PathVariable UUID id,
                                                 @Valid @RequestBody CastBallotReq req,
                                                 Authentication auth) {
        Ballot ballot = service.castBallot(id, auth.getName(), req.choice());
        return ResponseEntity.status(HttpStatus.CREATED).body(BallotDto.of(ballot));
    }

    @PostMapping("/api/quorum/motions/{id}/resolve")
    public ResponseEntity<ResolutionDto> resolve(@PathVariable UUID id, Authentication auth) {
        Resolution resolution = service.resolve(id, auth.getName());
        return ResponseEntity.ok(ResolutionDto.of(resolution));
    }

    @GetMapping("/api/quorum/motions/{id}")
    public MotionDto getMotion(@PathVariable UUID id) {
        return MotionDto.of(service.getMotion(id));
    }

    @GetMapping("/api/quorum/motions/{id}/ballots")
    public PageEnvelope<BallotDto> getBallots(@PathVariable UUID id,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               Authentication auth) {
        // Per-voter ballots (who voted what) are confidential in board/HOA/committee votes —
        // only the convener may read the ballot list; everyone else gets an IDOR-safe 404.
        return PageEnvelope.from(service.getBallots(id, auth.getName(), page, size), BallotDto::of);
    }

    @ExceptionHandler(QuorumException.class)
    public ResponseEntity<ProblemDetail> handle(QuorumException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
