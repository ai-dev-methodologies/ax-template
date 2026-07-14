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
