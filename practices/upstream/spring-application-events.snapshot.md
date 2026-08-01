# spring-application-events — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T02:24:26Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r130`
**Body SHA-256 (below the `---` divider, header excluded):** 438296871c5d0e0c005defe72e84bbdf110f678c4a19333237fb0119d5d6d811

---

---
snapshot_id: spring-application-events
source: "https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 3
bytes: 34847
sha: "6b468e92c457e2c096f5421800af3896a6ff2271353de20b592acbaab85d221b"
---

# spring application events — upstream snapshot

Source: https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html
Fetched: 2026-07-14

Additional Capabilities of the ApplicationContext :: Spring Framework
Edit this Page
 
 
 
 GitHub Project
 
 
 
 Stack Overflow

# Additional Capabilities of the ApplicationContext
As discussed in the chapter introduction, the org.springframework.beans.factory
package provides basic functionality for managing and manipulating beans, including in a
programmatic way. The org.springframework.context package adds the
ApplicationContext
interface, which extends the BeanFactory interface, in addition to extending other
interfaces to provide additional functionality in a more application
framework-oriented style. Many people use the ApplicationContext in a completely
declarative fashion, not even creating it programmatically, but instead relying on
support classes such as ContextLoader to automatically instantiate an
ApplicationContext as part of the normal startup process of a Jakarta EE web application.
To enhance BeanFactory functionality in a more framework-oriented style, the context
package also provides the following functionality:
Access to messages in i18n-style, through the MessageSource interface.
Access to resources, such as URLs and files, through the ResourceLoader interface.
Event publication, namely to beans that implement the ApplicationListener interface,
through the use of the ApplicationEventPublisher interface.
Loading of multiple (hierarchical) contexts, letting each be focused on one
particular layer, such as the web layer of an application, through the
HierarchicalBeanFactory interface.

## Internationalization using MessageSource
The ApplicationContext interface extends an interface called MessageSource and,
therefore, provides internationalization (“i18n”) functionality. Spring also provides the
HierarchicalMessageSource interface, which can resolve messages hierarchically.
Together, these interfaces provide the foundation upon which Spring effects message
resolution. The methods defined on these interfaces include:
String getMessage(String code, Object[] args, String default, Locale loc): The basic
method used to retrieve a message from the MessageSource. When no message is found
for the specified locale, the default message is used. Any arguments passed in become
replacement values, using the MessageFormat functionality provided by the standard
library.
String getMessage(String code, Object[] args, Locale loc): Essentially the same as
the previous method but with one difference: No default message can be specified. If
the message cannot be found, a NoSuchMessageException is thrown.
String getMessage(MessageSourceResolvable resolvable, Locale locale): All properties
used in the preceding methods are also wrapped in a class named
MessageSourceResolvable, which you can use with this method.
When an ApplicationContext is loaded, it automatically searches for a MessageSource
bean defined in the context. The bean must have the name messageSource. If such a bean
is found, all calls to the preceding methods are delegated to the message source. If no
message source is found, the ApplicationContext attempts to find a parent containing a
bean with the same name. If it does, it uses that bean as the MessageSource. If the
ApplicationContext cannot find any source for messages, an empty
DelegatingMessageSource is instantiated in order to be able to accept calls to the
methods defined above.
Spring provides three MessageSource implementations, ResourceBundleMessageSource, ReloadableResourceBundleMessageSource
and StaticMessageSource. All of them implement HierarchicalMessageSource in order to do nested
messaging. The StaticMessageSource is rarely used but provides programmatic ways to
add messages to the source. The following example shows ResourceBundleMessageSource:
format
 exceptions
 windows
The example assumes that you have three resource bundles called format, exceptions and windows
defined in your classpath. Any request to resolve a message is
handled in the JDK-standard way of resolving messages through ResourceBundle objects. For the
purposes of the example, assume the contents of two of the above resource bundle files
are as follows:
# in format.properties
message=Alligators rock!
# in exceptions.properties
argument.required=The {0} argument is required.
The next example shows a program to run the MessageSource functionality.
Remember that all ApplicationContext implementations are also MessageSource
implementations and so can be cast to the MessageSource interface.
Java
Kotlin
public static void main(String[] args) {
 MessageSource resources = new ClassPathXmlApplicationContext("beans.xml");
 String message = resources.getMessage("message", null, "Default", Locale.ENGLISH);
 System.out.println(message);
}
fun main() {
 val resources = ClassPathXmlApplicationContext("beans.xml")
 val message = resources.getMessage("message", null, "Default", Locale.ENGLISH)
 println(message)
}
The resulting output from the above program is as follows:
Alligators rock!
To summarize, the MessageSource is defined in a file called beans.xml, which
exists at the root of your classpath. The messageSource bean definition refers to a
number of resource bundles through its basenames property. The three files that are
passed in the list to the basenames property exist as files at the root of your
classpath and are called format.properties, exceptions.properties, and
windows.properties, respectively.
The next example shows arguments passed to the message lookup. These arguments are
converted into String objects and inserted into placeholders in the lookup message.
Java
Kotlin
public class Example {

 private MessageSource messages;

 public void setMessages(MessageSource messages) {
 this.messages = messages;
 }

 public void execute() {
 String message = this.messages.getMessage("argument.required",
 new Object [] {"userDao"}, "Required", Locale.ENGLISH);
 System.out.println(message);
 }
}
class Example {

 lateinit var messages: MessageSource

 fun execute() {
 val message = messages.getMessage("argument.required",
 arrayOf("userDao"), "Required", Locale.ENGLISH)
 println(message)
 }
}
The resulting output from the invocation of the execute() method is as follows:
The userDao argument is required.
With regard to internationalization (“i18n”), Spring’s various MessageSource
implementations follow the same locale resolution and fallback rules as the standard JDK
ResourceBundle. In short, and continuing with the example messageSource defined
previously, if you want to resolve messages against the British (en-GB) locale, you
would create files called format_en_GB.properties, exceptions_en_GB.properties, and
windows_en_GB.properties, respectively.
Typically, locale resolution is managed by the surrounding environment of the
application. In the following example, the locale against which (British) messages are
resolved is specified manually:
# in exceptions_en_GB.properties
argument.required=Ebagum lad, the ''{0}'' argument is required, I say, required.
Java
Kotlin
public static void main(final String[] args) {
 MessageSource resources = new ClassPathXmlApplicationContext("beans.xml");
 String message = resources.getMessage("argument.required",
 new Object [] {"userDao"}, "Required", Locale.UK);
 System.out.println(message);
}
fun main() {
 val resources = ClassPathXmlApplicationContext("beans.xml")
 val message = resources.getMessage("argument.required",
 arrayOf("userDao"), "Required", Locale.UK)
 println(message)
}
The resulting output from the running of the above program is as follows:
Ebagum lad, the 'userDao' argument is required, I say, required.
You can also use the MessageSourceAware interface to acquire a reference to any
MessageSource that has been defined. Any bean that is defined in an
ApplicationContext that implements the MessageSourceAware interface is injected with
the application context’s MessageSource when the bean is created and configured.
Because Spring’s MessageSource is based on Java’s ResourceBundle, it does not merge
bundles with the same base name, but will only use the first bundle found.
Subsequent message bundles with the same base name are ignored.
As an alternative to ResourceBundleMessageSource, Spring provides a
ReloadableResourceBundleMessageSource class. This variant supports the same bundle
file format but is more flexible than the standard JDK based
ResourceBundleMessageSource implementation. In particular, it allows for reading
files from any Spring resource location (not only from the classpath) and supports hot
reloading of bundle property files (while efficiently caching them in between).
See the ReloadableResourceBundleMessageSource
javadoc for details.

## Standard and Custom Events
Event handling in the ApplicationContext is provided through the ApplicationEvent
class and the ApplicationListener interface. If a bean that implements the
ApplicationListener interface is deployed into the context, every time an
ApplicationEvent gets published to the ApplicationContext, that bean is notified.
Essentially, this is the standard Observer design pattern.
As of Spring 4.2, the event infrastructure has been significantly improved and offers
an annotation-based model as well as the
ability to publish any arbitrary event (that is, an object that does not necessarily
extend from ApplicationEvent). When such an object is published, we wrap it in an
event for you.
The following table describes the standard events that Spring provides:
Table 1. Built-in Events
Event
Explanation
ContextRefreshedEvent
Published when the ApplicationContext is initialized or refreshed (for example, by
 using the refresh() method on the ConfigurableApplicationContext interface).
 Here, “initialized” means that all beans are loaded, post-processor beans are detected
 and activated, singletons are pre-instantiated, and the ApplicationContext object is
 ready for use. As long as the context has not been closed, a refresh can be triggered
 multiple times, provided that the chosen ApplicationContext actually supports such
 “hot” refreshes. For example, XmlWebApplicationContext supports hot refreshes, but
 GenericApplicationContext does not.
