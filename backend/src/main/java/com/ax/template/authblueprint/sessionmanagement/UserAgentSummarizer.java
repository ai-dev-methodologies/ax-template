package com.ax.template.authblueprint.sessionmanagement;

/**
 * Trivial UA summarizer — extracts a coarse browser / OS family from the raw header.
 *
 * <p>Trace: SESS-INTROSPECT-002. The raw User-Agent string is sensitive (precise version
 * + plugins enable fingerprinting). The DTO exposes only this summarized form.
 *
 * <p>This implementation is intentionally lightweight (no UA-parser dependency). Fork-receivers
 * with stricter requirements can swap in {@code ua-parser-java} or similar.
 */
public final class UserAgentSummarizer {

    private UserAgentSummarizer() {}

    public static String summarize(String ua) {
        if (ua == null || ua.isBlank()) {
            return "unknown";
        }
        String lower = ua.toLowerCase();
        String browser = detectBrowser(lower);
        String os = detectOs(lower);
        return browser + " on " + os;
    }

    private static String detectBrowser(String ua) {
        if (ua.contains("edg/")) return "Edge";
        if (ua.contains("chrome/")) return "Chrome";
        if (ua.contains("firefox/")) return "Firefox";
        if (ua.contains("safari/")) return "Safari";
        return "Other";
    }

    private static String detectOs(String ua) {
        if (ua.contains("windows")) return "Windows";
        if (ua.contains("macintosh") || ua.contains("mac os")) return "macOS";
        if (ua.contains("android")) return "Android";
        if (ua.contains("iphone") || ua.contains("ipad")) return "iOS";
        if (ua.contains("linux")) return "Linux";
        return "Other";
    }
}
