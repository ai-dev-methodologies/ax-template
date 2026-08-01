# spring-mvc-requestmapping — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T02:24:31Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r148`
**Body SHA-256 (below the `---` divider, header excluded):** 669c190a900f00ad4ba725162068009b475518c4a70805bca3054d34b26195be

---

---
snapshot_id: spring-mvc-requestmapping
source: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 3
bytes: 34995
sha: "3c23de9ff85be22fcd377e9b504f2af0b925bbee0f725477f1ba84751a38b068"
---

# spring mvc requestmapping — upstream snapshot

Source: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html
Fetched: 2026-07-14

Mapping Requests :: Spring Framework

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

Spring Framework7.0.8
Search

Overview

Core Technologies

The IoC Container

Introduction to the Spring IoC Container and Beans

Container Overview

Bean Overview

Dependencies

Dependency Injection

Dependencies and Configuration in Detail

Using depends-on

Lazy-initialized Beans

Autowiring Collaborators

Method Injection

Bean Scopes

Customizing the Nature of a Bean

Bean Definition Inheritance

Container Extension Points

Annotation-based Container Configuration

Using @Autowired

Fine-tuning Annotation-based Autowiring with @Primary or @Fallback

Fine-tuning Annotation-based Autowiring with Qualifiers

Using Generics as Autowiring Qualifiers

Using CustomAutowireConfigurer

Injection with @Resource

Using @Value

Using @PostConstruct and @PreDestroy

Classpath Scanning and Managed Components

Using JSR-330 Standard Annotations

Java-based Container Configuration

Basic Concepts: @Bean and @Configuration

Instantiating the Spring Container by Using AnnotationConfigApplicationContext

Using the @Bean Annotation

Using the @Configuration annotation

Composing Java-based Configurations

Programmatic Bean Registration

Environment Abstraction

Registering a LoadTimeWeaver

Additional Capabilities of the ApplicationContext

The BeanFactory API

Resources

Validation, Data Binding, and Type Conversion

Validation Using Spring’s Validator Interface

Data Binding

Resolving Error Codes to Error Messages

Spring Type Conversion

Spring Field Formatting

Configuring a Global Date and Time Format

Java Bean Validation

Spring Expression Language (SpEL)

Evaluation

Expressions in Bean Definitions

Language Reference

Literal Expressions

Properties, Arrays, Lists, Maps, and Indexers

Inline Lists

Inline Maps

Array Construction

Methods

Operators

Types

Constructors

Variables

Functions

Varargs Invocations

Bean References

Ternary Operator (If-Then-Else)

The Elvis Operator

Safe Navigation Operator

Collection Selection

Collection Projection

Expression Templating

Classes Used in the Examples

Aspect Oriented Programming with Spring

AOP Concepts

Spring AOP Capabilities and Goals

AOP Proxies

@AspectJ support

Enabling @AspectJ Support

Declaring an Aspect

Declaring a Pointcut

Declaring Advice

Introductions

Aspect Instantiation Models

An AOP Example

Schema-based AOP Support

Choosing which AOP Declaration Style to Use

Mixing Aspect Types

Proxying Mechanisms

Programmatic Creation of @AspectJ Proxies

Using AspectJ with Spring Applications

Further Resources

Spring AOP APIs

Pointcut API in Spring

Advice API in Spring

The Advisor API in Spring

Using the ProxyFactoryBean to Create AOP Proxies

Concise Proxy Definitions

Creating AOP Proxies Programmatically with the ProxyFactory

Manipulating Advised Objects

Using the "auto-proxy" facility

Using TargetSource Implementations

Defining New Advice Types

Resilience Features

Null-safety

Data Buffers and Codecs

Ahead of Time Optimizations

Appendix

XML Schemas

XML Schema Authoring

Application Startup Steps

Data Access

Transaction Management

Advantages of the Spring Framework’s Transaction Support Model

Understanding the Spring Framework Transaction Abstraction

Synchronizing Resources with Transactions

Declarative Transaction Management

Understanding the Spring Framework’s Declarative Transaction Implementation

Example of Declarative Transaction Implementation

Rolling Back a Declarative Transaction

Configuring Different Transactional Semantics for Different Beans

<tx:advice/> Settings

Using @Transactional

Transaction Propagation

Advising Transactional Operations

Using @Transactional with AspectJ

Programmatic Transaction Management

Choosing Between Programmatic and Declarative Transaction Management

Transaction-bound Events

Application server-specific integration

Solutions to Common Problems

Further Resources

DAO Support

Data Access with JDBC

Choosing an Approach for JDBC Database Access

Package Hierarchy

Using the JDBC Core Classes to Control Basic JDBC Processing and Error Handling

Controlling Database Connections

JDBC Batch Operations

Simplifying JDBC Operations with the SimpleJdbc Classes

Modeling JDBC Operations as Java Objects

Common Problems with Parameter and Data Value Handling

Embedded Database Support

Initializing a DataSource

Data Access with R2DBC

Object Relational Mapping (ORM) Data Access

Introduction to ORM with Spring

General ORM Integration Considerations

Hibernate

JPA

Marshalling XML by Using Object-XML Mappers

Appendix

Web on Servlet Stack

Spring Web MVC

DispatcherServlet

Context Hierarchy

Special Bean Types

Web MVC Config

Servlet Config

Processing

Path Matching

Interception

Exceptions

View Resolution

Locale

Multipart Resolver

Logging

Filters

HTTP Message Conversion

Annotated Controllers

Declaration

Mapping Requests

Handler Methods

Method Arguments

Return Values

Type Conversion

Matrix Variables

@RequestParam

@RequestHeader

@CookieValue

@ModelAttribute

@SessionAttributes

@SessionAttribute

@RequestAttribute

Redirect Attributes

Flash Attributes

Multipart

@RequestBody

HttpEntity

@ResponseBody

ResponseEntity

Jackson JSON

Model

@InitBinder

Validation

Exceptions

Controller Advice

Functional Endpoints

URI Links

Asynchronous Requests

Range Requests

Data Binding

CORS

API Versioning

Error Responses

Web Security

HTTP Caching

View Technologies

Thymeleaf

FreeMarker

Groovy Markup

Script Views

HTML Fragments

JSP and JSTL

RSS and Atom

PDF and Excel

Jackson

XML Marshalling

XSLT Views

MVC Config

Enable MVC Configuration

MVC Config API

Type Conversion

Validation

Interceptors

Content Types

Message Converters

View Controllers

View Resolvers

Static Resources

Default Servlet

Path Matching

API Version

Advanced Java Config

Advanced XML Config

HTTP/2

REST Clients

Testing

WebSockets

WebSocket API

SockJS Fallback

STOMP

Overview

Benefits

Enable STOMP

WebSocket Transport

Flow of Messages

Annotated Controllers

Sending Messages

Simple Broker

External Broker

Connecting to a Broker

Dots as Separators

Authentication

Token Authentication

Authorization

User Destinations

Order of Messages

Events

Interception

STOMP Client

WebSocket Scope

Performance

Monitoring

Testing

Web on Reactive Stack

Spring WebFlux

Overview

Reactive Core

DispatcherHandler

Annotated Controllers

@Controller

Mapping Requests

Handler Methods

Method Arguments

Return Values

Type Conversion

Matrix Variables

@RequestParam

@RequestHeader

@CookieValue

@ModelAttribute

@SessionAttributes

@SessionAttribute

@RequestAttribute

Multipart Content

@RequestBody

HttpEntity

@ResponseBody

ResponseEntity

Jackson JSON

Model

DataBinder

Validation

Exceptions

Controller Advice

Functional Endpoints

URI Links

Range Requests

Data Binding

CORS

API Versioning

Error Responses

Web Security

HTTP Caching

View Technologies

WebFlux Config

HTTP/2

WebClient

Configuration

retrieve()

Exchange

Request Body

Filters

Attributes

Context

Synchronous Use

Testing

HTTP Service Client

WebSockets

Testing

RSocket

Reactive Libraries

Testing

Introduction to Spring Testing

