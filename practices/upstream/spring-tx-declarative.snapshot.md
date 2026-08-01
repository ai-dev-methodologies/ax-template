# spring-tx-declarative — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T02:24:33Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r156`
**Body SHA-256 (below the `---` divider, header excluded):** faacc73bfdbec6b60c6ab512b8658072688d992f67a7b327ff62458cb9af4f0f

---

---
snapshot_id: spring-tx-declarative
source: "https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 3
bytes: 21383
sha: "b6f171f0e4358fc34d0f01bf55b234d03cd292b3e5248194718b97292b4d2d85"
---

# spring tx declarative — upstream snapshot

Source: https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html
Fetched: 2026-07-14

Using @Transactional :: Spring Framework
Edit this Page
 
 
 
 GitHub Project
 
 
 
 Stack Overflow

# Using @Transactional
In addition to the XML-based declarative approach to transaction configuration, you can
use an annotation-based approach. Declaring transaction semantics directly in the Java
source code puts the declarations much closer to the affected code. There is not much
danger of undue coupling, because code that is meant to be used transactionally is
almost always deployed that way anyway.
The standard jakarta.transaction.Transactional annotation is also supported as
a drop-in replacement for Spring’s own annotation. Please refer to the JTA documentation
for more details.
The ease-of-use afforded by the use of the @Transactional annotation is best
illustrated with an example, which is explained in the text that follows.
Consider the following class definition:
Java
Kotlin
// the service class that we want to make transactional
@Transactional
public class DefaultFooService implements FooService {

 @Override
 public Foo getFoo(String fooName) {
 // ...
 }

 @Override
 public Foo getFoo(String fooName, String barName) {
 // ...
 }

 @Override
 public void insertFoo(Foo foo) {
 // ...
 }

 @Override
 public void updateFoo(Foo foo) {
 // ...
 }
}
// the service class that we want to make transactional
@Transactional
class DefaultFooService : FooService {

 override fun getFoo(fooName: String): Foo {
 // ...
 }

 override fun getFoo(fooName: String, barName: String): Foo {
 // ...
 }

 override fun insertFoo(foo: Foo) {
 // ...
 }

 override fun updateFoo(foo: Foo) {
 // ...
 }
}
Used at the class level as above, the annotation indicates a default for all methods
of the declaring class (as well as its subclasses). Alternatively, each method can be
annotated individually. See
method visibility
for further details on which methods Spring considers transactional. Note that a class-level
annotation does not apply to ancestor classes up the class hierarchy; in such a scenario,
inherited methods need to be locally redeclared in order to participate in a
subclass-level annotation.
When a POJO class such as the one above is defined as a bean in a Spring context,
you can make the bean instance transactional through an @EnableTransactionManagement
annotation in a @Configuration class. See the
javadoc
for full details.
The following example shows the configuration needed to enable annotation-driven transaction management:
Java
Kotlin
Xml
@Configuration
@EnableTransactionManagement
public class AppConfig {

 @Bean
 public FooService fooService() {
 return new DefaultFooService();
 }

 @Bean
 public PlatformTransactionManager txManager(DataSource dataSource) {
 return new DataSourceTransactionManager(dataSource);
 }
}
@Configuration
@EnableTransactionManagement
class AppConfig {

 @Bean
 fun fooService(): FooService {
 return DefaultFooService()
 }

 @Bean
 fun txManager(dataSource: DataSource): PlatformTransactionManager {
 return DataSourceTransactionManager(dataSource)
 }
}
In programmatic configuration, the @EnableTransactionManagement annotation uses any
TransactionManager bean in the context. In XML configuration, you can omit
the transaction-manager attribute in the tag if the bean
name of the TransactionManager that you want to wire in has the name transactionManager.
If the TransactionManager bean has any other name, you have to use the
transaction-manager attribute explicitly, as in the preceding example.
Reactive transactional methods use reactive return types in contrast to imperative
programming arrangements as the following listing shows:
Java
Kotlin
// the reactive service class that we want to make transactional
@Transactional
public class DefaultFooService implements FooService {

 @Override
 public Publisher getFoo(String fooName) {
 // ...
 }

 @Override
 public Mono getFoo(String fooName, String barName) {
 // ...
 }

 @Override
 public Mono insertFoo(Foo foo) {
 // ...
 }

 @Override
 public Mono updateFoo(Foo foo) {
 // ...
 }
}
// the reactive service class that we want to make transactional
@Transactional
class DefaultFooService : FooService {

 override fun getFoo(fooName: String): Flow {
 // ...
 }

 override fun getFoo(fooName: String, barName: String): Mono {
 // ...
 }

 override fun insertFoo(foo: Foo): Mono {
 // ...
 }

 override fun updateFoo(foo: Foo): Mono {
 // ...
 }
}
Note that there are special considerations for the returned Publisher with regards to
Reactive Streams cancellation signals. See the
Cancel Signals
section under "Using the TransactionalOperator" for more details.
Method visibility and @Transactional in proxy mode
The @Transactional annotation is typically used on methods with public visibility.
As of 6.0, protected or package-visible methods can also be made transactional for
class-based proxies by default. Note that transactional methods in interface-based
proxies must always be public and defined in the proxied interface. For both kinds
of proxies, only external method calls coming in through the proxy are intercepted.
If you prefer consistent treatment of method visibility across the different kinds of
proxies (which was the default up until 5.3), consider specifying publicMethodsOnly:
/**
 * Register a custom AnnotationTransactionAttributeSource with the
 * publicMethodsOnly flag set to true to consistently ignore non-public methods.
 * @see ProxyTransactionManagementConfiguration#transactionAttributeSource()
 */
