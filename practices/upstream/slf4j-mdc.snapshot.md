# slf4j-mdc — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://www.slf4j.org/manual.html (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T02:24:25Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://www.slf4j.org/manual.html`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r128`
**Body SHA-256 (below the `---` divider, header excluded):** a4cb03c75006710566f967a8a488c82913a02d90c13ffded550746838d7b9e22

---

---
snapshot_id: slf4j-mdc
source: "https://www.slf4j.org/manual.html"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 3
bytes: 21963
sha: "815e9e3ef0c49c032765d1278ce86ac68851fec8b808e7ec62836a6528d4525e"
---

# slf4j mdc — upstream snapshot

Source: https://www.slf4j.org/manual.html
Fetched: 2026-07-14

SLF4J Manual

## SLF4J user manual
The Simple Logging Facade for Java (SLF4J) serves as a simple
 facade or abstraction for various logging frameworks, such as
 java.util.logging, log4j 1.x, reload4j and logback. SLF4J allows
 the end-user to plug in the desired logging framework at
 deployment time. Note that SLF4J-enabling your
 library/application implies the addition of only a single
 mandatory dependency, namely
 slf4j-api-2.0.18.jar.

### Where are the Maven coordinates?
At this time if you are only interested in obtaining the
 coordinates for using SLF4J API with a logging backend, you can jump to the relevant section.

### Hello World
As customary in programming tradition, here is an example
 illustrating the simplest way to output "Hello world" using SLF4J.
 It begins by getting a logger with the name "HelloWorld". This
 logger is in turn used to log the message "Hello World".
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloWorld {

 public static void main(String[] args) {
 Logger logger = LoggerFactory.getLogger(HelloWorld.class);
 logger.info("Hello World");
 }
}
To run this example, you first need to obtain slf4j artifacts. Once that is
 done, add the file
 slf4j-api-2.0.18.jar to your class
 path.
Compiling and running HelloWorld will result in the
 following output being printed on the console.
SLF4J: No SLF4J providers were found.
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See https://www.slf4j.org/codes.html#noProviders for further details.
If you are using SLF4J 1.7 or earlier, the message would be:
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See https://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
This warning is printed because no slf4j provider (or binding)
 could be found on your class path.
The warning will disappear as soon as you add a provider to your class path. Assuming you add
 slf4j-simple-2.0.18.jar so that your class
 path contains:
slf4j-api-2.0.18.jar
slf4j-simple-2.0.18.jar
Compiling and running HelloWorld will now result in
 the following output on the console.
0 [main] INFO HelloWorld - Hello World

### Typical usage
 pattern
The sample code below illustrates the typical usage pattern
 for SLF4J. Note the use of {}-placeholders on line 15. See the
 question "What is the
 fastest way of logging?" in the FAQ for more details.
1: import org.slf4j.Logger;
 2: import org.slf4j.LoggerFactory;
 3: 
 4: public class Wombat {
 5: 
 6: final Logger logger = LoggerFactory.getLogger(Wombat.class);
 7: Integer t;
 8: Integer oldT;
 9:
10: public void setTemperature(Integer temperature) {
11: 
12: oldT = t; 
13: t = temperature;
14:
15: logger.debug("Temperature set to {}. Old value was {}.", t, oldT);
16:
17: if(temperature.intValue() > 50) {
18: logger.info("Temperature has risen above 50 degrees.");
19: }
20: }
21: }

### Fluent Logging API
since 2.0.0 SLF4J API version
 2.0.0 requires Java 8 and introduces a backward-compatible
 fluent logging API. By backward-compatible, we mean that
 existing logging frameworks do not have to be changed in order
 for the user to benefit from the fluent logging API.
The idea is to build a logging event piece by piece with a LoggingEventBuilder
 and to log once the event is fully built. The
 atTrace(), atDebug(),
 atInfo(), atWarn() and
 atError() methods, all new in the
 org.slf4j.Logger interface, return an instance of
 LoggingEventBuilder.
 For disabled log levels, the returned
 LoggingEventBuilder instance does nothing, thus
 preserving the nanosecond level performance of the traditional
 logging interface.
When using the fluent API, you must terminate the invocation
 chain by calling one of the log() method
 variants. Forgetting to call any of the log()
 method variants will result in no logging regardless of the
 logging level. Fortunately, if this happens, some IDEs
 will alert you with a
 compiler warning.
Here are few usage examples:
The statement
logger.atInfo().log("Hello world.");
is equivalent to:
logger.info("Hello world.");
The following log statements are equivalent in their output
 (for the default implementation):
int newT = 15;
int oldT = 16;

// using traditional API
logger.debug("Temperature set to {}. Old value was {}.", newT, oldT);

// using fluent API, log message with arguments
logger.atDebug().log("Temperature set to {}. Old value was {}.", newT, oldT);
 
// using fluent API, add arguments one by one and then log message
logger.atDebug().setMessage("Temperature set to {}. Old value was {}.").addArgument(newT).addArgument(oldT).log();

// using fluent API, add one argument with a Supplier and then log message with one more argument.
// Assume the method t16() returns 16.
logger.atDebug().setMessage("Temperature set to {}. Old value was {}.").addArgument(() -> t16()).addArgument(oldT).log();
The fluent logging API allows the specification of many
 different types of data to a org.slf4j.Logger
 without a combinatorial explosion in the number of methods in
 the Logger interface.
