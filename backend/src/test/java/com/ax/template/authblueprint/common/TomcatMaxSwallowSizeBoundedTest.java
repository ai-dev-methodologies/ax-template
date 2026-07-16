package com.ax.template.authblueprint.common;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.tomcat.autoconfigure.TomcatServerProperties;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Config-invariant lock for {@code server.tomcat.max-swallow-size}.
 *
 * <p>An earlier revision set this property to {@code -1} (Tomcat "swallow unlimited") so a fast
 * 413 could always drain the unread request body for a clean response. That is a resource-hold
 * DoS: an attacker declaring an oversized {@code Content-Length} gets the fast 413, then trickles
 * bytes forever while Tomcat drains the connection unbounded, holding the request thread open
 * indefinitely. The fix removed the {@code -1} override so Tomcat's BOUNDED default (2 MiB)
 * applies — an oversized unread remainder now resets the connection instead of draining forever.
 *
 * <p>{@link RequestBodySizeLimitFilterTest} does NOT catch a regression of this fix: its
 * "chunked body over cap" case sends a finite ~20 MiB body that drains fast regardless of the
 * swallow-size setting, so re-adding {@code -1} to application.yml would NOT fail that test.
 * This test locks the ACTUAL effective bound Spring Boot 4.1 binds into the running Tomcat
 * container — {@link TomcatServerProperties#getMaxSwallowSize()}, populated from the
 * {@code server.tomcat} {@code @ConfigurationProperties} prefix (Boot 4.1 moved this off the old
 * {@code ServerProperties.Tomcat} inner class in spring-boot-autoconfigure onto a standalone
 * {@code TomcatServerProperties} bean in {@code spring-boot-tomcat}, registered by
 * {@code TomcatServletWebServerAutoConfiguration} via
 * {@code @EnableConfigurationProperties(TomcatServerProperties.class)} whenever the application
 * is a SERVLET web application — the default {@code @SpringBootTest} MOCK web environment
 * qualifies, so the bean is present here without a real embedded port).
 *
 * <p>If {@code server.tomcat.max-swallow-size: -1} is re-added to application.yml, this bean
 * binds {@code maxSwallowSize} to a {@code DataSize} of {@code -1} byte — this test's
 * {@code toBytes() > 0} assertion fails immediately, non-vacuously killing the regression.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Tag("COMMON_BODY_SIZE")
class TomcatMaxSwallowSizeBoundedTest {

    /** Sane ceiling — generous enough not to be brittle to Boot's exact default (2 MiB), tight
     *  enough to also catch a revert to some other effectively-unbounded huge override. */
    private static final long SANE_CEILING_BYTES = 64L * 1024 * 1024;

    @Autowired
    private TomcatServerProperties tomcatServerProperties;

    @Test
    void maxSwallowSizeIsBoundedNotUnlimited() {
        var maxSwallowSize = tomcatServerProperties.getMaxSwallowSize();

        assertThat(maxSwallowSize).as("server.tomcat.max-swallow-size must be set").isNotNull();
        assertThat(maxSwallowSize.toBytes())
            .as("max-swallow-size must be a BOUNDED positive byte count — "
                + "-1 (or any negative value) means Tomcat drains an oversized rejected body "
                + "UNBOUNDED before resetting the connection, which is the resource-hold DoS "
                + "this config was fixed to avoid")
            .isGreaterThan(0)
            .isLessThanOrEqualTo(SANE_CEILING_BYTES);
    }
}
