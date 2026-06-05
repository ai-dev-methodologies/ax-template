---
title: CORS MUST be an explicit origin allowlist with correct preflight + credentials policy — never a reflected-origin wildcard
impact: HIGH
impactDescription: "Reflecting the request Origin into Access-Control-Allow-Origin (or using '*') while also allowing credentials defeats the same-origin policy: any malicious site can make authenticated cross-origin requests with the victim's cookies and read the responses. CORS is a deliberate relaxation of SOP and MUST be a closed allowlist with a coherent preflight, methods, headers, and credentials policy."
tags:
  - cors
  - same-origin-policy
  - preflight
  - credentials
  - security
  - http
spec_ref: "specs/cors-l0.yaml#CORS-ORIGIN-001"
verification:
  type: review
  source: "specs/cors-l0.yaml#CORS-ORIGIN-001"
  pattern: "CORS MUST be configured as an explicit closed allowlist of permitted origins (CORS-ORIGIN-001) — never a literal '*' on a credentialed surface, and never naive reflection of the inbound Origin header into Access-Control-Allow-Origin without allowlist membership (reflection = effectively allow-all). Preflight OPTIONS requests MUST be answered with the matching Access-Control-Allow-* headers and MUST NOT execute the side-effecting handler (CORS-PREFLIGHT-001). Access-Control-Allow-Credentials=true MUST be paired with a SPECIFIC origin (never '*'); a credentialed policy with a wildcard origin is forbidden by the protocol and the catalog (CORS-CREDENTIALS-001). Allow-Methods reflects exactly the methods the endpoint supports (CORS-METHODS-001). Allow-Headers lists permitted request headers and Expose-Headers lists response headers readable by JS (CORS-HEADERS-001). Access-Control-Max-Age sets a bounded preflight cache duration (CORS-MAXAGE-001). Reject Allow-Origin:* with credentials, unconditional Origin reflection, and a preflight that mutates state."
upstream:
  - "https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CORS"
  - "https://fetch.spec.whatwg.org/#http-cors-protocol"
evidence:
  - source_type: external
    citation: "MDN Web Docs — Cross-Origin Resource Sharing (CORS) — definition"
    url: "https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CORS"
    quote: "Cross-Origin Resource Sharing (CORS) is an HTTP-header based mechanism that allows a server to indicate any origins (domain, scheme, or port) other than its own from which a browser should permit loading resources."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "MDN Web Docs — CORS — preflight request"
    url: "https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CORS"
    quote: "Unlike simple requests, for \"preflighted\" requests the browser first sends an HTTP request using the OPTIONS method to the resource on the other origin, in order to determine if the actual request is safe to send."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "MDN Web Docs — CORS — Access-Control-Allow-Credentials"
    url: "https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CORS"
    quote: "The Access-Control-Allow-Credentials header indicates whether or not the response to the request can be exposed when the credentials flag is true."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## CORS MUST be an explicit origin allowlist with correct preflight + credentials policy

**Impact: HIGH — CORS is a deliberate, controlled relaxation of the browser's same-origin policy. Per MDN, *Cross-Origin Resource Sharing (CORS) is an HTTP-header based mechanism that allows a server to indicate any origins (domain, scheme, or port) other than its own from which a browser should permit loading resources*. The danger is misconfiguration: a backend that reflects the inbound `Origin` straight back into `Access-Control-Allow-Origin` — or sends `*` — while ALSO sending `Access-Control-Allow-Credentials: true` lets any attacker page issue authenticated cross-origin requests with the victim's cookies and read the responses. That is a full SOP bypass. CORS MUST be a closed allowlist with a coherent preflight, methods, headers, and credentials policy.**

There are six load-bearing requirements — the items of `specs/cors-l0.yaml`, all governed by this rule.

**1. Explicit origin allowlist (CORS-ORIGIN-001).** Permitted origins are a closed, configured set. `Access-Control-Allow-Origin` is set to a member of that set (or omitted), NEVER a literal `*` on a credentialed surface, and NEVER the inbound `Origin` reflected without allowlist membership (reflection is allow-all in disguise). A wildcard-pattern config (e.g. `https://*.example.com`) is still bounded to a controlled suffix, not arbitrary.

**2. Preflight OPTIONS (CORS-PREFLIGHT-001).** Per MDN, *for "preflighted" requests the browser first sends an HTTP request using the OPTIONS method to the resource on the other origin, in order to determine if the actual request is safe to send*. The preflight is answered with the matching `Access-Control-Allow-*` headers and MUST NOT run the side-effecting handler — a preflight is a permission check, not the operation.

**3. Credentials policy (CORS-CREDENTIALS-001).** Per MDN, *the Access-Control-Allow-Credentials header indicates whether or not the response to the request can be exposed when the credentials flag is true*. `Allow-Credentials: true` MUST be paired with a SPECIFIC origin — the protocol forbids combining it with `Allow-Origin: *`, and the catalog forbids it as a CSRF/data-exfil vector.

**4. Allow-Methods (CORS-METHODS-001).** `Access-Control-Allow-Methods` reflects exactly the HTTP methods the endpoint supports — not a blanket list including methods the route does not implement.

**5. Allow-Headers + Expose-Headers (CORS-HEADERS-001).** `Access-Control-Allow-Headers` enumerates the request headers the client may send; `Access-Control-Expose-Headers` enumerates the response headers JavaScript is allowed to read. Both are explicit allowlists.

**6. Preflight cache (CORS-MAXAGE-001).** `Access-Control-Max-Age` sets a bounded duration the browser may cache the preflight result, trading a small staleness window for fewer preflights — bounded, not indefinite.

**Incorrect — reflects the Origin and allows credentials: any site can make authenticated cross-origin reads:**

```java
res.setHeader("Access-Control-Allow-Origin", req.getHeader("Origin")); // VIOLATION: unconditional reflection = allow-all
res.setHeader("Access-Control-Allow-Credentials", "true");             // VIOLATION: credentials + reflected origin = SOP bypass
```

**Correct — closed allowlist; credentials only with a specific allowed origin; preflight handled by the framework:**

```java
@Bean
CorsConfigurationSource cors() {
    CorsConfiguration c = new CorsConfiguration();
    c.setAllowedOrigins(List.of("https://app.example.com"));     // closed allowlist (CORS-ORIGIN-001)
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));// exactly supported (CORS-METHODS-001)
    c.setAllowedHeaders(List.of("Authorization", "Content-Type"));// explicit (CORS-HEADERS-001)
    c.setExposedHeaders(List.of("X-Request-Id"));
    c.setAllowCredentials(true);                                 // OK: paired with a specific origin (CORS-CREDENTIALS-001)
    c.setMaxAge(Duration.ofMinutes(30));                         // bounded preflight cache (CORS-MAXAGE-001)
    var src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", c);
    return src;                                                   // Spring answers OPTIONS preflight automatically (CORS-PREFLIGHT-001)
}
```

Verification: review-tier. CORS misconfiguration is a security property with no compile-time signal — a reflected-origin policy compiles and "works" for the legit frontend while silently exposing every authenticated endpoint. Verify by review against `specs/cors-l0.yaml`: origins are a closed allowlist (no `*` with credentials, no unconditional reflection); preflight OPTIONS is answered without running the handler; `Allow-Credentials: true` only with a specific origin; methods/headers are explicit allowlists; `Max-Age` is bounded. When a fork-receiver wires a real IT (a disallowed Origin gets no `Allow-Origin`; `*`+credentials is rejected), this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [MDN — Cross-Origin Resource Sharing (CORS)](https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CORS)
