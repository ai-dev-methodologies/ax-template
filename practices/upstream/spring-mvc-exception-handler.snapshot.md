# spring-mvc-exception-handler — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T02:24:30Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r146`
**Body SHA-256 (below the `---` divider, header excluded):** e117e971b8e94eb3e377ef18e83dc98761318b7c159df562141b7c341148021e

---

---
snapshot_id: spring-mvc-exception-handler
source: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 3
bytes: 12566
sha: "0b28cda1dc2eedab6c95ac7c02578ddf46643c5b14f7993dc89e160e386a8898"
---

# spring mvc exception handler — upstream snapshot

Source: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html
Fetched: 2026-07-14

Exceptions :: Spring Framework
Edit this Page
 
 
 
 GitHub Project
 
 
 
 Stack Overflow

# Exceptions
See equivalent in the Reactive stack
@Controller and @ControllerAdvice classes can have
@ExceptionHandler methods to handle exceptions from controller methods, as the following example shows:
Java
Kotlin
import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Controller
public class SimpleController {

 @ExceptionHandler(IOException.class)
 public ResponseEntity handle() {
 return ResponseEntity.internalServerError().body("Could not read file storage");
 }

}
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ExceptionHandler
import java.io.IOException

@Controller
class SimpleController {

 @ExceptionHandler(IOException::class)
 fun handle() : ResponseEntity {
 return ResponseEntity.internalServerError().body("Could not read file storage")
 }
 
}

## Exception Mapping
The exception may match against a top-level exception being propagated (for example, a direct
IOException being thrown) or against a nested cause within a wrapper exception (for example,
an IOException wrapped inside an IllegalStateException). As of 5.3, this can match
at arbitrary cause levels, whereas previously only an immediate cause was considered.
For matching exception types, preferably declare the target exception as a method argument,
as the preceding example shows. When multiple exception methods match, a root exception match is
generally preferred to a cause exception match. More specifically, the ExceptionDepthComparator
is used to sort exceptions based on their depth from the thrown exception type.
Alternatively, the annotation declaration may narrow the exception types to match,
as the following example shows:
Java
Kotlin
@ExceptionHandler({FileSystemException.class, RemoteException.class})
public ResponseEntity handleIOException(IOException ex) {
 return ResponseEntity.internalServerError().body(ex.getMessage());
}
@ExceptionHandler(FileSystemException::class, RemoteException::class)
fun handleIOException(ex: IOException): ResponseEntity {
 return ResponseEntity.internalServerError().body(ex.message)
}
You can even use a list of specific exception types with a very generic argument signature,
as the following example shows:
Java
Kotlin
@ExceptionHandler({FileSystemException.class, RemoteException.class})
public ResponseEntity handleExceptions(Exception ex) {
 return ResponseEntity.internalServerError().body(ex.getMessage());
}
@ExceptionHandler(FileSystemException::class, RemoteException::class)
fun handleExceptions(ex: Exception): ResponseEntity {
 return ResponseEntity.internalServerError().body(ex.message)
}
The distinction between root and cause exception matching can be surprising.
In the IOException variant shown earlier, the method is typically called with
the actual FileSystemException or RemoteException instance as the argument,
since both of them extend from IOException. However, if any such matching
exception is propagated within a wrapper exception which is itself an IOException,
the passed-in exception instance is that wrapper exception.
The behavior is even simpler in the handle(Exception) variant. This is
always invoked with the wrapper exception in a wrapping scenario, with the
actually matching exception to be found through ex.getCause() in that case.
The passed-in exception is the actual FileSystemException or
RemoteException instance only when these are thrown as top-level exceptions.
We generally recommend that you be as specific as possible in the argument signature,
reducing the potential for mismatches between root and cause exception types.
Consider breaking a multi-matching method into individual @ExceptionHandler
methods, each matching a single specific exception type through its signature.
In a multi-@ControllerAdvice arrangement, we recommend declaring your primary root exception
mappings on a @ControllerAdvice prioritized with a corresponding order. While a root
exception match is preferred to a cause, this is defined among the methods of a given
controller or @ControllerAdvice class. This means a cause match on a higher-priority
@ControllerAdvice bean is preferred to any match (for example, root) on a lower-priority
@ControllerAdvice bean.
Last but not least, an @ExceptionHandler method implementation can choose to back
out of dealing with a given exception instance by rethrowing it in its original form.
This is useful in scenarios where you are interested only in root-level matches or in
matches within a specific context that cannot be statically determined. A rethrown
exception is propagated through the remaining resolution chain, as though
the given @ExceptionHandler method would not have matched in the first place.
Support for @ExceptionHandler methods in Spring MVC is built on the DispatcherServlet
level, HandlerExceptionResolver mechanism.

