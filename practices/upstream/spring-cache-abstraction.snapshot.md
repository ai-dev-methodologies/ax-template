# spring-cache-abstraction — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T02:24:29Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r140`
**Body SHA-256 (below the `---` divider, header excluded):** 836db4ebe893182feb6fb32711a29134ea09e4189ca9a12807176e2f676b3c10

---

---
snapshot_id: spring-cache-abstraction
source: "https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 3
bytes: 42235
sha: "40686231dc0facc8ae8b109dcb49ebce4ee840d7efee5f632b8661430cc0ee39"
---

# spring cache abstraction — upstream snapshot

Source: https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html
Fetched: 2026-07-14

Declarative Annotation-based Caching :: Spring Framework

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

Integration

Cache Abstraction

Declarative Annotation-based Caching

# Declarative Annotation-based Caching

For caching declaration, Spring’s caching abstraction provides a set of Java annotations:

@Cacheable: Triggers cache population.

@CacheEvict: Triggers cache eviction.

@CachePut: Updates the cache without interfering with the method execution.

@Caching: Regroups multiple cache operations to be applied on a method.

@CacheConfig: Shares some common cache-related settings at class-level.

## The @Cacheable Annotation

As the name implies, you can use @Cacheable to demarcate methods that are cacheable — that is, methods for which the result is stored in the cache so that, on subsequent
invocations (with the same arguments), the value in the cache is returned without
having to actually invoke the method. In its simplest form, the annotation declaration
requires the name of the cache associated with the annotated method, as the following
example shows:

@Cacheable("books")
public Book findBook(ISBN isbn) {...}

In the preceding snippet, the findBook method is associated with the cache named books.
Each time the method is called, the cache is checked to see whether the invocation has
already been run and does not have to be repeated. While in most cases, only one
cache is declared, the annotation lets multiple names be specified so that more than one
cache is being used. In this case, each of the caches is checked before invoking the
method — if at least one cache is hit, the associated value is returned.

All the other caches that do not contain the value are also updated, even though
the cached method was not actually invoked.

The following example uses @Cacheable on the findBook method with multiple caches:

@Cacheable({"books", "isbns"})
public Book findBook(ISBN isbn) {...}

### Default Key Generation

Since caches are essentially key-value stores, each invocation of a cached method
needs to be translated into a suitable key for cache access. The caching abstraction
uses a simple KeyGenerator based on the following algorithm:

If no parameters are given, return SimpleKey.EMPTY.

If only one parameter is given, return that instance.

If more than one parameter is given, return a SimpleKey that contains all parameters.

This approach works well for most use-cases, as long as parameters have natural keys
and implement valid hashCode() and equals() methods. If that is not the case,
you need to change the strategy.

To provide a different default key generator, you need to implement the
org.springframework.cache.interceptor.KeyGenerator interface.

The default key generation strategy changed with the release of Spring 4.0. Earlier
versions of Spring used a key generation strategy that, for multiple key parameters,
considered only the hashCode() of parameters and not equals(). This could cause
unexpected key collisions (see spring-framework#14870
for background). The new SimpleKeyGenerator uses a compound key for such scenarios.

If you want to keep using the previous key strategy, you can configure the deprecated
org.springframework.cache.interceptor.DefaultKeyGenerator class or create a custom
hash-based KeyGenerator implementation.

### Custom Key Generation Declaration

Since caching is generic, the target methods are quite likely to have various signatures
that cannot be readily mapped on top of the cache structure. This tends to become obvious
when the target method has multiple arguments out of which only some are suitable for
caching (while the rest are used only by the method logic). Consider the following example:

@Cacheable("books")
public Book findBook(ISBN isbn, boolean checkWarehouse, boolean includeUsed)

At first glance, while the two boolean arguments influence the way the book is found,
they are no use for the cache. Furthermore, what if only one of the two is important
while the other is not?

For such cases, the @Cacheable annotation lets you specify how the key is generated
through its key attribute. You can use SpEL to pick the
arguments of interest (or their nested properties), perform operations, or even
invoke arbitrary methods without having to write any code or implement any interface.
This is the recommended approach over the
default generator,
since methods tend to be quite different in signatures as the code base grows. While the
default strategy might work for some methods, it rarely works for all methods.

The following examples use various SpEL declarations (if you are not familiar with SpEL,
do yourself a favor and read Spring Expression Language):

@Cacheable(cacheNames="books", key="#isbn")
public Book findBook(ISBN isbn, boolean checkWarehouse, boolean includeUsed)

@Cacheable(cacheNames="books", key="#isbn.rawNumber")
public Book findBook(ISBN isbn, boolean checkWarehouse, boolean includeUsed)

@Cacheable(cacheNames="books", key="T(someType).hash(#isbn)")
public Book findBook(ISBN isbn, boolean checkWarehouse, boolean includeUsed)

The preceding snippets show how easy it is to select a certain argument, one of its
properties, or even an arbitrary (static) method.

If the algorithm responsible for generating the key is too specific or if it needs
to be shared, you can define a custom keyGenerator on the operation. To do so,
specify the name of the KeyGenerator bean implementation to use, as the following
example shows:

@Cacheable(cacheNames="books", keyGenerator="myKeyGenerator")
public Book findBook(ISBN isbn, boolean checkWarehouse, boolean includeUsed)

The key and keyGenerator parameters are mutually exclusive and an operation
that specifies both results in an exception.

### Default Cache Resolution

The caching abstraction uses a simple CacheResolver that retrieves the caches
defined at the operation level by using the configured CacheManager.

To provide a different default cache resolver, you need to implement the
org.springframework.cache.interceptor.CacheResolver interface.

### Custom Cache Resolution

The default cache resolution fits well for applications that work with a
single CacheManager and have no complex cache resolution requirements.

For applications that work with several cache managers, you can set the
cacheManager to use for each operation, as the following example shows:

@Cacheable(cacheNames="books", cacheManager="anotherCacheManager") (1)
public Book findBook(ISBN isbn) {...}

1Specifying anotherCacheManager.

You can also replace the CacheResolver entirely in a fashion similar to that of
replacing key generation.
The resolution is requested for every cache operation, letting the implementation
actually resolve the caches to use based on runtime arguments. The following example
shows how to specify a CacheResolver:

@Cacheable(cacheResolver="runtimeCacheResolver") (1)
public Book findBook(ISBN isbn) {...}

1Specifying the CacheResolver.

Since Spring 4.1, the value attribute of the cache annotations are no longer
mandatory, since this particular information can be provided by the CacheResolver
regardless of the content of the annotation.

Similarly to key and keyGenerator, the cacheManager and cacheResolver
parameters are mutually exclusive, and an operation specifying both
results in an exception, as a custom CacheManager is ignored by the
CacheResolver implementation. This is probably not what you expect.

### Synchronized Caching

In a multi-threaded environment, certain operations might be concurrently invoked for
the same argument (typically on startup). By default, the cache abstraction does not
lock anything, and the same value may be computed several times, defeating the purpose
of caching.

For those particular cases, you can use the sync attribute to instruct the underlying
cache provider to lock the cache entry while the value is being computed. As a result,
only one thread is busy computing the value, while the others are blocked until the entry
is updated in the cache. The following example shows how to use the sync attribute:

@Cacheable(cacheNames="foos", sync=true) (1)
public Foo executeExpensiveOperation(String id) {...}

1Using the sync attribute.

This is an optional feature, and your favorite cache library may not support it.
All CacheManager implementations provided by the core framework support it. See the
documentation of your cache provider for more details.