It is now possible to pass multiple Markers, pass arguments
 with a Supplier
 or pass multiple key-value pairs. Key-value pairs are
 particularly useful in conjunction with log data analysers which
 can interpret them automatically.
The following log statements are equivalent:
int newT = 15;
int oldT = 16;

// using classical API
logger.debug("oldT={} newT={} Temperature changed.", oldT, newT);

// using fluent API 
logger.atDebug().setMessage("Temperature changed.").addKeyValue("oldT", oldT).addKeyValue("newT", newT).log();
The key-value pair variant of the API stores the key-value
 pairs as separates objects. The default implementation currently
 in the org.slf4j.Logger class
 prefixes key-value pairs to the message. Logging
 backends are free and are even encouraged to offer a more
 customizable behaviour.

### Linking with a logging
 framework at deployment time
As mentioned previously, SLF4J is intended as a facade for
 various logging frameworks. The SLF4J distribution ships with
 several jar files referred to as "providers", with each provider
 corresponding to a supported logging framework. Note that SLF4J
 versions 1.7 and earlier used the term "binding" for
 providers.
Here is a partial, i.e. non-exhaustive, list of provider
 artifacts.
slf4j-log4j12-2.0.18.jar
Binding/provider
 for log4j
 version 1.2, a widely used logging framework. Given that
 log4j 1.x has been declared EOL in 2015 and again in 2022, as
 of SLF4J 1.7.35, the slf4j-log4j module automatically
 redirects to the
 slf4j-reload4j module at build time.
 Assuming you wish to continue to use the log4j 1.x framework,
 we strongly encourage you to use slf4j-reload4j
 instead. See below.
slf4j-reload4j-2.0.18.jar
since 1.7.33
 Binding/provider for reload4j
 framework. Reload4j is a drop-in replacement for log4j
 version 1.2.7. You also need to place reload4j.jar
 on your class path.
slf4j-jdk14-2.0.18.jar
Binding/provider for java.util.logging, also referred to as JDK
 1.4 logging
slf4j-nop-2.0.18.jar
Binding/provider for NOP,
 silently discarding all logging.
slf4j-simple-2.0.18.jar
Binding/provider for Simple
 implementation, which outputs all events to
 System.err. Only messages of level INFO and higher are
 printed. This binding may be useful in the context of small
 applications.
slf4j-jcl-2.0.18.jar
Binding/provider
 for Apache
 Commons Logging. This binding will delegate all SLF4J
 logging to Apache Commons Logging a.k.a. Jakarta Commons
 Logging (JCL) .
logback-classic-1.5.15.jar for use with Jakarta EE, requires logback-core-1.5.15.jar
Native implementation There are also
 SLF4J bindings/providers external to the SLF4J project, e.g. logback which implements
 SLF4J natively. Logback's
 
 ch.qos.logback.classic.Logger class is a
 direct implementation of SLF4J's 
 org.slf4j.Logger interface. Thus, using SLF4J
 in conjunction with logback involves strictly zero memory and
 computational overhead.
To switch logging frameworks, just replace slf4j bindings on
 your class path. For example, to switch from java.util.logging
 to reload4j, just replace slf4j-jdk14-2.0.18.jar with
 slf4j-reload4j-2.0.18.jar.
since 2.0.0 As of version 2.0.0,
 SLF4J bindings are called providers. Nevertheless, the general
 idea remains the same. SLF4J API version 2.0.0 relies on the ServiceLoader
 mechanism to find its logging backend. See the relevant FAQ entry for more
 details.
Here is a graphical illustration of the general idea.
The SLF4J interfaces and their various adapters are extremely
 simple. Most developers familiar with the Java language should
 be able to read and fully understand the code in less than one
 hour. No knowledge of class loaders is necessary as SLF4J does
 not make use nor does it directly access any class loaders. As a
 consequence, SLF4J suffers from none of the class loader
 problems or memory leaks observed with Jakarta Commons Logging
 (JCL) also called Apache Commons Logging.
Given the simplicity of the SLF4J interfaces and its
 deployment model, developers of new logging frameworks should
 find it very easy to write SLF4J providers.
since 2.0.9 You can specify the
 provider class explicitly via the "slf4j.provider" system
 property. This bypasses the service loader mechanism for finding
 providers and may shorten SLF4J initialization. For a list of
 providers class names, see the relevant FAQ entry.

### Libraries
Authors of widely-distributed components and libraries may
 code against the SLF4J interface in order to avoid imposing a
 logging framework on their end-user. Thus, the end-user may
 choose the desired logging framework at deployment time by
 inserting the corresponding slf4j binding on the classpath,
 which may be changed later by replacing an existing binding with
 another on the class path and restarting the application. This
 approach has proven to be simple and very robust.
As of SLF4J version 1.6.0, if no binding is found on
 the class path, then slf4j-api will default to a no-operation
 implementation discarding all log requests. Thus, instead of
 throwing a NoClassDefFoundError because the
 org.slf4j.impl.StaticLoggerBinder class is missing,
 SLF4J version 1.6.0 and later will emit a single warning message
 about the absence of a binding and proceed to discard all log
 requests without further protest. For example, let Wombat be
 some biology-related framework depending on SLF4J for
 logging. In order to avoid imposing a logging framework on the
 end-user, Wombat's distribution includes slf4j-api.jar
 but no binding. Even in the absence of any SLF4J binding on the
 class path, Wombat's distribution will still work
 out-of-the-box, and without requiring the end-user to download a
 binding from SLF4J's web-site. Only when the end-user decides to
 enable logging will she need to install the SLF4J binding
 corresponding to the logging framework chosen by her.
