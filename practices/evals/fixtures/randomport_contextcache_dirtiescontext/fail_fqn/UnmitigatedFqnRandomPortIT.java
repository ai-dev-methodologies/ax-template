package com.ax.template.authblueprint.fixtures;

import org.junit.jupiter.api.Test;

/**
 * FAIL fixture (FQN escape, 2026-07-20) — a real
 * {@code @org.springframework.boot.test.context.SpringBootTest(RANDOM_PORT)}
 * class that NAMES the ContextCache hazard (this comment) but carries NO
 * {@code @DirtiesContext} to mitigate it. The annotation is used in its
 * FULLY-QUALIFIED form (no import, no bare `@SpringBootTest` substring in the
 * source) — prior to the FQN-tolerant regex fix this class was invisible to
 * the guard's literal `"@SpringBootTest" not in code` substring check, even
 * though it is exactly as exposed to the R22 ContextCache cap-32 eviction
 * hazard as a class using the bare annotation. Must still exit 1.
 */
@org.springframework.boot.test.context.SpringBootTest(
        webEnvironment = org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT)
class UnmitigatedFqnRandomPortIT {

    @Test
    void hitsRealHttpSurface() {
        // deliberately missing @DirtiesContext — ContextCache eviction hazard,
        // even though @SpringBootTest is spelled out fully-qualified here.
    }
}
