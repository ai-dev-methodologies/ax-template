# spring-beans-constructor-injection — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T02:24:26Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r131`
**Body SHA-256 (below the `---` divider, header excluded):** 3dc28709f8ec84009f7997b27175c1d191ec7fa5301cd56e7b6a4920b373735b

---

---
snapshot_id: spring-beans-constructor-injection
source: "https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 3
bytes: 18101
sha: "1fe7dcde843cbacf52e3d639335ea28878ab1e648d6c1f180e5d5073ad04c4ef"
---

# spring beans constructor injection — upstream snapshot

Source: https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html
Fetched: 2026-07-14

Dependency Injection :: Spring Framework
Edit this Page
 
 
 
 GitHub Project
 
 
 
 Stack Overflow

# Dependency Injection
Dependency injection (DI) is a process whereby objects define their dependencies
(that is, the other objects with which they work) only through constructor arguments,
arguments to a factory method, or properties that are set on the object instance after
it is constructed or returned from a factory method. The container then injects those
dependencies when it creates the bean. This process is fundamentally the inverse (hence
the name, Inversion of Control) of the bean itself controlling the instantiation
or location of its dependencies on its own by using direct construction of classes or
the Service Locator pattern.
Code is cleaner with the DI principle, and decoupling is more effective when objects are
provided with their dependencies. The object does not look up its dependencies and does
not know the location or class of the dependencies. As a result, your classes become easier
to test, particularly when the dependencies are on interfaces or abstract base classes,
which allow for stub or mock implementations to be used in unit tests.
DI exists in two major variants:
Constructor-based dependency injection
and Setter-based dependency injection.

## Constructor-based Dependency Injection
Constructor-based DI is accomplished by the container invoking a constructor with a
number of arguments, each representing a dependency. Calling a static factory method
with specific arguments to construct the bean is nearly equivalent, and this discussion
treats arguments to a constructor and to a static factory method similarly. The
following example shows a class that can only be dependency-injected with constructor
injection:
Java
Kotlin
public class SimpleMovieLister {

 // the SimpleMovieLister has a dependency on a MovieFinder
 private final MovieFinder movieFinder;

 // a constructor so that the Spring container can inject a MovieFinder
 public SimpleMovieLister(MovieFinder movieFinder) {
 this.movieFinder = movieFinder;
 }

 // business logic that actually uses the injected MovieFinder is omitted...
}
// a constructor so that the Spring container can inject a MovieFinder
class SimpleMovieLister(private val movieFinder: MovieFinder) {
 // business logic that actually uses the injected MovieFinder is omitted...
}
Notice that there is nothing special about this class. It is a POJO that
has no dependencies on container specific interfaces, base classes, or annotations.

### Constructor Argument Resolution
Constructor argument resolution matching occurs by using the argument’s type. If no
potential ambiguity exists in the constructor arguments of a bean definition, the
order in which the constructor arguments are defined in a bean definition is the order
in which those arguments are supplied to the appropriate constructor when the bean is
being instantiated. Consider the following class:
Java
Kotlin
package x.y;

public class ThingOne {

 public ThingOne(ThingTwo thingTwo, ThingThree thingThree) {
 // ...
 }
}
package x.y

class ThingOne(thingTwo: ThingTwo, thingThree: ThingThree)
Assuming that the ThingTwo and ThingThree classes are not related by inheritance, no
potential ambiguity exists. Thus, the following configuration works fine, and you do not
need to specify the constructor argument indexes or types explicitly in the
 element.
When another bean is referenced, the type is known, and matching can occur (as was the
case with the preceding example). When a simple type is used, such as
true, Spring cannot determine the type of the value, and so cannot match
by type without help. Consider the following class:
Java
Kotlin
package examples;

public class ExampleBean {

 // Number of years to calculate the Ultimate Answer
 private final int years;

 // The Answer to Life, the Universe, and Everything
 private final String ultimateAnswer;

 public ExampleBean(int years, String ultimateAnswer) {
 this.years = years;
 this.ultimateAnswer = ultimateAnswer;
 }
}
package examples

class ExampleBean(
 private val years: Int, // Number of years to calculate the Ultimate Answer
 private val ultimateAnswer: String // The Answer to Life, the Universe, and Everything
)

#### Constructor argument type matching
In the preceding scenario, the container can use type matching with simple types if
you explicitly specify the type of the constructor argument via the type attribute,
as the following example shows:

#### Constructor argument index
You can use the index attribute to specify explicitly the index of constructor arguments,
as the following example shows:
In addition to resolving the ambiguity of multiple simple values, specifying an index
resolves ambiguity where a constructor has two arguments of the same type.
The index is 0-based.

#### Constructor argument name
You can also use the constructor parameter name for value disambiguation, as the following
example shows:
Keep in mind that, to make this work out of the box, your code must be compiled with the
-parameters flag enabled so that Spring can look up the parameter name from the constructor.
If you cannot or do not want to compile your code with the -parameters flag, you can use the
@ConstructorProperties
JDK annotation to explicitly name your constructor arguments. The sample class would
then have to look as follows:
Java
Kotlin
package examples;

public class ExampleBean {

 // Fields omitted

 @ConstructorProperties({"years", "ultimateAnswer"})
 public ExampleBean(int years, String ultimateAnswer) {
 this.years = years;
 this.ultimateAnswer = ultimateAnswer;
 }
}
package examples

