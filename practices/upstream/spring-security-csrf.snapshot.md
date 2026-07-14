---
snapshot_id: spring-security-csrf
source: "https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 3
bytes: 45319
sha: "a19d60833f2370b49a9b99c927108aa3981c3467c5984ba749a4da11c2382692"
---

# spring security csrf — upstream snapshot

Source: https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html
Fetched: 2026-07-14

Cross Site Request Forgery (CSRF) :: Spring Security

Why Spring
Overview
Trending
Generative AICloud
Architecture Patterns
MicroservicesReactiveEvent Driven
Application Types
Web ApplicationsServerlessBatch

Learn

Getting Started
QuickstartGuides
Academy
Courses
Get Certified

Projects
Overview
Projects
Spring BootSpring FrameworkSpring CloudSpring AISpring DataSpring IntegrationSpring BatchSpring Security
Foundational Projects
Micrometer
Reactor

Development Tools
Spring ToolsSpring Initializr

Resources
BlogRelease CalendarVersion MappingsRelease HighlightsSecurity Advisories
GitHub Orgs
Spring Projects
Spring Cloud

Community
OverviewEventsAuthors

Enterprise
OverviewLong-term SupportAutomated UpgradesGovernance and ComplianceModern App Development

light

Spring Security7.1.0
Search

Overview

Prerequisites

Community

What’s New

Preparing for 8.0

Migrating to 7

Servlet

Authorization

OAuth 2.0

SAML 2.0

Reactive

Getting Spring Security

Javadoc

KDoc

Features

Authentication

Password Storage

Authorization

Protection Against Exploits

CSRF

HTTP Headers

HTTP Requests

Integrations

REST Client

HTTP Service Clients

Cryptography

Spring Data

Java’s Concurrency APIs

Jackson

Localization

Project Modules

Samples

Servlet Applications

Getting Started

Architecture

Authentication

Authentication Architecture

Username/Password

Reading Username/Password

Form

Basic

Digest

Password Storage

In Memory

JDBC

UserDetails

CredentialsContainer

Password Erasure

UserDetailsService

PasswordEncoder

DaoAuthenticationProvider

LDAP

Multi-Factor Authentication

Persistence

Passkeys

One-Time Token

Session Management

Remember Me

Anonymous

Pre-Authentication

JAAS

CAS

X509

Run-As

Logout

Authentication Events

Kerberos

Introduction

Reference

Samples

Appendices

Authorization

Authorization Architecture

Authorize HTTP Requests

Method Security

Domain Object Security ACLs

Authorization Events

OAuth2

OAuth2 Log In

Core Configuration

Advanced Configuration

OIDC Logout

OAuth2 Client

Core Interfaces and Classes

OAuth2 Authorization Grants

OAuth2 Client Authentication

OAuth2 Authorized Clients

OAuth2 Resource Server

JWT

Opaque Token

Multitenancy

Bearer Tokens

DPoP-bound Access Tokens

Protected Resource Metadata

OAuth2 Authorization Server

Getting Started

Configuration Model

Core Model / Components

Protocol Endpoints

SAML2

SAML2 Log In

SAML2 Log In Overview

SAML2 Authentication Requests

SAML2 Authentication Responses

SAML2 Logout

SAML2 Metadata

Migrating from Spring Security SAML Extension

Protection Against Exploits

Cross Site Request Forgery (CSRF)

Security HTTP Response Headers

HTTP

HttpFirewall

Integrations

Concurrency

Localization

Servlet APIs

Spring Data

Spring MVC

WebSocket

Spring’s CORS Support

JSP Taglib

Observability

Configuration

Java Configuration

Kotlin Configuration

Namespace Configuration

Testing

Method Security

MockMvc Support

MockMvc Setup

Security RequestPostProcessors

Mocking Users

Mocking CSRF

Mocking Form Login

Mocking HTTP Basic

Mocking OAuth2

Mocking Logout

Security RequestBuilders

Security ResultMatchers

Security ResultHandlers

Appendix

Database Schemas

XML Namespace

Authentication Services

Web Security

Method Security

LDAP Security

WebSocket Security

Proxy Server Configuration

FAQ

Reactive Applications

Getting Started

Authentication

X.509 Authentication

Logout

Session Management

Concurrent Sessions Control

Authorization

Authorize HTTP Requests

EnableReactiveMethodSecurity

OAuth2

OAuth2 Log In

Core Configuration

Advanced Configuration

OIDC Logout

OAuth2 Client

Core Interfaces and Classes

OAuth2 Authorization Grants

OAuth2 Client Authentication

OAuth2 Authorized Clients

OAuth2 Resource Server

JWT

Opaque Token

Multitenancy

Bearer Tokens

Protection Against Exploits

CSRF

Headers

HTTP Requests

ServerWebExchangeFirewall

Integrations

CORS

RSocket

Observability

Testing

Testing Method Security

Testing Web Security

WebTestClient Setup

Testing Authentication

Testing CSRF

Testing OAuth 2.0

Testing X509

WebFlux Security

GraalVM Native Image Support

Method Security

Search

Edit this Page

GitHub Project

Stack Overflow