ContextStartedEvent
Published when the ApplicationContext is started by using the start() method on the
 ConfigurableApplicationContext interface. Here, “started” means that all Lifecycle
 beans receive an explicit start signal. Typically, this signal is used to restart beans
 after an explicit stop, but it may also be used to start components that have not been
 configured for autostart (for example, components that have not already started on
 initialization).
ContextStoppedEvent
Published when the ApplicationContext is stopped by using the stop() method on the
 ConfigurableApplicationContext interface. Here, “stopped” means that all Lifecycle
 beans receive an explicit stop signal. A stopped context may be restarted through a
 start() call.
ContextClosedEvent
Published when the ApplicationContext is being closed by using the close() method
 on the ConfigurableApplicationContext interface or via a JVM shutdown hook. Here,
 "closed" means that all singleton beans will be destroyed. Once the context is closed,
 it reaches its end of life and cannot be refreshed or restarted.
RequestHandledEvent
A web-specific event telling all beans that an HTTP request has been serviced. This
 event is published after the request is complete. This event is only applicable to
 web applications that use Spring’s DispatcherServlet.
ServletRequestHandledEvent
A subclass of RequestHandledEvent that adds Servlet-specific context information.
You can also create and publish your own custom events. The following example shows a
simple class that extends Spring’s ApplicationEvent base class:
Java
Kotlin
public class BlockedListEvent extends ApplicationEvent {

 private final String address;
 private final String content;

 public BlockedListEvent(Object source, String address, String content) {
 super(source);
 this.address = address;
 this.content = content;
 }

 // accessor and other methods...
}
class BlockedListEvent(source: Any,
 val address: String,
 val content: String) : ApplicationEvent(source)
To publish a custom ApplicationEvent, call the publishEvent() method on an
ApplicationEventPublisher. Typically, this is done by creating a class that implements
ApplicationEventPublisherAware and registering it as a Spring bean. The following
example shows such a class:
Java
Kotlin
public class EmailService implements ApplicationEventPublisherAware {

 private List blockedList;
 private ApplicationEventPublisher publisher;

 public void setBlockedList(List blockedList) {
 this.blockedList = blockedList;
 }

 public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
 this.publisher = publisher;
 }

 public void sendEmail(String address, String content) {
 if (blockedList.contains(address)) {
 publisher.publishEvent(new BlockedListEvent(this, address, content));
 return;
 }
 // send email...
 }
}
class EmailService : ApplicationEventPublisherAware {

 private lateinit var blockedList: List
 private lateinit var publisher: ApplicationEventPublisher

 fun setBlockedList(blockedList: List) {
 this.blockedList = blockedList
 }

 override fun setApplicationEventPublisher(publisher: ApplicationEventPublisher) {
 this.publisher = publisher
 }

 fun sendEmail(address: String, content: String) {
 if (blockedList!!.contains(address)) {
 publisher!!.publishEvent(BlockedListEvent(this, address, content))
 return
 }
 // send email...
 }
}
At configuration time, the Spring container detects that EmailService implements
ApplicationEventPublisherAware and automatically calls
setApplicationEventPublisher(). In reality, the parameter passed in is the Spring
container itself. You are interacting with the application context through its
ApplicationEventPublisher interface.
To receive the custom ApplicationEvent, you can create a class that implements
ApplicationListener and register it as a Spring bean. The following example
shows such a class:
Java
Kotlin
public class BlockedListNotifier implements ApplicationListener {

 private String notificationAddress;

 public void setNotificationAddress(String notificationAddress) {
 this.notificationAddress = notificationAddress;
 }

 public void onApplicationEvent(BlockedListEvent event) {
 // notify appropriate parties via notificationAddress...
 }
}
class BlockedListNotifier : ApplicationListener {

 lateinit var notificationAddress: String

 override fun onApplicationEvent(event: BlockedListEvent) {
 // notify appropriate parties via notificationAddress...
 }
}
Notice that ApplicationListener is generically parameterized with the type of your custom event (BlockedListEvent in the preceding example).
This means that the onApplicationEvent() method can remain type-safe, avoiding any need for downcasting.
You can register as many event listeners as you wish, but note that, by default, event listeners receive events synchronously.
This means that the publishEvent() method blocks until all listeners have finished processing the event.
One advantage of this synchronous and single-threaded approach is that, when a listener receives an event,
it operates inside the transaction context of the publisher if a transaction context is available.
If another strategy for event publication becomes necessary, for example, asynchronous event processing by default,
see the javadoc for Spring’s ApplicationEventMulticaster interface
and SimpleApplicationEventMulticaster implementation
for configuration options which can be applied to a custom "applicationEventMulticaster" bean definition.
In these cases, ThreadLocals and logging context are not propagated for the event processing.
See the @EventListener Observability section
for more information on Observability concerns.
The following example shows the bean definitions used to register and configure each of
the classes above:
[email protected]
 [email protected]
 [email protected]
 
 

 [email protected]"/>
Putting it all together, when the sendEmail() method of the emailService bean is
called, if there are any email messages that should be blocked, a custom event of type
BlockedListEvent is published. The blockedListNotifier bean is registered as an
ApplicationListener and receives the BlockedListEvent, at which point it can
notify appropriate parties.
Spring’s eventing mechanism is designed for simple communication between Spring beans
within the same application context. However, for more sophisticated enterprise
integration needs, the separately maintained
Spring Integration project provides
complete support for building lightweight,
pattern-oriented, event-driven
architectures that build upon the well-known Spring programming model.

### Annotation-based Event Listeners
You can register an event listener on any method of a managed bean by using the
@EventListener annotation. The BlockedListNotifier can be rewritten as follows:
Java
Kotlin
public class BlockedListNotifier {

 private String notificationAddress;

 public void setNotificationAddress(String notificationAddress) {
 this.notificationAddress = notificationAddress;
 }

 @EventListener
 public void processBlockedListEvent(BlockedListEvent event) {
 // notify appropriate parties via notificationAddress...
 }
}
class BlockedListNotifier {

 lateinit var notificationAddress: String

 @EventListener
 fun processBlockedListEvent(event: BlockedListEvent) {
 // notify appropriate parties via notificationAddress...
 }
}
Do not define such beans to be lazy as the ApplicationContext will honor that and will not register the method to listen to events.
The method signature once again declares the event type to which it listens,
but, this time, with a flexible name and without implementing a specific listener interface.
The event type can also be narrowed through generics as long as the actual event type
resolves your generic parameter in its implementation hierarchy.
If your method should listen to several events or if you want to define it with no
parameter at all, the event types can also be specified on the annotation itself. The
following example shows how to do so:
Java
Kotlin
@EventListener({ContextStartedEvent.class, ContextRefreshedEvent.class})
public void handleContextStart() {
 // ...
}
@EventListener(ContextStartedEvent::class, ContextRefreshedEvent::class)
fun handleContextStart() {
 // ...
}
It is also possible to add additional runtime filtering by using the condition attribute
of the annotation that defines a SpEL expression, which should match
to actually invoke the method for a particular event.
The following example shows how our notifier can be rewritten to be invoked only if the
content attribute of the event is equal to my-event:
Java
Kotlin
@EventListener(condition = "#blEvent.content == 'my-event'")
public void processBlockedListEvent(BlockedListEvent blEvent) {
 // notify appropriate parties via notificationAddress...
}
@EventListener(condition = "#blEvent.content == 'my-event'")
fun processBlockedListEvent(blEvent: BlockedListEvent) {
 // notify appropriate parties via notificationAddress...
}
Each SpEL expression evaluates against a dedicated context. The following table lists the
items made available to the context so that you can use them for conditional event processing:
Table 2. Event metadata available in SpEL expressions
Name
Location
Description
Example
Event
root object
The actual ApplicationEvent.
#root.event or event
Arguments array
root object
The arguments (as an object array) used to invoke the method.
#root.args or args; args[0] to access the first argument, etc.
Argument name
evaluation context
The name of a particular method argument. If the names are not available
 (for example, because the code was compiled without the -parameters flag), individual
 arguments are also available using the #a<#arg> syntax where <#arg> stands for the
 argument index (starting from 0).