Basic rule Embedded components
 such as libraries or frameworks should not declare a dependency
 on any SLF4J binding/provider but only depend on
 slf4j-api. When a library declares a transitive dependency
 on a specific binding, that binding is imposed on the end-user
 negating the purpose of SLF4J. Note that declaring a
 non-transitive dependency on a binding, for example for testing,
 does not affect the end-user.
SLF4J usage in embedded components is also discussed in the
 FAQ in relation with logging configuration, dependency reduction and
 testing.

### Declaring project
 dependencies for logging
Given Maven's transitive dependency rules, for "regular"
 projects (not libraries or frameworks) declaring logging
 dependencies can be accomplished with a single dependency
 declaration.
SLF4J API SLF4J API ships
 within the "org.slf4j:slf4j-api" artifact. You can explicitly
 declare a dependency to it in your pom.xml file as
 shown below. Note that most logging implementations will
 automatically pull-in slf4j-api as a dependency. However, it is
 often a good idea to declare an explicit dependency to slf4j-api
 in order to fix the correct version of slf4j-api your project by
 virtue of of Maven's "nearest definition" dependency mediation
 rule.
org.slf4j
 slf4j-api
 2.0.18
logback-classic 1.3.x (Javax
 EE) If you wish to use logback-classic for Javax EE as the underlying logging
 framework, all you need to do is to declare
 "ch.qos.logback:logback-classic" as a dependency in your
 pom.xml file as shown below. In addition to
 logback-classic-1.3.14.jar, this will
 pull in slf4j-api-2.0.18.jar as well
 as logback-core-1.3.14.jar into your
 project. Note that explicitly declaring a dependency on
 logback-core-1.3.14 or
 slf4j-api-2.0.18.jar is not wrong and
 may be necessary to impose the correct version of said artifacts
 by virtue of Maven's "nearest definition" dependency mediation
 rule.
ch.qos.logback
 logback-classic
 1.3.14
logback-classic
 1.5.x
 (Jakarta EE) 

 If you wish to use logback-classic for Jakarta EE as the underlying logging framework,
 all you need to do is to declare
 "ch.qos.logback:logback-classic" as a dependency in your
 pom.xml file as shown below. In addition to
 logback-classic-1.5.15.jar, this
 will pull in slf4j-api-2.0.18.jar as
 well as logback-core-1.5.15.jar
 into your project. Note that explicitly declaring a
 dependency on logback-core-1.5.15
 or slf4j-api-2.0.18.jar is not wrong
 and may be necessary to impose the correct version of said
 artifacts by virtue of Maven's "nearest definition" dependency
 mediation rule.
ch.qos.logback
 logback-classic
 1.5.15
reload4j If you wish to use
 reload4j as the underlying logging framework, all you need to do
 is to declare "org.slf4j:slf4j-reload4j" as a dependency in your
 pom.xml file as shown below. In addition to
 slf4j-reload4j-2.0.18.jar, this will
 pull in slf4j-api-2.0.18.jar as well
 as reload4j-1.2.26.jar into your project.
 Note that explicitly declaring a dependency on
 reload4j-1.2.26.jar or
 slf4j-api-2.0.18.jar is not wrong and
 may be necessary to impose the correct version of said artifacts
 by virtue of Maven's "nearest definition" dependency mediation
 rule.
org.slf4j
 slf4j-reload4j
 2.0.18
log4j 1.2.x As of SLF4J
 version 1.7.36, declaring a dependency on
 org.slf4j:slf4j-log4j12 redirects to
 org.slf4j:slf4j-reload4j by virtue of Maven
 directive.
org.slf4j
 slf4j-log4j12
 2.0.18
java.util.logging If you
 wish to use java.util.logging as the underlying logging
 framework, all you need to do is to declare
 "org.slf4j:slf4j-jdk14" as a dependency in your pom.xml
 file as shown below. In addition to
 slf4j-jdk14-2.0.18.jar, this will
 pull in slf4j-api-2.0.18.jar into
 your project. Note that explicitly declaring a dependency on
 slf4j-api-2.0.18.jar is not wrong and
 may be necessary to impose the correct version of said artifact
 by virtue of Maven's "nearest definition" dependency mediation
 rule.
org.slf4j
 slf4j-jdk14
 2.0.18
SLF4J Simple If you
 wish to use org.slf4j.simple as the underlying logging
 implementation, all you need to do is to declare
 "org.slf4j:slf4j-simple" as a dependency in your pom.xml
 file as shown below. In addition to
 slf4j-simple-2.0.18.jar, this will
 pull in slf4j-api-2.0.18.jar into
 your project. Note that explicitly declaring a dependency on
 slf4j-api-2.0.18.jar is not wrong and
 may be necessary to impose the correct version of said artifact
 by virtue of Maven's "nearest definition" dependency mediation
 rule.
org.slf4j
 slf4j-simple
 2.0.18

### Binary
 compatibility