Spring Security

Servlet Applications

Protection Against Exploits

Cross Site Request Forgery (CSRF)

# Cross Site Request Forgery (CSRF)

In an application where end users can log in, it is important to consider how to protect against Cross Site Request Forgery (CSRF).

Spring Security protects against CSRF attacks by default for unsafe HTTP methods, such as a POST request, so no additional code is necessary.
You can specify the default configuration explicitly using the following:

Configure CSRF Protection

Java

Kotlin

XML

@Configuration
@EnableWebSecurity
public class SecurityConfig {

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
http
// ...
.csrf(Customizer.withDefaults());
return http.build();
}
}

import org.springframework.security.config.annotation.web.invoke

@Configuration
@EnableWebSecurity
class SecurityConfig {

@Bean
open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
http {
// ...
csrf { }
}
return http.build()
}
}

<http>
<!-- ... -->
<csrf/>
</http>

To learn more about CSRF protection for your application, consider the following use cases:

I want to understand CSRF protection’s components

I need to migrate an application from Spring Security 5 to 6

I want to store the CsrfToken in a cookie instead of the session

I want to store the CsrfToken in a custom location

I want to opt-out of deferred tokens

I want to opt-out of BREACH protection

I need guidance integrating Thymeleaf, JSPs or another view technology with the backend

I need guidance integrating Angular or another JavaScript framework with the backend

I need guidance integrating a mobile application or another client with the backend

I need guidance on handling errors

I want to test CSRF protection

I need guidance on disabling CSRF protection

## Understanding CSRF Protection’s Components

CSRF protection is provided by several components that are composed within the CsrfFilter:

Figure 1. CsrfFilter Components

CSRF protection is divided into two parts:

Make the CsrfToken available to the application by delegating to the CsrfTokenRequestHandler.

Determine if the request requires CSRF protection, load and validate the token, and handle AccessDeniedException.

Figure 2. CsrfFilter Processing

First, the DeferredCsrfToken is loaded, which holds a reference to the CsrfTokenRepository so that the persisted CsrfToken can be loaded later (in ).

Second, a Supplier<CsrfToken> (created from DeferredCsrfToken) is given to the CsrfTokenRequestHandler, which is responsible for populating a request attribute to make the CsrfToken available to the rest of the application.

Next, the main CSRF protection processing begins and checks if the current request requires CSRF protection. If not required, the filter chain is continued and processing ends.

If CSRF protection is required, the persisted CsrfToken is finally loaded from the DeferredCsrfToken.

Continuing, the actual CSRF token provided by the client (if any) is resolved using the CsrfTokenRequestHandler.

The actual CSRF token is compared against the persisted CsrfToken. If valid, the filter chain is continued and processing ends.

If the actual CSRF token is invalid (or missing), an AccessDeniedException is passed to the AccessDeniedHandler and processing ends.

## Migrating to Spring Security 6

When migrating from Spring Security 5 to 6, there are a few changes that may impact your application.
The following is an overview of the aspects of CSRF protection that have changed in Spring Security 6:

Loading of the CsrfToken is now deferred by default to improve performance by no longer requiring the session to be loaded on every request.

The CsrfToken now includes randomness on every request by default to protect the CSRF token from a BREACH attack.

The changes in Spring Security 6 require additional configuration for single-page applications, and as such you may find the Single-Page Applications section particularly useful.

See the Exploit Protection section of the Migration chapter for more information on migrating a Spring Security 5 application.

## Persisting the CsrfToken

The CsrfToken is persisted using a CsrfTokenRepository.

By default, the HttpSessionCsrfTokenRepository is used for storing tokens in a session.
Spring Security also provides the CookieCsrfTokenRepository for storing tokens in a cookie.
You can also specify your own implementation to store tokens wherever you like.

### Using the HttpSessionCsrfTokenRepository

By default, Spring Security stores the expected CSRF token in the HttpSession by using HttpSessionCsrfTokenRepository, so no additional code is necessary.

The HttpSessionCsrfTokenRepository reads the token from a session (whether in-memory, cache, or database). If you need to access the session attribute directly, please first configure the session attribute name using HttpSessionCsrfTokenRepository#setSessionAttributeName.

You can specify the default configuration explicitly using the following configuration:

Configure HttpSessionCsrfTokenRepository

Java

Kotlin

XML

@Configuration
@EnableWebSecurity
public class SecurityConfig {

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
http
// ...
.csrf((csrf) -> csrf
.csrfTokenRepository(new HttpSessionCsrfTokenRepository())
);
return http.build();
}
}

import org.springframework.security.config.annotation.web.invoke

@Configuration
@EnableWebSecurity
class SecurityConfig {

@Bean
open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
http {
// ...
csrf {
csrfTokenRepository = HttpSessionCsrfTokenRepository()
}
}
return http.build()
}
}

<http>
<!-- ... -->
<csrf token-repository-ref="tokenRepository"/>
</http>
<b:bean id="tokenRepository"
class="org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository"/>

### Using the CookieCsrfTokenRepository

You can persist the CsrfToken in a cookie to support a JavaScript-based application using the CookieCsrfTokenRepository.