@Bean
TransactionAttributeSource transactionAttributeSource() {
 return new AnnotationTransactionAttributeSource(true);
}
The Spring TestContext Framework supports non-private @Transactional test methods
by default as well. See Transaction Management
in the testing chapter for examples.
You can apply the @Transactional annotation to an interface definition, a method
on an interface, a class definition, or a method on a class. However, the mere presence
of the @Transactional annotation is not enough to activate the transactional behavior.
The @Transactional annotation is merely metadata that can be consumed by corresponding
runtime infrastructure which uses that metadata to configure the appropriate beans with
transactional behavior. In the preceding examples that use programmatic configuration,
the @EnableTransactionManagement annotation switches on actual transaction management
at runtime. Whereas, in the preceding example that uses XML configuration, the
 element switches on actual transaction management at runtime.
The Spring team recommends that you annotate methods of concrete classes with the
@Transactional annotation, rather than relying on annotated methods in interfaces,
even if the latter does work for interface-based and target-class proxies as of 5.0.
Since Java annotations are not inherited from interfaces, interface-declared annotations
are still not recognized by the weaving infrastructure when using AspectJ mode, so the
aspect does not get applied. As a consequence, your transaction annotations may be
silently ignored: Your code might appear to "work" until you test a rollback scenario.
In proxy mode (which is the default), only external method calls coming in through
the proxy are intercepted. This means that self-invocation (in effect, a method within
the target object calling another method of the target object) does not lead to an actual
transaction at runtime even if the invoked method is marked with @Transactional. Also,
the proxy must be fully initialized to provide the expected behavior, so you should not
rely on this feature in your initialization code — for example, in a @PostConstruct method.
Consider using AspectJ mode (see the mode attribute in the following table) if you
expect self-invocations to be wrapped with transactions as well. In this case, there is
no proxy in the first place. Instead, the target class is woven (that is, its byte code
is modified) to support @Transactional runtime behavior on any kind of method.
Table 1. Annotation driven transaction settings
XML Attribute
Annotation Attribute
Default
Description
transaction-manager
N/A (see TransactionManagementConfigurer javadoc)
transactionManager
Name of the transaction manager to use. Required only if the name of the transaction
 manager is not transactionManager, as in the preceding example.
mode
mode
proxy
The default mode (proxy) processes annotated beans to be proxied by using Spring’s AOP
 framework (following proxy semantics, as discussed earlier, applying to method calls
 coming in through the proxy only). The alternative mode (aspectj) instead weaves the
 affected classes with Spring’s AspectJ transaction aspect, modifying the target class
 byte code to apply to any kind of method call. AspectJ weaving requires spring-aspects.jar
 in the classpath as well as having load-time weaving (or compile-time weaving) enabled.
 (See Spring configuration for details
 on how to set up load-time weaving.)
proxy-target-class
proxyTargetClass
false
Applies to proxy mode only. Controls what type of transactional proxies are created
 for classes annotated with the @Transactional annotation. If the proxy-target-class
 attribute is set to true, class-based proxies are created. If proxy-target-class is
 false or if the attribute is omitted, then standard JDK interface-based proxies are
 created. (See Proxying Mechanisms for a detailed examination
 of the different proxy types.)
order
order
Ordered.LOWEST_PRECEDENCE
Defines the order of the transaction advice that is applied to beans annotated with
 @Transactional. (For more information about the rules related to ordering of AOP
 advice, see Advice Ordering.)
 No specified ordering means that the AOP subsystem determines the order of the advice.
The default advice mode for processing @Transactional annotations is proxy,
which allows for interception of calls through the proxy only. Local calls within the
same class cannot get intercepted that way. For a more advanced mode of interception,
consider switching to aspectj mode in combination with compile-time or load-time weaving.
The proxy-target-class attribute controls what type of transactional proxies are
created for classes annotated with the @Transactional annotation. If
proxy-target-class is set to true, class-based proxies are created. If
proxy-target-class is false or if the attribute is omitted, standard JDK
interface-based proxies are created. (See Proxying Mechanisms
for a discussion of the different proxy types.)
@EnableTransactionManagement and look for
@Transactional only on beans in the same application context in which they are defined.
This means that, if you put annotation-driven configuration in a WebApplicationContext
for a DispatcherServlet, it checks for @Transactional beans only in your controllers
and not in your services. See MVC for more information.
The most derived location takes precedence when evaluating the transactional settings
for a method. In the case of the following example, the DefaultFooService class is
annotated at the class level with the settings for a read-only transaction, but the
@Transactional annotation on the updateFoo(Foo) method in the same class takes
precedence over the transactional settings defined at the class level.
Java
Kotlin
@Transactional(readOnly = true)
public class DefaultFooService implements FooService {

 public Foo getFoo(String fooName) {
 // ...
 }

 // these settings have precedence for this method
 @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
 public void updateFoo(Foo foo) {
 // ...
 }
}
@Transactional(readOnly = true)
class DefaultFooService : FooService {

 override fun getFoo(fooName: String): Foo {
 // ...
 }

 // these settings have precedence for this method
 @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
 override fun updateFoo(foo: Foo) {
 // ...
 }
}

