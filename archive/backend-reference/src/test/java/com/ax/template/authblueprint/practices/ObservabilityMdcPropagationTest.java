package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@Tag("PRACTICES")
@Tag("PRACTICES-OBS-002")
class ObservabilityMdcPropagationTest {

    @Test
    void practices_OBS_002_usesIncomingRequestIdHeader() throws Exception {
        MdcRequestIdFilter filter = new MdcRequestIdFilter();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(MdcRequestIdFilter.HEADER, "abc-123");
        MockHttpServletResponse res = new MockHttpServletResponse();
        AtomicReference<String> captured = new AtomicReference<>();
        FilterChain chain = (r, rsp) -> captured.set(MDC.get(MdcRequestIdFilter.MDC_KEY));

        filter.doFilter(req, res, chain);

        assertThat(captured.get()).isEqualTo("abc-123");
        // After the chain returns the MDC key must be cleared so a thread-reused worker
        // does not leak the previous request's id into the next one.
        assertThat(MDC.get(MdcRequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void practices_OBS_002_mintsUuidWhenHeaderMissing() throws Exception {
        MdcRequestIdFilter filter = new MdcRequestIdFilter();
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        AtomicReference<String> captured = new AtomicReference<>();
        FilterChain chain = (r, rsp) -> captured.set(MDC.get(MdcRequestIdFilter.MDC_KEY));

        filter.doFilter(req, res, chain);

        // UUID v4 string (36 chars including hyphens). We just assert non-null + sane length.
        assertThat(captured.get()).isNotNull().hasSize(36);
        assertThat(MDC.get(MdcRequestIdFilter.MDC_KEY)).isNull();
    }
}
