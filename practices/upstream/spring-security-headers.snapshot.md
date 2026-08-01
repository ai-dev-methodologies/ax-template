# spring-security-headers — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://docs.spring.io/spring-security/reference/servlet/exploits/headers.html (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T02:24:32Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://docs.spring.io/spring-security/reference/servlet/exploits/headers.html`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r153`
**Body SHA-256 (below the `---` divider, header excluded):** 8b7f63385cccc8126fd93f6f2f9a3b329c867117873cfed2a9b8fcce7482dfc1

---

---
snapshot_id: spring-security-headers
source: "https://docs.spring.io/spring-security/reference/servlet/exploits/headers.html"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 3
bytes: 18301
sha: "58617b2cb3db25d078d7ce037e53cb0481d35f604b857c0c1754b28c4e2a719f"
---

# spring security headers — upstream snapshot

Source: https://docs.spring.io/spring-security/reference/servlet/exploits/headers.html
Fetched: 2026-07-14

Security HTTP Response Headers :: Spring Security
Edit this Page
 
 
 
 GitHub Project
 
 
 
 Stack Overflow

# Security HTTP Response Headers
You can use Security HTTP Response Headers to increase the security of web applications.
This section is dedicated to servlet-based support for Security HTTP Response Headers.

## Default Security Headers
Spring Security provides a default set of Security HTTP Response Headers to provide secure defaults.
While each of these headers are considered best practice, it should be noted that not all clients use the headers, so additional testing is encouraged.
You can customize specific headers.
For example, assume that you want the defaults but you wish to specify SAMEORIGIN for X-Frame-Options.
You can do so with the following configuration:
Customize Default Security Headers
Java
XML
Kotlin
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

 @Bean
 public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
 http
 // ...
 .headers((headers) -> headers
 .frameOptions((frameOptions) -> frameOptions
 .sameOrigin()
 )
 );
 return http.build();
 }
}
@Configuration
@EnableWebSecurity
class SecurityConfig {
 @Bean
 open fun filterChain(http: HttpSecurity): SecurityFilterChain {
 http {
 // ...
 headers {
 frameOptions {
 sameOrigin = true
 }
 }
 }
 return http.build()
 }
}
If you do not want the defaults to be added and want explicit control over what should be used, you can disable the defaults.
The next code listing shows how to do so.
If you use Spring Security’s configuration, the following adds only Cache Control:
Customize Cache Control Headers
Java
XML
Kotlin
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

 @Bean
 public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
 http
 // ...
 .headers((headers) -> headers
 // do not use any default headers unless explicitly listed
 .defaultsDisabled()
 .cacheControl(withDefaults())
 );
 return http.build();
 }
}
@Configuration
@EnableWebSecurity
class SecurityConfig {
 @Bean
 open fun filterChain(http: HttpSecurity): SecurityFilterChain {
 http {
 // ...
 headers {
 // do not use any default headers unless explicitly listed
 defaultsDisabled = true
 cacheControl {
 }
 }
 }
 return http.build()
 }
}
If necessary, you can disable all of the HTTP Security response headers with the following configuration:
Disable All HTTP Security Headers
Java
XML
Kotlin
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

 @Bean
 public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
 http
 // ...
 .headers((headers) -> headers.disable());
 return http.build();
 }
}
@Configuration
@EnableWebSecurity
class SecurityConfig {
 @Bean
 open fun filterChain(http: HttpSecurity): SecurityFilterChain {
 http {
 // ...
 headers {
 disable()
 }
 }
 return http.build()
 }
}

## Cache Control
Spring Security includes Cache Control headers by default.
However, if you actually want to cache specific responses, your application can selectively invoke HttpServletResponse.setHeader(String,String) to override the header set by Spring Security.
You can use this to ensure that content (such as CSS, JavaScript, and images) is properly cached.
When you use Spring Web MVC, this is typically done within your configuration.
You can find details on how to do this in the Static Resources portion of the Spring Reference documentation
If necessary, you can also disable Spring Security’s cache control HTTP response headers.
Cache Control Disabled
Java
XML
Kotlin
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

 @Bean
 public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
 http
 // ...
 .headers((headers) -> headers
 .cacheControl((cache) -> cache.disable())
 );
 return http.build();
 }
}
@Configuration
@EnableWebSecurity
class SecurityConfig {

 @Bean
 open fun filterChain(http: HttpSecurity): SecurityFilterChain {
 http {
 headers {
 cacheControl {
 disable()
 }
 }
 }
 return http.build()
 }
}

## Content Type Options
Spring Security includes Content-Type headers by default.
However, you can disable it:
Content Type Options Disabled
Java
XML
Kotlin
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

 @Bean
 public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
 http
 // ...
 .headers((headers) -> headers
 .contentTypeOptions((contentTypeOptions) -> contentTypeOptions.disable())
 );
 return http.build();
 }
}
@Configuration
@EnableWebSecurity
class SecurityConfig {

 @Bean
 open fun filterChain(http: HttpSecurity): SecurityFilterChain {
 http {
 headers {
 contentTypeOptions {
 disable()
 }
 }
 }
 return http.build()
 }
}