## Media Type Mapping
See equivalent in the Reactive stack
In addition to exception types, @ExceptionHandler methods can also declare producible media types.
This allows to refine error responses depending on the media types requested by HTTP clients, typically in the "Accept" HTTP request header.
Applications can declare producible media types directly on annotations, for the same exception type:
Java
Kotlin
@ExceptionHandler(produces = "application/json")
public ResponseEntity handleJson(IllegalArgumentException exc) {
 return ResponseEntity.badRequest().body(new ErrorMessage(exc.getMessage(), 42));
}

@ExceptionHandler(produces = "text/html")
public String handle(IllegalArgumentException exc, Model model) {
 model.addAttribute("error", new ErrorMessage(exc.getMessage(), 42));
 return "errorView";
}
@ExceptionHandler(produces = ["application/json"])
fun handleJson(exc: IllegalArgumentException): ResponseEntity {
 return ResponseEntity.badRequest().body(ErrorMessage(exc.message, 42))
}

@ExceptionHandler(produces = ["text/html"])
fun handle(exc: IllegalArgumentException, model: Model): String {
 model.addAttribute("error", ErrorMessage(exc.message, 42))
 return "errorView"
}
Here, methods handle the same exception type but will not be rejected as duplicates.
Instead, API clients requesting "application/json" will receive a JSON error, and browsers will get an HTML error view.
Each @ExceptionHandler annotation can declare several producible media types,
the content negotiation during the error handling phase will decide which content type will be used.

## Method Arguments
See equivalent in the Reactive stack
@ExceptionHandler methods support the following arguments:
Method argument
Description
Exception type
For access to the raised exception.
HandlerMethod
For access to the controller method that raised the exception.
WebRequest, NativeWebRequest
Generic access to request parameters and request and session attributes without direct
 use of the Servlet API.
jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse
Choose any specific request or response type (for example, ServletRequest or
 HttpServletRequest or Spring’s MultipartRequest or MultipartHttpServletRequest).
jakarta.servlet.http.HttpSession
Enforces the presence of a session. As a consequence, such an argument is never null.

 Note that session access is not thread-safe. Consider setting the
 RequestMappingHandlerAdapter instance’s synchronizeOnSession flag to true if multiple
 requests are allowed to access a session concurrently.
java.security.Principal
Currently authenticated user — possibly a specific Principal implementation class if known.
HttpMethod
The HTTP method of the request.
java.util.Locale
The current request locale, determined by the most specific LocaleResolver available — in
 effect, the configured LocaleResolver or LocaleContextResolver.
java.util.TimeZone, java.time.ZoneId
The time zone associated with the current request, as determined by a LocaleContextResolver.
java.io.OutputStream, java.io.Writer
For access to the raw response body, as exposed by the Servlet API.
java.util.Map, org.springframework.ui.Model, org.springframework.ui.ModelMap
For access to the model for an error response. Always empty.
RedirectAttributes
Specify attributes to use in case of a redirect — (that is to be appended to the query
 string) and flash attributes to be stored temporarily until the request after the redirect.
 See Redirect Attributes and Flash Attributes.
@SessionAttribute
For access to any session attribute, in contrast to model attributes stored in the
 session as a result of a class-level @SessionAttributes declaration.
 See @SessionAttribute for more details.
@RequestAttribute
For access to request attributes. See @RequestAttribute for more details.

