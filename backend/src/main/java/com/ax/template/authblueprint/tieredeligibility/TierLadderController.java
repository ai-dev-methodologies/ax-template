package com.ax.template.authblueprint.tieredeligibility;

import com.ax.template.authblueprint.common.PageEnvelope;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
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

/**
 * tiered-eligibility-l0 thin controller. Any authenticated caller may create a ladder, post accruals,
 * request a use, and issue an explicit restore. The accrual/restore response carries the tier reached so
 * the caller learns a change immediately (TIER-LADDER-001). Delegates to {@link TierLadderService}.
 */
@RestController
public class TierLadderController {

    public record CreateReq(@NotBlank @Size(max = 200) String ladderKey,
                            @NotEmpty List<@NotBlank String> tierNames,
                            @NotEmpty List<Integer> thresholds,
                            @PositiveOrZero int initialCount) {}
    public record AccrueReq(int delta) {}
    public record RestoreReq(@PositiveOrZero int newCount, @NotBlank String reason) {}

    public record LadderDto(String ladderKey, List<String> tierNames, List<Integer> thresholds,
                            int count, int currentTierIndex, String currentTierName, Long version) {
        static LadderDto of(TierLadder l) {
            List<String> names = l.getTiers().stream().map(TierDefinition::getName).toList();
            List<Integer> thresholds = l.getTiers().stream().skip(1)
                .map(TierDefinition::getEnterAtCount).toList();
            return new LadderDto(l.getLadderKey(), names, thresholds, l.getCount(), l.getCurrentTierIndex(),
                l.getCurrentTierName(), l.getVersion());
        }
    }
    public record AccrualDto(int delta, int countAfter, int tierIndexAfter, long sequenceNo, Instant recordedAt) {
        static AccrualDto of(TierAccrual a) {
            return new AccrualDto(a.getDelta(), a.getCountAfter(), a.getTierIndexAfter(), a.getSequenceNo(),
                a.getRecordedAt());
        }
    }
    public record RestoreDto(int countAfter, int tierIndexAfter, String reason, long sequenceNo,
                             Instant recordedAt) {
        static RestoreDto of(TierRestoreEvent r) {
            return new RestoreDto(r.getCountAfter(), r.getTierIndexAfter(), r.getReason(), r.getSequenceNo(),
                r.getRecordedAt());
        }
    }

    private final TierLadderService service;

    public TierLadderController(TierLadderService service) {
        this.service = service;
    }

    @PostMapping("/api/tiered-eligibility/ladders")
    public ResponseEntity<LadderDto> create(@Valid @RequestBody CreateReq req) {
        TierLadder l = service.createLadder(req.ladderKey(), req.tierNames(), req.thresholds(), req.initialCount());
        return ResponseEntity.status(HttpStatus.CREATED).body(LadderDto.of(l));
    }

    /** TIER-LADDER-001 — the response exposes the tier reached, including a multi-boundary jump. */
    @PostMapping("/api/tiered-eligibility/ladders/{ladderKey}/accruals")
    public LadderDto accrue(@PathVariable String ladderKey, @RequestBody AccrueReq req) {
        return LadderDto.of(service.accrue(ladderKey, req.delta()));
    }

    /** TIER-DERIVE-001 — the derived capability (dispatch / issue / transact). */
    @PostMapping("/api/tiered-eligibility/ladders/{ladderKey}/use")
    public LadderDto use(@PathVariable String ladderKey) {
        return LadderDto.of(service.use(ladderKey));
    }

    /** TIER-MONOTONE-001 — the ONLY explicit, audited path back to a better tier. */
    @PostMapping("/api/tiered-eligibility/ladders/{ladderKey}/restore")
    public LadderDto restore(@PathVariable String ladderKey, @Valid @RequestBody RestoreReq req) {
        return LadderDto.of(service.restore(ladderKey, req.newCount(), req.reason()));
    }

    @GetMapping("/api/tiered-eligibility/ladders/{ladderKey}")
    public LadderDto get(@PathVariable String ladderKey) {
        return LadderDto.of(service.getLadder(ladderKey));
    }

    @GetMapping("/api/tiered-eligibility/ladders/{ladderKey}/accruals")
    public PageEnvelope<AccrualDto> accruals(@PathVariable String ladderKey,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "100") int size) {
        return PageEnvelope.from(service.listAccruals(ladderKey, page, size), AccrualDto::of);
    }

    @GetMapping("/api/tiered-eligibility/ladders/{ladderKey}/restores")
    public PageEnvelope<RestoreDto> restores(@PathVariable String ladderKey,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "100") int size) {
        return PageEnvelope.from(service.listRestores(ladderKey, page, size), RestoreDto::of);
    }

    @ExceptionHandler(TierException.class)
    public ResponseEntity<ProblemDetail> handle(TierException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
