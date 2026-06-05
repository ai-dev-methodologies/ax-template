package com.ax.template.authblueprint.optlock;

import com.ax.template.authblueprint.common.OptimisticLockingSupport;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * optimistic-locking-l0 reference workload — a mutable, owner-scoped resource served with strong
 * ETags and an If-Match precondition on every mutation:
 *
 * <ul>
 *   <li>GET → 200 + strong ETag derived from {@code @Version} (OPTLOCK-ETAG-001);</li>
 *   <li>PUT without If-Match → 428 (OPTLOCK-IFMATCH-001); with a stale validator → 412 +
 *       {@code current_etag} (OPTLOCK-CONFLICT-001 / -RETRY-001); a concurrent flush race → 409
 *       (OPTLOCK-CONFLICT-001 / -LOSTUPDATE-001).</li>
 * </ul>
 *
 * The 428/412/409 bodies are RFC 9457 problem+json via {@code common.GlobalProblemDetailAdvice};
 * this controller only records the bounded {@link OptlockMetrics} on each path. The version is
 * read-only (no writable DTO field). Spec: specs/optimistic-locking-l0.yaml.
 */
@RestController
@RequestMapping("/api/optlock/resources")
public class OptlockController {

    public record CreateRequest(String name, int quantity) {}
    public record UpdateRequest(String name, int quantity) {}

    private final OptlockService service;
    private final OptlockMetrics metrics;

    public OptlockController(OptlockService service, OptlockMetrics metrics) {
        this.service = service;
        this.metrics = metrics;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody CreateRequest req, Authentication auth) {
        OptlockResource r = service.create(auth.getName(), req.name(), req.quantity());
        return ResponseEntity.status(HttpStatus.CREATED).eTag(etag(r)).body(dto(r));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable UUID id, Authentication auth) {
        OptlockResource r = service.get(id, auth.getName());
        return ResponseEntity.ok().eTag(etag(r)).body(dto(r));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable UUID id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestBody UpdateRequest req,
            Authentication auth) {
        long start = System.nanoTime();
        try {
            OptlockResource r = service.update(id, auth.getName(), ifMatch, req.name(), req.quantity());
            metrics.write("applied", elapsed(start));
            return ResponseEntity.ok().eTag(etag(r)).body(dto(r));
        } catch (OptimisticLockingSupport.PreconditionRequiredException e) {
            metrics.preconditionRequired();                       // 428 (mapped by global advice)
            throw e;
        } catch (OptimisticLockingSupport.PreconditionFailedException e) {
            metrics.conflict("precondition_failed");              // 412
            metrics.write("conflict", elapsed(start));
            throw e;
        } catch (ObjectOptimisticLockingFailureException e) {
            metrics.conflict("lock_conflict");                    // 409
            metrics.write("conflict", elapsed(start));
            throw e;
        }
    }

    private static String etag(OptlockResource r) {
        // OPTLOCK-ETAG-001 3-part format "<entityType>-<id>-<version>": the entityType is folded
        // into the resourceId passed to the support helper (which appends "-<version>").
        return OptimisticLockingSupport.etag(OptlockService.resourceKey(r.getId()), r.getVersion());
    }

    private static Map<String, Object> dto(OptlockResource r) {
        return Map.of("id", r.getId().toString(), "name", r.getName(),
                "quantity", r.getQuantity(), "version", r.getVersion());
    }

    private static Duration elapsed(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos);
    }
}
