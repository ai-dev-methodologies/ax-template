# spring-aop-proxying — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://docs.spring.io/spring-framework/reference/core/aop/proxying.html (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T02:24:26Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://docs.spring.io/spring-framework/reference/core/aop/proxying.html`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r129`
**Body SHA-256 (below the `---` divider, header excluded):** 870141d2d98cec3d6aac46f2e9029ac6656222fecbf544afe0868b555cc85d45

---

---
snapshot_id: spring-aop-proxying
source: "https://docs.spring.io/spring-framework/reference/core/aop/proxying.html"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 3
bytes: 9786
sha: "eb561e06bdd7707749b1588662679165c044fdc690d3ff3676a03a388a0bb910"
---

# spring aop proxying — upstream snapshot

Source: https://docs.spring.io/spring-framework/reference/core/aop/proxying.html
Fetched: 2026-07-14

Proxying Mechanisms :: Spring Framework
Edit this Page
 
 
 
 GitHub Project
 
 
 
 Stack Overflow

# Proxying Mechanisms
Spring AOP uses either JDK dynamic proxies or CGLIB to create the proxy for a given
target object. JDK dynamic proxies are built into the JDK, whereas CGLIB is a common
open-source class definition library (repackaged into spring-core).
If the target object to be proxied implements at least one interface, a JDK dynamic
proxy is used, and all of the interfaces implemented by the target type are proxied.
If the target object does not implement any interfaces, a CGLIB proxy is created which
is a runtime-generated subclass of the target type.
If you want to force the use of CGLIB proxying (for example, to proxy every method
defined for the target object, not only those implemented by its interfaces),
you can do so. However, you should consider the following issues:
final classes cannot be proxied, because they cannot be extended.
final methods cannot be advised, because they cannot be overridden.
private methods cannot be advised, because they cannot be overridden.
Methods that are not visible – for example, package-private methods in a parent class
from a different package – cannot be advised because they are effectively private.
The constructor of your proxied object will not be called twice, since the CGLIB proxy
instance is created through Objenesis. However, if your JVM does not allow for
constructor bypassing, you might see double invocations and corresponding debug log
entries from Spring’s AOP support.
Your CGLIB proxy usage may face limitations with the Java Module System. As a typical
case, you cannot create a CGLIB proxy for a class from the java.lang package when
deploying on the module path. Such cases require a JVM bootstrap flag
--add-opens=java.base/java.lang=ALL-UNNAMED which is not available for modules.

## Forcing Specific AOP Proxy Types
To force the use of CGLIB proxies, set the value of the proxy-target-class attribute
of the element to true, as follows:
To force CGLIB proxying when you use the @AspectJ auto-proxy support, set the
proxy-target-class attribute of the element to true,
as follows:
Multiple sections are collapsed into a single unified auto-proxy creator
at runtime, which applies the strongest proxy settings that any of the
 sections (typically from different XML bean definition files) specified.
This also applies to the and 
elements.
To be clear, using proxy-target-class="true" on ,
, or elements forces the use of CGLIB
proxies for all three of them.
@EnableAspectJAutoProxy, @EnableTransactionManagement and related configuration
annotations offer a corresponding proxyTargetClass attribute. These are collapsed
into a single unified auto-proxy creator too, effectively applying the strongest
proxy settings at runtime. As of 7.0, this applies to individual proxy processors
as well, for example @EnableAsync, consistently participating in unified global
default settings for all auto-proxying attempts in a given application.
The global default proxy type may differ between setups. While the core framework
suggests interface-based proxies by default, Spring Boot may - depending on
configuration properties - enable class-based proxies by default.
As of 7.0, forcing a specific proxy type for individual beans is possible through
the @Proxyable annotation on a given @Bean method or @Component class, with
@Proxyable(INTERFACES) or @Proxyable(TARGET_CLASS) overriding any globally
configured default. For very specific purposes, you may even specify the proxy
interface(s) to use through @Proxyable(interfaces=…​), limiting the exposure
to selected interfaces rather than all interfaces that the target bean implements.