### Caching with CompletableFuture and Reactive Return Types

As of 6.1, cache annotations take CompletableFuture and reactive return types
into account, automatically adapting the cache interaction accordingly.

For a method returning a CompletableFuture, the object produced by that future
will be cached whenever it is complete, and the cache lookup for a cache hit will
be retrieved via a CompletableFuture:

@Cacheable("books")
public CompletableFuture<Book> findBook(ISBN isbn) {...}

For a method returning a Reactor Mono, the object emitted by that Reactive Streams
publisher will be cached whenever it is available, and the cache lookup for a cache
hit will be retrieved as a Mono (backed by a CompletableFuture):

@Cacheable("books")
public Mono<Book> findBook(ISBN isbn) {...}

For a method returning a Reactor Flux, the objects emitted by that Reactive Streams
publisher will be collected into a List and cached whenever that list is complete,
and the cache lookup for a cache hit will be retrieved as a Flux (backed by a
CompletableFuture for the cached List value):

@Cacheable("books")
public Flux<Book> findBooks(String author) {...}

Such CompletableFuture and reactive adaptation also works for synchronized caching,
computing the value only once in case of a concurrent cache miss:

@Cacheable(cacheNames="foos", sync=true) (1)
public CompletableFuture<Foo> executeExpensiveOperation(String id) {...}

1Using the sync attribute.

In order for such an arrangement to work at runtime, the configured cache
needs to be capable of CompletableFuture-based retrieval. The Spring-provided
ConcurrentMapCacheManager automatically adapts to that retrieval style, and
CaffeineCacheManager natively supports it when its asynchronous cache mode is
enabled: set setAsyncCacheMode(true) on your CaffeineCacheManager instance.

@Bean
CacheManager cacheManager() {
CaffeineCacheManager cacheManager = new CaffeineCacheManager();
cacheManager.setCacheSpecification(...);
cacheManager.setAsyncCacheMode(true);
return cacheManager;
}

Last but not least, be aware that annotation-driven caching is not appropriate
for sophisticated reactive interactions involving composition and back pressure.
If you choose to declare @Cacheable on specific reactive methods, consider the
impact of the rather coarse-granular cache interaction which simply stores the
emitted object for a Mono or even a pre-collected list of objects for a Flux.

### Conditional Caching

Sometimes, a method might not be suitable for caching all the time (for example, it might
depend on the given arguments). The cache annotations support such use cases through the
condition parameter, which takes a SpEL expression that is evaluated to either true
or false. If true, the method is cached. If not, it behaves as if the method is not
cached (that is, the method is invoked every time no matter what values are in the cache
or what arguments are used). For example, the following method is cached only if the
argument name has a length shorter than 32:

@Cacheable(cacheNames="book", condition="#name.length() < 32") (1)
public Book findBook(String name)

1Setting a condition on @Cacheable.

In addition to the condition parameter, you can use the unless parameter to veto the
adding of a value to the cache. Unlike condition, unless expressions are evaluated
after the method has been invoked. To expand on the previous example, perhaps we only
want to cache paperback books, as the following example does:

@Cacheable(cacheNames="book", condition="#name.length() < 32", unless="#result.hardback") (1)
public Book findBook(String name)

1Using the unless attribute to block hardbacks.

The cache abstraction supports java.util.Optional return types. If an Optional value
is present, it will be stored in the associated cache. If an Optional value is not
present, null will be stored in the associated cache. #result always refers to the
business entity and never a supported wrapper, so the previous example can be rewritten
as follows:

@Cacheable(cacheNames="book", condition="#name.length() < 32", unless="#result?.hardback")
public Optional<Book> findBook(String name)

Note that #result still refers to Book and not Optional<Book>. Since it might be
null, we use SpEL’s safe navigation operator.

### Available Caching SpEL Evaluation Context

Each SpEL expression evaluates against a dedicated context.
In addition to the built-in parameters, the framework provides dedicated caching-related
metadata, such as the argument names. The following table describes the items made
available to the context so that you can use them for key and conditional computations:

Table 1. Cache metadata available in SpEL expressions

NameLocationDescriptionExample

methodName

Root object

The name of the method being invoked

#root.methodName

method

Root object

The method being invoked

#root.method.name

target

Root object

The target object being invoked

#root.target

targetClass

Root object

The class of the target being invoked

#root.targetClass

args

Root object

The arguments (as an object array) used for invoking the target

#root.args[0]

caches

Root object

Collection of caches against which the current method is run

#root.caches[0].name

Argument name

Evaluation context

The name of a particular method argument. If the names are not available
(for example, because the code was compiled without the -parameters flag), individual
arguments are also available using the #a<#arg> syntax where <#arg> stands for the
argument index (starting from 0).

#iban or #a0 (you can also use #p0 or #p<#arg> notation as an alias).

result

Evaluation context

The result of the method call (the value to be cached). Only available in unless
expressions, cache put expressions (to compute the key), or cache evict
expressions (when beforeInvocation is false). For supported wrappers (such as
Optional), #result refers to the actual object, not the wrapper.

#result

## The @CachePut Annotation

When the cache needs to be updated without interfering with the method execution,
you can use the @CachePut annotation. That is, the method is always invoked and its
result is placed into the cache (according to the @CachePut options). It supports
the same options as @Cacheable and should be used for cache population rather than
method flow optimization. The following example uses the @CachePut annotation:

@CachePut(cacheNames="book", key="#isbn")
public Book updateBook(ISBN isbn, BookDescriptor descriptor)