An SLF4J provider/binding designates an artifact such as
 slf4j-jdk14.jar or slf4j-reload4j.jar used to
 bind slf4j to an underlying logging framework, say,
 java.util.logging and respectively reload4j.
From the client's perspective the
 slf4j-api, more specifically classes in
 the org.slf4j package, are backward compatible for
 all versions. Client code compiled with slf4j-api-N.jar will
 run perfectly fine with slf4j-api-M.jar for any N and M. You
 only need to ensure that the version of your provider/binding
 matches that of the slf4j-api.jar. You do not have to worry
 about the version of slf4j-api.jar used by a given dependency in
 your project.
Mixing different versions of slf4j-api.jar and SLF4J
 provider/binding can cause problems. For example, if you are using
 slf4j-api-2.0.18.jar, then you should also use
 slf4j-simple-2.0.18.jar, using
 slf4j-simple-1.5.5.jar will not work.
However, from the client's perspective the SLF4J API, more
 specifically classes in the org.slf4j package, are
 backward compatible for all versions. Client code compiled with
 slf4j-api-N.jar will run perfectly fine with
 slf4j-api-M.jar for any N and M. You only need to
 ensure that the version of your provider/binding matches that of
 the slf4j-api.jar. You do not have to worry about the version of
 slf4j-api.jar used by a given dependency in your project. You
 can always use any version of slf4j-api.jar, and as
 long as the version of slf4j-api.jar and its
 provider/binding match, you should be fine.

### Consolidate logging via
 SLF4J
Often times, a given project will depend on various
 components which rely on logging APIs other than SLF4J. It is
 common to find projects depending on a combination of JCL,
 java.util.logging, log4j and SLF4J. It then becomes desirable to
 consolidate logging through a single channel. SLF4J caters for
 this common use-case by providing bridging modules for JCL,
 java.util.logging and log4j. For more details, please refer to
 the page on Bridging legacy
 APIs.

### Support for JDK Platform Logging (JEP 264)
 SLF4J
Since 2.0.0-alpha5 The
 slf4j-jdk-platform-logging module adds support for JDK Platform
 Logging.
org.slf4j
 slf4j-jdk-platform-logging
 2.0.18

### Mapped Diagnostic Context (MDC) support
"Mapped Diagnostic Context" is essentially a map maintained
 by the logging framework where the application code provides
 key-value pairs which can then be inserted by the logging
 framework in log messages. MDC data can also be highly helpful
 in filtering messages or triggering certain actions.
SLF4J supports MDC, or mapped diagnostic context. If the
 underlying logging framework offers MDC functionality, then
 SLF4J will delegate to the underlying framework's MDC. Note that
 at this time, only log4j and logback offer MDC functionality. If
 the underlying framework does not offer MDC, for example
 java.util.logging, then SLF4J will still store MDC data but the
 information therein will need to be retrieved by custom user
 code.
Thus, as a SLF4J user, you can take advantage of MDC
 information in the presence of log4j or logback, but without
 forcing these logging frameworks upon your users as
 dependencies.
For more information on MDC please see the chapter on MDC
 in the logback manual.

### Executive summary
Advantage
Description
Separation of logging API and configuration
Given that SLF4J offers a narrow API limited to writing
 log statements but no log configuration, SLF4J enforces
 separation of concerns. Logging statements are written using
 SLF4j API and configured via the underlying logging
 back-end, usually at a single location.
Select your logging framework at deployment time
The desired logging framework can be plugged in at
 deployment time by inserting the appropriate jar file
 (provider/binding) on your class path.
Fail-fast operation
A provider will be searched very early on during SLF4J
 initialization. If SLF4J cannot find a provider on the class
 path it will emit a single warning message and default to
 no-operation implementation.
Providers for popular logging frameworks
SLF4J supports popular logging frameworks, namely
 reload4j, log4j 1.x, log4j 2.x, java.util.logging, Simple
 logging and
 NOP. The logback, logevents, penna
 projects support SLF4J natively.
Bridging legacy logging APIs
The implementation of JCL over SLF4J, i.e
 jcl-over-slf4j.jar, will allow your project to
 migrate to SLF4J piecemeal, without breaking compatibility
 with existing software using JCL. Similarly,
 log4j-over-slf4j.jar and jul-to-slf4j modules will allow
 you to redirect log4j and respectively java.util.logging
 calls to SLF4J. See the page on Bridging legacy APIs for more
 details.
Migrate your source code
The slf4j-migrator utility
 can help you migrate your source to use SLF4J.
Support for parameterized log messages
All SLF4J providers/bindings support parameterized log
 messages with significantly improved performance
 results.

### Salient historical changes
since 1.6.0 If no binding is found on the
 class path, then SLF4J will default to a no-operation
 implementation.
since 1.7.0 Printing methods in the
 Logger
 interface now offer variants accepting varargs
 instead of Object[]. This change implies that SLF4J
 requires JDK 1.5 or later. Under the hood the Java compiler
 transforms the varargs part in methods into
 Object[]. Thus, the Logger interface generated by the
 compiler is indistinguishable in 1.7.x from its 1.6.x
 counterpart. It follows that SLF4J version 1.7.x is totally 100%
 no-ifs-or-buts compatible with SLF4J version 1.6.x.
since 1.7.5 Significant improvement
 in logger retrieval times. Given the extent of the improvement,
 users are highly encouraged to migrate to SLF4J 1.7.5 or later.