#blEvent or #a0 (you can also use #p0 or #p<#arg> parameter notation as an alias)
Note that #root.event gives you access to the underlying event, even if your method
signature actually refers to an arbitrary object that was published.
If you need to publish an event as the result of processing another event, you can change the
method signature to return the event that should be published, as the following example shows:
Java
Kotlin
@EventListener
public ListUpdateEvent handleBlockedListEvent(BlockedListEvent event) {
 // notify appropriate parties via notificationAddress and
 // then publish a ListUpdateEvent...
}
@EventListener
fun handleBlockedListEvent(event: BlockedListEvent): ListUpdateEvent {
 // notify appropriate parties via notificationAddress and
 // then publish a ListUpdateEvent...
}
This feature is not supported for
asynchronous listeners.
The handleBlockedListEvent() method publishes a new ListUpdateEvent for every
BlockedListEvent that it handles. If you need to publish several events, you can return
a Collection or an array of events instead.

### Asynchronous Listeners
If you want a particular listener to process events asynchronously, you can reuse the
regular @Async support.
The following example shows how to do so:
Java
Kotlin
@EventListener
@Async
public void processBlockedListEvent(BlockedListEvent event) {
 // BlockedListEvent is processed in a separate thread
}
@EventListener
@Async
fun processBlockedListEvent(event: BlockedListEvent) {
 // BlockedListEvent is processed in a separate thread
}
Be aware of the following limitations when using asynchronous events:
If an asynchronous event listener throws an Exception, it is not propagated to the
caller. See
AsyncUncaughtExceptionHandler
for more details.
Asynchronous event listener methods cannot publish a subsequent event by returning a
value. If you need to publish another event as the result of the processing, inject an
ApplicationEventPublisher
to publish the event manually.
ThreadLocals and logging context are not propagated by default for the event processing.
See the @EventListener Observability section
for more information on Observability concerns.

### Ordering Listeners
If you need one listener to be invoked before another one, you can add the @Order
annotation to the method declaration, as the following example shows:
Java
Kotlin
@EventListener
@Order(42)
public void processBlockedListEvent(BlockedListEvent event) {
 // notify appropriate parties via notificationAddress...
}
@EventListener
@Order(42)
fun processBlockedListEvent(event: BlockedListEvent) {
 // notify appropriate parties via notificationAddress...
}

### Generic Events
You can also use generics to further define the structure of your event. Consider using an
EntityCreatedEvent where T is the type of the actual entity that got created. For example, you
can create the following listener definition to receive only EntityCreatedEvent for a
Person:
Java
Kotlin
@EventListener
public void onPersonCreated(EntityCreatedEvent event) {
 // ...
}
@EventListener
fun onPersonCreated(event: EntityCreatedEvent) {
 // ...
}
Due to type erasure, this works only if the event that is fired resolves the generic
parameters on which the event listener filters (that is, something like
class PersonCreatedEvent extends EntityCreatedEvent { …​ }).
In certain circumstances, this may become quite tedious if all events follow the same
structure (as should be the case for the event in the preceding example). In such a case,
you can implement ResolvableTypeProvider to guide the framework beyond what the runtime
environment provides. The following event shows how to do so:
Java
Kotlin
public class EntityCreatedEvent extends ApplicationEvent implements ResolvableTypeProvider {

 public EntityCreatedEvent(T entity) {
 super(entity);
 }

 @Override
 public ResolvableType getResolvableType() {
 return ResolvableType.forClassWithGenerics(getClass(), ResolvableType.forInstance(getSource()));
 }
}
class EntityCreatedEvent(entity: T) : ApplicationEvent(entity), ResolvableTypeProvider {

 override fun getResolvableType(): ResolvableType? {
 return ResolvableType.forClassWithGenerics(javaClass, ResolvableType.forInstance(getSource()))
 }
}
This works not only for ApplicationEvent but any arbitrary object that you send as
an event.
Finally, as with classic ApplicationListener implementations, the actual multicasting
happens via a context-wide ApplicationEventMulticaster at runtime. By default, this is a
SimpleApplicationEventMulticaster with synchronous event publication in the caller thread.
This can be replaced/customized through an "applicationEventMulticaster" bean definition,
for example, for processing all events asynchronously and/or for handling listener exceptions:
@Bean
ApplicationEventMulticaster applicationEventMulticaster() {
 SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster();
 multicaster.setTaskExecutor(...);
 multicaster.setErrorHandler(...);
 return multicaster;
}

## Convenient Access to Low-level Resources
For optimal usage and understanding of application contexts, you should familiarize
yourself with Spring’s Resource abstraction, as described in
Resources.
An application context is a ResourceLoader, which can be used to load Resource objects.
A Resource is essentially a more feature rich version of the JDK java.net.URL class.
In fact, implementations of Resource wrap an instance of java.net.URL, where
appropriate. A Resource can obtain low-level resources from almost any location in a
transparent fashion, including from the classpath, a filesystem location, anywhere
describable with a standard URL, and some other variations. If the resource location
string is a simple path without any special prefixes, where those resources come from is
specific and appropriate to the actual application context type.
You can configure a bean deployed into the application context to implement the special
callback interface, ResourceLoaderAware, to be automatically called back at
initialization time with the application context itself passed in as the ResourceLoader.
You can also expose properties of type Resource, to be used to access static resources.
They are injected into it like any other properties. You can specify those Resource
properties as simple String paths and rely on automatic conversion from those text
strings to actual Resource objects when the bean is deployed.
The location path or paths supplied to an ApplicationContext constructor are actually
resource strings and, in simple form, are treated appropriately according to the specific
context implementation. For example ClassPathXmlApplicationContext treats a simple
location path as a classpath location. You can also use location paths (resource strings)
with special prefixes to force loading of definitions from the classpath or a URL,
regardless of the actual context type.

## Application Startup Tracking
The ApplicationContext manages the lifecycle of Spring applications and provides a rich
programming model around components. As a result, complex applications can have equally
complex component graphs and startup phases.
Tracking the application startup steps with specific metrics can help understand where
time is being spent during the startup phase, but it can also be used as a way to better
understand the context lifecycle as a whole.
The AbstractApplicationContext (and its subclasses) is instrumented with an
ApplicationStartup, which collects StartupStep data about various startup phases:
application context lifecycle (base packages scanning, config classes management)
beans lifecycle (instantiation, smart initialization, post processing)
application events processing
Here is an example of instrumentation in the AnnotationConfigApplicationContext:
Java
Kotlin
// create a startup step and start recording
try (StartupStep scanPackages = getApplicationStartup().start("spring.context.base-packages.scan")) {
 // add tagging information to the current step
 scanPackages.tag("packages", () -> Arrays.toString(basePackages));
 // perform the actual phase we're instrumenting
 this.scanner.scan(basePackages);
}
// create a startup step and start recording
try (val scanPackages = getApplicationStartup().start("spring.context.base-packages.scan")) {
 // add tagging information to the current step
 scanPackages.tag("packages", () -> Arrays.toString(basePackages));
 // perform the actual phase we're instrumenting
 this.scanner.scan(basePackages);
}
The application context is already instrumented with multiple steps.
Once recorded, these startup steps can be collected, displayed and analyzed with specific tools.
For a complete list of existing startup steps, you can check out the
dedicated appendix section.
The default ApplicationStartup implementation is a no-op variant, for minimal overhead.
This means no metrics will be collected during application startup by default.
Spring Framework ships with an implementation for tracking startup steps with Java Flight Recorder:
FlightRecorderApplicationStartup. To use this variant, you must configure an instance of it
to the ApplicationContext as soon as it’s been created.
Developers can also use the ApplicationStartup infrastructure if they’re providing their own
AbstractApplicationContext subclass, or if they wish to collect more precise data.
ApplicationStartup is meant to be only used during application startup and for
the core container; this is by no means a replacement for Java profilers or
metrics libraries like Micrometer.
To start collecting custom StartupStep, components can either get the ApplicationStartup
instance from the application context directly, make their component implement ApplicationStartupAware,
or ask for the ApplicationStartup type on any injection point.
Developers should not use the "spring.*" namespace when creating custom startup steps.
This namespace is reserved for internal Spring usage and is subject to change.

## Convenient ApplicationContext Instantiation for Web Applications
You can create ApplicationContext instances declaratively by using, for example, a
ContextLoader. Of course, you can also create ApplicationContext instances
programmatically by using one of the ApplicationContext implementations.
You can register an ApplicationContext by using the ContextLoaderListener, as the
following example shows:
contextConfigLocation
 /WEB-INF/daoContext.xml /WEB-INF/applicationContext.xml

 org.springframework.web.context.ContextLoaderListener
