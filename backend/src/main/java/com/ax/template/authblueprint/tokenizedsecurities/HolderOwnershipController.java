package com.ax.template.authblueprint.tokenizedsecurities;

import java.net.URI;
import java.time.Instant;

import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HolderOwnershipController {

    public record OwnershipDto(String holderId, String ownerPrincipal, Instant claimedAt) {
        static OwnershipDto of(HolderOwnership o) {
            return new OwnershipDto(o.getHolderId(), o.getOwnerPrincipal(), o.getClaimedAt());
        }
    }

    private final HolderOwnershipService service;

    public HolderOwnershipController(HolderOwnershipService service) { this.service = service; }

    /**
     * READ-HOLDER-001: return the current owner of a holder (200) or 404 if unclaimed.
     * Any authenticated caller may read ownership state.
     */
    @GetMapping("/api/security-tokens/holders/{holderId}/owner")
    public ResponseEntity<OwnershipDto> getOwner(
            @PathVariable @Size(max = 200) String holderId) {
        return service.findOwner(holderId)
                .map(o -> ResponseEntity.ok(OwnershipDto.of(o)))
                .orElseThrow(TokenizedSecuritiesException::notFound);
    }

    /**
     * Self-claim: bind holderId to the authenticated caller's principal (first-claim-wins).
     * Returns 201 Created on first claim, 200 OK on idempotent re-claim by same principal
     * (HOLDER-AUTHZ-002 / F4 closure).
     */
    @PostMapping("/api/security-tokens/holders/{holderId}/ownership")
    public ResponseEntity<OwnershipDto> claim(
            @PathVariable @Size(max = 200) String holderId,
            Authentication authentication) {
        HolderOwnershipService.ClaimResult result = service.claim(holderId, authentication.getName());
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(OwnershipDto.of(result.ownership()));
    }

    @ExceptionHandler(TokenizedSecuritiesException.class)
    public ResponseEntity<ProblemDetail> handle(TokenizedSecuritiesException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