The CookieCsrfTokenRepository writes to a cookie named XSRF-TOKEN and reads it from an HTTP request header named X-XSRF-TOKEN or the request parameter _csrf by default.
These defaults come from Angular and its predecessor AngularJS.

See the HttpClient XSRF/CSRF security and the withXsrfConfiguration for more recent information on this topic.

You can configure the CookieCsrfTokenRepository using the following configuration:

Configure CookieCsrfTokenRepository

Java

Kotlin

XML

@Configuration
@EnableWebSecurity
public class SecurityConfig {

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
http
// ...
.csrf((csrf) -> csrf
.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
);
return http.build();
}
}

import org.springframework.security.config.annotation.web.invoke

@Configuration
@EnableWebSecurity
class SecurityConfig {

@Bean
open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
http {
// ...
csrf {
csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse()
}
}
return http.build()
}
}

<http>
<!-- ... -->
<csrf token-repository-ref="tokenRepository"/>
</http>
<b:bean id="tokenRepository"
class="org.springframework.security.web.csrf.CookieCsrfTokenRepository"
p:cookieHttpOnly="false"/>

The example explicitly sets HttpOnly to false.
This is necessary to let JavaScript frameworks (such as Angular) read it.
If you do not need the ability to read the cookie with JavaScript directly, we recommend omitting HttpOnly (by using new CookieCsrfTokenRepository() instead) to improve security.

### Customizing the CsrfTokenRepository

There can be cases where you want to implement a custom CsrfTokenRepository.

Once you’ve implemented the CsrfTokenRepository interface, you can configure Spring Security to use it with the following configuration:

Configure Custom CsrfTokenRepository

Java

Kotlin

XML

@Configuration
@EnableWebSecurity
public class SecurityConfig {

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
http
// ...
.csrf((csrf) -> csrf
.csrfTokenRepository(new CustomCsrfTokenRepository())
);
return http.build();
}
}

import org.springframework.security.config.annotation.web.invoke

@Configuration
@EnableWebSecurity
class SecurityConfig {

@Bean
open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
http {
// ...
csrf {
csrfTokenRepository = CustomCsrfTokenRepository()
}
}
return http.build()
}
}

<http>
<!-- ... -->
<csrf token-repository-ref="tokenRepository"/>
</http>
<b:bean id="tokenRepository"
class="example.CustomCsrfTokenRepository"/>

## Handling the CsrfToken

The CsrfToken is made available to an application using a CsrfTokenRequestHandler.
This component is also responsible for resolving the CsrfToken from HTTP headers or request parameters.

By default, the XorCsrfTokenRequestAttributeHandler is used for providing BREACH protection of the CsrfToken.
Spring Security also provides the CsrfTokenRequestAttributeHandler for opting out of BREACH protection.
You can also specify your own implementation to customize the strategy for handling and resolving tokens.

### Using the XorCsrfTokenRequestAttributeHandler (BREACH)

The XorCsrfTokenRequestAttributeHandler makes the CsrfToken available as an HttpServletRequest attribute called _csrf, and additionally provides protection for BREACH.

The CsrfToken is also made available as a request attribute using the name CsrfToken.class.getName().
This name is not configurable, but the name _csrf can be changed using XorCsrfTokenRequestAttributeHandler#setCsrfRequestAttributeName.

This implementation also resolves the token value from the request as either a request header (one of X-CSRF-TOKEN or X-XSRF-TOKEN by default) or a request parameter (_csrf by default).

BREACH protection is provided by encoding randomness into the CSRF token value to ensure the returned CsrfToken changes on every request.
When the token is later resolved as a header value or request parameter, it is decoded to obtain the raw token which is then compared to the persisted CsrfToken.

Spring Security protects the CSRF token from a BREACH attack by default, so no additional code is necessary.
You can specify the default configuration explicitly using the following configuration:

Configure BREACH protection

Java

Kotlin

XML

@Configuration
@EnableWebSecurity
public class SecurityConfig {

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
http
// ...
.csrf((csrf) -> csrf
.csrfTokenRequestHandler(new XorCsrfTokenRequestAttributeHandler())
);
return http.build();
}
}

import org.springframework.security.config.annotation.web.invoke

@Configuration
@EnableWebSecurity
class SecurityConfig {

@Bean
open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
http {
// ...
csrf {
csrfTokenRequestHandler = XorCsrfTokenRequestAttributeHandler()
}
}
return http.build()
}
}

<http>
<!-- ... -->
<csrf request-handler-ref="requestHandler"/>
</http>
<b:bean id="requestHandler"
class="org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler"/>

### Using the CsrfTokenRequestAttributeHandler

The CsrfTokenRequestAttributeHandler makes the CsrfToken available as an HttpServletRequest attribute called _csrf.

The CsrfToken is also made available as a request attribute using the name CsrfToken.class.getName().
This name is not configurable, but the name _csrf can be changed using CsrfTokenRequestAttributeHandler#setCsrfRequestAttributeName.

This implementation also resolves the token value from the request as either a request header (one of X-CSRF-TOKEN or X-XSRF-TOKEN by default) or a request parameter (_csrf by default).

