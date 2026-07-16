package com.ax.template.authblueprint.idempotency;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * idempotency-l0 reference workload — a state-changing resource surface that honors the
 * {@code Idempotency-Key} contract end to end:
 *
 * <ul>
 *   <li>POST with no key → no dedup (default behavior, SCOPE-001);</li>
 *   <li>POST with an invalid key → 400 (KEY-001);</li>
 *   <li>POST with a valid key → first-seen 201 (cached), replay of an identical request returns the
 *       cached body verbatim, a same-key/different-payload request → 422, an in-flight duplicate →
 *       409 + Retry-After (DEDUP-001 / CONCURRENT-001 / PAYLOAD-001);</li>
 *   <li>GET with a key → 400 (SCOPE-001 — inherently-idempotent method).</li>
 * </ul>
 *
 * Tenant = the authenticated principal (per-tenant key isolation, CACHE-001).
 * Spec: specs/idempotency-l0.yaml.
 */
@RestController
@RequestMapping("/api/idempotency-demo")
public class IdempotencyDemoController {

    private static final String PATH = "/api/idempotency-demo/resources";
    private static final URI CONFLICT_TYPE = URI.create("https://errors.example.com/idempotency-conflict");
    private static final URI MISMATCH_TYPE = URI.create("https://errors.example.com/idempotency-key-reused");

    /**
     * Simulated processing latency so a concurrent duplicate reliably observes the IN_FLIGHT slot
     * (CONCURRENT-001 "10 parallel → 1 success + 9 conflicts"). A real handler's own work provides
     * this window; the reference makes it explicit + deterministic.
     */
    static final long WORK_LATENCY_MS = 200;

    /**
     * Reject an oversized request body BEFORE it reaches {@link RequestFingerprint} (which parses +
     * reserializes it). Matches the standalone mapper's 20MB read cap so a huge body never triggers a
     * large tree/string allocation. Response-amplification defense; the rejection never echoes the body.
     */
    static final int MAX_BODY_CHARS = 20_000_000;

    private static final URI PAYLOAD_TOO_LARGE_TYPE = URI.create("https://errors.example.com/request-body-too-large");

    private final IdempotencyReplayService service;

    public IdempotencyDemoController(IdempotencyReplayService service) {
        this.service = service;
    }

    @PostMapping("/resources")
    public ResponseEntity<?> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @RequestBody(required = false) String body,
            Authentication auth) {
        if (body != null && body.length() > MAX_BODY_CHARS) {
            return payloadTooLarge();                                    // reject before fingerprint parse
        }
        if (key == null || key.isBlank()) {
            return resource(createResource(body), false, 201);          // SCOPE-001: absent → no dedup
        }
        if (!IdempotencyKeyValidator.isValid(key)) {
            throw new IdempotencyKeyInvalidException();                  // KEY-001
        }
        String fingerprint = RequestFingerprint.of("POST", PATH, "", body);
        IdempotencyReplayService.Outcome o =
                service.process(auth.getName(), key, fingerprint, () -> createResource(body));
        return switch (o.label()) {
            case "first_seen" -> resource(new IdempotencyReplayService.Result(o.status(), o.body()), false, 201);
            case "replayed" -> resource(new IdempotencyReplayService.Result(o.status(), o.body()), true, o.status());
            case "conflict" -> conflict();
            default -> mismatch();                                       // fingerprint_mismatch
        };
    }

    @GetMapping("/resources")
    public ResponseEntity<?> list(@RequestHeader(value = "Idempotency-Key", required = false) String key) {
        if (key != null) {
            throw new IdempotencyKeyNotAllowedException();               // SCOPE-001: key on GET → 400
        }
        return ResponseEntity.ok(Map.of("items", List.of()));
    }

    private IdempotencyReplayService.Result createResource(String body) {
        try {
            Thread.sleep(WORK_LATENCY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String id = UUID.randomUUID().toString();
        return new IdempotencyReplayService.Result(201, "{\"id\":\"" + id + "\",\"created\":true}");
    }

    private ResponseEntity<String> resource(IdempotencyReplayService.Result r, boolean replayed, int status) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Replayed", Boolean.toString(replayed))
                .body(r.body());
    }

    private ResponseEntity<ProblemDetail> conflict() {
        ProblemDetail pd = problem(HttpStatus.CONFLICT, CONFLICT_TYPE, "Idempotency Conflict",
                "IDEMPOTENCY_KEY_CONFLICT",
                "A request with this Idempotency-Key is still being processed; retry shortly.");
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .header(HttpHeaders.RETRY_AFTER, "1")
                .body(pd);
    }

    private ResponseEntity<ProblemDetail> payloadTooLarge() {
        // Never echo the (oversized) body back in the error detail.
        ProblemDetail pd = problem(HttpStatus.PAYLOAD_TOO_LARGE, PAYLOAD_TOO_LARGE_TYPE, "Request body too large",
                "REQUEST_BODY_TOO_LARGE",
                "The request body exceeds the maximum allowed size.");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    private ResponseEntity<ProblemDetail> mismatch() {
        ProblemDetail pd = problem(HttpStatus.UNPROCESSABLE_ENTITY, MISMATCH_TYPE, "Idempotency Key Reused",
                "IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD",
                "This Idempotency-Key was already used with a different request payload.");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    private static ProblemDetail problem(HttpStatus status, URI type, String title, String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(type);
        pd.setTitle(title);
        pd.setProperty("code", code);
        return pd;
    }
}
