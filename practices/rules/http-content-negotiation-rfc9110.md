---
title: An API serving multiple representations MUST do proactive content negotiation — rank Accept, validate Content-Type with 415, answer 406 when nothing matches
impact: MEDIUM
impactDescription: "An endpoint that ignores the Accept header and always returns one format breaks clients that asked for another; one that does not validate the request Content-Type silently mis-parses a body sent in an unexpected media type; one that returns 200 with a default format when it cannot satisfy Accept lies to the client instead of returning 406. Correct proactive negotiation per RFC 9110 keeps representation selection predictable and honest."
tags:
  - http
  - content-negotiation
  - rfc-9110
  - accept
  - media-type
spec_ref: "specs/content-negotiation-l0.yaml#CONNEG-ACCEPT-001"
verification:
  type: review
  source: "specs/content-negotiation-l0.yaml#CONNEG-ACCEPT-001"
  pattern: "An endpoint that can return more than one representation MUST do proactive (server-driven) content negotiation. It MUST parse the Accept header, honor quality values (q) to rank acceptable media types, and select the highest-ranked representation it can produce (CONNEG-ACCEPT-001). It MUST validate the request body's Content-Type and respond 415 Unsupported Media Type when the body media type is not one it accepts (CONNEG-TYPE-001). Language selection follows Accept-Language with a Content-Language response (composing i18n-policy) (CONNEG-LANGUAGE-001). Accept-Encoding (gzip/br) drives a Content-Encoding response when compression is offered (CONNEG-ENCODING-001). When NO available representation matches the Accept preferences, it MUST respond 406 Not Acceptable — never 200 with an unrequested format (CONNEG-406-001). When Accept is absent or */*, it serves a declared default representation (CONNEG-DEFAULT-001). Reject an endpoint that ignores Accept and hardcodes one format where multiple are contracted, that skips request Content-Type validation, or that returns a default representation with 200 when it cannot satisfy a specific Accept."
upstream:
  - "https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/Content_negotiation"
  - "https://www.rfc-editor.org/rfc/rfc9110#section-12"
evidence:
  - source_type: external
    citation: "MDN Web Docs — Content negotiation (definition)"
    url: "https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/Content_negotiation"
    quote: "In HTTP, content negotiation is the mechanism that is used for serving different representations of a resource to the same URI to help the user agent specify which representation is best suited for the user (for example, which document language, which image format, or which content encoding)."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "MDN Web Docs — Content negotiation (server-driven / proactive)"
    url: "https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/Content_negotiation"
    quote: "In server-driven content negotiation, or proactive content negotiation, the browser (or any other kind of user agent) sends several HTTP headers along with the URL."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "MDN Web Docs — Content negotiation (406 / 415 when no suitable resource)"
    url: "https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/Content_negotiation"
    quote: "If it can't provide a suitable resource, it might respond with 406 (Not Acceptable) or 415 (Unsupported Media Type) and set headers for the types of media that it does support (e.g., using the Accept-Post or Accept-Patch for POST and PATCH requests, respectively)."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## An API serving multiple representations MUST do proactive content negotiation

**Impact: MEDIUM — When an endpoint can return more than one representation (JSON or CSV; English or Korean; gzip or identity), the client states its preference in request headers and the server is responsible for honoring it. Per MDN, *content negotiation is the mechanism that is used for serving different representations of a resource to the same URI*, and in *server-driven content negotiation, or proactive content negotiation, the browser ... sends several HTTP headers along with the URL*. An endpoint that ignores `Accept` and always returns one format breaks clients that need another; one that does not validate request `Content-Type` mis-parses unexpected bodies; one that returns `200` with a default it was not asked for instead of `406` lies about what it produced.**

There are six load-bearing requirements — the items of `specs/content-negotiation-l0.yaml`, all governed by this rule.

**1. Accept parsing + quality-value ranking (CONNEG-ACCEPT-001).** Parse `Accept`, honor the `q` quality values to rank the client's acceptable media types, and select the highest-ranked representation the endpoint can actually produce. `Accept: application/json;q=0.9, text/csv;q=1.0` means CSV is preferred.

**2. Request Content-Type validation → 415 (CONNEG-TYPE-001).** Validate the *request body's* `Content-Type`. If the body arrives in a media type the endpoint does not accept, respond `415 Unsupported Media Type` — never attempt to parse it as the expected type. Per MDN, the server *might respond with ... 415 (Unsupported Media Type)* when it cannot handle the representation.

**3. Language selection (CONNEG-LANGUAGE-001).** `Accept-Language` selects the response language; echo the chosen language in `Content-Language`. Composes `i18n-policy-l0`.

**4. Encoding (CONNEG-ENCODING-001).** When compression is offered, `Accept-Encoding` (gzip, br) drives a `Content-Encoding` response header naming the encoding actually applied.

**5. 406 when nothing matches (CONNEG-406-001).** When NO representation the endpoint can produce matches the `Accept` preferences, respond `406 Not Acceptable` — per MDN, *if it can't provide a suitable resource, it might respond with 406 (Not Acceptable)*. It MUST NOT return `200` with a representation the client did not ask for.

**6. Default when Accept absent / \*/\* (CONNEG-DEFAULT-001).** When `Accept` is absent or `*/*`, serve a single *declared* default representation. The default applies only to the unspecified/wildcard case — never as a silent substitute for an unsatisfiable specific `Accept` (that is requirement 5's 406).

**Incorrect — ignores Accept, returns a default with 200 even when it cannot satisfy the request:**

```java
@GetMapping("/report/{id}")            // contracted to serve JSON AND CSV
public ResponseEntity<String> report(@PathVariable String id) {
    // VIOLATION: Accept ignored; always JSON; a client that sent Accept: text/csv
    // gets 200 + JSON it cannot parse, instead of CSV — or 406 if CSV is impossible.
    return ResponseEntity.ok(toJson(load(id)));
}
```

**Correct — produces per Accept, validates request Content-Type, 406 when unsatisfiable:**

```java
@GetMapping(value = "/report/{id}",
            produces = { MediaType.APPLICATION_JSON_VALUE, "text/csv" })  // declared representations
public ResponseEntity<?> report(@PathVariable String id,
                                @RequestHeader(value = "Accept", required = false) String accept) {
    Report r = load(id);
    MediaType chosen = negotiate(accept, List.of(APPLICATION_JSON, parseMediaType("text/csv")));
    if (chosen == null) {                                  // nothing the client accepts (CONNEG-406-001)
        return ResponseEntity.status(NOT_ACCEPTABLE).build();
    }
    return ResponseEntity.ok().contentType(chosen)
        .body(chosen.equals(APPLICATION_JSON) ? toJson(r) : toCsv(r));
}
// @PostMapping(consumes = APPLICATION_JSON_VALUE) → Spring returns 415 for a non-JSON body (CONNEG-TYPE-001).
```

Verification: review-tier. Negotiation correctness is an HTTP-contract property — an Accept-ignoring endpoint compiles and serves its one format fine until a client needs another. Verify by review against `specs/content-negotiation-l0.yaml`: multi-representation endpoints parse Accept and rank by q; request Content-Type is validated with 415; Accept-Language drives Content-Language; Accept-Encoding drives Content-Encoding; an unsatisfiable Accept returns 406 (not a 200 default); absent/`*/*` Accept serves a declared default. When a fork-receiver wires a real IT (`Accept: text/csv` → CSV; an impossible Accept → 406; wrong-type body → 415), this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [MDN — Content negotiation](https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/Content_negotiation)

Reference: [RFC 9110 §12 — Content Negotiation](https://www.rfc-editor.org/rfc/rfc9110#section-12)
