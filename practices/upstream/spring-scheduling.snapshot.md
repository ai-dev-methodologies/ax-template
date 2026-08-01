# spring-scheduling — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://docs.spring.io/spring-framework/reference/integration/scheduling.html (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T02:24:31Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://docs.spring.io/spring-framework/reference/integration/scheduling.html`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r151`
**Body SHA-256 (below the `---` divider, header excluded):** cd52deacec5c152d3570d64903e12af1ced2828caf1d132e20e8d23d4038e784

---

---
snapshot_id: spring-scheduling
source: "https://docs.spring.io/spring-framework/reference/integration/scheduling.html"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 3
bytes: 40914
sha: "2995203acd38f08d1b9d84395c59e062ab99e31437781b24478c19aed88004f2"
---

# spring scheduling — upstream snapshot

Source: https://docs.spring.io/spring-framework/reference/integration/scheduling.html
Fetched: 2026-07-14

Task Execution and Scheduling :: Spring Framework
Edit this Page
 
 
 
 GitHub Project
 
 
 
 Stack Overflow

# Task Execution and Scheduling
The Spring Framework provides abstractions for the asynchronous execution and scheduling of
tasks with the TaskExecutor and TaskScheduler interfaces, respectively. Spring also
features implementations of those interfaces that support thread pools or delegation to
CommonJ within an application server environment. Ultimately, the use of these
implementations behind the common interfaces abstracts away the differences between
Java SE and Jakarta EE environments.
Spring also features integration classes to support scheduling with the
Quartz Scheduler.

## The Spring TaskExecutor Abstraction
Executors are the JDK name for the concept of thread pools. The “executor” naming is
due to the fact that there is no guarantee that the underlying implementation is
actually a pool. An executor may be single-threaded or even synchronous. Spring’s
abstraction hides implementation details between the Java SE and Jakarta EE environments.
Spring’s TaskExecutor interface is identical to the java.util.concurrent.Executor
interface. In fact, originally, its primary reason for existence was to abstract away
the need for Java 5 when using thread pools. The interface has a single method
(execute(Runnable task)) that accepts a task for execution based on the semantics
and configuration of the thread pool.
The TaskExecutor was originally created to give other Spring components an abstraction
for thread pooling where needed. Components such as the ApplicationEventMulticaster,
JMS’s AbstractMessageListenerContainer, and Quartz integration all use the
TaskExecutor abstraction to pool threads. However, if your beans need thread pooling
behavior, you can also use this abstraction for your own needs.

### TaskExecutor Types
Spring includes a number of pre-built implementations of TaskExecutor.
In all likelihood, you should never need to implement your own.
The variants that Spring provides are as follows:
SyncTaskExecutor:
This implementation does not run invocations asynchronously. Instead, each
invocation takes place in the calling thread. It is primarily used in situations
where multi-threading is not necessary, such as in simple test cases.
SimpleAsyncTaskExecutor:
This implementation does not reuse any threads. Rather, it starts up a new thread
for each invocation. However, it does support a concurrency limit that blocks
any invocations that are over the limit until a slot has been freed up. If you
are looking for true pooling, see ThreadPoolTaskExecutor, later in this list.
This will use JDK 21’s Virtual Threads, when the "virtualThreads"
option is enabled. This implementation also supports graceful shutdown through
Spring’s lifecycle management.
ConcurrentTaskExecutor:
This implementation is an adapter for a java.util.concurrent.Executor instance.
There is an alternative (ThreadPoolTaskExecutor) that exposes the Executor
configuration parameters as bean properties. There is rarely a need to use
ConcurrentTaskExecutor directly. However, if the ThreadPoolTaskExecutor is not
flexible enough for your needs, ConcurrentTaskExecutor is an alternative.
ThreadPoolTaskExecutor:
This implementation is most commonly used. It exposes bean properties for configuring
a java.util.concurrent.ThreadPoolExecutor and wraps it in a TaskExecutor.
If you need to adapt to a different kind of java.util.concurrent.Executor,
we recommend that you use a ConcurrentTaskExecutor instead.
It also provides a pause/resume capability and graceful shutdown through
Spring’s lifecycle management.
DefaultManagedTaskExecutor:
This implementation uses a JNDI-obtained ManagedExecutorService in a JSR-236
compatible runtime environment (such as a Jakarta EE application server),
replacing a CommonJ WorkManager for that purpose.

### Using a TaskExecutor
Spring’s TaskExecutor implementations are commonly used with dependency injection.
In the following example, we define a bean that uses the ThreadPoolTaskExecutor
to asynchronously print out a set of messages:
Java
Kotlin
public class TaskExecutorExample {

 private class MessagePrinterTask implements Runnable {

 private String message;

 public MessagePrinterTask(String message) {
 this.message = message;
 }

 public void run() {
 System.out.println(message);
 }
 }

 private TaskExecutor taskExecutor;

 public TaskExecutorExample(TaskExecutor taskExecutor) {
 this.taskExecutor = taskExecutor;
 }

 public void printMessages() {
 for(int i = 0; i < 25; i++) {
 taskExecutor.execute(new MessagePrinterTask("Message" + i));
 }
 }
}
class TaskExecutorExample(private val taskExecutor: TaskExecutor) {

 private inner class MessagePrinterTask(private val message: String) : Runnable {
 override fun run() {
 println(message)
 }
 }

 fun printMessages() {
 for (i in 0..24) {
 taskExecutor.execute(
 MessagePrinterTask(
 "Message$i"
 )
 )
 }
 }
}
As you can see, rather than retrieving a thread from the pool and executing it yourself,
you add your Runnable to the queue. Then the TaskExecutor uses its internal rules to
decide when the task gets run.
To configure the rules that the TaskExecutor uses, we expose simple bean properties:
Java
Kotlin
Xml
@Bean
ThreadPoolTaskExecutor taskExecutor() {
 ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
 taskExecutor.setCorePoolSize(5);
 taskExecutor.setMaxPoolSize(10);
 taskExecutor.setQueueCapacity(25);
 return taskExecutor;
}

@Bean
TaskExecutorExample taskExecutorExample(ThreadPoolTaskExecutor taskExecutor) {
 return new TaskExecutorExample(taskExecutor);
}
@Bean
fun taskExecutor() = ThreadPoolTaskExecutor().apply {
 corePoolSize = 5
 maxPoolSize = 10
 queueCapacity = 25
}

@Bean
fun taskExecutorExample(taskExecutor: ThreadPoolTaskExecutor) = TaskExecutorExample(taskExecutor)
Most TaskExecutor implementations provide a way to automatically wrap tasks submitted
with a TaskDecorator. Decorators should delegate to the task it is wrapping, possibly
implementing custom behavior before/after the execution of the task.
Let’s consider a simple implementation that will log messages before and after the execution
or our tasks:
Java
Kotlin
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.task.TaskDecorator;

public class LoggingTaskDecorator implements TaskDecorator {

 private static final Log logger = LogFactory.getLog(LoggingTaskDecorator.class);

 @Override
 public Runnable decorate(Runnable runnable) {
 return () -> {
 logger.debug("Before execution of " + runnable);
 runnable.run();
 logger.debug("After execution of " + runnable);
 };
 }
}
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.core.task.TaskDecorator

class LoggingTaskDecorator : TaskDecorator {

 override fun decorate(runnable: Runnable): Runnable {
 return Runnable {
 logger.debug("Before execution of $runnable")
 runnable.run()
 logger.debug("After execution of $runnable")
 }
 }

 companion object {
 private val logger: Log = LogFactory.getLog(
 LoggingTaskDecorator::class.java
 )
 }
}
We can then configure our decorator on a TaskExecutor instance:
Java
Kotlin
Xml
@Bean
ThreadPoolTaskExecutor decoratedTaskExecutor() {
 ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
 taskExecutor.setTaskDecorator(new LoggingTaskDecorator());
 return taskExecutor;
}
@Bean
fun decoratedTaskExecutor() = ThreadPoolTaskExecutor().apply {
 setTaskDecorator(LoggingTaskDecorator())
}
In case multiple decorators are needed, the org.springframework.core.task.support.CompositeTaskDecorator
can be used to execute sequentially multiple decorators.

## The Spring TaskScheduler Abstraction
In addition to the TaskExecutor abstraction, Spring has a TaskScheduler SPI with a
variety of methods for scheduling tasks to run at some point in the future. The following
listing shows the TaskScheduler interface definition:
public interface TaskScheduler {

 Clock getClock();

 ScheduledFuture schedule(Runnable task, Trigger trigger);

 ScheduledFuture schedule(Runnable task, Instant startTime);

 ScheduledFuture scheduleAtFixedRate(Runnable task, Instant startTime, Duration period);

 ScheduledFuture scheduleAtFixedRate(Runnable task, Duration period);

 ScheduledFuture scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay);

 ScheduledFuture scheduleWithFixedDelay(Runnable task, Duration delay);
The simplest method is the one named schedule that takes only a Runnable and an Instant.
That causes the task to run once after the specified time. All of the other methods
are capable of scheduling tasks to run repeatedly. The fixed-rate and fixed-delay
methods are for simple, periodic execution, but the method that accepts a Trigger is
much more flexible.

### Trigger Interface
The Trigger interface is essentially inspired by JSR-236. The basic idea of the
Trigger is that execution times may be determined based on past execution outcomes or
even arbitrary conditions. If these determinations take into account the outcome of the
preceding execution, that information is available within a TriggerContext. The
Trigger interface itself is quite simple, as the following listing shows:
public interface Trigger {

 Instant nextExecution(TriggerContext triggerContext);
}
The TriggerContext is the most important part. It encapsulates all of
the relevant data and is open for extension in the future, if necessary. The
TriggerContext is an interface (a SimpleTriggerContext implementation is used by
default). The following listing shows the available methods for Trigger implementations.
public interface TriggerContext {

 Clock getClock();

 Instant lastScheduledExecution();

 Instant lastActualExecution();

 Instant lastCompletion();
}

### Trigger Implementations
Spring provides two implementations of the Trigger interface. The most interesting one
is the CronTrigger. It enables the scheduling of tasks based on
cron expressions.
For example, the following task is scheduled to run 15 minutes past each hour but only
during the 9-to-5 "business hours" on weekdays:
scheduler.schedule(task, new CronTrigger("0 15 9-17 * * MON-FRI"));
The other implementation is a PeriodicTrigger that accepts a fixed
period, an optional initial delay value, and a boolean to indicate whether the period
should be interpreted as a fixed-rate or a fixed-delay. Since the TaskScheduler
interface already defines methods for scheduling tasks at a fixed rate or with a
fixed delay, those methods should be used directly whenever possible. The value of the
PeriodicTrigger implementation is that you can use it within components that rely on
the Trigger abstraction. For example, it may be convenient to allow periodic triggers,
cron-based triggers, and even custom trigger implementations to be used interchangeably.
Such a component could take advantage of dependency injection so that you can configure
such Triggers externally and, therefore, easily modify or extend them.