## @Transactional Settings
The @Transactional annotation is metadata that specifies that an interface, class,
or method must have transactional semantics (for example, "start a brand new read-only
transaction when this method is invoked, suspending any existing transaction").
The default @Transactional settings are as follows:
The propagation setting is PROPAGATION_REQUIRED.
The isolation level is ISOLATION_DEFAULT.
The transaction is read-write.
The transaction timeout defaults to the default timeout of the underlying transaction
system, or to none if timeouts are not supported.
Any RuntimeException or Error triggers rollback, and any checked Exception does
not.
You can change these default settings. The following table summarizes the various
properties of the @Transactional annotation:
Table 2. @Transactional Settings
Property
Type
Description
value
String
Optional qualifier that specifies the transaction manager to be used.
transactionManager
String
Alias for value.
label
Array of String labels to add an expressive description to the transaction.
Labels may be evaluated by transaction managers to associate implementation-specific behavior with the actual transaction.
propagation
enum: Propagation
Optional propagation setting.
isolation
enum: Isolation
Optional isolation level. Applies only to propagation values of REQUIRED or REQUIRES_NEW.
timeout
int (in seconds of granularity)
Optional transaction timeout. Applies only to propagation values of REQUIRED or REQUIRES_NEW.
timeoutString
String (in seconds of granularity)
Alternative for specifying the timeout in seconds as a String value — for example, as a placeholder.
readOnly
boolean
Read-write versus read-only transaction. Only applicable to values of REQUIRED or REQUIRES_NEW.
rollbackFor
Array of Class objects, which must be derived from Throwable.
Optional array of exception types that must cause rollback.
rollbackForClassName
Array of exception name patterns.
Optional array of exception name patterns that must cause rollback.
noRollbackFor
Array of Class objects, which must be derived from Throwable.
Optional array of exception types that must not cause rollback.
noRollbackForClassName
Array of exception name patterns.
Optional array of exception name patterns that must not cause rollback.
See
Rollback rules
for further details on rollback rule semantics, patterns, and warnings
regarding possible unintentional matches for pattern-based rollback rules.
As of 6.2, you can globally change the default rollback behavior – for example, through
@EnableTransactionManagement(rollbackOn=ALL_EXCEPTIONS), leading to a rollback
for all exceptions raised within a transaction, including any checked exception.
For further customizations, AnnotationTransactionAttributeSource provides an
addDefaultRollbackRule(RollbackRuleAttribute) method for custom default rules.
Note that transaction-specific rollback rules override the default behavior but
retain the chosen default for unspecified exceptions. This is the case for
Spring’s @Transactional as well as JTA’s jakarta.transaction.Transactional
annotation.
Unless you rely on EJB-style business exceptions with commit behavior, it is
advisable to switch to ALL_EXCEPTIONS for consistent rollback semantics even
in case of a (potentially accidental) checked exception. Also, it is advisable
to make that switch for Kotlin-based applications where there is no enforcement
of checked exceptions at all.
Currently, you cannot have explicit control over the name of a transaction, where 'name'
means the transaction name that appears in a transaction monitor and in logging output.
For declarative transactions, the transaction name is always the fully-qualified class
name of the transactionally advised class + . + the method name. For example, if the
handlePayment(..) method of the BusinessService class started a transaction, the
name of the transaction would be com.example.BusinessService.handlePayment.

## Multiple Transaction Managers with @Transactional
Most Spring applications need only a single transaction manager, but there may be
situations where you want multiple independent transaction managers in a single
application. You can use the value or transactionManager attribute of the
@Transactional annotation to optionally specify the identity of the
TransactionManager to be used. This can either be the bean name or the qualifier value
of the transaction manager bean. For example, using the qualifier notation, you can
combine the following Java code with the following transaction manager bean declarations
in the application context:
Java
Kotlin
public class TransactionalService {

 @Transactional("order")
 public void setSomething(String name) { ... }

 @Transactional("account")
 public void doSomething() { ... }

 @Transactional("reactive-account")
 public Mono doSomethingReactive() { ... }
}
class TransactionalService {

 @Transactional("order")
 fun setSomething(name: String) {
 // ...
 }

 @Transactional("account")
 fun doSomething() {
 // ...
 }

 @Transactional("reactive-account")
 fun doSomethingReactive(): Mono {
 // ...
 }
}
The following listing shows the bean declarations:
...
 
 

 
 ...
 
 

 
 ...
In this case, the individual methods on TransactionalService run under separate
transaction managers, differentiated by the order, account, and reactive-account
qualifiers. The default target bean name, transactionManager,
is still used if no specifically qualified TransactionManager bean is found.
If all transactional methods on the same class share the same qualifier, consider
declaring a type-level org.springframework.beans.factory.annotation.Qualifier
annotation instead. If its value matches the qualifier value (or bean name) of a
specific transaction manager, that transaction manager is going to be used for
transaction definitions without a specific qualifier on @Transactional itself.
Such a type-level qualifier can be declared on the concrete class, applying to
transaction definitions from a base class as well. This effectively overrides
the default transaction manager choice for any unqualified base class methods.
Last but not least, such a type-level bean qualifier can serve multiple purposes,
for example, with a value of "order" it can be used for autowiring purposes (identifying
the order repository) as well as transaction manager selection, as long as the
target beans for autowiring as well as the associated transaction manager
definitions declare the same qualifier value. Such a qualifier value only needs
to be unique within a set of type-matching beans, not having to serve as an ID.

## Custom Composed Annotations
If you find you repeatedly use the same attributes with @Transactional on many different methods,
Spring’s meta-annotation support
lets you define custom composed annotations for your specific use cases. For example, consider the
following annotation definitions:
Java
Kotlin
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Transactional(transactionManager = "order", label = "causal-consistency")
public @interface OrderTx {
}

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Transactional(transactionManager = "account", label = "retryable")
public @interface AccountTx {
}
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@Transactional(transactionManager = "order", label = ["causal-consistency"])
annotation class OrderTx

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@Transactional(transactionManager = "account", label = ["retryable"])
annotation class AccountTx
The preceding annotations let us write the example from the previous section as follows:
Java
Kotlin
public class TransactionalService {

 @OrderTx
 public void setSomething(String name) {
 // ...
 }

 @AccountTx
 public void doSomething() {
 // ...
 }
}
class TransactionalService {

 @OrderTx
 fun setSomething(name: String) {
 // ...
 }

 @AccountTx
 fun doSomething() {
 // ...
 }
}
In the preceding example, we used the syntax to define the transaction manager qualifier
and transactional labels, but we could also have included propagation behavior,
rollback rules, timeouts, and other features.
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

Source: https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html
HTTP status: 200 · extracted bytes: 34837 · sha256: 4e62112f5c9cd2e999cf44f334c93f47df49a489ff09ac191d5fe610d573287b
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r156`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

Using @Transactional :: Spring Framework Why Spring Overview Trending Generative AI Cloud Architecture Patterns Microservices Reactive Event Driven Application Types Web Applications Serverless Batch Learn Getting Started Quickstart Guides Academy Courses Get Certified Projects Overview Projects Spring Boot Spring Framework Spring Cloud Spring AI Spring Data Spring Integration Spring Batch Spring Security Foundational Projects Micrometer Reactor Development Tools Spring Tools Spring Initializr Resources Blog Release Calendar Version Mappings Release Highlights Security Advisories GitHub Orgs Spring Projects Spring Cloud Community Overview Events Authors Enterprise Overview Long-term Support Automated Upgrades Governance and Compliance Modern App Development light Spring Framework 7.0.8 Search Overview Core Technologies The IoC Container Introduction to the Spring IoC Container and Beans Container Overview Bean Overview Dependencies Dependency Injection Dependencies and Configuration in Detail Using depends-on Lazy-initialized Beans Autowiring Collaborators Method Injection Bean Scopes Customizing the Nature of a Bean Bean Definition Inheritance Container Extension Points Annotation-based Container Configuration Using @Autowired Fine-tuning Annotation-based Autowiring with @Primary or @Fallback Fine-tuning Annotation-based Autowiring with Qualifiers Using Generics as Autowiring Qualifiers Using CustomAutowireConfigurer Injection with @Resource Using @Value Using @PostConstruct and @PreDestroy Classpath Scanning and Managed Components Using JSR-330 Standard Annotations Java-based Container Configuration Basic Concepts: @Bean and @Configuration Instantiating the Spring Container by Using AnnotationConfigApplicationContext Using the @Bean Annotation Using the @Configuration annotation Composing Java-based Configurations Programmatic Bean Registration Environment Abstraction Registering a LoadTimeWeaver Additional Capabilities of the ApplicationContext The BeanFactory API Resources Validation, Data Binding, and Type Conversion Validation Using Spring’s Validator Interface Data Binding Resolving Error Codes to Error Messages Spring Type Conversion Spring Field Formatting Configuring a Global Date and Time Format Java Bean Validation Spring Expression Language (SpEL) Evaluation Expressions in Bean Definitions Language Reference Literal Expressions Properties, Arrays, Lists, Maps, and Indexers Inline Lists Inline Maps Array Construction Methods Operators Types Constructors Variables Functions Varargs Invocations Bean References Ternary Operator (If-Then-Else) The Elvis Operator Safe Navigation Operator Collection Selection Collection Projection Expression Templating Classes Used in the Examples Aspect Oriented Programming with Spring AOP Concepts Spring AOP Capabilities and Goals AOP Proxies @AspectJ support Enabling @AspectJ Support Declaring an Aspect Declaring a Pointcut Declaring Advice Introductions Aspect Instantiation Models An AOP Example Schema-based AOP Support Choosing which AOP Declaration Style to Use Mixing Aspect Types Proxying Mechanisms Programmatic Creation of @AspectJ Proxies Using AspectJ with Spring Applications Further Resources Spring AOP APIs Pointcut API in Spring Advice API in Spring The Advisor API in Spring Using the ProxyFactoryBean to Create AOP Proxies Concise Proxy Definitions Creating AOP Proxies Programmatically with the ProxyFactory Manipulating Advised Objects Using the "auto-proxy" facility Using TargetSource Implementations Defining New Advice Types Resilience Features Null-safety Data Buffers and Codecs Ahead of Time Optimizations Appendix XML Schemas XML Schema Authoring Application Startup Steps Data Access Transaction Management Advantages of the Spring Framework’s Transaction Support Model Understanding the Spring Framework Transaction Abstraction Synchronizing Resources with Transactions Declarative Transaction Management Understanding the Spring Framework’s Declarative Transaction Implementation Example of Declarative Transaction Implementation Rolling Back a Declarative Transaction Configuring Different Transactional Semantics for Different Beans <tx:advice/> Settings Using @Transactional Transaction Propagation Advising Transactional Operations Using @Transactional with AspectJ Programmatic Transaction Management Choosing Between Programmatic and Declarative Transaction Management Transaction-bound Events Application server-specific integration Solutions to Common Problems Further Resources DAO Support Data Access with JDBC Choosing an Approach for JDBC Database Access Package Hierarchy Using the JDBC Core Classes to Control Basic JDBC Processing and Error Handling Controlling Database Connections JDBC Batch Operations Simplifying JDBC Operations with the SimpleJdbc Classes Modeling JDBC Operations as Java Objects Common Problems with Parameter and Data Value Handling Embedded Database Support Initializing a DataSource Data Access with R2DBC Object Relational Mapping (ORM) Data Access Introduction to ORM with Spring General ORM Integration Considerations Hibernate JPA Marshalling XML by Using Object-XML Mappers Appendix Web on Servlet Stack Spring Web MVC DispatcherServlet Context Hierarchy Special Bean Types Web MVC Config Servlet Config Processing Path Matching Interception Exceptions View Resolution Locale Multipart Resolver Logging Filters HTTP Message Conversion Annotated Controllers Declaration Mapping Requests Handler Methods Method Arguments Return Values Type Conversion Matrix Variables @RequestParam @RequestHeader @CookieValue @ModelAttribute @SessionAttributes @SessionAttribute @RequestAttribute Redirect Attributes Flash Attributes Multipart @RequestBody HttpEntity @ResponseBody ResponseEntity Jackson JSON Model @InitBinder Validation Exceptions Controller Advice Functional Endpoints URI Links Asynchronous Requests Range Requests Data Binding CORS API Versioning Error Responses Web Security HTTP Caching View Technologies Thymeleaf FreeMarker Groovy Markup Script Views HTML Fragments JSP and JSTL RSS and Atom PDF and Excel Jackson XML Marshalling XSLT Views MVC Config Enable MVC Configuration MVC Config API Type Conversion Validation Interceptors Content Types Message Converters View Controllers View Resolvers Static Resources Default Servlet Path Matching API Version Advanced Java Config Advanced XML Config HTTP/2 REST Clients Testing WebSockets WebSocket API SockJS Fallback STOMP Overview Benefits Enable STOMP WebSocket Transport Flow of Messages Annotated Controllers Sending Messages Simple Broker External Broker Connecting to a Broker Dots as Separators Authentication Token Authentication Authorization User Destinations Order of Messages Events Interception STOMP Client WebSocket Scope Performance Monitoring Testing Web on Reactive Stack Spring WebFlux Overview Reactive Core DispatcherHandler Annotated Controllers @Controller Mapping Requests Handler Methods Method Arguments Return Values Type Conversion Matrix Variables @RequestParam @RequestHeader @CookieValue @ModelAttribute @SessionAttributes @SessionAttribute @RequestAttribute Multipart Content @RequestBody HttpEntity @ResponseBody ResponseEntity Jackson JSON Model DataBinder Validation Exceptions Controller Advice Functional Endpoints URI Links Range Requests Data Binding CORS API Versioning Error Responses Web Security HTTP Caching View Technologies WebFlux Config HTTP/2 WebClient Configuration retrieve() Exchange Request Body Filters Attributes Context Synchronous Use Testing HTTP Service Client WebSockets Testing RSocket Reactive Libraries Testing Introduction to Spring Testing Unit Testing Integration Testing JDBC Testing Support Spring TestContext Framework Key Abstractions Bootstrapping the TestContext Framework TestExecutionListener Configuration Application Events Test Execution Events Context Management Context Configuration with Component Classes Context Configuration with XML Resources Context Configuration with Groovy Scripts Default Context Configuration Mixing Component Classes, XML, and Groovy Scripts Context Configuration with Context Customizers Context Configuration with Context Initializers Context Configuration Inheritance Context Configuration with Environment Profiles Context Configuration with Test Property Sources Context Configuration with Dynamic Property Sources Loading a WebApplicationContext Working with Web Mocks Context Caching Context Pausing Context Failure Threshold Context Hierarchies Dependency Injection of Test Fixtures Bean Overriding in Tests Testing Request- and Session-scoped Beans Transaction Management Executing SQL Scripts Parallel Test Execution TestContext Framework Support Classes Ahead of Time Support for Tests WebTestClient RestTestClient MockMvc Overview Setup Options Hamcrest Integration Static Imports Configuring MockMvc Setup Features Performing Requests Defining Expectations Async Requests Streaming Responses Filter Registrations AssertJ Integration Configuring MockMvcTester Performing Requests Defining Expectations MockMvc integration HtmlUnit Integration Why HtmlUnit Integration? MockMvc and HtmlUnit MockMvc and WebDriver MockMvc and Geb MockMvc vs End-to-End Tests Further Examples Testing Client Applications Appendix Annotations Standard Annotation Support Spring Testing Annotations @BootstrapWith @ContextConfiguration @WebAppConfiguration @ContextHierarchy @ContextCustomizerFactories @ActiveProfiles @TestPropertySource @DynamicPropertySource @TestBean @MockitoBean and @MockitoSpyBean @DirtiesContext @TestExecutionListeners @RecordApplicationEvents @Commit @Rollback @BeforeTransaction @AfterTransaction @Sql @SqlConfig @SqlMergeMode @SqlGroup @DisabledInAotMode Spring JUnit 4 Testing Annotations Spring JUnit Jupiter Testing Annotations Meta-Annotation Support for Testing Further Resources Integration REST Clients JMS (Java Message Service) Using Spring JMS Sending a Message Receiving a Message Support for JCA Message Endpoints Annotation-driven Listener Endpoints JMS Namespace Support JMX Exporting Your Beans to JMX Controlling the Management Interface of Your Beans Controlling ObjectName Instances for Your Beans Using JSR-160 Connectors Accessing MBeans through Proxies Notifications Further Resources Email Task Execution and Scheduling Cache Abstraction Understanding the Cache Abstraction Declarative Annotation-based Caching JCache (JSR-107) Annotations Declarative XML-based Caching Configuring the Cache Storage Plugging-in Different Back-end Caches How can I Set the TTL/TTI/Eviction policy/XXX feature? Observability Support JVM AOT Cache JVM Checkpoint Restore Appendix Language Support Kotlin Requirements Extensions Null-safety Classes and Interfaces Annotations Bean Registration DSL Web Coroutines Spring Projects in Kotlin Getting Started Resources Apache Groovy Appendix Java API Kotlin API Wiki Search Edit this Page GitHub Project Stack Overflow Spring Framework Data Access Transaction Management Declarative Transaction Management Using @Transactional Using @Transactional In addition to the XML-based declarative approach to transaction configuration, you can use an annotation-based approach. Declaring transaction semantics directly in the Java source code puts the declarations much closer to the affected code. There is not much danger of undue coupling, because code that is meant to be used transactionally is almost always deployed that way anyway. The standard jakarta.transaction.Transactional annotation is also supported as a drop-in replacement for Spring’s own annotation. Please refer to the JTA documentation for more details. The ease-of-use afforded by the use of the @Transactional annotation is best illustrated with an example, which is explained in the text that follows. Consider the following class definition: Java Kotlin // the service class that we want to make transactional @Transactional public class DefaultFooService implements FooService { @Override public Foo getFoo(String fooName) { // ... } @Override public Foo getFoo(String fooName, String barName) { // ... } @Override public void insertFoo(Foo foo) { // ... } @Override public void updateFoo(Foo foo) { // ... } } // the service class that we want to make transactional @Transactional class DefaultFooService : FooService { override fun getFoo(fooName: String): Foo { // ... } override fun getFoo(fooName: String, barName: String): Foo { // ... } override fun insertFoo(foo: Foo) { // ... } override fun updateFoo(foo: Foo) { // ... } } Used at the class level as above, the annotation indicates a default for all methods of the declaring class (as well as its subclasses). Alternatively, each method can be annotated individually. See method visibility for further details on which methods Spring considers transactional. Note that a class-level annotation does not apply to ancestor classes up the class hierarchy; in such a scenario, inherited methods need to be locally redeclared in order to participate in a subclass-level annotation. When a POJO class such as the one above is defined as a bean in a Spring context, you can make the bean instance transactional through an @EnableTransactionManagement annotation in a @Configuration class. See the javadoc for full details. The following example shows the configuration needed to enable annotation-driven transaction management: Java Kotlin Xml @Configuration @EnableTransactionManagement public class AppConfig { @Bean public FooService fooService() { return new DefaultFooService(); } @Bean public PlatformTransactionManager txManager(DataSource dataSource) { return new DataSourceTransactionManager(dataSource); } } @Configuration @EnableTransactionManagement class AppConfig { @Bean fun fooService(): FooService { return DefaultFooService() } @Bean fun txManager(dataSource: DataSource): PlatformTransactionManager { return DataSourceTransactionManager(dataSource) } } <beans xmlns="http://www.springframework.org/schema/beans" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:aop="http://www.springframework.org/schema/aop" xmlns:tx="http://www.springframework.org/schema/tx" xsi:schemaLocation=" http://www.springframework.org/schema/beans https://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/tx https://www.springframework.org/schema/tx/spring-tx.xsd http://www.springframework.org/schema/aop https://www.springframework.org/schema/aop/spring-aop.xsd"> <!-- this is the service object that we want to make transactional --> <bean id="fooService" class="x.y.service.DefaultFooService"/> <!-- enable the configuration of transactional behavior based on annotations --> <!-- a TransactionManager is still required --> <tx:annotation-driven transaction-manager="txManager"/> <bean id="txManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager"> <!-- (this dependency is defined somewhere else) --> <property name="dataSource" ref="dataSource"/> </bean> <!-- other <bean/> definitions here --> </beans> In programmatic configuration, the @EnableTransactionManagement annotation uses any TransactionManager bean in the context. In XML configuration, you can omit the transaction-manager attribute in the <tx:annotation-driven/> tag if the bean name of the TransactionManager that you want to wire in has the name transactionManager . If the TransactionManager bean has any other name, you have to use the transaction-manager attribute explicitly, as in the preceding example. Reactive transactional methods use reactive return types in contrast to imperative programming arrangements as the following listing shows: Java Kotlin // the reactive service class that we want to make transactional @Transactional public class DefaultFooService implements FooService { @Override public Publisher<Foo> getFoo(String fooName) { // ... } @Override public Mono<Foo> getFoo(String fooName, String barName) { // ... } @Override public Mono<Void> insertFoo(Foo foo) { // ... } @Override public Mono<Void> updateFoo(Foo foo) { // ... } } // the reactive service class that we want to make transactional @Transactional class DefaultFooService : FooService { override fun getFoo(fooName: String): Flow<Foo> { // ... } override fun getFoo(fooName: String, barName: String): Mono<Foo> { // ... } override fun insertFoo(foo: Foo): Mono<Void> { // ... } override fun updateFoo(foo: Foo): Mono<Void> { // ... } } Note that there are special considerations for the returned Publisher with regards to Reactive Streams cancellation signals. See the Cancel Signals section under "Using the TransactionalOperator" for more details. Method visibility and @Transactional in proxy mode The @Transactional annotation is typically used on methods with public visibility. As of 6.0, protected or package-visible methods can also be made transactional for class-based proxies by default. Note that transactional methods in interface-based proxies must always be public and defined in the proxied interface. For both kinds of proxies, only external method calls coming in through the proxy are intercepted. If you prefer consistent treatment of method visibility across the different kinds of proxies (which was the default up until 5.3), consider specifying publicMethodsOnly : /** * Register a custom AnnotationTransactionAttributeSource with the * publicMethodsOnly flag set to true to consistently ignore non-public methods. * @see ProxyTransactionManagementConfiguration#transactionAttributeSource() */ @Bean TransactionAttributeSource transactionAttributeSource() { return new AnnotationTransactionAttributeSource(true); } The Spring TestContext Framework supports non-private @Transactional test methods by default as well. See Transaction Management in the testing chapter for examples. You can apply the @Transactional annotation to an interface definition, a method on an interface, a class definition, or a method on a class. However, the mere presence of the @Transactional annotation is not enough to activate the transactional behavior. The @Transactional annotation is merely metadata that can be consumed by corresponding runtime infrastructure which uses that metadata to configure the appropriate beans with transactional behavior. In the preceding examples that use programmatic configuration, the @EnableTransactionManagement annotation switches on actual transaction management at runtime. Whereas, in the preceding example that uses XML configuration, the <tx:annotation-driven/> element switches on actual transaction management at runtime. The Spring team recommends that you annotate methods of concrete classes with the @Transactional annotation, rather than relying on annotated methods in interfaces, even if the latter does work for interface-based and target-class proxies as of 5.0. Since Java annotations are not inherited from interfaces, interface-declared annotations are still not recognized by the weaving infrastructure when using AspectJ mode, so the aspect does not get applied. As a consequence, your transaction annotations may be silently ignored: Your code might appear to "work" until you test a rollback scenario. In proxy mode (which is the default), only external method calls coming in through the proxy are intercepted. This means that self-invocation (in effect, a method within the target object calling another method of the target object) does not lead to an actual transaction at runtime even if the invoked method is marked with @Transactional . Also, the proxy must be fully initialized to provide the expected behavior, so you should not rely on this feature in your initialization code — for example, in a @PostConstruct method. Consider using AspectJ mode (see the mode attribute in the following table) if you expect self-invocations to be wrapped with transactions as well. In this case, there is no proxy in the first place. Instead, the target class is woven (that is, its byte code is modified) to support @Transactional runtime behavior on any kind of method. Table 1. Annotation driven transaction settings XML Attribute Annotation Attribute Default Description transaction-manager N/A (see TransactionManagementConfigurer javadoc) transactionManager Name of the transaction manager to use. Required only if the name of the transaction manager is not transactionManager , as in the preceding example. mode mode proxy The default mode ( proxy ) processes annotated beans to be proxied by using Spring’s AOP framework (following proxy semantics, as discussed earlier, applying to method calls coming in through the proxy only). The alternative mode ( aspectj ) instead weaves the affected classes with Spring’s AspectJ transaction aspect, modifying the target class byte code to apply to any kind of method call. AspectJ weaving requires spring-aspects.jar in the classpath as well as having load-time weaving (or compile-time weaving) enabled. (See Spring configuration for details on how to set up load-time weaving.) proxy-target-class proxyTargetClass false Applies to proxy mode only. Controls what type of transactional proxies are created for classes annotated with the @Transactional annotation. If the proxy-target-class attribute is set to true , class-based proxies are created. If proxy-target-class is false or if the attribute is omitted, then standard JDK interface-based proxies are created. (See Proxying Mechanisms for a detailed examination of the different proxy types.) order order Ordered.LOWEST_PRECEDENCE Defines the order of the transaction advice that is applied to beans annotated with @Transactional . (For more information about the rules related to ordering of AOP advice, see Advice Ordering .) No specified ordering means that the AOP subsystem determines the order of the advice. The default advice mode for processing @Transactional annotations is proxy , which allows for interception of calls through the proxy only. Local calls within the same class cannot get intercepted that way. For a more advanced mode of interception, consider switching to aspectj mode in combination with compile-time or load-time weaving. The proxy-target-class attribute controls what type of transactional proxies are created for classes annotated with the @Transactional annotation. If proxy-target-class is set to true , class-based proxies are created. If proxy-target-class is false or if the attribute is omitted, standard JDK interface-based proxies are created. (See Proxying Mechanisms for a discussion of the different proxy types.) @EnableTransactionManagement and <tx:annotation-driven/> look for @Transactional only on beans in the same application context in which they are defined. This means that, if you put annotation-driven configuration in a WebApplicationContext for a DispatcherServlet , it checks for @Transactional beans only in your controllers and not in your services. See MVC for more information. The most derived location takes precedence when evaluating the transactional settings for a method. In the case of the following example, the DefaultFooService class is annotated at the class level with the settings for a read-only transaction, but the @Transactional annotation on the updateFoo(Foo) method in the same class takes precedence over the transactional settings defined at the class level. Java Kotlin @Transactional(readOnly = true) public class DefaultFooService implements FooService { public Foo getFoo(String fooName) { // ... } // these settings have precedence for this method @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW) public void updateFoo(Foo foo) { // ... } } @Transactional(readOnly = true) class DefaultFooService : FooService { override fun getFoo(fooName: String): Foo { // ... } // these settings have precedence for this method @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW) override fun updateFoo(foo: Foo) { // ... } } @Transactional Settings The @Transactional annotation is metadata that specifies that an interface, class, or method must have transactional semantics (for example, "start a brand new read-only transaction when this method is invoked, suspending any existing transaction"). The default @Transactional settings are as follows: The propagation setting is PROPAGATION_REQUIRED. The isolation level is ISOLATION_DEFAULT. The transaction is read-write. The transaction timeout defaults to the default timeout of the underlying transaction system, or to none if timeouts are not supported. Any RuntimeException or Error triggers rollback, and any checked Exception does not. You can change these default settings. The following table summarizes the various properties of the @Transactional annotation: Table 2. @Transactional Settings Property Type Description value String Optional qualifier that specifies the transaction manager to be used. transactionManager String Alias for value . label Array of String labels to add an expressive description to the transaction. Labels may be evaluated by transaction managers to associate implementation-specific behavior with the actual transaction. propagation enum : Propagation Optional propagation setting. isolation enum : Isolation Optional isolation level. Applies only to propagation values of REQUIRED or REQUIRES_NEW . timeout int (in seconds of granularity) Optional transaction timeout. Applies only to propagation values of REQUIRED or REQUIRES_NEW . timeoutString String (in seconds of granularity) Alternative for specifying the timeout in seconds as a String value — for example, as a placeholder. readOnly boolean Read-write versus read-only transaction. Only applicable to values of REQUIRED or REQUIRES_NEW . rollbackFor Array of Class objects, which must be derived from Throwable. Optional array of exception types that must cause rollback. rollbackForClassName Array of exception name patterns. Optional array of exception name patterns that must cause rollback. noRollbackFor Array of Class objects, which must be derived from Throwable. Optional array of exception types that must not cause rollback. noRollbackForClassName Array of exception name patterns. Optional array of exception name patterns that must not cause rollback. See Rollback rules for further details on rollback rule semantics, patterns, and warnings regarding possible unintentional matches for pattern-based rollback rules. As of 6.2, you can globally change the default rollback behavior – for example, through @EnableTransactionManagement(rollbackOn=ALL_EXCEPTIONS) , leading to a rollback for all exceptions raised within a transaction, including any checked exception. For further customizations, AnnotationTransactionAttributeSource provides an addDefaultRollbackRule(RollbackRuleAttribute) method for custom default rules. Note that transaction-specific rollback rules override the default behavior but retain the chosen default for unspecified exceptions. This is the case for Spring’s @Transactional as well as JTA’s jakarta.transaction.Transactional annotation. Unless you rely on EJB-style business exceptions with commit behavior, it is advisable to switch to ALL_EXCEPTIONS for consistent rollback semantics even in case of a (potentially accidental) checked exception. Also, it is advisable to make that switch for Kotlin-based applications where there is no enforcement of checked exceptions at all. Currently, you cannot have explicit control over the name of a transaction, where 'name' means the transaction name that appears in a transaction monitor and in logging output. For declarative transactions, the transaction name is always the fully-qualified class name of the transactionally advised class + . + the method name. For example, if the handlePayment(..) method of the BusinessService class started a transaction, the name of the transaction would be com.example.BusinessService.handlePayment . Multiple Transaction Managers with @Transactional Most Spring applications need only a single transaction manager, but there may be situations where you want multiple independent transaction managers in a single application. You can use the value or transactionManager attribute of the @Transactional annotation to optionally specify the identity of the TransactionManager to be used. This can either be the bean name or the qualifier value of the transaction manager bean. For example, using the qualifier notation, you can combine the following Java code with the following transaction manager bean declarations in the application context: Java Kotlin public class TransactionalService { @Transactional("order") public void setSomething(String name) { ... } @Transactional("account") public void doSomething() { ... } @Transactional("reactive-account") public Mono<Void> doSomethingReactive() { ... } } class TransactionalService { @Transactional("order") fun setSomething(name: String) { // ... } @Transactional("account") fun doSomething() { // ... } @Transactional("reactive-account") fun doSomethingReactive(): Mono<Void> { // ... } } The following listing shows the bean declarations: <tx:annotation-driven/> <bean id="transactionManager1" class="org.springframework.jdbc.support.JdbcTransactionManager"> ... <qualifier value="order"/> </bean> <bean id="transactionManager2" class="org.springframework.jdbc.support.JdbcTransactionManager"> ... <qualifier value="account"/> </bean> <bean id="transactionManager3" class="org.springframework.data.r2dbc.connection.R2dbcTransactionManager"> ... <qualifier value="reactive-account"/> </bean> In this case, the individual methods on TransactionalService run under separate transaction managers, differentiated by the order , account , and reactive-account qualifiers. The default <tx:annotation-driven> target bean name, transactionManager , is still used if no specifically qualified TransactionManager bean is found. If all transactional methods on the same class share the same qualifier, consider declaring a type-level org.springframework.beans.factory.annotation.Qualifier annotation instead. If its value matches the qualifier value (or bean name) of a specific transaction manager, that transaction manager is going to be used for transaction definitions without a specific qualifier on @Transactional itself. Such a type-level qualifier can be declared on the concrete class, applying to transaction definitions from a base class as well. This effectively overrides the default transaction manager choice for any unqualified base class methods. Last but not least, such a type-level bean qualifier can serve multiple purposes, for example, with a value of "order" it can be used for autowiring purposes (identifying the order repository) as well as transaction manager selection, as long as the target beans for autowiring as well as the associated transaction manager definitions declare the same qualifier value. Such a qualifier value only needs to be unique within a set of type-matching beans, not having to serve as an ID. Custom Composed Annotations If you find you repeatedly use the same attributes with @Transactional on many different methods, Spring’s meta-annotation support lets you define custom composed annotations for your specific use cases. For example, consider the following annotation definitions: Java Kotlin @Target({ElementType.METHOD, ElementType.TYPE}) @Retention(RetentionPolicy.RUNTIME) @Transactional(transactionManager = "order", label = "causal-consistency") public @interface OrderTx { } @Target({ElementType.METHOD, ElementType.TYPE}) @Retention(RetentionPolicy.RUNTIME) @Transactional(transactionManager = "account", label = "retryable") public @interface AccountTx { } @Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE) @Retention(AnnotationRetention.RUNTIME) @Transactional(transactionManager = "order", label = ["causal-consistency"]) annotation class OrderTx @Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE) @Retention(AnnotationRetention.RUNTIME) @Transactional(transactionManager = "account", label = ["retryable"]) annotation class AccountTx The preceding annotations let us write the example from the previous section as follows: Java Kotlin public class TransactionalService { @OrderTx public void setSomething(String name) { // ... } @AccountTx public void doSomething() { // ... } } class TransactionalService { @OrderTx fun setSomething(name: String) { // ... } @AccountTx fun doSomething() { // ... } } In the preceding example, we used the syntax to define the transaction manager qualifier and transactional labels, but we could also have included propagation behavior, rollback rules, timeouts, and other features. <tx:advice/> Settings Transaction Propagation Spring Framework Stable 7.0.8 6.2.19 Snapshot 7.1.0-SNAPSHOT 7.0.9-SNAPSHOT 6.2.20-SNAPSHOT Related Spring Documentation Spring Boot Spring Framework Spring Cloud Spring Cloud Build Spring Cloud Bus Spring Cloud Circuit Breaker Spring Cloud Commons Spring Cloud Config Spring Cloud Consul Spring Cloud Contract Spring Cloud Function Spring Cloud Gateway Spring Cloud Kubernetes Spring Cloud Netflix Spring Cloud OpenFeign Spring Cloud Stream Spring Cloud Task Spring Cloud Vault Spring Cloud Zookeeper Spring Data Spring Data Cassandra Spring Data Commons Spring Data Couchbase Spring Data Elasticsearch Spring Data JPA Spring Data KeyValue Spring Data LDAP Spring Data MongoDB Spring Data Neo4j Spring Data Redis Spring Data JDBC & R2DBC Spring Data REST Spring Integration Spring Batch Spring Security Spring Authorization Server Spring LDAP Spring Security Kerberos Spring Session Spring Vault Spring AI Spring AMQP Spring CLI Spring GraphQL Spring for Apache Kafka Spring Modulith Spring for Apache Pulsar Spring Shell All Docs... Copyright © 2005 - Broadcom. All Rights Reserved. The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries. Terms of Use • Privacy • Trademark Guidelines • Thank you • Your California Privacy Rights • Cookie Settings Apache®, Apache Tomcat®, Apache Kafka®, Apache Cassandra™, and Apache Geode™ are trademarks or registered trademarks of the Apache Software Foundation in the United States and/or other countries. Java™, Java™ SE, Java™ EE, and OpenJDK™ are trademarks of Oracle and/or its affiliates. Kubernetes® is a registered trademark of the Linux Foundation in the United States and other countries. Linux® is the registered trademark of Linus Torvalds in the United States and other countries. Windows® and Microsoft® Azure are registered trademarks of Microsoft Corporation. “AWS” and “Amazon Web Services” are trademarks or registered trademarks of Amazon.com Inc. or its affiliates. All other trademarks and copyrights are property of their respective owners and are only mentioned for informative purposes. Other names may be trademarks of their respective owners. Search in all Spring Docs
