package com.ax.template.authblueprint.fixtures;

import org.junit.jupiter.api.Test;

/**
 * PASS fixture — plain Jackson/unit test that NAMES the ContextCache hazard only
 * to explain why it deliberately avoids it (no {@code @SpringBootTest}, no
 * Spring context of any kind). Such a class CANNOT suffer ContextCache eviction
 * pressure, so demanding {@code @DirtiesContext} on it is meaningless — the
 * guard must not flag it. Plain Jackson unit test — no {@code @SpringBootTest},
 * zero ContextCache pressure.
 */
class PlainJacksonGoldenTest {

    @Test
    void serializesGoldenFixture() {
        // no Spring context is loaded here — plain Jackson only.
    }
}
