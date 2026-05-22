package com.ax.template.authblueprint.sessionmanagement;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("SESSION")
class IpAddressMaskerTest {

    @Test
    @Tag("SESS-INTROSPECT-002")
    void ipv4_redactsLastOctet() {
        assertThat(IpAddressMasker.mask("192.168.1.42")).isEqualTo("192.168.1.xxx");
        assertThat(IpAddressMasker.mask("10.0.0.1")).isEqualTo("10.0.0.xxx");
    }

    @Test
    @Tag("SESS-INTROSPECT-002")
    void ipv6_redactsLast4Groups() {
        // 2001:db8:0000:0000:0000:0000:0000:0001 — last 4 groups = "xxx"
        String masked = IpAddressMasker.mask("2001:db8:0:0:0:0:0:1");
        assertThat(masked).endsWith(":xxx:xxx:xxx:xxx");
        assertThat(masked).startsWith("2001:db8:0:0:");
    }

    @Test
    @Tag("SESS-INTROSPECT-002")
    void nullAndEmpty_yieldEmpty() {
        assertThat(IpAddressMasker.mask(null)).isEmpty();
        assertThat(IpAddressMasker.mask("")).isEmpty();
        assertThat(IpAddressMasker.mask("   ")).isEmpty();
    }
}