since 1.7.9 By setting the
 slf4j.detectLoggerNameMismatch system property to
 true, SLF4J can automatically spot incorrectly named
 loggers.
since 2.0.0 SLF4J API version 2.0.0
 requires Java 8 and introduces a backward-compatible fluent
 logging API. By backward-compatible, we mean that existing logging
 frameworks do not have to be changed in order for the user to
 benefit from the fluent logging API.
since 2.0.0 SLF4J API version 2.0.0
 relies on the ServiceLoader
 mechanism to find its logging backend. See the relevant FAQ entry for more
 details.

---

## Upstream refresh 2026-08-01 (verbatim extractor output)

Source: https://www.slf4j.org/manual.html
HTTP status: 200 · extracted bytes: 22382 · sha256: 0f598d0fd529ac13a3009deec68880fed861f417203a7c68258e92f1edf81327
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r128`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

SLF4J Manual SLF4J user manual The Simple Logging Facade for Java (SLF4J) serves as a simple facade or abstraction for various logging frameworks, such as java.util.logging, log4j 1.x, reload4j and logback. SLF4J allows the end-user to plug in the desired logging framework at deployment time. Note that SLF4J-enabling your library/application implies the addition of only a single mandatory dependency, namely slf4j-api-2.0.18.jar . Where are the Maven coordinates? At this time if you are only interested in obtaining the coordinates for using SLF4J API with a logging backend, you can jump to the relevant section . Hello World As customary in programming tradition, here is an example illustrating the simplest way to output "Hello world" using SLF4J. It begins by getting a logger with the name "HelloWorld". This logger is in turn used to log the message "Hello World". import org.slf4j.Logger; import org.slf4j.LoggerFactory; public class HelloWorld { public static void main(String[] args) { Logger logger = LoggerFactory.getLogger(HelloWorld.class); logger.info("Hello World"); } } To run this example, you first need to obtain slf4j artifacts. Once that is done, add the file slf4j-api-2.0.18.jar to your class path. Compiling and running HelloWorld will result in the following output being printed on the console. SLF4J: No SLF4J providers were found. SLF4J: Defaulting to no-operation (NOP) logger implementation SLF4J: See https://www.slf4j.org/codes.html#noProviders for further details. If you are using SLF4J 1.7 or earlier, the message would be: SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder". SLF4J: Defaulting to no-operation (NOP) logger implementation SLF4J: See https://www.slf4j.org/codes.html#StaticLoggerBinder for further details. This warning is printed because no slf4j provider (or binding) could be found on your class path. The warning will disappear as soon as you add a provider to your class path. Assuming you add slf4j-simple-2.0.18.jar so that your class path contains: slf4j-api-2.0.18.jar slf4j-simple-2.0.18.jar Compiling and running HelloWorld will now result in the following output on the console. 0 [main] INFO HelloWorld - Hello World Typical usage pattern The sample code below illustrates the typical usage pattern for SLF4J. Note the use of {}-placeholders on line 15. See the question "What is the fastest way of logging?" in the FAQ for more details. 1: import org.slf4j.Logger; 2: import org.slf4j.LoggerFactory; 3: 4: public class Wombat { 5: 6: final Logger logger = LoggerFactory.getLogger(Wombat.class); 7: Integer t; 8: Integer oldT; 9: 10: public void setTemperature(Integer temperature) { 11: 12: oldT = t; 13: t = temperature; 14: 15: logger.debug("Temperature set to {}. Old value was {}.", t, oldT); 16: 17: if(temperature.intValue() > 50) { 18: logger.info("Temperature has risen above 50 degrees."); 19: } 20: } 21: } Fluent Logging API since 2.0.0 SLF4J API version 2.0.0 requires Java 8 and introduces a backward-compatible fluent logging API. By backward-compatible, we mean that existing logging frameworks do not have to be changed in order for the user to benefit from the fluent logging API . The idea is to build a logging event piece by piece with a LoggingEventBuilder and to log once the event is fully built. The atTrace() , atDebug() , atInfo() , atWarn() and atError() methods, all new in the org.slf4j.Logger interface, return an instance of LoggingEventBuilder . For disabled log levels, the returned LoggingEventBuilder instance does nothing, thus preserving the nanosecond level performance of the traditional logging interface. When using the fluent API, you must terminate the invocation chain by calling one of the log() method variants. Forgetting to call any of the log() method variants will result in no logging regardless of the logging level. Fortunately, if this happens, some IDEs will alert you with a compiler warning . Here are few usage examples: The statement logger.atInfo().log("Hello world."); is equivalent to: logger.info("Hello world."); The following log statements are equivalent in their output (for the default implementation): int newT = 15; int oldT = 16; // using traditional API logger.debug("Temperature set to {}. Old value was {}.", newT, oldT); // using fluent API, log message with arguments logger. atDebug() .log("Temperature set to {}. Old value was {}.", newT, oldT); // using fluent API, add arguments one by one and then log message logger. atDebug() .setMessage("Temperature set to {}. Old value was {}.").addArgument(newT).addArgument(oldT). log() ; // using fluent API, add one argument with a Supplier and then log message with one more argument. // Assume the method t16() returns 16. logger. atDebug() .setMessage("Temperature set to {}. Old value was {}.").addArgument(() -> t16()).addArgument(oldT). log() ; The fluent logging API allows the specification of many different types of data to a org.slf4j.Logger without a combinatorial explosion in the number of methods in the Logger interface. It is now possible to pass multiple Markers , pass arguments with a Supplier or pass multiple key-value pairs. Key-value pairs are particularly useful in conjunction with log data analysers which can interpret them automatically. The following log statements are equivalent: int newT = 15; int oldT = 16; // using classical API logger.debug("oldT={} newT={} Temperature changed.", oldT, newT); // using fluent API logger.atDebug().setMessage("Temperature changed."). addKeyValue("oldT", oldT) .addKeyValue("newT", newT).log(); The key-value pair variant of the API stores the key-value pairs as separates objects. The default implementation currently in the org.slf4j.Logger class prefixes key-value pairs to the message. Logging backends are free and are even encouraged to offer a more customizable behaviour. Linking with a logging framework at deployment time As mentioned previously, SLF4J is intended as a facade for various logging frameworks. The SLF4J distribution ships with several jar files referred to as "providers", with each provider corresponding to a supported logging framework. Note that SLF4J versions 1.7 and earlier used the term "binding" for providers. Here is a partial, i.e. non-exhaustive, list of provider artifacts. slf4j-log4j12-2.0.18.jar Binding/provider for log4j version 1.2 , a widely used logging framework. Given that log4j 1.x has been declared EOL in 2015 and again in 2022, as of SLF4J 1.7.35, the slf4j-log4j module automatically redirects to the slf4j-reload4j module at build time . Assuming you wish to continue to use the log4j 1.x framework, we strongly encourage you to use slf4j-reload4j instead. See below. slf4j-reload4j-2.0.18.jar since 1.7.33 Binding/provider for reload4j framework. Reload4j is a drop-in replacement for log4j version 1.2.7. You also need to place reload4j.jar on your class path. slf4j-jdk14-2.0.18.jar Binding/provider for java.util.logging , also referred to as JDK 1.4 logging slf4j-nop-2.0.18.jar Binding/provider for NOP , silently discarding all logging. slf4j-simple-2.0.18.jar Binding/provider for Simple implementation, which outputs all events to System.err. Only messages of level INFO and higher are printed. This binding may be useful in the context of small applications. slf4j-jcl-2.0.18.jar Binding/provider for Apache Commons Logging . This binding will delegate all SLF4J logging to Apache Commons Logging a.k.a. Jakarta Commons Logging (JCL) . logback-classic-1.5.15.jar for use with Jakarta EE, requires logback-core-1.5.15.jar Native implementation There are also SLF4J bindings/providers external to the SLF4J project, e.g. logback which implements SLF4J natively. Logback's ch.qos.logback.classic.Logger class is a direct implementation of SLF4J's org.slf4j.Logger interface. Thus, using SLF4J in conjunction with logback involves strictly zero memory and computational overhead. To switch logging frameworks, just replace slf4j bindings on your class path. For example, to switch from java.util.logging to reload4j, just replace slf4j-jdk14-2.0.18.jar with slf4j-reload4j-2.0.18.jar. since 2.0.0 As of version 2.0.0, SLF4J bindings are called providers. Nevertheless, the general idea remains the same. SLF4J API version 2.0.0 relies on the ServiceLoader mechanism to find its logging backend. See the relevant FAQ entry for more details. Here is a graphical illustration of the general idea. The SLF4J interfaces and their various adapters are extremely simple. Most developers familiar with the Java language should be able to read and fully understand the code in less than one hour. No knowledge of class loaders is necessary as SLF4J does not make use nor does it directly access any class loaders. As a consequence, SLF4J suffers from none of the class loader problems or memory leaks observed with Jakarta Commons Logging (JCL) also called Apache Commons Logging. Given the simplicity of the SLF4J interfaces and its deployment model, developers of new logging frameworks should find it very easy to write SLF4J providers. since 2.0.9 You can specify the provider class explicitly via the "slf4j.provider" system property. This bypasses the service loader mechanism for finding providers and may shorten SLF4J initialization. For a list of providers class names, see the relevant FAQ entry . Libraries Authors of widely-distributed components and libraries may code against the SLF4J interface in order to avoid imposing a logging framework on their end-user. Thus, the end-user may choose the desired logging framework at deployment time by inserting the corresponding slf4j binding on the classpath, which may be changed later by replacing an existing binding with another on the class path and restarting the application. This approach has proven to be simple and very robust. As of SLF4J version 1.6.0 , if no binding is found on the class path, then slf4j-api will default to a no-operation implementation discarding all log requests. Thus, instead of throwing a NoClassDefFoundError because the org.slf4j.impl.StaticLoggerBinder class is missing, SLF4J version 1.6.0 and later will emit a single warning message about the absence of a binding and proceed to discard all log requests without further protest. For example, let Wombat be some biology-related framework depending on SLF4J for logging. In order to avoid imposing a logging framework on the end-user, Wombat's distribution includes slf4j-api.jar but no binding. Even in the absence of any SLF4J binding on the class path, Wombat's distribution will still work out-of-the-box, and without requiring the end-user to download a binding from SLF4J's web-site. Only when the end-user decides to enable logging will she need to install the SLF4J binding corresponding to the logging framework chosen by her. Basic rule Embedded components such as libraries or frameworks should not declare a dependency on any SLF4J binding/provider but only depend on slf4j-api . When a library declares a transitive dependency on a specific binding, that binding is imposed on the end-user negating the purpose of SLF4J. Note that declaring a non-transitive dependency on a binding, for example for testing, does not affect the end-user. SLF4J usage in embedded components is also discussed in the FAQ in relation with logging configuration , dependency reduction and testing . Declaring project dependencies for logging Given Maven's transitive dependency rules, for "regular" projects (not libraries or frameworks) declaring logging dependencies can be accomplished with a single dependency declaration. SLF4J API SLF4J API ships within the "org.slf4j:slf4j-api" artifact. You can explicitly declare a dependency to it in your pom.xml file as shown below. Note that most logging implementations will automatically pull-in slf4j-api as a dependency. However, it is often a good idea to declare an explicit dependency to slf4j-api in order to fix the correct version of slf4j-api your project by virtue of of Maven's "nearest definition" dependency mediation rule. <dependency> <groupId>org.slf4j</groupId> <artifactId>slf4j-api</artifactId> <version>2.0.18</version> </dependency> logback-classic 1.3.x (Javax EE) If you wish to use logback-classic for Javax EE as the underlying logging framework, all you need to do is to declare "ch.qos.logback:logback-classic" as a dependency in your pom.xml file as shown below. In addition to logback-classic-1.3.14.jar , this will pull in slf4j-api-2.0.18.jar as well as logback-core-1.3.14.jar into your project. Note that explicitly declaring a dependency on logback-core-1.3.14 or slf4j-api-2.0.18.jar is not wrong and may be necessary to impose the correct version of said artifacts by virtue of Maven's "nearest definition" dependency mediation rule. <dependency> <groupId>ch.qos.logback</groupId> <artifactId>logback-classic</artifactId> <version>1.3.14</version> </dependency> logback-classic 1.5.x (Jakarta EE) If you wish to use logback-classic for Jakarta EE as the underlying logging framework, all you need to do is to declare "ch.qos.logback:logback-classic" as a dependency in your pom.xml file as shown below. In addition to logback-classic-1.5.15.jar , this will pull in slf4j-api-2.0.18.jar as well as logback-core-1.5.15.jar into your project. Note that explicitly declaring a dependency on logback-core-1.5.15 or slf4j-api-2.0.18.jar is not wrong and may be necessary to impose the correct version of said artifacts by virtue of Maven's "nearest definition" dependency mediation rule. <dependency> <groupId>ch.qos.logback</groupId> <artifactId>logback-classic</artifactId> <version>1.5.15</version> </dependency> reload4j If you wish to use reload4j as the underlying logging framework, all you need to do is to declare "org.slf4j:slf4j-reload4j" as a dependency in your pom.xml file as shown below. In addition to slf4j-reload4j-2.0.18.jar , this will pull in slf4j-api-2.0.18.jar as well as reload4j-1.2.26.jar into your project. Note that explicitly declaring a dependency on reload4j-1.2.26.jar or slf4j-api-2.0.18.jar is not wrong and may be necessary to impose the correct version of said artifacts by virtue of Maven's "nearest definition" dependency mediation rule. <dependency> <groupId>org.slf4j</groupId> <artifactId>slf4j-reload4j</artifactId> <version>2.0.18</version> </dependency> log4j 1.2.x As of SLF4J version 1.7.36, declaring a dependency on org.slf4j:slf4j-log4j12 redirects to org.slf4j:slf4j-reload4j by virtue of Maven <relocation> directive. <dependency> <groupId>org.slf4j</groupId> <artifactId>slf4j-log4j12</artifactId> <version>2.0.18</version> </dependency> java.util.logging If you wish to use java.util.logging as the underlying logging framework, all you need to do is to declare "org.slf4j:slf4j-jdk14" as a dependency in your pom.xml file as shown below. In addition to slf4j-jdk14-2.0.18.jar , this will pull in slf4j-api-2.0.18.jar into your project. Note that explicitly declaring a dependency on slf4j-api-2.0.18.jar is not wrong and may be necessary to impose the correct version of said artifact by virtue of Maven's "nearest definition" dependency mediation rule. <dependency> <groupId>org.slf4j</groupId> <artifactId>slf4j-jdk14</artifactId> <version>2.0.18</version> </dependency> SLF4J Simple If you wish to use org.slf4j.simple as the underlying logging implementation, all you need to do is to declare "org.slf4j:slf4j-simple" as a dependency in your pom.xml file as shown below. In addition to slf4j-simple-2.0.18.jar , this will pull in slf4j-api-2.0.18.jar into your project. Note that explicitly declaring a dependency on slf4j-api-2.0.18.jar is not wrong and may be necessary to impose the correct version of said artifact by virtue of Maven's "nearest definition" dependency mediation rule. <dependency> <groupId>org.slf4j</groupId> <artifactId>slf4j-simple</artifactId> <version>2.0.18</version> </dependency> Binary compatibility An SLF4J provider/binding designates an artifact such as slf4j-jdk14.jar or slf4j-reload4j.jar used to bind slf4j to an underlying logging framework, say, java.util.logging and respectively reload4j. From the client's perspective the slf4j-api, more specifically classes in the org.slf4j package, are backward compatible for all versions. Client code compiled with slf4j-api-N.jar will run perfectly fine with slf4j-api-M.jar for any N and M. You only need to ensure that the version of your provider/binding matches that of the slf4j-api.jar. You do not have to worry about the version of slf4j-api.jar used by a given dependency in your project. Mixing different versions of slf4j-api.jar and SLF4J provider/binding can cause problems. For example, if you are using slf4j-api-2.0.18.jar, then you should also use slf4j-simple-2.0.18.jar, using slf4j-simple-1.5.5.jar will not work. However, from the client's perspective the SLF4J API, more specifically classes in the org.slf4j package, are backward compatible for all versions. Client code compiled with slf4j-api-N.jar will run perfectly fine with slf4j-api-M.jar for any N and M. You only need to ensure that the version of your provider/binding matches that of the slf4j-api.jar. You do not have to worry about the version of slf4j-api.jar used by a given dependency in your project. You can always use any version of slf4j-api.jar , and as long as the version of slf4j-api.jar and its provider/binding match, you should be fine. At initialization time, if SLF4J suspects that there may be an slf4j-api vs. provider/binding version mismatch problem, it will emit a warning about the suspected mismatch. --> Consolidate logging via SLF4J Often times, a given project will depend on various components which rely on logging APIs other than SLF4J. It is common to find projects depending on a combination of JCL, java.util.logging, log4j and SLF4J. It then becomes desirable to consolidate logging through a single channel. SLF4J caters for this common use-case by providing bridging modules for JCL, java.util.logging and log4j. For more details, please refer to the page on Bridging legacy APIs . Support for JDK Platform Logging (JEP 264) SLF4J Since 2.0.0-alpha5 The slf4j-jdk-platform-logging module adds support for JDK Platform Logging . <dependency> <groupId>org.slf4j</groupId> <artifactId>slf4j-jdk-platform-logging</artifactId> <version>2.0.18</version> </dependency> Mapped Diagnostic Context (MDC) support "Mapped Diagnostic Context" is essentially a map maintained by the logging framework where the application code provides key-value pairs which can then be inserted by the logging framework in log messages. MDC data can also be highly helpful in filtering messages or triggering certain actions. SLF4J supports MDC, or mapped diagnostic context. If the underlying logging framework offers MDC functionality, then SLF4J will delegate to the underlying framework's MDC. Note that at this time, only log4j and logback offer MDC functionality. If the underlying framework does not offer MDC, for example java.util.logging, then SLF4J will still store MDC data but the information therein will need to be retrieved by custom user code. Thus, as a SLF4J user, you can take advantage of MDC information in the presence of log4j or logback, but without forcing these logging frameworks upon your users as dependencies. For more information on MDC please see the chapter on MDC in the logback manual. Executive summary Advantage Description Separation of logging API and configuration Given that SLF4J offers a narrow API limited to writing log statements but no log configuration, SLF4J enforces separation of concerns. Logging statements are written using SLF4j API and configured via the underlying logging back-end, usually at a single location. Select your logging framework at deployment time The desired logging framework can be plugged in at deployment time by inserting the appropriate jar file (provider/binding) on your class path. Fail-fast operation A provider will be searched very early on during SLF4J initialization. If SLF4J cannot find a provider on the class path it will emit a single warning message and default to no-operation implementation. Providers for popular logging frameworks SLF4J supports popular logging frameworks, namely reload4j, log4j 1.x, log4j 2.x, java.util.logging, Simple logging and NOP. The logback , logevents , penna projects support SLF4J natively. Bridging legacy logging APIs The implementation of JCL over SLF4J, i.e jcl-over-slf4j.jar , will allow your project to migrate to SLF4J piecemeal, without breaking compatibility with existing software using JCL. Similarly, log4j-over-slf4j.jar and jul-to-slf4j modules will allow you to redirect log4j and respectively java.util.logging calls to SLF4J. See the page on Bridging legacy APIs for more details. Migrate your source code The slf4j-migrator utility can help you migrate your source to use SLF4J. Support for parameterized log messages All SLF4J providers/bindings support parameterized log messages with significantly improved performance results. Salient historical changes since 1.6.0 If no binding is found on the class path, then SLF4J will default to a no-operation implementation. since 1.7.0 Printing methods in the Logger interface now offer variants accepting varargs instead of Object[] . This change implies that SLF4J requires JDK 1.5 or later. Under the hood the Java compiler transforms the varargs part in methods into Object[] . Thus, the Logger interface generated by the compiler is indistinguishable in 1.7.x from its 1.6.x counterpart. It follows that SLF4J version 1.7.x is totally 100% no-ifs-or-buts compatible with SLF4J version 1.6.x. since 1.7.5 Significant improvement in logger retrieval times. Given the extent of the improvement, users are highly encouraged to migrate to SLF4J 1.7.5 or later. since 1.7.9 By setting the slf4j.detectLoggerNameMismatch system property to true, SLF4J can automatically spot incorrectly named loggers . since 2.0.0 SLF4J API version 2.0.0 requires Java 8 and introduces a backward-compatible fluent logging API. By backward-compatible, we mean that existing logging frameworks do not have to be changed in order for the user to benefit from the fluent logging API . since 2.0.0 SLF4J API version 2.0.0 relies on the ServiceLoader mechanism to find its logging backend. See the relevant FAQ entry for more details.
