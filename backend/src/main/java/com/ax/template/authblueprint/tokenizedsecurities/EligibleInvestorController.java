package com.ax.template.authblueprint.tokenizedsecurities;

import java.net.URI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EligibleInvestorController {

    public record GrantReq(@NotBlank @Size(max = 200) String holderId) {}
    public record GrantDto(String tokenCode, String holderId) {}

    private final EligibleInvestorService service;

    public EligibleInvestorController(EligibleInvestorService service) { this.service = service; }

    @PostMapping("/api/security-tokens/{tokenCode}/eligible-investors")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<GrantDto> grant(@PathVariable String tokenCode, @Valid @RequestBody GrantReq req) {
        EligibleInvestor g = service.grant(tokenCode, req.holderId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new GrantDto(tokenCode, g.getHolderId()));
    }

    @ExceptionHandler(TokenizedSecuritiesException.class)
    public ResponseEntity<ProblemDetail> handle(TokenizedSecuritiesException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
