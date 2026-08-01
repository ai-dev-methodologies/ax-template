# spring-mvc-validation — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T02:24:31Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r149`
**Body SHA-256 (below the `---` divider, header excluded):** 3f8a0ceb3ae0a41dcd399c9029972df318fb723ddf2ea3275671ee9829f73b50

---

---
snapshot_id: spring-mvc-validation
source: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 3
bytes: 5870
sha: "142360342ff5c38e5a2d284c13e346a1d3af6950ff27a856a18f130f7d04792c"
---

# spring mvc validation — upstream snapshot

Source: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html
Fetched: 2026-07-14

Validation :: Spring Framework
Edit this Page
 
 
 
 GitHub Project
 
 
 
 Stack Overflow

# Validation
See equivalent in the Reactive stack
Spring MVC has built-in validation for
@RequestMapping methods, including Java Bean Validation.
Validation may be applied at one of two levels:
Java Bean Validation is applied individually to an
@ModelAttribute,
@RequestBody, and
@RequestPart method parameter
annotated with @jakarta.validation.Valid or Spring’s @Validated so long as
it is a command object rather than a container such as Map or Collection, it does not
have Errors or BindingResult immediately after in the method signature, and does not
otherwise require method validation (see next). MethodArgumentNotValidException is the
exception raised when validating a method parameter individually.
Java Bean Validation is applied to the method when @Constraint annotations such as
@Min, @NotBlank and others are declared directly on method parameters, or on the
method for the return value, and it supersedes any validation that would be applied
otherwise to a method parameter individually because method validation covers both
method parameter constraints and nested constraints via @Valid.
HandlerMethodValidationException is the exception raised validation is applied
to the method.
Applications should handle both MethodArgumentNotValidException and
HandlerMethodValidationException since either may be raised depending on the controller
method signature. The two exceptions, however are designed to be very similar, and can be
handled with almost identical code. The main difference is that the former is for a single
object while the latter is for a list of method parameters.
@Valid is not a constraint annotation, but rather for nested constraints within
an Object. Therefore, by itself @Valid does not lead to method validation. @NotNull
on the other hand is a constraint, and adding it to an @Valid parameter leads to method
validation. For nullability specifically, you may also use the required flag of
@RequestBody or @ModelAttribute.
Method validation may be used in combination with Errors or BindingResult method
parameters. However, the controller method is called only if all validation errors are on
method parameters with an Errors immediately after. If there are validation errors on
any other method parameter then HandlerMethodValidationException is raised.
You can configure a Validator globally through the
WebMvc config, or locally through an
@InitBinder method in an
@Controller or @ControllerAdvice. You can also use multiple validators.
If a controller has a class level @Validated, then
method validation is applied
through an AOP proxy. In order to take advantage of the Spring MVC built-in support for
method validation added in Spring Framework 6.1, you need to remove the class level
@Validated annotation from the controller.
The Error Responses section provides further
details on how MethodArgumentNotValidException and HandlerMethodValidationException
are handled, and also how their rendering can be customized through a MessageSource and
locale and language specific resource bundles.
For further custom handling of method validation errors, you can extend
ResponseEntityExceptionHandler or use an @ExceptionHandler method in a controller
or in a @ControllerAdvice, and handle HandlerMethodValidationException directly.
The exception contains a list of ParameterValidationResults that group validation errors
by method parameter. You can either iterate over those, or provide a visitor with callback
methods by controller method parameter type:
Java
Kotlin
HandlerMethodValidationException ex = ... ;

