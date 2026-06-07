package com.ax.template.authblueprint.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import com.ax.template.authblueprint.apikey.ApiKeyAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    public SecurityConfig(ApiKeyAuthenticationFilter apiKeyAuthenticationFilter) {
        this.apiKeyAuthenticationFilter = apiKeyAuthenticationFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/practices/demo/**"))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info", "/actuator/mappings").permitAll()
                // PAYMENT-AUTHZ-004: all /api/admin/** paths require ROLE_ADMIN —
                // including the reconciliation heartbeat. The recon endpoint emits
                // append-only ledger events (RECONCILIATION_DRIFT) plus increments
                // observability counters; allowing unauthenticated access would let
                // an adversary pollute the ledger and metric stream without audit.
                // P5 security-review finding (US-014, HIGH): tightened from permitAll.
                .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                // R20 feature-flags domain (specs/feature-flags-l0.yaml):
                // FF-AUTHZ-001 — public eval endpoint (permitAll) MUST precede the
                // admin matcher so anonymous callers reach the controller, not a
                // 401 from Spring Security.
                // FF-AUTHZ-002 — admin CRUD surface requires ROLE_ADMIN.
                .requestMatchers(HttpMethod.GET, "/api/v1/feature-flags/*/active").permitAll()
                .requestMatchers("/api/v1/admin/feature-flags/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/api/items/**").authenticated()
                // ANN-AUTHZ-001 — active-list read is any authenticated user; the admin write
                // paths (/api/admin/announcements/**) are gated by the /api/admin/** rule above.
                .requestMatchers("/api/announcements/**").authenticated()
                // DISPATCH (exclusive-assignment-l0 + timed-offer-l0): actor surface
                // (create request / accept / decline / heartbeat) is any authenticated user;
                // the dispatcher write paths (/api/admin/dispatch/**: register provider, offer)
                // are gated by the /api/admin/** ROLE_ADMIN rule above.
                .requestMatchers("/api/dispatch/**").authenticated()
                // Redirect-style PG callbacks (KG이니시스 / NICE페이먼츠 / KCP / Toss V1)
                // are unauthenticated by user JWT — authentication is the PG signature
                // verified by PaymentCallbackVerifier per PAYMENT-CALLBACK-001. The
                // permitAll() carve-out MUST appear BEFORE /api/payments/** so callback
                // POSTs reach the controller (and the verifier's 401), not a Spring
                // Security 401. dogfood-6 G-NEW-1 closure — PaymentCallbackTest
                // CALLBACK-001 was false-confidence PASS (Spring 401 indistinguishable
                // from verifier 401) before this carve-out.
                .requestMatchers("/api/payments/callback/**").permitAll()
                .requestMatchers("/api/payments/**").authenticated()
                // R14 audit-log domain (specs/audit-log-l0.yaml):
                // LIST / GET — any authenticated user (blueprints/audit-log-manifest.yaml#rbac).
                // EXPORT — method-level @PreAuthorize on AuditLogExportController enforces ADMIN/AUDITOR.
                .requestMatchers("/api/audit-logs/**").authenticated()
                // R15 notification domain (specs/notification-l0.yaml):
                // NOTIF-AUTHZ-001 — every endpoint requires a valid JWT.
                // Owner-only access (NOTIF-AUTHZ-002/003) is enforced in the
                // service layer via the caller's userId from Authentication,
                // never from a path parameter.
                .requestMatchers("/api/notifications/**").authenticated()
                // R16 file-storage domain (specs/file-storage-l0.yaml):
                // FILE-AUTHZ-001 — every endpoint requires a valid JWT.
                // Owner-only access (FILE-AUTHZ-002/003) is enforced in the
                // service layer via the caller's userId from Authentication,
                // never from a path parameter.
                .requestMatchers("/api/files/**").authenticated()
                // R17 search domain (specs/search-l0.yaml):
                // SEARCH-AUTHZ-001 — every endpoint requires a valid JWT.
                // SEARCH-AUTHZ-002 — tenant scoping is derived from
                // Authentication#getName(); clients never pass a tenantId in
                // the URL or body. Cross-tenant queries return 0 hits because
                // the tenant filter is appended to every backend call.
                .requestMatchers("/api/v1/search/**").authenticated()
                // R21 billing domain (specs/billing-l0.yaml):
                // BILLING-WEBHOOK-001 — webhook intake is permitAll; auth is
                // the provider HMAC verified inside BillingWebhookController.
                // The carve-out MUST appear before /api/subscriptions/** so
                // unsigned callers reach the controller's 401, not Spring's.
                .requestMatchers("/api/webhooks/billing").permitAll()
                // BILLING-AUTHZ-001 — every subscription endpoint requires JWT.
                // BILLING-AUTHZ-002 — owner-scoped lookups in BillingService.
                .requestMatchers("/api/subscriptions/**").authenticated()
                // R23 e-commerce capstone (recipes/e-commerce/RECIPE.md):
                // Product list / detail are PUBLIC so anonymous shoppers can
                // browse before signing up (ECOM common pattern). Mutations
                // (POST/PUT/DELETE on /api/ecommerce/products) and all cart /
                // order endpoints require a JWT — owner scoping is enforced in
                // the service layer.
                .requestMatchers(HttpMethod.GET, "/api/ecommerce/products").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/ecommerce/products/*").permitAll()
                .requestMatchers("/api/ecommerce/**").authenticated()
                // Report-export domain — every endpoint requires a valid JWT
                // (EXPORT-AUTHZ-001). Owner-only access is then enforced inside
                // ReportExportService.
                .requestMatchers("/api/exports/**").authenticated()
                // caching-l0 reference workload (specs/caching-l0.yaml):
                // CACHE-KEY-001 — the authenticated principal IS the cache tenant, so a
                // JWT is required (an anonymous caller has no tenant to isolate keys by).
                .requestMatchers("/api/cache/**").authenticated()
                // pagination-l0 reference workload (specs/pagination-l0.yaml): tenant = principal.
                .requestMatchers("/api/pagination/**").authenticated()
                // problem-details-l0 reference workload (specs/problem-details-l0.yaml):
                // the demo surface deliberately raises 4xx/5xx to verify the RFC 9457
                // problem+json contract; authenticated so a principal-scoped trace_id exists.
                .requestMatchers("/api/problem-demo/**").authenticated()
                // request-validation-l0 reference workload (specs/request-validation-l0.yaml):
                // declarative boundary validation; authenticated so the demo mirrors a real
                // command surface.
                .requestMatchers("/api/request-validation/**").authenticated()
                // idempotency-l0 reference workload (specs/idempotency-l0.yaml): tenant = principal,
                // so per-tenant Idempotency-Key isolation requires a JWT.
                .requestMatchers("/api/idempotency-demo/**").authenticated()
                // optimistic-locking-l0 reference workload (specs/optimistic-locking-l0.yaml):
                // owner = principal (owner-scoped 404), so mutations require a JWT.
                .requestMatchers("/api/optlock/**").authenticated()
                // secrets-management-l0 reference workload (specs/secrets-management-l0.yaml):
                // SECRET-ACCESS-001 — the caller IS the audited principal whose per-secret grant is
                // checked, so every endpoint requires a JWT. Least-privilege denial (403) and the
                // value-free audit trail are enforced inside SecretService against
                // Authentication.getName(); no principal/secret-id ever travels in a query/body for
                // authorization. The demo never echoes a secret value (presence/ciphertext/masked only).
                .requestMatchers("/api/secrets-demo/**").authenticated()
                // soft-delete-l0 reference workload (specs/soft-delete-l0.yaml): owner = principal;
                // the include_deleted opt-in is gated to ROLE_ADMIN inside the controller.
                .requestMatchers("/api/soft-delete/**").authenticated()
                // R30 api-key domain (specs/api-key-l0.yaml):
                // KEY-AUTHZ-001 — management surface is JWT-only (the
                // ApiKeyAuthenticationFilter explicitly skips these paths
                // via shouldNotFilter). The scope-probe endpoint under the
                // same prefix is reachable via X-API-Key.
                .requestMatchers("/api/api-keys/**").authenticated()
                // R31 approval-workflow domain (specs/approval-workflow-l0.yaml):
                // WF-AUTHZ-001 — every endpoint requires JWT. Visibility scoping
                // (requester vs approver) is enforced inside ApprovalService.
                .requestMatchers("/api/approvals/**").authenticated()
                // R32 tag-categorization domain (specs/tag-categorization-l0.yaml):
                // TAG-AUTHZ-001 — every endpoint requires JWT. Definition mutations
                // (POST/PUT/DELETE on /api/tags/{id}) additionally require ROLE_ADMIN
                // via @PreAuthorize on TagController. Attach/detach is authenticated-only.
                .requestMatchers("/api/tags/**").authenticated()
                // R33 session-management domain (specs/session-management-l0.yaml):
                // SESS-AUTHZ-001 — /api/sessions/** requires JWT.
                // SESS-AUTHZ-003 — admin force-logout under /api/admin/sessions/**
                // is gated by the upstream "/api/admin/**" hasAuthority("ROLE_ADMIN")
                // matcher; the @PreAuthorize on AdminSessionController is defense-in-depth.
                .requestMatchers("/api/sessions/**").authenticated()
                // R34 favorites-bookmarks domain (specs/favorites-bookmarks-l0.yaml):
                // FAV-AUTHZ-001 — every endpoint requires JWT. Owner scoping is
                // structural (Authentication.getName() only; no userId path param).
                .requestMatchers("/api/favorites/**").authenticated()
                // R35 activity-feed domain (specs/activity-feed-l0.yaml):
                // ACT-AUTHZ-001 — every endpoint requires JWT. Visibility scoping
                // (actor OR audience contains caller) enforced inside ActivityService.
                .requestMatchers("/api/activities/**").authenticated()
                // R36 comment-thread domain (specs/comment-thread-l0.yaml):
                // COMMENT-AUTHZ-001 — every endpoint requires JWT. Author + admin
                // scoping (edit author-only; delete author-or-admin; history scoped)
                // enforced inside CommentService.
                .requestMatchers("/api/comments/**").authenticated()
                // Identity verification callbacks (PASS / KCB) follow the same
                // pattern: authentication is the provider HMAC signature
                // verified inside IdentityVerificationCallbackController per
                // IDV-CALLBACK-001 (specs/identity-verification-l0.yaml). The
                // permitAll() carve-out MUST appear before any catch-all so the
                // callback POST reaches the controller (and the controller's 401
                // on HMAC mismatch), not a Spring Security 401 — same false-
                // confidence trap closed for /api/payments/callback in dogfood-6.
                .requestMatchers("/api/identity-verification/callback/**").permitAll()
                // IMW6 data-subject-rights domain (specs/data-subject-rights-l0.yaml):
                // DSR-ACCESS/RECTIFY/ERASURE/PORTABILITY/RESTRICT/SLA-001 — the
                // self-service surface requires JWT; the data-subject is always
                // Authentication.getName() (no subject id in path/body). Cross-subject
                // admin lookups use /api/admin/dsr/** which the upstream
                // "/api/admin/**" hasAuthority("ROLE_ADMIN") matcher already gates.
                .requestMatchers("/api/me/dsr/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/auth/email/password-change").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/auth/oauth/link").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/auth/oauth/unlink/**").authenticated()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/practices/demo/**").permitAll()
                .requestMatchers("/api/ratelimit/**").permitAll()
                // i18n-policy domain (specs/i18n-policy-l0.yaml): the probe surface is a
                // stateless policy demonstrator (locale negotiation / message bundle /
                // currency formatting / UTC time round-trip) — no user data, no
                // authorization decision. permitAll keeps the compliance test black-box
                // and the wiring additive (it claims a NEW path prefix only).
                .requestMatchers("/api/i18n/**").permitAll()
                // realtime-policy domain (specs/realtime-policy-l0.yaml): SSE subscribe is
                // a plain chunked HTTP GET, so the EXISTING chain authenticates it — an
                // unauthenticated subscribe is rejected 401 here BEFORE the controller runs
                // (RT-CHANNEL-AUTH-001; there is no "WebSocket bypass" because there is no
                // protocol switch). Cross-tenant topic rejection (RT-CHANNEL-AUTH-002, 403)
                // is enforced inside RealtimeController against Authentication.getName().
                .requestMatchers("/api/realtime/**").authenticated()
                // PRACTICES-INTEG-001: webhook endpoints are HMAC-authenticated, not JWT.
                // External systems (GitHub, Stripe, etc.) POST raw payloads with HMAC-SHA256
                // signatures — they do not carry Bearer tokens. The WebhookReceiver.verify()
                // call inside the handler is the security gate.
                .requestMatchers("/api/test/webhooks").permitAll()
                // webhook-signing-l0 reference workload (specs/webhook-signing-l0.yaml):
                // WHSIGN-VERIFY-001 — an INBOUND signed webhook is unauthenticated-but-
                // signature-verified: the HMAC-SHA256 signature in the Webhook-Signature
                // header IS the authentication (no Bearer token), verified inside
                // InboundSignatureVerifier. permitAll so the signed POST reaches the
                // controller (and the verifier's 400/401/409), not a Spring Security 401 —
                // same carve-out posture as /api/payments/callback/** and /api/webhooks/billing.
                .requestMatchers("/api/webhook-signing-demo/**").permitAll()
                // api-versioning-l0 reference workload (specs/api-versioning-l0.yaml):
                // VERSION-DISCOVERY-001 mandates an UNAUTHENTICATED, cacheable discovery
                // endpoint (/api/versions); the demo surface negotiates the served version
                // by the url-path strategy and carries no user data or authorization
                // decision (it is API-surface plumbing, distinct from any business L4).
                // permitAll keeps the compliance test black-box and the wiring additive
                // (it claims only NEW path prefixes). The carve-outs MUST precede the
                // catch-all denyAll so the unauthenticated GETs reach the controller (and
                // the resolver's 400/404/410), not a Spring Security 401.
                .requestMatchers(HttpMethod.GET, "/api/versions").permitAll()
                .requestMatchers("/api/api-versioning-demo/**").permitAll()
                .anyRequest().denyAll()
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                // PAYMENT-SEC-003: HSTS — strong cryptography for cardholder data transit
                // per PCI-DSS 4.1. Production TLS termination at load balancer; this header
                // documents the policy and is asserted by PaymentSecurityTest.
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                    // PAYMENT-SEC-003: enable HSTS over plain HTTP in test mode so the
                    // SecurityConfig contract is exercised. Production load balancers
                    // terminate TLS; the header documents the requires-secure policy.
                    .requestMatcher(req -> true))
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())))
            // R30 api-key domain: X-API-Key processing happens AFTER the JWT filter
            // so a JWT-bearing request shortcircuits before we look at the header
            // (manifest authentication.jwt_takes_precedence). The filter itself
            // skips the management-path surface for KEY-AUTHZ-001.
            .addFilterAfter(apiKeyAuthenticationFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            if (role == null) return Collections.emptyList();
            return List.of(new SimpleGrantedAuthority("ROLE_" + role));
        });
        return converter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Port-wildcard PATTERNS so each per-persona app (own dev port: 3000, 3001, 3002, ...) is
        // allowed without a backend restart per port. Covers localhost + the Tailscale tailnet
        // (carray-mac -> home-mac) on ANY port. allowedOriginPatterns (not allowedOrigins) is
        // required because allowCredentials=true forbids a bare "*" — patterns are the supported
        // credential-safe wildcard form (Spring CorsConfiguration).
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:[*]", "http://127.0.0.1:[*]", "http://100.112.5.105:[*]"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-CSRF-TOKEN"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
