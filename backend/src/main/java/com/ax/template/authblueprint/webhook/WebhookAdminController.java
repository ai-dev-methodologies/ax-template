package com.ax.template.authblueprint.webhook;

import com.ax.template.authblueprint.auditlog.Audited;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Admin REST surface for the webhook domain.
 * <p>
 * All endpoints sit under {@code /api/admin/**} — the global SecurityConfig
 * already enforces {@code ROLE_ADMIN} for that prefix. This controller ALSO
 * declares a class-level {@code @PreAuthorize("hasAuthority('ROLE_ADMIN')")} as
 * defense-in-depth (method security is the primary, locally-verifiable gate; the
 * path matcher stays as a complementary layer).
 * <p>
 * Trace:
 * <ul>
 *   <li>blueprints/webhook-manifest.yaml#admin_api — endpoint surface</li>
 *   <li>blueprints/webhook-manifest.yaml#authz — admin-only</li>
 *   <li>WEBHOOK-DEAD-LETTER-002 — {@code POST /webhook-deliveries/{id}/replay}</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class WebhookAdminController {

    public static final String NOT_FOUND_TYPE = "https://ax-template.dev/problems/webhook-not-found";

    private final WebhookEndpointService endpointService;
    private final WebhookAdminService adminService;

    public WebhookAdminController(WebhookEndpointService endpointService,
                                  WebhookAdminService adminService) {
        this.endpointService = endpointService;
        this.adminService = adminService;
    }

    // ─── endpoints ───────────────────────────────────────────────────────────

    @GetMapping("/webhook-endpoints")
    public List<WebhookDto.EndpointResponse> listEndpoints() {
        return endpointService.listAll().stream()
            .map(WebhookDto.EndpointResponse::from)
            .toList();
    }

    @PostMapping("/webhook-endpoints")
    @Audited(action = "REGISTER", resourceType = "webhook_endpoint")
    public WebhookDto.EndpointWithSecret register(@RequestBody WebhookDto.RegisterRequest req) {
        WebhookEndpoint endpoint = endpointService.register(req.url(), req.eventFilter());
        return WebhookDto.EndpointWithSecret.from(endpoint);
    }

    @GetMapping("/webhook-endpoints/{id}")
    public WebhookDto.EndpointResponse getEndpoint(@PathVariable UUID id) {
        WebhookEndpoint endpoint = endpointService.findById(id)
            .orElseThrow(() -> new WebhookEndpointNotFoundException(id));
        return WebhookDto.EndpointResponse.from(endpoint);
    }

    @DeleteMapping("/webhook-endpoints/{id}")
    @Audited(action = "DELETE", resourceType = "webhook_endpoint")
    public ResponseEntity<Void> deleteEndpoint(@PathVariable UUID id) {
        endpointService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ─── deliveries ──────────────────────────────────────────────────────────

    /**
     * Dead-letter listing — WEBHOOK-DEAD-LETTER-001 retains FAILED_PERMANENT
     * rows for admin inspection.
     */
    @GetMapping("/webhook-deliveries")
    public List<WebhookDto.DeliveryResponse> listDeliveries(
            @RequestParam(defaultValue = "FAILED_PERMANENT") WebhookDeliveryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<WebhookDelivery> rows = adminService.listDeliveries(status, page, size);
        return rows.stream().map(WebhookDto.DeliveryResponse::from).toList();
    }

    @GetMapping("/webhook-deliveries/{id}")
    public WebhookDto.DeliveryResponse getDelivery(@PathVariable UUID id) {
        WebhookDelivery delivery = adminService.getDelivery(id);
        return WebhookDto.DeliveryResponse.from(delivery);
    }

    @PostMapping("/webhook-deliveries/{id}/replay")
    @Audited(action = "REPLAY", resourceType = "webhook_delivery")
    public WebhookDto.DeliveryResponse replay(@PathVariable UUID id) {
        return WebhookDto.DeliveryResponse.from(adminService.replay(id));
    }

    // ─── error mapping ───────────────────────────────────────────────────────

    @ExceptionHandler(WebhookEndpointNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleEndpointNotFound(WebhookEndpointNotFoundException ex) {
        return notFound(ex.getMessage(), "Webhook endpoint not found");
    }

    @ExceptionHandler(WebhookDeliveryNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleDeliveryNotFound(WebhookDeliveryNotFoundException ex) {
        return notFound(ex.getMessage(), "Webhook delivery not found");
    }

    private ResponseEntity<ProblemDetail> notFound(String detail, String title) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, detail);
        pd.setType(URI.create(NOT_FOUND_TYPE));
        pd.setTitle(title);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }
}