## HTTP Strict Transport Security (HSTS)
By default, Spring Security provides the Strict Transport Security header.
However, you can explicitly customize the results.
The following example explicitly provides HSTS:
Strict Transport Security
Java
XML
Kotlin
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

 @Bean
 public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
 http
 // ...
 .headers((headers) -> headers
 .httpStrictTransportSecurity((hsts) -> hsts
 .includeSubDomains(true)
 .preload(true)
 .maxAgeInSeconds(31536000)
 )
 );
 return http.build();
 }
}
@Configuration
@EnableWebSecurity
class SecurityConfig {

 @Bean
 open fun filterChain(http: HttpSecurity): SecurityFilterChain {
 http {
 headers {
 httpStrictTransportSecurity {
 includeSubDomains = true
 preload = true
 maxAgeInSeconds = 31536000
 }
 }
 }
 return http.build()
 }
}

## HTTP Public Key Pinning (HPKP)
Spring Security provides servlet support for HTTP Public Key Pinning, but it is no longer recommended.
You can enable HPKP headers with the following configuration:
HTTP Public Key Pinning
Java
XML
Kotlin
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

 @Bean
 public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
 http
 // ...
 .headers((headers) -> headers
 .httpPublicKeyPinning((hpkp) -> hpkp
 .includeSubDomains(true)
 .reportUri("https://example.net/pkp-report")
 .addSha256Pins("d6qzRu9zOECb90Uez27xWltNsj0e1Md7GkYYkVoZWmM=", "E9CZ9INDbd+2eRQozYqqbQ2yXLVKB9+xcprMF+44U1g=")
 )
 );
 return http.build();
 }
}
d6qzRu9zOECb90Uez27xWltNsj0e1Md7GkYYkVoZWmM=
 E9CZ9INDbd+2eRQozYqqbQ2yXLVKB9+xcprMF+44U1g=
@Configuration
@EnableWebSecurity
class SecurityConfig {

 @Bean
 open fun filterChain(http: HttpSecurity): SecurityFilterChain {
 http {
 headers {
 httpPublicKeyPinning {
 includeSubDomains = true
 reportUri = "https://example.net/pkp-report"
 pins = mapOf("d6qzRu9zOECb90Uez27xWltNsj0e1Md7GkYYkVoZWmM=" to "sha256",
 "E9CZ9INDbd+2eRQozYqqbQ2yXLVKB9+xcprMF+44U1g=" to "sha256")
 }
 }
 }
 return http.build()
 }
}

## X-Frame-Options
By default, Spring Security instructs browsers to block reflected XSS attacks by using the X-Frame-Options.
For example, the following configuration specifies that Spring Security should no longer instruct browsers to block the content:
X-Frame-Options: SAMEORIGIN
Java
XML
Kotlin
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

 @Bean
 public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
 http
 // ...
 .headers((headers) -> headers
 .frameOptions((frameOptions) -> frameOptions
 .sameOrigin()
 )
 );
 return http.build();
 }
}
@Configuration
@EnableWebSecurity
class SecurityConfig {

 @Bean
 open fun filterChain(http: HttpSecurity): SecurityFilterChain {
 http {
 headers {
 frameOptions {
 sameOrigin = true
 }
 }
 }
 return http.build()
 }
}

## X-XSS-Protection
By default, Spring Security instructs browsers to disable the XSS Auditor by using <.
However, you can change this default.
For example, the following configuration specifies that Spring Security instruct compatible browsers to enable filtering,
and block the content:
X-XSS-Protection Customization
Java
XML
Kotlin
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

 @Bean
 public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
 http
 // ...
 .headers((headers) -> headers
 .xssProtection((xss) -> xss
 .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
 )
 );
 return http.build();
 }
}
@Configuration
@EnableWebSecurity
class SecurityConfig {

 @Bean
 open fun filterChain(http: HttpSecurity): SecurityFilterChain {
 // ...
 http {
 headers {
 xssProtection {
 headerValue = XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK
 }
 }
 }
 return http.build()
 }
}

## Content Security Policy (CSP)
Spring Security does not add Content Security Policy by default, because a reasonable default is impossible to know without knowing the context of the application.
The web application author must declare the security policy (or policies) to enforce or monitor for the protected resources.
Consider the following security policy:
Content Security Policy Example
Content-Security-Policy: script-src 'self' https://trustedscripts.example.com; object-src https://trustedplugins.example.com; report-uri /csp-report-endpoint/
Given the preceding security policy, you can enable the CSP header:
Content Security Policy
Java
XML
Kotlin
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

 @Bean
 public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
 http
 // ...
 .headers((headers) -> headers
 .contentSecurityPolicy((csp) -> csp
 .policyDirectives("script-src 'self' https://trustedscripts.example.com; object-src https://trustedplugins.example.com; report-uri /csp-report-endpoint/")
 )
 );
 return http.build();
 }
}
@Configuration
@EnableWebSecurity
class SecurityConfig {

 @Bean
 open fun filterChain(http: HttpSecurity): SecurityFilterChain {
 http {
 // ...
 headers {
 contentSecurityPolicy {
 policyDirectives = "script-src 'self' https://trustedscripts.example.com; object-src https://trustedplugins.example.com; report-uri /csp-report-endpoint/"
 }
 }
 }
 return http.build()
 }
}
To enable the CSP report-only header, provide the following configuration:
Content Security Policy Report Only
Java
XML
Kotlin
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

 @Bean
 public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
 http
 // ...
 .headers((headers) -> headers
 .contentSecurityPolicy((csp) -> csp
 .policyDirectives("script-src 'self' https://trustedscripts.example.com; object-src https://trustedplugins.example.com; report-uri /csp-report-endpoint/")
 .reportOnly()
 )
 );
 return http.build();
 }
}
@Configuration
@EnableWebSecurity
class SecurityConfig {