Unit Testing

Integration Testing

JDBC Testing Support

Spring TestContext Framework

Key Abstractions

Bootstrapping the TestContext Framework

TestExecutionListener Configuration

Application Events

Test Execution Events

Context Management

Context Configuration with Component Classes

Context Configuration with XML Resources

Context Configuration with Groovy Scripts

Default Context Configuration

Mixing Component Classes, XML, and Groovy Scripts

Context Configuration with Context Customizers

Context Configuration with Context Initializers

Context Configuration Inheritance

Context Configuration with Environment Profiles

Context Configuration with Test Property Sources

Context Configuration with Dynamic Property Sources

Loading a WebApplicationContext

Working with Web Mocks

Context Caching

Context Pausing

Context Failure Threshold

Context Hierarchies

Dependency Injection of Test Fixtures

Bean Overriding in Tests

Testing Request- and Session-scoped Beans

Transaction Management

Executing SQL Scripts

Parallel Test Execution

TestContext Framework Support Classes

Ahead of Time Support for Tests

WebTestClient

RestTestClient

MockMvc

Overview

Setup Options

Hamcrest Integration

Static Imports

Configuring MockMvc

Setup Features

Performing Requests

Defining Expectations

Async Requests

Streaming Responses

Filter Registrations

AssertJ Integration

Configuring MockMvcTester

Performing Requests

Defining Expectations

MockMvc integration

HtmlUnit Integration

Why HtmlUnit Integration?

MockMvc and HtmlUnit

MockMvc and WebDriver

MockMvc and Geb

MockMvc vs End-to-End Tests

Further Examples

Testing Client Applications

Appendix

Annotations

Standard Annotation Support

Spring Testing Annotations

@BootstrapWith

@ContextConfiguration

@WebAppConfiguration

@ContextHierarchy

@ContextCustomizerFactories

@ActiveProfiles

@TestPropertySource

@DynamicPropertySource

@TestBean

@MockitoBean and @MockitoSpyBean

@DirtiesContext

@TestExecutionListeners

@RecordApplicationEvents

@Commit

@Rollback

@BeforeTransaction

@AfterTransaction

@Sql

@SqlConfig

@SqlMergeMode

@SqlGroup

@DisabledInAotMode

Spring JUnit 4 Testing Annotations

Spring JUnit Jupiter Testing Annotations

Meta-Annotation Support for Testing

Further Resources

Integration

REST Clients

JMS (Java Message Service)

Using Spring JMS

Sending a Message

Receiving a Message

Support for JCA Message Endpoints

Annotation-driven Listener Endpoints

JMS Namespace Support

JMX

Exporting Your Beans to JMX

Controlling the Management Interface of Your Beans

Controlling ObjectName Instances for Your Beans

Using JSR-160 Connectors

Accessing MBeans through Proxies

Notifications

Further Resources

Email

Task Execution and Scheduling

Cache Abstraction

Understanding the Cache Abstraction

Declarative Annotation-based Caching

JCache (JSR-107) Annotations

Declarative XML-based Caching

Configuring the Cache Storage

Plugging-in Different Back-end Caches

How can I Set the TTL/TTI/Eviction policy/XXX feature?

Observability Support

JVM AOT Cache

JVM Checkpoint Restore

Appendix

Language Support

Kotlin

Requirements

Extensions

Null-safety

Classes and Interfaces

Annotations

Bean Registration DSL

Web

Coroutines

Spring Projects in Kotlin

Getting Started

Resources

Apache Groovy

Appendix

Java API

Kotlin API

Wiki

Search

Edit this Page

GitHub Project

Stack Overflow

Spring Framework

Web on Servlet Stack

Spring Web MVC

Annotated Controllers

Mapping Requests

# Mapping Requests

See equivalent in the Reactive stack

This section discusses request mapping for annotated controllers.

## @RequestMapping

See equivalent in the Reactive stack

You can use the @RequestMapping annotation to map requests to controllers methods. It has
various attributes to match by URL, HTTP method, request parameters, headers, and media
types. You can use it at the class level to express shared mappings or at the method level
to narrow down to a specific endpoint mapping.

There are also HTTP method specific shortcut variants of @RequestMapping:

@GetMapping

@PostMapping

@PutMapping

@DeleteMapping

@PatchMapping

The shortcuts are
Custom Annotations
that are provided because, arguably, most controller methods should be mapped to a specific
HTTP method versus using @RequestMapping, which, by default, matches to all HTTP methods.
A @RequestMapping is still needed at the class level to express shared mappings.

@RequestMapping cannot be used in conjunction with other @RequestMapping
annotations that are declared on the same element (class, interface, or method). If
multiple @RequestMapping annotations are detected on the same element, a warning will
be logged, and only the first mapping will be used. This also applies to composed
@RequestMapping annotations such as @GetMapping, @PostMapping, etc.

The following example has type and method level mappings:

Java

Kotlin

@RestController
@RequestMapping("/persons")
class PersonController {

@GetMapping("/{id}")
public Person getPerson(@PathVariable Long id) {
// ...
}

@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public void add(@RequestBody Person person) {
// ...
}
}

@RestController
@RequestMapping("/persons")
class PersonController {

@GetMapping("/{id}")
fun getPerson(@PathVariable id: Long): Person {
// ...
}

@PostMapping
@ResponseStatus(HttpStatus.CREATED)
fun add(@RequestBody person: Person) {
// ...
}
}

## URI patterns

See equivalent in the Reactive stack

@RequestMapping methods can be mapped using URL patterns.
Spring MVC is using PathPattern — a pre-parsed pattern matched against the URL path also pre-parsed as PathContainer.
Designed for web use, this solution deals effectively with encoding and path parameters, and matches efficiently.
See MVC config for customizations of path matching options.

the AntPathMatcher variant is now deprecated because it is less efficient and the String path input is a
challenge for dealing effectively with encoding and other issues with URLs.

You can map requests by using glob patterns and wildcards:

PatternDescriptionExample

spring

Literal pattern

"/spring" matches "/spring"

?

Matches one character

"/pages/t?st.html" matches "/pages/test.html" and "/pages/t3st.html"

*

Matches zero or more characters within a path segment

"/resources/*.png" matches "/resources/file.png"

"/projects/*/versions" matches "/projects/spring/versions" but does not match "/projects/spring/boot/versions".

"/projects/*" matches "/projects/spring" but does not match "/projects" as the path segment is not present.

**

Matches zero or more path segments

"/resources/**" matches "/resources", "/resources/file.png" and "/resources/images/file.png"

"/**/info" matches "/info", "/spring/info" and "/spring/framework/info"

"/resources/**/file.png" is invalid as ** is not allowed in the middle of the path.

"/**/spring/**" is not allowed, as only a single **/{*path} instance is allowed per pattern.

{name}

Similar to *, but also captures the path segment as a variable named "name"

"/projects/{project}/versions" matches "/projects/spring/versions" and captures project=spring

"/projects/{project}/versions" does not match "/projects/spring/framework/versions" as it captures a single path segment.

{name:[a-z]+}

Matches the regexp "[a-z]+" as a path variable named "name"

"/projects/{project:[a-z]+}/versions" matches "/projects/spring/versions" but not "/projects/spring1/versions"

{*path}

Similar to **, but also captures the path segments as a variable named "path"

"/resources/{*file}" matches "/resources/images/file.png" and captures file=/images/file.png

"{*path}/resources" matches "/spring/framework/resources" and captures path=/spring/framework

"/resources/{*path}/file.png" is invalid as {*path} is not allowed in the middle of the path.

"/{*path}/spring/**" is not allowed, as only a single **/{*path} instance is allowed per pattern.

Captured URI variables can be accessed with @PathVariable. For example:

Java

Kotlin

@GetMapping("/owners/{ownerId}/pets/{petId}")
public Pet findPet(@PathVariable Long ownerId, @PathVariable Long petId) {
// ...
}

@GetMapping("/owners/{ownerId}/pets/{petId}")
fun findPet(@PathVariable ownerId: Long, @PathVariable petId: Long): Pet {
// ...
}