class ExampleBean
@ConstructorProperties("years", "ultimateAnswer")
constructor(val years: Int, val ultimateAnswer: String)

## Setter-based Dependency Injection
Setter-based DI is accomplished by the container calling setter methods on your
beans after invoking a no-argument constructor or a no-argument static factory method to
instantiate your bean.
The following example shows a class that can only be dependency-injected by using pure
setter injection. This class is conventional Java. It is a POJO that has no dependencies
on container specific interfaces, base classes, or annotations.
Java
Kotlin
public class SimpleMovieLister {

 // the SimpleMovieLister has a dependency on the MovieFinder
 private MovieFinder movieFinder;

 // a setter method so that the Spring container can inject a MovieFinder
 public void setMovieFinder(MovieFinder movieFinder) {
 this.movieFinder = movieFinder;
 }

 // business logic that actually uses the injected MovieFinder is omitted...
}
class SimpleMovieLister {

 // a late-initialized property so that the Spring container can inject a MovieFinder
 lateinit var movieFinder: MovieFinder

 // business logic that actually uses the injected MovieFinder is omitted...
}
The ApplicationContext supports constructor-based and setter-based DI for the beans it
manages. It also supports setter-based DI after some dependencies have already been
injected through the constructor approach. You configure the dependencies in the form of
a BeanDefinition, which you use in conjunction with PropertyEditor instances to
convert properties from one format to another. However, most Spring users do not work
with these classes directly (that is, programmatically) but rather with XML bean
definitions, annotated components (that is, classes annotated with @Component,
@Controller, and so forth), or @Bean methods in Java-based @Configuration classes.
These sources are then converted internally into instances of BeanDefinition and used to
load an entire Spring IoC container instance.
Constructor-based or setter-based DI?
Since you can mix constructor-based and setter-based DI, it is a good rule of thumb to
use constructors for mandatory dependencies and setter methods or configuration methods
for optional dependencies. Note that use of the @Autowired
annotation on a setter method can be used to make the property be a required dependency;
however, constructor injection with programmatic validation of arguments is preferable.
The Spring team generally advocates constructor injection, as it lets you implement
application components as immutable objects and ensures that required dependencies
are not null. Furthermore, constructor-injected components are always returned to the client
(calling) code in a fully initialized state. As a side note, a large number of constructor
arguments is a bad code smell, implying that the class likely has too many
responsibilities and should be refactored to better address proper separation of concerns.
Setter injection should primarily only be used for optional dependencies that can be
assigned reasonable default values within the class. Otherwise, not-null checks must be
performed everywhere the code uses the dependency. One benefit of setter injection is that
setter methods make objects of that class amenable to reconfiguration or re-injection
later. Management through JMX MBeans is therefore a compelling
use case for setter injection.
Use the DI style that makes the most sense for a particular class. Sometimes, when dealing
with third-party classes for which you do not have the source, the choice is made for you.
For example, if a third-party class does not expose any setter methods, then constructor
injection may be the only available form of DI.

## Dependency Resolution Process
The container performs bean dependency resolution as follows:
The ApplicationContext is created and initialized with configuration metadata that
describes all the beans. Configuration metadata can be specified by XML, Java code, or
annotations.
For each bean, its dependencies are expressed in the form of properties, constructor
arguments, or arguments to the static-factory method (if you use that instead of a
normal constructor). These dependencies are provided to the bean, when the bean is
actually created.
Each property or constructor argument is an actual definition of the value to set, or
a reference to another bean in the container.
Each property or constructor argument that is a value is converted from its specified
format to the actual type of that property or constructor argument. By default, Spring
can convert a value supplied in string format to all built-in types, such as int,
long, String, boolean, and so forth.
The Spring container validates the configuration of each bean as the container is created.
However, the bean properties themselves are not set until the bean is actually created.
Beans that are singleton-scoped and set to be pre-instantiated (the default) are created
when the container is created. Scopes are defined in Bean Scopes. Otherwise,
the bean is created only when it is requested. Creation of a bean potentially causes a
graph of beans to be created, as the bean’s dependencies and its dependencies'
dependencies (and so on) are created and assigned. Note that resolution mismatches among
those dependencies may show up late — that is, on first creation of the affected bean.
Circular dependencies
If you use predominantly constructor injection, it is possible to create an unresolvable
circular dependency scenario.
For example: Class A requires an instance of class B through constructor injection, and
class B requires an instance of class A through constructor injection. If you configure
beans for classes A and B to be injected into each other, the Spring IoC container
detects this circular reference at runtime, and throws a
BeanCurrentlyInCreationException.
One possible solution is to edit the source code of some classes to be configured by
setters rather than constructors. Alternatively, avoid constructor injection and use
setter injection only. In other words, although it is not recommended, you can configure
circular dependencies with setter injection.
Unlike the typical case (with no circular dependencies), a circular dependency
between bean A and bean B forces one of the beans to be injected into the other prior to
being fully initialized itself (a classic chicken-and-egg scenario).
You can generally trust Spring to do the right thing. It detects configuration problems,
such as references to non-existent beans and circular dependencies, at container
load-time. Spring sets properties and resolves dependencies as late as possible, when
the bean is actually created. This means that a Spring container that has loaded
correctly can later generate an exception when you request an object if there is a
problem creating that object or one of its dependencies — for example, the bean throws an
exception as a result of a missing or invalid property. This potentially delayed
visibility of some configuration issues is why ApplicationContext implementations by
default pre-instantiate singleton beans. At the cost of some upfront time and memory to
create these beans before they are actually needed, you discover configuration issues
when the ApplicationContext is created, not later. You can still override this default
behavior so that singleton beans initialize lazily, rather than being eagerly
pre-instantiated.
If no circular dependencies exist, when one or more collaborating beans are being
injected into a dependent bean, each collaborating bean is totally configured prior
to being injected into the dependent bean. This means that, if bean A has a dependency on
bean B, the Spring IoC container completely configures bean B prior to invoking the
setter method on bean A. In other words, the bean is instantiated (if it is not a
pre-instantiated singleton), its dependencies are set, and the relevant lifecycle
methods (such as a configured init method
or the InitializingBean callback method)
are invoked.