## Understanding AOP Proxies
Spring AOP is proxy-based. It is vitally important that you grasp the semantics of
what that last statement actually means before you write your own aspects or use any of
the Spring AOP-based aspects supplied with the Spring Framework.
Consider first the scenario where you have a plain-vanilla, un-proxied object reference,
as the following code snippet shows:
Java
Kotlin
public class SimplePojo implements Pojo {

 public void foo() {
 // this next method invocation is a direct call on the 'this' reference
 this.bar();
 }

 public void bar() {
 // some logic...
 }
}
class SimplePojo : Pojo {

 fun foo() {
 // this next method invocation is a direct call on the 'this' reference
 this.bar()
 }

 fun bar() {
 // some logic...
 }
}
If you invoke a method on an object reference, the method is invoked directly on
that object reference, as the following image and listing show:
Java
Kotlin
public class Main {

 public static void main(String[] args) {
 Pojo pojo = new SimplePojo();
 // this is a direct method call on the 'pojo' reference
 pojo.foo();
 }
}
fun main() {
 val pojo = SimplePojo()
 // this is a direct method call on the 'pojo' reference
 pojo.foo()
}
Things change slightly when the reference that client code has is a proxy. Consider the
following diagram and code snippet:
Java
Kotlin
public class Main {

 public static void main(String[] args) {
 ProxyFactory factory = new ProxyFactory(new SimplePojo());
 factory.addInterface(Pojo.class);
 factory.addAdvice(new RetryAdvice());

 Pojo pojo = (Pojo) factory.getProxy();
 // this is a method call on the proxy!
 pojo.foo();
 }
}
fun main() {
 val factory = ProxyFactory(SimplePojo())
 factory.addInterface(Pojo::class.java)
 factory.addAdvice(RetryAdvice())

 val pojo = factory.proxy as Pojo
 // this is a method call on the proxy!
 pojo.foo()
}
The key thing to understand here is that the client code inside the main(..) method
of the Main class has a reference to the proxy. This means that method calls on that
object reference are calls on the proxy. As a result, the proxy can delegate to all of
the interceptors (advice) that are relevant to that particular method call. However,
once the call has finally reached the target object (the SimplePojo reference in
this case), any method calls that it may make on itself, such as this.bar() or
this.foo(), are going to be invoked against the this reference, and not the proxy.
This has important implications. It means that self invocation is not going to result
in the advice associated with a method invocation getting a chance to run. In other words,
self invocation via an explicit or implicit this reference will bypass the advice.
To address that, you have the following options.
Avoid self invocation
The best approach (the term "best" is used loosely here) is to refactor your code such
that the self invocation does not happen. This does entail some work on your part, but
it is the best, least-invasive approach.
Inject a self reference
An alternative approach is to make use of
self injection,
and invoke methods on the proxy via the self reference instead of via this.
Use AopContext.currentProxy()
This last approach is highly discouraged, and we hesitate to point it out, in favor of
the previous options. However, as a last resort you can choose to tie the logic within
your class to Spring AOP, as the following example shows.
Java
Kotlin
public class SimplePojo implements Pojo {

 public void foo() {
 // This works, but it should be avoided if possible.
 ((Pojo) AopContext.currentProxy()).bar();
 }

 public void bar() {
 // some logic...
 }
}
class SimplePojo : Pojo {

 fun foo() {
 // This works, but it should be avoided if possible.
 (AopContext.currentProxy() as Pojo).bar()
 }

 fun bar() {
 // some logic...
 }
}
The use of AopContext.currentProxy() totally couples your code to Spring AOP, and it
makes the class itself aware of the fact that it is being used in an AOP context, which
reduces some of the benefits of AOP. It also requires that the ProxyFactory is
configured to expose the proxy, as the following example shows:
Java
Kotlin
public class Main {

 public static void main(String[] args) {
 ProxyFactory factory = new ProxyFactory(new SimplePojo());
 factory.addInterface(Pojo.class);
 factory.addAdvice(new RetryAdvice());
 factory.setExposeProxy(true);

 Pojo pojo = (Pojo) factory.getProxy();
 // this is a method call on the proxy!
 pojo.foo();
 }
}
fun main() {
 val factory = ProxyFactory(SimplePojo())
 factory.addInterface(Pojo::class.java)
 factory.addAdvice(RetryAdvice())
 factory.isExposeProxy = true

 val pojo = factory.proxy as Pojo
 // this is a method call on the proxy!
 pojo.foo()
}
AspectJ compile-time weaving and load-time weaving do not have this self-invocation
issue because they apply advice within the bytecode instead of via a proxy.
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

