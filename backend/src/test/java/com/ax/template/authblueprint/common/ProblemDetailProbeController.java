package com.ax.template.authblueprint.common;

import org.springframework.web.bind.annotation.GetMapping;
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
}