## Examples of Dependency Injection
The following example uses XML-based configuration metadata for setter-based DI. A small
part of a Spring XML configuration file specifies some bean definitions as follows:
The following example shows the corresponding ExampleBean class:
Java
Kotlin
public class ExampleBean {

 private AnotherBean beanOne;

 private YetAnotherBean beanTwo;

 private int i;

 public void setBeanOne(AnotherBean beanOne) {
 this.beanOne = beanOne;
 }

 public void setBeanTwo(YetAnotherBean beanTwo) {
 this.beanTwo = beanTwo;
 }

 public void setIntegerProperty(int i) {
 this.i = i;
 }
}
class ExampleBean {
 lateinit var beanOne: AnotherBean
 lateinit var beanTwo: YetAnotherBean
 var i: Int = 0
}
In the preceding example, setters are declared to match against the properties specified
in the XML file. The following example uses constructor-based DI:
The following example shows the corresponding ExampleBean class:
Java
Kotlin
public class ExampleBean {

 private AnotherBean beanOne;

 private YetAnotherBean beanTwo;

 private int i;

 public ExampleBean(
 AnotherBean anotherBean, YetAnotherBean yetAnotherBean, int i) {
 this.beanOne = anotherBean;
 this.beanTwo = yetAnotherBean;
 this.i = i;
 }
}
class ExampleBean(
 private val beanOne: AnotherBean,
 private val beanTwo: YetAnotherBean,
 private val i: Int)
The constructor arguments specified in the bean definition are used as arguments to
the constructor of the ExampleBean.
Now consider a variant of this example, where, instead of using a constructor, Spring is
told to call a static factory method to return an instance of the object:
The following example shows the corresponding ExampleBean class:
Java
Kotlin
public class ExampleBean {

 // a private constructor
 private ExampleBean(...) {
 ...
 }

 // a static factory method; the arguments to this method can be
 // considered the dependencies of the bean that is returned,
 // regardless of how those arguments are actually used.
 public static ExampleBean createInstance (
 AnotherBean anotherBean, YetAnotherBean yetAnotherBean, int i) {

 ExampleBean eb = new ExampleBean (...);
 // some other operations...
 return eb;
 }
}
class ExampleBean private constructor() {
 companion object {
 // a static factory method; the arguments to this method can be
 // considered the dependencies of the bean that is returned,
 // regardless of how those arguments are actually used.
 @JvmStatic
 fun createInstance(anotherBean: AnotherBean, yetAnotherBean: YetAnotherBean, i: Int): ExampleBean {
 val eb = ExampleBean (...)
 // some other operations...
 return eb
 }
 }
}
Arguments to the static factory method are supplied by elements,
exactly the same as if a constructor had actually been used. The type of the class being
returned by the factory method does not have to be of the same type as the class that
contains the static factory method (although, in this example, it is). An instance
(non-static) factory method can be used in an essentially identical fashion (aside
from the use of the factory-bean attribute instead of the class attribute), so we
do not discuss those details here.
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