The listener inspects the contextConfigLocation parameter. If the parameter does not
exist, the listener uses /WEB-INF/applicationContext.xml as a default. When the
parameter does exist, the listener separates the String by using predefined
delimiters (comma, semicolon, and whitespace) and uses the values as locations where
application contexts are searched. Ant-style path patterns are supported as well.
Examples are /WEB-INF/*Context.xml (for all files with names that end with
Context.xml and that reside in the WEB-INF directory) and /WEB-INF/**/*Context.xml
(for all such files in any subdirectory of WEB-INF).

## Deploying a Spring ApplicationContext as a Jakarta EE RAR File
It is possible to deploy a Spring ApplicationContext as a RAR file, encapsulating the
context and all of its required bean classes and library JARs in a Jakarta EE RAR deployment
unit. This is the equivalent of bootstrapping a stand-alone ApplicationContext (only hosted
in Jakarta EE environment) being able to access the Jakarta EE servers facilities. RAR deployment
is a more natural alternative to a scenario of deploying a headless WAR file — in effect,
a WAR file without any HTTP entry points that is used only for bootstrapping a Spring
ApplicationContext in a Jakarta EE environment.
RAR deployment is ideal for application contexts that do not need HTTP entry points but
rather consist only of message endpoints and scheduled jobs. Beans in such a context can
use application server resources such as the JTA transaction manager and JNDI-bound JDBC
DataSource instances and JMS ConnectionFactory instances and can also register with
the platform’s JMX server — all through Spring’s standard transaction management and JNDI
and JMX support facilities. Application components can also interact with the application
server’s JCA WorkManager through Spring’s TaskExecutor abstraction.
See the javadoc of the
SpringContextResourceAdapter
class for the configuration details involved in RAR deployment.
For a simple deployment of a Spring ApplicationContext as a Jakarta EE RAR file:
Package
all application classes into a RAR file (which is a standard JAR file with a different
file extension).
Add all required library JARs into the root of the RAR archive.
Add a
META-INF/ra.xml deployment descriptor (as shown in the
javadoc for SpringContextResourceAdapter)
and the corresponding Spring XML bean definition file(s) (typically
META-INF/applicationContext.xml).
Drop the resulting RAR file into your
application server’s deployment directory.
Such RAR deployment units are usually self-contained. They do not expose components
to the outside world, not even to other modules of the same application. Interaction with a
RAR-based ApplicationContext usually occurs through JMS destinations that it shares with
other modules. A RAR-based ApplicationContext may also, for example, schedule some jobs
or react to new files in the file system (or the like). If it needs to allow synchronous
access from the outside, it could (for example) export RMI endpoints, which may be used
by other application modules on the same machine.
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

