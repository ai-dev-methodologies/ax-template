package com.ax.template.authblueprint.identityclaim;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

import com.ax.template.authblueprint.identityclaim.IdentityClaimDtos.AddRecordRequest;
import com.ax.template.authblueprint.identityclaim.IdentityClaimDtos.ClaimRequest;
import com.ax.template.authblueprint.identityclaim.IdentityClaimDtos.ClaimResult;
import com.ax.template.authblueprint.identityclaim.IdentityClaimDtos.RecordResponse;

@RestController
@RequestMapping("/api/identity-claim")
public class IdentityClaimController {

    private final IdentityClaimService service;

    public IdentityClaimController(IdentityClaimService service) {
        this.service = service;
    }

    /**
     * POST /api/identity-claim/records — creates an anonymous record (ownerUserId=null).
     * Returns 201.
     */
    @PostMapping("/records")
    public ResponseEntity<RecordResponse> addRecord(@Valid @RequestBody AddRecordRequest body,
                                                    Authentication auth) {
        ClaimableRecord record = service.addAnonymousRecord(body.claimKey(), body.label());
        RecordResponse resp = RecordResponse.from(record);
        return ResponseEntity.status(HttpStatus.CREATED)
            .location(URI.create("/api/identity-claim/records/" + resp.id()))
            .body(resp);
    }

    /**
     * POST /api/identity-claim/claim — claims all unclaimed records for claimKey,
     * using the authenticated principal as the userId.
     * Returns 200 ClaimResult(claimedCount).
     * IDCLAIM-IDEMPOTENT-001: replays return claimedCount=0.
     * IDCLAIM-GUARD-001: records already owned by another user are never transferred.
     */
    @PostMapping("/claim")
    public ClaimResult claim(@Valid @RequestBody ClaimRequest body, Authentication auth) {
        String userId = auth.getName();
        return service.claimOnFirstAuth(body.claimKey(), userId);
    }

    /**
     * GET /api/identity-claim/records?claimKey=... — returns records for the given claimKey
     * that are either unclaimed (ownerUserId IS NULL) or owned by the authenticated caller.
     *
     * <p>Scoping rule — caller sees only:
     * <ul>
     *   <li>Records not yet claimed by anyone (ownerUserId == null) — visible pre-claim.</li>
     *   <li>Records the caller themselves own (ownerUserId.equals(caller)) — visible post-claim.</li>
     * </ul>
     * Records owned by a DIFFERENT user are filtered out, consistent with the domain's
     * cross-principal leak-prevention thesis (IDCLAIM-GUARD-001).
     */
    @GetMapping("/records")
    public List<RecordResponse> getByClaimKey(@RequestParam String claimKey, Authentication auth) {
        return service.recordsVisibleTo(claimKey, auth.getName()).stream()
            .map(RecordResponse::from)
            .toList();
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "validation failed");
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setProperty("code", code);
        return ResponseEntity.status(status).body(pd);
    }
}