Source: https://docs.spring.io/spring-framework/reference/core/aop/proxying.html
HTTP status: 200 · extracted bytes: 21996 · sha256: cd580057ed4d7f27260d85cb56dfe85fe56e7a09a2cdc923356434c7c333cff0
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r129`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

Proxying Mechanisms :: Spring Framework Why Spring Overview Trending Generative AI Cloud Architecture Patterns Microservices Reactive Event Driven Application Types Web Applications Serverless Batch Learn Getting Started Quickstart Guides Academy Courses Get Certified Projects Overview Projects Spring Boot Spring Framework Spring Cloud Spring AI Spring Data Spring Integration Spring Batch Spring Security Foundational Projects Micrometer Reactor Development Tools Spring Tools Spring Initializr Resources Blog Release Calendar Version Mappings Release Highlights Security Advisories GitHub Orgs Spring Projects Spring Cloud Community Overview Events Authors Enterprise Overview Long-term Support Automated Upgrades Governance and Compliance Modern App Development light Spring Framework 7.0.8 Search Overview Core Technologies The IoC Container Introduction to the Spring IoC Container and Beans Container Overview Bean Overview Dependencies Dependency Injection Dependencies and Configuration in Detail Using depends-on Lazy-initialized Beans Autowiring Collaborators Method Injection Bean Scopes Customizing the Nature of a Bean Bean Definition Inheritance Container Extension Points Annotation-based Container Configuration Using @Autowired Fine-tuning Annotation-based Autowiring with @Primary or @Fallback Fine-tuning Annotation-based Autowiring with Qualifiers Using Generics as Autowiring Qualifiers Using CustomAutowireConfigurer Injection with @Resource Using @Value Using @PostConstruct and @PreDestroy Classpath Scanning and Managed Components Using JSR-330 Standard Annotations Java-based Container Configuration Basic Concepts: @Bean and @Configuration Instantiating the Spring Container by Using AnnotationConfigApplicationContext Using the @Bean Annotation Using the @Configuration annotation Composing Java-based Configurations Programmatic Bean Registration Environment Abstraction Registering a LoadTimeWeaver Additional Capabilities of the ApplicationContext The BeanFactory API Resources Validation, Data Binding, and Type Conversion Validation Using Spring’s Validator Interface Data Binding Resolving Error Codes to Error Messages Spring Type Conversion Spring Field Formatting Configuring a Global Date and Time Format Java Bean Validation Spring Expression Language (SpEL) Evaluation Expressions in Bean Definitions Language Reference Literal Expressions Properties, Arrays, Lists, Maps, and Indexers Inline Lists Inline Maps Array Construction Methods Operators Types Constructors Variables Functions Varargs Invocations Bean References Ternary Operator (If-Then-Else) The Elvis Operator Safe Navigation Operator Collection Selection Collection Projection Expression Templating Classes Used in the Examples Aspect Oriented Programming with Spring AOP Concepts Spring AOP Capabilities and Goals AOP Proxies @AspectJ support Enabling @AspectJ Support Declaring an Aspect Declaring a Pointcut Declaring Advice Introductions Aspect Instantiation Models An AOP Example Schema-based AOP Support Choosing which AOP Declaration Style to Use Mixing Aspect Types Proxying Mechanisms Programmatic Creation of @AspectJ Proxies Using AspectJ with Spring Applications Further Resources Spring AOP APIs Pointcut API in Spring Advice API in Spring The Advisor API in Spring Using the ProxyFactoryBean to Create AOP Proxies Concise Proxy Definitions Creating AOP Proxies Programmatically with the ProxyFactory Manipulating Advised Objects Using the "auto-proxy" facility Using TargetSource Implementations Defining New Advice Types Resilience Features Null-safety Data Buffers and Codecs Ahead of Time Optimizations Appendix XML Schemas XML Schema Authoring Application Startup Steps Data Access Transaction Management Advantages of the Spring Framework’s Transaction Support Model Understanding the Spring Framework Transaction Abstraction Synchronizing Resources with Transactions Declarative Transaction Management Understanding the Spring Framework’s Declarative Transaction Implementation Example of Declarative Transaction Implementation Rolling Back a Declarative Transaction Configuring Different Transactional Semantics for Different Beans <tx:advice/> Settings Using @Transactional Transaction Propagation Advising Transactional Operations Using @Transactional with AspectJ Programmatic Transaction Management Choosing Between Programmatic and Declarative Transaction Management Transaction-bound Events Application server-specific integration Solutions to Common Problems Further Resources DAO Support Data Access with JDBC Choosing an Approach for JDBC Database Access Package Hierarchy Using the JDBC Core Classes to Control Basic JDBC Processing and Error Handling Controlling Database Connections JDBC Batch Operations Simplifying JDBC Operations with the SimpleJdbc Classes Modeling JDBC Operations as Java Objects Common Problems with Parameter and Data Value Handling Embedded Database Support Initializing a DataSource Data Access with R2DBC Object Relational Mapping (ORM) Data Access Introduction to ORM with Spring General ORM Integration Considerations Hibernate JPA Marshalling XML by Using Object-XML Mappers Appendix Web on Servlet Stack Spring Web MVC DispatcherServlet Context Hierarchy Special Bean Types Web MVC Config Servlet Config Processing Path Matching Interception Exceptions View Resolution Locale Multipart Resolver Logging Filters HTTP Message Conversion Annotated Controllers Declaration Mapping Requests Handler Methods Method Arguments Return Values Type Conversion Matrix Variables @RequestParam @RequestHeader @CookieValue @ModelAttribute @SessionAttributes @SessionAttribute @RequestAttribute Redirect Attributes Flash Attributes Multipart @RequestBody HttpEntity @ResponseBody ResponseEntity Jackson JSON Model @InitBinder Validation Exceptions Controller Advice Functional Endpoints URI Links Asynchronous Requests Range Requests Data Binding CORS API Versioning Error Responses Web Security HTTP Caching View Technologies Thymeleaf FreeMarker Groovy Markup Script Views HTML Fragments JSP and JSTL RSS and Atom PDF and Excel Jackson XML Marshalling XSLT Views MVC Config Enable MVC Configuration MVC Config API Type Conversion Validation Interceptors Content Types Message Converters View Controllers View Resolvers Static Resources Default Servlet Path Matching API Version Advanced Java Config Advanced XML Config HTTP/2 REST Clients Testing WebSockets WebSocket API SockJS Fallback STOMP Overview Benefits Enable STOMP WebSocket Transport Flow of Messages Annotated Controllers Sending Messages Simple Broker External Broker Connecting to a Broker Dots as Separators Authentication Token Authentication Authorization User Destinations Order of Messages Events Interception STOMP Client WebSocket Scope Performance Monitoring Testing Web on Reactive Stack Spring WebFlux Overview Reactive Core DispatcherHandler Annotated Controllers @Controller Mapping Requests Handler Methods Method Arguments Return Values Type Conversion Matrix Variables @RequestParam @RequestHeader @CookieValue @ModelAttribute @SessionAttributes @SessionAttribute @RequestAttribute Multipart Content @RequestBody HttpEntity @ResponseBody ResponseEntity Jackson JSON Model DataBinder Validation Exceptions Controller Advice Functional Endpoints URI Links Range Requests Data Binding CORS API Versioning Error Responses Web Security HTTP Caching View Technologies WebFlux Config HTTP/2 WebClient Configuration retrieve() Exchange Request Body Filters Attributes Context Synchronous Use Testing HTTP Service Client WebSockets Testing RSocket Reactive Libraries Testing Introduction to Spring Testing Unit Testing Integration Testing JDBC Testing Support Spring TestContext Framework Key Abstractions Bootstrapping the TestContext Framework TestExecutionListener Configuration Application Events Test Execution Events Context Management Context Configuration with Component Classes Context Configuration with XML Resources Context Configuration with Groovy Scripts Default Context Configuration Mixing Component Classes, XML, and Groovy Scripts Context Configuration with Context Customizers Context Configuration with Context Initializers Context Configuration Inheritance Context Configuration with Environment Profiles Context Configuration with Test Property Sources Context Configuration with Dynamic Property Sources Loading a WebApplicationContext Working with Web Mocks Context Caching Context Pausing Context Failure Threshold Context Hierarchies Dependency Injection of Test Fixtures Bean Overriding in Tests Testing Request- and Session-scoped Beans Transaction Management Executing SQL Scripts Parallel Test Execution TestContext Framework Support Classes Ahead of Time Support for Tests WebTestClient RestTestClient MockMvc Overview Setup Options Hamcrest Integration Static Imports Configuring MockMvc Setup Features Performing Requests Defining Expectations Async Requests Streaming Responses Filter Registrations AssertJ Integration Configuring MockMvcTester Performing Requests Defining Expectations MockMvc integration HtmlUnit Integration Why HtmlUnit Integration? MockMvc and HtmlUnit MockMvc and WebDriver MockMvc and Geb MockMvc vs End-to-End Tests Further Examples Testing Client Applications Appendix Annotations Standard Annotation Support Spring Testing Annotations @BootstrapWith @ContextConfiguration @WebAppConfiguration @ContextHierarchy @ContextCustomizerFactories @ActiveProfiles @TestPropertySource @DynamicPropertySource @TestBean @MockitoBean and @MockitoSpyBean @DirtiesContext @TestExecutionListeners @RecordApplicationEvents @Commit @Rollback @BeforeTransaction @AfterTransaction @Sql @SqlConfig @SqlMergeMode @SqlGroup @DisabledInAotMode Spring JUnit 4 Testing Annotations Spring JUnit Jupiter Testing Annotations Meta-Annotation Support for Testing Further Resources Integration REST Clients JMS (Java Message Service) Using Spring JMS Sending a Message Receiving a Message Support for JCA Message Endpoints Annotation-driven Listener Endpoints JMS Namespace Support JMX Exporting Your Beans to JMX Controlling the Management Interface of Your Beans Controlling ObjectName Instances for Your Beans Using JSR-160 Connectors Accessing MBeans through Proxies Notifications Further Resources Email Task Execution and Scheduling Cache Abstraction Understanding the Cache Abstraction Declarative Annotation-based Caching JCache (JSR-107) Annotations Declarative XML-based Caching Configuring the Cache Storage Plugging-in Different Back-end Caches How can I Set the TTL/TTI/Eviction policy/XXX feature? Observability Support JVM AOT Cache JVM Checkpoint Restore Appendix Language Support Kotlin Requirements Extensions Null-safety Classes and Interfaces Annotations Bean Registration DSL Web Coroutines Spring Projects in Kotlin Getting Started Resources Apache Groovy Appendix Java API Kotlin API Wiki Search Edit this Page GitHub Project Stack Overflow Spring Framework Core Technologies Aspect Oriented Programming with Spring Proxying Mechanisms Proxying Mechanisms Spring AOP uses either JDK dynamic proxies or CGLIB to create the proxy for a given target object. JDK dynamic proxies are built into the JDK, whereas CGLIB is a common open-source class definition library (repackaged into spring-core ). If the target object to be proxied implements at least one interface, a JDK dynamic proxy is used, and all of the interfaces implemented by the target type are proxied. If the target object does not implement any interfaces, a CGLIB proxy is created which is a runtime-generated subclass of the target type. If you want to force the use of CGLIB proxying (for example, to proxy every method defined for the target object, not only those implemented by its interfaces), you can do so. However, you should consider the following issues: final classes cannot be proxied, because they cannot be extended. final methods cannot be advised, because they cannot be overridden. private methods cannot be advised, because they cannot be overridden. Methods that are not visible – for example, package-private methods in a parent class from a different package – cannot be advised because they are effectively private. The constructor of your proxied object will not be called twice, since the CGLIB proxy instance is created through Objenesis. However, if your JVM does not allow for constructor bypassing, you might see double invocations and corresponding debug log entries from Spring’s AOP support. Your CGLIB proxy usage may face limitations with the Java Module System. As a typical case, you cannot create a CGLIB proxy for a class from the java.lang package when deploying on the module path. Such cases require a JVM bootstrap flag --add-opens=java.base/java.lang=ALL-UNNAMED which is not available for modules. Forcing Specific AOP Proxy Types To force the use of CGLIB proxies, set the value of the proxy-target-class attribute of the <aop:config> element to true, as follows: <aop:config proxy-target-class="true"> <!-- other beans defined here... --> </aop:config> To force CGLIB proxying when you use the @AspectJ auto-proxy support, set the proxy-target-class attribute of the <aop:aspectj-autoproxy> element to true , as follows: <aop:aspectj-autoproxy proxy-target-class="true"/> Multiple <aop:config/> sections are collapsed into a single unified auto-proxy creator at runtime, which applies the strongest proxy settings that any of the <aop:config/> sections (typically from different XML bean definition files) specified. This also applies to the <tx:annotation-driven/> and <aop:aspectj-autoproxy/> elements. To be clear, using proxy-target-class="true" on <tx:annotation-driven/> , <aop:aspectj-autoproxy/> , or <aop:config/> elements forces the use of CGLIB proxies for all three of them . @EnableAspectJAutoProxy , @EnableTransactionManagement and related configuration annotations offer a corresponding proxyTargetClass attribute. These are collapsed into a single unified auto-proxy creator too, effectively applying the strongest proxy settings at runtime. As of 7.0, this applies to individual proxy processors as well, for example @EnableAsync , consistently participating in unified global default settings for all auto-proxying attempts in a given application. The global default proxy type may differ between setups. While the core framework suggests interface-based proxies by default, Spring Boot may - depending on configuration properties - enable class-based proxies by default. As of 7.0, forcing a specific proxy type for individual beans is possible through the @Proxyable annotation on a given @Bean method or @Component class, with @Proxyable(INTERFACES) or @Proxyable(TARGET_CLASS) overriding any globally configured default. For very specific purposes, you may even specify the proxy interface(s) to use through @Proxyable(interfaces=…​) , limiting the exposure to selected interfaces rather than all interfaces that the target bean implements. Understanding AOP Proxies Spring AOP is proxy-based. It is vitally important that you grasp the semantics of what that last statement actually means before you write your own aspects or use any of the Spring AOP-based aspects supplied with the Spring Framework. Consider first the scenario where you have a plain-vanilla, un-proxied object reference, as the following code snippet shows: Java Kotlin public class SimplePojo implements Pojo { public void foo() { // this next method invocation is a direct call on the 'this' reference this.bar(); } public void bar() { // some logic... } } class SimplePojo : Pojo { fun foo() { // this next method invocation is a direct call on the 'this' reference this.bar() } fun bar() { // some logic... } } If you invoke a method on an object reference, the method is invoked directly on that object reference, as the following image and listing show: Java Kotlin public class Main { public static void main(String[] args) { Pojo pojo = new SimplePojo(); // this is a direct method call on the 'pojo' reference pojo.foo(); } } fun main() { val pojo = SimplePojo() // this is a direct method call on the 'pojo' reference pojo.foo() } Things change slightly when the reference that client code has is a proxy. Consider the following diagram and code snippet: Java Kotlin public class Main { public static void main(String[] args) { ProxyFactory factory = new ProxyFactory(new SimplePojo()); factory.addInterface(Pojo.class); factory.addAdvice(new RetryAdvice()); Pojo pojo = (Pojo) factory.getProxy(); // this is a method call on the proxy! pojo.foo(); } } fun main() { val factory = ProxyFactory(SimplePojo()) factory.addInterface(Pojo::class.java) factory.addAdvice(RetryAdvice()) val pojo = factory.proxy as Pojo // this is a method call on the proxy! pojo.foo() } The key thing to understand here is that the client code inside the main(..) method of the Main class has a reference to the proxy. This means that method calls on that object reference are calls on the proxy. As a result, the proxy can delegate to all of the interceptors (advice) that are relevant to that particular method call. However, once the call has finally reached the target object (the SimplePojo reference in this case), any method calls that it may make on itself, such as this.bar() or this.foo() , are going to be invoked against the this reference, and not the proxy. This has important implications. It means that self invocation is not going to result in the advice associated with a method invocation getting a chance to run. In other words, self invocation via an explicit or implicit this reference will bypass the advice. To address that, you have the following options. Avoid self invocation The best approach (the term "best" is used loosely here) is to refactor your code such that the self invocation does not happen. This does entail some work on your part, but it is the best, least-invasive approach. Inject a self reference An alternative approach is to make use of self injection , and invoke methods on the proxy via the self reference instead of via this . Use AopContext.currentProxy() This last approach is highly discouraged, and we hesitate to point it out, in favor of the previous options. However, as a last resort you can choose to tie the logic within your class to Spring AOP, as the following example shows. Java Kotlin public class SimplePojo implements Pojo { public void foo() { // This works, but it should be avoided if possible. ((Pojo) AopContext.currentProxy()).bar(); } public void bar() { // some logic... } } class SimplePojo : Pojo { fun foo() { // This works, but it should be avoided if possible. (AopContext.currentProxy() as Pojo).bar() } fun bar() { // some logic... } } The use of AopContext.currentProxy() totally couples your code to Spring AOP, and it makes the class itself aware of the fact that it is being used in an AOP context, which reduces some of the benefits of AOP. It also requires that the ProxyFactory is configured to expose the proxy, as the following example shows: Java Kotlin public class Main { public static void main(String[] args) { ProxyFactory factory = new ProxyFactory(new SimplePojo()); factory.addInterface(Pojo.class); factory.addAdvice(new RetryAdvice()); factory.setExposeProxy(true); Pojo pojo = (Pojo) factory.getProxy(); // this is a method call on the proxy! pojo.foo(); } } fun main() { val factory = ProxyFactory(SimplePojo()) factory.addInterface(Pojo::class.java) factory.addAdvice(RetryAdvice()) factory.isExposeProxy = true val pojo = factory.proxy as Pojo // this is a method call on the proxy! pojo.foo() } AspectJ compile-time weaving and load-time weaving do not have this self-invocation issue because they apply advice within the bytecode instead of via a proxy. Mixing Aspect Types Programmatic Creation of @AspectJ Proxies Spring Framework Stable 7.0.8 6.2.19 Snapshot 7.1.0-SNAPSHOT 7.0.9-SNAPSHOT 6.2.20-SNAPSHOT Related Spring Documentation Spring Boot Spring Framework Spring Cloud Spring Cloud Build Spring Cloud Bus Spring Cloud Circuit Breaker Spring Cloud Commons Spring Cloud Config Spring Cloud Consul Spring Cloud Contract Spring Cloud Function Spring Cloud Gateway Spring Cloud Kubernetes Spring Cloud Netflix Spring Cloud OpenFeign Spring Cloud Stream Spring Cloud Task Spring Cloud Vault Spring Cloud Zookeeper Spring Data Spring Data Cassandra Spring Data Commons Spring Data Couchbase Spring Data Elasticsearch Spring Data JPA Spring Data KeyValue Spring Data LDAP Spring Data MongoDB Spring Data Neo4j Spring Data Redis Spring Data JDBC & R2DBC Spring Data REST Spring Integration Spring Batch Spring Security Spring Authorization Server Spring LDAP Spring Security Kerberos Spring Session Spring Vault Spring AI Spring AMQP Spring CLI Spring GraphQL Spring for Apache Kafka Spring Modulith Spring for Apache Pulsar Spring Shell All Docs... Copyright © 2005 - Broadcom. All Rights Reserved. The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries. Terms of Use • Privacy • Trademark Guidelines • Thank you • Your California Privacy Rights • Cookie Settings Apache®, Apache Tomcat®, Apache Kafka®, Apache Cassandra™, and Apache Geode™ are trademarks or registered trademarks of the Apache Software Foundation in the United States and/or other countries. Java™, Java™ SE, Java™ EE, and OpenJDK™ are trademarks of Oracle and/or its affiliates. Kubernetes® is a registered trademark of the Linux Foundation in the United States and other countries. Linux® is the registered trademark of Linus Torvalds in the United States and other countries. Windows® and Microsoft® Azure are registered trademarks of Microsoft Corporation. “AWS” and “Amazon Web Services” are trademarks or registered trademarks of Amazon.com Inc. or its affiliates. All other trademarks and copyrights are property of their respective owners and are only mentioned for informative purposes. Other names may be trademarks of their respective owners. Search in all Spring Docs
