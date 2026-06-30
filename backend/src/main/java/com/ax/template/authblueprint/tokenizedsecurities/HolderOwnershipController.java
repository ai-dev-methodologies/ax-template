package com.ax.template.authblueprint.tokenizedsecurities;

import java.net.URI;
import java.time.Instant;

import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
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

    /** Self-claim: bind holderId to the authenticated caller's principal (first-claim-wins). */
    @PostMapping("/api/security-tokens/holders/{holderId}/ownership")
    public ResponseEntity<OwnershipDto> claim(
            @PathVariable @Size(max = 200) String holderId,
            Authentication authentication) {
        HolderOwnership ownership = service.claim(holderId, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(OwnershipDto.of(ownership));
    }

    @ExceptionHandler(TokenizedSecuritiesException.class)
    public ResponseEntity<ProblemDetail> handle(TokenizedSecuritiesException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
