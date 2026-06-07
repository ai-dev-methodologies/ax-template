package com.ax.template.authblueprint.dispatch;

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
import java.util.UUID;

/**
 * dispatch thin controller. Dispatcher surface lives under /api/admin/dispatch/** (gated to
 * ROLE_ADMIN by the /api/admin/** SecurityConfig rule); actor surface (create request / accept /
 * decline / heartbeat / cancel) is any authenticated user. Delegates to {@link DispatchService}
 * ONLY (no repository access). Domain errors → RFC 9457 ProblemDetail with a machine-readable
 * {@code code} (EXCL-409-004); @Valid 400s are handled by common/GlobalProblemDetailAdvice.
 */
@RestController
public class DispatchController {

    public record RegisterProviderRequest(@NotBlank @Size(max = 120) String handle) {}
    public record CreateRequestRequest(@NotBlank @Size(max = 500) String description) {}
    public record OfferRequest(@NotNull UUID requestId, @NotNull UUID providerId) {}

    public record ProviderDto(UUID id, String handle, ProviderStatus status,
                              Instant lastHeartbeatAt, Instant createdAt) {
        static ProviderDto of(Provider p) {
            return new ProviderDto(p.getId(), p.getHandle(), p.getStatus(), p.getLastHeartbeatAt(), p.getCreatedAt());
        }
    }

    public record RequestDto(UUID id, String description, ServiceRequestStatus status,
                             UUID assignedProviderId, String createdBy, Instant createdAt) {
        static RequestDto of(ServiceRequest r) {
            return new RequestDto(r.getId(), r.getDescription(), r.getStatus(),
                r.getAssignedProviderId(), r.getCreatedBy(), r.getCreatedAt());
        }
    }

    public record OfferDto(UUID id, UUID requestId, UUID providerId, OfferStatus status,
                           Instant expiresAt, int ordinal) {
        static OfferDto of(Offer o) {
            return new OfferDto(o.getId(), o.getRequestId(), o.getProviderId(), o.getStatus(),
                o.getExpiresAt(), o.getOrdinal());
        }
    }

    private final DispatchService service;

    public DispatchController(DispatchService service) {
        this.service = service;
    }

    // ── dispatcher surface (/api/admin/dispatch/** → ROLE_ADMIN) ────────────────
    @PostMapping("/api/admin/dispatch/providers")
    public ResponseEntity<ProviderDto> register(@Valid @RequestBody RegisterProviderRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ProviderDto.of(service.registerProvider(req.handle())));
    }

    @PostMapping("/api/admin/dispatch/offers")
    public ResponseEntity<OfferDto> offer(@Valid @RequestBody OfferRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(OfferDto.of(service.offer(req.requestId(), req.providerId())));
    }

    // ── actor surface (/api/dispatch/** → authenticated) ────────────────────────
    @PostMapping("/api/dispatch/requests")
    public ResponseEntity<RequestDto> createRequest(@Valid @RequestBody CreateRequestRequest req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(RequestDto.of(service.createRequest(auth.getName(), req.description())));
    }

    @PostMapping("/api/dispatch/requests/{id}/cancel")
    public RequestDto cancelRequest(@PathVariable UUID id) {
        return RequestDto.of(service.cancelRequest(id));
    }

    @PostMapping("/api/dispatch/providers/{id}/heartbeat")
    public ProviderDto heartbeat(@PathVariable UUID id) {
        return ProviderDto.of(service.heartbeat(id));
    }

    @PostMapping("/api/dispatch/offers/{id}/accept")
    public OfferDto accept(@PathVariable UUID id) {
        return OfferDto.of(service.acceptOffer(id));
    }

    @PostMapping("/api/dispatch/offers/{id}/decline")
    public OfferDto decline(@PathVariable UUID id) {
        return OfferDto.of(service.declineOffer(id));
    }

    @GetMapping("/api/dispatch/requests/{id}")
    public RequestDto getRequest(@PathVariable UUID id) {
        return RequestDto.of(service.getRequest(id));
    }

    @GetMapping("/api/dispatch/providers/{id}")
    public ProviderDto getProvider(@PathVariable UUID id) {
        return ProviderDto.of(service.getProvider(id));
    }

    @GetMapping("/api/dispatch/offers/{id}")
    public OfferDto getOffer(@PathVariable UUID id) {
        return OfferDto.of(service.getOffer(id));
    }

    @GetMapping("/api/dispatch/requests/{id}/current-offer")
    public OfferDto currentOffer(@PathVariable UUID id) {
        return OfferDto.of(service.currentPendingOffer(id));
    }

    // ── domain exception -> RFC 9457 ProblemDetail (with machine-readable code) ──
    @ExceptionHandler(DispatchException.class)
    public ResponseEntity<ProblemDetail> handle(DispatchException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());   // EXCL-409-004 — distinct race codes
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