## Return Values
See equivalent in the Reactive stack
@ExceptionHandler methods support the following return values:
Return value
Description
@ResponseBody
The return value is converted through HttpMessageConverter instances and written to the
 response. See @ResponseBody.
HttpEntity, ResponseEntity
The return value specifies that the full response (including the HTTP headers and the body)
 be converted through HttpMessageConverter instances and written to the response.
 See ResponseEntity.
ErrorResponse, ProblemDetail
To render an RFC 9457 error response with details in the body,
 see Error Responses
String
A view name to be resolved with ViewResolver implementations and used together with the
 implicit model — determined through command objects and @ModelAttribute methods.
 The handler method can also programmatically enrich the model by declaring a Model
 argument (described earlier).
View
A View instance to use for rendering together with the implicit model — determined
 through command objects and @ModelAttribute methods. The handler method may also
 programmatically enrich the model by declaring a Model argument (described earlier).
java.util.Map, org.springframework.ui.Model
Attributes to be added to the implicit model with the view name implicitly determined
 through a RequestToViewNameTranslator.
@ModelAttribute
An attribute to be added to the model with the view name implicitly determined through
 a RequestToViewNameTranslator.
Note that @ModelAttribute is optional. See “Any other return value” at the end of
 this table.
ModelAndView object
The view and model attributes to use and, optionally, a response status.
void
A method with a void return type (or null return value) is considered to have fully
 handled the response if it also has a ServletResponse an OutputStream argument, or
 a @ResponseStatus annotation. The same is also true if the controller has made a positive
 ETag or lastModified timestamp check (see Controllers for details).
If none of the above is true, a void return type can also indicate “no response body” for
 REST controllers or default view name selection for HTML controllers.