### TaskScheduler implementations
As with Spring’s TaskExecutor abstraction, the primary benefit of the TaskScheduler
arrangement is that an application’s scheduling needs are decoupled from the deployment
environment. This abstraction level is particularly relevant when deploying to an
application server environment where threads should not be created directly by the
application itself. For such scenarios, Spring provides a DefaultManagedTaskScheduler
that delegates to a JSR-236 ManagedScheduledExecutorService in a Jakarta EE environment.
Whenever external thread management is not a requirement, a simpler alternative is
a local ScheduledExecutorService setup within the application, which can be adapted
through Spring’s ConcurrentTaskScheduler. As a convenience, Spring also provides a
ThreadPoolTaskScheduler, which internally delegates to a ScheduledExecutorService
to provide common bean-style configuration along the lines of ThreadPoolTaskExecutor.
These variants work perfectly fine for locally embedded thread pool setups in lenient
application server environments, as well — in particular on Tomcat and Jetty.
As of 6.1, ThreadPoolTaskScheduler provides a pause/resume capability and graceful
shutdown through Spring’s lifecycle management. There is also a new option called
SimpleAsyncTaskScheduler which is aligned with JDK 21’s Virtual Threads, using a
single scheduler thread but firing up a new thread for every scheduled task execution
(except for fixed-delay tasks which all operate on a single scheduler thread, so for
this virtual-thread-aligned option, fixed rates and cron triggers are recommended).

## Annotation Support for Scheduling and Asynchronous Execution
Spring provides annotation support for both task scheduling and asynchronous method
execution.

### Enable Scheduling Annotations
To enable support for @Scheduled and @Async annotations, you can add @EnableScheduling
and @EnableAsync to one of your @Configuration classes, or element,
as the following example shows:
Java
Kotlin
Xml
@Configuration
@EnableAsync
@EnableScheduling
public class SchedulingConfiguration {
}
@Configuration
@EnableAsync
@EnableScheduling
class SchedulingConfiguration
You can pick and choose the relevant annotations for your application. For example,
if you need only support for @Scheduled, you can omit @EnableAsync. For more
fine-grained control, you can additionally implement the SchedulingConfigurer
interface, the AsyncConfigurer interface, or both. See the
SchedulingConfigurer
and AsyncConfigurer
javadoc for full details.
Note that, with the preceding XML, an executor reference is provided for handling those
tasks that correspond to methods with the @Async annotation, and the scheduler
reference is provided for managing those methods annotated with @Scheduled.
The default advice mode for processing @Async annotations is proxy which allows
for interception of calls through the proxy only. Local calls within the same class
cannot get intercepted that way. For a more advanced mode of interception, consider
switching to aspectj mode in combination with compile-time or load-time weaving.

### The @Scheduled annotation
You can add the @Scheduled annotation to a method, along with trigger metadata. For
example, the following method is invoked every five seconds (5000 milliseconds) with a
fixed delay, meaning that the period is measured from the completion time of each
preceding invocation.
@Scheduled(fixedDelay = 5000)
public void doSomething() {
 // something that should run periodically
}
By default, milliseconds will be used as the time unit for fixed delay, fixed rate, and
initial delay values. If you would like to use a different time unit such as seconds or
minutes, you can configure this via the timeUnit attribute in @Scheduled.
For example, the previous example can also be written as follows.
@Scheduled(fixedDelay = 5, timeUnit = TimeUnit.SECONDS)
public void doSomething() {
 // something that should run periodically
}
If you need a fixed-rate execution, you can use the fixedRate attribute within the
annotation. The following method is invoked every five seconds (measured between the
successive start times of each invocation):
@Scheduled(fixedRate = 5, timeUnit = TimeUnit.SECONDS)
public void doSomething() {
 // something that should run periodically
}
For fixed-delay and fixed-rate tasks, you can specify an initial delay by indicating
the amount of time to wait before the first execution of the method, as the following
fixedRate example shows:
@Scheduled(initialDelay = 1000, fixedRate = 5000)
public void doSomething() {
 // something that should run periodically
}
For one-time tasks, you can just specify an initial delay by indicating the amount
of time to wait before the intended execution of the method:
@Scheduled(initialDelay = 1000)
public void doSomething() {
 // something that should run only once
}
If simple periodic scheduling is not expressive enough, you can provide a
cron expression.
The following example runs only on weekdays:
@Scheduled(cron="*/5 * * * * MON-FRI")
public void doSomething() {
 // something that should run on weekdays only
}
You can also use the zone attribute to specify the time zone in which the cron
expression is resolved.
Notice that the methods to be scheduled must have void returns and must not accept any
arguments. If the method needs to interact with other objects from the application
context, those would typically have been provided through dependency injection.
@Scheduled can be used as a repeatable annotation. If several scheduled declarations
are found on the same method, each of them will be processed independently, with a
separate trigger firing for each of them. As a consequence, such co-located schedules
may overlap and execute multiple times in parallel or in immediate succession.
Please make sure that your specified cron expressions etc do not accidentally overlap.
As of Spring Framework 4.3, @Scheduled methods are supported on beans of any scope.
Make sure that you are not initializing multiple instances of the same @Scheduled
annotation class at runtime, unless you do want to schedule callbacks to each such
instance. Related to this, make sure that you do not use @Configurable on bean
classes that are annotated with @Scheduled and registered as regular Spring beans
with the container. Otherwise, you would get double initialization (once through the
container and once through the @Configurable aspect), with the consequence of each
@Scheduled method being invoked twice.

### The @Scheduled annotation on Reactive methods or Kotlin suspending functions
As of Spring Framework 6.1, @Scheduled methods are also supported on several types
of reactive methods:
methods with a Publisher return type (or any concrete implementation of Publisher)
like in the following example:
@Scheduled(fixedDelay = 500)
public Publisher reactiveSomething() {
 // return an instance of Publisher
}
methods with a return type that can be adapted to Publisher via the shared instance
of the ReactiveAdapterRegistry, provided the type supports deferred subscription like
in the following example:
@Scheduled(fixedDelay = 500)
public Single rxjavaNonPublisher() {
 return Single.just("example");
}
The CompletableFuture class is an example of a type that can typically be adapted
to Publisher but doesn’t support deferred subscription. Its ReactiveAdapter in the
registry denotes that by having the getDescriptor().isDeferred() method return false.
Kotlin suspending functions, like in the following example:
@Scheduled(fixedDelay = 500)
suspend fun something() {
 // do something asynchronous
}
methods that return a Kotlin Flow or Deferred instance, like in the following example:
@Scheduled(fixedDelay = 500)
fun something(): Flow {
 flow {
 // do something asynchronous
 }
}
All these types of methods must be declared without any arguments. In the case of Kotlin
suspending functions, the kotlinx.coroutines.reactor bridge must also be present to allow
the framework to invoke a suspending function as a Publisher.
The Spring Framework will obtain a Publisher for the annotated method once and will
schedule a Runnable in which it subscribes to said Publisher. These inner regular
subscriptions occur according to the corresponding cron/fixedDelay/fixedRate configuration.
If the Publisher emits onNext signal(s), these are ignored and discarded (the same way
return values from synchronous @Scheduled methods are ignored).
In the following example, the Flux emits onNext("Hello"), onNext("World") every 5
seconds, but these values are unused:
@Scheduled(initialDelay = 5000, fixedRate = 5000)
public Flux reactiveSomething() {
 return Flux.just("Hello", "World");
}
If the Publisher emits an onError signal, it is logged at WARN level and recovered.
Because of the asynchronous and lazy nature of Publisher instances, exceptions are
not thrown from the Runnable task: this means that the ErrorHandler contract is not
involved for reactive methods.
As a result, further scheduled subscription occurs despite the error.
In the following example, the Mono subscription fails twice in the first five seconds.
Then subscriptions start succeeding, printing a message to the standard output every five
seconds:
@Scheduled(initialDelay = 0, fixedRate = 5000)
public Mono reactiveSomething() {
 AtomicInteger countdown = new AtomicInteger(2);

 return Mono.defer(() -> {
 if (countDown.get() == 0 || countDown.decrementAndGet() == 0) {
 return Mono.fromRunnable(() -> System.out.println("Message"));
 }
 return Mono.error(new IllegalStateException("Cannot deliver message"));
 })
}
When destroying the annotated bean or closing the application context, Spring Framework cancels
scheduled tasks, which includes the next scheduled subscription to the Publisher as well
as any past subscription that is still currently active (for example, for long-running publishers
or even infinite publishers).

### The @Async annotation
You can provide the @Async annotation on a method so that invocation of that method
occurs asynchronously. In other words, the caller returns immediately upon
invocation, while the actual execution of the method occurs in a task that has been
submitted to a Spring TaskExecutor. In the simplest case, you can apply the annotation
to a method that returns void, as the following example shows:
@Async
void doSomething() {
 // this will be run asynchronously
}
Unlike the methods annotated with the @Scheduled annotation, these methods can expect
arguments, because they are invoked in the “normal” way by callers at runtime rather
than from a scheduled task being managed by the container. For example, the following
code is a legitimate application of the @Async annotation:
@Async
void doSomething(String s) {
 // this will be run asynchronously
}
Even methods that return a value can be invoked asynchronously. However, such methods
are required to have a Future-typed return value. This still provides the benefit of
asynchronous execution so that the caller can perform other tasks prior to calling
get() on that Future. The following example shows how to use @Async on a method
that returns a value:
@Async
Future returnSomething(int i) {
 // this will be run asynchronously
}
@Async methods may not only declare a regular java.util.concurrent.Future return
type but also java.util.concurrent.CompletableFuture, for richer interaction with
the asynchronous task and for immediate composition with further processing steps.
You can not use @Async in conjunction with lifecycle callbacks such as @PostConstruct.
To asynchronously initialize Spring beans, you currently have to use a separate
initializing Spring bean that then invokes the @Async annotated method on the target,
as the following example shows:
public class SampleBeanImpl implements SampleBean {

 @Async
 void doSomething() {
 // ...
 }

}

public class SampleBeanInitializer {