The primary use of CsrfTokenRequestAttributeHandler is to opt-out of BREACH protection of the CsrfToken, which can be configured using the following configuration:

Opt-out of BREACH protection

Java

Kotlin

XML

@Configuration
@EnableWebSecurity
public class SecurityConfig {

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
http
// ...
.csrf((csrf) -> csrf
.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
);
return http.build();
}
}

import org.springframework.security.config.annotation.web.invoke

@Configuration
@EnableWebSecurity
class SecurityConfig {

@Bean
open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
http {
// ...
csrf {
csrfTokenRequestHandler = CsrfTokenRequestAttributeHandler()
}
}
return http.build()
}
}

<http>
<!-- ... -->
<csrf request-handler-ref="requestHandler"/>
</http>
<b:bean id="requestHandler"
class="org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler"/>

### Customizing the CsrfTokenRequestHandler

You can implement the CsrfTokenRequestHandler interface to customize the strategy for handling and resolving tokens.

The CsrfTokenRequestHandler interface is a @FunctionalInterface that can be implemented using a lambda expression to customize request handling.
You will need to implement the full interface to customize how tokens are resolved from the request.
See Configure CSRF for Single-Page Application for an example that uses delegation to implement a custom strategy for handling and resolving tokens.

Once you’ve implemented the CsrfTokenRequestHandler interface, you can configure Spring Security to use it with the following configuration:

Configure Custom CsrfTokenRequestHandler

Java

Kotlin

XML

@Configuration
@EnableWebSecurity
public class SecurityConfig {

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
http
// ...
.csrf((csrf) -> csrf
.csrfTokenRequestHandler(new CustomCsrfTokenRequestHandler())
);
return http.build();
}
}

import org.springframework.security.config.annotation.web.invoke

@Configuration
@EnableWebSecurity
class SecurityConfig {

@Bean
open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
http {
// ...
csrf {
csrfTokenRequestHandler = CustomCsrfTokenRequestHandler()
}
}
return http.build()
}
}

<http>
<!-- ... -->
<csrf request-handler-ref="requestHandler"/>
</http>
<b:bean id="requestHandler"
class="example.CustomCsrfTokenRequestHandler"/>

## Deferred Loading of the CsrfToken

By default, Spring Security defers loading of the CsrfToken until it is needed.

The CsrfToken is needed whenever a request is made with an unsafe HTTP method, such as a POST.
Additionally, it is needed by any request that renders the token to the response, such as a web page with a <form> tag that includes a hidden <input> for the CSRF token.

Because Spring Security also stores the CsrfToken in the HttpSession by default, deferred CSRF tokens can improve performance by not requiring the session to be loaded on every request.

In the event that you want to opt-out of deferred tokens and cause the CsrfToken to be loaded on every request, you can do so with the following configuration:

Opt-out of Deferred CSRF Tokens

Java

Kotlin

XML

@Configuration
@EnableWebSecurity
public class SecurityConfig {

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
XorCsrfTokenRequestAttributeHandler requestHandler = new XorCsrfTokenRequestAttributeHandler();
// set the name of the attribute the CsrfToken will be populated on
requestHandler.setCsrfRequestAttributeName(null);
http
// ...
.csrf((csrf) -> csrf
.csrfTokenRequestHandler(requestHandler)
);
return http.build();
}
}

import org.springframework.security.config.annotation.web.invoke

@Configuration
@EnableWebSecurity
class SecurityConfig {

@Bean
open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
val requestHandler = XorCsrfTokenRequestAttributeHandler()
// set the name of the attribute the CsrfToken will be populated on
requestHandler.setCsrfRequestAttributeName(null)
http {
// ...
csrf {
csrfTokenRequestHandler = requestHandler
}
}
return http.build()
}
}

<http>
<!-- ... -->
<csrf request-handler-ref="requestHandler"/>
</http>
<b:bean id="requestHandler"
class="org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler">
<b:property name="csrfRequestAttributeName">
<b:null/>
</b:property>
</b:bean>

By setting the csrfRequestAttributeName to null, the CsrfToken must first be loaded to determine what attribute name to use.
This causes the CsrfToken to be loaded on every request.

## Integrating with CSRF Protection

For the synchronizer token pattern to protect against CSRF attacks, we must include the actual CSRF token in the HTTP request.
This must be included in a part of the request (a form parameter, an HTTP header, or other part) that is not automatically included in the HTTP request by the browser.

The following sections describe the various ways a frontend or client application can integrate with a CSRF-protected backend application:

HTML Forms

JavaScript Applications

Mobile Applications

### HTML Forms

To submit an HTML form, the CSRF token must be included in the form as a hidden input.
For example, the rendered HTML might look like:

CSRF Token in HTML Form

<input type="hidden"
name="_csrf"
value="4bfd1575-3ad1-4d21-96c7-4ef2d9f86721"/>

The following view technologies automatically include the actual CSRF token in a form that has an unsafe HTTP method, such as a POST:

Spring’s form tag library

Thymeleaf

Any other view technology that integrates with RequestDataValueProcessor (via CsrfRequestDataValueProcessor)