Using @CachePut and @Cacheable annotations on the same method is generally
strongly discouraged because they have different behaviors. While the latter causes the
method invocation to be skipped by using the cache, the former forces the invocation in
order to run a cache update. This leads to unexpected behavior and, with the exception
of specific corner-cases (such as annotations having conditions that exclude them from each
other), such declarations should be avoided. Note also that such conditions should not rely
on the result object (that is, the #result variable), as these are validated up-front to
confirm the exclusion.

As of 6.1, @CachePut takes CompletableFuture and reactive return types into account,
performing the put operation whenever the produced object is available.

## The @CacheEvict Annotation

The cache abstraction allows not just population of a cache store but also eviction.
This process is useful for removing stale or unused data from the cache. As opposed to
@Cacheable, @CacheEvict demarcates methods that perform cache
eviction (that is, methods that act as triggers for removing data from the cache).
Similarly to its sibling, @CacheEvict requires specifying one or more caches
that are affected by the action, allows a custom cache and key resolution or a
condition to be specified, and features an extra parameter
(allEntries) that indicates whether a cache-wide eviction needs to be performed
rather than just an entry eviction (based on the key). The following example evicts
all entries from the books cache:

@CacheEvict(cacheNames="books", allEntries=true) (1)
public void loadBooks(InputStream batch)

1Using the allEntries attribute to evict all entries from the cache.

This option comes in handy when an entire cache region needs to be cleared out.
Rather than evicting each entry (which would take a long time, since it is inefficient),
all the entries are removed in one operation, as the preceding example shows.
Note that the framework ignores any key specified in this scenario as it does not apply
(the entire cache is evicted, not only one entry).

You can also indicate whether the eviction should occur after (the default) or before
the method is invoked by using the beforeInvocation attribute. The former provides the
same semantics as the rest of the annotations: Once the method completes successfully,
an action (in this case, eviction) on the cache is run. If the method does not
run (as it might be cached) or an exception is thrown, the eviction does not occur.
The latter (beforeInvocation=true) causes the eviction to always occur before the
method is invoked. This is useful in cases where the eviction does not need to be tied
to the method outcome.

Note that void methods can be used with @CacheEvict - as the methods act as a
trigger, the return values are ignored (as they do not interact with the cache). This is
not the case with @Cacheable which adds data to the cache or updates data in the cache
and, thus, requires a result.

As of 6.1, @CacheEvict takes CompletableFuture and reactive return types into account,
performing an after-invocation evict operation whenever processing has completed.

## The @Caching Annotation

Sometimes, multiple annotations of the same type (such as @CacheEvict or
@CachePut) need to be specified — for example, because the condition or the key
expression is different between different caches. @Caching lets multiple nested
@Cacheable, @CachePut, and @CacheEvict annotations be used on the same method.
The following example uses two @CacheEvict annotations:

@Caching(evict = { @CacheEvict("primary"), @CacheEvict(cacheNames="secondary", key="#p0") })
public Book importBooks(String deposit, Date date)

## The @CacheConfig Annotation

So far, we have seen that caching operations offer many customization options and that
you can set these options for each operation. However, some of the customization options
can be tedious to configure if they apply to all operations of the class. For
instance, specifying the name of the cache to use for every cache operation of the
class can be replaced by a single class-level definition. This is where @CacheConfig
comes into play. The following examples uses @CacheConfig to set the name of the cache:

@CacheConfig("books") (1)
public class BookRepositoryImpl implements BookRepository {

@Cacheable
public Book findBook(ISBN isbn) {...}
}

1Using @CacheConfig to set the name of the cache.

@CacheConfig is a class-level annotation that allows sharing the cache names,
the custom KeyGenerator, the custom CacheManager, and the custom CacheResolver.
Placing this annotation on the class does not turn on any caching operation.

An operation-level customization always overrides a customization set on @CacheConfig.
Therefore, this gives three levels of customizations for each cache operation:

Globally configured, for example, through CachingConfigurer: see next section.

At the class level, using @CacheConfig.

At the operation level.

Provider-specific settings are typically available on the CacheManager bean,
for example, on CaffeineCacheManager. These are effectively also global.

## Enabling Caching Annotations

It is important to note that even though declaring the cache annotations does not
automatically trigger their actions - like many things in Spring, the feature has to be
declaratively enabled (which means if you ever suspect caching is to blame, you can
disable it by removing only one configuration line rather than all the annotations in
your code).

To enable caching annotations add the annotation @EnableCaching to one of your
@Configuration classes or use the cache:annotation-driven element with XML:

Java

Kotlin

Xml

@Configuration
@EnableCaching
class CacheConfiguration {

@Bean
CacheManager cacheManager() {
CaffeineCacheManager cacheManager = new CaffeineCacheManager();
cacheManager.setCacheSpecification("...");
return cacheManager;
}
}

@Configuration
@EnableCaching
class CacheConfiguration {

@Bean
fun cacheManager(): CacheManager {
return CaffeineCacheManager().apply {
setCacheSpecification("...")
}
}
}

<beans xmlns="http://www.springframework.org/schema/beans"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xmlns:cache="http://www.springframework.org/schema/cache"
xsi:schemaLocation="
http://www.springframework.org/schema/beans https://www.springframework.org/schema/beans/spring-beans.xsd
http://www.springframework.org/schema/cache https://www.springframework.org/schema/cache/spring-cache.xsd">

<cache:annotation-driven/>

<bean id="cacheManager" class="org.springframework.cache.caffeine.CaffeineCacheManager">
<property name="cacheSpecification" value="..."/>
</bean>
</beans>

Both the cache:annotation-driven element and the @EnableCaching annotation let you
specify various options that influence the way the caching behavior is added to the
application through AOP. The configuration is intentionally similar with that of
@Transactional.

The default advice mode for processing caching annotations is proxy, which allows
for interception of calls through the proxy only. Local calls within the same class
cannot get intercepted that way. For a more advanced mode of interception, consider
switching to aspectj mode in combination with compile-time or load-time weaving.

For more detail about advanced customizations (using Java configuration) that are
required to implement CachingConfigurer, see the
javadoc.

Table 2. Cache annotation settings

XML AttributeAnnotation AttributeDefaultDescription

cache-manager

N/A (see the CachingConfigurer javadoc)

cacheManager

The name of the cache manager to use. A default CacheResolver is initialized behind
the scenes with this cache manager (or cacheManager if not set). For more
fine-grained management of the cache resolution, consider setting the 'cache-resolver'
attribute.

cache-resolver

N/A (see the CachingConfigurer javadoc)

A SimpleCacheResolver using the configured cacheManager.

The bean name of the CacheResolver that is to be used to resolve the backing caches.
This attribute is not required and needs to be specified only as an alternative to
the 'cache-manager' attribute.

key-generator

N/A (see the CachingConfigurer javadoc)

SimpleKeyGenerator

Name of the custom key generator to use.

error-handler

N/A (see the CachingConfigurer javadoc)

SimpleCacheErrorHandler

The name of the custom cache error handler to use. By default, any exception thrown during
a cache related operation is thrown back at the client.

mode

mode

proxy

The default mode (proxy) processes annotated beans to be proxied by using Spring’s AOP
framework (following proxy semantics, as discussed earlier, applying to method calls
coming in through the proxy only). The alternative mode (aspectj) instead weaves the
affected classes with Spring’s AspectJ caching aspect, modifying the target class byte
code to apply to any kind of method call. AspectJ weaving requires spring-aspects.jar
in the classpath as well as load-time weaving (or compile-time weaving) enabled. (See
Spring configuration for details on how to set up
load-time weaving.)

proxy-target-class

proxyTargetClass

false

Applies to proxy mode only. Controls what type of caching proxies are created for
classes annotated with the @Cacheable or @CacheEvict annotations. If the
proxy-target-class attribute is set to true, class-based proxies are created.
If proxy-target-class is false or if the attribute is omitted, standard JDK
interface-based proxies are created. (See Proxying Mechanisms
for a detailed examination of the different proxy types.)

order

order

Ordered.LOWEST_PRECEDENCE

Defines the order of the cache advice that is applied to beans annotated with
@Cacheable or @CacheEvict. (For more information about the rules related to
ordering AOP advice, see Advice Ordering.)
No specified ordering means that the AOP subsystem determines the order of the advice.

<cache:annotation-driven/> looks for @Cacheable/@CachePut/@CacheEvict/@Caching
only on beans in the same application context in which it is defined. This means that,
if you put <cache:annotation-driven/> in a WebApplicationContext for a
DispatcherServlet, it checks for beans only in your controllers, not your services.
See the MVC section for more information.

Method visibility and cache annotations

When you use proxies, you should apply the cache annotations only to methods with
public visibility. If you do annotate protected, private, or package-visible methods
with these annotations, no error is raised, but the annotated method does not exhibit
the configured caching settings. Consider using AspectJ (see the rest of this section)
if you need to annotate non-public methods, as it changes the bytecode itself.

Spring recommends that you only annotate concrete classes (and methods of concrete
classes) with the @Cache* annotations, as opposed to annotating interfaces.
You certainly can place an @Cache* annotation on an interface (or an interface
method), but this works only if you use the proxy mode (mode="proxy"). If you use the
weaving-based aspect (mode="aspectj"), the caching settings are not recognized on
interface-level declarations by the weaving infrastructure.

In proxy mode (the default), only external method calls coming in through the
proxy are intercepted. This means that self-invocation (in effect, a method within the
target object that calls another method of the target object) does not lead to actual
caching at runtime even if the invoked method is marked with @Cacheable. Consider
using the aspectj mode in this case. Also, the proxy must be fully initialized to
provide the expected behavior, so you should not rely on this feature in your
initialization code (that is, @PostConstruct).

## Using Custom Annotations

Custom annotation and AspectJ

This feature works only with the proxy-based approach but can be enabled
with a bit of extra effort by using AspectJ.

The spring-aspects module defines an aspect for the standard annotations only.
If you have defined your own annotations, you also need to define an aspect for
those. Check AnnotationCacheAspect for an example.

The caching abstraction lets you use your own annotations to identify what method
triggers cache population or eviction. This is quite handy as a template mechanism,
as it eliminates the need to duplicate cache annotation declarations, which is
especially useful if the key or condition are specified or if the foreign imports
(org.springframework) are not allowed in your code base. Similarly to the rest
of the stereotype
annotations, you can use @Cacheable, @CachePut, @CacheEvict, and @CacheConfig
as meta-annotations
(that is, annotations that can annotate other annotations). In the following example,
we replace a common @Cacheable declaration with our own custom annotation:

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Cacheable(cacheNames="books", key="#isbn")
public @interface SlowService {
}

In the preceding example, we have defined our own SlowService annotation,
which itself is annotated with @Cacheable. Now we can replace the following code:

@Cacheable(cacheNames="books", key="#isbn")
public Book findBook(ISBN isbn, boolean checkWarehouse, boolean includeUsed)

The following example shows the custom annotation with which we can replace the
preceding code:

@SlowService
public Book findBook(ISBN isbn, boolean checkWarehouse, boolean includeUsed)

Even though @SlowService is not a Spring annotation, the container automatically picks
up its declaration at runtime and understands its meaning. Note that, as mentioned
earlier,
annotation-driven behavior needs to be enabled.

Understanding the Cache AbstractionJCache (JSR-107) Annotations

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

Source: https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html
HTTP status: 200 · extracted bytes: 41533 · sha256: 48f9e999100d19a6a409889f79b35854bedf9daedc993016d2262c596f637a2c
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r140`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

Declarative Annotation-based Caching :: Spring Framework Why Spring Overview Trending Generative AI Cloud Architecture Patterns Microservices Reactive Event Driven Application Types Web Applications Serverless Batch Learn Getting Started Quickstart Guides Academy Courses Get Certified Projects Overview Projects Spring Boot Spring Framework Spring Cloud Spring AI Spring Data Spring Integration Spring Batch Spring Security Foundational Projects Micrometer Reactor Development Tools Spring Tools Spring Initializr Resources Blog Release Calendar Version Mappings Release Highlights Security Advisories GitHub Orgs Spring Projects Spring Cloud Community Overview Events Authors Enterprise Overview Long-term Support Automated Upgrades Governance and Compliance Modern App Development light Spring Framework 7.0.8 Search Overview Core Technologies The IoC Container Introduction to the Spring IoC Container and Beans Container Overview Bean Overview Dependencies Dependency Injection Dependencies and Configuration in Detail Using depends-on Lazy-initialized Beans Autowiring Collaborators Method Injection Bean Scopes Customizing the Nature of a Bean Bean Definition Inheritance Container Extension Points Annotation-based Container Configuration Using @Autowired Fine-tuning Annotation-based Autowiring with @Primary or @Fallback Fine-tuning Annotation-based Autowiring with Qualifiers Using Generics as Autowiring Qualifiers Using CustomAutowireConfigurer Injection with @Resource Using @Value Using @PostConstruct and @PreDestroy Classpath Scanning and Managed Components Using JSR-330 Standard Annotations Java-based Container Configuration Basic Concepts: @Bean and @Configuration Instantiating the Spring Container by Using AnnotationConfigApplicationContext Using the @Bean Annotation Using the @Configuration annotation Composing Java-based Configurations Programmatic Bean Registration Environment Abstraction Registering a LoadTimeWeaver Additional Capabilities of the ApplicationContext The BeanFactory API Resources Validation, Data Binding, and Type Conversion Validation Using Spring’s Validator Interface Data Binding Resolving Error Codes to Error Messages Spring Type Conversion Spring Field Formatting Configuring a Global Date and Time Format Java Bean Validation Spring Expression Language (SpEL) Evaluation Expressions in Bean Definitions Language Reference Literal Expressions Properties, Arrays, Lists, Maps, and Indexers Inline Lists Inline Maps Array Construction Methods Operators Types Constructors Variables Functions Varargs Invocations Bean References Ternary Operator (If-Then-Else) The Elvis Operator Safe Navigation Operator Collection Selection Collection Projection Expression Templating Classes Used in the Examples Aspect Oriented Programming with Spring AOP Concepts Spring AOP Capabilities and Goals AOP Proxies @AspectJ support Enabling @AspectJ Support Declaring an Aspect Declaring a Pointcut Declaring Advice Introductions Aspect Instantiation Models An AOP Example Schema-based AOP Support Choosing which AOP Declaration Style to Use Mixing Aspect Types Proxying Mechanisms Programmatic Creation of @AspectJ Proxies Using AspectJ with Spring Applications Further Resources Spring AOP APIs Pointcut API in Spring Advice API in Spring The Advisor API in Spring Using the ProxyFactoryBean to Create AOP Proxies Concise Proxy Definitions Creating AOP Proxies Programmatically with the ProxyFactory Manipulating Advised Objects Using the "auto-proxy" facility Using TargetSource Implementations Defining New Advice Types Resilience Features Null-safety Data Buffers and Codecs Ahead of Time Optimizations Appendix XML Schemas XML Schema Authoring Application Startup Steps Data Access Transaction Management Advantages of the Spring Framework’s Transaction Support Model Understanding the Spring Framework Transaction Abstraction Synchronizing Resources with Transactions Declarative Transaction Management Understanding the Spring Framework’s Declarative Transaction Implementation Example of Declarative Transaction Implementation Rolling Back a Declarative Transaction Configuring Different Transactional Semantics for Different Beans <tx:advice/> Settings Using @Transactional Transaction Propagation Advising Transactional Operations Using @Transactional with AspectJ Programmatic Transaction Management Choosing Between Programmatic and Declarative Transaction Management Transaction-bound Events Application server-specific integration Solutions to Common Problems Further Resources DAO Support Data Access with JDBC Choosing an Approach for JDBC Database Access Package Hierarchy Using the JDBC Core Classes to Control Basic JDBC Processing and Error Handling Controlling Database Connections JDBC Batch Operations Simplifying JDBC Operations with the SimpleJdbc Classes Modeling JDBC Operations as Java Objects Common Problems with Parameter and Data Value Handling Embedded Database Support Initializing a DataSource Data Access with R2DBC Object Relational Mapping (ORM) Data Access Introduction to ORM with Spring General ORM Integration Considerations Hibernate JPA Marshalling XML by Using Object-XML Mappers Appendix Web on Servlet Stack Spring Web MVC DispatcherServlet Context Hierarchy Special Bean Types Web MVC Config Servlet Config Processing Path Matching Interception Exceptions View Resolution Locale Multipart Resolver Logging Filters HTTP Message Conversion Annotated Controllers Declaration Mapping Requests Handler Methods Method Arguments Return Values Type Conversion Matrix Variables @RequestParam @RequestHeader @CookieValue @ModelAttribute @SessionAttributes @SessionAttribute @RequestAttribute Redirect Attributes Flash Attributes Multipart @RequestBody HttpEntity @ResponseBody ResponseEntity Jackson JSON Model @InitBinder Validation Exceptions Controller Advice Functional Endpoints URI Links Asynchronous Requests Range Requests Data Binding CORS API Versioning Error Responses Web Security HTTP Caching View Technologies Thymeleaf FreeMarker Groovy Markup Script Views HTML Fragments JSP and JSTL RSS and Atom PDF and Excel Jackson XML Marshalling XSLT Views MVC Config Enable MVC Configuration MVC Config API Type Conversion Validation Interceptors Content Types Message Converters View Controllers View Resolvers Static Resources Default Servlet Path Matching API Version Advanced Java Config Advanced XML Config HTTP/2 REST Clients Testing WebSockets WebSocket API SockJS Fallback STOMP Overview Benefits Enable STOMP WebSocket Transport Flow of Messages Annotated Controllers Sending Messages Simple Broker External Broker Connecting to a Broker Dots as Separators Authentication Token Authentication Authorization User Destinations Order of Messages Events Interception STOMP Client WebSocket Scope Performance Monitoring Testing Web on Reactive Stack Spring WebFlux Overview Reactive Core DispatcherHandler Annotated Controllers @Controller Mapping Requests Handler Methods Method Arguments Return Values Type Conversion Matrix Variables @RequestParam @RequestHeader @CookieValue @ModelAttribute @SessionAttributes @SessionAttribute @RequestAttribute Multipart Content @RequestBody HttpEntity @ResponseBody ResponseEntity Jackson JSON Model DataBinder Validation Exceptions Controller Advice Functional Endpoints URI Links Range Requests Data Binding CORS API Versioning Error Responses Web Security HTTP Caching View Technologies WebFlux Config HTTP/2 WebClient Configuration retrieve() Exchange Request Body Filters Attributes Context Synchronous Use Testing HTTP Service Client WebSockets Testing RSocket Reactive Libraries Testing Introduction to Spring Testing Unit Testing Integration Testing JDBC Testing Support Spring TestContext Framework Key Abstractions Bootstrapping the TestContext Framework TestExecutionListener Configuration Application Events Test Execution Events Context Management Context Configuration with Component Classes Context Configuration with XML Resources Context Configuration with Groovy Scripts Default Context Configuration Mixing Component Classes, XML, and Groovy Scripts Context Configuration with Context Customizers Context Configuration with Context Initializers Context Configuration Inheritance Context Configuration with Environment Profiles Context Configuration with Test Property Sources Context Configuration with Dynamic Property Sources Loading a WebApplicationContext Working with Web Mocks Context Caching Context Pausing Context Failure Threshold Context Hierarchies Dependency Injection of Test Fixtures Bean Overriding in Tests Testing Request- and Session-scoped Beans Transaction Management Executing SQL Scripts Parallel Test Execution TestContext Framework Support Classes Ahead of Time Support for Tests WebTestClient RestTestClient MockMvc Overview Setup Options Hamcrest Integration Static Imports Configuring MockMvc Setup Features Performing Requests Defining Expectations Async Requests Streaming Responses Filter Registrations AssertJ Integration Configuring MockMvcTester Performing Requests Defining Expectations MockMvc integration HtmlUnit Integration Why HtmlUnit Integration? MockMvc and HtmlUnit MockMvc and WebDriver MockMvc and Geb MockMvc vs End-to-End Tests Further Examples Testing Client Applications Appendix Annotations Standard Annotation Support Spring Testing Annotations @BootstrapWith @ContextConfiguration @WebAppConfiguration @ContextHierarchy @ContextCustomizerFactories @ActiveProfiles @TestPropertySource @DynamicPropertySource @TestBean @MockitoBean and @MockitoSpyBean @DirtiesContext @TestExecutionListeners @RecordApplicationEvents @Commit @Rollback @BeforeTransaction @AfterTransaction @Sql @SqlConfig @SqlMergeMode @SqlGroup @DisabledInAotMode Spring JUnit 4 Testing Annotations Spring JUnit Jupiter Testing Annotations Meta-Annotation Support for Testing Further Resources Integration REST Clients JMS (Java Message Service) Using Spring JMS Sending a Message Receiving a Message Support for JCA Message Endpoints Annotation-driven Listener Endpoints JMS Namespace Support JMX Exporting Your Beans to JMX Controlling the Management Interface of Your Beans Controlling ObjectName Instances for Your Beans Using JSR-160 Connectors Accessing MBeans through Proxies Notifications Further Resources Email Task Execution and Scheduling Cache Abstraction Understanding the Cache Abstraction Declarative Annotation-based Caching JCache (JSR-107) Annotations Declarative XML-based Caching Configuring the Cache Storage Plugging-in Different Back-end Caches How can I Set the TTL/TTI/Eviction policy/XXX feature? Observability Support JVM AOT Cache JVM Checkpoint Restore Appendix Language Support Kotlin Requirements Extensions Null-safety Classes and Interfaces Annotations Bean Registration DSL Web Coroutines Spring Projects in Kotlin Getting Started Resources Apache Groovy Appendix Java API Kotlin API Wiki Search Edit this Page GitHub Project Stack Overflow Spring Framework Integration Cache Abstraction Declarative Annotation-based Caching Declarative Annotation-based Caching For caching declaration, Spring’s caching abstraction provides a set of Java annotations: @Cacheable : Triggers cache population. @CacheEvict : Triggers cache eviction. @CachePut : Updates the cache without interfering with the method execution. @Caching : Regroups multiple cache operations to be applied on a method. @CacheConfig : Shares some common cache-related settings at class-level. The @Cacheable Annotation As the name implies, you can use @Cacheable to demarcate methods that are cacheable — that is, methods for which the result is stored in the cache so that, on subsequent invocations (with the same arguments), the value in the cache is returned without having to actually invoke the method. In its simplest form, the annotation declaration requires the name of the cache associated with the annotated method, as the following example shows: @Cacheable("books") public Book findBook(ISBN isbn) {...} In the preceding snippet, the findBook method is associated with the cache named books . Each time the method is called, the cache is checked to see whether the invocation has already been run and does not have to be repeated. While in most cases, only one cache is declared, the annotation lets multiple names be specified so that more than one cache is being used. In this case, each of the caches is checked before invoking the method — if at least one cache is hit, the associated value is returned. All the other caches that do not contain the value are also updated, even though the cached method was not actually invoked. The following example uses @Cacheable on the findBook method with multiple caches: @Cacheable({"books", "isbns"}) public Book findBook(ISBN isbn) {...} Default Key Generation Since caches are essentially key-value stores, each invocation of a cached method needs to be translated into a suitable key for cache access. The caching abstraction uses a simple KeyGenerator based on the following algorithm: If no parameters are given, return SimpleKey.EMPTY . If only one parameter is given, return that instance. If more than one parameter is given, return a SimpleKey that contains all parameters. This approach works well for most use-cases, as long as parameters have natural keys and implement valid hashCode() and equals() methods. If that is not the case, you need to change the strategy. To provide a different default key generator, you need to implement the org.springframework.cache.interceptor.KeyGenerator interface. The default key generation strategy changed with the release of Spring 4.0. Earlier versions of Spring used a key generation strategy that, for multiple key parameters, considered only the hashCode() of parameters and not equals() . This could cause unexpected key collisions (see spring-framework#14870 for background). The new SimpleKeyGenerator uses a compound key for such scenarios. If you want to keep using the previous key strategy, you can configure the deprecated org.springframework.cache.interceptor.DefaultKeyGenerator class or create a custom hash-based KeyGenerator implementation. Custom Key Generation Declaration Since caching is generic, the target methods are quite likely to have various signatures that cannot be readily mapped on top of the cache structure. This tends to become obvious when the target method has multiple arguments out of which only some are suitable for caching (while the rest are used only by the method logic). Consider the following example: @Cacheable("books") public Book findBook(ISBN isbn, boolean checkWarehouse, boolean includeUsed) At first glance, while the two boolean arguments influence the way the book is found, they are no use for the cache. Furthermore, what if only one of the two is important while the other is not? For such cases, the @Cacheable annotation lets you specify how the key is generated through its key attribute. You can use SpEL to pick the arguments of interest (or their nested properties), perform operations, or even invoke arbitrary methods without having to write any code or implement any interface. This is the recommended approach over the default generator , since methods tend to be quite different in signatures as the code base grows. While the default strategy might work for some methods, it rarely works for all methods. The following examples use various SpEL declarations (if you are not familiar with SpEL, do yourself a favor and read Spring Expression Language ): @Cacheable(cacheNames="books", key="#isbn") public Book findBook(ISBN isbn, boolean checkWarehouse, boolean includeUsed) @Cacheable(cacheNames="books", key="#isbn.rawNumber") public Book findBook(ISBN isbn, boolean checkWarehouse, boolean includeUsed) @Cacheable(cacheNames="books", key="T(someType).hash(#isbn)") public Book findBook(ISBN isbn, boolean checkWarehouse, boolean includeUsed) The preceding snippets show how easy it is to select a certain argument, one of its properties, or even an arbitrary (static) method. If the algorithm responsible for generating the key is too specific or if it needs to be shared, you can define a custom keyGenerator on the operation. To do so, specify the name of the KeyGenerator bean implementation to use, as the following example shows: @Cacheable(cacheNames="books", keyGenerator="myKeyGenerator") public Book findBook(ISBN isbn, boolean checkWarehouse, boolean includeUsed) The key and keyGenerator parameters are mutually exclusive and an operation that specifies both results in an exception. Default Cache Resolution The caching abstraction uses a simple CacheResolver that retrieves the caches defined at the operation level by using the configured CacheManager . To provide a different default cache resolver, you need to implement the org.springframework.cache.interceptor.CacheResolver interface. Custom Cache Resolution The default cache resolution fits well for applications that work with a single CacheManager and have no complex cache resolution requirements. For applications that work with several cache managers, you can set the cacheManager to use for each operation, as the following example shows: @Cacheable(cacheNames="books", cacheManager="anotherCacheManager") (1) public Book findBook(ISBN isbn) {...} 1 Specifying anotherCacheManager . You can also replace the CacheResolver entirely in a fashion similar to that of replacing key generation . The resolution is requested for every cache operation, letting the implementation actually resolve the caches to use based on runtime arguments. The following example shows how to specify a CacheResolver : @Cacheable(cacheResolver="runtimeCacheResolver") (1) public Book findBook(ISBN isbn) {...} 1 Specifying the CacheResolver . Since Spring 4.1, the value attribute of the cache annotations are no longer mandatory, since this particular information can be provided by the CacheResolver regardless of the content of the annotation. Similarly to key and keyGenerator , the cacheManager and cacheResolver parameters are mutually exclusive, and an operation specifying both results in an exception, as a custom CacheManager is ignored by the CacheResolver implementation. This is probably not what you expect. Synchronized Caching In a multi-threaded environment, certain operations might be concurrently invoked for the same argument (typically on startup). By default, the cache abstraction does not lock anything, and the same value may be computed several times, defeating the purpose of caching. For those particular cases, you can use the sync attribute to instruct the underlying cache provider to lock the cache entry while the value is being computed. As a result, only one thread is busy computing the value, while the others are blocked until the entry is updated in the cache. The following example shows how to use the sync attribute: @Cacheable(cacheNames="foos", sync=true) (1) public Foo executeExpensiveOperation(String id) {...} 1 Using the sync attribute. This is an optional feature, and your favorite cache library may not support it. All CacheManager implementations provided by the core framework support it. See the documentation of your cache provider for more details. Caching with CompletableFuture and Reactive Return Types As of 6.1, cache annotations take CompletableFuture and reactive return types into account, automatically adapting the cache interaction accordingly. For a method returning a CompletableFuture , the object produced by that future will be cached whenever it is complete, and the cache lookup for a cache hit will be retrieved via a CompletableFuture : @Cacheable("books") public CompletableFuture<Book> findBook(ISBN isbn) {...} For a method returning a Reactor Mono , the object emitted by that Reactive Streams publisher will be cached whenever it is available, and the cache lookup for a cache hit will be retrieved as a Mono (backed by a CompletableFuture ): @Cacheable("books") public Mono<Book> findBook(ISBN isbn) {...} For a method returning a Reactor Flux , the objects emitted by that Reactive Streams publisher will be collected into a List and cached whenever that list is complete, and the cache lookup for a cache hit will be retrieved as a Flux (backed by a CompletableFuture for the cached List value): @Cacheable("books") public Flux<Book> findBooks(String author) {...} Such CompletableFuture and reactive adaptation also works for synchronized caching, computing the value only once in case of a concurrent cache miss: @Cacheable(cacheNames="foos", sync=true) (1) public CompletableFuture<Foo> executeExpensiveOperation(String id) {...} 1 Using the sync attribute. In order for such an arrangement to work at runtime, the configured cache needs to be capable of CompletableFuture -based retrieval. The Spring-provided ConcurrentMapCacheManager automatically adapts to that retrieval style, and CaffeineCacheManager natively supports it when its asynchronous cache mode is enabled: set setAsyncCacheMode(true) on your CaffeineCacheManager instance. @Bean CacheManager cacheManager() { CaffeineCacheManager cacheManager = new CaffeineCacheManager(); cacheManager.setCacheSpecification(...); cacheManager.setAsyncCacheMode(true); return cacheManager; } Last but not least, be aware that annotation-driven caching is not appropriate for sophisticated reactive interactions involving composition and back pressure. If you choose to declare @Cacheable on specific reactive methods, consider the impact of the rather coarse-granular cache interaction which simply stores the emitted object for a Mono or even a pre-collected list of objects for a Flux . Conditional Caching Sometimes, a method might not be suitable for caching all the time (for example, it might depend on the given arguments). The cache annotations support such use cases through the condition parameter, which takes a SpEL expression that is evaluated to either true or false . If true , the method is cached. If not, it behaves as if the method is not cached (that is, the method is invoked every time no matter what values are in the cache or what arguments are used). For example, the following method is cached only if the argument name has a length shorter than 32: @Cacheable(cacheNames="book", condition="#name.length() < 32") (1) public Book findBook(String name) 1 Setting a condition on @Cacheable . In addition to the condition parameter, you can use the unless parameter to veto the adding of a value to the cache. Unlike condition , unless expressions are evaluated after the method has been invoked. To expand on the previous example, perhaps we only want to cache paperback books, as the following example does: @Cacheable(cacheNames="book", condition="#name.length() < 32", unless="#result.hardback") (1) public Book findBook(String name) 1 Using the unless attribute to block hardbacks. The cache abstraction supports java.util.Optional return types. If an Optional value is present , it will be stored in the associated cache. If an Optional value is not present, null will be stored in the associated cache. #result always refers to the business entity and never a supported wrapper, so the previous example can be rewritten as follows: @Cacheable(cacheNames="book", condition="#name.length() < 32", unless="#result?.hardback") public Optional<Book> findBook(String name) Note that #result still refers to Book and not Optional<Book> . Since it might be null , we use SpEL’s safe navigation operator . Available Caching SpEL Evaluation Context Each SpEL expression evaluates against a dedicated context . In addition to the built-in parameters, the framework provides dedicated caching-related metadata, such as the argument names. The following table describes the items made available to the context so that you can use them for key and conditional computations: Table 1. Cache metadata available in SpEL expressions Name Location Description Example methodName Root object The name of the method being invoked #root.methodName method Root object The method being invoked #root.method.name target Root object The target object being invoked #root.target targetClass Root object The class of the target being invoked #root.targetClass args Root object The arguments (as an object array) used for invoking the target #root.args[0] caches Root object Collection of caches against which the current method is run #root.caches[0].name Argument name Evaluation context The name of a particular method argument. If the names are not available (for example, because the code was compiled without the -parameters flag), individual arguments are also available using the #a<#arg> syntax where <#arg> stands for the argument index (starting from 0). #iban or #a0 (you can also use #p0 or #p<#arg> notation as an alias). result Evaluation context The result of the method call (the value to be cached). Only available in unless expressions, cache put expressions (to compute the key ), or cache evict expressions (when beforeInvocation is false ). For supported wrappers (such as Optional ), #result refers to the actual object, not the wrapper. #result The @CachePut Annotation When the cache needs to be updated without interfering with the method execution, you can use the @CachePut annotation. That is, the method is always invoked and its result is placed into the cache (according to the @CachePut options). It supports the same options as @Cacheable and should be used for cache population rather than method flow optimization. The following example uses the @CachePut annotation: @CachePut(cacheNames="book", key="#isbn") public Book updateBook(ISBN isbn, BookDescriptor descriptor) Using @CachePut and @Cacheable annotations on the same method is generally strongly discouraged because they have different behaviors. While the latter causes the method invocation to be skipped by using the cache, the former forces the invocation in order to run a cache update. This leads to unexpected behavior and, with the exception of specific corner-cases (such as annotations having conditions that exclude them from each other), such declarations should be avoided. Note also that such conditions should not rely on the result object (that is, the #result variable), as these are validated up-front to confirm the exclusion. As of 6.1, @CachePut takes CompletableFuture and reactive return types into account, performing the put operation whenever the produced object is available. The @CacheEvict Annotation The cache abstraction allows not just population of a cache store but also eviction. This process is useful for removing stale or unused data from the cache. As opposed to @Cacheable , @CacheEvict demarcates methods that perform cache eviction (that is, methods that act as triggers for removing data from the cache). Similarly to its sibling, @CacheEvict requires specifying one or more caches that are affected by the action, allows a custom cache and key resolution or a condition to be specified, and features an extra parameter ( allEntries ) that indicates whether a cache-wide eviction needs to be performed rather than just an entry eviction (based on the key). The following example evicts all entries from the books cache: @CacheEvict(cacheNames="books", allEntries=true) (1) public void loadBooks(InputStream batch) 1 Using the allEntries attribute to evict all entries from the cache. This option comes in handy when an entire cache region needs to be cleared out. Rather than evicting each entry (which would take a long time, since it is inefficient), all the entries are removed in one operation, as the preceding example shows. Note that the framework ignores any key specified in this scenario as it does not apply (the entire cache is evicted, not only one entry). You can also indicate whether the eviction should occur after (the default) or before the method is invoked by using the beforeInvocation attribute. The former provides the same semantics as the rest of the annotations: Once the method completes successfully, an action (in this case, eviction) on the cache is run. If the method does not run (as it might be cached) or an exception is thrown, the eviction does not occur. The latter ( beforeInvocation=true ) causes the eviction to always occur before the method is invoked. This is useful in cases where the eviction does not need to be tied to the method outcome. Note that void methods can be used with @CacheEvict - as the methods act as a trigger, the return values are ignored (as they do not interact with the cache). This is not the case with @Cacheable which adds data to the cache or updates data in the cache and, thus, requires a result. As of 6.1, @CacheEvict takes CompletableFuture and reactive return types into account, performing an after-invocation evict operation whenever processing has completed. The @Caching Annotation Sometimes, multiple annotations of the same type (such as @CacheEvict or @CachePut ) need to be specified — for example, because the condition or the key expression is different between different caches. @Caching lets multiple nested @Cacheable , @CachePut , and @CacheEvict annotations be used on the same method. The following example uses two @CacheEvict annotations: @Caching(evict = { @CacheEvict("primary"), @CacheEvict(cacheNames="secondary", key="#p0") }) public Book importBooks(String deposit, Date date) The @CacheConfig Annotation So far, we have seen that caching operations offer many customization options and that you can set these options for each operation. However, some of the customization options can be tedious to configure if they apply to all operations of the class. For instance, specifying the name of the cache to use for every cache operation of the class can be replaced by a single class-level definition. This is where @CacheConfig comes into play. The following examples uses @CacheConfig to set the name of the cache: @CacheConfig("books") (1) public class BookRepositoryImpl implements BookRepository { @Cacheable public Book findBook(ISBN isbn) {...} } 1 Using @CacheConfig to set the name of the cache. @CacheConfig is a class-level annotation that allows sharing the cache names, the custom KeyGenerator , the custom CacheManager , and the custom CacheResolver . Placing this annotation on the class does not turn on any caching operation. An operation-level customization always overrides a customization set on @CacheConfig . Therefore, this gives three levels of customizations for each cache operation: Globally configured, for example, through CachingConfigurer : see next section. At the class level, using @CacheConfig . At the operation level. Provider-specific settings are typically available on the CacheManager bean, for example, on CaffeineCacheManager . These are effectively also global. Enabling Caching Annotations It is important to note that even though declaring the cache annotations does not automatically trigger their actions - like many things in Spring, the feature has to be declaratively enabled (which means if you ever suspect caching is to blame, you can disable it by removing only one configuration line rather than all the annotations in your code). To enable caching annotations add the annotation @EnableCaching to one of your @Configuration classes or use the cache:annotation-driven element with XML: Java Kotlin Xml @Configuration @EnableCaching class CacheConfiguration { @Bean CacheManager cacheManager() { CaffeineCacheManager cacheManager = new CaffeineCacheManager(); cacheManager.setCacheSpecification("..."); return cacheManager; } } @Configuration @EnableCaching class CacheConfiguration { @Bean fun cacheManager(): CacheManager { return CaffeineCacheManager().apply { setCacheSpecification("...") } } } <beans xmlns="http://www.springframework.org/schema/beans" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:cache="http://www.springframework.org/schema/cache" xsi:schemaLocation=" http://www.springframework.org/schema/beans https://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/cache https://www.springframework.org/schema/cache/spring-cache.xsd"> <cache:annotation-driven/> <bean id="cacheManager" class="org.springframework.cache.caffeine.CaffeineCacheManager"> <property name="cacheSpecification" value="..."/> </bean> </beans> Both the cache:annotation-driven element and the @EnableCaching annotation let you specify various options that influence the way the caching behavior is added to the application through AOP. The configuration is intentionally similar with that of @Transactional . The default advice mode for processing caching annotations is proxy , which allows for interception of calls through the proxy only. Local calls within the same class cannot get intercepted that way. For a more advanced mode of interception, consider switching to aspectj mode in combination with compile-time or load-time weaving. For more detail about advanced customizations (using Java configuration) that are required to implement CachingConfigurer , see the javadoc . Table 2. Cache annotation settings XML Attribute Annotation Attribute Default Description cache-manager N/A (see the CachingConfigurer javadoc) cacheManager The name of the cache manager to use. A default CacheResolver is initialized behind the scenes with this cache manager (or cacheManager if not set). For more fine-grained management of the cache resolution, consider setting the 'cache-resolver' attribute. cache-resolver N/A (see the CachingConfigurer javadoc) A SimpleCacheResolver using the configured cacheManager . The bean name of the CacheResolver that is to be used to resolve the backing caches. This attribute is not required and needs to be specified only as an alternative to the 'cache-manager' attribute. key-generator N/A (see the CachingConfigurer javadoc) SimpleKeyGenerator Name of the custom key generator to use. error-handler N/A (see the CachingConfigurer javadoc) SimpleCacheErrorHandler The name of the custom cache error handler to use. By default, any exception thrown during a cache related operation is thrown back at the client. mode mode proxy The default mode ( proxy ) processes annotated beans to be proxied by using Spring’s AOP framework (following proxy semantics, as discussed earlier, applying to method calls coming in through the proxy only). The alternative mode ( aspectj ) instead weaves the affected classes with Spring’s AspectJ caching aspect, modifying the target class byte code to apply to any kind of method call. AspectJ weaving requires spring-aspects.jar in the classpath as well as load-time weaving (or compile-time weaving) enabled. (See Spring configuration for details on how to set up load-time weaving.) proxy-target-class proxyTargetClass false Applies to proxy mode only. Controls what type of caching proxies are created for classes annotated with the @Cacheable or @CacheEvict annotations. If the proxy-target-class attribute is set to true , class-based proxies are created. If proxy-target-class is false or if the attribute is omitted, standard JDK interface-based proxies are created. (See Proxying Mechanisms for a detailed examination of the different proxy types.) order order Ordered.LOWEST_PRECEDENCE Defines the order of the cache advice that is applied to beans annotated with @Cacheable or @CacheEvict . (For more information about the rules related to ordering AOP advice, see Advice Ordering .) No specified ordering means that the AOP subsystem determines the order of the advice. <cache:annotation-driven/> looks for @Cacheable/@CachePut/@CacheEvict/@Caching only on beans in the same application context in which it is defined. This means that, if you put <cache:annotation-driven/> in a WebApplicationContext for a DispatcherServlet , it checks for beans only in your controllers, not your services. See the MVC section for more information. Method visibility and cache annotations When you use proxies, you should apply the cache annotations only to methods with public visibility. If you do annotate protected, private, or package-visible methods with these annotations, no error is raised, but the annotated method does not exhibit the configured caching settings. Consider using AspectJ (see the rest of this section) if you need to annotate non-public methods, as it changes the bytecode itself. Spring recommends that you only annotate concrete classes (and methods of concrete classes) with the @Cache* annotations, as opposed to annotating interfaces. You certainly can place an @Cache* annotation on an interface (or an interface method), but this works only if you use the proxy mode ( mode="proxy" ). If you use the weaving-based aspect ( mode="aspectj" ), the caching settings are not recognized on interface-level declarations by the weaving infrastructure. In proxy mode (the default), only external method calls coming in through the proxy are intercepted. This means that self-invocation (in effect, a method within the target object that calls another method of the target object) does not lead to actual caching at runtime even if the invoked method is marked with @Cacheable . Consider using the aspectj mode in this case. Also, the proxy must be fully initialized to provide the expected behavior, so you should not rely on this feature in your initialization code (that is, @PostConstruct ). Using Custom Annotations Custom annotation and AspectJ This feature works only with the proxy-based approach but can be enabled with a bit of extra effort by using AspectJ. The spring-aspects module defines an aspect for the standard annotations only. If you have defined your own annotations, you also need to define an aspect for those. Check AnnotationCacheAspect for an example. The caching abstraction lets you use your own annotations to identify what method triggers cache population or eviction. This is quite handy as a template mechanism, as it eliminates the need to duplicate cache annotation declarations, which is especially useful if the key or condition are specified or if the foreign imports ( org.springframework ) are not allowed in your code base. Similarly to the rest of the stereotype annotations, you can use @Cacheable , @CachePut , @CacheEvict , and @CacheConfig as meta-annotations (that is, annotations that can annotate other annotations). In the following example, we replace a common @Cacheable declaration with our own custom annotation: @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.METHOD}) @Cacheable(cacheNames="books", key="#isbn") public @interface SlowService { } In the preceding example, we have defined our own SlowService annotation, which itself is annotated with @Cacheable . Now we can replace the following code: @Cacheable(cacheNames="books", key="#isbn") public Book findBook(ISBN isbn, boolean checkWarehouse, boolean includeUsed) The following example shows the custom annotation with which we can replace the preceding code: @SlowService public Book findBook(ISBN isbn, boolean checkWarehouse, boolean includeUsed) Even though @SlowService is not a Spring annotation, the container automatically picks up its declaration at runtime and understands its meaning. Note that, as mentioned earlier , annotation-driven behavior needs to be enabled. Understanding the Cache Abstraction JCache (JSR-107) Annotations Spring Framework Stable 7.0.8 6.2.19 Snapshot 7.1.0-SNAPSHOT 7.0.9-SNAPSHOT 6.2.20-SNAPSHOT Related Spring Documentation Spring Boot Spring Framework Spring Cloud Spring Cloud Build Spring Cloud Bus Spring Cloud Circuit Breaker Spring Cloud Commons Spring Cloud Config Spring Cloud Consul Spring Cloud Contract Spring Cloud Function Spring Cloud Gateway Spring Cloud Kubernetes Spring Cloud Netflix Spring Cloud OpenFeign Spring Cloud Stream Spring Cloud Task Spring Cloud Vault Spring Cloud Zookeeper Spring Data Spring Data Cassandra Spring Data Commons Spring Data Couchbase Spring Data Elasticsearch Spring Data JPA Spring Data KeyValue Spring Data LDAP Spring Data MongoDB Spring Data Neo4j Spring Data Redis Spring Data JDBC & R2DBC Spring Data REST Spring Integration Spring Batch Spring Security Spring Authorization Server Spring LDAP Spring Security Kerberos Spring Session Spring Vault Spring AI Spring AMQP Spring CLI Spring GraphQL Spring for Apache Kafka Spring Modulith Spring for Apache Pulsar Spring Shell All Docs... Copyright © 2005 - Broadcom. All Rights Reserved. The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries. Terms of Use • Privacy • Trademark Guidelines • Thank you • Your California Privacy Rights • Cookie Settings Apache®, Apache Tomcat®, Apache Kafka®, Apache Cassandra™, and Apache Geode™ are trademarks or registered trademarks of the Apache Software Foundation in the United States and/or other countries. Java™, Java™ SE, Java™ EE, and OpenJDK™ are trademarks of Oracle and/or its affiliates. Kubernetes® is a registered trademark of the Linux Foundation in the United States and other countries. Linux® is the registered trademark of Linus Torvalds in the United States and other countries. Windows® and Microsoft® Azure are registered trademarks of Microsoft Corporation. “AWS” and “Amazon Web Services” are trademarks or registered trademarks of Amazon.com Inc. or its affiliates. All other trademarks and copyrights are property of their respective owners and are only mentioned for informative purposes. Other names may be trademarks of their respective owners. Search in all Spring Docs
