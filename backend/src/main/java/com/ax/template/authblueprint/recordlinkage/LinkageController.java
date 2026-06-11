package com.ax.template.authblueprint.recordlinkage;

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

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * record-linkage-l0 thin controller. The proposer/decider is ALWAYS the authenticated caller
 * (caller-authentication-only-no-userid-param). Delegates to {@link LinkageService}.
 */
@RestController
public class LinkageController {

    public record CreateRecordReq(@NotBlank @Size(max = 200) String fullName,
                                  LocalDate birthDate,
                                  @Size(max = 100) String identifier) {}
    public record ProposeReq(@NotNull UUID recordAId, @NotNull UUID recordBId) {}

    public record RecordDto(UUID id, String fullName, LocalDate birthDate, String identifier,
                            RecordStatus status, UUID mergedIntoId) {
        static RecordDto of(LinkageRecord r) {
            return new RecordDto(r.getId(), r.getFullName(), r.getBirthDate(), r.getIdentifier(),
                r.getStatus(), r.getMergedIntoId());
        }
    }
    public record ProposalDto(UUID id, UUID lowRecordId, UUID highRecordId, BigDecimal score,
                              String breakdownJson, BigDecimal lowerThreshold, BigDecimal upperThreshold,
                              MatchBand band, ProposalStatus status, String decidedBy, Instant decidedAt) {
        static ProposalDto of(MatchProposal p) {
            return new ProposalDto(p.getId(), p.getLowRecordId(), p.getHighRecordId(), p.getScore(),
                p.getBreakdownJson(), p.getLowerThreshold(), p.getUpperThreshold(), p.getBand(),
                p.getStatus(), p.getDecidedBy(), p.getDecidedAt());
        }
    }
    public record DecisionDto(String fieldName, String winningValue, UUID sourceRecordId,
                              String ruleApplied, Instant decidedAt) {
        static DecisionDto of(SurvivorshipDecision d) {
            return new DecisionDto(d.getFieldName(), d.getWinningValue(), d.getSourceRecordId(),
                d.getRuleApplied(), d.getDecidedAt());
        }
    }

    private final LinkageService service;

    public LinkageController(LinkageService service) {
        this.service = service;
    }

    @PostMapping("/api/linkage/records")
    public ResponseEntity<RecordDto> createRecord(@Valid @RequestBody CreateRecordReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(RecordDto.of(service.createRecord(req.fullName(), req.birthDate(), req.identifier())));
    }

    @PostMapping("/api/linkage/proposals")
    public ResponseEntity<ProposalDto> propose(@Valid @RequestBody ProposeReq req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ProposalDto.of(service.propose(req.recordAId(), req.recordBId(), auth.getName())));
    }

    @PostMapping("/api/linkage/proposals/{id}/confirm")
    public ProposalDto confirm(@PathVariable UUID id, Authentication auth) {
        return ProposalDto.of(service.confirm(id, auth.getName()));
    }

    @PostMapping("/api/linkage/proposals/{id}/reject")
    public ProposalDto reject(@PathVariable UUID id, Authentication auth) {
        return ProposalDto.of(service.reject(id, auth.getName()));
    }

    @GetMapping("/api/linkage/records/{id}")
    public RecordDto get(@PathVariable UUID id) {
        return RecordDto.of(service.get(id));
    }

    /** LINK-RESOLVE-001 — follow the merged-into chain to the living survivor. */
    @GetMapping("/api/linkage/records/{id}/resolve")
    public RecordDto resolve(@PathVariable UUID id) {
        return RecordDto.of(service.resolve(id));
    }

    @GetMapping("/api/linkage/proposals/{id}")
    public ProposalDto getProposal(@PathVariable UUID id) {
        return ProposalDto.of(service.getProposal(id));
    }

    @GetMapping("/api/linkage/proposals/{id}/decisions")
    public List<DecisionDto> decisions(@PathVariable UUID id) {
        return service.decisions(id).stream().map(DecisionDto::of).toList();
    }

    @ExceptionHandler(LinkageException.class)
    public ResponseEntity<ProblemDetail> handle(LinkageException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