You can also include the token yourself via the csrfInput tag

If these options are not available, you can take advantage of the fact that the CsrfToken is exposed as an HttpServletRequest attribute named _csrf.
The following example does this with a JSP:

CSRF Token in HTML Form with Request Attribute

<c:url var="logoutUrl" value="/logout"/>
<form action="${logoutUrl}"
method="post">
<input type="submit"
value="Log out" />
<input type="hidden"
name="${_csrf.parameterName}"
value="${_csrf.token}"/>
</form>

### JavaScript Applications

JavaScript applications typically use JSON instead of HTML.
If you use JSON, you can submit the CSRF token within an HTTP request header instead of a request parameter.

In order to obtain the CSRF token, you can configure Spring Security to store the expected CSRF token in a cookie.
By storing the expected token in a cookie, JavaScript frameworks such as Angular can automatically include the actual CSRF token as an HTTP request header.

There are special considerations for BREACH protection and deferred tokens when integrating a single-page application (SPA) with Spring Security’s CSRF protection.
A full configuration example is provided in the next section.

You can read about different types of JavaScript applications in the following sections:

Single-Page Applications

Multi-Page Applications

Other JavaScript Applications

#### Single-Page Applications

There are special considerations for integrating a single-page application (SPA) with Spring Security’s CSRF protection.

Recall that Spring Security provides BREACH protection of the CsrfToken by default.
When storing the expected CSRF token in a cookie, JavaScript applications will only have access to the plain token value and will not have access to the encoded value.
A customized request handler for resolving the actual token value will need to be provided.

In addition, the cookie storing the CSRF token will be cleared upon authentication success and logout success.
Spring Security defers loading a new CSRF token by default, and additional work is required to return a fresh cookie.

Refreshing the token after authentication success and logout success is required because the CsrfAuthenticationStrategy and CsrfLogoutHandler will clear the previous token.
The client application will not be able to perform an unsafe HTTP request, such as a POST, without obtaining a fresh token.

In order to easily integrate a single-page application with Spring Security, the following configuration can be used:

Configure CSRF for Single-Page Application

Java

Kotlin

XML

@Configuration
@EnableWebSecurity
public class SecurityConfig {

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
http
// ...
.csrf((csrf) -> csrf.spa());
return http.build();
}
}

import org.springframework.security.config.annotation.web.invoke

@Configuration
@EnableWebSecurity
class SecurityConfig {

@Bean
open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
http {
// ...
csrf {
spa()
}
}
return http.build()
}
}

<http>
<!-- ... -->
<csrf>
<spa />
</csrf>
</http>

#### Multi-Page Applications

For multi-page applications where JavaScript is loaded on each page, an alternative to exposing the CSRF token in a cookie is to include the CSRF token within your meta tags.
The HTML might look something like this:

CSRF Token in HTML Meta Tag

<html>
<head>
<meta name="_csrf" content="4bfd1575-3ad1-4d21-96c7-4ef2d9f86721"/>
<meta name="_csrf_header" content="X-CSRF-TOKEN"/>
<!-- ... -->
</head>
<!-- ... -->
</html>

In order to include the CSRF token in the request, you can take advantage of the fact that the CsrfToken is exposed as an HttpServletRequest attribute named _csrf.
The following example does this with a JSP:

CSRF Token in HTML Meta Tag with Request Attribute

<html>
<head>
<meta name="_csrf" content="${_csrf.token}"/>
<!-- default header name is X-CSRF-TOKEN -->
<meta name="_csrf_header" content="${_csrf.headerName}"/>
<!-- ... -->
</head>
<!-- ... -->
</html>

Once the meta tags contain the CSRF token, the JavaScript code can read the meta tags and include the CSRF token as a header.
If you use jQuery, you can do this with the following code:

Include CSRF Token in AJAX Request

$(function () {
var token = $("meta[name='_csrf']").attr("content");
var header = $("meta[name='_csrf_header']").attr("content");
$(document).ajaxSend(function(e, xhr, options) {
xhr.setRequestHeader(header, token);
});
});

#### Other JavaScript Applications

Another option for JavaScript applications is to include the CSRF token in an HTTP response header.

One way to achieve this is through the use of a @ControllerAdvice with the CsrfTokenArgumentResolver.
The following is an example of @ControllerAdvice that applies to all controller endpoints in the application:

CSRF Token in HTTP Response Header

Java

Kotlin

@ControllerAdvice
public class CsrfControllerAdvice {

@ModelAttribute
public void getCsrfToken(HttpServletResponse response, CsrfToken csrfToken) {
response.setHeader(csrfToken.getHeaderName(), csrfToken.getToken());
}

}

@ControllerAdvice
class CsrfControllerAdvice {

@ModelAttribute
fun getCsrfToken(response: HttpServletResponse, csrfToken: CsrfToken) {
response.setHeader(csrfToken.headerName, csrfToken.token)
}

}

Because this @ControllerAdvice applies to all endpoints in the application, it will cause the CSRF token to be loaded on every request, which can negate the benefits of deferred tokens when using the HttpSessionCsrfTokenRepository.
However, this is not usually an issue when using the CookieCsrfTokenRepository.

