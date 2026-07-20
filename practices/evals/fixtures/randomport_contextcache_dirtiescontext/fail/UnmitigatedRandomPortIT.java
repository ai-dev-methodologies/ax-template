package com.ax.template.authblueprint.fixtures;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * FAIL fixture — a real {@code @SpringBootTest(RANDOM_PORT)} class that NAMES
 * the ContextCache hazard (this comment) but carries NO {@code @DirtiesContext}
 * to mitigate it. This is the exact regression the guard exists to catch: a
 * new random-port Spring context adds cache pressure without the R22 lever.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UnmitigatedRandomPortIT {

    @Test
    void hitsRealHttpSurface() {
        // deliberately missing @DirtiesContext — ContextCache eviction hazard.
    }
}