Source: https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html
HTTP status: 200 · extracted bytes: 48038 · sha256: 166ad86467520b875422724645f4b007a78cbe48842f0c3f239ada8d679ef834
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r130`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

Additional Capabilities of the ApplicationContext :: Spring Framework Why Spring Overview Trending Generative AI Cloud Architecture Patterns Microservices Reactive Event Driven Application Types Web Applications Serverless Batch Learn Getting Started Quickstart Guides Academy Courses Get Certified Projects Overview Projects Spring Boot Spring Framework Spring Cloud Spring AI Spring Data Spring Integration Spring Batch Spring Security Foundational Projects Micrometer Reactor Development Tools Spring Tools Spring Initializr Resources Blog Release Calendar Version Mappings Release Highlights Security Advisories GitHub Orgs Spring Projects Spring Cloud Community Overview Events Authors Enterprise Overview Long-term Support Automated Upgrades Governance and Compliance Modern App Development light Spring Framework 7.0.8 Search Overview Core Technologies The IoC Container Introduction to the Spring IoC Container and Beans Container Overview Bean Overview Dependencies Dependency Injection Dependencies and Configuration in Detail Using depends-on Lazy-initialized Beans Autowiring Collaborators Method Injection Bean Scopes Customizing the Nature of a Bean Bean Definition Inheritance Container Extension Points Annotation-based Container Configuration Using @Autowired Fine-tuning Annotation-based Autowiring with @Primary or @Fallback Fine-tuning Annotation-based Autowiring with Qualifiers Using Generics as Autowiring Qualifiers Using CustomAutowireConfigurer Injection with @Resource Using @Value Using @PostConstruct and @PreDestroy Classpath Scanning and Managed Components Using JSR-330 Standard Annotations Java-based Container Configuration Basic Concepts: @Bean and @Configuration Instantiating the Spring Container by Using AnnotationConfigApplicationContext Using the @Bean Annotation Using the @Configuration annotation Composing Java-based Configurations Programmatic Bean Registration Environment Abstraction Registering a LoadTimeWeaver Additional Capabilities of the ApplicationContext The BeanFactory API Resources Validation, Data Binding, and Type Conversion Validation Using Spring’s Validator Interface Data Binding Resolving Error Codes to Error Messages Spring Type Conversion Spring Field Formatting Configuring a Global Date and Time Format Java Bean Validation Spring Expression Language (SpEL) Evaluation Expressions in Bean Definitions Language Reference Literal Expressions Properties, Arrays, Lists, Maps, and Indexers Inline Lists Inline Maps Array Construction Methods Operators Types Constructors Variables Functions Varargs Invocations Bean References Ternary Operator (If-Then-Else) The Elvis Operator Safe Navigation Operator Collection Selection Collection Projection Expression Templating Classes Used in the Examples Aspect Oriented Programming with Spring AOP Concepts Spring AOP Capabilities and Goals AOP Proxies @AspectJ support Enabling @AspectJ Support Declaring an Aspect Declaring a Pointcut Declaring Advice Introductions Aspect Instantiation Models An AOP Example Schema-based AOP Support Choosing which AOP Declaration Style to Use Mixing Aspect Types Proxying Mechanisms Programmatic Creation of @AspectJ Proxies Using AspectJ with Spring Applications Further Resources Spring AOP APIs Pointcut API in Spring Advice API in Spring The Advisor API in Spring Using the ProxyFactoryBean to Create AOP Proxies Concise Proxy Definitions Creating AOP Proxies Programmatically with the ProxyFactory Manipulating Advised Objects Using the "auto-proxy" facility Using TargetSource Implementations Defining New Advice Types Resilience Features Null-safety Data Buffers and Codecs Ahead of Time Optimizations Appendix XML Schemas XML Schema Authoring Application Startup Steps Data Access Transaction Management Advantages of the Spring Framework’s Transaction Support Model Understanding the Spring Framework Transaction Abstraction Synchronizing Resources with Transactions Declarative Transaction Management Understanding the Spring Framework’s Declarative Transaction Implementation Example of Declarative Transaction Implementation Rolling Back a Declarative Transaction Configuring Different Transactional Semantics for Different Beans <tx:advice/> Settings Using @Transactional Transaction Propagation Advising Transactional Operations Using @Transactional with AspectJ Programmatic Transaction Management Choosing Between Programmatic and Declarative Transaction Management Transaction-bound Events Application server-specific integration Solutions to Common Problems Further Resources DAO Support Data Access with JDBC Choosing an Approach for JDBC Database Access Package Hierarchy Using the JDBC Core Classes to Control Basic JDBC Processing and Error Handling Controlling Database Connections JDBC Batch Operations Simplifying JDBC Operations with the SimpleJdbc Classes Modeling JDBC Operations as Java Objects Common Problems with Parameter and Data Value Handling Embedded Database Support Initializing a DataSource Data Access with R2DBC Object Relational Mapping (ORM) Data Access Introduction to ORM with Spring General ORM Integration Considerations Hibernate JPA Marshalling XML by Using Object-XML Mappers Appendix Web on Servlet Stack Spring Web MVC DispatcherServlet Context Hierarchy Special Bean Types Web MVC Config Servlet Config Processing Path Matching Interception Exceptions View Resolution Locale Multipart Resolver Logging Filters HTTP Message Conversion Annotated Controllers Declaration Mapping Requests Handler Methods Method Arguments Return Values Type Conversion Matrix Variables @RequestParam @RequestHeader @CookieValue @ModelAttribute @SessionAttributes @SessionAttribute @RequestAttribute Redirect Attributes Flash Attributes Multipart @RequestBody HttpEntity @ResponseBody ResponseEntity Jackson JSON Model @InitBinder Validation Exceptions Controller Advice Functional Endpoints URI Links Asynchronous Requests Range Requests Data Binding CORS API Versioning Error Responses Web Security HTTP Caching View Technologies Thymeleaf FreeMarker Groovy Markup Script Views HTML Fragments JSP and JSTL RSS and Atom PDF and Excel Jackson XML Marshalling XSLT Views MVC Config Enable MVC Configuration MVC Config API Type Conversion Validation Interceptors Content Types Message Converters View Controllers View Resolvers Static Resources Default Servlet Path Matching API Version Advanced Java Config Advanced XML Config HTTP/2 REST Clients Testing WebSockets WebSocket API SockJS Fallback STOMP Overview Benefits Enable STOMP WebSocket Transport Flow of Messages Annotated Controllers Sending Messages Simple Broker External Broker Connecting to a Broker Dots as Separators Authentication Token Authentication Authorization User Destinations Order of Messages Events Interception STOMP Client WebSocket Scope Performance Monitoring Testing Web on Reactive Stack Spring WebFlux Overview Reactive Core DispatcherHandler Annotated Controllers @Controller Mapping Requests Handler Methods Method Arguments Return Values Type Conversion Matrix Variables @RequestParam @RequestHeader @CookieValue @ModelAttribute @SessionAttributes @SessionAttribute @RequestAttribute Multipart Content @RequestBody HttpEntity @ResponseBody ResponseEntity Jackson JSON Model DataBinder Validation Exceptions Controller Advice Functional Endpoints URI Links Range Requests Data Binding CORS API Versioning Error Responses Web Security HTTP Caching View Technologies WebFlux Config HTTP/2 WebClient Configuration retrieve() Exchange Request Body Filters Attributes Context Synchronous Use Testing HTTP Service Client WebSockets Testing RSocket Reactive Libraries Testing Introduction to Spring Testing Unit Testing Integration Testing JDBC Testing Support Spring TestContext Framework Key Abstractions Bootstrapping the TestContext Framework TestExecutionListener Configuration Application Events Test Execution Events Context Management Context Configuration with Component Classes Context Configuration with XML Resources Context Configuration with Groovy Scripts Default Context Configuration Mixing Component Classes, XML, and Groovy Scripts Context Configuration with Context Customizers Context Configuration with Context Initializers Context Configuration Inheritance Context Configuration with Environment Profiles Context Configuration with Test Property Sources Context Configuration with Dynamic Property Sources Loading a WebApplicationContext Working with Web Mocks Context Caching Context Pausing Context Failure Threshold Context Hierarchies Dependency Injection of Test Fixtures Bean Overriding in Tests Testing Request- and Session-scoped Beans Transaction Management Executing SQL Scripts Parallel Test Execution TestContext Framework Support Classes Ahead of Time Support for Tests WebTestClient RestTestClient MockMvc Overview Setup Options Hamcrest Integration Static Imports Configuring MockMvc Setup Features Performing Requests Defining Expectations Async Requests Streaming Responses Filter Registrations AssertJ Integration Configuring MockMvcTester Performing Requests Defining Expectations MockMvc integration HtmlUnit Integration Why HtmlUnit Integration? MockMvc and HtmlUnit MockMvc and WebDriver MockMvc and Geb MockMvc vs End-to-End Tests Further Examples Testing Client Applications Appendix Annotations Standard Annotation Support Spring Testing Annotations @BootstrapWith @ContextConfiguration @WebAppConfiguration @ContextHierarchy @ContextCustomizerFactories @ActiveProfiles @TestPropertySource @DynamicPropertySource @TestBean @MockitoBean and @MockitoSpyBean @DirtiesContext @TestExecutionListeners @RecordApplicationEvents @Commit @Rollback @BeforeTransaction @AfterTransaction @Sql @SqlConfig @SqlMergeMode @SqlGroup @DisabledInAotMode Spring JUnit 4 Testing Annotations Spring JUnit Jupiter Testing Annotations Meta-Annotation Support for Testing Further Resources Integration REST Clients JMS (Java Message Service) Using Spring JMS Sending a Message Receiving a Message Support for JCA Message Endpoints Annotation-driven Listener Endpoints JMS Namespace Support JMX Exporting Your Beans to JMX Controlling the Management Interface of Your Beans Controlling ObjectName Instances for Your Beans Using JSR-160 Connectors Accessing MBeans through Proxies Notifications Further Resources Email Task Execution and Scheduling Cache Abstraction Understanding the Cache Abstraction Declarative Annotation-based Caching JCache (JSR-107) Annotations Declarative XML-based Caching Configuring the Cache Storage Plugging-in Different Back-end Caches How can I Set the TTL/TTI/Eviction policy/XXX feature? Observability Support JVM AOT Cache JVM Checkpoint Restore Appendix Language Support Kotlin Requirements Extensions Null-safety Classes and Interfaces Annotations Bean Registration DSL Web Coroutines Spring Projects in Kotlin Getting Started Resources Apache Groovy Appendix Java API Kotlin API Wiki Search Edit this Page GitHub Project Stack Overflow Spring Framework Core Technologies The IoC Container Additional Capabilities of the ApplicationContext Additional Capabilities of the ApplicationContext As discussed in the chapter introduction , the org.springframework.beans.factory package provides basic functionality for managing and manipulating beans, including in a programmatic way. The org.springframework.context package adds the ApplicationContext interface, which extends the BeanFactory interface, in addition to extending other interfaces to provide additional functionality in a more application framework-oriented style. Many people use the ApplicationContext in a completely declarative fashion, not even creating it programmatically, but instead relying on support classes such as ContextLoader to automatically instantiate an ApplicationContext as part of the normal startup process of a Jakarta EE web application. To enhance BeanFactory functionality in a more framework-oriented style, the context package also provides the following functionality: Access to messages in i18n-style, through the MessageSource interface. Access to resources, such as URLs and files, through the ResourceLoader interface. Event publication, namely to beans that implement the ApplicationListener interface, through the use of the ApplicationEventPublisher interface. Loading of multiple (hierarchical) contexts, letting each be focused on one particular layer, such as the web layer of an application, through the HierarchicalBeanFactory interface. Internationalization using MessageSource The ApplicationContext interface extends an interface called MessageSource and, therefore, provides internationalization (“i18n”) functionality. Spring also provides the HierarchicalMessageSource interface, which can resolve messages hierarchically. Together, these interfaces provide the foundation upon which Spring effects message resolution. The methods defined on these interfaces include: String getMessage(String code, Object[] args, String default, Locale loc) : The basic method used to retrieve a message from the MessageSource . When no message is found for the specified locale, the default message is used. Any arguments passed in become replacement values, using the MessageFormat functionality provided by the standard library. String getMessage(String code, Object[] args, Locale loc) : Essentially the same as the previous method but with one difference: No default message can be specified. If the message cannot be found, a NoSuchMessageException is thrown. String getMessage(MessageSourceResolvable resolvable, Locale locale) : All properties used in the preceding methods are also wrapped in a class named MessageSourceResolvable , which you can use with this method. When an ApplicationContext is loaded, it automatically searches for a MessageSource bean defined in the context. The bean must have the name messageSource . If such a bean is found, all calls to the preceding methods are delegated to the message source. If no message source is found, the ApplicationContext attempts to find a parent containing a bean with the same name. If it does, it uses that bean as the MessageSource . If the ApplicationContext cannot find any source for messages, an empty DelegatingMessageSource is instantiated in order to be able to accept calls to the methods defined above. Spring provides three MessageSource implementations, ResourceBundleMessageSource , ReloadableResourceBundleMessageSource and StaticMessageSource . All of them implement HierarchicalMessageSource in order to do nested messaging. The StaticMessageSource is rarely used but provides programmatic ways to add messages to the source. The following example shows ResourceBundleMessageSource : <beans> <bean id="messageSource" class="org.springframework.context.support.ResourceBundleMessageSource"> <property name="basenames"> <list> <value>format</value> <value>exceptions</value> <value>windows</value> </list> </property> </bean> </beans> The example assumes that you have three resource bundles called format , exceptions and windows defined in your classpath. Any request to resolve a message is handled in the JDK-standard way of resolving messages through ResourceBundle objects. For the purposes of the example, assume the contents of two of the above resource bundle files are as follows: # in format.properties message=Alligators rock! # in exceptions.properties argument.required=The {0} argument is required. The next example shows a program to run the MessageSource functionality. Remember that all ApplicationContext implementations are also MessageSource implementations and so can be cast to the MessageSource interface. Java Kotlin public static void main(String[] args) { MessageSource resources = new ClassPathXmlApplicationContext("beans.xml"); String message = resources.getMessage("message", null, "Default", Locale.ENGLISH); System.out.println(message); } fun main() { val resources = ClassPathXmlApplicationContext("beans.xml") val message = resources.getMessage("message", null, "Default", Locale.ENGLISH) println(message) } The resulting output from the above program is as follows: Alligators rock! To summarize, the MessageSource is defined in a file called beans.xml , which exists at the root of your classpath. The messageSource bean definition refers to a number of resource bundles through its basenames property. The three files that are passed in the list to the basenames property exist as files at the root of your classpath and are called format.properties , exceptions.properties , and windows.properties , respectively. The next example shows arguments passed to the message lookup. These arguments are converted into String objects and inserted into placeholders in the lookup message. <beans> <!-- this MessageSource is being used in a web application --> <bean id="messageSource" class="org.springframework.context.support.ResourceBundleMessageSource"> <property name="basename" value="exceptions"/> </bean> <!-- lets inject the above MessageSource into this POJO --> <bean id="example" class="com.something.Example"> <property name="messages" ref="messageSource"/> </bean> </beans> Java Kotlin public class Example { private MessageSource messages; public void setMessages(MessageSource messages) { this.messages = messages; } public void execute() { String message = this.messages.getMessage("argument.required", new Object [] {"userDao"}, "Required", Locale.ENGLISH); System.out.println(message); } } class Example { lateinit var messages: MessageSource fun execute() { val message = messages.getMessage("argument.required", arrayOf("userDao"), "Required", Locale.ENGLISH) println(message) } } The resulting output from the invocation of the execute() method is as follows: The userDao argument is required. With regard to internationalization (“i18n”), Spring’s various MessageSource implementations follow the same locale resolution and fallback rules as the standard JDK ResourceBundle . In short, and continuing with the example messageSource defined previously, if you want to resolve messages against the British ( en-GB ) locale, you would create files called format_en_GB.properties , exceptions_en_GB.properties , and windows_en_GB.properties , respectively. Typically, locale resolution is managed by the surrounding environment of the application. In the following example, the locale against which (British) messages are resolved is specified manually: # in exceptions_en_GB.properties argument.required=Ebagum lad, the ''{0}'' argument is required, I say, required. Java Kotlin public static void main(final String[] args) { MessageSource resources = new ClassPathXmlApplicationContext("beans.xml"); String message = resources.getMessage("argument.required", new Object [] {"userDao"}, "Required", Locale.UK); System.out.println(message); } fun main() { val resources = ClassPathXmlApplicationContext("beans.xml") val message = resources.getMessage("argument.required", arrayOf("userDao"), "Required", Locale.UK) println(message) } The resulting output from the running of the above program is as follows: Ebagum lad, the 'userDao' argument is required, I say, required. You can also use the MessageSourceAware interface to acquire a reference to any MessageSource that has been defined. Any bean that is defined in an ApplicationContext that implements the MessageSourceAware interface is injected with the application context’s MessageSource when the bean is created and configured. Because Spring’s MessageSource is based on Java’s ResourceBundle , it does not merge bundles with the same base name, but will only use the first bundle found. Subsequent message bundles with the same base name are ignored. As an alternative to ResourceBundleMessageSource , Spring provides a ReloadableResourceBundleMessageSource class. This variant supports the same bundle file format but is more flexible than the standard JDK based ResourceBundleMessageSource implementation. In particular, it allows for reading files from any Spring resource location (not only from the classpath) and supports hot reloading of bundle property files (while efficiently caching them in between). See the ReloadableResourceBundleMessageSource javadoc for details. Standard and Custom Events Event handling in the ApplicationContext is provided through the ApplicationEvent class and the ApplicationListener interface. If a bean that implements the ApplicationListener interface is deployed into the context, every time an ApplicationEvent gets published to the ApplicationContext , that bean is notified. Essentially, this is the standard Observer design pattern. As of Spring 4.2, the event infrastructure has been significantly improved and offers an annotation-based model as well as the ability to publish any arbitrary event (that is, an object that does not necessarily extend from ApplicationEvent ). When such an object is published, we wrap it in an event for you. The following table describes the standard events that Spring provides: Table 1. Built-in Events Event Explanation ContextRefreshedEvent Published when the ApplicationContext is initialized or refreshed (for example, by using the refresh() method on the ConfigurableApplicationContext interface). Here, “initialized” means that all beans are loaded, post-processor beans are detected and activated, singletons are pre-instantiated, and the ApplicationContext object is ready for use. As long as the context has not been closed, a refresh can be triggered multiple times, provided that the chosen ApplicationContext actually supports such “hot” refreshes. For example, XmlWebApplicationContext supports hot refreshes, but GenericApplicationContext does not. ContextStartedEvent Published when the ApplicationContext is started by using the start() method on the ConfigurableApplicationContext interface. Here, “started” means that all Lifecycle beans receive an explicit start signal. Typically, this signal is used to restart beans after an explicit stop, but it may also be used to start components that have not been configured for autostart (for example, components that have not already started on initialization). ContextStoppedEvent Published when the ApplicationContext is stopped by using the stop() method on the ConfigurableApplicationContext interface. Here, “stopped” means that all Lifecycle beans receive an explicit stop signal. A stopped context may be restarted through a start() call. ContextClosedEvent Published when the ApplicationContext is being closed by using the close() method on the ConfigurableApplicationContext interface or via a JVM shutdown hook. Here, "closed" means that all singleton beans will be destroyed. Once the context is closed, it reaches its end of life and cannot be refreshed or restarted. RequestHandledEvent A web-specific event telling all beans that an HTTP request has been serviced. This event is published after the request is complete. This event is only applicable to web applications that use Spring’s DispatcherServlet . ServletRequestHandledEvent A subclass of RequestHandledEvent that adds Servlet-specific context information. You can also create and publish your own custom events. The following example shows a simple class that extends Spring’s ApplicationEvent base class: Java Kotlin public class BlockedListEvent extends ApplicationEvent { private final String address; private final String content; public BlockedListEvent(Object source, String address, String content) { super(source); this.address = address; this.content = content; } // accessor and other methods... } class BlockedListEvent(source: Any, val address: String, val content: String) : ApplicationEvent(source) To publish a custom ApplicationEvent , call the publishEvent() method on an ApplicationEventPublisher . Typically, this is done by creating a class that implements ApplicationEventPublisherAware and registering it as a Spring bean. The following example shows such a class: Java Kotlin public class EmailService implements ApplicationEventPublisherAware { private List<String> blockedList; private ApplicationEventPublisher publisher; public void setBlockedList(List<String> blockedList) { this.blockedList = blockedList; } public void setApplicationEventPublisher(ApplicationEventPublisher publisher) { this.publisher = publisher; } public void sendEmail(String address, String content) { if (blockedList.contains(address)) { publisher.publishEvent(new BlockedListEvent(this, address, content)); return; } // send email... } } class EmailService : ApplicationEventPublisherAware { private lateinit var blockedList: List<String> private lateinit var publisher: ApplicationEventPublisher fun setBlockedList(blockedList: List<String>) { this.blockedList = blockedList } override fun setApplicationEventPublisher(publisher: ApplicationEventPublisher) { this.publisher = publisher } fun sendEmail(address: String, content: String) { if (blockedList!!.contains(address)) { publisher!!.publishEvent(BlockedListEvent(this, address, content)) return } // send email... } } At configuration time, the Spring container detects that EmailService implements ApplicationEventPublisherAware and automatically calls setApplicationEventPublisher() . In reality, the parameter passed in is the Spring container itself. You are interacting with the application context through its ApplicationEventPublisher interface. To receive the custom ApplicationEvent , you can create a class that implements ApplicationListener and register it as a Spring bean. The following example shows such a class: Java Kotlin public class BlockedListNotifier implements ApplicationListener<BlockedListEvent> { private String notificationAddress; public void setNotificationAddress(String notificationAddress) { this.notificationAddress = notificationAddress; } public void onApplicationEvent(BlockedListEvent event) { // notify appropriate parties via notificationAddress... } } class BlockedListNotifier : ApplicationListener<BlockedListEvent> { lateinit var notificationAddress: String override fun onApplicationEvent(event: BlockedListEvent) { // notify appropriate parties via notificationAddress... } } Notice that ApplicationListener is generically parameterized with the type of your custom event ( BlockedListEvent in the preceding example). This means that the onApplicationEvent() method can remain type-safe, avoiding any need for downcasting. You can register as many event listeners as you wish, but note that, by default, event listeners receive events synchronously. This means that the publishEvent() method blocks until all listeners have finished processing the event. One advantage of this synchronous and single-threaded approach is that, when a listener receives an event, it operates inside the transaction context of the publisher if a transaction context is available. If another strategy for event publication becomes necessary, for example, asynchronous event processing by default, see the javadoc for Spring’s ApplicationEventMulticaster interface and SimpleApplicationEventMulticaster implementation for configuration options which can be applied to a custom "applicationEventMulticaster" bean definition. In these cases, ThreadLocals and logging context are not propagated for the event processing. See the @EventListener Observability section for more information on Observability concerns. The following example shows the bean definitions used to register and configure each of the classes above: <bean id="emailService" class="example.EmailService"> <property name="blockedList"> <list> <value> [email protected] </value> <value> [email protected] </value> <value> [email protected] </value> </list> </property> </bean> <bean id="blockedListNotifier" class="example.BlockedListNotifier"> <property name="notificationAddress" value=" [email protected] "/> </bean> <!-- optional: a custom ApplicationEventMulticaster definition --> <bean id="applicationEventMulticaster" class="org.springframework.context.event.SimpleApplicationEventMulticaster"> <property name="taskExecutor" ref="..."/> <property name="errorHandler" ref="..."/> </bean> Putting it all together, when the sendEmail() method of the emailService bean is called, if there are any email messages that should be blocked, a custom event of type BlockedListEvent is published. The blockedListNotifier bean is registered as an ApplicationListener and receives the BlockedListEvent , at which point it can notify appropriate parties. Spring’s eventing mechanism is designed for simple communication between Spring beans within the same application context. However, for more sophisticated enterprise integration needs, the separately maintained Spring Integration project provides complete support for building lightweight, pattern-oriented , event-driven architectures that build upon the well-known Spring programming model. Annotation-based Event Listeners You can register an event listener on any method of a managed bean by using the @EventListener annotation. The BlockedListNotifier can be rewritten as follows: Java Kotlin public class BlockedListNotifier { private String notificationAddress; public void setNotificationAddress(String notificationAddress) { this.notificationAddress = notificationAddress; } @EventListener public void processBlockedListEvent(BlockedListEvent event) { // notify appropriate parties via notificationAddress... } } class BlockedListNotifier { lateinit var notificationAddress: String @EventListener fun processBlockedListEvent(event: BlockedListEvent) { // notify appropriate parties via notificationAddress... } } Do not define such beans to be lazy as the ApplicationContext will honor that and will not register the method to listen to events. The method signature once again declares the event type to which it listens, but, this time, with a flexible name and without implementing a specific listener interface. The event type can also be narrowed through generics as long as the actual event type resolves your generic parameter in its implementation hierarchy. If your method should listen to several events or if you want to define it with no parameter at all, the event types can also be specified on the annotation itself. The following example shows how to do so: Java Kotlin @EventListener({ContextStartedEvent.class, ContextRefreshedEvent.class}) public void handleContextStart() { // ... } @EventListener(ContextStartedEvent::class, ContextRefreshedEvent::class) fun handleContextStart() { // ... } It is also possible to add additional runtime filtering by using the condition attribute of the annotation that defines a SpEL expression , which should match to actually invoke the method for a particular event. The following example shows how our notifier can be rewritten to be invoked only if the content attribute of the event is equal to my-event : Java Kotlin @EventListener(condition = "#blEvent.content == 'my-event'") public void processBlockedListEvent(BlockedListEvent blEvent) { // notify appropriate parties via notificationAddress... } @EventListener(condition = "#blEvent.content == 'my-event'") fun processBlockedListEvent(blEvent: BlockedListEvent) { // notify appropriate parties via notificationAddress... } Each SpEL expression evaluates against a dedicated context. The following table lists the items made available to the context so that you can use them for conditional event processing: Table 2. Event metadata available in SpEL expressions Name Location Description Example Event root object The actual ApplicationEvent . #root.event or event Arguments array root object The arguments (as an object array) used to invoke the method. #root.args or args ; args[0] to access the first argument, etc. Argument name evaluation context The name of a particular method argument. If the names are not available (for example, because the code was compiled without the -parameters flag), individual arguments are also available using the #a<#arg> syntax where <#arg> stands for the argument index (starting from 0). #blEvent or #a0 (you can also use #p0 or #p<#arg> parameter notation as an alias) Note that #root.event gives you access to the underlying event, even if your method signature actually refers to an arbitrary object that was published. If you need to publish an event as the result of processing another event, you can change the method signature to return the event that should be published, as the following example shows: Java Kotlin @EventListener public ListUpdateEvent handleBlockedListEvent(BlockedListEvent event) { // notify appropriate parties via notificationAddress and // then publish a ListUpdateEvent... } @EventListener fun handleBlockedListEvent(event: BlockedListEvent): ListUpdateEvent { // notify appropriate parties via notificationAddress and // then publish a ListUpdateEvent... } This feature is not supported for asynchronous listeners . The handleBlockedListEvent() method publishes a new ListUpdateEvent for every BlockedListEvent that it handles. If you need to publish several events, you can return a Collection or an array of events instead. Asynchronous Listeners If you want a particular listener to process events asynchronously, you can reuse the regular @Async support . The following example shows how to do so: Java Kotlin @EventListener @Async public void processBlockedListEvent(BlockedListEvent event) { // BlockedListEvent is processed in a separate thread } @EventListener @Async fun processBlockedListEvent(event: BlockedListEvent) { // BlockedListEvent is processed in a separate thread } Be aware of the following limitations when using asynchronous events: If an asynchronous event listener throws an Exception , it is not propagated to the caller. See AsyncUncaughtExceptionHandler for more details. Asynchronous event listener methods cannot publish a subsequent event by returning a value. If you need to publish another event as the result of the processing, inject an ApplicationEventPublisher to publish the event manually. ThreadLocals and logging context are not propagated by default for the event processing. See the @EventListener Observability section for more information on Observability concerns. Ordering Listeners If you need one listener to be invoked before another one, you can add the @Order annotation to the method declaration, as the following example shows: Java Kotlin @EventListener @Order(42) public void processBlockedListEvent(BlockedListEvent event) { // notify appropriate parties via notificationAddress... } @EventListener @Order(42) fun processBlockedListEvent(event: BlockedListEvent) { // notify appropriate parties via notificationAddress... } Generic Events You can also use generics to further define the structure of your event. Consider using an EntityCreatedEvent<T> where T is the type of the actual entity that got created. For example, you can create the following listener definition to receive only EntityCreatedEvent for a Person : Java Kotlin @EventListener public void onPersonCreated(EntityCreatedEvent<Person> event) { // ... } @EventListener fun onPersonCreated(event: EntityCreatedEvent<Person>) { // ... } Due to type erasure, this works only if the event that is fired resolves the generic parameters on which the event listener filters (that is, something like class PersonCreatedEvent extends EntityCreatedEvent<Person> { …​ } ). In certain circumstances, this may become quite tedious if all events follow the same structure (as should be the case for the event in the preceding example). In such a case, you can implement ResolvableTypeProvider to guide the framework beyond what the runtime environment provides. The following event shows how to do so: Java Kotlin public class EntityCreatedEvent<T> extends ApplicationEvent implements ResolvableTypeProvider { public EntityCreatedEvent(T entity) { super(entity); } @Override public ResolvableType getResolvableType() { return ResolvableType.forClassWithGenerics(getClass(), ResolvableType.forInstance(getSource())); } } class EntityCreatedEvent<T>(entity: T) : ApplicationEvent(entity), ResolvableTypeProvider { override fun getResolvableType(): ResolvableType? { return ResolvableType.forClassWithGenerics(javaClass, ResolvableType.forInstance(getSource())) } } This works not only for ApplicationEvent but any arbitrary object that you send as an event. Finally, as with classic ApplicationListener implementations, the actual multicasting happens via a context-wide ApplicationEventMulticaster at runtime. By default, this is a SimpleApplicationEventMulticaster with synchronous event publication in the caller thread. This can be replaced/customized through an "applicationEventMulticaster" bean definition, for example, for processing all events asynchronously and/or for handling listener exceptions: @Bean ApplicationEventMulticaster applicationEventMulticaster() { SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster(); multicaster.setTaskExecutor(...); multicaster.setErrorHandler(...); return multicaster; } Convenient Access to Low-level Resources For optimal usage and understanding of application contexts, you should familiarize yourself with Spring’s Resource abstraction, as described in Resources . An application context is a ResourceLoader , which can be used to load Resource objects. A Resource is essentially a more feature rich version of the JDK java.net.URL class. In fact, implementations of Resource wrap an instance of java.net.URL , where appropriate. A Resource can obtain low-level resources from almost any location in a transparent fashion, including from the classpath, a filesystem location, anywhere describable with a standard URL, and some other variations. If the resource location string is a simple path without any special prefixes, where those resources come from is specific and appropriate to the actual application context type. You can configure a bean deployed into the application context to implement the special callback interface, ResourceLoaderAware , to be automatically called back at initialization time with the application context itself passed in as the ResourceLoader . You can also expose properties of type Resource , to be used to access static resources. They are injected into it like any other properties. You can specify those Resource properties as simple String paths and rely on automatic conversion from those text strings to actual Resource objects when the bean is deployed. The location path or paths supplied to an ApplicationContext constructor are actually resource strings and, in simple form, are treated appropriately according to the specific context implementation. For example ClassPathXmlApplicationContext treats a simple location path as a classpath location. You can also use location paths (resource strings) with special prefixes to force loading of definitions from the classpath or a URL, regardless of the actual context type. Application Startup Tracking The ApplicationContext manages the lifecycle of Spring applications and provides a rich programming model around components. As a result, complex applications can have equally complex component graphs and startup phases. Tracking the application startup steps with specific metrics can help understand where time is being spent during the startup phase, but it can also be used as a way to better understand the context lifecycle as a whole. The AbstractApplicationContext (and its subclasses) is instrumented with an ApplicationStartup , which collects StartupStep data about various startup phases: application context lifecycle (base packages scanning, config classes management) beans lifecycle (instantiation, smart initialization, post processing) application events processing Here is an example of instrumentation in the AnnotationConfigApplicationContext : Java Kotlin // create a startup step and start recording try (StartupStep scanPackages = getApplicationStartup().start("spring.context.base-packages.scan")) { // add tagging information to the current step scanPackages.tag("packages", () -> Arrays.toString(basePackages)); // perform the actual phase we're instrumenting this.scanner.scan(basePackages); } // create a startup step and start recording try (val scanPackages = getApplicationStartup().start("spring.context.base-packages.scan")) { // add tagging information to the current step scanPackages.tag("packages", () -> Arrays.toString(basePackages)); // perform the actual phase we're instrumenting this.scanner.scan(basePackages); } The application context is already instrumented with multiple steps. Once recorded, these startup steps can be collected, displayed and analyzed with specific tools. For a complete list of existing startup steps, you can check out the dedicated appendix section . The default ApplicationStartup implementation is a no-op variant, for minimal overhead. This means no metrics will be collected during application startup by default. Spring Framework ships with an implementation for tracking startup steps with Java Flight Recorder: FlightRecorderApplicationStartup . To use this variant, you must configure an instance of it to the ApplicationContext as soon as it’s been created. Developers can also use the ApplicationStartup infrastructure if they’re providing their own AbstractApplicationContext subclass, or if they wish to collect more precise data. ApplicationStartup is meant to be only used during application startup and for the core container; this is by no means a replacement for Java profilers or metrics libraries like Micrometer . To start collecting custom StartupStep , components can either get the ApplicationStartup instance from the application context directly, make their component implement ApplicationStartupAware , or ask for the ApplicationStartup type on any injection point. Developers should not use the "spring.*" namespace when creating custom startup steps. This namespace is reserved for internal Spring usage and is subject to change. Convenient ApplicationContext Instantiation for Web Applications You can create ApplicationContext instances declaratively by using, for example, a ContextLoader . Of course, you can also create ApplicationContext instances programmatically by using one of the ApplicationContext implementations. You can register an ApplicationContext by using the ContextLoaderListener , as the following example shows: <context-param> <param-name>contextConfigLocation</param-name> <param-value>/WEB-INF/daoContext.xml /WEB-INF/applicationContext.xml</param-value> </context-param> <listener> <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class> </listener> The listener inspects the contextConfigLocation parameter. If the parameter does not exist, the listener uses /WEB-INF/applicationContext.xml as a default. When the parameter does exist, the listener separates the String by using predefined delimiters (comma, semicolon, and whitespace) and uses the values as locations where application contexts are searched. Ant-style path patterns are supported as well. Examples are /WEB-INF/*Context.xml (for all files with names that end with Context.xml and that reside in the WEB-INF directory) and /WEB-INF/**/*Context.xml (for all such files in any subdirectory of WEB-INF ). Deploying a Spring ApplicationContext as a Jakarta EE RAR File It is possible to deploy a Spring ApplicationContext as a RAR file, encapsulating the context and all of its required bean classes and library JARs in a Jakarta EE RAR deployment unit. This is the equivalent of bootstrapping a stand-alone ApplicationContext (only hosted in Jakarta EE environment) being able to access the Jakarta EE servers facilities. RAR deployment is a more natural alternative to a scenario of deploying a headless WAR file — in effect, a WAR file without any HTTP entry points that is used only for bootstrapping a Spring ApplicationContext in a Jakarta EE environment. RAR deployment is ideal for application contexts that do not need HTTP entry points but rather consist only of message endpoints and scheduled jobs. Beans in such a context can use application server resources such as the JTA transaction manager and JNDI-bound JDBC DataSource instances and JMS ConnectionFactory instances and can also register with the platform’s JMX server — all through Spring’s standard transaction management and JNDI and JMX support facilities. Application components can also interact with the application server’s JCA WorkManager through Spring’s TaskExecutor abstraction. See the javadoc of the SpringContextResourceAdapter class for the configuration details involved in RAR deployment. For a simple deployment of a Spring ApplicationContext as a Jakarta EE RAR file: Package all application classes into a RAR file (which is a standard JAR file with a different file extension). Add all required library JARs into the root of the RAR archive. Add a META-INF/ra.xml deployment descriptor (as shown in the javadoc for SpringContextResourceAdapter ) and the corresponding Spring XML bean definition file(s) (typically META-INF/applicationContext.xml ). Drop the resulting RAR file into your application server’s deployment directory. Such RAR deployment units are usually self-contained. They do not expose components to the outside world, not even to other modules of the same application. Interaction with a RAR-based ApplicationContext usually occurs through JMS destinations that it shares with other modules. A RAR-based ApplicationContext may also, for example, schedule some jobs or react to new files in the file system (or the like). If it needs to allow synchronous access from the outside, it could (for example) export RMI endpoints, which may be used by other application modules on the same machine. Registering a LoadTimeWeaver The BeanFactory API Spring Framework Stable 7.0.8 6.2.19 Snapshot 7.1.0-SNAPSHOT 7.0.9-SNAPSHOT 6.2.20-SNAPSHOT Related Spring Documentation Spring Boot Spring Framework Spring Cloud Spring Cloud Build Spring Cloud Bus Spring Cloud Circuit Breaker Spring Cloud Commons Spring Cloud Config Spring Cloud Consul Spring Cloud Contract Spring Cloud Function Spring Cloud Gateway Spring Cloud Kubernetes Spring Cloud Netflix Spring Cloud OpenFeign Spring Cloud Stream Spring Cloud Task Spring Cloud Vault Spring Cloud Zookeeper Spring Data Spring Data Cassandra Spring Data Commons Spring Data Couchbase Spring Data Elasticsearch Spring Data JPA Spring Data KeyValue Spring Data LDAP Spring Data MongoDB Spring Data Neo4j Spring Data Redis Spring Data JDBC & R2DBC Spring Data REST Spring Integration Spring Batch Spring Security Spring Authorization Server Spring LDAP Spring Security Kerberos Spring Session Spring Vault Spring AI Spring AMQP Spring CLI Spring GraphQL Spring for Apache Kafka Spring Modulith Spring for Apache Pulsar Spring Shell All Docs... Copyright © 2005 - Broadcom. All Rights Reserved. The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries. Terms of Use • Privacy • Trademark Guidelines • Thank you • Your California Privacy Rights • Cookie Settings Apache®, Apache Tomcat®, Apache Kafka®, Apache Cassandra™, and Apache Geode™ are trademarks or registered trademarks of the Apache Software Foundation in the United States and/or other countries. Java™, Java™ SE, Java™ EE, and OpenJDK™ are trademarks of Oracle and/or its affiliates. Kubernetes® is a registered trademark of the Linux Foundation in the United States and other countries. Linux® is the registered trademark of Linus Torvalds in the United States and other countries. Windows® and Microsoft® Azure are registered trademarks of Microsoft Corporation. “AWS” and “Amazon Web Services” are trademarks or registered trademarks of Amazon.com Inc. or its affiliates. All other trademarks and copyrights are property of their respective owners and are only mentioned for informative purposes. Other names may be trademarks of their respective owners. Search in all Spring Docs
