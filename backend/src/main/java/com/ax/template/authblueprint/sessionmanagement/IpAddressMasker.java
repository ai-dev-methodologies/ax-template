package com.ax.template.authblueprint.sessionmanagement;

/**
 * Privacy helper — redacts identifying bytes of an IP address.
 *
 * <p>Trace: SESS-INTROSPECT-002. The raw IP stays on the server entity (forensics)
 * but ANY DTO returned to the caller passes through this masker first.
 */
public final class IpAddressMasker {

    private IpAddressMasker() {}

    /** {@code 192.168.1.42} → {@code 192.168.1.xxx}; {@code 2001:db8::1} → tail 4 groups redacted. */
    public static String mask(String ip) {
        if (ip == null || ip.isBlank()) {
            return "";
        }
        if (ip.indexOf(':') >= 0) {
            return maskIPv6(ip);
        }
        return maskIPv4(ip);
    }

    private static String maskIPv4(String ip) {
        int lastDot = ip.lastIndexOf('.');
        if (lastDot < 0) {
            return ip;
        }
        return ip.substring(0, lastDot) + ".xxx";
    }

    private static String maskIPv6(String ip) {
        // IPv6 is 8 colon-separated groups. Replace the last 4 with "xxx" so /64 prefix remains.
        String expanded = ip;
        // Don't bother with full canonicalization — splitting is enough for the contract.
        String[] groups = expanded.split(":");
        StringBuilder out = new StringBuilder();
        int keep = Math.max(0, groups.length - 4);
        for (int i = 0; i < groups.length; i++) {
            if (i > 0) out.append(':');
            out.append(i < keep ? groups[i] : "xxx");
        }
        return out.toString();
    }
}