You can declare URI variables at the class and method levels, as the following example shows:

Java

Kotlin

@Controller
@RequestMapping("/owners/{ownerId}")
public class OwnerController {

@GetMapping("/pets/{petId}")
public Pet findPet(@PathVariable Long ownerId, @PathVariable Long petId) {
// ...
}
}

@Controller
@RequestMapping("/owners/{ownerId}")
class OwnerController {

@GetMapping("/pets/{petId}")
fun findPet(@PathVariable ownerId: Long, @PathVariable petId: Long): Pet {
// ...
}
}

URI variables are automatically converted to the appropriate type, or TypeMismatchException
is raised. Simple types (int, long, Date, and so on) are supported by default and you can
register support for any other data type.
See Type Conversion and DataBinder.

You can explicitly name URI variables (for example, @PathVariable("customId")), but you can
leave that detail out if the names are the same and your code is compiled with the -parameters
compiler flag.

The syntax {varName:regex} declares a URI variable with a regular expression that has
syntax of {varName:regex}. For example, given URL "/spring-web-3.0.5.jar", the following method
extracts the name, version, and file extension:

Java

Kotlin

@GetMapping("/{name:[a-z-]+}-{version:\\d\\.\\d\\.\\d}{ext:\\.[a-z]+}")
public void handle(@PathVariable String name, @PathVariable String version, @PathVariable String ext) {
// ...
}

@GetMapping("/{name:[a-z-]+}-{version:\\d\\.\\d\\.\\d}{ext:\\.[a-z]+}")
fun handle(@PathVariable name: String, @PathVariable version: String, @PathVariable ext: String) {
// ...
}

URI path patterns can also have:

Embedded ${…​} placeholders that are resolved on startup via
PropertySourcesPlaceholderConfigurer against local, system, environment, and
other property sources. This is useful, for example, to parameterize a base URL based on
external configuration.

SpEL expression #{…​}.

## Pattern Comparison

See equivalent in the Reactive stack

When multiple patterns match a URL, the best match must be selected. This is done with
one of the following depending on whether use of parsed PathPattern is enabled for use or not:

PathPattern.SPECIFICITY_COMPARATOR

AntPathMatcher.getPatternComparator(String path)

Both help to sort patterns with more specific ones on top. A pattern is more specific if
it has a lower count of URI variables (counted as 1), single wildcards (counted as 1),
and double wildcards (counted as 2). Given an equal score, the longer pattern is chosen.
Given the same score and length, the pattern with more URI variables than wildcards is
chosen.