It is important to remember that controller endpoints and controller advice are called after the Spring Security filter chain.
This means that this @ControllerAdvice will only be applied if the request passes through the filter chain to your application.
See the configuration for single-page applications for an example of adding a filter to the filter chain for earlier access to the HttpServletResponse.

The CSRF token will now be available in a response header (X-CSRF-TOKEN or X-XSRF-TOKEN by default) for any custom endpoints the controller advice applies to.
Any request to the backend can be used to obtain the token from the response, and a subsequent request can include the token in a request header with the same name.

### Mobile Applications

Like JavaScript applications, mobile applications typically use JSON instead of HTML.
A backend application that does not serve browser traffic may choose to disable CSRF.
In that case, no additional work is required.

However, a backend application that also serves browser traffic and therefore still requires CSRF protection may continue to store the CsrfToken in the session instead of in a cookie.

In this case, a typical pattern for integrating with the backend is to expose a /csrf endpoint to allow the frontend (mobile or browser client) to request a CSRF token on demand.
The benefit of using this pattern is that the CSRF token can continue to be deferred and only needs to be loaded from the session when a request requires CSRF protection.
The use of a custom endpoint also means the client application can request that a new token be generated on demand (if necessary) by issuing an explicit request.

This pattern can be used for any type of application that requires CSRF protection, not just mobile applications.
While this approach isn’t typically required in those cases, it is another option for integrating with a CSRF-protected backend.

The following is an example of the /csrf endpoint that makes use of the CsrfTokenArgumentResolver:

The /csrf endpoint

Java

Kotlin

@RestController
public class CsrfController {

@GetMapping("/csrf")
public CsrfToken csrf(CsrfToken csrfToken) {
return csrfToken;
}

}

@RestController
class CsrfController {

@GetMapping("/csrf")
fun csrf(csrfToken: CsrfToken): CsrfToken {
return csrfToken
}

}

You may consider adding .requestMatchers("/csrf").permitAll() if the endpoint above is required prior to authenticating with the server.

This endpoint should be called to obtain a CSRF token when the application is launched or initialized (e.g. at load time), and also after authentication success and logout success.

Refreshing the token after authentication success and logout success is required because the CsrfAuthenticationStrategy and CsrfLogoutHandler will clear the previous token.
The client application will not be able to perform an unsafe HTTP request, such as a POST, without obtaining a fresh token.

Once you’ve obtained the CSRF token, you will need to include it as an HTTP request header (one of X-CSRF-TOKEN or X-XSRF-TOKEN by default) yourself.

## Handle AccessDeniedException

To handle an AccessDeniedException such as InvalidCsrfTokenException, you can configure Spring Security to handle these exceptions in any way you like.
For example, you can configure a custom access denied page using the following configuration:

Configure AccessDeniedHandler

Java

Kotlin

XML

@Configuration
@EnableWebSecurity
public class SecurityConfig {

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
http
// ...
.exceptionHandling((exceptionHandling) -> exceptionHandling
.accessDeniedPage("/access-denied")
);
return http.build();
}
}

import org.springframework.security.config.annotation.web.invoke

@Configuration
@EnableWebSecurity
class SecurityConfig {

@Bean
open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
http {
// ...
exceptionHandling {
accessDeniedPage = "/access-denied"
}
}
return http.build()
}
}

<http>
<!-- ... -->
<access-denied-handler error-page="/access-denied"/>
</http>

## CSRF Testing

You can use Spring Security’s testing support and CsrfRequestPostProcessor to test CSRF protection, like this:

Test CSRF Protection

Java

Kotlin

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SecurityConfig.class)
@WebAppConfiguration
public class CsrfTests {

private MockMvc mockMvc;

@BeforeEach
public void setUp(WebApplicationContext applicationContext) {
this.mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
.apply(springSecurity())
.build();
}

@Test
public void loginWhenValidCsrfTokenThenSuccess() throws Exception {
this.mockMvc.perform(post("/login").with(csrf())
.accept(MediaType.TEXT_HTML)
.param("username", "user")
.param("password", "password"))
.andExpect(status().is3xxRedirection())
.andExpect(header().string(HttpHeaders.LOCATION, "/"));
}

@Test
public void loginWhenInvalidCsrfTokenThenForbidden() throws Exception {
this.mockMvc.perform(post("/login").with(csrf().useInvalidToken())
.accept(MediaType.TEXT_HTML)
.param("username", "user")
.param("password", "password"))
.andExpect(status().isForbidden());
}

@Test
public void loginWhenMissingCsrfTokenThenForbidden() throws Exception {
this.mockMvc.perform(post("/login")
.accept(MediaType.TEXT_HTML)
.param("username", "user")
.param("password", "password"))
.andExpect(status().isForbidden());
}

@Test
@WithMockUser
public void logoutWhenValidCsrfTokenThenSuccess() throws Exception {
this.mockMvc.perform(post("/logout").with(csrf())
.accept(MediaType.TEXT_HTML))
.andExpect(status().is3xxRedirection())
.andExpect(header().string(HttpHeaders.LOCATION, "/login?logout"));
}
}