Source: https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html
HTTP status: 200 · extracted bytes: 31843 · sha256: 278a29d06c98cd9692cdb50c514133c2725f6cd3504853fddc0d80ddd526f77b
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r131`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

Dependency Injection :: Spring Framework Why Spring Overview Trending Generative AI Cloud Architecture Patterns Microservices Reactive Event Driven Application Types Web Applications Serverless Batch Learn Getting Started Quickstart Guides Academy Courses Get Certified Projects Overview Projects Spring Boot Spring Framework Spring Cloud Spring AI Spring Data Spring Integration Spring Batch Spring Security Foundational Projects Micrometer Reactor Development Tools Spring Tools Spring Initializr Resources Blog Release Calendar Version Mappings Release Highlights Security Advisories GitHub Orgs Spring Projects Spring Cloud Community Overview Events Authors Enterprise Overview Long-term Support Automated Upgrades Governance and Compliance Modern App Development light Spring Framework 7.0.8 Search Overview Core Technologies The IoC Container Introduction to the Spring IoC Container and Beans Container Overview Bean Overview Dependencies Dependency Injection Dependencies and Configuration in Detail Using depends-on Lazy-initialized Beans Autowiring Collaborators Method Injection Bean Scopes Customizing the Nature of a Bean Bean Definition Inheritance Container Extension Points Annotation-based Container Configuration Using @Autowired Fine-tuning Annotation-based Autowiring with @Primary or @Fallback Fine-tuning Annotation-based Autowiring with Qualifiers Using Generics as Autowiring Qualifiers Using CustomAutowireConfigurer Injection with @Resource Using @Value Using @PostConstruct and @PreDestroy Classpath Scanning and Managed Components Using JSR-330 Standard Annotations Java-based Container Configuration Basic Concepts: @Bean and @Configuration Instantiating the Spring Container by Using AnnotationConfigApplicationContext Using the @Bean Annotation Using the @Configuration annotation Composing Java-based Configurations Programmatic Bean Registration Environment Abstraction Registering a LoadTimeWeaver Additional Capabilities of the ApplicationContext The BeanFactory API Resources Validation, Data Binding, and Type Conversion Validation Using Spring’s Validator Interface Data Binding Resolving Error Codes to Error Messages Spring Type Conversion Spring Field Formatting Configuring a Global Date and Time Format Java Bean Validation Spring Expression Language (SpEL) Evaluation Expressions in Bean Definitions Language Reference Literal Expressions Properties, Arrays, Lists, Maps, and Indexers Inline Lists Inline Maps Array Construction Methods Operators Types Constructors Variables Functions Varargs Invocations Bean References Ternary Operator (If-Then-Else) The Elvis Operator Safe Navigation Operator Collection Selection Collection Projection Expression Templating Classes Used in the Examples Aspect Oriented Programming with Spring AOP Concepts Spring AOP Capabilities and Goals AOP Proxies @AspectJ support Enabling @AspectJ Support Declaring an Aspect Declaring a Pointcut Declaring Advice Introductions Aspect Instantiation Models An AOP Example Schema-based AOP Support Choosing which AOP Declaration Style to Use Mixing Aspect Types Proxying Mechanisms Programmatic Creation of @AspectJ Proxies Using AspectJ with Spring Applications Further Resources Spring AOP APIs Pointcut API in Spring Advice API in Spring The Advisor API in Spring Using the ProxyFactoryBean to Create AOP Proxies Concise Proxy Definitions Creating AOP Proxies Programmatically with the ProxyFactory Manipulating Advised Objects Using the "auto-proxy" facility Using TargetSource Implementations Defining New Advice Types Resilience Features Null-safety Data Buffers and Codecs Ahead of Time Optimizations Appendix XML Schemas XML Schema Authoring Application Startup Steps Data Access Transaction Management Advantages of the Spring Framework’s Transaction Support Model Understanding the Spring Framework Transaction Abstraction Synchronizing Resources with Transactions Declarative Transaction Management Understanding the Spring Framework’s Declarative Transaction Implementation Example of Declarative Transaction Implementation Rolling Back a Declarative Transaction Configuring Different Transactional Semantics for Different Beans <tx:advice/> Settings Using @Transactional Transaction Propagation Advising Transactional Operations Using @Transactional with AspectJ Programmatic Transaction Management Choosing Between Programmatic and Declarative Transaction Management Transaction-bound Events Application server-specific integration Solutions to Common Problems Further Resources DAO Support Data Access with JDBC Choosing an Approach for JDBC Database Access Package Hierarchy Using the JDBC Core Classes to Control Basic JDBC Processing and Error Handling Controlling Database Connections JDBC Batch Operations Simplifying JDBC Operations with the SimpleJdbc Classes Modeling JDBC Operations as Java Objects Common Problems with Parameter and Data Value Handling Embedded Database Support Initializing a DataSource Data Access with R2DBC Object Relational Mapping (ORM) Data Access Introduction to ORM with Spring General ORM Integration Considerations Hibernate JPA Marshalling XML by Using Object-XML Mappers Appendix Web on Servlet Stack Spring Web MVC DispatcherServlet Context Hierarchy Special Bean Types Web MVC Config Servlet Config Processing Path Matching Interception Exceptions View Resolution Locale Multipart Resolver Logging Filters HTTP Message Conversion Annotated Controllers Declaration Mapping Requests Handler Methods Method Arguments Return Values Type Conversion Matrix Variables @RequestParam @RequestHeader @CookieValue @ModelAttribute @SessionAttributes @SessionAttribute @RequestAttribute Redirect Attributes Flash Attributes Multipart @RequestBody HttpEntity @ResponseBody ResponseEntity Jackson JSON Model @InitBinder Validation Exceptions Controller Advice Functional Endpoints URI Links Asynchronous Requests Range Requests Data Binding CORS API Versioning Error Responses Web Security HTTP Caching View Technologies Thymeleaf FreeMarker Groovy Markup Script Views HTML Fragments JSP and JSTL RSS and Atom PDF and Excel Jackson XML Marshalling XSLT Views MVC Config Enable MVC Configuration MVC Config API Type Conversion Validation Interceptors Content Types Message Converters View Controllers View Resolvers Static Resources Default Servlet Path Matching API Version Advanced Java Config Advanced XML Config HTTP/2 REST Clients Testing WebSockets WebSocket API SockJS Fallback STOMP Overview Benefits Enable STOMP WebSocket Transport Flow of Messages Annotated Controllers Sending Messages Simple Broker External Broker Connecting to a Broker Dots as Separators Authentication Token Authentication Authorization User Destinations Order of Messages Events Interception STOMP Client WebSocket Scope Performance Monitoring Testing Web on Reactive Stack Spring WebFlux Overview Reactive Core DispatcherHandler Annotated Controllers @Controller Mapping Requests Handler Methods Method Arguments Return Values Type Conversion Matrix Variables @RequestParam @RequestHeader @CookieValue @ModelAttribute @SessionAttributes @SessionAttribute @RequestAttribute Multipart Content @RequestBody HttpEntity @ResponseBody ResponseEntity Jackson JSON Model DataBinder Validation Exceptions Controller Advice Functional Endpoints URI Links Range Requests Data Binding CORS API Versioning Error Responses Web Security HTTP Caching View Technologies WebFlux Config HTTP/2 WebClient Configuration retrieve() Exchange Request Body Filters Attributes Context Synchronous Use Testing HTTP Service Client WebSockets Testing RSocket Reactive Libraries Testing Introduction to Spring Testing Unit Testing Integration Testing JDBC Testing Support Spring TestContext Framework Key Abstractions Bootstrapping the TestContext Framework TestExecutionListener Configuration Application Events Test Execution Events Context Management Context Configuration with Component Classes Context Configuration with XML Resources Context Configuration with Groovy Scripts Default Context Configuration Mixing Component Classes, XML, and Groovy Scripts Context Configuration with Context Customizers Context Configuration with Context Initializers Context Configuration Inheritance Context Configuration with Environment Profiles Context Configuration with Test Property Sources Context Configuration with Dynamic Property Sources Loading a WebApplicationContext Working with Web Mocks Context Caching Context Pausing Context Failure Threshold Context Hierarchies Dependency Injection of Test Fixtures Bean Overriding in Tests Testing Request- and Session-scoped Beans Transaction Management Executing SQL Scripts Parallel Test Execution TestContext Framework Support Classes Ahead of Time Support for Tests WebTestClient RestTestClient MockMvc Overview Setup Options Hamcrest Integration Static Imports Configuring MockMvc Setup Features Performing Requests Defining Expectations Async Requests Streaming Responses Filter Registrations AssertJ Integration Configuring MockMvcTester Performing Requests Defining Expectations MockMvc integration HtmlUnit Integration Why HtmlUnit Integration? MockMvc and HtmlUnit MockMvc and WebDriver MockMvc and Geb MockMvc vs End-to-End Tests Further Examples Testing Client Applications Appendix Annotations Standard Annotation Support Spring Testing Annotations @BootstrapWith @ContextConfiguration @WebAppConfiguration @ContextHierarchy @ContextCustomizerFactories @ActiveProfiles @TestPropertySource @DynamicPropertySource @TestBean @MockitoBean and @MockitoSpyBean @DirtiesContext @TestExecutionListeners @RecordApplicationEvents @Commit @Rollback @BeforeTransaction @AfterTransaction @Sql @SqlConfig @SqlMergeMode @SqlGroup @DisabledInAotMode Spring JUnit 4 Testing Annotations Spring JUnit Jupiter Testing Annotations Meta-Annotation Support for Testing Further Resources Integration REST Clients JMS (Java Message Service) Using Spring JMS Sending a Message Receiving a Message Support for JCA Message Endpoints Annotation-driven Listener Endpoints JMS Namespace Support JMX Exporting Your Beans to JMX Controlling the Management Interface of Your Beans Controlling ObjectName Instances for Your Beans Using JSR-160 Connectors Accessing MBeans through Proxies Notifications Further Resources Email Task Execution and Scheduling Cache Abstraction Understanding the Cache Abstraction Declarative Annotation-based Caching JCache (JSR-107) Annotations Declarative XML-based Caching Configuring the Cache Storage Plugging-in Different Back-end Caches How can I Set the TTL/TTI/Eviction policy/XXX feature? Observability Support JVM AOT Cache JVM Checkpoint Restore Appendix Language Support Kotlin Requirements Extensions Null-safety Classes and Interfaces Annotations Bean Registration DSL Web Coroutines Spring Projects in Kotlin Getting Started Resources Apache Groovy Appendix Java API Kotlin API Wiki Search Edit this Page GitHub Project Stack Overflow Spring Framework Core Technologies The IoC Container Dependencies Dependency Injection Dependency Injection Dependency injection (DI) is a process whereby objects define their dependencies (that is, the other objects with which they work) only through constructor arguments, arguments to a factory method, or properties that are set on the object instance after it is constructed or returned from a factory method. The container then injects those dependencies when it creates the bean. This process is fundamentally the inverse (hence the name, Inversion of Control) of the bean itself controlling the instantiation or location of its dependencies on its own by using direct construction of classes or the Service Locator pattern. Code is cleaner with the DI principle, and decoupling is more effective when objects are provided with their dependencies. The object does not look up its dependencies and does not know the location or class of the dependencies. As a result, your classes become easier to test, particularly when the dependencies are on interfaces or abstract base classes, which allow for stub or mock implementations to be used in unit tests. DI exists in two major variants: Constructor-based dependency injection and Setter-based dependency injection . Constructor-based Dependency Injection Constructor-based DI is accomplished by the container invoking a constructor with a number of arguments, each representing a dependency. Calling a static factory method with specific arguments to construct the bean is nearly equivalent, and this discussion treats arguments to a constructor and to a static factory method similarly. The following example shows a class that can only be dependency-injected with constructor injection: Java Kotlin public class SimpleMovieLister { // the SimpleMovieLister has a dependency on a MovieFinder private final MovieFinder movieFinder; // a constructor so that the Spring container can inject a MovieFinder public SimpleMovieLister(MovieFinder movieFinder) { this.movieFinder = movieFinder; } // business logic that actually uses the injected MovieFinder is omitted... } // a constructor so that the Spring container can inject a MovieFinder class SimpleMovieLister(private val movieFinder: MovieFinder) { // business logic that actually uses the injected MovieFinder is omitted... } Notice that there is nothing special about this class. It is a POJO that has no dependencies on container specific interfaces, base classes, or annotations. Constructor Argument Resolution Constructor argument resolution matching occurs by using the argument’s type. If no potential ambiguity exists in the constructor arguments of a bean definition, the order in which the constructor arguments are defined in a bean definition is the order in which those arguments are supplied to the appropriate constructor when the bean is being instantiated. Consider the following class: Java Kotlin package x.y; public class ThingOne { public ThingOne(ThingTwo thingTwo, ThingThree thingThree) { // ... } } package x.y class ThingOne(thingTwo: ThingTwo, thingThree: ThingThree) Assuming that the ThingTwo and ThingThree classes are not related by inheritance, no potential ambiguity exists. Thus, the following configuration works fine, and you do not need to specify the constructor argument indexes or types explicitly in the <constructor-arg/> element. <beans> <bean id="beanOne" class="x.y.ThingOne"> <constructor-arg ref="beanTwo"/> <constructor-arg ref="beanThree"/> </bean> <bean id="beanTwo" class="x.y.ThingTwo"/> <bean id="beanThree" class="x.y.ThingThree"/> </beans> When another bean is referenced, the type is known, and matching can occur (as was the case with the preceding example). When a simple type is used, such as <value>true</value> , Spring cannot determine the type of the value, and so cannot match by type without help. Consider the following class: Java Kotlin package examples; public class ExampleBean { // Number of years to calculate the Ultimate Answer private final int years; // The Answer to Life, the Universe, and Everything private final String ultimateAnswer; public ExampleBean(int years, String ultimateAnswer) { this.years = years; this.ultimateAnswer = ultimateAnswer; } } package examples class ExampleBean( private val years: Int, // Number of years to calculate the Ultimate Answer private val ultimateAnswer: String // The Answer to Life, the Universe, and Everything ) Constructor argument type matching In the preceding scenario, the container can use type matching with simple types if you explicitly specify the type of the constructor argument via the type attribute, as the following example shows: <bean id="exampleBean" class="examples.ExampleBean"> <constructor-arg type="int" value="7500000"/> <constructor-arg type="java.lang.String" value="42"/> </bean> Constructor argument index You can use the index attribute to specify explicitly the index of constructor arguments, as the following example shows: <bean id="exampleBean" class="examples.ExampleBean"> <constructor-arg index="0" value="7500000"/> <constructor-arg index="1" value="42"/> </bean> In addition to resolving the ambiguity of multiple simple values, specifying an index resolves ambiguity where a constructor has two arguments of the same type. The index is 0-based. Constructor argument name You can also use the constructor parameter name for value disambiguation, as the following example shows: <bean id="exampleBean" class="examples.ExampleBean"> <constructor-arg name="years" value="7500000"/> <constructor-arg name="ultimateAnswer" value="42"/> </bean> Keep in mind that, to make this work out of the box, your code must be compiled with the -parameters flag enabled so that Spring can look up the parameter name from the constructor. If you cannot or do not want to compile your code with the -parameters flag, you can use the @ConstructorProperties JDK annotation to explicitly name your constructor arguments. The sample class would then have to look as follows: Java Kotlin package examples; public class ExampleBean { // Fields omitted @ConstructorProperties({"years", "ultimateAnswer"}) public ExampleBean(int years, String ultimateAnswer) { this.years = years; this.ultimateAnswer = ultimateAnswer; } } package examples class ExampleBean @ConstructorProperties("years", "ultimateAnswer") constructor(val years: Int, val ultimateAnswer: String) Setter-based Dependency Injection Setter-based DI is accomplished by the container calling setter methods on your beans after invoking a no-argument constructor or a no-argument static factory method to instantiate your bean. The following example shows a class that can only be dependency-injected by using pure setter injection. This class is conventional Java. It is a POJO that has no dependencies on container specific interfaces, base classes, or annotations. Java Kotlin public class SimpleMovieLister { // the SimpleMovieLister has a dependency on the MovieFinder private MovieFinder movieFinder; // a setter method so that the Spring container can inject a MovieFinder public void setMovieFinder(MovieFinder movieFinder) { this.movieFinder = movieFinder; } // business logic that actually uses the injected MovieFinder is omitted... } class SimpleMovieLister { // a late-initialized property so that the Spring container can inject a MovieFinder lateinit var movieFinder: MovieFinder // business logic that actually uses the injected MovieFinder is omitted... } The ApplicationContext supports constructor-based and setter-based DI for the beans it manages. It also supports setter-based DI after some dependencies have already been injected through the constructor approach. You configure the dependencies in the form of a BeanDefinition , which you use in conjunction with PropertyEditor instances to convert properties from one format to another. However, most Spring users do not work with these classes directly (that is, programmatically) but rather with XML bean definitions, annotated components (that is, classes annotated with @Component , @Controller , and so forth), or @Bean methods in Java-based @Configuration classes. These sources are then converted internally into instances of BeanDefinition and used to load an entire Spring IoC container instance. Constructor-based or setter-based DI? Since you can mix constructor-based and setter-based DI, it is a good rule of thumb to use constructors for mandatory dependencies and setter methods or configuration methods for optional dependencies. Note that use of the @Autowired annotation on a setter method can be used to make the property be a required dependency; however, constructor injection with programmatic validation of arguments is preferable. The Spring team generally advocates constructor injection, as it lets you implement application components as immutable objects and ensures that required dependencies are not null . Furthermore, constructor-injected components are always returned to the client (calling) code in a fully initialized state. As a side note, a large number of constructor arguments is a bad code smell, implying that the class likely has too many responsibilities and should be refactored to better address proper separation of concerns. Setter injection should primarily only be used for optional dependencies that can be assigned reasonable default values within the class. Otherwise, not-null checks must be performed everywhere the code uses the dependency. One benefit of setter injection is that setter methods make objects of that class amenable to reconfiguration or re-injection later. Management through JMX MBeans is therefore a compelling use case for setter injection. Use the DI style that makes the most sense for a particular class. Sometimes, when dealing with third-party classes for which you do not have the source, the choice is made for you. For example, if a third-party class does not expose any setter methods, then constructor injection may be the only available form of DI. Dependency Resolution Process The container performs bean dependency resolution as follows: The ApplicationContext is created and initialized with configuration metadata that describes all the beans. Configuration metadata can be specified by XML, Java code, or annotations. For each bean, its dependencies are expressed in the form of properties, constructor arguments, or arguments to the static-factory method (if you use that instead of a normal constructor). These dependencies are provided to the bean, when the bean is actually created. Each property or constructor argument is an actual definition of the value to set, or a reference to another bean in the container. Each property or constructor argument that is a value is converted from its specified format to the actual type of that property or constructor argument. By default, Spring can convert a value supplied in string format to all built-in types, such as int , long , String , boolean , and so forth. The Spring container validates the configuration of each bean as the container is created. However, the bean properties themselves are not set until the bean is actually created. Beans that are singleton-scoped and set to be pre-instantiated (the default) are created when the container is created. Scopes are defined in Bean Scopes . Otherwise, the bean is created only when it is requested. Creation of a bean potentially causes a graph of beans to be created, as the bean’s dependencies and its dependencies' dependencies (and so on) are created and assigned. Note that resolution mismatches among those dependencies may show up late — that is, on first creation of the affected bean. Circular dependencies If you use predominantly constructor injection, it is possible to create an unresolvable circular dependency scenario. For example: Class A requires an instance of class B through constructor injection, and class B requires an instance of class A through constructor injection. If you configure beans for classes A and B to be injected into each other, the Spring IoC container detects this circular reference at runtime, and throws a BeanCurrentlyInCreationException . One possible solution is to edit the source code of some classes to be configured by setters rather than constructors. Alternatively, avoid constructor injection and use setter injection only. In other words, although it is not recommended, you can configure circular dependencies with setter injection. Unlike the typical case (with no circular dependencies), a circular dependency between bean A and bean B forces one of the beans to be injected into the other prior to being fully initialized itself (a classic chicken-and-egg scenario). You can generally trust Spring to do the right thing. It detects configuration problems, such as references to non-existent beans and circular dependencies, at container load-time. Spring sets properties and resolves dependencies as late as possible, when the bean is actually created. This means that a Spring container that has loaded correctly can later generate an exception when you request an object if there is a problem creating that object or one of its dependencies — for example, the bean throws an exception as a result of a missing or invalid property. This potentially delayed visibility of some configuration issues is why ApplicationContext implementations by default pre-instantiate singleton beans. At the cost of some upfront time and memory to create these beans before they are actually needed, you discover configuration issues when the ApplicationContext is created, not later. You can still override this default behavior so that singleton beans initialize lazily, rather than being eagerly pre-instantiated. If no circular dependencies exist, when one or more collaborating beans are being injected into a dependent bean, each collaborating bean is totally configured prior to being injected into the dependent bean. This means that, if bean A has a dependency on bean B, the Spring IoC container completely configures bean B prior to invoking the setter method on bean A. In other words, the bean is instantiated (if it is not a pre-instantiated singleton), its dependencies are set, and the relevant lifecycle methods (such as a configured init method or the InitializingBean callback method ) are invoked. Examples of Dependency Injection The following example uses XML-based configuration metadata for setter-based DI. A small part of a Spring XML configuration file specifies some bean definitions as follows: <bean id="exampleBean" class="examples.ExampleBean"> <!-- setter injection using the nested ref element --> <property name="beanOne"> <ref bean="anotherExampleBean"/> </property> <!-- setter injection using the neater ref attribute --> <property name="beanTwo" ref="yetAnotherBean"/> <property name="integerProperty" value="1"/> </bean> <bean id="anotherExampleBean" class="examples.AnotherBean"/> <bean id="yetAnotherBean" class="examples.YetAnotherBean"/> The following example shows the corresponding ExampleBean class: Java Kotlin public class ExampleBean { private AnotherBean beanOne; private YetAnotherBean beanTwo; private int i; public void setBeanOne(AnotherBean beanOne) { this.beanOne = beanOne; } public void setBeanTwo(YetAnotherBean beanTwo) { this.beanTwo = beanTwo; } public void setIntegerProperty(int i) { this.i = i; } } class ExampleBean { lateinit var beanOne: AnotherBean lateinit var beanTwo: YetAnotherBean var i: Int = 0 } In the preceding example, setters are declared to match against the properties specified in the XML file. The following example uses constructor-based DI: <bean id="exampleBean" class="examples.ExampleBean"> <!-- constructor injection using the nested ref element --> <constructor-arg> <ref bean="anotherExampleBean"/> </constructor-arg> <!-- constructor injection using the neater ref attribute --> <constructor-arg ref="yetAnotherBean"/> <constructor-arg type="int" value="1"/> </bean> <bean id="anotherExampleBean" class="examples.AnotherBean"/> <bean id="yetAnotherBean" class="examples.YetAnotherBean"/> The following example shows the corresponding ExampleBean class: Java Kotlin public class ExampleBean { private AnotherBean beanOne; private YetAnotherBean beanTwo; private int i; public ExampleBean( AnotherBean anotherBean, YetAnotherBean yetAnotherBean, int i) { this.beanOne = anotherBean; this.beanTwo = yetAnotherBean; this.i = i; } } class ExampleBean( private val beanOne: AnotherBean, private val beanTwo: YetAnotherBean, private val i: Int) The constructor arguments specified in the bean definition are used as arguments to the constructor of the ExampleBean . Now consider a variant of this example, where, instead of using a constructor, Spring is told to call a static factory method to return an instance of the object: <bean id="exampleBean" class="examples.ExampleBean" factory-method="createInstance"> <constructor-arg ref="anotherExampleBean"/> <constructor-arg ref="yetAnotherBean"/> <constructor-arg value="1"/> </bean> <bean id="anotherExampleBean" class="examples.AnotherBean"/> <bean id="yetAnotherBean" class="examples.YetAnotherBean"/> The following example shows the corresponding ExampleBean class: Java Kotlin public class ExampleBean { // a private constructor private ExampleBean(...) { ... } // a static factory method; the arguments to this method can be // considered the dependencies of the bean that is returned, // regardless of how those arguments are actually used. public static ExampleBean createInstance ( AnotherBean anotherBean, YetAnotherBean yetAnotherBean, int i) { ExampleBean eb = new ExampleBean (...); // some other operations... return eb; } } class ExampleBean private constructor() { companion object { // a static factory method; the arguments to this method can be // considered the dependencies of the bean that is returned, // regardless of how those arguments are actually used. @JvmStatic fun createInstance(anotherBean: AnotherBean, yetAnotherBean: YetAnotherBean, i: Int): ExampleBean { val eb = ExampleBean (...) // some other operations... return eb } } } Arguments to the static factory method are supplied by <constructor-arg/> elements, exactly the same as if a constructor had actually been used. The type of the class being returned by the factory method does not have to be of the same type as the class that contains the static factory method (although, in this example, it is). An instance (non-static) factory method can be used in an essentially identical fashion (aside from the use of the factory-bean attribute instead of the class attribute), so we do not discuss those details here. Dependencies Dependencies and Configuration in Detail Spring Framework Stable 7.0.8 6.2.19 Snapshot 7.1.0-SNAPSHOT 7.0.9-SNAPSHOT 6.2.20-SNAPSHOT Related Spring Documentation Spring Boot Spring Framework Spring Cloud Spring Cloud Build Spring Cloud Bus Spring Cloud Circuit Breaker Spring Cloud Commons Spring Cloud Config Spring Cloud Consul Spring Cloud Contract Spring Cloud Function Spring Cloud Gateway Spring Cloud Kubernetes Spring Cloud Netflix Spring Cloud OpenFeign Spring Cloud Stream Spring Cloud Task Spring Cloud Vault Spring Cloud Zookeeper Spring Data Spring Data Cassandra Spring Data Commons Spring Data Couchbase Spring Data Elasticsearch Spring Data JPA Spring Data KeyValue Spring Data LDAP Spring Data MongoDB Spring Data Neo4j Spring Data Redis Spring Data JDBC & R2DBC Spring Data REST Spring Integration Spring Batch Spring Security Spring Authorization Server Spring LDAP Spring Security Kerberos Spring Session Spring Vault Spring AI Spring AMQP Spring CLI Spring GraphQL Spring for Apache Kafka Spring Modulith Spring for Apache Pulsar Spring Shell All Docs... Copyright © 2005 - Broadcom. All Rights Reserved. The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries. Terms of Use • Privacy • Trademark Guidelines • Thank you • Your California Privacy Rights • Cookie Settings Apache®, Apache Tomcat®, Apache Kafka®, Apache Cassandra™, and Apache Geode™ are trademarks or registered trademarks of the Apache Software Foundation in the United States and/or other countries. Java™, Java™ SE, Java™ EE, and OpenJDK™ are trademarks of Oracle and/or its affiliates. Kubernetes® is a registered trademark of the Linux Foundation in the United States and other countries. Linux® is the registered trademark of Linus Torvalds in the United States and other countries. Windows® and Microsoft® Azure are registered trademarks of Microsoft Corporation. “AWS” and “Amazon Web Services” are trademarks or registered trademarks of Amazon.com Inc. or its affiliates. All other trademarks and copyrights are property of their respective owners and are only mentioned for informative purposes. Other names may be trademarks of their respective owners. Search in all Spring Docs