 private final SampleBean bean;

 public SampleBeanInitializer(SampleBean bean) {
 this.bean = bean;
 }

 @PostConstruct
 public void initialize() {
 bean.doSomething();
 }

}
There is no direct XML equivalent for @Async, since such methods should be designed
for asynchronous execution in the first place, not externally re-declared to be asynchronous.
However, you can manually set up Spring’s AsyncExecutionInterceptor with Spring AOP,
in combination with a custom pointcut.

### Executor Qualification with @Async
By default, when specifying @Async on a method, the executor that is used is the
one configured when enabling async support,
i.e. the “annotation-driven” element if you are using XML or your AsyncConfigurer
implementation, if any. However, you can use the value attribute of the @Async
annotation when you need to indicate that an executor other than the default should be
used when executing a given method. The following example shows how to do so:
@Async("otherExecutor")
void doSomething(String s) {
 // this will be run asynchronously by "otherExecutor"
}
In this case, "otherExecutor" can be the name of any Executor bean in the Spring
container, or it may be the name of a qualifier associated with any Executor (for example,
as specified with the element or Spring’s @Qualifier annotation).

### Exception Management with @Async
When an @Async method has a Future-typed return value, it is easy to manage
an exception that was thrown during the method execution, as this exception is
thrown when calling get on the Future result. With a void return type,
however, the exception is uncaught and cannot be transmitted. You can provide an
AsyncUncaughtExceptionHandler to handle such exceptions. The following example shows
how to do so:
public class MyAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {

 @Override
 public void handleUncaughtException(Throwable ex, Method method, Object... params) {
 // handle exception
 }
}
By default, the exception is merely logged. You can define a custom AsyncUncaughtExceptionHandler
by using AsyncConfigurer or the XML element.

## The task Namespace
As of version 3.0, Spring includes an XML namespace for configuring TaskExecutor and
TaskScheduler instances. It also provides a convenient way to configure tasks to be
scheduled with a trigger.

### The scheduler Element
The following element creates a ThreadPoolTaskScheduler instance with the
specified thread pool size:
The value provided for the id attribute is used as the prefix for thread names
within the pool. The scheduler element is relatively straightforward. If you do not
provide a pool-size attribute, the default thread pool has only a single thread.
There are no other configuration options for the scheduler.

### The executor Element
The following creates a ThreadPoolTaskExecutor instance:
As with the scheduler shown in the previous section,
the value provided for the id attribute is used as the prefix for thread names within
the pool. As far as the pool size is concerned, the executor element supports more
configuration options than the scheduler element. For one thing, the thread pool for
a ThreadPoolTaskExecutor is itself more configurable. Rather than only a single size,
an executor’s thread pool can have different values for the core and the max size.
If you provide a single value, the executor has a fixed-size thread pool (the core and
max sizes are the same). However, the executor element’s pool-size attribute also
accepts a range in the form of min-max. The following example sets a minimum value of
5 and a maximum value of 25:
In the preceding configuration, a queue-capacity value has also been provided.
The configuration of the thread pool should also be considered in light of the
executor’s queue capacity. For the full description of the relationship between pool
size and queue capacity, see the documentation for
ThreadPoolExecutor.
The main idea is that, when a task is submitted, the executor first tries to use a
free thread if the number of active threads is currently less than the core size.
If the core size has been reached, the task is added to the queue, as long as its
capacity has not yet been reached. Only then, if the queue’s capacity has been
reached, does the executor create a new thread beyond the core size. If the max size
has also been reached, then the executor rejects the task.
By default, the queue is unbounded, but this is rarely the desired configuration,
because it can lead to OutOfMemoryError if enough tasks are added to that queue while
all pool threads are busy. Furthermore, if the queue is unbounded, the max size has
no effect at all. Since the executor always tries the queue before creating a new
thread beyond the core size, a queue must have a finite capacity for the thread pool to
grow beyond the core size (this is why a fixed-size pool is the only sensible case
when using an unbounded queue).
Consider the case, as mentioned above, when a task is rejected. By default, when a
task is rejected, a thread pool executor throws a TaskRejectedException. However,
the rejection policy is actually configurable. The exception is thrown when using
the default rejection policy, which is the AbortPolicy implementation.
For applications where some tasks can be skipped under heavy load, you can instead
configure either DiscardPolicy or DiscardOldestPolicy. Another option that works
well for applications that need to throttle the submitted tasks under heavy load is
the CallerRunsPolicy. Instead of throwing an exception or discarding tasks,
that policy forces the thread that is calling the submit method to run the task itself.
The idea is that such a caller is busy while running that task and not able to submit
other tasks immediately. Therefore, it provides a simple way to throttle the incoming
load while maintaining the limits of the thread pool and queue. Typically, this allows
the executor to “catch up” on the tasks it is handling and thereby frees up some
capacity on the queue, in the pool, or both. You can choose any of these options from an
enumeration of values available for the rejection-policy attribute on the executor
element.
The following example shows an executor element with a number of attributes to specify
various behaviors:
Finally, the keep-alive setting determines the time limit (in seconds) for which threads
may remain idle before being stopped. If there are more than the core number of threads
currently in the pool, after waiting this amount of time without processing a task, excess
threads get stopped. A time value of zero causes excess threads to stop
immediately after executing a task without remaining follow-up work in the task queue.
The following example sets the keep-alive value to two minutes:

### The scheduled-tasks Element
The most powerful feature of Spring’s task namespace is the support for configuring
tasks to be scheduled within a Spring Application Context. This follows an approach
similar to other “method-invokers” in Spring, such as that provided by the JMS namespace
for configuring message-driven POJOs. Basically, a ref attribute can point to any
Spring-managed object, and the method attribute provides the name of a method to be
invoked on that object. The following listing shows a simple example:
The scheduler is referenced by the outer element, and each individual
task includes the configuration of its trigger metadata. In the preceding example,
that metadata defines a periodic trigger with a fixed delay indicating the number of
milliseconds to wait after each task execution has completed. Another option is
fixed-rate, indicating how often the method should be run regardless of how long
any previous execution takes. Additionally, for both fixed-delay and fixed-rate
tasks, you can specify an 'initial-delay' parameter, indicating the number of
milliseconds to wait before the first execution of the method. For more control,
you can instead provide a cron attribute to provide a
cron expression.
The following example shows these other options:

## Cron Expressions
All Spring cron expressions have to conform to the same format, whether you are using them in
@Scheduled annotations,
task:scheduled-tasks elements,
or someplace else. A well-formed cron expression, such as * * * * * *, consists of six
space-separated time and date fields, each with its own range of valid values:
┌───────────── second (0-59)
 │ ┌───────────── minute (0 - 59)
 │ │ ┌───────────── hour (0 - 23)
 │ │ │ ┌───────────── day of the month (1 - 31)
 │ │ │ │ ┌───────────── month (1 - 12) (or JAN-DEC)
 │ │ │ │ │ ┌───────────── day of the week (0 - 7)
 │ │ │ │ │ │ (0 or 7 is Sunday, or MON-SUN)
 │ │ │ │ │ │
 * * * * * *
There are some rules that apply:
A field may be an asterisk (*), which always stands for “first-last”.
For the day-of-the-month or day-of-the-week fields, a question mark (?) may be used instead of an
asterisk.
Commas (,) are used to separate items of a list.
Two numbers separated with a hyphen (-) express a range of numbers.
The specified range is inclusive.
Following a range (or *) with / specifies the interval of the number’s value through the range.
English names can also be used for the month and day-of-week fields.
Use the first three letters of the particular day or month (case does not matter).
The day-of-month and day-of-week fields can contain an L character, which has a different meaning.
In the day-of-month field, L stands for the last day of the month.
If followed by a negative offset (that is, L-n), it means nth-to-last day of the month.
In the day-of-week field, L stands for the last day of the week.
If prefixed by a number or three-letter name (dL or DDDL), it means the last day of week (d
or DDD) in the month.
The day-of-month field can be nW, which stands for the nearest weekday to day of the month n.
If n falls on Saturday, this yields the Friday before it.
If n falls on Sunday, this yields the Monday after, which also happens if n is 1 and falls on
a Saturday (that is: 1W stands for the first weekday of the month).
If the day-of-month field is LW, it means the last weekday of the month.
The day-of-week field can be d#n (or DDD#n), which stands for the nth day of week d
(or DDD) in the month.
Here are some examples:
Cron Expression
Meaning
0 0 * * * *
top of every hour of every day
*/10 * * * * *
every ten seconds
0 0 8-10 * * *
8, 9 and 10 o’clock of every day
0 0 6,19 * * *
6:00 AM and 7:00 PM every day
0 0/30 8-10 * * *
8:00, 8:30, 9:00, 9:30, 10:00 and 10:30 every day
0 0 9-17 * * MON-FRI
on the hour nine-to-five weekdays
0 0 0 25 DEC ?
every Christmas Day at midnight
0 0 0 L * *
last day of the month at midnight
0 0 0 L-3 * *
third-to-last day of the month at midnight
0 0 0 * * 5L
last Friday of the month at midnight
0 0 0 * * THUL
last Thursday of the month at midnight
0 0 0 1W * *
first weekday of the month at midnight
0 0 0 LW * *
last weekday of the month at midnight
0 0 0 ? * 5#2
the second Friday in the month at midnight
0 0 0 ? * MON#1
the first Monday in the month at midnight

### Macros
Expressions such as 0 0 * * * * are hard for humans to parse and are, therefore,
hard to fix in case of bugs. To improve readability, Spring supports the following
macros, which represent commonly used sequences. You can use these macros instead
of the six-digit value, thus: @Scheduled(cron = "@hourly").
Macro
Meaning
@yearly (or @annually)
once a year (0 0 0 1 1 *)
@monthly
once a month (0 0 0 1 * *)
@weekly
once a week (0 0 0 * * 0)
@daily (or @midnight)
once a day (0 0 0 * * *), or
@hourly
once an hour, (0 0 * * * *)

## Using the Quartz Scheduler
Quartz uses Trigger, Job, and JobDetail objects to realize scheduling of all
kinds of jobs. For the basic concepts behind Quartz, see the
Quartz Web site. For convenience purposes, Spring
offers a couple of classes that simplify using Quartz within Spring-based applications.