 @Bean
 open fun filterChain(http: HttpSecurity): SecurityFilterChain {
 http {
 // ...
 headers {
 contentSecurityPolicy {
 policyDirectives = "script-src 'self' https://trustedscripts.example.com; object-src https://trustedplugins.example.com; report-uri /csp-report-endpoint/"
 reportOnly = true
 }
 }
 }
 return http.build()
 }
}

## Referrer Policy
Spring Security does not add Referrer Policy headers by default.
You can enable the Referrer Policy header by using the configuration:
Referrer Policy
Java
XML
Kotlin
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

 @Bean
 public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
 http
 // ...
 .headers((headers) -> headers
 .referrerPolicy((referrer) -> referrer
 .policy(ReferrerPolicy.SAME_ORIGIN)
 )
 );
 return http.build();
 }
}
@Configuration
@EnableWebSecurity
class SecurityConfig {

 @Bean
 open fun filterChain(http: HttpSecurity): SecurityFilterChain {
 http {
 // ...
 headers {
 referrerPolicy {
 policy = ReferrerPolicy.SAME_ORIGIN
 }
 }
 }
 return http.build()
 }
}

## Feature Policy
Spring Security does not add Feature Policy headers by default.
Consider the following Feature-Policy header:
Feature-Policy Example
Feature-Policy: geolocation 'self'
You can enable the preceding feature policy header by using the following configuration:
Feature-Policy
Java
XML
Kotlin
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

 @Bean
 public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
 http
 // ...
 .headers((headers) -> headers
 .featurePolicy("geolocation 'self'")
 );
 return http.build();
 }
}
@Configuration
@EnableWebSecurity
class SecurityConfig {

 @Bean
 open fun filterChain(http: HttpSecurity): SecurityFilterChain {
 http {
 // ...
 headers {
 featurePolicy("geolocation 'self'")
 }
 }
 return http.build()
 }
}

## Permissions Policy
Spring Security does not add Permissions Policy headers by default.
Consider the following Permissions-Policy header:
Permissions-Policy Example
Permissions-Policy: geolocation=(self)
You can enable the preceding permissions policy header using the following configuration:
Permissions-Policy
Java
XML
Kotlin
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

 @Bean
 public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
 http
 // ...
 .headers((headers) -> headers
 .permissionsPolicy((permissions) -> permissions
 .policy("geolocation=(self)")
 )
 );
 return http.build();
 }
}
@Configuration
@EnableWebSecurity
class SecurityConfig {

 @Bean
 open fun filterChain(http: HttpSecurity): SecurityFilterChain {
 http {
 // ...
 headers {
 permissionPolicy {
 policy = "geolocation=(self)"
 }
 }
 }
 return http.build()
 }
}

## Clear Site Data
Spring Security does not add Clear-Site-Data headers by default.
Consider the following Clear-Site-Data header:
Clear-Site-Data Example
Clear-Site-Data: "cache", "cookies"
You can send the preceding header on log out with the following configuration:
Clear-Site-Data
Java
Kotlin
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

 @Bean
 public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
 http
 // ...
 .logout((logout) -> logout
 .addLogoutHandler(new HeaderWriterLogoutHandler(new ClearSiteDataHeaderWriter(CACHE, COOKIES)))
 );
 return http.build();
 }
}
@Configuration
@EnableWebSecurity
class SecurityConfig {

 @Bean
 open fun filterChain(http: HttpSecurity): SecurityFilterChain {
 http {
 // ...
 logout {
 addLogoutHandler(HeaderWriterLogoutHandler(ClearSiteDataHeaderWriter(CACHE, COOKIES)))
 }
 }
 return http.build()
 }
}

## Custom Headers
Spring Security has mechanisms to make it convenient to add the more common security headers to your application.
However, it also provides hooks to enable adding custom headers.

### Static Headers
There may be times when you wish to inject custom security headers that are not supported out of the box into your application.
Consider the following custom security header:
X-Custom-Security-Header: header-value
Given the preceding header, you could add the headers to the response by using the following configuration:
StaticHeadersWriter
Java
XML
Kotlin
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

 @Bean
 public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
 http
 // ...
 .headers((headers) -> headers
 .addHeaderWriter(new StaticHeadersWriter("X-Custom-Security-Header","header-value"))
 );
 return http.build();
 }
}
@Configuration
@EnableWebSecurity
class SecurityConfig {

 @Bean
 open fun filterChain(http: HttpSecurity): SecurityFilterChain {
 http {
 // ...
 headers {
 addHeaderWriter(StaticHeadersWriter("X-Custom-Security-Header","header-value"))
 }
 }
 return http.build()
 }
}

### Headers Writer
When the namespace or Java configuration does not support the headers you want, you can create a custom HeadersWriter instance or even provide a custom implementation of the HeadersWriter.
The next example use a custom instance of XFrameOptionsHeaderWriter.
If you wanted to explicitly configure X-Frame-Options, you could do so with the following configuration:
Headers Writer
Java
XML
Kotlin
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

 @Bean
 public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
 http
 // ...
 .headers((headers) -> headers
 .addHeaderWriter(new XFrameOptionsHeaderWriter(XFrameOptionsMode.SAMEORIGIN))
 );
 return http.build();
 }
}
@Configuration
@EnableWebSecurity
class SecurityConfig {