ex.visitResults(new HandlerMethodValidationException.Visitor() {

 @Override
 public void requestHeader(RequestHeader requestHeader, ParameterValidationResult result) {
 // ...
 }

 @Override
 public void requestParam(@Nullable RequestParam requestParam, ParameterValidationResult result) {
 // ...
 }

 @Override
 public void modelAttribute(@Nullable ModelAttribute modelAttribute, ParameterErrors errors) {

 // ...

 @Override
 public void other(ParameterValidationResult result) {
 // ...
 }
});
// HandlerMethodValidationException
val ex

ex.visitResults(object : HandlerMethodValidationException.Visitor {

 override fun requestHeader(requestHeader: RequestHeader, result: ParameterValidationResult) {
 // ...
 }

 override fun requestParam(requestParam: RequestParam?, result: ParameterValidationResult) {
 // ...
 }

 override fun modelAttribute(modelAttribute: ModelAttribute?, errors: ParameterErrors) {
 // ...
 }

 // ...

 override fun other(result: ParameterValidationResult) {
 // ...
 }
})
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

Source: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html
HTTP status: 200 · extracted bytes: 17743 · sha256: 5755b2f9c002a1edbee19df95691867a25c7d2d7bee7197e85a8e05ed7f06dba
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r149`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

Validation :: Spring Framework Why Spring Overview Trending Generative AI Cloud Architecture Patterns Microservices Reactive Event Driven Application Types Web Applications Serverless Batch Learn Getting Started Quickstart Guides Academy Courses Get Certified Projects Overview Projects Spring Boot Spring Framework Spring Cloud Spring AI Spring Data Spring Integration Spring Batch Spring Security Foundational Projects Micrometer Reactor Development Tools Spring Tools Spring Initializr Resources Blog Release Calendar Version Mappings Release Highlights Security Advisories GitHub Orgs Spring Projects Spring Cloud Community Overview Events Authors Enterprise Overview Long-term Support Automated Upgrades Governance and Compliance Modern App Development light Spring Framework 7.0.8 Search Overview Core Technologies The IoC Container Introduction to the Spring IoC Container and Beans Container Overview Bean Overview Dependencies Dependency Injection Dependencies and Configuration in Detail Using depends-on Lazy-initialized Beans Autowiring Collaborators Method Injection Bean Scopes Customizing the Nature of a Bean Bean Definition Inheritance Container Extension Points Annotation-based Container Configuration Using @Autowired Fine-tuning Annotation-based Autowiring with @Primary or @Fallback Fine-tuning Annotation-based Autowiring with Qualifiers Using Generics as Autowiring Qualifiers Using CustomAutowireConfigurer Injection with @Resource Using @Value Using @PostConstruct and @PreDestroy Classpath Scanning and Managed Components Using JSR-330 Standard Annotations Java-based Container Configuration Basic Concepts: @Bean and @Configuration Instantiating the Spring Container by Using AnnotationConfigApplicationContext Using the @Bean Annotation Using the @Configuration annotation Composing Java-based Configurations Programmatic Bean Registration Environment Abstraction Registering a LoadTimeWeaver Additional Capabilities of the ApplicationContext The BeanFactory API Resources Validation, Data Binding, and Type Conversion Validation Using Spring’s Validator Interface Data Binding Resolving Error Codes to Error Messages Spring Type Conversion Spring Field Formatting Configuring a Global Date and Time Format Java Bean Validation Spring Expression Language (SpEL) Evaluation Expressions in Bean Definitions Language Reference Literal Expressions Properties, Arrays, Lists, Maps, and Indexers Inline Lists Inline Maps Array Construction Methods Operators Types Constructors Variables Functions Varargs Invocations Bean References Ternary Operator (If-Then-Else) The Elvis Operator Safe Navigation Operator Collection Selection Collection Projection Expression Templating Classes Used in the Examples Aspect Oriented Programming with Spring AOP Concepts Spring AOP Capabilities and Goals AOP Proxies @AspectJ support Enabling @AspectJ Support Declaring an Aspect Declaring a Pointcut Declaring Advice Introductions Aspect Instantiation Models An AOP Example Schema-based AOP Support Choosing which AOP Declaration Style to Use Mixing Aspect Types Proxying Mechanisms Programmatic Creation of @AspectJ Proxies Using AspectJ with Spring Applications Further Resources Spring AOP APIs Pointcut API in Spring Advice API in Spring The Advisor API in Spring Using the ProxyFactoryBean to Create AOP Proxies Concise Proxy Definitions Creating AOP Proxies Programmatically with the ProxyFactory Manipulating Advised Objects Using the "auto-proxy" facility Using TargetSource Implementations Defining New Advice Types Resilience Features Null-safety Data Buffers and Codecs Ahead of Time Optimizations Appendix XML Schemas XML Schema Authoring Application Startup Steps Data Access Transaction Management Advantages of the Spring Framework’s Transaction Support Model Understanding the Spring Framework Transaction Abstraction Synchronizing Resources with Transactions Declarative Transaction Management Understanding the Spring Framework’s Declarative Transaction Implementation Example of Declarative Transaction Implementation Rolling Back a Declarative Transaction Configuring Different Transactional Semantics for Different Beans <tx:advice/> Settings Using @Transactional Transaction Propagation Advising Transactional Operations Using @Transactional with AspectJ Programmatic Transaction Management Choosing Between Programmatic and Declarative Transaction Management Transaction-bound Events Application server-specific integration Solutions to Common Problems Further Resources DAO Support Data Access with JDBC Choosing an Approach for JDBC Database Access Package Hierarchy Using the JDBC Core Classes to Control Basic JDBC Processing and Error Handling Controlling Database Connections JDBC Batch Operations Simplifying JDBC Operations with the SimpleJdbc Classes Modeling JDBC Operations as Java Objects Common Problems with Parameter and Data Value Handling Embedded Database Support Initializing a DataSource Data Access with R2DBC Object Relational Mapping (ORM) Data Access Introduction to ORM with Spring General ORM Integration Considerations Hibernate JPA Marshalling XML by Using Object-XML Mappers Appendix Web on Servlet Stack Spring Web MVC DispatcherServlet Context Hierarchy Special Bean Types Web MVC Config Servlet Config Processing Path Matching Interception Exceptions View Resolution Locale Multipart Resolver Logging Filters HTTP Message Conversion Annotated Controllers Declaration Mapping Requests Handler Methods Method Arguments Return Values Type Conversion Matrix Variables @RequestParam @RequestHeader @CookieValue @ModelAttribute @SessionAttributes @SessionAttribute @RequestAttribute Redirect Attributes Flash Attributes Multipart @RequestBody HttpEntity @ResponseBody ResponseEntity Jackson JSON Model @InitBinder Validation Exceptions Controller Advice Functional Endpoints URI Links Asynchronous Requests Range Requests Data Binding CORS API Versioning Error Responses Web Security HTTP Caching View Technologies Thymeleaf FreeMarker Groovy Markup Script Views HTML Fragments JSP and JSTL RSS and Atom PDF and Excel Jackson XML Marshalling XSLT Views MVC Config Enable MVC Configuration MVC Config API Type Conversion Validation Interceptors Content Types Message Converters View Controllers View Resolvers Static Resources Default Servlet Path Matching API Version Advanced Java Config Advanced XML Config HTTP/2 REST Clients Testing WebSockets WebSocket API SockJS Fallback STOMP Overview Benefits Enable STOMP WebSocket Transport Flow of Messages Annotated Controllers Sending Messages Simple Broker External Broker Connecting to a Broker Dots as Separators Authentication Token Authentication Authorization User Destinations Order of Messages Events Interception STOMP Client WebSocket Scope Performance Monitoring Testing Web on Reactive Stack Spring WebFlux Overview Reactive Core DispatcherHandler Annotated Controllers @Controller Mapping Requests Handler Methods Method Arguments Return Values Type Conversion Matrix Variables @RequestParam @RequestHeader @CookieValue @ModelAttribute @SessionAttributes @SessionAttribute @RequestAttribute Multipart Content @RequestBody HttpEntity @ResponseBody ResponseEntity Jackson JSON Model DataBinder Validation Exceptions Controller Advice Functional Endpoints URI Links Range Requests Data Binding CORS API Versioning Error Responses Web Security HTTP Caching View Technologies WebFlux Config HTTP/2 WebClient Configuration retrieve() Exchange Request Body Filters Attributes Context Synchronous Use Testing HTTP Service Client WebSockets Testing RSocket Reactive Libraries Testing Introduction to Spring Testing Unit Testing Integration Testing JDBC Testing Support Spring TestContext Framework Key Abstractions Bootstrapping the TestContext Framework TestExecutionListener Configuration Application Events Test Execution Events Context Management Context Configuration with Component Classes Context Configuration with XML Resources Context Configuration with Groovy Scripts Default Context Configuration Mixing Component Classes, XML, and Groovy Scripts Context Configuration with Context Customizers Context Configuration with Context Initializers Context Configuration Inheritance Context Configuration with Environment Profiles Context Configuration with Test Property Sources Context Configuration with Dynamic Property Sources Loading a WebApplicationContext Working with Web Mocks Context Caching Context Pausing Context Failure Threshold Context Hierarchies Dependency Injection of Test Fixtures Bean Overriding in Tests Testing Request- and Session-scoped Beans Transaction Management Executing SQL Scripts Parallel Test Execution TestContext Framework Support Classes Ahead of Time Support for Tests WebTestClient RestTestClient MockMvc Overview Setup Options Hamcrest Integration Static Imports Configuring MockMvc Setup Features Performing Requests Defining Expectations Async Requests Streaming Responses Filter Registrations AssertJ Integration Configuring MockMvcTester Performing Requests Defining Expectations MockMvc integration HtmlUnit Integration Why HtmlUnit Integration? MockMvc and HtmlUnit MockMvc and WebDriver MockMvc and Geb MockMvc vs End-to-End Tests Further Examples Testing Client Applications Appendix Annotations Standard Annotation Support Spring Testing Annotations @BootstrapWith @ContextConfiguration @WebAppConfiguration @ContextHierarchy @ContextCustomizerFactories @ActiveProfiles @TestPropertySource @DynamicPropertySource @TestBean @MockitoBean and @MockitoSpyBean @DirtiesContext @TestExecutionListeners @RecordApplicationEvents @Commit @Rollback @BeforeTransaction @AfterTransaction @Sql @SqlConfig @SqlMergeMode @SqlGroup @DisabledInAotMode Spring JUnit 4 Testing Annotations Spring JUnit Jupiter Testing Annotations Meta-Annotation Support for Testing Further Resources Integration REST Clients JMS (Java Message Service) Using Spring JMS Sending a Message Receiving a Message Support for JCA Message Endpoints Annotation-driven Listener Endpoints JMS Namespace Support JMX Exporting Your Beans to JMX Controlling the Management Interface of Your Beans Controlling ObjectName Instances for Your Beans Using JSR-160 Connectors Accessing MBeans through Proxies Notifications Further Resources Email Task Execution and Scheduling Cache Abstraction Understanding the Cache Abstraction Declarative Annotation-based Caching JCache (JSR-107) Annotations Declarative XML-based Caching Configuring the Cache Storage Plugging-in Different Back-end Caches How can I Set the TTL/TTI/Eviction policy/XXX feature? Observability Support JVM AOT Cache JVM Checkpoint Restore Appendix Language Support Kotlin Requirements Extensions Null-safety Classes and Interfaces Annotations Bean Registration DSL Web Coroutines Spring Projects in Kotlin Getting Started Resources Apache Groovy Appendix Java API Kotlin API Wiki Search Edit this Page GitHub Project Stack Overflow Spring Framework Web on Servlet Stack Spring Web MVC Annotated Controllers Validation Validation See equivalent in the Reactive stack Spring MVC has built-in validation for @RequestMapping methods, including Java Bean Validation . Validation may be applied at one of two levels: Java Bean Validation is applied individually to an @ModelAttribute , @RequestBody , and @RequestPart method parameter annotated with @jakarta.validation.Valid or Spring’s @Validated so long as it is a command object rather than a container such as Map or Collection , it does not have Errors or BindingResult immediately after in the method signature, and does not otherwise require method validation (see next). MethodArgumentNotValidException is the exception raised when validating a method parameter individually. Java Bean Validation is applied to the method when @Constraint annotations such as @Min , @NotBlank and others are declared directly on method parameters, or on the method for the return value, and it supersedes any validation that would be applied otherwise to a method parameter individually because method validation covers both method parameter constraints and nested constraints via @Valid . HandlerMethodValidationException is the exception raised validation is applied to the method. Applications should handle both MethodArgumentNotValidException and HandlerMethodValidationException since either may be raised depending on the controller method signature. The two exceptions, however are designed to be very similar, and can be handled with almost identical code. The main difference is that the former is for a single object while the latter is for a list of method parameters. @Valid is not a constraint annotation, but rather for nested constraints within an Object. Therefore, by itself @Valid does not lead to method validation. @NotNull on the other hand is a constraint, and adding it to an @Valid parameter leads to method validation. For nullability specifically, you may also use the required flag of @RequestBody or @ModelAttribute . Method validation may be used in combination with Errors or BindingResult method parameters. However, the controller method is called only if all validation errors are on method parameters with an Errors immediately after. If there are validation errors on any other method parameter then HandlerMethodValidationException is raised. You can configure a Validator globally through the WebMvc config , or locally through an @InitBinder method in an @Controller or @ControllerAdvice . You can also use multiple validators. If a controller has a class level @Validated , then method validation is applied through an AOP proxy. In order to take advantage of the Spring MVC built-in support for method validation added in Spring Framework 6.1, you need to remove the class level @Validated annotation from the controller. The Error Responses section provides further details on how MethodArgumentNotValidException and HandlerMethodValidationException are handled, and also how their rendering can be customized through a MessageSource and locale and language specific resource bundles. For further custom handling of method validation errors, you can extend ResponseEntityExceptionHandler or use an @ExceptionHandler method in a controller or in a @ControllerAdvice , and handle HandlerMethodValidationException directly. The exception contains a list of ParameterValidationResult s that group validation errors by method parameter. You can either iterate over those, or provide a visitor with callback methods by controller method parameter type: Java Kotlin HandlerMethodValidationException ex = ... ; ex.visitResults(new HandlerMethodValidationException.Visitor() { @Override public void requestHeader(RequestHeader requestHeader, ParameterValidationResult result) { // ... } @Override public void requestParam(@Nullable RequestParam requestParam, ParameterValidationResult result) { // ... } @Override public void modelAttribute(@Nullable ModelAttribute modelAttribute, ParameterErrors errors) { // ... @Override public void other(ParameterValidationResult result) { // ... } }); // HandlerMethodValidationException val ex ex.visitResults(object : HandlerMethodValidationException.Visitor { override fun requestHeader(requestHeader: RequestHeader, result: ParameterValidationResult) { // ... } override fun requestParam(requestParam: RequestParam?, result: ParameterValidationResult) { // ... } override fun modelAttribute(modelAttribute: ModelAttribute?, errors: ParameterErrors) { // ... } // ... override fun other(result: ParameterValidationResult) { // ... } }) @InitBinder Exceptions Spring Framework Stable 7.0.8 6.2.19 Snapshot 7.1.0-SNAPSHOT 7.0.9-SNAPSHOT 6.2.20-SNAPSHOT Related Spring Documentation Spring Boot Spring Framework Spring Cloud Spring Cloud Build Spring Cloud Bus Spring Cloud Circuit Breaker Spring Cloud Commons Spring Cloud Config Spring Cloud Consul Spring Cloud Contract Spring Cloud Function Spring Cloud Gateway Spring Cloud Kubernetes Spring Cloud Netflix Spring Cloud OpenFeign Spring Cloud Stream Spring Cloud Task Spring Cloud Vault Spring Cloud Zookeeper Spring Data Spring Data Cassandra Spring Data Commons Spring Data Couchbase Spring Data Elasticsearch Spring Data JPA Spring Data KeyValue Spring Data LDAP Spring Data MongoDB Spring Data Neo4j Spring Data Redis Spring Data JDBC & R2DBC Spring Data REST Spring Integration Spring Batch Spring Security Spring Authorization Server Spring LDAP Spring Security Kerberos Spring Session Spring Vault Spring AI Spring AMQP Spring CLI Spring GraphQL Spring for Apache Kafka Spring Modulith Spring for Apache Pulsar Spring Shell All Docs... Copyright © 2005 - Broadcom. All Rights Reserved. The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries. Terms of Use • Privacy • Trademark Guidelines • Thank you • Your California Privacy Rights • Cookie Settings Apache®, Apache Tomcat®, Apache Kafka®, Apache Cassandra™, and Apache Geode™ are trademarks or registered trademarks of the Apache Software Foundation in the United States and/or other countries. Java™, Java™ SE, Java™ EE, and OpenJDK™ are trademarks of Oracle and/or its affiliates. Kubernetes® is a registered trademark of the Linux Foundation in the United States and other countries. Linux® is the registered trademark of Linus Torvalds in the United States and other countries. Windows® and Microsoft® Azure are registered trademarks of Microsoft Corporation. “AWS” and “Amazon Web Services” are trademarks or registered trademarks of Amazon.com Inc. or its affiliates. All other trademarks and copyrights are property of their respective owners and are only mentioned for informative purposes. Other names may be trademarks of their respective owners. Search in all Spring Docs
