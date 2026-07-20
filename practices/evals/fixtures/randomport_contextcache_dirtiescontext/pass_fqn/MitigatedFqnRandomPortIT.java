package com.ax.template.authblueprint.fixtures;

import org.junit.jupiter.api.Test;

/**
 * PASS fixture (FQN escape, 2026-07-20) — a real
 * {@code @org.springframework.boot.test.context.SpringBootTest(RANDOM_PORT)}
 * class that NAMES the ContextCache hazard (this comment) AND carries the
 * R22 lever, also spelled out fully-qualified:
 * {@code @org.springframework.test.annotation.DirtiesContext}. Both
 * annotations use their FQN form with no bare `@SpringBootTest` /
 * `@DirtiesContext` substring anywhere in the source, proving the
 * FQN-tolerant regex recognizes a real mitigation and does not just flag
 * every FQN class as unmitigated. Must exit 0.
 */
@org.springframework.boot.test.context.SpringBootTest(
        webEnvironment = org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.annotation.DirtiesContext(
        classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS)
class MitigatedFqnRandomPortIT {

    @Test
    void hitsRealHttpSurface() {
        // @DirtiesContext (FQN) forces a fresh context boot — mitigated.
    }
}