The default mapping pattern (/**) is excluded from scoring and always
sorted last. Also, prefix patterns (such as /public/**) are considered less
specific than other pattern that do not have double wildcards.

For the full details, follow the above links to the pattern Comparators.

## Suffix Match and RFD

A reflected file download (RFD) attack is similar to XSS in that it relies on request input
(for example, a query parameter and a URI variable) being reflected in the response. However, instead of
inserting JavaScript into HTML, an RFD attack relies on the browser switching to perform a
download and treating the response as an executable script when double-clicked later.

In Spring MVC, @ResponseBody and ResponseEntity methods are at risk, because
they can render different content types, which clients can request through URL path extensions.
Disabling suffix pattern matching and using path extensions for content negotiation
lower the risk but are not sufficient to prevent RFD attacks.

To prevent RFD attacks, prior to rendering the response body, Spring MVC adds a
Content-Disposition:inline;filename=f.txt header to suggest a fixed and safe download
file. This is done only if the URL path contains a file extension that is neither
allowed as safe nor explicitly registered for content negotiation. However, it can
potentially have side effects when URLs are typed directly into a browser.

Many common path extensions are allowed as safe by default. Applications with custom
HttpMessageConverter implementations can explicitly register file extensions for content
negotiation to avoid having a Content-Disposition header added for those extensions.
See Content Types.

See CVE-2015-5211 for additional
recommendations related to RFD.

## Consumable Media Types

See equivalent in the Reactive stack

You can narrow the request mapping based on the Content-Type of the request,
as the following example shows:

Java

Kotlin

@PostMapping(path = "/pets", consumes = "application/json") (1)
public void addPet(@RequestBody Pet pet) {
// ...
}

1Using a consumes attribute to narrow the mapping by the content type.

@PostMapping("/pets", consumes = ["application/json"]) (1)
fun addPet(@RequestBody pet: Pet) {
// ...
}

1Using a consumes attribute to narrow the mapping by the content type.

The consumes attribute also supports negation expressions — for example, !text/plain means any
content type other than text/plain.

You can declare a shared consumes attribute at the class level. Unlike most other
request-mapping attributes, however, when used at the class level, a method-level consumes attribute
overrides rather than extends the class-level declaration.

MediaType provides constants for commonly used media types, such as
APPLICATION_JSON_VALUE and APPLICATION_XML_VALUE.

## Producible Media Types

See equivalent in the Reactive stack

You can narrow the request mapping based on the Accept request header and the list of
content types that a controller method produces, as the following example shows:

Java

Kotlin

@GetMapping(path = "/pets/{petId}", produces = "application/json") (1)
@ResponseBody
public Pet getPet(@PathVariable String petId) {
// ...
}

1Using a produces attribute to narrow the mapping by the content type.

@GetMapping("/pets/{petId}", produces = ["application/json"]) (1)
@ResponseBody
fun getPet(@PathVariable petId: String): Pet {
// ...
}

1Using a produces attribute to narrow the mapping by the content type.

The media type can specify a character set. Negated expressions are supported — for example,
!text/plain means any content type other than "text/plain".

You can declare a shared produces attribute at the class level. Unlike most other
request-mapping attributes, however, when used at the class level, a method-level produces attribute
overrides rather than extends the class-level declaration.

MediaType provides constants for commonly used media types, such as
APPLICATION_JSON_VALUE and APPLICATION_XML_VALUE.

## Parameters, headers

See equivalent in the Reactive stack

You can narrow request mappings based on request parameter conditions. You can test for the
presence of a request parameter (myParam), for the absence of one (!myParam), or for a
specific value (myParam=myValue). The following example shows how to test for a specific value:

Java

Kotlin

@GetMapping(path = "/pets/{petId}", params = "myParam=myValue") (1)
public void findPet(@PathVariable String petId) {
// ...
}

1Testing whether myParam equals myValue.

@GetMapping("/pets/{petId}", params = ["myParam=myValue"]) (1)
fun findPet(@PathVariable petId: String) {
// ...
}

1Testing whether myParam equals myValue.

You can also use the same with request header conditions, as the following example shows:

Java

Kotlin

@GetMapping(path = "/pets/{petId}", headers = "myHeader=myValue") (1)
public void findPet(@PathVariable String petId) {
// ...
}

1Testing whether myHeader equals myValue.

@GetMapping("/pets/{petId}", headers = ["myHeader=myValue"]) (1)
fun findPet(@PathVariable petId: String) {
// ...
}

1Testing whether myHeader equals myValue.

You can match Content-Type and Accept with the headers condition, but it is better to use
consumes
and produces
instead.

## API Version

See equivalent in the Reactive stack

There is no standard way to specify an API version, so when you enable API versioning
in the MVC Config you need
to specify how to resolve the version. The MVC Config creates an
ApiVersionStrategy that in turn
is used to map requests.

Once API versioning is enabled, you can begin to map requests with versions.
The @RequestMapping version attribute supports the following:

Fixed version ("1.2") — matches the given version only

Baseline version ("1.2+") — matches the given and supported versions above

No value — matches any version, but is superseded by a more specific version match

If multiple controller methods have a version less than or equal to the request version,
the highest of those, and closest to the request version, is the one considered,
in effect superseding the rest.

To illustrate this, consider the following mappings:

Java

@RestController
@RequestMapping("/account/{id}")
public class AccountController {

@GetMapping (1)
public Account getAccount() {
}

@GetMapping(version = "1.1") (2)
public Account getAccount1_1() {
}

@GetMapping(version = "1.2+") (3)
public Account getAccount1_2() {
}

@GetMapping(version = "1.5") (4)
public Account getAccount1_5() {
}
}

1match any version

2match version 1.1

3match version 1.2 and supported versions above

4match version 1.5

For request with version "1.3":

(1) matches as it matches any version

(2) does not match

(3) matches as it matches 1.2 and above, and is chosen as the highest match

(4) is higher and does not match

Version 1.3 must be present in the mappings, or be
configured as supported.

For request with version "1.5":

(1) matches as it matches any version

(2) does not match

(3) matches as it matches 1.2 and above

(4) matches and is chosen as the highest match

A request with version "1.6" does not have a match. (1) and (3) do match, but are
superseded by (4), which allows only a strict match, and therefore does not match.
In this scenario, a NotAcceptableApiVersionException results in a 400 response.

Controller methods without a version are intended to support clients created before a
versioned alternative was introduced. Therefore, even though an unversioned controller
method is considered a match for any version, it is in fact given the lowest priority,
and is effectively superseded by any alternative controller method with a version.

See API Versioning for more details on underlying
infrastructure and support for API Versioning.

## HTTP HEAD, OPTIONS

See equivalent in the Reactive stack

@GetMapping (and @RequestMapping(method=HttpMethod.GET)) support HTTP HEAD
transparently for request mapping. Controller methods do not need to change.
A response wrapper, applied in jakarta.servlet.http.HttpServlet, ensures a Content-Length
header is set to the number of bytes written (without actually writing to the response).

By default, HTTP OPTIONS is handled by setting the Allow response header to the list of HTTP
methods listed in all @RequestMapping methods that have matching URL patterns.

For a @RequestMapping without HTTP method declarations, the Allow header is set to
GET,HEAD,POST,PUT,PATCH,DELETE,OPTIONS. Controller methods should always declare the
supported HTTP methods (for example, by using the HTTP method specific variants:
@GetMapping, @PostMapping, and others).

You can explicitly map the @RequestMapping method to HTTP HEAD and HTTP OPTIONS, but that
is not necessary in the common case.

## Custom Annotations

See equivalent in the Reactive stack

Spring MVC supports the use of composed annotations
for request mapping. Those are annotations that are themselves meta-annotated with
@RequestMapping and composed to redeclare a subset (or all) of the @RequestMapping
attributes with a narrower, more specific purpose.

@GetMapping, @PostMapping, @PutMapping, @DeleteMapping, and @PatchMapping are
examples of composed annotations. They are provided because, arguably, most
controller methods should be mapped to a specific HTTP method versus using @RequestMapping,
which, by default, matches to all HTTP methods. If you need an example of how to implement
a composed annotation, look at how those are declared.

@RequestMapping cannot be used in conjunction with other @RequestMapping
annotations that are declared on the same element (class, interface, or method). If
multiple @RequestMapping annotations are detected on the same element, a warning will
be logged, and only the first mapping will be used. This also applies to composed
@RequestMapping annotations such as @GetMapping, @PostMapping, etc.

Spring MVC also supports custom request-mapping attributes with custom request-matching
logic. This is a more advanced option that requires subclassing
RequestMappingHandlerMapping and overriding the getCustomMethodCondition method, where
you can check the custom attribute and return your own RequestCondition.

## Explicit Registrations

See equivalent in the Reactive stack

You can programmatically register handler methods, which you can use for dynamic
registrations or for advanced cases, such as different instances of the same handler
under different URLs. The following example registers a handler method:

Java

Kotlin

@Configuration
public class MyConfiguration {

// Inject the target handler and the handler mapping for controllers
@Autowired
public void setHandlerMapping(RequestMappingHandlerMapping mapping, UserHandler handler)
throws NoSuchMethodException {

// Prepare the request mapping meta data
RequestMappingInfo info = RequestMappingInfo
.paths("/user/{id}").methods(RequestMethod.GET).build();

// Get the handler method
Method method = UserHandler.class.getMethod("getUser", Long.class);

// Add the registration
mapping.registerMapping(info, handler, method);
}
}

@Configuration
class MyConfiguration {

// Inject the target handler and the handler mapping for controllers
@Autowired
fun setHandlerMapping(mapping: RequestMappingHandlerMapping, handler: UserHandler) {

// Get the handler method
val info = RequestMappingInfo.paths("/user/{id}").methods(RequestMethod.GET).build()

// Get the handler method
val method = UserHandler::class.java.getMethod("getUser", Long::class.java)

// Add the registration
mapping.registerMapping(info, handler, method)
}
}

## @HttpExchange

See equivalent in the Reactive stack

While the main purpose of @HttpExchange is to abstract HTTP client code with a
generated proxy, the interface on which such annotations are placed is a contract neutral
to client vs server use. In addition to simplifying client code, there are also cases
where an HTTP Service Client
may be a convenient way for servers to expose their API for client access. This leads
to increased coupling between client and server and is often not a good choice,
especially for public API’s, but may be exactly the goal for an internal API.
It is an approach commonly used in Spring Cloud, and it is why @HttpExchange is
supported as an alternative to @RequestMapping for server side handling in
controller classes.

For example:

Java

Kotlin

@HttpExchange("/persons")
interface PersonService {

@GetExchange("/{id}")
Person getPerson(@PathVariable Long id);

@PostExchange
void add(@RequestBody Person person);
}

@RestController
class PersonController implements PersonService {

public Person getPerson(@PathVariable Long id) {
// ...
}

@ResponseStatus(HttpStatus.CREATED)
public void add(@RequestBody Person person) {
// ...
}
}

@HttpExchange("/persons")
interface PersonService {

@GetExchange("/{id}")
fun getPerson(@PathVariable id: Long): Person

@PostExchange
fun add(@RequestBody person: Person)
}

@RestController
class PersonController : PersonService {

override fun getPerson(@PathVariable id: Long): Person {
// ...
}

@ResponseStatus(HttpStatus.CREATED)
override fun add(@RequestBody person: Person) {
// ...
}
}

@HttpExchange and @RequestMapping have differences.
@RequestMapping can map to any number of requests by path patterns, HTTP methods,
and more, while @HttpExchange declares a single endpoint with a concrete HTTP method,
path, and content types.

For method parameters and returns values, generally, @HttpExchange supports a
subset of the method parameters that @RequestMapping does. Notably, it excludes any
server-side specific parameter types. For details, see the list for
@HttpExchange and
@RequestMapping.

@HttpExchange also supports a headers() parameter which accepts "name=value"-like
pairs like in @RequestMapping(headers={}) on the client side. On the server side,
this extends to the full syntax that
@RequestMapping supports.

DeclarationHandler Methods

Spring Framework

Stable

7.0.8

6.2.19

Snapshot

7.1.0-SNAPSHOT

7.0.9-SNAPSHOT

6.2.20-SNAPSHOT

Related Spring Documentation

Spring Boot

Spring Framework

Spring Cloud

Spring Cloud Build

Spring Cloud Bus

Spring Cloud Circuit Breaker

Spring Cloud Commons

Spring Cloud Config

Spring Cloud Consul

Spring Cloud Contract

Spring Cloud Function

Spring Cloud Gateway

Spring Cloud Kubernetes

Spring Cloud Netflix

Spring Cloud OpenFeign

Spring Cloud Stream

Spring Cloud Task

Spring Cloud Vault

Spring Cloud Zookeeper

Spring Data

Spring Data Cassandra

Spring Data Commons

Spring Data Couchbase

Spring Data Elasticsearch

Spring Data JPA

Spring Data KeyValue

Spring Data LDAP

Spring Data MongoDB

Spring Data Neo4j

Spring Data Redis

Spring Data JDBC & R2DBC

Spring Data REST

Spring Integration

Spring Batch

Spring Security

Spring Authorization Server

Spring LDAP

Spring Security Kerberos

Spring Session

Spring Vault

Spring AI

Spring AMQP

Spring CLI

Spring GraphQL

Spring for Apache Kafka

Spring Modulith

Spring for Apache Pulsar

Spring Shell

All Docs...

Copyright © 2005 - Broadcom. All Rights Reserved. The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.

Terms of Use • Privacy • Trademark Guidelines • Thank you • Your California Privacy Rights • Cookie Settings

Apache®, Apache Tomcat®, Apache Kafka®, Apache Cassandra™, and Apache Geode™ are trademarks or registered trademarks of the Apache Software Foundation in the United States and/or other countries. Java™, Java™ SE, Java™ EE, and OpenJDK™ are trademarks of Oracle and/or its affiliates. Kubernetes® is a registered trademark of the Linux Foundation in the United States and other countries. Linux® is the registered trademark of Linus Torvalds in the United States and other countries. Windows® and Microsoft® Azure are registered trademarks of Microsoft Corporation. “AWS” and “Amazon Web Services” are trademarks or registered trademarks of Amazon.com Inc. or its affiliates. All other trademarks and copyrights are property of their respective owners and are only mentioned for informative purposes. Other names may be trademarks of their respective owners.

Search in all Spring Docs

---

## Upstream refresh 2026-08-01 (verbatim extractor output)

Source: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html
HTTP status: 200 · extracted bytes: 34267 · sha256: b52299e5011c817bea260fb899bd4b91edd05507a75763f16231440d9d322b40
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r148`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

Mapping Requests :: Spring Framework Why Spring Overview Trending Generative AI Cloud Architecture Patterns Microservices Reactive Event Driven Application Types Web Applications Serverless Batch Learn Getting Started Quickstart Guides Academy Courses Get Certified Projects Overview Projects Spring Boot Spring Framework Spring Cloud Spring AI Spring Data Spring Integration Spring Batch Spring Security Foundational Projects Micrometer Reactor Development Tools Spring Tools Spring Initializr Resources Blog Release Calendar Version Mappings Release Highlights Security Advisories GitHub Orgs Spring Projects Spring Cloud Community Overview Events Authors Enterprise Overview Long-term Support Automated Upgrades Governance and Compliance Modern App Development light Spring Framework 7.0.8 Search Overview Core Technologies The IoC Container Introduction to the Spring IoC Container and Beans Container Overview Bean Overview Dependencies Dependency Injection Dependencies and Configuration in Detail Using depends-on Lazy-initialized Beans Autowiring Collaborators Method Injection Bean Scopes Customizing the Nature of a Bean Bean Definition Inheritance Container Extension Points Annotation-based Container Configuration Using @Autowired Fine-tuning Annotation-based Autowiring with @Primary or @Fallback Fine-tuning Annotation-based Autowiring with Qualifiers Using Generics as Autowiring Qualifiers Using CustomAutowireConfigurer Injection with @Resource Using @Value Using @PostConstruct and @PreDestroy Classpath Scanning and Managed Components Using JSR-330 Standard Annotations Java-based Container Configuration Basic Concepts: @Bean and @Configuration Instantiating the Spring Container by Using AnnotationConfigApplicationContext Using the @Bean Annotation Using the @Configuration annotation Composing Java-based Configurations Programmatic Bean Registration Environment Abstraction Registering a LoadTimeWeaver Additional Capabilities of the ApplicationContext The BeanFactory API Resources Validation, Data Binding, and Type Conversion Validation Using Spring’s Validator Interface Data Binding Resolving Error Codes to Error Messages Spring Type Conversion Spring Field Formatting Configuring a Global Date and Time Format Java Bean Validation Spring Expression Language (SpEL) Evaluation Expressions in Bean Definitions Language Reference Literal Expressions Properties, Arrays, Lists, Maps, and Indexers Inline Lists Inline Maps Array Construction Methods Operators Types Constructors Variables Functions Varargs Invocations Bean References Ternary Operator (If-Then-Else) The Elvis Operator Safe Navigation Operator Collection Selection Collection Projection Expression Templating Classes Used in the Examples Aspect Oriented Programming with Spring AOP Concepts Spring AOP Capabilities and Goals AOP Proxies @AspectJ support Enabling @AspectJ Support Declaring an Aspect Declaring a Pointcut Declaring Advice Introductions Aspect Instantiation Models An AOP Example Schema-based AOP Support Choosing which AOP Declaration Style to Use Mixing Aspect Types Proxying Mechanisms Programmatic Creation of @AspectJ Proxies Using AspectJ with Spring Applications Further Resources Spring AOP APIs Pointcut API in Spring Advice API in Spring The Advisor API in Spring Using the ProxyFactoryBean to Create AOP Proxies Concise Proxy Definitions Creating AOP Proxies Programmatically with the ProxyFactory Manipulating Advised Objects Using the "auto-proxy" facility Using TargetSource Implementations Defining New Advice Types Resilience Features Null-safety Data Buffers and Codecs Ahead of Time Optimizations Appendix XML Schemas XML Schema Authoring Application Startup Steps Data Access Transaction Management Advantages of the Spring Framework’s Transaction Support Model Understanding the Spring Framework Transaction Abstraction Synchronizing Resources with Transactions Declarative Transaction Management Understanding the Spring Framework’s Declarative Transaction Implementation Example of Declarative Transaction Implementation Rolling Back a Declarative Transaction Configuring Different Transactional Semantics for Different Beans <tx:advice/> Settings Using @Transactional Transaction Propagation Advising Transactional Operations Using @Transactional with AspectJ Programmatic Transaction Management Choosing Between Programmatic and Declarative Transaction Management Transaction-bound Events Application server-specific integration Solutions to Common Problems Further Resources DAO Support Data Access with JDBC Choosing an Approach for JDBC Database Access Package Hierarchy Using the JDBC Core Classes to Control Basic JDBC Processing and Error Handling Controlling Database Connections JDBC Batch Operations Simplifying JDBC Operations with the SimpleJdbc Classes Modeling JDBC Operations as Java Objects Common Problems with Parameter and Data Value Handling Embedded Database Support Initializing a DataSource Data Access with R2DBC Object Relational Mapping (ORM) Data Access Introduction to ORM with Spring General ORM Integration Considerations Hibernate JPA Marshalling XML by Using Object-XML Mappers Appendix Web on Servlet Stack Spring Web MVC DispatcherServlet Context Hierarchy Special Bean Types Web MVC Config Servlet Config Processing Path Matching Interception Exceptions View Resolution Locale Multipart Resolver Logging Filters HTTP Message Conversion Annotated Controllers Declaration Mapping Requests Handler Methods Method Arguments Return Values Type Conversion Matrix Variables @RequestParam @RequestHeader @CookieValue @ModelAttribute @SessionAttributes @SessionAttribute @RequestAttribute Redirect Attributes Flash Attributes Multipart @RequestBody HttpEntity @ResponseBody ResponseEntity Jackson JSON Model @InitBinder Validation Exceptions Controller Advice Functional Endpoints URI Links Asynchronous Requests Range Requests Data Binding CORS API Versioning Error Responses Web Security HTTP Caching View Technologies Thymeleaf FreeMarker Groovy Markup Script Views HTML Fragments JSP and JSTL RSS and Atom PDF and Excel Jackson XML Marshalling XSLT Views MVC Config Enable MVC Configuration MVC Config API Type Conversion Validation Interceptors Content Types Message Converters View Controllers View Resolvers Static Resources Default Servlet Path Matching API Version Advanced Java Config Advanced XML Config HTTP/2 REST Clients Testing WebSockets WebSocket API SockJS Fallback STOMP Overview Benefits Enable STOMP WebSocket Transport Flow of Messages Annotated Controllers Sending Messages Simple Broker External Broker Connecting to a Broker Dots as Separators Authentication Token Authentication Authorization User Destinations Order of Messages Events Interception STOMP Client WebSocket Scope Performance Monitoring Testing Web on Reactive Stack Spring WebFlux Overview Reactive Core DispatcherHandler Annotated Controllers @Controller Mapping Requests Handler Methods Method Arguments Return Values Type Conversion Matrix Variables @RequestParam @RequestHeader @CookieValue @ModelAttribute @SessionAttributes @SessionAttribute @RequestAttribute Multipart Content @RequestBody HttpEntity @ResponseBody ResponseEntity Jackson JSON Model DataBinder Validation Exceptions Controller Advice Functional Endpoints URI Links Range Requests Data Binding CORS API Versioning Error Responses Web Security HTTP Caching View Technologies WebFlux Config HTTP/2 WebClient Configuration retrieve() Exchange Request Body Filters Attributes Context Synchronous Use Testing HTTP Service Client WebSockets Testing RSocket Reactive Libraries Testing Introduction to Spring Testing Unit Testing Integration Testing JDBC Testing Support Spring TestContext Framework Key Abstractions Bootstrapping the TestContext Framework TestExecutionListener Configuration Application Events Test Execution Events Context Management Context Configuration with Component Classes Context Configuration with XML Resources Context Configuration with Groovy Scripts Default Context Configuration Mixing Component Classes, XML, and Groovy Scripts Context Configuration with Context Customizers Context Configuration with Context Initializers Context Configuration Inheritance Context Configuration with Environment Profiles Context Configuration with Test Property Sources Context Configuration with Dynamic Property Sources Loading a WebApplicationContext Working with Web Mocks Context Caching Context Pausing Context Failure Threshold Context Hierarchies Dependency Injection of Test Fixtures Bean Overriding in Tests Testing Request- and Session-scoped Beans Transaction Management Executing SQL Scripts Parallel Test Execution TestContext Framework Support Classes Ahead of Time Support for Tests WebTestClient RestTestClient MockMvc Overview Setup Options Hamcrest Integration Static Imports Configuring MockMvc Setup Features Performing Requests Defining Expectations Async Requests Streaming Responses Filter Registrations AssertJ Integration Configuring MockMvcTester Performing Requests Defining Expectations MockMvc integration HtmlUnit Integration Why HtmlUnit Integration? MockMvc and HtmlUnit MockMvc and WebDriver MockMvc and Geb MockMvc vs End-to-End Tests Further Examples Testing Client Applications Appendix Annotations Standard Annotation Support Spring Testing Annotations @BootstrapWith @ContextConfiguration @WebAppConfiguration @ContextHierarchy @ContextCustomizerFactories @ActiveProfiles @TestPropertySource @DynamicPropertySource @TestBean @MockitoBean and @MockitoSpyBean @DirtiesContext @TestExecutionListeners @RecordApplicationEvents @Commit @Rollback @BeforeTransaction @AfterTransaction @Sql @SqlConfig @SqlMergeMode @SqlGroup @DisabledInAotMode Spring JUnit 4 Testing Annotations Spring JUnit Jupiter Testing Annotations Meta-Annotation Support for Testing Further Resources Integration REST Clients JMS (Java Message Service) Using Spring JMS Sending a Message Receiving a Message Support for JCA Message Endpoints Annotation-driven Listener Endpoints JMS Namespace Support JMX Exporting Your Beans to JMX Controlling the Management Interface of Your Beans Controlling ObjectName Instances for Your Beans Using JSR-160 Connectors Accessing MBeans through Proxies Notifications Further Resources Email Task Execution and Scheduling Cache Abstraction Understanding the Cache Abstraction Declarative Annotation-based Caching JCache (JSR-107) Annotations Declarative XML-based Caching Configuring the Cache Storage Plugging-in Different Back-end Caches How can I Set the TTL/TTI/Eviction policy/XXX feature? Observability Support JVM AOT Cache JVM Checkpoint Restore Appendix Language Support Kotlin Requirements Extensions Null-safety Classes and Interfaces Annotations Bean Registration DSL Web Coroutines Spring Projects in Kotlin Getting Started Resources Apache Groovy Appendix Java API Kotlin API Wiki Search Edit this Page GitHub Project Stack Overflow Spring Framework Web on Servlet Stack Spring Web MVC Annotated Controllers Mapping Requests Mapping Requests See equivalent in the Reactive stack This section discusses request mapping for annotated controllers. @RequestMapping See equivalent in the Reactive stack You can use the @RequestMapping annotation to map requests to controllers methods. It has various attributes to match by URL, HTTP method, request parameters, headers, and media types. You can use it at the class level to express shared mappings or at the method level to narrow down to a specific endpoint mapping. There are also HTTP method specific shortcut variants of @RequestMapping : @GetMapping @PostMapping @PutMapping @DeleteMapping @PatchMapping The shortcuts are Custom Annotations that are provided because, arguably, most controller methods should be mapped to a specific HTTP method versus using @RequestMapping , which, by default, matches to all HTTP methods. A @RequestMapping is still needed at the class level to express shared mappings. @RequestMapping cannot be used in conjunction with other @RequestMapping annotations that are declared on the same element (class, interface, or method). If multiple @RequestMapping annotations are detected on the same element, a warning will be logged, and only the first mapping will be used. This also applies to composed @RequestMapping annotations such as @GetMapping , @PostMapping , etc. The following example has type and method level mappings: Java Kotlin @RestController @RequestMapping("/persons") class PersonController { @GetMapping("/{id}") public Person getPerson(@PathVariable Long id) { // ... } @PostMapping @ResponseStatus(HttpStatus.CREATED) public void add(@RequestBody Person person) { // ... } } @RestController @RequestMapping("/persons") class PersonController { @GetMapping("/{id}") fun getPerson(@PathVariable id: Long): Person { // ... } @PostMapping @ResponseStatus(HttpStatus.CREATED) fun add(@RequestBody person: Person) { // ... } } URI patterns See equivalent in the Reactive stack @RequestMapping methods can be mapped using URL patterns. Spring MVC is using PathPattern — a pre-parsed pattern matched against the URL path also pre-parsed as PathContainer . Designed for web use, this solution deals effectively with encoding and path parameters, and matches efficiently. See MVC config for customizations of path matching options. the AntPathMatcher variant is now deprecated because it is less efficient and the String path input is a challenge for dealing effectively with encoding and other issues with URLs. You can map requests by using glob patterns and wildcards: Pattern Description Example spring Literal pattern "/spring" matches "/spring" ? Matches one character "/pages/t?st.html" matches "/pages/test.html" and "/pages/t3st.html" * Matches zero or more characters within a path segment "/resources/*.png" matches "/resources/file.png" "/projects/*/versions" matches "/projects/spring/versions" but does not match "/projects/spring/boot/versions" . "/projects/*" matches "/projects/spring" but does not match "/projects" as the path segment is not present. ** Matches zero or more path segments "/resources/**" matches "/resources" , "/resources/file.png" and "/resources/images/file.png" "/**/info" matches "/info" , "/spring/info" and "/spring/framework/info" "/resources/**/file.png" is invalid as ** is not allowed in the middle of the path. "/**/spring/**" is not allowed, as only a single ** / {*path} instance is allowed per pattern. {name} Similar to * , but also captures the path segment as a variable named "name" "/projects/{project}/versions" matches "/projects/spring/versions" and captures project=spring "/projects/{project}/versions" does not match "/projects/spring/framework/versions" as it captures a single path segment. {name:[a-z]+} Matches the regexp "[a-z]+" as a path variable named "name" "/projects/{project:[a-z]+}/versions" matches "/projects/spring/versions" but not "/projects/spring1/versions" {*path} Similar to ** , but also captures the path segments as a variable named "path" "/resources/{*file}" matches "/resources/images/file.png" and captures file=/images/file.png "{*path}/resources" matches "/spring/framework/resources" and captures path=/spring/framework "/resources/{*path}/file.png" is invalid as {*path} is not allowed in the middle of the path. "/{*path}/spring/**" is not allowed, as only a single ** / {*path} instance is allowed per pattern. Captured URI variables can be accessed with @PathVariable . For example: Java Kotlin @GetMapping("/owners/{ownerId}/pets/{petId}") public Pet findPet(@PathVariable Long ownerId, @PathVariable Long petId) { // ... } @GetMapping("/owners/{ownerId}/pets/{petId}") fun findPet(@PathVariable ownerId: Long, @PathVariable petId: Long): Pet { // ... } You can declare URI variables at the class and method levels, as the following example shows: Java Kotlin @Controller @RequestMapping("/owners/{ownerId}") public class OwnerController { @GetMapping("/pets/{petId}") public Pet findPet(@PathVariable Long ownerId, @PathVariable Long petId) { // ... } } @Controller @RequestMapping("/owners/{ownerId}") class OwnerController { @GetMapping("/pets/{petId}") fun findPet(@PathVariable ownerId: Long, @PathVariable petId: Long): Pet { // ... } } URI variables are automatically converted to the appropriate type, or TypeMismatchException is raised. Simple types ( int , long , Date , and so on) are supported by default and you can register support for any other data type. See Type Conversion and DataBinder . You can explicitly name URI variables (for example, @PathVariable("customId") ), but you can leave that detail out if the names are the same and your code is compiled with the -parameters compiler flag. The syntax {varName:regex} declares a URI variable with a regular expression that has syntax of {varName:regex} . For example, given URL "/spring-web-3.0.5.jar" , the following method extracts the name, version, and file extension: Java Kotlin @GetMapping("/{name:[a-z-]+}-{version:\\d\\.\\d\\.\\d}{ext:\\.[a-z]+}") public void handle(@PathVariable String name, @PathVariable String version, @PathVariable String ext) { // ... } @GetMapping("/{name:[a-z-]+}-{version:\\d\\.\\d\\.\\d}{ext:\\.[a-z]+}") fun handle(@PathVariable name: String, @PathVariable version: String, @PathVariable ext: String) { // ... } URI path patterns can also have: Embedded ${…​} placeholders that are resolved on startup via PropertySourcesPlaceholderConfigurer against local, system, environment, and other property sources. This is useful, for example, to parameterize a base URL based on external configuration. SpEL expression #{…​} . Pattern Comparison See equivalent in the Reactive stack When multiple patterns match a URL, the best match must be selected. This is done with one of the following depending on whether use of parsed PathPattern is enabled for use or not: PathPattern.SPECIFICITY_COMPARATOR AntPathMatcher.getPatternComparator(String path) Both help to sort patterns with more specific ones on top. A pattern is more specific if it has a lower count of URI variables (counted as 1), single wildcards (counted as 1), and double wildcards (counted as 2). Given an equal score, the longer pattern is chosen. Given the same score and length, the pattern with more URI variables than wildcards is chosen. The default mapping pattern ( /** ) is excluded from scoring and always sorted last. Also, prefix patterns (such as /public/** ) are considered less specific than other pattern that do not have double wildcards. For the full details, follow the above links to the pattern Comparators. Suffix Match and RFD A reflected file download (RFD) attack is similar to XSS in that it relies on request input (for example, a query parameter and a URI variable) being reflected in the response. However, instead of inserting JavaScript into HTML, an RFD attack relies on the browser switching to perform a download and treating the response as an executable script when double-clicked later. In Spring MVC, @ResponseBody and ResponseEntity methods are at risk, because they can render different content types, which clients can request through URL path extensions. Disabling suffix pattern matching and using path extensions for content negotiation lower the risk but are not sufficient to prevent RFD attacks. To prevent RFD attacks, prior to rendering the response body, Spring MVC adds a Content-Disposition:inline;filename=f.txt header to suggest a fixed and safe download file. This is done only if the URL path contains a file extension that is neither allowed as safe nor explicitly registered for content negotiation. However, it can potentially have side effects when URLs are typed directly into a browser. Many common path extensions are allowed as safe by default. Applications with custom HttpMessageConverter implementations can explicitly register file extensions for content negotiation to avoid having a Content-Disposition header added for those extensions. See Content Types . See CVE-2015-5211 for additional recommendations related to RFD. Consumable Media Types See equivalent in the Reactive stack You can narrow the request mapping based on the Content-Type of the request, as the following example shows: Java Kotlin @PostMapping(path = "/pets", consumes = "application/json") (1) public void addPet(@RequestBody Pet pet) { // ... } 1 Using a consumes attribute to narrow the mapping by the content type. @PostMapping("/pets", consumes = ["application/json"]) (1) fun addPet(@RequestBody pet: Pet) { // ... } 1 Using a consumes attribute to narrow the mapping by the content type. The consumes attribute also supports negation expressions — for example, !text/plain means any content type other than text/plain . You can declare a shared consumes attribute at the class level. Unlike most other request-mapping attributes, however, when used at the class level, a method-level consumes attribute overrides rather than extends the class-level declaration. MediaType provides constants for commonly used media types, such as APPLICATION_JSON_VALUE and APPLICATION_XML_VALUE . Producible Media Types See equivalent in the Reactive stack You can narrow the request mapping based on the Accept request header and the list of content types that a controller method produces, as the following example shows: Java Kotlin @GetMapping(path = "/pets/{petId}", produces = "application/json") (1) @ResponseBody public Pet getPet(@PathVariable String petId) { // ... } 1 Using a produces attribute to narrow the mapping by the content type. @GetMapping("/pets/{petId}", produces = ["application/json"]) (1) @ResponseBody fun getPet(@PathVariable petId: String): Pet { // ... } 1 Using a produces attribute to narrow the mapping by the content type. The media type can specify a character set. Negated expressions are supported — for example, !text/plain means any content type other than "text/plain". You can declare a shared produces attribute at the class level. Unlike most other request-mapping attributes, however, when used at the class level, a method-level produces attribute overrides rather than extends the class-level declaration. MediaType provides constants for commonly used media types, such as APPLICATION_JSON_VALUE and APPLICATION_XML_VALUE . Parameters, headers See equivalent in the Reactive stack You can narrow request mappings based on request parameter conditions. You can test for the presence of a request parameter ( myParam ), for the absence of one ( !myParam ), or for a specific value ( myParam=myValue ). The following example shows how to test for a specific value: Java Kotlin @GetMapping(path = "/pets/{petId}", params = "myParam=myValue") (1) public void findPet(@PathVariable String petId) { // ... } 1 Testing whether myParam equals myValue . @GetMapping("/pets/{petId}", params = ["myParam=myValue"]) (1) fun findPet(@PathVariable petId: String) { // ... } 1 Testing whether myParam equals myValue . You can also use the same with request header conditions, as the following example shows: Java Kotlin @GetMapping(path = "/pets/{petId}", headers = "myHeader=myValue") (1) public void findPet(@PathVariable String petId) { // ... } 1 Testing whether myHeader equals myValue . @GetMapping("/pets/{petId}", headers = ["myHeader=myValue"]) (1) fun findPet(@PathVariable petId: String) { // ... } 1 Testing whether myHeader equals myValue . You can match Content-Type and Accept with the headers condition, but it is better to use consumes and produces instead. API Version See equivalent in the Reactive stack There is no standard way to specify an API version, so when you enable API versioning in the MVC Config you need to specify how to resolve the version. The MVC Config creates an ApiVersionStrategy that in turn is used to map requests. Once API versioning is enabled, you can begin to map requests with versions. The @RequestMapping version attribute supports the following: Fixed version ("1.2") — matches the given version only Baseline version ("1.2+") — matches the given and supported versions above No value — matches any version, but is superseded by a more specific version match If multiple controller methods have a version less than or equal to the request version, the highest of those, and closest to the request version, is the one considered, in effect superseding the rest. To illustrate this, consider the following mappings: Java @RestController @RequestMapping("/account/{id}") public class AccountController { @GetMapping (1) public Account getAccount() { } @GetMapping(version = "1.1") (2) public Account getAccount1_1() { } @GetMapping(version = "1.2+") (3) public Account getAccount1_2() { } @GetMapping(version = "1.5") (4) public Account getAccount1_5() { } } 1 match any version 2 match version 1.1 3 match version 1.2 and supported versions above 4 match version 1.5 For request with version "1.3" : (1) matches as it matches any version (2) does not match (3) matches as it matches 1.2 and above, and is chosen as the highest match (4) is higher and does not match Version 1.3 must be present in the mappings, or be configured as supported . For request with version "1.5" : (1) matches as it matches any version (2) does not match (3) matches as it matches 1.2 and above (4) matches and is chosen as the highest match A request with version "1.6" does not have a match. (1) and (3) do match, but are superseded by (4), which allows only a strict match, and therefore does not match. In this scenario, a NotAcceptableApiVersionException results in a 400 response. Controller methods without a version are intended to support clients created before a versioned alternative was introduced. Therefore, even though an unversioned controller method is considered a match for any version, it is in fact given the lowest priority, and is effectively superseded by any alternative controller method with a version. See API Versioning for more details on underlying infrastructure and support for API Versioning. HTTP HEAD, OPTIONS See equivalent in the Reactive stack @GetMapping (and @RequestMapping(method=HttpMethod.GET) ) support HTTP HEAD transparently for request mapping. Controller methods do not need to change. A response wrapper, applied in jakarta.servlet.http.HttpServlet , ensures a Content-Length header is set to the number of bytes written (without actually writing to the response). By default, HTTP OPTIONS is handled by setting the Allow response header to the list of HTTP methods listed in all @RequestMapping methods that have matching URL patterns. For a @RequestMapping without HTTP method declarations, the Allow header is set to GET,HEAD,POST,PUT,PATCH,DELETE,OPTIONS . Controller methods should always declare the supported HTTP methods (for example, by using the HTTP method specific variants: @GetMapping , @PostMapping , and others). You can explicitly map the @RequestMapping method to HTTP HEAD and HTTP OPTIONS, but that is not necessary in the common case. Custom Annotations See equivalent in the Reactive stack Spring MVC supports the use of composed annotations for request mapping. Those are annotations that are themselves meta-annotated with @RequestMapping and composed to redeclare a subset (or all) of the @RequestMapping attributes with a narrower, more specific purpose. @GetMapping , @PostMapping , @PutMapping , @DeleteMapping , and @PatchMapping are examples of composed annotations. They are provided because, arguably, most controller methods should be mapped to a specific HTTP method versus using @RequestMapping , which, by default, matches to all HTTP methods. If you need an example of how to implement a composed annotation, look at how those are declared. @RequestMapping cannot be used in conjunction with other @RequestMapping annotations that are declared on the same element (class, interface, or method). If multiple @RequestMapping annotations are detected on the same element, a warning will be logged, and only the first mapping will be used. This also applies to composed @RequestMapping annotations such as @GetMapping , @PostMapping , etc. Spring MVC also supports custom request-mapping attributes with custom request-matching logic. This is a more advanced option that requires subclassing RequestMappingHandlerMapping and overriding the getCustomMethodCondition method, where you can check the custom attribute and return your own RequestCondition . Explicit Registrations See equivalent in the Reactive stack You can programmatically register handler methods, which you can use for dynamic registrations or for advanced cases, such as different instances of the same handler under different URLs. The following example registers a handler method: Java Kotlin @Configuration public class MyConfiguration { // Inject the target handler and the handler mapping for controllers @Autowired public void setHandlerMapping(RequestMappingHandlerMapping mapping, UserHandler handler) throws NoSuchMethodException { // Prepare the request mapping meta data RequestMappingInfo info = RequestMappingInfo .paths("/user/{id}").methods(RequestMethod.GET).build(); // Get the handler method Method method = UserHandler.class.getMethod("getUser", Long.class); // Add the registration mapping.registerMapping(info, handler, method); } } @Configuration class MyConfiguration { // Inject the target handler and the handler mapping for controllers @Autowired fun setHandlerMapping(mapping: RequestMappingHandlerMapping, handler: UserHandler) { // Get the handler method val info = RequestMappingInfo.paths("/user/{id}").methods(RequestMethod.GET).build() // Get the handler method val method = UserHandler::class.java.getMethod("getUser", Long::class.java) // Add the registration mapping.registerMapping(info, handler, method) } } @HttpExchange See equivalent in the Reactive stack While the main purpose of @HttpExchange is to abstract HTTP client code with a generated proxy, the interface on which such annotations are placed is a contract neutral to client vs server use. In addition to simplifying client code, there are also cases where an HTTP Service Client may be a convenient way for servers to expose their API for client access. This leads to increased coupling between client and server and is often not a good choice, especially for public API’s, but may be exactly the goal for an internal API. It is an approach commonly used in Spring Cloud, and it is why @HttpExchange is supported as an alternative to @RequestMapping for server side handling in controller classes. For example: Java Kotlin @HttpExchange("/persons") interface PersonService { @GetExchange("/{id}") Person getPerson(@PathVariable Long id); @PostExchange void add(@RequestBody Person person); } @RestController class PersonController implements PersonService { public Person getPerson(@PathVariable Long id) { // ... } @ResponseStatus(HttpStatus.CREATED) public void add(@RequestBody Person person) { // ... } } @HttpExchange("/persons") interface PersonService { @GetExchange("/{id}") fun getPerson(@PathVariable id: Long): Person @PostExchange fun add(@RequestBody person: Person) } @RestController class PersonController : PersonService { override fun getPerson(@PathVariable id: Long): Person { // ... } @ResponseStatus(HttpStatus.CREATED) override fun add(@RequestBody person: Person) { // ... } } @HttpExchange and @RequestMapping have differences. @RequestMapping can map to any number of requests by path patterns, HTTP methods, and more, while @HttpExchange declares a single endpoint with a concrete HTTP method, path, and content types. For method parameters and returns values, generally, @HttpExchange supports a subset of the method parameters that @RequestMapping does. Notably, it excludes any server-side specific parameter types. For details, see the list for @HttpExchange and @RequestMapping . @HttpExchange also supports a headers() parameter which accepts "name=value" -like pairs like in @RequestMapping(headers={}) on the client side. On the server side, this extends to the full syntax that @RequestMapping supports. Declaration Handler Methods Spring Framework Stable 7.0.8 6.2.19 Snapshot 7.1.0-SNAPSHOT 7.0.9-SNAPSHOT 6.2.20-SNAPSHOT Related Spring Documentation Spring Boot Spring Framework Spring Cloud Spring Cloud Build Spring Cloud Bus Spring Cloud Circuit Breaker Spring Cloud Commons Spring Cloud Config Spring Cloud Consul Spring Cloud Contract Spring Cloud Function Spring Cloud Gateway Spring Cloud Kubernetes Spring Cloud Netflix Spring Cloud OpenFeign Spring Cloud Stream Spring Cloud Task Spring Cloud Vault Spring Cloud Zookeeper Spring Data Spring Data Cassandra Spring Data Commons Spring Data Couchbase Spring Data Elasticsearch Spring Data JPA Spring Data KeyValue Spring Data LDAP Spring Data MongoDB Spring Data Neo4j Spring Data Redis Spring Data JDBC & R2DBC Spring Data REST Spring Integration Spring Batch Spring Security Spring Authorization Server Spring LDAP Spring Security Kerberos Spring Session Spring Vault Spring AI Spring AMQP Spring CLI Spring GraphQL Spring for Apache Kafka Spring Modulith Spring for Apache Pulsar Spring Shell All Docs... Copyright © 2005 - Broadcom. All Rights Reserved. The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries. Terms of Use • Privacy • Trademark Guidelines • Thank you • Your California Privacy Rights • Cookie Settings Apache®, Apache Tomcat®, Apache Kafka®, Apache Cassandra™, and Apache Geode™ are trademarks or registered trademarks of the Apache Software Foundation in the United States and/or other countries. Java™, Java™ SE, Java™ EE, and OpenJDK™ are trademarks of Oracle and/or its affiliates. Kubernetes® is a registered trademark of the Linux Foundation in the United States and other countries. Linux® is the registered trademark of Linus Torvalds in the United States and other countries. Windows® and Microsoft® Azure are registered trademarks of Microsoft Corporation. “AWS” and “Amazon Web Services” are trademarks or registered trademarks of Amazon.com Inc. or its affiliates. All other trademarks and copyrights are property of their respective owners and are only mentioned for informative purposes. Other names may be trademarks of their respective owners. Search in all Spring Docs