import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [SecurityConfig::class])
@WebAppConfiguration
class CsrfTests {
private lateinit var mockMvc: MockMvc

@BeforeEach
fun setUp(applicationContext: WebApplicationContext) {
mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
.apply<DefaultMockMvcBuilder>(springSecurity())
.build()
}

@Test
fun loginWhenValidCsrfTokenThenSuccess() {
mockMvc.perform(post("/login").with(csrf())
.accept(MediaType.TEXT_HTML)
.param("username", "user")
.param("password", "password"))
.andExpect(status().is3xxRedirection)
.andExpect(header().string(HttpHeaders.LOCATION, "/"))
}

@Test
fun loginWhenInvalidCsrfTokenThenForbidden() {
mockMvc.perform(post("/login").with(csrf().useInvalidToken())
.accept(MediaType.TEXT_HTML)
.param("username", "user")
.param("password", "password"))
.andExpect(status().isForbidden)
}

@Test
fun loginWhenMissingCsrfTokenThenForbidden() {
mockMvc.perform(post("/login")
.accept(MediaType.TEXT_HTML)
.param("username", "user")
.param("password", "password"))
.andExpect(status().isForbidden)
}

@Test
@WithMockUser
@Throws(Exception::class)
fun logoutWhenValidCsrfTokenThenSuccess() {
mockMvc.perform(post("/logout").with(csrf())
.accept(MediaType.TEXT_HTML))
.andExpect(status().is3xxRedirection)
.andExpect(header().string(HttpHeaders.LOCATION, "/login?logout"))
}
}

## Disable CSRF Protection

By default, CSRF protection is enabled, which affects integrating with the backend and testing your application.
Before disabling CSRF protection, consider whether it makes sense for your application.

You can also consider whether only certain endpoints do not require CSRF protection and configure an ignoring rule, as in the following example:

Ignoring Requests

Java

Kotlin

XML

@Configuration
@EnableWebSecurity
public class SecurityConfig {

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
http
// ...
.csrf((csrf) -> csrf
.ignoringRequestMatchers("/api/*")
);
return http.build();
}
}

import org.springframework.security.config.annotation.web.invoke

@Configuration
@EnableWebSecurity
class SecurityConfig {

@Bean
open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
http {
// ...
csrf {
ignoringRequestMatchers("/api/*")
}
}
return http.build()
}
}

<http>
<!-- ... -->
<csrf request-matcher-ref="csrfMatcher"/>
</http>
<b:bean id="csrfMatcher"
class="org.springframework.security.web.util.matcher.AndRequestMatcher">
<b:constructor-arg value="#{T(org.springframework.security.web.csrf.CsrfFilter).DEFAULT_CSRF_MATCHER}"/>
<b:constructor-arg>
<b:bean class="org.springframework.security.web.util.matcher.NegatedRequestMatcher">
<b:bean class="org.springframework.security.config.http.PathPatternRequestMatcherFactoryBean">
<b:constructor-arg value="/api/*"/>
</b:bean>
</b:bean>
</b:constructor-arg>
</b:bean>

If you need to disable CSRF protection, you can do so using the following configuration:

Disable CSRF

Java

Kotlin

XML

@Configuration
@EnableWebSecurity
public class SecurityConfig {

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
http
// ...
.csrf((csrf) -> csrf.disable());
return http.build();
}
}

import org.springframework.security.config.annotation.web.invoke

@Configuration
@EnableWebSecurity
class SecurityConfig {

@Bean
open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
http {
// ...
csrf {
disable()
}
}
return http.build()
}
}

<http>
<!-- ... -->
<csrf disabled="true"/>
</http>

## CSRF Considerations

There are a few special considerations when implementing protection against CSRF attacks.
This section discusses those considerations as they pertain to servlet environments.
See CSRF Considerations for a more general discussion.

### Logging In

It is important to require CSRF for log in requests to protect against forging log in attempts.
Spring Security’s servlet support does this out of the box.

### Logging Out

It is important to require CSRF for log out requests to protect against forging logout attempts.
If CSRF protection is enabled (the default), Spring Security’s LogoutFilter will only process HTTP POST requests.
This ensures that logging out requires a CSRF token and that a malicious user cannot forcibly log your users out.

The easiest approach is to use a form to log the user out.
If you really want a link, you can use JavaScript to have the link perform a POST (maybe on a hidden form).
For browsers with JavaScript that is disabled, you can optionally have the link take the user to a log out confirmation page that performs the POST.

If you really want to use HTTP GET with logout, you can do so.
However, remember that this is generally not recommended.
For example, the following logs out when the /logout URL is requested with any HTTP method:

Log Out with Any HTTP Method

Java

Kotlin

@Configuration
@EnableWebSecurity
public class SecurityConfig {

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
http
// ...
.logout((logout) -> logout
.logoutRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher("/logout"))
);
return http.build();
}
}

import org.springframework.security.config.annotation.web.invoke

@Configuration
@EnableWebSecurity
class SecurityConfig {

@Bean
open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
http {
// ...
logout {
logoutRequestMatcher = PathPatternRequestMatcher.withDefaults().matcher("/logout")
}
}
return http.build()
}
}