Any other return value
If a return value is not matched to any of the above and is not a simple type (as determined by
 BeanUtils#isSimpleProperty),
 by default, it is treated as a model attribute to be added to the model. If it is a simple type,
 it remains unresolved.
Spring Framework
7.0.8
6.2.19
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
Search in all Spring Docs

---

## Upstream refresh 2026-08-01 (verbatim extractor output)

Source: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html
HTTP status: 200 · extracted bytes: 24471 · sha256: 879be1a527c41a6308348663c46f24d90b7783483f8212f8217cc3f5a439d86d
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r146`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

Exceptions :: Spring Framework Why Spring Overview Trending Generative AI Cloud Architecture Patterns Microservices Reactive Event Driven Application Types Web Applications Serverless Batch Learn Getting Started Quickstart Guides Academy Courses Get Certified Projects Overview Projects Spring Boot Spring Framework Spring Cloud Spring AI Spring Data Spring Integration Spring Batch Spring Security Foundational Projects Micrometer Reactor Development Tools Spring Tools Spring Initializr Resources Blog Release Calendar Version Mappings Release Highlights Security Advisories GitHub Orgs Spring Projects Spring Cloud Community Overview Events Authors Enterprise Overview Long-term Support Automated Upgrades Governance and Compliance Modern App Development light Spring Framework 7.0.8 Search Overview Core Technologies The IoC Container Introduction to the Spring IoC Container and Beans Container Overview Bean Overview Dependencies Dependency Injection Dependencies and Configuration in Detail Using depends-on Lazy-initialized Beans Autowiring Collaborators Method Injection Bean Scopes Customizing the Nature of a Bean Bean Definition Inheritance Container Extension Points Annotation-based Container Configuration Using @Autowired Fine-tuning Annotation-based Autowiring with @Primary or @Fallback Fine-tuning Annotation-based Autowiring with Qualifiers Using Generics as Autowiring Qualifiers Using CustomAutowireConfigurer Injection with @Resource Using @Value Using @PostConstruct and @PreDestroy Classpath Scanning and Managed Components Using JSR-330 Standard Annotations Java-based Container Configuration Basic Concepts: @Bean and @Configuration Instantiating the Spring Container by Using AnnotationConfigApplicationContext Using the @Bean Annotation Using the @Configuration annotation Composing Java-based Configurations Programmatic Bean Registration Environment Abstraction Registering a LoadTimeWeaver Additional Capabilities of the ApplicationContext The BeanFactory API Resources Validation, Data Binding, and Type Conversion Validation Using Spring’s Validator Interface Data Binding Resolving Error Codes to Error Messages Spring Type Conversion Spring Field Formatting Configuring a Global Date and Time Format Java Bean Validation Spring Expression Language (SpEL) Evaluation Expressions in Bean Definitions Language Reference Literal Expressions Properties, Arrays, Lists, Maps, and Indexers Inline Lists Inline Maps Array Construction Methods Operators Types Constructors Variables Functions Varargs Invocations Bean References Ternary Operator (If-Then-Else) The Elvis Operator Safe Navigation Operator Collection Selection Collection Projection Expression Templating Classes Used in the Examples Aspect Oriented Programming with Spring AOP Concepts Spring AOP Capabilities and Goals AOP Proxies @AspectJ support Enabling @AspectJ Support Declaring an Aspect Declaring a Pointcut Declaring Advice Introductions Aspect Instantiation Models An AOP Example Schema-based AOP Support Choosing which AOP Declaration Style to Use Mixing Aspect Types Proxying Mechanisms Programmatic Creation of @AspectJ Proxies Using AspectJ with Spring Applications Further Resources Spring AOP APIs Pointcut API in Spring Advice API in Spring The Advisor API in Spring Using the ProxyFactoryBean to Create AOP Proxies Concise Proxy Definitions Creating AOP Proxies Programmatically with the ProxyFactory Manipulating Advised Objects Using the "auto-proxy" facility Using TargetSource Implementations Defining New Advice Types Resilience Features Null-safety Data Buffers and Codecs Ahead of Time Optimizations Appendix XML Schemas XML Schema Authoring Application Startup Steps Data Access Transaction Management Advantages of the Spring Framework’s Transaction Support Model Understanding the Spring Framework Transaction Abstraction Synchronizing Resources with Transactions Declarative Transaction Management Understanding the Spring Framework’s Declarative Transaction Implementation Example of Declarative Transaction Implementation Rolling Back a Declarative Transaction Configuring Different Transactional Semantics for Different Beans <tx:advice/> Settings Using @Transactional Transaction Propagation Advising Transactional Operations Using @Transactional with AspectJ Programmatic Transaction Management Choosing Between Programmatic and Declarative Transaction Management Transaction-bound Events Application server-specific integration Solutions to Common Problems Further Resources DAO Support Data Access with JDBC Choosing an Approach for JDBC Database Access Package Hierarchy Using the JDBC Core Classes to Control Basic JDBC Processing and Error Handling Controlling Database Connections JDBC Batch Operations Simplifying JDBC Operations with the SimpleJdbc Classes Modeling JDBC Operations as Java Objects Common Problems with Parameter and Data Value Handling Embedded Database Support Initializing a DataSource Data Access with R2DBC Object Relational Mapping (ORM) Data Access Introduction to ORM with Spring General ORM Integration Considerations Hibernate JPA Marshalling XML by Using Object-XML Mappers Appendix Web on Servlet Stack Spring Web MVC DispatcherServlet Context Hierarchy Special Bean Types Web MVC Config Servlet Config Processing Path Matching Interception Exceptions View Resolution Locale Multipart Resolver Logging Filters HTTP Message Conversion Annotated Controllers Declaration Mapping Requests Handler Methods Method Arguments Return Values Type Conversion Matrix Variables @RequestParam @RequestHeader @CookieValue @ModelAttribute @SessionAttributes @SessionAttribute @RequestAttribute Redirect Attributes Flash Attributes Multipart @RequestBody HttpEntity @ResponseBody ResponseEntity Jackson JSON Model @InitBinder Validation Exceptions Controller Advice Functional Endpoints URI Links Asynchronous Requests Range Requests Data Binding CORS API Versioning Error Responses Web Security HTTP Caching View Technologies Thymeleaf FreeMarker Groovy Markup Script Views HTML Fragments JSP and JSTL RSS and Atom PDF and Excel Jackson XML Marshalling XSLT Views MVC Config Enable MVC Configuration MVC Config API Type Conversion Validation Interceptors Content Types Message Converters View Controllers View Resolvers Static Resources Default Servlet Path Matching API Version Advanced Java Config Advanced XML Config HTTP/2 REST Clients Testing WebSockets WebSocket API SockJS Fallback STOMP Overview Benefits Enable STOMP WebSocket Transport Flow of Messages Annotated Controllers Sending Messages Simple Broker External Broker Connecting to a Broker Dots as Separators Authentication Token Authentication Authorization User Destinations Order of Messages Events Interception STOMP Client WebSocket Scope Performance Monitoring Testing Web on Reactive Stack Spring WebFlux Overview Reactive Core DispatcherHandler Annotated Controllers @Controller Mapping Requests Handler Methods Method Arguments Return Values Type Conversion Matrix Variables @RequestParam @RequestHeader @CookieValue @ModelAttribute @SessionAttributes @SessionAttribute @RequestAttribute Multipart Content @RequestBody HttpEntity @ResponseBody ResponseEntity Jackson JSON Model DataBinder Validation Exceptions Controller Advice Functional Endpoints URI Links Range Requests Data Binding CORS API Versioning Error Responses Web Security HTTP Caching View Technologies WebFlux Config HTTP/2 WebClient Configuration retrieve() Exchange Request Body Filters Attributes Context Synchronous Use Testing HTTP Service Client WebSockets Testing RSocket Reactive Libraries Testing Introduction to Spring Testing Unit Testing Integration Testing JDBC Testing Support Spring TestContext Framework Key Abstractions Bootstrapping the TestContext Framework TestExecutionListener Configuration Application Events Test Execution Events Context Management Context Configuration with Component Classes Context Configuration with XML Resources Context Configuration with Groovy Scripts Default Context Configuration Mixing Component Classes, XML, and Groovy Scripts Context Configuration with Context Customizers Context Configuration with Context Initializers Context Configuration Inheritance Context Configuration with Environment Profiles Context Configuration with Test Property Sources Context Configuration with Dynamic Property Sources Loading a WebApplicationContext Working with Web Mocks Context Caching Context Pausing Context Failure Threshold Context Hierarchies Dependency Injection of Test Fixtures Bean Overriding in Tests Testing Request- and Session-scoped Beans Transaction Management Executing SQL Scripts Parallel Test Execution TestContext Framework Support Classes Ahead of Time Support for Tests WebTestClient RestTestClient MockMvc Overview Setup Options Hamcrest Integration Static Imports Configuring MockMvc Setup Features Performing Requests Defining Expectations Async Requests Streaming Responses Filter Registrations AssertJ Integration Configuring MockMvcTester Performing Requests Defining Expectations MockMvc integration HtmlUnit Integration Why HtmlUnit Integration? MockMvc and HtmlUnit MockMvc and WebDriver MockMvc and Geb MockMvc vs End-to-End Tests Further Examples Testing Client Applications Appendix Annotations Standard Annotation Support Spring Testing Annotations @BootstrapWith @ContextConfiguration @WebAppConfiguration @ContextHierarchy @ContextCustomizerFactories @ActiveProfiles @TestPropertySource @DynamicPropertySource @TestBean @MockitoBean and @MockitoSpyBean @DirtiesContext @TestExecutionListeners @RecordApplicationEvents @Commit @Rollback @BeforeTransaction @AfterTransaction @Sql @SqlConfig @SqlMergeMode @SqlGroup @DisabledInAotMode Spring JUnit 4 Testing Annotations Spring JUnit Jupiter Testing Annotations Meta-Annotation Support for Testing Further Resources Integration REST Clients JMS (Java Message Service) Using Spring JMS Sending a Message Receiving a Message Support for JCA Message Endpoints Annotation-driven Listener Endpoints JMS Namespace Support JMX Exporting Your Beans to JMX Controlling the Management Interface of Your Beans Controlling ObjectName Instances for Your Beans Using JSR-160 Connectors Accessing MBeans through Proxies Notifications Further Resources Email Task Execution and Scheduling Cache Abstraction Understanding the Cache Abstraction Declarative Annotation-based Caching JCache (JSR-107) Annotations Declarative XML-based Caching Configuring the Cache Storage Plugging-in Different Back-end Caches How can I Set the TTL/TTI/Eviction policy/XXX feature? Observability Support JVM AOT Cache JVM Checkpoint Restore Appendix Language Support Kotlin Requirements Extensions Null-safety Classes and Interfaces Annotations Bean Registration DSL Web Coroutines Spring Projects in Kotlin Getting Started Resources Apache Groovy Appendix Java API Kotlin API Wiki Search Edit this Page GitHub Project Stack Overflow Spring Framework Web on Servlet Stack Spring Web MVC Annotated Controllers Exceptions Exceptions See equivalent in the Reactive stack @Controller and @ControllerAdvice classes can have @ExceptionHandler methods to handle exceptions from controller methods, as the following example shows: Java Kotlin import java.io.IOException; import org.springframework.http.ResponseEntity; import org.springframework.stereotype.Controller; import org.springframework.web.bind.annotation.ExceptionHandler; @Controller public class SimpleController { @ExceptionHandler(IOException.class) public ResponseEntity<String> handle() { return ResponseEntity.internalServerError().body("Could not read file storage"); } } import org.springframework.http.ResponseEntity import org.springframework.stereotype.Controller import org.springframework.web.bind.annotation.ExceptionHandler import java.io.IOException @Controller class SimpleController { @ExceptionHandler(IOException::class) fun handle() : ResponseEntity<String> { return ResponseEntity.internalServerError().body("Could not read file storage") } } Exception Mapping The exception may match against a top-level exception being propagated (for example, a direct IOException being thrown) or against a nested cause within a wrapper exception (for example, an IOException wrapped inside an IllegalStateException ). As of 5.3, this can match at arbitrary cause levels, whereas previously only an immediate cause was considered. For matching exception types, preferably declare the target exception as a method argument, as the preceding example shows. When multiple exception methods match, a root exception match is generally preferred to a cause exception match. More specifically, the ExceptionDepthComparator is used to sort exceptions based on their depth from the thrown exception type. Alternatively, the annotation declaration may narrow the exception types to match, as the following example shows: Java Kotlin @ExceptionHandler({FileSystemException.class, RemoteException.class}) public ResponseEntity<String> handleIOException(IOException ex) { return ResponseEntity.internalServerError().body(ex.getMessage()); } @ExceptionHandler(FileSystemException::class, RemoteException::class) fun handleIOException(ex: IOException): ResponseEntity<String> { return ResponseEntity.internalServerError().body(ex.message) } You can even use a list of specific exception types with a very generic argument signature, as the following example shows: Java Kotlin @ExceptionHandler({FileSystemException.class, RemoteException.class}) public ResponseEntity<String> handleExceptions(Exception ex) { return ResponseEntity.internalServerError().body(ex.getMessage()); } @ExceptionHandler(FileSystemException::class, RemoteException::class) fun handleExceptions(ex: Exception): ResponseEntity<String> { return ResponseEntity.internalServerError().body(ex.message) } The distinction between root and cause exception matching can be surprising. In the IOException variant shown earlier, the method is typically called with the actual FileSystemException or RemoteException instance as the argument, since both of them extend from IOException . However, if any such matching exception is propagated within a wrapper exception which is itself an IOException , the passed-in exception instance is that wrapper exception. The behavior is even simpler in the handle(Exception) variant. This is always invoked with the wrapper exception in a wrapping scenario, with the actually matching exception to be found through ex.getCause() in that case. The passed-in exception is the actual FileSystemException or RemoteException instance only when these are thrown as top-level exceptions. We generally recommend that you be as specific as possible in the argument signature, reducing the potential for mismatches between root and cause exception types. Consider breaking a multi-matching method into individual @ExceptionHandler methods, each matching a single specific exception type through its signature. In a multi- @ControllerAdvice arrangement, we recommend declaring your primary root exception mappings on a @ControllerAdvice prioritized with a corresponding order. While a root exception match is preferred to a cause, this is defined among the methods of a given controller or @ControllerAdvice class. This means a cause match on a higher-priority @ControllerAdvice bean is preferred to any match (for example, root) on a lower-priority @ControllerAdvice bean. Last but not least, an @ExceptionHandler method implementation can choose to back out of dealing with a given exception instance by rethrowing it in its original form. This is useful in scenarios where you are interested only in root-level matches or in matches within a specific context that cannot be statically determined. A rethrown exception is propagated through the remaining resolution chain, as though the given @ExceptionHandler method would not have matched in the first place. Support for @ExceptionHandler methods in Spring MVC is built on the DispatcherServlet level, HandlerExceptionResolver mechanism. Media Type Mapping See equivalent in the Reactive stack In addition to exception types, @ExceptionHandler methods can also declare producible media types. This allows to refine error responses depending on the media types requested by HTTP clients, typically in the "Accept" HTTP request header. Applications can declare producible media types directly on annotations, for the same exception type: Java Kotlin @ExceptionHandler(produces = "application/json") public ResponseEntity<ErrorMessage> handleJson(IllegalArgumentException exc) { return ResponseEntity.badRequest().body(new ErrorMessage(exc.getMessage(), 42)); } @ExceptionHandler(produces = "text/html") public String handle(IllegalArgumentException exc, Model model) { model.addAttribute("error", new ErrorMessage(exc.getMessage(), 42)); return "errorView"; } @ExceptionHandler(produces = ["application/json"]) fun handleJson(exc: IllegalArgumentException): ResponseEntity<ErrorMessage> { return ResponseEntity.badRequest().body(ErrorMessage(exc.message, 42)) } @ExceptionHandler(produces = ["text/html"]) fun handle(exc: IllegalArgumentException, model: Model): String { model.addAttribute("error", ErrorMessage(exc.message, 42)) return "errorView" } Here, methods handle the same exception type but will not be rejected as duplicates. Instead, API clients requesting "application/json" will receive a JSON error, and browsers will get an HTML error view. Each @ExceptionHandler annotation can declare several producible media types, the content negotiation during the error handling phase will decide which content type will be used. Method Arguments See equivalent in the Reactive stack @ExceptionHandler methods support the following arguments: Method argument Description Exception type For access to the raised exception. HandlerMethod For access to the controller method that raised the exception. WebRequest , NativeWebRequest Generic access to request parameters and request and session attributes without direct use of the Servlet API. jakarta.servlet.ServletRequest , jakarta.servlet.ServletResponse Choose any specific request or response type (for example, ServletRequest or HttpServletRequest or Spring’s MultipartRequest or MultipartHttpServletRequest ). jakarta.servlet.http.HttpSession Enforces the presence of a session. As a consequence, such an argument is never null . Note that session access is not thread-safe. Consider setting the RequestMappingHandlerAdapter instance’s synchronizeOnSession flag to true if multiple requests are allowed to access a session concurrently. java.security.Principal Currently authenticated user — possibly a specific Principal implementation class if known. HttpMethod The HTTP method of the request. java.util.Locale The current request locale, determined by the most specific LocaleResolver available — in effect, the configured LocaleResolver or LocaleContextResolver . java.util.TimeZone , java.time.ZoneId The time zone associated with the current request, as determined by a LocaleContextResolver . java.io.OutputStream , java.io.Writer For access to the raw response body, as exposed by the Servlet API. java.util.Map , org.springframework.ui.Model , org.springframework.ui.ModelMap For access to the model for an error response. Always empty. RedirectAttributes Specify attributes to use in case of a redirect — (that is to be appended to the query string) and flash attributes to be stored temporarily until the request after the redirect. See Redirect Attributes and Flash Attributes . @SessionAttribute For access to any session attribute, in contrast to model attributes stored in the session as a result of a class-level @SessionAttributes declaration. See @SessionAttribute for more details. @RequestAttribute For access to request attributes. See @RequestAttribute for more details. Return Values See equivalent in the Reactive stack @ExceptionHandler methods support the following return values: Return value Description @ResponseBody The return value is converted through HttpMessageConverter instances and written to the response. See @ResponseBody . HttpEntity<B> , ResponseEntity<B> The return value specifies that the full response (including the HTTP headers and the body) be converted through HttpMessageConverter instances and written to the response. See ResponseEntity . ErrorResponse , ProblemDetail To render an RFC 9457 error response with details in the body, see Error Responses String A view name to be resolved with ViewResolver implementations and used together with the implicit model — determined through command objects and @ModelAttribute methods. The handler method can also programmatically enrich the model by declaring a Model argument (described earlier). View A View instance to use for rendering together with the implicit model — determined through command objects and @ModelAttribute methods. The handler method may also programmatically enrich the model by declaring a Model argument (described earlier). java.util.Map , org.springframework.ui.Model Attributes to be added to the implicit model with the view name implicitly determined through a RequestToViewNameTranslator . @ModelAttribute An attribute to be added to the model with the view name implicitly determined through a RequestToViewNameTranslator . Note that @ModelAttribute is optional. See “Any other return value” at the end of this table. ModelAndView object The view and model attributes to use and, optionally, a response status. void A method with a void return type (or null return value) is considered to have fully handled the response if it also has a ServletResponse an OutputStream argument, or a @ResponseStatus annotation. The same is also true if the controller has made a positive ETag or lastModified timestamp check (see Controllers for details). If none of the above is true, a void return type can also indicate “no response body” for REST controllers or default view name selection for HTML controllers. Any other return value If a return value is not matched to any of the above and is not a simple type (as determined by BeanUtils#isSimpleProperty ), by default, it is treated as a model attribute to be added to the model. If it is a simple type, it remains unresolved. Validation Controller Advice Spring Framework Stable 7.0.8 6.2.19 Snapshot 7.1.0-SNAPSHOT 7.0.9-SNAPSHOT 6.2.20-SNAPSHOT Related Spring Documentation Spring Boot Spring Framework Spring Cloud Spring Cloud Build Spring Cloud Bus Spring Cloud Circuit Breaker Spring Cloud Commons Spring Cloud Config Spring Cloud Consul Spring Cloud Contract Spring Cloud Function Spring Cloud Gateway Spring Cloud Kubernetes Spring Cloud Netflix Spring Cloud OpenFeign Spring Cloud Stream Spring Cloud Task Spring Cloud Vault Spring Cloud Zookeeper Spring Data Spring Data Cassandra Spring Data Commons Spring Data Couchbase Spring Data Elasticsearch Spring Data JPA Spring Data KeyValue Spring Data LDAP Spring Data MongoDB Spring Data Neo4j Spring Data Redis Spring Data JDBC & R2DBC Spring Data REST Spring Integration Spring Batch Spring Security Spring Authorization Server Spring LDAP Spring Security Kerberos Spring Session Spring Vault Spring AI Spring AMQP Spring CLI Spring GraphQL Spring for Apache Kafka Spring Modulith Spring for Apache Pulsar Spring Shell All Docs... Copyright © 2005 - Broadcom. All Rights Reserved. The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries. Terms of Use • Privacy • Trademark Guidelines • Thank you • Your California Privacy Rights • Cookie Settings Apache®, Apache Tomcat®, Apache Kafka®, Apache Cassandra™, and Apache Geode™ are trademarks or registered trademarks of the Apache Software Foundation in the United States and/or other countries. Java™, Java™ SE, Java™ EE, and OpenJDK™ are trademarks of Oracle and/or its affiliates. Kubernetes® is a registered trademark of the Linux Foundation in the United States and other countries. Linux® is the registered trademark of Linus Torvalds in the United States and other countries. Windows® and Microsoft® Azure are registered trademarks of Microsoft Corporation. “AWS” and “Amazon Web Services” are trademarks or registered trademarks of Amazon.com Inc. or its affiliates. All other trademarks and copyrights are property of their respective owners and are only mentioned for informative purposes. Other names may be trademarks of their respective owners. Search in all Spring Docs