 @Bean
 open fun filterChain(http: HttpSecurity): SecurityFilterChain {
 http {
 // ...
 headers {
 addHeaderWriter(XFrameOptionsHeaderWriter(XFrameOptionsMode.SAMEORIGIN))
 }
 }
 return http.build()
 }
}

### DelegatingRequestMatcherHeaderWriter
At times, you may want to write a header only for certain requests.
For example, perhaps you want to protect only your login page from being framed.
You could use the DelegatingRequestMatcherHeaderWriter to do so.
The following configuration example uses DelegatingRequestMatcherHeaderWriter:
DelegatingRequestMatcherHeaderWriter Java Configuration
Java
XML
Kotlin
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

 @Bean
 public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
 RequestMatcher matcher = PathPatternRequestMatcher.withDefaults().matcher("/login");
 DelegatingRequestMatcherHeaderWriter headerWriter =
 new DelegatingRequestMatcherHeaderWriter(matcher,new XFrameOptionsHeaderWriter());
 http
 // ...
 .headers((headers) -> headers
 .frameOptions((frameOptions) -> frameOptions.disable())
 .addHeaderWriter(headerWriter)
 );
 return http.build();
 }
}
@Configuration
@EnableWebSecurity
class SecurityConfig {

 @Bean
 open fun filterChain(http: HttpSecurity): SecurityFilterChain {
 val matcher: RequestMatcher = PathPatternRequestMatcher.withDefaults().matcher("/login")
 val headerWriter = DelegatingRequestMatcherHeaderWriter(matcher, XFrameOptionsHeaderWriter())
 http {
 headers {
 frameOptions {
 disable()
 }
 addHeaderWriter(headerWriter)
 }
 }
 return http.build()
 }
}
Spring Security
7.1.0
7.0.6
6.5.11
7.1.1-SNAPSHOT
7.0.7-SNAPSHOT
6.5.12-SNAPSHOT
Related Spring Documentation
Spring Framework
Spring Security
Spring Authorization Server
Spring LDAP
Spring Security Kerberos
Spring Session
Spring Vault
Spring GraphQL
All Docs...
Search in all Spring Docs

---

## Upstream refresh 2026-08-01 (verbatim extractor output)

