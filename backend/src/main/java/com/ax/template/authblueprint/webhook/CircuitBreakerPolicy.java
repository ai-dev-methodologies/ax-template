package com.ax.template.authblueprint.webhook;

import com.ax.template.authblueprint.auditlog.AuditLog;
import com.ax.template.authblueprint.auditlog.AuditLogService;
import com.ax.template.authblueprint.auditlog.AuditOutcome;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Rolling failure-rate circuit breaker per endpoint.
 * <p>
 * Trace:
 * <ul>
 *   <li>WEBHOOK-CIRCUIT-001 — when an endpoint's last {@link #WINDOW_SIZE}
 *       deliveries hit a terminal state with ≥ {@link #FAILURE_THRESHOLD}
 *       failure ratio, the endpoint is auto-deactivated AND an audit-log row
 *       is emitted with {@code action=webhook.endpoint.circuit_opened}.</li>
 *   <li>blueprints/webhook-manifest.yaml#circuit-breaker</li>
 * </ul>
 *
 * <p>Only terminal-state rows ({@link WebhookDeliveryStatus#SUCCEEDED} and
 * {@link WebhookDeliveryStatus#FAILED_PERMANENT}) are counted — in-flight
 * {@code PENDING_RETRY} rows don't yet have a verdict.
 */
@Component
public class CircuitBreakerPolicy {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerPolicy.class);

    /** Rolling-window size from blueprints/webhook-manifest.yaml#circuit-breaker. */
    public static final int WINDOW_SIZE = 50;
    /** ≥ 90% failure rate over the window opens the circuit. */
    public static final double FAILURE_THRESHOLD = 0.90;
    /** Audit action emitted when the circuit opens. */
    public static final String AUDIT_ACTION_CIRCUIT_OPENED = "webhook.endpoint.circuit_opened";

    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookEndpointRepository endpointRepository;
    private final AuditLogService auditLogService;

    public CircuitBreakerPolicy(WebhookDeliveryRepository deliveryRepository,
                                WebhookEndpointRepository endpointRepository,
                                AuditLogService auditLogService) {
        this.deliveryRepository = deliveryRepository;
        this.endpointRepository = endpointRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Evaluate the endpoint's recent terminal-state history; deactivate + audit
     * if the failure rate crosses the threshold.
     */
    public void evaluate(UUID endpointId) {
        List<WebhookDelivery> recent = deliveryRepository
            .findByEndpointIdOrderByCreatedAtDesc(endpointId, PageRequest.of(0, WINDOW_SIZE));

        int terminal = 0;
        int failed = 0;
        for (WebhookDelivery d : recent) {
            if (d.getStatus() == WebhookDeliveryStatus.SUCCEEDED
                || d.getStatus() == WebhookDeliveryStatus.FAILED_PERMANENT) {
                terminal++;
                if (d.getStatus() == WebhookDeliveryStatus.FAILED_PERMANENT) {
                    failed++;
                }
            }
        }
        if (terminal < WINDOW_SIZE) {
            // Not enough history yet — manifest says rolling window of 50.
            return;
        }
        final double rate = (double) failed / terminal;
        if (rate < FAILURE_THRESHOLD) {
            return;
        }
        final int totalTerminal = terminal;
        final int totalFailed = failed;
        endpointRepository.findById(endpointId).ifPresent(endpoint -> {
            if (!endpoint.isActive()) {
                return; // already open
            }
            endpoint.deactivate();
            endpointRepository.save(endpoint);
            log.error("webhook: circuit opened endpointId={} failureRate={} windowSize={}",
                endpointId, rate, totalTerminal);
            auditLogService.record(AuditLog.builder()
                .action(AUDIT_ACTION_CIRCUIT_OPENED)
                .resourceType("webhook_endpoint")
                .resourceId(endpointId.toString())
                .outcome(AuditOutcome.SUCCESS)
                .metadataJson(String.format(
                    "{\"failure_rate\":%.2f,\"window_size\":%d,\"failed\":%d}",
                    rate, totalTerminal, totalFailed))
                .build());
        });
    }
}
