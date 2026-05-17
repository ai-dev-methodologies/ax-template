package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.Cacheable;

@Tag("PRACTICES")
@Tag("PRACTICES-CACHE-001")
class CacheExplicitNameKeyTest {

    @Test
    void practices_CACHE_001_cacheableCarriesExplicitNameAndKeyAndSync() throws Exception {
        // Bare @Cacheable picks up the method's full parameter list as the key — fine for
        // single-argument methods, lethal for multi-argument ones where the key becomes
        // unstable (parameter ordering, mutable types). The remedy is to always declare
        // value (cache name) and key (SpEL) explicitly, and sync=true so concurrent loads
        // for the same cold key serialize through one upstream call instead of a stampede.
        Method m = CachedLookupService.class.getDeclaredMethod("lookup", Long.class);
        Cacheable ann = m.getAnnotation(Cacheable.class);
        assertThat(ann).as("@Cacheable must be present on lookup()").isNotNull();
        assertThat(ann.value())
                .as("@Cacheable.value (cache name) must be declared")
                .isNotEmpty();
        assertThat(ann.key())
                .as("@Cacheable.key (SpEL) must be declared explicitly, not defaulted")
                .isNotBlank();
        assertThat(ann.sync())
                .as("@Cacheable.sync must be true to prevent cache stampede on cold keys")
                .isTrue();
    }
}