Source: https://docs.spring.io/spring-security/reference/servlet/exploits/headers.html
HTTP status: 200 · extracted bytes: 25689 · sha256: f1b0d6aa1f4466623ab55d22fc58f6d8183f86712d6ea03c2b35f2344863076e
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r153`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

Security HTTP Response Headers :: Spring Security Why Spring Overview Trending Generative AI Cloud Architecture Patterns Microservices Reactive Event Driven Application Types Web Applications Serverless Batch Learn Getting Started Quickstart Guides Academy Courses Get Certified Projects Overview Projects Spring Boot Spring Framework Spring Cloud Spring AI Spring Data Spring Integration Spring Batch Spring Security Foundational Projects Micrometer Reactor Development Tools Spring Tools Spring Initializr Resources Blog Release Calendar Version Mappings Release Highlights Security Advisories GitHub Orgs Spring Projects Spring Cloud Community Overview Events Authors Enterprise Overview Long-term Support Automated Upgrades Governance and Compliance Modern App Development light Spring Security 7.1.0 Search Overview Prerequisites Community What’s New Preparing for 8.0 Migrating to 7 Servlet Authorization OAuth 2.0 SAML 2.0 Reactive Getting Spring Security Javadoc KDoc Features Authentication Password Storage Authorization Protection Against Exploits CSRF HTTP Headers HTTP Requests Integrations REST Client HTTP Service Clients Cryptography Spring Data Java’s Concurrency APIs Jackson Localization Project Modules Samples Servlet Applications Getting Started Architecture Authentication Authentication Architecture Username/Password Reading Username/Password Form Basic Digest Password Storage In Memory JDBC UserDetails CredentialsContainer Password Erasure UserDetailsService PasswordEncoder DaoAuthenticationProvider LDAP Multi-Factor Authentication Persistence Passkeys One-Time Token Session Management Remember Me Anonymous Pre-Authentication JAAS CAS X509 Run-As Logout Authentication Events Kerberos Introduction Reference Samples Appendices Authorization Authorization Architecture Authorize HTTP Requests Method Security Domain Object Security ACLs Authorization Events OAuth2 OAuth2 Log In Core Configuration Advanced Configuration OIDC Logout OAuth2 Client Core Interfaces and Classes OAuth2 Authorization Grants OAuth2 Client Authentication OAuth2 Authorized Clients OAuth2 Resource Server JWT Opaque Token Multitenancy Bearer Tokens DPoP-bound Access Tokens Protected Resource Metadata OAuth2 Authorization Server Getting Started Configuration Model Core Model / Components Protocol Endpoints SAML2 SAML2 Log In SAML2 Log In Overview SAML2 Authentication Requests SAML2 Authentication Responses SAML2 Logout SAML2 Metadata Migrating from Spring Security SAML Extension Protection Against Exploits Cross Site Request Forgery (CSRF) Security HTTP Response Headers HTTP HttpFirewall Integrations Concurrency Localization Servlet APIs Spring Data Spring MVC WebSocket Spring’s CORS Support JSP Taglib Observability Configuration Java Configuration Kotlin Configuration Namespace Configuration Testing Method Security MockMvc Support MockMvc Setup Security RequestPostProcessors Mocking Users Mocking CSRF Mocking Form Login Mocking HTTP Basic Mocking OAuth2 Mocking Logout Security RequestBuilders Security ResultMatchers Security ResultHandlers Appendix Database Schemas XML Namespace Authentication Services Web Security Method Security LDAP Security WebSocket Security Proxy Server Configuration FAQ Reactive Applications Getting Started Authentication X.509 Authentication Logout Session Management Concurrent Sessions Control Authorization Authorize HTTP Requests EnableReactiveMethodSecurity OAuth2 OAuth2 Log In Core Configuration Advanced Configuration OIDC Logout OAuth2 Client Core Interfaces and Classes OAuth2 Authorization Grants OAuth2 Client Authentication OAuth2 Authorized Clients OAuth2 Resource Server JWT Opaque Token Multitenancy Bearer Tokens Protection Against Exploits CSRF Headers HTTP Requests ServerWebExchangeFirewall Integrations CORS RSocket Observability Testing Testing Method Security Testing Web Security WebTestClient Setup Testing Authentication Testing CSRF Testing OAuth 2.0 Testing X509 WebFlux Security GraalVM Native Image Support Method Security Search Edit this Page GitHub Project Stack Overflow Spring Security Servlet Applications Protection Against Exploits Security HTTP Response Headers Security HTTP Response Headers You can use Security HTTP Response Headers to increase the security of web applications. This section is dedicated to servlet-based support for Security HTTP Response Headers. Default Security Headers Spring Security provides a default set of Security HTTP Response Headers to provide secure defaults. While each of these headers are considered best practice, it should be noted that not all clients use the headers, so additional testing is encouraged. You can customize specific headers. For example, assume that you want the defaults but you wish to specify SAMEORIGIN for X-Frame-Options . You can do so with the following configuration: Customize Default Security Headers Java XML Kotlin @Configuration @EnableWebSecurity public class WebSecurityConfig { @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { http // ... .headers((headers) -> headers .frameOptions((frameOptions) -> frameOptions .sameOrigin() ) ); return http.build(); } } <http> <!-- ... --> <headers> <frame-options policy="SAMEORIGIN" /> </headers> </http> @Configuration @EnableWebSecurity class SecurityConfig { @Bean open fun filterChain(http: HttpSecurity): SecurityFilterChain { http { // ... headers { frameOptions { sameOrigin = true } } } return http.build() } } If you do not want the defaults to be added and want explicit control over what should be used, you can disable the defaults. The next code listing shows how to do so. If you use Spring Security’s configuration, the following adds only Cache Control : Customize Cache Control Headers Java XML Kotlin @Configuration @EnableWebSecurity public class WebSecurityConfig { @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { http // ... .headers((headers) -> headers // do not use any default headers unless explicitly listed .defaultsDisabled() .cacheControl(withDefaults()) ); return http.build(); } } <http> <!-- ... --> <headers defaults-disabled="true"> <cache-control/> </headers> </http> @Configuration @EnableWebSecurity class SecurityConfig { @Bean open fun filterChain(http: HttpSecurity): SecurityFilterChain { http { // ... headers { // do not use any default headers unless explicitly listed defaultsDisabled = true cacheControl { } } } return http.build() } } If necessary, you can disable all of the HTTP Security response headers with the following configuration: Disable All HTTP Security Headers Java XML Kotlin @Configuration @EnableWebSecurity public class WebSecurityConfig { @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { http // ... .headers((headers) -> headers.disable()); return http.build(); } } <http> <!-- ... --> <headers disabled="true" /> </http> @Configuration @EnableWebSecurity class SecurityConfig { @Bean open fun filterChain(http: HttpSecurity): SecurityFilterChain { http { // ... headers { disable() } } return http.build() } } Cache Control Spring Security includes Cache Control headers by default. However, if you actually want to cache specific responses, your application can selectively invoke HttpServletResponse.setHeader(String,String) to override the header set by Spring Security. You can use this to ensure that content (such as CSS, JavaScript, and images) is properly cached. When you use Spring Web MVC, this is typically done within your configuration. You can find details on how to do this in the Static Resources portion of the Spring Reference documentation If necessary, you can also disable Spring Security’s cache control HTTP response headers. Cache Control Disabled Java XML Kotlin @Configuration @EnableWebSecurity public class WebSecurityConfig { @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { http // ... .headers((headers) -> headers .cacheControl((cache) -> cache.disable()) ); return http.build(); } } <http> <!-- ... --> <headers> <cache-control disabled="true"/> </headers> </http> @Configuration @EnableWebSecurity class SecurityConfig { @Bean open fun filterChain(http: HttpSecurity): SecurityFilterChain { http { headers { cacheControl { disable() } } } return http.build() } } Content Type Options Spring Security includes Content-Type headers by default. However, you can disable it: Content Type Options Disabled Java XML Kotlin @Configuration @EnableWebSecurity public class WebSecurityConfig { @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { http // ... .headers((headers) -> headers .contentTypeOptions((contentTypeOptions) -> contentTypeOptions.disable()) ); return http.build(); } } <http> <!-- ... --> <headers> <content-type-options disabled="true"/> </headers> </http> @Configuration @EnableWebSecurity class SecurityConfig { @Bean open fun filterChain(http: HttpSecurity): SecurityFilterChain { http { headers { contentTypeOptions { disable() } } } return http.build() } } HTTP Strict Transport Security (HSTS) By default, Spring Security provides the Strict Transport Security header. However, you can explicitly customize the results. The following example explicitly provides HSTS: Strict Transport Security Java XML Kotlin @Configuration @EnableWebSecurity public class WebSecurityConfig { @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { http // ... .headers((headers) -> headers .httpStrictTransportSecurity((hsts) -> hsts .includeSubDomains(true) .preload(true) .maxAgeInSeconds(31536000) ) ); return http.build(); } } <http> <!-- ... --> <headers> <hsts include-subdomains="true" max-age-seconds="31536000" preload="true" /> </headers> </http> @Configuration @EnableWebSecurity class SecurityConfig { @Bean open fun filterChain(http: HttpSecurity): SecurityFilterChain { http { headers { httpStrictTransportSecurity { includeSubDomains = true preload = true maxAgeInSeconds = 31536000 } } } return http.build() } } HTTP Public Key Pinning (HPKP) Spring Security provides servlet support for HTTP Public Key Pinning , but it is no longer recommended . You can enable HPKP headers with the following configuration: HTTP Public Key Pinning Java XML Kotlin @Configuration @EnableWebSecurity public class WebSecurityConfig { @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { http // ... .headers((headers) -> headers .httpPublicKeyPinning((hpkp) -> hpkp .includeSubDomains(true) .reportUri("https://example.net/pkp-report") .addSha256Pins("d6qzRu9zOECb90Uez27xWltNsj0e1Md7GkYYkVoZWmM=", "E9CZ9INDbd+2eRQozYqqbQ2yXLVKB9+xcprMF+44U1g=") ) ); return http.build(); } } <http> <!-- ... --> <headers> <hpkp include-subdomains="true" report-uri="https://example.net/pkp-report"> <pins> <pin algorithm="sha256">d6qzRu9zOECb90Uez27xWltNsj0e1Md7GkYYkVoZWmM=</pin> <pin algorithm="sha256">E9CZ9INDbd+2eRQozYqqbQ2yXLVKB9+xcprMF+44U1g=</pin> </pins> </hpkp> </headers> </http> @Configuration @EnableWebSecurity class SecurityConfig { @Bean open fun filterChain(http: HttpSecurity): SecurityFilterChain { http { headers { httpPublicKeyPinning { includeSubDomains = true reportUri = "https://example.net/pkp-report" pins = mapOf("d6qzRu9zOECb90Uez27xWltNsj0e1Md7GkYYkVoZWmM=" to "sha256", "E9CZ9INDbd+2eRQozYqqbQ2yXLVKB9+xcprMF+44U1g=" to "sha256") } } } return http.build() } } X-Frame-Options By default, Spring Security instructs browsers to block reflected XSS attacks by using the X-Frame-Options . For example, the following configuration specifies that Spring Security should no longer instruct browsers to block the content: X-Frame-Options: SAMEORIGIN Java XML Kotlin @Configuration @EnableWebSecurity public class WebSecurityConfig { @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { http // ... .headers((headers) -> headers .frameOptions((frameOptions) -> frameOptions .sameOrigin() ) ); return http.build(); } } <http> <!-- ... --> <headers> <frame-options policy="SAMEORIGIN" /> </headers> </http> @Configuration @EnableWebSecurity class SecurityConfig { @Bean open fun filterChain(http: HttpSecurity): SecurityFilterChain { http { headers { frameOptions { sameOrigin = true } } } return http.build() } } X-XSS-Protection By default, Spring Security instructs browsers to disable the XSS Auditor by using <<headers-xss-protection,X-XSS-Protection header>. However, you can change this default. For example, the following configuration specifies that Spring Security instruct compatible browsers to enable filtering, and block the content: X-XSS-Protection Customization Java XML Kotlin @Configuration @EnableWebSecurity public class WebSecurityConfig { @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { http // ... .headers((headers) -> headers .xssProtection((xss) -> xss .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK) ) ); return http.build(); } } <http> <!-- ... --> <headers> <xss-protection headerValue="1; mode=block"/> </headers> </http> @Configuration @EnableWebSecurity class SecurityConfig { @Bean open fun filterChain(http: HttpSecurity): SecurityFilterChain { // ... http { headers { xssProtection { headerValue = XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK } } } return http.build() } } Content Security Policy (CSP) Spring Security does not add Content Security Policy by default, because a reasonable default is impossible to know without knowing the context of the application. The web application author must declare the security policy (or policies) to enforce or monitor for the protected resources. Consider the following security policy: Content Security Policy Example Content-Security-Policy: script-src 'self' https://trustedscripts.example.com; object-src https://trustedplugins.example.com; report-uri /csp-report-endpoint/ Given the preceding security policy, you can enable the CSP header: Content Security Policy Java XML Kotlin @Configuration @EnableWebSecurity public class WebSecurityConfig { @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { http // ... .headers((headers) -> headers .contentSecurityPolicy((csp) -> csp .policyDirectives("script-src 'self' https://trustedscripts.example.com; object-src https://trustedplugins.example.com; report-uri /csp-report-endpoint/") ) ); return http.build(); } } <http> <!-- ... --> <headers> <content-security-policy policy-directives="script-src 'self' https://trustedscripts.example.com; object-src https://trustedplugins.example.com; report-uri /csp-report-endpoint/" /> </headers> </http> @Configuration @EnableWebSecurity class SecurityConfig { @Bean open fun filterChain(http: HttpSecurity): SecurityFilterChain { http { // ... headers { contentSecurityPolicy { policyDirectives = "script-src 'self' https://trustedscripts.example.com; object-src https://trustedplugins.example.com; report-uri /csp-report-endpoint/" } } } return http.build() } } To enable the CSP report-only header, provide the following configuration: Content Security Policy Report Only Java XML Kotlin @Configuration @EnableWebSecurity public class WebSecurityConfig { @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { http // ... .headers((headers) -> headers .contentSecurityPolicy((csp) -> csp .policyDirectives("script-src 'self' https://trustedscripts.example.com; object-src https://trustedplugins.example.com; report-uri /csp-report-endpoint/") .reportOnly() ) ); return http.build(); } } <http> <!-- ... --> <headers> <content-security-policy policy-directives="script-src 'self' https://trustedscripts.example.com; object-src https://trustedplugins.example.com; report-uri /csp-report-endpoint/" report-only="true" /> </headers> </http> @Configuration @EnableWebSecurity class SecurityConfig { @Bean open fun filterChain(http: HttpSecurity): SecurityFilterChain { http { // ... headers { contentSecurityPolicy { policyDirectives = "script-src 'self' https://trustedscripts.example.com; object-src https://trustedplugins.example.com; report-uri /csp-report-endpoint/" reportOnly = true } } } return http.build() } } Referrer Policy Spring Security does not add Referrer Policy headers by default. You can enable the Referrer Policy header by using the configuration: Referrer Policy Java XML Kotlin @Configuration @EnableWebSecurity public class WebSecurityConfig { @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { http // ... .headers((headers) -> headers .referrerPolicy((referrer) -> referrer .policy(ReferrerPolicy.SAME_ORIGIN) ) ); return http.build(); } } <http> <!-- ... --> <headers> <referrer-policy policy="same-origin" /> </headers> </http> @Configuration @EnableWebSecurity class SecurityConfig { @Bean open fun filterChain(http: HttpSecurity): SecurityFilterChain { http { // ... headers { referrerPolicy { policy = ReferrerPolicy.SAME_ORIGIN } } } return http.build() } } Feature Policy Spring Security does not add Feature Policy headers by default. Consider the following Feature-Policy header: Feature-Policy Example Feature-Policy: geolocation 'self' You can enable the preceding feature policy header by using the following configuration: Feature-Policy Java XML Kotlin @Configuration @EnableWebSecurity public class WebSecurityConfig { @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { http // ... .headers((headers) -> headers .featurePolicy("geolocation 'self'") ); return http.build(); } } <http> <!-- ... --> <headers> <feature-policy policy-directives="geolocation 'self'" /> </headers> </http> @Configuration @EnableWebSecurity class SecurityConfig { @Bean open fun filterChain(http: HttpSecurity): SecurityFilterChain { http { // ... headers { featurePolicy("geolocation 'self'") } } return http.build() } } Permissions Policy Spring Security does not add Permissions Policy headers by default. Consider the following Permissions-Policy header: Permissions-Policy Example Permissions-Policy: geolocation=(self) You can enable the preceding permissions policy header using the following configuration: Permissions-Policy Java XML Kotlin @Configuration @EnableWebSecurity public class WebSecurityConfig { @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { http // ... .headers((headers) -> headers .permissionsPolicy((permissions) -> permissions .policy("geolocation=(self)") ) ); return http.build(); } } <http> <!-- ... --> <headers> <permissions-policy policy="geolocation=(self)" /> </headers> </http> @Configuration @EnableWebSecurity class SecurityConfig { @Bean open fun filterChain(http: HttpSecurity): SecurityFilterChain { http { // ... headers { permissionPolicy { policy = "geolocation=(self)" } } } return http.build() } } Clear Site Data Spring Security does not add Clear-Site-Data headers by default. Consider the following Clear-Site-Data header: Clear-Site-Data Example Clear-Site-Data: "cache", "cookies" You can send the preceding header on log out with the following configuration: Clear-Site-Data Java Kotlin @Configuration @EnableWebSecurity public class WebSecurityConfig { @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { http // ... .logout((logout) -> logout .addLogoutHandler(new HeaderWriterLogoutHandler(new ClearSiteDataHeaderWriter(CACHE, COOKIES))) ); return http.build(); } } @Configuration @EnableWebSecurity class SecurityConfig { @Bean open fun filterChain(http: HttpSecurity): SecurityFilterChain { http { // ... logout { addLogoutHandler(HeaderWriterLogoutHandler(ClearSiteDataHeaderWriter(CACHE, COOKIES))) } } return http.build() } } Custom Headers Spring Security has mechanisms to make it convenient to add the more common security headers to your application. However, it also provides hooks to enable adding custom headers. Static Headers There may be times when you wish to inject custom security headers that are not supported out of the box into your application. Consider the following custom security header: X-Custom-Security-Header: header-value Given the preceding header, you could add the headers to the response by using the following configuration: StaticHeadersWriter Java XML Kotlin @Configuration @EnableWebSecurity public class WebSecurityConfig { @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { http // ... .headers((headers) -> headers .addHeaderWriter(new StaticHeadersWriter("X-Custom-Security-Header","header-value")) ); return http.build(); } } <http> <!-- ... --> <headers> <header name="X-Custom-Security-Header" value="header-value"/> </headers> </http> @Configuration @EnableWebSecurity class SecurityConfig { @Bean open fun filterChain(http: HttpSecurity): SecurityFilterChain { http { // ... headers { addHeaderWriter(StaticHeadersWriter("X-Custom-Security-Header","header-value")) } } return http.build() } } Headers Writer When the namespace or Java configuration does not support the headers you want, you can create a custom HeadersWriter instance or even provide a custom implementation of the HeadersWriter . The next example use a custom instance of XFrameOptionsHeaderWriter . If you wanted to explicitly configure X-Frame-Options , you could do so with the following configuration: Headers Writer Java XML Kotlin @Configuration @EnableWebSecurity public class WebSecurityConfig { @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { http // ... .headers((headers) -> headers .addHeaderWriter(new XFrameOptionsHeaderWriter(XFrameOptionsMode.SAMEORIGIN)) ); return http.build(); } } <http> <!-- ... --> <headers> <header ref="frameOptionsWriter"/> </headers> </http> <!-- Requires the c-namespace. See https://docs.spring.io/spring-framework/reference/7.0.8/core/beans/dependencies/factory-properties-detailed.html#beans-c-namespace --> <beans:bean id="frameOptionsWriter" class="org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter" c:frameOptionsMode="SAMEORIGIN"/> @Configuration @EnableWebSecurity class SecurityConfig { @Bean open fun filterChain(http: HttpSecurity): SecurityFilterChain { http { // ... headers { addHeaderWriter(XFrameOptionsHeaderWriter(XFrameOptionsMode.SAMEORIGIN)) } } return http.build() } } DelegatingRequestMatcherHeaderWriter At times, you may want to write a header only for certain requests. For example, perhaps you want to protect only your login page from being framed. You could use the DelegatingRequestMatcherHeaderWriter to do so. The following configuration example uses DelegatingRequestMatcherHeaderWriter : DelegatingRequestMatcherHeaderWriter Java Configuration Java XML Kotlin @Configuration @EnableWebSecurity public class WebSecurityConfig { @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { RequestMatcher matcher = PathPatternRequestMatcher.withDefaults().matcher("/login"); DelegatingRequestMatcherHeaderWriter headerWriter = new DelegatingRequestMatcherHeaderWriter(matcher,new XFrameOptionsHeaderWriter()); http // ... .headers((headers) -> headers .frameOptions((frameOptions) -> frameOptions.disable()) .addHeaderWriter(headerWriter) ); return http.build(); } } <http> <!-- ... --> <headers> <frame-options disabled="true"/> <header ref="headerWriter"/> </headers> </http> <beans:bean id="headerWriter" class="org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter"> <beans:constructor-arg> <bean class="org.springframework.security.config.http.PathPatternRequestMatcherFactoryBean" c:pattern="/login"/> </beans:constructor-arg> <beans:constructor-arg> <beans:bean class="org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter"/> </beans:constructor-arg> </beans:bean> @Configuration @EnableWebSecurity class SecurityConfig { @Bean open fun filterChain(http: HttpSecurity): SecurityFilterChain { val matcher: RequestMatcher = PathPatternRequestMatcher.withDefaults().matcher("/login") val headerWriter = DelegatingRequestMatcherHeaderWriter(matcher, XFrameOptionsHeaderWriter()) http { headers { frameOptions { disable() } addHeaderWriter(headerWriter) } } return http.build() } } Spring Security Stable 7.1.0 7.0.6 6.5.11 Snapshot 7.1.1-SNAPSHOT 7.0.7-SNAPSHOT 6.5.12-SNAPSHOT Related Spring Documentation Spring Framework Spring Security Spring Authorization Server Spring LDAP Spring Security Kerberos Spring Session Spring Vault Spring GraphQL All Docs... Copyright © 2005 - Broadcom. All Rights Reserved. The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries. Terms of Use • Privacy • Trademark Guidelines • Thank you • Your California Privacy Rights • Cookie Settings Apache®, Apache Tomcat®, Apache Kafka®, Apache Cassandra™, and Apache Geode™ are trademarks or registered trademarks of the Apache Software Foundation in the United States and/or other countries. Java™, Java™ SE, Java™ EE, and OpenJDK™ are trademarks of Oracle and/or its affiliates. Kubernetes® is a registered trademark of the Linux Foundation in the United States and other countries. Linux® is the registered trademark of Linus Torvalds in the United States and other countries. Windows® and Microsoft® Azure are registered trademarks of Microsoft Corporation. “AWS” and “Amazon Web Services” are trademarks or registered trademarks of Amazon.com Inc. or its affiliates. All other trademarks and copyrights are property of their respective owners and are only mentioned for informative purposes. Other names may be trademarks of their respective owners. Search in all Spring Docs