### Using the JobDetailFactoryBean
Quartz JobDetail objects contain all the information needed to run a job. Spring
provides a JobDetailFactoryBean, which provides bean-style properties for XML
configuration purposes. Consider the following example:
The job detail configuration has all the information it needs to run the job (ExampleJob).
The timeout is specified in the job data map. The job data map is available through the
JobExecutionContext (passed to you at execution time), but the JobDetail also gets
its properties from the job data mapped to properties of the job instance. So, in the
following example, the ExampleJob contains a bean property named timeout, and the
JobDetail has it applied automatically:
package example;

public class ExampleJob extends QuartzJobBean {

 private int timeout;

 /**
 * Setter called after the ExampleJob is instantiated
 * with the value from the JobDetailFactoryBean.
 */
 public void setTimeout(int timeout) {
 this.timeout = timeout;
 }

 protected void executeInternal(JobExecutionContext ctx) throws JobExecutionException {
 // do the actual work
 }
}
All additional properties from the job data map are available to you as well.
By using the name and group properties, you can modify the name and the group
of the job, respectively. By default, the name of the job matches the bean name
of the JobDetailFactoryBean (exampleJob in the preceding example above).

### Using the MethodInvokingJobDetailFactoryBean
Often you merely need to invoke a method on a specific object. By using the
MethodInvokingJobDetailFactoryBean, you can do exactly this, as the following example shows:
The preceding example results in the doIt method being called on the
exampleBusinessObject method, as the following example shows:
public class ExampleBusinessObject {

 // properties and collaborators

 public void doIt() {
 // do the actual work
 }
}
By using the MethodInvokingJobDetailFactoryBean, you need not create one-line jobs
that merely invoke a method. You need only create the actual business object and
wire up the detail object.
By default, Quartz Jobs are stateless, resulting in the possibility of jobs interfering
with each other. If you specify two triggers for the same JobDetail, it is possible
that the second one starts before the first job has finished. If JobDetail classes
implement the Stateful interface, this does not happen: the second job does not start
before the first one has finished.
To make jobs resulting from the MethodInvokingJobDetailFactoryBean be non-concurrent,
set the concurrent flag to false, as the following example shows:
By default, jobs will run in a concurrent fashion.

### Wiring up Jobs by Using Triggers and SchedulerFactoryBean
We have created job details and jobs. We have also reviewed the convenience bean that
lets you invoke a method on a specific object. Of course, we still need to schedule the
jobs themselves. This is done by using triggers and a SchedulerFactoryBean. Several
triggers are available within Quartz, and Spring offers two Quartz FactoryBean
implementations with convenient defaults: CronTriggerFactoryBean and
SimpleTriggerFactoryBean.
Triggers need to be scheduled. Spring offers a SchedulerFactoryBean that exposes
triggers to be set as properties. SchedulerFactoryBean schedules the actual jobs with
those triggers.
The following listing uses both a SimpleTriggerFactoryBean and a CronTriggerFactoryBean:
The preceding example sets up two triggers, one running every 50 seconds with a starting
delay of 10 seconds and one running every morning at 6 AM. To finalize everything,
we need to set up the SchedulerFactoryBean, as the following example shows:
More properties are available for the SchedulerFactoryBean, such as the calendars used by the
job details, properties to customize Quartz with, and a Spring-provided JDBC DataSource. See
the SchedulerFactoryBean
javadoc for more information.
SchedulerFactoryBean also recognizes a quartz.properties file in the classpath,
based on Quartz property keys, as with regular Quartz configuration. Please note that many
SchedulerFactoryBean settings interact with common Quartz settings in the properties file;
it is therefore not recommended to specify values at both levels. For example, do not set
an "org.quartz.jobStore.class" property if you mean to rely on a Spring-provided DataSource,
or specify an org.springframework.scheduling.quartz.LocalDataSourceJobStore variant which
is a full-fledged replacement for the standard org.quartz.impl.jdbcjobstore.JobStoreTX.
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