See the Logout chapter for more information.

### CSRF and Session Timeouts

By default, Spring Security stores the CSRF token in the HttpSession using the HttpSessionCsrfTokenRepository.
This can lead to a situation where the session expires, leaving no CSRF token to validate against.

We have already discussed general solutions to session timeouts.
This section discusses the specifics of CSRF timeouts as it pertains to the servlet support.

You can change the storage of the CSRF token to be in a cookie.
For details, see the Using the CookieCsrfTokenRepository section.

If a token does expire, you might want to customize how it is handled by specifying a custom AccessDeniedHandler.
The custom AccessDeniedHandler can process the InvalidCsrfTokenException any way you like.

### Multipart (file upload)

We have already discussed how protecting multipart requests (file uploads) from CSRF attacks causes a chicken and the egg problem.
When JavaScript is available, we recommend including the CSRF token in an HTTP request header to side-step the issue.

If JavaScript is not available, the following sections discuss options for placing the CSRF token in the body and url within a servlet application.

You can find more information about using multipart forms with Spring in the Multipart Resolver section of the Spring reference and the MultipartFilter javadoc.

#### Place CSRF Token in the Body

We have already discussed the tradeoffs of placing the CSRF token in the body.
In this section, we discuss how to configure Spring Security to read the CSRF from the body.

To read the CSRF token from the body, the MultipartFilter is specified before the Spring Security filter.
Specifying the MultipartFilter before the Spring Security filter means that there is no authorization for invoking the MultipartFilter, which means anyone can place temporary files on your server.
However, only authorized users can submit a file that is processed by your application.
In general, this is the recommended approach because the temporary file upload should have a negligible impact on most servers.

Configure MultipartFilter

Java

Kotlin

XML

public class SecurityApplicationInitializer extends AbstractSecurityWebApplicationInitializer {

@Override
protected void beforeSpringSecurityFilterChain(ServletContext servletContext) {
insertFilters(servletContext, new MultipartFilter());
}
}

class SecurityApplicationInitializer : AbstractSecurityWebApplicationInitializer() {
override fun beforeSpringSecurityFilterChain(servletContext: ServletContext?) {
insertFilters(servletContext, MultipartFilter())
}
}

<filter>
<filter-name>MultipartFilter</filter-name>
<filter-class>org.springframework.web.multipart.support.MultipartFilter</filter-class>
</filter>
<filter>
<filter-name>springSecurityFilterChain</filter-name>
<filter-class>org.springframework.web.filter.DelegatingFilterProxy</filter-class>
</filter>
<filter-mapping>
<filter-name>MultipartFilter</filter-name>
<url-pattern>/*</url-pattern>
</filter-mapping>
<filter-mapping>
<filter-name>springSecurityFilterChain</filter-name>
<url-pattern>/*</url-pattern>
</filter-mapping>

To ensure that MultipartFilter is specified before the Spring Security filter with XML configuration, you can ensure the <filter-mapping> element of the MultipartFilter is placed before the springSecurityFilterChain within the web.xml file.

#### Include a CSRF Token in a URL

If letting unauthorized users upload temporary files is not acceptable, an alternative is to place the MultipartFilter after the Spring Security filter and include the CSRF as a query parameter in the action attribute of the form.
Since the CsrfToken is exposed as an HttpServletRequest attribute named _csrf, we can use that to create an action with the CSRF token in it.
The following example does this with a JSP:

CSRF Token in Action

<form method="post"
action="./upload?${_csrf.parameterName}=${_csrf.token}"
enctype="multipart/form-data">

### HiddenHttpMethodFilter

We have already discussed the trade-offs of placing the CSRF token in the body.

In Spring’s Servlet support, overriding the HTTP method is done by using HiddenHttpMethodFilter.
You can find more information in the HTTP Method Conversion section of the reference documentation.

## Further Reading

Now that you have reviewed CSRF protection, consider learning more about exploit protection including secure headers and the HTTP firewall or move on to learning how to test your application.

Spring Security

Stable

7.1.0

7.0.6

6.5.11

Snapshot

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

Copyright © 2005 - Broadcom. All Rights Reserved. The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.

Terms of Use • Privacy • Trademark Guidelines • Thank you • Your California Privacy Rights • Cookie Settings

Apache®, Apache Tomcat®, Apache Kafka®, Apache Cassandra™, and Apache Geode™ are trademarks or registered trademarks of the Apache Software Foundation in the United States and/or other countries. Java™, Java™ SE, Java™ EE, and OpenJDK™ are trademarks of Oracle and/or its affiliates. Kubernetes® is a registered trademark of the Linux Foundation in the United States and other countries. Linux® is the registered trademark of Linus Torvalds in the United States and other countries. Windows® and Microsoft® Azure are registered trademarks of Microsoft Corporation. “AWS” and “Amazon Web Services” are trademarks or registered trademarks of Amazon.com Inc. or its affiliates. All other trademarks and copyrights are property of their respective owners and are only mentioned for informative purposes. Other names may be trademarks of their respective owners.

Search in all Spring Docs
