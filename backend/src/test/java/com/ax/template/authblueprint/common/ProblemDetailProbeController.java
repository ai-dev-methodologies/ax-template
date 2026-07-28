package com.ax.template.authblueprint.common;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * TEST-ONLY probe controller proving the two IDW3 COMMON handlers in
 * {@link GlobalProblemDetailAdvice} behave end-to-end behind the reference
 * {@code SecurityConfig}.
 *
 * <p>Mounted under {@code /api/items/...} so it inherits the existing
 * {@code /api/items/** → authenticated()} rule without touching {@code SecurityConfig}.
 * Both endpoints are reachable only with a valid JWT, so the assertions exercise the
 * REAL filter chain (the very chain whose {@code anyRequest().denyAll()} is what turns
 * an unmapped 404/400 into a misleading 403). It ships NO local {@code @ExceptionHandler},
 * so a request that throws here falls through to the {@code LOWEST_PRECEDENCE}
 * {@link GlobalProblemDetailAdvice} fallback — exactly what we want to prove.
 *
 * <p>Registered via {@code @Import} from {@link GlobalProblemDetailProbeTest}; it is a
 * {@code @Bean}-registered {@code @RestController}, so Spring MVC's
 * {@code RequestMappingHandlerMapping} maps its routes.
 */
@RestController
@RequestMapping("/api/items/probe")
public class ProblemDetailProbeController {

    /** Throws the COMMON IDOR-safe 404 → must surface as 404 problem+json, NOT 403. */
    @GetMapping("/not-found")
    public String notFound() {
        throw new ResourceNotFoundException("probe resource not found");
    }

    /**
     * Calls {@link OffsetPageSupport#clamp(int, int, int)} with an out-of-range size →
     * {@link InvalidPageRequestException} → must surface as 400 PAGE_SIZE_INVALID.
     */
    @GetMapping("/page")
    public String page(@RequestParam(defaultValue = "999") int size) {
        OffsetPageSupport.clamp(0, size, OffsetPageSupport.DEFAULT_MAX_PAGE_SIZE);
        return "unreachable";
    }

    /**
     * P2-41 — a REQUIRED {@code @RequestParam} with no default. Calling this endpoint
     * WITHOUT {@code ?ref=} raises {@code MissingServletRequestParameterException}
     * during argument resolution, which (before the handler was added) fell through
     * to {@code /error} and surfaced as an EMPTY 403 behind the reference
     * {@code SecurityConfig}. It must now surface as 400 MISSING_PARAMETER.
     */
    @GetMapping("/required-param")
    public String requiredParam(@RequestParam String ref) {
        return ref;
    }

    /**
     * Conditional write through {@link OptimisticLockingSupport#requireMatch(String, String, long)}.
     * Probes the three IMW4 global mappings the COMMON
     * {@link GlobalProblemDetailAdvice} now owns (no local {@code @ExceptionHandler}):
     * <ul>
     *   <li>no {@code If-Match} header → {@code PreconditionRequiredException} → 428;</li>
     *   <li>a stale validator (anything other than the current {@code "7-3"}) →
     *       {@code PreconditionFailedException} → 412 (with {@code current_etag});</li>
     *   <li>{@code If-Match: race} → simulate the concurrent-write race that surfaces at
     *       flush time after the precondition check passed → 409.</li>
     * </ul>
     * The current strong validator is the deterministic {@code "7-3"}
     * ({@code etag("7", 3)}); a matching {@code If-Match} therefore proceeds to the write.
     */
    @PutMapping("/optlock")
    public String optlock(@RequestHeader(name = "If-Match", required = false) String ifMatch) {
        if ("race".equals(ifMatch)) {
            throw new ObjectOptimisticLockingFailureException(Object.class, "7");
        }
        OptimisticLockingSupport.requireMatch(ifMatch, "7", 3L);
        return "written";
    }
}