Source: https://docs.spring.io/spring-framework/reference/integration/scheduling.html
HTTP status: 200 · extracted bytes: 56305 · sha256: dc923a23e58043a4cef31a65e701f6d61af6904fbf00bf1926f69be163bba588
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r151`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

Task Execution and Scheduling :: Spring Framework Why Spring Overview Trending Generative AI Cloud Architecture Patterns Microservices Reactive Event Driven Application Types Web Applications Serverless Batch Learn Getting Started Quickstart Guides Academy Courses Get Certified Projects Overview Projects Spring Boot Spring Framework Spring Cloud Spring AI Spring Data Spring Integration Spring Batch Spring Security Foundational Projects Micrometer Reactor Development Tools Spring Tools Spring Initializr Resources Blog Release Calendar Version Mappings Release Highlights Security Advisories GitHub Orgs Spring Projects Spring Cloud Community Overview Events Authors Enterprise Overview Long-term Support Automated Upgrades Governance and Compliance Modern App Development light Spring Framework 7.0.8 Search Overview Core Technologies The IoC Container Introduction to the Spring IoC Container and Beans Container Overview Bean Overview Dependencies Dependency Injection Dependencies and Configuration in Detail Using depends-on Lazy-initialized Beans Autowiring Collaborators Method Injection Bean Scopes Customizing the Nature of a Bean Bean Definition Inheritance Container Extension Points Annotation-based Container Configuration Using @Autowired Fine-tuning Annotation-based Autowiring with @Primary or @Fallback Fine-tuning Annotation-based Autowiring with Qualifiers Using Generics as Autowiring Qualifiers Using CustomAutowireConfigurer Injection with @Resource Using @Value Using @PostConstruct and @PreDestroy Classpath Scanning and Managed Components Using JSR-330 Standard Annotations Java-based Container Configuration Basic Concepts: @Bean and @Configuration Instantiating the Spring Container by Using AnnotationConfigApplicationContext Using the @Bean Annotation Using the @Configuration annotation Composing Java-based Configurations Programmatic Bean Registration Environment Abstraction Registering a LoadTimeWeaver Additional Capabilities of the ApplicationContext The BeanFactory API Resources Validation, Data Binding, and Type Conversion Validation Using Spring’s Validator Interface Data Binding Resolving Error Codes to Error Messages Spring Type Conversion Spring Field Formatting Configuring a Global Date and Time Format Java Bean Validation Spring Expression Language (SpEL) Evaluation Expressions in Bean Definitions Language Reference Literal Expressions Properties, Arrays, Lists, Maps, and Indexers Inline Lists Inline Maps Array Construction Methods Operators Types Constructors Variables Functions Varargs Invocations Bean References Ternary Operator (If-Then-Else) The Elvis Operator Safe Navigation Operator Collection Selection Collection Projection Expression Templating Classes Used in the Examples Aspect Oriented Programming with Spring AOP Concepts Spring AOP Capabilities and Goals AOP Proxies @AspectJ support Enabling @AspectJ Support Declaring an Aspect Declaring a Pointcut Declaring Advice Introductions Aspect Instantiation Models An AOP Example Schema-based AOP Support Choosing which AOP Declaration Style to Use Mixing Aspect Types Proxying Mechanisms Programmatic Creation of @AspectJ Proxies Using AspectJ with Spring Applications Further Resources Spring AOP APIs Pointcut API in Spring Advice API in Spring The Advisor API in Spring Using the ProxyFactoryBean to Create AOP Proxies Concise Proxy Definitions Creating AOP Proxies Programmatically with the ProxyFactory Manipulating Advised Objects Using the "auto-proxy" facility Using TargetSource Implementations Defining New Advice Types Resilience Features Null-safety Data Buffers and Codecs Ahead of Time Optimizations Appendix XML Schemas XML Schema Authoring Application Startup Steps Data Access Transaction Management Advantages of the Spring Framework’s Transaction Support Model Understanding the Spring Framework Transaction Abstraction Synchronizing Resources with Transactions Declarative Transaction Management Understanding the Spring Framework’s Declarative Transaction Implementation Example of Declarative Transaction Implementation Rolling Back a Declarative Transaction Configuring Different Transactional Semantics for Different Beans <tx:advice/> Settings Using @Transactional Transaction Propagation Advising Transactional Operations Using @Transactional with AspectJ Programmatic Transaction Management Choosing Between Programmatic and Declarative Transaction Management Transaction-bound Events Application server-specific integration Solutions to Common Problems Further Resources DAO Support Data Access with JDBC Choosing an Approach for JDBC Database Access Package Hierarchy Using the JDBC Core Classes to Control Basic JDBC Processing and Error Handling Controlling Database Connections JDBC Batch Operations Simplifying JDBC Operations with the SimpleJdbc Classes Modeling JDBC Operations as Java Objects Common Problems with Parameter and Data Value Handling Embedded Database Support Initializing a DataSource Data Access with R2DBC Object Relational Mapping (ORM) Data Access Introduction to ORM with Spring General ORM Integration Considerations Hibernate JPA Marshalling XML by Using Object-XML Mappers Appendix Web on Servlet Stack Spring Web MVC DispatcherServlet Context Hierarchy Special Bean Types Web MVC Config Servlet Config Processing Path Matching Interception Exceptions View Resolution Locale Multipart Resolver Logging Filters HTTP Message Conversion Annotated Controllers Declaration Mapping Requests Handler Methods Method Arguments Return Values Type Conversion Matrix Variables @RequestParam @RequestHeader @CookieValue @ModelAttribute @SessionAttributes @SessionAttribute @RequestAttribute Redirect Attributes Flash Attributes Multipart @RequestBody HttpEntity @ResponseBody ResponseEntity Jackson JSON Model @InitBinder Validation Exceptions Controller Advice Functional Endpoints URI Links Asynchronous Requests Range Requests Data Binding CORS API Versioning Error Responses Web Security HTTP Caching View Technologies Thymeleaf FreeMarker Groovy Markup Script Views HTML Fragments JSP and JSTL RSS and Atom PDF and Excel Jackson XML Marshalling XSLT Views MVC Config Enable MVC Configuration MVC Config API Type Conversion Validation Interceptors Content Types Message Converters View Controllers View Resolvers Static Resources Default Servlet Path Matching API Version Advanced Java Config Advanced XML Config HTTP/2 REST Clients Testing WebSockets WebSocket API SockJS Fallback STOMP Overview Benefits Enable STOMP WebSocket Transport Flow of Messages Annotated Controllers Sending Messages Simple Broker External Broker Connecting to a Broker Dots as Separators Authentication Token Authentication Authorization User Destinations Order of Messages Events Interception STOMP Client WebSocket Scope Performance Monitoring Testing Web on Reactive Stack Spring WebFlux Overview Reactive Core DispatcherHandler Annotated Controllers @Controller Mapping Requests Handler Methods Method Arguments Return Values Type Conversion Matrix Variables @RequestParam @RequestHeader @CookieValue @ModelAttribute @SessionAttributes @SessionAttribute @RequestAttribute Multipart Content @RequestBody HttpEntity @ResponseBody ResponseEntity Jackson JSON Model DataBinder Validation Exceptions Controller Advice Functional Endpoints URI Links Range Requests Data Binding CORS API Versioning Error Responses Web Security HTTP Caching View Technologies WebFlux Config HTTP/2 WebClient Configuration retrieve() Exchange Request Body Filters Attributes Context Synchronous Use Testing HTTP Service Client WebSockets Testing RSocket Reactive Libraries Testing Introduction to Spring Testing Unit Testing Integration Testing JDBC Testing Support Spring TestContext Framework Key Abstractions Bootstrapping the TestContext Framework TestExecutionListener Configuration Application Events Test Execution Events Context Management Context Configuration with Component Classes Context Configuration with XML Resources Context Configuration with Groovy Scripts Default Context Configuration Mixing Component Classes, XML, and Groovy Scripts Context Configuration with Context Customizers Context Configuration with Context Initializers Context Configuration Inheritance Context Configuration with Environment Profiles Context Configuration with Test Property Sources Context Configuration with Dynamic Property Sources Loading a WebApplicationContext Working with Web Mocks Context Caching Context Pausing Context Failure Threshold Context Hierarchies Dependency Injection of Test Fixtures Bean Overriding in Tests Testing Request- and Session-scoped Beans Transaction Management Executing SQL Scripts Parallel Test Execution TestContext Framework Support Classes Ahead of Time Support for Tests WebTestClient RestTestClient MockMvc Overview Setup Options Hamcrest Integration Static Imports Configuring MockMvc Setup Features Performing Requests Defining Expectations Async Requests Streaming Responses Filter Registrations AssertJ Integration Configuring MockMvcTester Performing Requests Defining Expectations MockMvc integration HtmlUnit Integration Why HtmlUnit Integration? MockMvc and HtmlUnit MockMvc and WebDriver MockMvc and Geb MockMvc vs End-to-End Tests Further Examples Testing Client Applications Appendix Annotations Standard Annotation Support Spring Testing Annotations @BootstrapWith @ContextConfiguration @WebAppConfiguration @ContextHierarchy @ContextCustomizerFactories @ActiveProfiles @TestPropertySource @DynamicPropertySource @TestBean @MockitoBean and @MockitoSpyBean @DirtiesContext @TestExecutionListeners @RecordApplicationEvents @Commit @Rollback @BeforeTransaction @AfterTransaction @Sql @SqlConfig @SqlMergeMode @SqlGroup @DisabledInAotMode Spring JUnit 4 Testing Annotations Spring JUnit Jupiter Testing Annotations Meta-Annotation Support for Testing Further Resources Integration REST Clients JMS (Java Message Service) Using Spring JMS Sending a Message Receiving a Message Support for JCA Message Endpoints Annotation-driven Listener Endpoints JMS Namespace Support JMX Exporting Your Beans to JMX Controlling the Management Interface of Your Beans Controlling ObjectName Instances for Your Beans Using JSR-160 Connectors Accessing MBeans through Proxies Notifications Further Resources Email Task Execution and Scheduling Cache Abstraction Understanding the Cache Abstraction Declarative Annotation-based Caching JCache (JSR-107) Annotations Declarative XML-based Caching Configuring the Cache Storage Plugging-in Different Back-end Caches How can I Set the TTL/TTI/Eviction policy/XXX feature? Observability Support JVM AOT Cache JVM Checkpoint Restore Appendix Language Support Kotlin Requirements Extensions Null-safety Classes and Interfaces Annotations Bean Registration DSL Web Coroutines Spring Projects in Kotlin Getting Started Resources Apache Groovy Appendix Java API Kotlin API Wiki Search Edit this Page GitHub Project Stack Overflow Spring Framework Integration Task Execution and Scheduling Task Execution and Scheduling The Spring Framework provides abstractions for the asynchronous execution and scheduling of tasks with the TaskExecutor and TaskScheduler interfaces, respectively. Spring also features implementations of those interfaces that support thread pools or delegation to CommonJ within an application server environment. Ultimately, the use of these implementations behind the common interfaces abstracts away the differences between Java SE and Jakarta EE environments. Spring also features integration classes to support scheduling with the Quartz Scheduler . The Spring TaskExecutor Abstraction Executors are the JDK name for the concept of thread pools. The “executor” naming is due to the fact that there is no guarantee that the underlying implementation is actually a pool. An executor may be single-threaded or even synchronous. Spring’s abstraction hides implementation details between the Java SE and Jakarta EE environments. Spring’s TaskExecutor interface is identical to the java.util.concurrent.Executor interface. In fact, originally, its primary reason for existence was to abstract away the need for Java 5 when using thread pools. The interface has a single method ( execute(Runnable task) ) that accepts a task for execution based on the semantics and configuration of the thread pool. The TaskExecutor was originally created to give other Spring components an abstraction for thread pooling where needed. Components such as the ApplicationEventMulticaster , JMS’s AbstractMessageListenerContainer , and Quartz integration all use the TaskExecutor abstraction to pool threads. However, if your beans need thread pooling behavior, you can also use this abstraction for your own needs. TaskExecutor Types Spring includes a number of pre-built implementations of TaskExecutor . In all likelihood, you should never need to implement your own. The variants that Spring provides are as follows: SyncTaskExecutor : This implementation does not run invocations asynchronously. Instead, each invocation takes place in the calling thread. It is primarily used in situations where multi-threading is not necessary, such as in simple test cases. SimpleAsyncTaskExecutor : This implementation does not reuse any threads. Rather, it starts up a new thread for each invocation. However, it does support a concurrency limit that blocks any invocations that are over the limit until a slot has been freed up. If you are looking for true pooling, see ThreadPoolTaskExecutor , later in this list. This will use JDK 21’s Virtual Threads, when the "virtualThreads" option is enabled. This implementation also supports graceful shutdown through Spring’s lifecycle management. ConcurrentTaskExecutor : This implementation is an adapter for a java.util.concurrent.Executor instance. There is an alternative ( ThreadPoolTaskExecutor ) that exposes the Executor configuration parameters as bean properties. There is rarely a need to use ConcurrentTaskExecutor directly. However, if the ThreadPoolTaskExecutor is not flexible enough for your needs, ConcurrentTaskExecutor is an alternative. ThreadPoolTaskExecutor : This implementation is most commonly used. It exposes bean properties for configuring a java.util.concurrent.ThreadPoolExecutor and wraps it in a TaskExecutor . If you need to adapt to a different kind of java.util.concurrent.Executor , we recommend that you use a ConcurrentTaskExecutor instead. It also provides a pause/resume capability and graceful shutdown through Spring’s lifecycle management. DefaultManagedTaskExecutor : This implementation uses a JNDI-obtained ManagedExecutorService in a JSR-236 compatible runtime environment (such as a Jakarta EE application server), replacing a CommonJ WorkManager for that purpose. Using a TaskExecutor Spring’s TaskExecutor implementations are commonly used with dependency injection. In the following example, we define a bean that uses the ThreadPoolTaskExecutor to asynchronously print out a set of messages: Java Kotlin public class TaskExecutorExample { private class MessagePrinterTask implements Runnable { private String message; public MessagePrinterTask(String message) { this.message = message; } public void run() { System.out.println(message); } } private TaskExecutor taskExecutor; public TaskExecutorExample(TaskExecutor taskExecutor) { this.taskExecutor = taskExecutor; } public void printMessages() { for(int i = 0; i < 25; i++) { taskExecutor.execute(new MessagePrinterTask("Message" + i)); } } } class TaskExecutorExample(private val taskExecutor: TaskExecutor) { private inner class MessagePrinterTask(private val message: String) : Runnable { override fun run() { println(message) } } fun printMessages() { for (i in 0..24) { taskExecutor.execute( MessagePrinterTask( "Message$i" ) ) } } } As you can see, rather than retrieving a thread from the pool and executing it yourself, you add your Runnable to the queue. Then the TaskExecutor uses its internal rules to decide when the task gets run. To configure the rules that the TaskExecutor uses, we expose simple bean properties: Java Kotlin Xml @Bean ThreadPoolTaskExecutor taskExecutor() { ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor(); taskExecutor.setCorePoolSize(5); taskExecutor.setMaxPoolSize(10); taskExecutor.setQueueCapacity(25); return taskExecutor; } @Bean TaskExecutorExample taskExecutorExample(ThreadPoolTaskExecutor taskExecutor) { return new TaskExecutorExample(taskExecutor); } @Bean fun taskExecutor() = ThreadPoolTaskExecutor().apply { corePoolSize = 5 maxPoolSize = 10 queueCapacity = 25 } @Bean fun taskExecutorExample(taskExecutor: ThreadPoolTaskExecutor) = TaskExecutorExample(taskExecutor) <bean id="taskExecutor" class="org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor"> <property name="corePoolSize" value="5"/> <property name="maxPoolSize" value="10"/> <property name="queueCapacity" value="25"/> </bean> <bean id="taskExecutorExample" class="TaskExecutorExample"> <constructor-arg ref="taskExecutor"/> </bean> Most TaskExecutor implementations provide a way to automatically wrap tasks submitted with a TaskDecorator . Decorators should delegate to the task it is wrapping, possibly implementing custom behavior before/after the execution of the task. Let’s consider a simple implementation that will log messages before and after the execution or our tasks: Java Kotlin import org.apache.commons.logging.Log; import org.apache.commons.logging.LogFactory; import org.springframework.core.task.TaskDecorator; public class LoggingTaskDecorator implements TaskDecorator { private static final Log logger = LogFactory.getLog(LoggingTaskDecorator.class); @Override public Runnable decorate(Runnable runnable) { return () -> { logger.debug("Before execution of " + runnable); runnable.run(); logger.debug("After execution of " + runnable); }; } } import org.apache.commons.logging.Log import org.apache.commons.logging.LogFactory import org.springframework.core.task.TaskDecorator class LoggingTaskDecorator : TaskDecorator { override fun decorate(runnable: Runnable): Runnable { return Runnable { logger.debug("Before execution of $runnable") runnable.run() logger.debug("After execution of $runnable") } } companion object { private val logger: Log = LogFactory.getLog( LoggingTaskDecorator::class.java ) } } We can then configure our decorator on a TaskExecutor instance: Java Kotlin Xml @Bean ThreadPoolTaskExecutor decoratedTaskExecutor() { ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor(); taskExecutor.setTaskDecorator(new LoggingTaskDecorator()); return taskExecutor; } @Bean fun decoratedTaskExecutor() = ThreadPoolTaskExecutor().apply { setTaskDecorator(LoggingTaskDecorator()) } <bean id="decoratedTaskExecutor" class="org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor"> <property name="taskDecorator" ref="loggingTaskDecorator"/> </bean> In case multiple decorators are needed, the org.springframework.core.task.support.CompositeTaskDecorator can be used to execute sequentially multiple decorators. The Spring TaskScheduler Abstraction In addition to the TaskExecutor abstraction, Spring has a TaskScheduler SPI with a variety of methods for scheduling tasks to run at some point in the future. The following listing shows the TaskScheduler interface definition: public interface TaskScheduler { Clock getClock(); ScheduledFuture schedule(Runnable task, Trigger trigger); ScheduledFuture schedule(Runnable task, Instant startTime); ScheduledFuture scheduleAtFixedRate(Runnable task, Instant startTime, Duration period); ScheduledFuture scheduleAtFixedRate(Runnable task, Duration period); ScheduledFuture scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay); ScheduledFuture scheduleWithFixedDelay(Runnable task, Duration delay); The simplest method is the one named schedule that takes only a Runnable and an Instant . That causes the task to run once after the specified time. All of the other methods are capable of scheduling tasks to run repeatedly. The fixed-rate and fixed-delay methods are for simple, periodic execution, but the method that accepts a Trigger is much more flexible. Trigger Interface The Trigger interface is essentially inspired by JSR-236. The basic idea of the Trigger is that execution times may be determined based on past execution outcomes or even arbitrary conditions. If these determinations take into account the outcome of the preceding execution, that information is available within a TriggerContext . The Trigger interface itself is quite simple, as the following listing shows: public interface Trigger { Instant nextExecution(TriggerContext triggerContext); } The TriggerContext is the most important part. It encapsulates all of the relevant data and is open for extension in the future, if necessary. The TriggerContext is an interface (a SimpleTriggerContext implementation is used by default). The following listing shows the available methods for Trigger implementations. public interface TriggerContext { Clock getClock(); Instant lastScheduledExecution(); Instant lastActualExecution(); Instant lastCompletion(); } Trigger Implementations Spring provides two implementations of the Trigger interface. The most interesting one is the CronTrigger . It enables the scheduling of tasks based on cron expressions . For example, the following task is scheduled to run 15 minutes past each hour but only during the 9-to-5 "business hours" on weekdays: scheduler.schedule(task, new CronTrigger("0 15 9-17 * * MON-FRI")); The other implementation is a PeriodicTrigger that accepts a fixed period, an optional initial delay value, and a boolean to indicate whether the period should be interpreted as a fixed-rate or a fixed-delay. Since the TaskScheduler interface already defines methods for scheduling tasks at a fixed rate or with a fixed delay, those methods should be used directly whenever possible. The value of the PeriodicTrigger implementation is that you can use it within components that rely on the Trigger abstraction. For example, it may be convenient to allow periodic triggers, cron-based triggers, and even custom trigger implementations to be used interchangeably. Such a component could take advantage of dependency injection so that you can configure such Triggers externally and, therefore, easily modify or extend them. TaskScheduler implementations As with Spring’s TaskExecutor abstraction, the primary benefit of the TaskScheduler arrangement is that an application’s scheduling needs are decoupled from the deployment environment. This abstraction level is particularly relevant when deploying to an application server environment where threads should not be created directly by the application itself. For such scenarios, Spring provides a DefaultManagedTaskScheduler that delegates to a JSR-236 ManagedScheduledExecutorService in a Jakarta EE environment. Whenever external thread management is not a requirement, a simpler alternative is a local ScheduledExecutorService setup within the application, which can be adapted through Spring’s ConcurrentTaskScheduler . As a convenience, Spring also provides a ThreadPoolTaskScheduler , which internally delegates to a ScheduledExecutorService to provide common bean-style configuration along the lines of ThreadPoolTaskExecutor . These variants work perfectly fine for locally embedded thread pool setups in lenient application server environments, as well — in particular on Tomcat and Jetty. As of 6.1, ThreadPoolTaskScheduler provides a pause/resume capability and graceful shutdown through Spring’s lifecycle management. There is also a new option called SimpleAsyncTaskScheduler which is aligned with JDK 21’s Virtual Threads, using a single scheduler thread but firing up a new thread for every scheduled task execution (except for fixed-delay tasks which all operate on a single scheduler thread, so for this virtual-thread-aligned option, fixed rates and cron triggers are recommended). Annotation Support for Scheduling and Asynchronous Execution Spring provides annotation support for both task scheduling and asynchronous method execution. Enable Scheduling Annotations To enable support for @Scheduled and @Async annotations, you can add @EnableScheduling and @EnableAsync to one of your @Configuration classes, or <task:annotation-driven> element, as the following example shows: Java Kotlin Xml @Configuration @EnableAsync @EnableScheduling public class SchedulingConfiguration { } @Configuration @EnableAsync @EnableScheduling class SchedulingConfiguration <beans xmlns="http://www.springframework.org/schema/beans" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:task="http://www.springframework.org/schema/task" xsi:schemaLocation="http://www.springframework.org/schema/beans https://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/task https://www.springframework.org/schema/task/spring-task.xsd"> <task:annotation-driven executor="myExecutor" scheduler="myScheduler"/> <task:executor id="myExecutor" pool-size="5"/> <task:scheduler id="myScheduler" pool-size="10"/> </beans> You can pick and choose the relevant annotations for your application. For example, if you need only support for @Scheduled , you can omit @EnableAsync . For more fine-grained control, you can additionally implement the SchedulingConfigurer interface, the AsyncConfigurer interface, or both. See the SchedulingConfigurer and AsyncConfigurer javadoc for full details. Note that, with the preceding XML, an executor reference is provided for handling those tasks that correspond to methods with the @Async annotation, and the scheduler reference is provided for managing those methods annotated with @Scheduled . The default advice mode for processing @Async annotations is proxy which allows for interception of calls through the proxy only. Local calls within the same class cannot get intercepted that way. For a more advanced mode of interception, consider switching to aspectj mode in combination with compile-time or load-time weaving. The @Scheduled annotation You can add the @Scheduled annotation to a method, along with trigger metadata. For example, the following method is invoked every five seconds (5000 milliseconds) with a fixed delay, meaning that the period is measured from the completion time of each preceding invocation. @Scheduled(fixedDelay = 5000) public void doSomething() { // something that should run periodically } By default, milliseconds will be used as the time unit for fixed delay, fixed rate, and initial delay values. If you would like to use a different time unit such as seconds or minutes, you can configure this via the timeUnit attribute in @Scheduled . For example, the previous example can also be written as follows. @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.SECONDS) public void doSomething() { // something that should run periodically } If you need a fixed-rate execution, you can use the fixedRate attribute within the annotation. The following method is invoked every five seconds (measured between the successive start times of each invocation): @Scheduled(fixedRate = 5, timeUnit = TimeUnit.SECONDS) public void doSomething() { // something that should run periodically } For fixed-delay and fixed-rate tasks, you can specify an initial delay by indicating the amount of time to wait before the first execution of the method, as the following fixedRate example shows: @Scheduled(initialDelay = 1000, fixedRate = 5000) public void doSomething() { // something that should run periodically } For one-time tasks, you can just specify an initial delay by indicating the amount of time to wait before the intended execution of the method: @Scheduled(initialDelay = 1000) public void doSomething() { // something that should run only once } If simple periodic scheduling is not expressive enough, you can provide a cron expression . The following example runs only on weekdays: @Scheduled(cron="*/5 * * * * MON-FRI") public void doSomething() { // something that should run on weekdays only } You can also use the zone attribute to specify the time zone in which the cron expression is resolved. Notice that the methods to be scheduled must have void returns and must not accept any arguments. If the method needs to interact with other objects from the application context, those would typically have been provided through dependency injection. @Scheduled can be used as a repeatable annotation. If several scheduled declarations are found on the same method, each of them will be processed independently, with a separate trigger firing for each of them. As a consequence, such co-located schedules may overlap and execute multiple times in parallel or in immediate succession. Please make sure that your specified cron expressions etc do not accidentally overlap. As of Spring Framework 4.3, @Scheduled methods are supported on beans of any scope. Make sure that you are not initializing multiple instances of the same @Scheduled annotation class at runtime, unless you do want to schedule callbacks to each such instance. Related to this, make sure that you do not use @Configurable on bean classes that are annotated with @Scheduled and registered as regular Spring beans with the container. Otherwise, you would get double initialization (once through the container and once through the @Configurable aspect), with the consequence of each @Scheduled method being invoked twice. The @Scheduled annotation on Reactive methods or Kotlin suspending functions As of Spring Framework 6.1, @Scheduled methods are also supported on several types of reactive methods: methods with a Publisher return type (or any concrete implementation of Publisher ) like in the following example: @Scheduled(fixedDelay = 500) public Publisher<Void> reactiveSomething() { // return an instance of Publisher } methods with a return type that can be adapted to Publisher via the shared instance of the ReactiveAdapterRegistry , provided the type supports deferred subscription like in the following example: @Scheduled(fixedDelay = 500) public Single<String> rxjavaNonPublisher() { return Single.just("example"); } The CompletableFuture class is an example of a type that can typically be adapted to Publisher but doesn’t support deferred subscription. Its ReactiveAdapter in the registry denotes that by having the getDescriptor().isDeferred() method return false . Kotlin suspending functions, like in the following example: @Scheduled(fixedDelay = 500) suspend fun something() { // do something asynchronous } methods that return a Kotlin Flow or Deferred instance, like in the following example: @Scheduled(fixedDelay = 500) fun something(): Flow<Void> { flow { // do something asynchronous } } All these types of methods must be declared without any arguments. In the case of Kotlin suspending functions, the kotlinx.coroutines.reactor bridge must also be present to allow the framework to invoke a suspending function as a Publisher . The Spring Framework will obtain a Publisher for the annotated method once and will schedule a Runnable in which it subscribes to said Publisher . These inner regular subscriptions occur according to the corresponding cron / fixedDelay / fixedRate configuration. If the Publisher emits onNext signal(s), these are ignored and discarded (the same way return values from synchronous @Scheduled methods are ignored). In the following example, the Flux emits onNext("Hello") , onNext("World") every 5 seconds, but these values are unused: @Scheduled(initialDelay = 5000, fixedRate = 5000) public Flux<String> reactiveSomething() { return Flux.just("Hello", "World"); } If the Publisher emits an onError signal, it is logged at WARN level and recovered. Because of the asynchronous and lazy nature of Publisher instances, exceptions are not thrown from the Runnable task: this means that the ErrorHandler contract is not involved for reactive methods. As a result, further scheduled subscription occurs despite the error. In the following example, the Mono subscription fails twice in the first five seconds. Then subscriptions start succeeding, printing a message to the standard output every five seconds: @Scheduled(initialDelay = 0, fixedRate = 5000) public Mono<Void> reactiveSomething() { AtomicInteger countdown = new AtomicInteger(2); return Mono.defer(() -> { if (countDown.get() == 0 || countDown.decrementAndGet() == 0) { return Mono.fromRunnable(() -> System.out.println("Message")); } return Mono.error(new IllegalStateException("Cannot deliver message")); }) } When destroying the annotated bean or closing the application context, Spring Framework cancels scheduled tasks, which includes the next scheduled subscription to the Publisher as well as any past subscription that is still currently active (for example, for long-running publishers or even infinite publishers). The @Async annotation You can provide the @Async annotation on a method so that invocation of that method occurs asynchronously. In other words, the caller returns immediately upon invocation, while the actual execution of the method occurs in a task that has been submitted to a Spring TaskExecutor . In the simplest case, you can apply the annotation to a method that returns void , as the following example shows: @Async void doSomething() { // this will be run asynchronously } Unlike the methods annotated with the @Scheduled annotation, these methods can expect arguments, because they are invoked in the “normal” way by callers at runtime rather than from a scheduled task being managed by the container. For example, the following code is a legitimate application of the @Async annotation: @Async void doSomething(String s) { // this will be run asynchronously } Even methods that return a value can be invoked asynchronously. However, such methods are required to have a Future -typed return value. This still provides the benefit of asynchronous execution so that the caller can perform other tasks prior to calling get() on that Future . The following example shows how to use @Async on a method that returns a value: @Async Future<String> returnSomething(int i) { // this will be run asynchronously } @Async methods may not only declare a regular java.util.concurrent.Future return type but also java.util.concurrent.CompletableFuture , for richer interaction with the asynchronous task and for immediate composition with further processing steps. You can not use @Async in conjunction with lifecycle callbacks such as @PostConstruct . To asynchronously initialize Spring beans, you currently have to use a separate initializing Spring bean that then invokes the @Async annotated method on the target, as the following example shows: public class SampleBeanImpl implements SampleBean { @Async void doSomething() { // ... } } public class SampleBeanInitializer { private final SampleBean bean; public SampleBeanInitializer(SampleBean bean) { this.bean = bean; } @PostConstruct public void initialize() { bean.doSomething(); } } There is no direct XML equivalent for @Async , since such methods should be designed for asynchronous execution in the first place, not externally re-declared to be asynchronous. However, you can manually set up Spring’s AsyncExecutionInterceptor with Spring AOP, in combination with a custom pointcut. Executor Qualification with @Async By default, when specifying @Async on a method, the executor that is used is the one configured when enabling async support , i.e. the “annotation-driven” element if you are using XML or your AsyncConfigurer implementation, if any. However, you can use the value attribute of the @Async annotation when you need to indicate that an executor other than the default should be used when executing a given method. The following example shows how to do so: @Async("otherExecutor") void doSomething(String s) { // this will be run asynchronously by "otherExecutor" } In this case, "otherExecutor" can be the name of any Executor bean in the Spring container, or it may be the name of a qualifier associated with any Executor (for example, as specified with the <qualifier> element or Spring’s @Qualifier annotation). Exception Management with @Async When an @Async method has a Future -typed return value, it is easy to manage an exception that was thrown during the method execution, as this exception is thrown when calling get on the Future result. With a void return type, however, the exception is uncaught and cannot be transmitted. You can provide an AsyncUncaughtExceptionHandler to handle such exceptions. The following example shows how to do so: public class MyAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler { @Override public void handleUncaughtException(Throwable ex, Method method, Object... params) { // handle exception } } By default, the exception is merely logged. You can define a custom AsyncUncaughtExceptionHandler by using AsyncConfigurer or the <task:annotation-driven/> XML element. The task Namespace As of version 3.0, Spring includes an XML namespace for configuring TaskExecutor and TaskScheduler instances. It also provides a convenient way to configure tasks to be scheduled with a trigger. The scheduler Element The following element creates a ThreadPoolTaskScheduler instance with the specified thread pool size: <task:scheduler id="scheduler" pool-size="10"/> The value provided for the id attribute is used as the prefix for thread names within the pool. The scheduler element is relatively straightforward. If you do not provide a pool-size attribute, the default thread pool has only a single thread. There are no other configuration options for the scheduler. The executor Element The following creates a ThreadPoolTaskExecutor instance: <task:executor id="executor" pool-size="10"/> As with the scheduler shown in the previous section , the value provided for the id attribute is used as the prefix for thread names within the pool. As far as the pool size is concerned, the executor element supports more configuration options than the scheduler element. For one thing, the thread pool for a ThreadPoolTaskExecutor is itself more configurable. Rather than only a single size, an executor’s thread pool can have different values for the core and the max size. If you provide a single value, the executor has a fixed-size thread pool (the core and max sizes are the same). However, the executor element’s pool-size attribute also accepts a range in the form of min-max . The following example sets a minimum value of 5 and a maximum value of 25 : <task:executor id="executorWithPoolSizeRange" pool-size="5-25" queue-capacity="100"/> In the preceding configuration, a queue-capacity value has also been provided. The configuration of the thread pool should also be considered in light of the executor’s queue capacity. For the full description of the relationship between pool size and queue capacity, see the documentation for ThreadPoolExecutor . The main idea is that, when a task is submitted, the executor first tries to use a free thread if the number of active threads is currently less than the core size. If the core size has been reached, the task is added to the queue, as long as its capacity has not yet been reached. Only then, if the queue’s capacity has been reached, does the executor create a new thread beyond the core size. If the max size has also been reached, then the executor rejects the task. By default, the queue is unbounded, but this is rarely the desired configuration, because it can lead to OutOfMemoryError if enough tasks are added to that queue while all pool threads are busy. Furthermore, if the queue is unbounded, the max size has no effect at all. Since the executor always tries the queue before creating a new thread beyond the core size, a queue must have a finite capacity for the thread pool to grow beyond the core size (this is why a fixed-size pool is the only sensible case when using an unbounded queue). Consider the case, as mentioned above, when a task is rejected. By default, when a task is rejected, a thread pool executor throws a TaskRejectedException . However, the rejection policy is actually configurable. The exception is thrown when using the default rejection policy, which is the AbortPolicy implementation. For applications where some tasks can be skipped under heavy load, you can instead configure either DiscardPolicy or DiscardOldestPolicy . Another option that works well for applications that need to throttle the submitted tasks under heavy load is the CallerRunsPolicy . Instead of throwing an exception or discarding tasks, that policy forces the thread that is calling the submit method to run the task itself. The idea is that such a caller is busy while running that task and not able to submit other tasks immediately. Therefore, it provides a simple way to throttle the incoming load while maintaining the limits of the thread pool and queue. Typically, this allows the executor to “catch up” on the tasks it is handling and thereby frees up some capacity on the queue, in the pool, or both. You can choose any of these options from an enumeration of values available for the rejection-policy attribute on the executor element. The following example shows an executor element with a number of attributes to specify various behaviors: <task:executor id="executorWithCallerRunsPolicy" pool-size="5-25" queue-capacity="100" rejection-policy="CALLER_RUNS"/> Finally, the keep-alive setting determines the time limit (in seconds) for which threads may remain idle before being stopped. If there are more than the core number of threads currently in the pool, after waiting this amount of time without processing a task, excess threads get stopped. A time value of zero causes excess threads to stop immediately after executing a task without remaining follow-up work in the task queue. The following example sets the keep-alive value to two minutes: <task:executor id="executorWithKeepAlive" pool-size="5-25" keep-alive="120"/> The scheduled-tasks Element The most powerful feature of Spring’s task namespace is the support for configuring tasks to be scheduled within a Spring Application Context. This follows an approach similar to other “method-invokers” in Spring, such as that provided by the JMS namespace for configuring message-driven POJOs. Basically, a ref attribute can point to any Spring-managed object, and the method attribute provides the name of a method to be invoked on that object. The following listing shows a simple example: <task:scheduled-tasks scheduler="myScheduler"> <task:scheduled ref="beanA" method="methodA" fixed-delay="5000"/> </task:scheduled-tasks> <task:scheduler id="myScheduler" pool-size="10"/> The scheduler is referenced by the outer element, and each individual task includes the configuration of its trigger metadata. In the preceding example, that metadata defines a periodic trigger with a fixed delay indicating the number of milliseconds to wait after each task execution has completed. Another option is fixed-rate , indicating how often the method should be run regardless of how long any previous execution takes. Additionally, for both fixed-delay and fixed-rate tasks, you can specify an 'initial-delay' parameter, indicating the number of milliseconds to wait before the first execution of the method. For more control, you can instead provide a cron attribute to provide a cron expression . The following example shows these other options: <task:scheduled-tasks scheduler="myScheduler"> <task:scheduled ref="beanA" method="methodA" fixed-delay="5000" initial-delay="1000"/> <task:scheduled ref="beanB" method="methodB" fixed-rate="5000"/> <task:scheduled ref="beanC" method="methodC" cron="*/5 * * * * MON-FRI"/> </task:scheduled-tasks> <task:scheduler id="myScheduler" pool-size="10"/> Cron Expressions All Spring cron expressions have to conform to the same format, whether you are using them in @Scheduled annotations , task:scheduled-tasks elements , or someplace else. A well-formed cron expression, such as * * * * * * , consists of six space-separated time and date fields, each with its own range of valid values: ┌───────────── second (0-59) │ ┌───────────── minute (0 - 59) │ │ ┌───────────── hour (0 - 23) │ │ │ ┌───────────── day of the month (1 - 31) │ │ │ │ ┌───────────── month (1 - 12) (or JAN-DEC) │ │ │ │ │ ┌───────────── day of the week (0 - 7) │ │ │ │ │ │ (0 or 7 is Sunday, or MON-SUN) │ │ │ │ │ │ * * * * * * There are some rules that apply: A field may be an asterisk ( * ), which always stands for “first-last”. For the day-of-the-month or day-of-the-week fields, a question mark ( ? ) may be used instead of an asterisk. Commas ( , ) are used to separate items of a list. Two numbers separated with a hyphen ( - ) express a range of numbers. The specified range is inclusive. Following a range (or * ) with / specifies the interval of the number’s value through the range. English names can also be used for the month and day-of-week fields. Use the first three letters of the particular day or month (case does not matter). The day-of-month and day-of-week fields can contain an L character, which has a different meaning. In the day-of-month field, L stands for the last day of the month . If followed by a negative offset (that is, L-n ), it means n th-to-last day of the month . In the day-of-week field, L stands for the last day of the week . If prefixed by a number or three-letter name ( dL or DDDL ), it means the last day of week ( d or DDD ) in the month . The day-of-month field can be nW , which stands for the nearest weekday to day of the month n . If n falls on Saturday, this yields the Friday before it. If n falls on Sunday, this yields the Monday after, which also happens if n is 1 and falls on a Saturday (that is: 1W stands for the first weekday of the month ). If the day-of-month field is LW , it means the last weekday of the month . The day-of-week field can be d#n (or DDD#n ), which stands for the n th day of week d (or DDD ) in the month . Here are some examples: Cron Expression Meaning 0 0 * * * * top of every hour of every day */10 * * * * * every ten seconds 0 0 8-10 * * * 8, 9 and 10 o’clock of every day 0 0 6,19 * * * 6:00 AM and 7:00 PM every day 0 0/30 8-10 * * * 8:00, 8:30, 9:00, 9:30, 10:00 and 10:30 every day 0 0 9-17 * * MON-FRI on the hour nine-to-five weekdays 0 0 0 25 DEC ? every Christmas Day at midnight 0 0 0 L * * last day of the month at midnight 0 0 0 L-3 * * third-to-last day of the month at midnight 0 0 0 * * 5L last Friday of the month at midnight 0 0 0 * * THUL last Thursday of the month at midnight 0 0 0 1W * * first weekday of the month at midnight 0 0 0 LW * * last weekday of the month at midnight 0 0 0 ? * 5#2 the second Friday in the month at midnight 0 0 0 ? * MON#1 the first Monday in the month at midnight Macros Expressions such as 0 0 * * * * are hard for humans to parse and are, therefore, hard to fix in case of bugs. To improve readability, Spring supports the following macros, which represent commonly used sequences. You can use these macros instead of the six-digit value, thus: @Scheduled(cron = "@hourly") . Macro Meaning @yearly (or @annually ) once a year ( 0 0 0 1 1 * ) @monthly once a month ( 0 0 0 1 * * ) @weekly once a week ( 0 0 0 * * 0 ) @daily (or @midnight ) once a day ( 0 0 0 * * * ), or @hourly once an hour, ( 0 0 * * * * ) Using the Quartz Scheduler Quartz uses Trigger , Job , and JobDetail objects to realize scheduling of all kinds of jobs. For the basic concepts behind Quartz, see the Quartz Web site . For convenience purposes, Spring offers a couple of classes that simplify using Quartz within Spring-based applications. Using the JobDetailFactoryBean Quartz JobDetail objects contain all the information needed to run a job. Spring provides a JobDetailFactoryBean , which provides bean-style properties for XML configuration purposes. Consider the following example: <bean name="exampleJob" class="org.springframework.scheduling.quartz.JobDetailFactoryBean"> <property name="jobClass" value="example.ExampleJob"/> <property name="jobDataAsMap"> <map> <entry key="timeout" value="5"/> </map> </property> </bean> The job detail configuration has all the information it needs to run the job ( ExampleJob ). The timeout is specified in the job data map. The job data map is available through the JobExecutionContext (passed to you at execution time), but the JobDetail also gets its properties from the job data mapped to properties of the job instance. So, in the following example, the ExampleJob contains a bean property named timeout , and the JobDetail has it applied automatically: package example; public class ExampleJob extends QuartzJobBean { private int timeout; /** * Setter called after the ExampleJob is instantiated * with the value from the JobDetailFactoryBean. */ public void setTimeout(int timeout) { this.timeout = timeout; } protected void executeInternal(JobExecutionContext ctx) throws JobExecutionException { // do the actual work } } All additional properties from the job data map are available to you as well. By using the name and group properties, you can modify the name and the group of the job, respectively. By default, the name of the job matches the bean name of the JobDetailFactoryBean ( exampleJob in the preceding example above). Using the MethodInvokingJobDetailFactoryBean Often you merely need to invoke a method on a specific object. By using the MethodInvokingJobDetailFactoryBean , you can do exactly this, as the following example shows: <bean id="jobDetail" class="org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean"> <property name="targetObject" ref="exampleBusinessObject"/> <property name="targetMethod" value="doIt"/> </bean> The preceding example results in the doIt method being called on the exampleBusinessObject method, as the following example shows: public class ExampleBusinessObject { // properties and collaborators public void doIt() { // do the actual work } } <bean id="exampleBusinessObject" class="examples.ExampleBusinessObject"/> By using the MethodInvokingJobDetailFactoryBean , you need not create one-line jobs that merely invoke a method. You need only create the actual business object and wire up the detail object. By default, Quartz Jobs are stateless, resulting in the possibility of jobs interfering with each other. If you specify two triggers for the same JobDetail , it is possible that the second one starts before the first job has finished. If JobDetail classes implement the Stateful interface, this does not happen: the second job does not start before the first one has finished. To make jobs resulting from the MethodInvokingJobDetailFactoryBean be non-concurrent, set the concurrent flag to false , as the following example shows: <bean id="jobDetail" class="org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean"> <property name="targetObject" ref="exampleBusinessObject"/> <property name="targetMethod" value="doIt"/> <property name="concurrent" value="false"/> </bean> By default, jobs will run in a concurrent fashion. Wiring up Jobs by Using Triggers and SchedulerFactoryBean We have created job details and jobs. We have also reviewed the convenience bean that lets you invoke a method on a specific object. Of course, we still need to schedule the jobs themselves. This is done by using triggers and a SchedulerFactoryBean . Several triggers are available within Quartz, and Spring offers two Quartz FactoryBean implementations with convenient defaults: CronTriggerFactoryBean and SimpleTriggerFactoryBean . Triggers need to be scheduled. Spring offers a SchedulerFactoryBean that exposes triggers to be set as properties. SchedulerFactoryBean schedules the actual jobs with those triggers. The following listing uses both a SimpleTriggerFactoryBean and a CronTriggerFactoryBean : <bean id="simpleTrigger" class="org.springframework.scheduling.quartz.SimpleTriggerFactoryBean"> <!-- see the example of method invoking job above --> <property name="jobDetail" ref="jobDetail"/> <!-- 10 seconds --> <property name="startDelay" value="10000"/> <!-- repeat every 50 seconds --> <property name="repeatInterval" value="50000"/> </bean> <bean id="cronTrigger" class="org.springframework.scheduling.quartz.CronTriggerFactoryBean"> <property name="jobDetail" ref="exampleJob"/> <!-- run every morning at 6 AM --> <property name="cronExpression" value="0 0 6 * * ?"/> </bean> The preceding example sets up two triggers, one running every 50 seconds with a starting delay of 10 seconds and one running every morning at 6 AM. To finalize everything, we need to set up the SchedulerFactoryBean , as the following example shows: <bean class="org.springframework.scheduling.quartz.SchedulerFactoryBean"> <property name="triggers"> <list> <ref bean="cronTrigger"/> <ref bean="simpleTrigger"/> </list> </property> </bean> More properties are available for the SchedulerFactoryBean , such as the calendars used by the job details, properties to customize Quartz with, and a Spring-provided JDBC DataSource. See the SchedulerFactoryBean javadoc for more information. SchedulerFactoryBean also recognizes a quartz.properties file in the classpath, based on Quartz property keys, as with regular Quartz configuration. Please note that many SchedulerFactoryBean settings interact with common Quartz settings in the properties file; it is therefore not recommended to specify values at both levels. For example, do not set an "org.quartz.jobStore.class" property if you mean to rely on a Spring-provided DataSource, or specify an org.springframework.scheduling.quartz.LocalDataSourceJobStore variant which is a full-fledged replacement for the standard org.quartz.impl.jdbcjobstore.JobStoreTX . Email Cache Abstraction Spring Framework Stable 7.0.8 6.2.19 Snapshot 7.1.0-SNAPSHOT 7.0.9-SNAPSHOT 6.2.20-SNAPSHOT Related Spring Documentation Spring Boot Spring Framework Spring Cloud Spring Cloud Build Spring Cloud Bus Spring Cloud Circuit Breaker Spring Cloud Commons Spring Cloud Config Spring Cloud Consul Spring Cloud Contract Spring Cloud Function Spring Cloud Gateway Spring Cloud Kubernetes Spring Cloud Netflix Spring Cloud OpenFeign Spring Cloud Stream Spring Cloud Task Spring Cloud Vault Spring Cloud Zookeeper Spring Data Spring Data Cassandra Spring Data Commons Spring Data Couchbase Spring Data Elasticsearch Spring Data JPA Spring Data KeyValue Spring Data LDAP Spring Data MongoDB Spring Data Neo4j Spring Data Redis Spring Data JDBC & R2DBC Spring Data REST Spring Integration Spring Batch Spring Security Spring Authorization Server Spring LDAP Spring Security Kerberos Spring Session Spring Vault Spring AI Spring AMQP Spring CLI Spring GraphQL Spring for Apache Kafka Spring Modulith Spring for Apache Pulsar Spring Shell All Docs... Copyright © 2005 - Broadcom. All Rights Reserved. The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries. Terms of Use • Privacy • Trademark Guidelines • Thank you • Your California Privacy Rights • Cookie Settings Apache®, Apache Tomcat®, Apache Kafka®, Apache Cassandra™, and Apache Geode™ are trademarks or registered trademarks of the Apache Software Foundation in the United States and/or other countries. Java™, Java™ SE, Java™ EE, and OpenJDK™ are trademarks of Oracle and/or its affiliates. Kubernetes® is a registered trademark of the Linux Foundation in the United States and other countries. Linux® is the registered trademark of Linus Torvalds in the United States and other countries. Windows® and Microsoft® Azure are registered trademarks of Microsoft Corporation. “AWS” and “Amazon Web Services” are trademarks or registered trademarks of Amazon.com Inc. or its affiliates. All other trademarks and copyrights are property of their respective owners and are only mentioned for informative purposes. Other names may be trademarks of their respective owners. Search in all Spring Docs
