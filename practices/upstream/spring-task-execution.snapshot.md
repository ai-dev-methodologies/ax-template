# spring-task-execution — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T02:24:32Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r155`
**Body SHA-256 (below the `---` divider, header excluded):** 5580c33a6ccaf2d7b6b53db5b5cc49598634057ebac5d47ba4cc5a4f58c099e9

---

---
snapshot_id: spring-task-execution
source: "https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 3
bytes: 14280
sha: "9005d64cc445bc6dd036a3b4d54d4fb6c9ab80267be44aa393adc74c5c7a8452"
---

# spring task execution — upstream snapshot

Source: https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html
Fetched: 2026-07-14

Task Execution and Scheduling :: Spring Boot
Edit this Page
 
 
 
 GitHub Project
 
 
 
 Stack Overflow

# Task Execution and Scheduling
In the absence of an Executor bean in the context, Spring Boot auto-configures an AsyncTaskExecutor.
When virtual threads are enabled (using Java 21+ and spring.threads.virtual.enabled set to true) this will be a SimpleAsyncTaskExecutor that uses virtual threads.
Otherwise, it will be a ThreadPoolTaskExecutor with sensible defaults.
The auto-configured AsyncTaskExecutor is used for the following integrations unless a custom Executor bean is defined:
Execution of asynchronous tasks using @EnableAsync, unless a bean of type AsyncConfigurer is defined.
Asynchronous handling of Callable return values from controller methods in Spring for GraphQL.
Asynchronous request handling in Spring MVC.
Support for blocking execution in Spring WebFlux.
Utilized for inbound and outbound message channels in Spring WebSocket.
Bootstrap executor for JPA, based on the bootstrap mode of JPA repositories.
Bootstrap executor for background initialization of beans in the ApplicationContext.
While this approach works in most scenarios, Spring Boot allows you to override the auto-configured AsyncTaskExecutor.
By default, when a custom Executor bean is registered, the auto-configured AsyncTaskExecutor backs off, and the custom Executor is used for regular task execution (via @EnableAsync).
However, Spring MVC, Spring WebFlux, and Spring GraphQL all require a bean named applicationTaskExecutor.
For Spring MVC and Spring WebFlux, this bean must be of type AsyncTaskExecutor, whereas Spring GraphQL does not enforce this type requirement.
Spring WebSocket and JPA will use AsyncTaskExecutor if either a single bean of this type is available or a bean named applicationTaskExecutor is defined.
Finally, the boostrap executor of the ApplicationContext uses a bean named applicationTaskExecutor unless a bean named bootstrapExecutor is defined.
The following code snippet demonstrates how to register a custom AsyncTaskExecutor to be used with Spring MVC, Spring WebFlux, Spring GraphQL, Spring WebSocket, JPA, and background initialization of beans.
Java
Kotlin
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@Configuration(proxyBeanMethods = false)
public class MyTaskExecutorConfiguration {

 @Bean("applicationTaskExecutor")
 SimpleAsyncTaskExecutor applicationTaskExecutor() {
 return new SimpleAsyncTaskExecutor("app-");
 }

}
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.SimpleAsyncTaskExecutor

@Configuration(proxyBeanMethods = false)
class MyTaskExecutorConfiguration {

 @Bean("applicationTaskExecutor")
 fun applicationTaskExecutor(): SimpleAsyncTaskExecutor {
 return SimpleAsyncTaskExecutor("app-")
 }

}
The applicationTaskExecutor bean will also be used for regular task execution if there is no @Primary bean or a bean named taskExecutor of type Executor or AsyncConfigurer present in the application context.
If neither the auto-configured AsyncTaskExecutor nor the applicationTaskExecutor bean is defined, the application defaults to a bean named taskExecutor for regular task execution (@EnableAsync), following Spring Framework’s behavior.
However, this bean will not be used for Spring MVC, Spring WebFlux, Spring GraphQL.
It could, however, be used for Spring WebSocket or JPA if the bean’s type is AsyncTaskExecutor.
If your application needs multiple Executor beans for different integrations, such as one for regular task execution with @EnableAsync and other for Spring MVC, Spring WebFlux, Spring WebSocket and JPA, you can configure them as follows.
Java
Kotlin
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration(proxyBeanMethods = false)
public class MyTaskExecutorConfiguration {

 @Bean("applicationTaskExecutor")
 SimpleAsyncTaskExecutor applicationTaskExecutor() {
 return new SimpleAsyncTaskExecutor("app-");
 }

 @Bean("taskExecutor")
 ThreadPoolTaskExecutor taskExecutor() {
 ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
 threadPoolTaskExecutor.setThreadNamePrefix("async-");
 return threadPoolTaskExecutor;
 }

}
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration(proxyBeanMethods = false)
class MyTaskExecutorConfiguration {

 @Bean("applicationTaskExecutor")
 fun applicationTaskExecutor(): SimpleAsyncTaskExecutor {
 return SimpleAsyncTaskExecutor("app-")
 }

 @Bean("taskExecutor")
 fun taskExecutor(): ThreadPoolTaskExecutor {
 val threadPoolTaskExecutor = ThreadPoolTaskExecutor()
 threadPoolTaskExecutor.setThreadNamePrefix("async-")
 return threadPoolTaskExecutor
 }

}
The auto-configured ThreadPoolTaskExecutorBuilder or SimpleAsyncTaskExecutorBuilder allow you to easily create instances of type AsyncTaskExecutor that replicate the default behavior of auto-configuration.
Java
Kotlin
import org.springframework.boot.task.SimpleAsyncTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@Configuration(proxyBeanMethods = false)
public class MyTaskExecutorConfiguration {

 @Bean
 SimpleAsyncTaskExecutor taskExecutor(SimpleAsyncTaskExecutorBuilder builder) {
 return builder.build();
 }

}
import org.springframework.boot.task.SimpleAsyncTaskExecutorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.SimpleAsyncTaskExecutor

@Configuration(proxyBeanMethods = false)
class MyTaskExecutorConfiguration {

 @Bean
 fun taskExecutor(builder: SimpleAsyncTaskExecutorBuilder): SimpleAsyncTaskExecutor {
 return builder.build()
 }

}
If a taskExecutor named bean is not an option, you can mark your bean as @Primary or define an AsyncConfigurer bean to specify the Executor responsible for handling regular task execution with @EnableAsync.
The following example demonstrates how to achieve this.
Java
Kotlin
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;

@Configuration(proxyBeanMethods = false)
public class MyTaskExecutorConfiguration {

 @Bean
 AsyncConfigurer asyncConfigurer(ExecutorService executorService) {
 return new AsyncConfigurer() {

 @Override
 public Executor getAsyncExecutor() {
 return executorService;
 }

 };
 }

 @Bean
 ExecutorService executorService() {
 return Executors.newCachedThreadPool();
 }

}
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Configuration(proxyBeanMethods = false)
class MyTaskExecutorConfiguration {

 @Bean
 fun asyncConfigurer(executorService: ExecutorService): AsyncConfigurer {
 return object : AsyncConfigurer {
 override fun getAsyncExecutor(): Executor {
 return executorService
 }
 }
 }

 @Bean
 fun executorService(): ExecutorService {
 return Executors.newCachedThreadPool()
 }

}
To register a custom Executor while keeping the auto-configured AsyncTaskExecutor, you can create a custom Executor bean and set the defaultCandidate=false attribute in its @Bean annotation, as demonstrated in the following example:
Java
Kotlin
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MyTaskExecutorConfiguration {

 @Bean(defaultCandidate = false)
 @Qualifier("scheduledExecutorService")
 ScheduledExecutorService scheduledExecutorService() {
 return Executors.newSingleThreadScheduledExecutor();
 }

}
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

@Configuration(proxyBeanMethods = false)
class MyTaskExecutorConfiguration {

 @Bean(defaultCandidate = false)
 @Qualifier("scheduledExecutorService")
 fun scheduledExecutorService(): ScheduledExecutorService {
 return Executors.newSingleThreadScheduledExecutor()
 }

}
In that case, you will be able to autowire your custom Executor into other components while retaining the auto-configured AsyncTaskExecutor.
However, remember to use the @Qualifier annotation alongside @Autowired.
If this is not possible for you, you can request Spring Boot to auto-configure an AsyncTaskExecutor anyway, as follows:
Properties
YAML
spring.task.execution.mode=force
spring:
 task:
 execution:
 mode: force
The auto-configured AsyncTaskExecutor will be used automatically for all integrations, even if a custom Executor bean is registered, including those marked as @Primary.
These integrations include:
Asynchronous task execution (@EnableAsync), unless an AsyncConfigurer bean is present.
Spring for GraphQL’s asynchronous handling of Callable return values from controller methods.
Spring MVC’s asynchronous request processing.
Spring WebFlux’s blocking execution support.
Utilized for inbound and outbound message channels in Spring WebSocket.
Bootstrap executor for JPA, based on the bootstrap mode of JPA repositories.
Bootstrap executor for background initialization of beans in the ApplicationContext, unless a bean named bootstrapExecutor is defined.
Depending on your target arrangement, you could set spring.task.execution.mode to force to auto-configure an applicationTaskExecutor, change your Executor into an AsyncTaskExecutor or define both an AsyncTaskExecutor and an AsyncConfigurer wrapping your custom Executor.
When force mode is enabled, applicationTaskExecutor will also be configured for regular task execution with @EnableAsync, even if a @Primary bean or a bean named taskExecutor of type Executor is present.
The only way to override the Executor for regular tasks is by registering an AsyncConfigurer bean.
When a ThreadPoolTaskExecutor is auto-configured, the thread pool uses 8 core threads that can grow and shrink according to the load.
Those default settings can be fine-tuned using the spring.task.execution namespace, as shown in the following example:
Properties
YAML
spring.task.execution.pool.max-size=16
spring.task.execution.pool.queue-capacity=100
spring.task.execution.pool.keep-alive=10s
spring:
 task:
 execution:
 pool:
 max-size: 16
 queue-capacity: 100
 keep-alive: "10s"
This changes the thread pool to use a bounded queue so that when the queue is full (100 tasks), the thread pool increases to maximum 16 threads.
Shrinking of the pool is more aggressive as threads are reclaimed when they are idle for 10 seconds (rather than 60 seconds by default).
A scheduler can also be auto-configured if it needs to be associated with scheduled task execution (using @EnableScheduling for instance).
If virtual threads are enabled (using Java 21+ and spring.threads.virtual.enabled set to true) this will be a SimpleAsyncTaskScheduler that uses virtual threads.
This SimpleAsyncTaskScheduler will ignore any pooling related properties.
If virtual threads are not enabled, it will be a ThreadPoolTaskScheduler with sensible defaults.
The ThreadPoolTaskScheduler uses one thread by default and its settings can be fine-tuned using the spring.task.scheduling namespace, as shown in the following example:
Properties
YAML
spring.task.scheduling.thread-name-prefix=scheduling-
spring.task.scheduling.pool.size=2
spring:
 task:
 scheduling:
 thread-name-prefix: "scheduling-"
 pool:
 size: 2
A ThreadPoolTaskExecutorBuilder bean, a SimpleAsyncTaskExecutorBuilder bean, a ThreadPoolTaskSchedulerBuilder bean and a SimpleAsyncTaskSchedulerBuilder are made available in the context if a custom executor or scheduler needs to be created.
The SimpleAsyncTaskExecutorBuilder and SimpleAsyncTaskSchedulerBuilder beans are auto-configured to use virtual threads if they are enabled (using Java 21+ and spring.threads.virtual.enabled set to true).
Spring Boot
4.1.0
4.0.7
3.5.16
3.4.13
3.3.13
4.1.1-SNAPSHOT
4.0.8-SNAPSHOT
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

Source: https://docs.spring.io/spring-boot/reference/features/task-execution-and-scheduling.html
HTTP status: 200 · extracted bytes: 22509 · sha256: 79556bb0c167bb1e9184f4d9dee26fe399bfd1402e7f6f4c7ae569bbecb150b7
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r155`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

Task Execution and Scheduling :: Spring Boot Why Spring Overview Trending Generative AI Cloud Architecture Patterns Microservices Reactive Event Driven Application Types Web Applications Serverless Batch Learn Getting Started Quickstart Guides Academy Courses Get Certified Projects Overview Projects Spring Boot Spring Framework Spring Cloud Spring AI Spring Data Spring Integration Spring Batch Spring Security Foundational Projects Micrometer Reactor Development Tools Spring Tools Spring Initializr Resources Blog Release Calendar Version Mappings Release Highlights Security Advisories GitHub Orgs Spring Projects Spring Cloud Community Overview Events Authors Enterprise Overview Long-term Support Automated Upgrades Governance and Compliance Modern App Development light Spring Boot 4.1.0 Search Overview Documentation Community System Requirements Installing Spring Boot Upgrading Spring Boot Tutorials Developing Your First Spring Boot Application Reference Developing with Spring Boot Build Systems Structuring Your Code Configuration Classes Auto-configuration Spring Beans and Dependency Injection Using the @SpringBootApplication Annotation Running Your Application Developer Tools Packaging Your Application for Production Core Features SpringApplication Externalized Configuration Profiles Logging Internationalization Aspect-Oriented Programming JSON Task Execution and Scheduling Development-time Services Creating Your Own Auto-configuration Kotlin Support SSL Web Servlet Web Applications Reactive Web Applications Graceful Shutdown Spring Security Spring Session Spring for GraphQL Spring HATEOAS Data SQL Databases Working with NoSQL Technologies IO Caching Spring Batch gRPC Hazelcast Quartz Scheduler Sending Email Validation Calling REST Services Web Services Distributed Transactions With JTA Messaging JMS AMQP Apache Kafka Support Apache Pulsar Support RSocket Spring Integration WebSockets Security OAuth2 SAML 2.0 Testing Test Modules Test Scope Dependencies Testing Spring Applications Testing Spring Boot Applications Testcontainers Test Utilities Packaging Spring Boot Applications Efficient Deployments AOT Cache Ahead-of-Time Processing With the JVM GraalVM Native Images Introducing GraalVM Native Images Advanced Native Images Topics Checkpoint and Restore With the JVM Container Images Efficient Container Images Dockerfiles Cloud Native Buildpacks Production-ready Features Enabling Production-ready Features Endpoints Monitoring and Management Over HTTP Monitoring and Management over JMX Observability Loggers Metrics Tracing Auditing Recording HTTP Exchanges Process Monitoring Cloud Foundry Support How-to Guides Spring Boot Application Properties and Configuration Embedded Web Servers Spring MVC Jersey HTTP Clients Logging Data Access Database Initialization NoSQL Messaging Batch Applications Actuator Security Hot Swapping Testing Build Ahead-of-Time Processing GraalVM Native Applications Developing Your First GraalVM Native Application Testing GraalVM Native Images AOT Cache Deploying Spring Boot Applications Traditional Deployment Deploying to the Cloud Installing Spring Boot Applications Docker Compose Build Tool Plugins Maven Plugin Getting Started Using the Plugin Goals Packaging Executable Archives Packaging OCI Images Running your Application with Maven Ahead-of-Time Processing Running Integration Tests Integrating with Actuator Help Information Gradle Plugin Getting Started Managing Dependencies Packaging Executable Archives Packaging OCI Images Publishing your Application Running your Application with Gradle Ahead-of-Time Processing Integrating with Actuator Reacting to Other Plugins Spring Boot AntLib Module Supporting Other Build Systems Spring Boot CLI Installing the CLI Using the CLI Rest APIs Actuator Audit Events ( auditevents ) Beans ( beans ) Caches ( caches ) Conditions Evaluation Report ( conditions ) Configuration Properties ( configprops ) Environment ( env ) Flyway ( flyway ) Health ( health ) Heap Dump ( heapdump ) HTTP Exchanges ( httpexchanges ) Info ( info ) Spring Integration Graph ( integrationgraph ) Liquibase ( liquibase ) Log File ( logfile ) Loggers ( loggers ) Mappings ( mappings ) Metrics ( metrics ) Prometheus ( prometheus ) Quartz ( quartz ) Software Bill of Materials ( sbom ) Scheduled Tasks ( scheduledtasks ) Sessions ( sessions ) Shutdown ( shutdown ) Application Startup ( startup ) Thread Dump ( threaddump ) Java APIs Spring Boot Gradle Plugin Maven Plugin Kotlin APIs Spring Boot Specifications Configuration Metadata Metadata Format Providing Manual Hints Generating Your Own Metadata by Using the Annotation Processor The Executable Jar Format Nested JARs Spring Boot’s “NestedJarFile” Class Launching Executable Jars PropertiesLauncher Features Executable Jar Restrictions Alternative Single Jar Solutions Appendix Common Application Properties Deprecated Application Properties Auto-configuration Classes spring-boot-activemq spring-boot-actuator-autoconfigure spring-boot-amqp spring-boot-artemis spring-boot-autoconfigure spring-boot-batch spring-boot-batch-data-mongodb spring-boot-batch-jdbc spring-boot-cache spring-boot-cassandra spring-boot-cloudfoundry spring-boot-couchbase spring-boot-data-cassandra spring-boot-data-commons spring-boot-data-couchbase spring-boot-data-elasticsearch spring-boot-data-jdbc spring-boot-data-jpa spring-boot-data-ldap spring-boot-data-mongodb spring-boot-data-neo4j spring-boot-data-r2dbc spring-boot-data-redis spring-boot-data-rest spring-boot-devtools spring-boot-elasticsearch spring-boot-flyway spring-boot-freemarker spring-boot-graphql spring-boot-groovy-templates spring-boot-grpc-client spring-boot-grpc-server spring-boot-gson spring-boot-h2console spring-boot-hateoas spring-boot-hazelcast spring-boot-health spring-boot-hibernate spring-boot-http-client spring-boot-http-codec spring-boot-http-converter spring-boot-integration spring-boot-jackson spring-boot-jackson2 spring-boot-jdbc spring-boot-jersey spring-boot-jetty spring-boot-jms spring-boot-jooq spring-boot-jsonb spring-boot-kafka spring-boot-kotlinx-serialization-json spring-boot-ldap spring-boot-liquibase spring-boot-mail spring-boot-micrometer-metrics spring-boot-micrometer-observation spring-boot-micrometer-tracing spring-boot-micrometer-tracing-brave spring-boot-micrometer-tracing-opentelemetry spring-boot-mongodb spring-boot-mustache spring-boot-neo4j spring-boot-netty spring-boot-opentelemetry spring-boot-persistence spring-boot-pulsar spring-boot-quartz spring-boot-r2dbc spring-boot-reactor spring-boot-reactor-netty spring-boot-restclient spring-boot-resttestclient spring-boot-rsocket spring-boot-security spring-boot-security-oauth2-authorization-server spring-boot-security-oauth2-client spring-boot-security-oauth2-resource-server spring-boot-security-saml2 spring-boot-sendgrid spring-boot-servlet spring-boot-session spring-boot-session-data-redis spring-boot-session-jdbc spring-boot-testcontainers spring-boot-thymeleaf spring-boot-tomcat spring-boot-transaction spring-boot-validation spring-boot-webclient spring-boot-webflux spring-boot-webmvc spring-boot-webservices spring-boot-websocket spring-boot-zipkin Test Auto-configuration Annotations Test Slices Dependency Versions Managed Dependency Coordinates Version Properties Search Edit this Page GitHub Project Stack Overflow Spring Boot Reference Core Features Task Execution and Scheduling Task Execution and Scheduling In the absence of an Executor bean in the context, Spring Boot auto-configures an AsyncTaskExecutor . When virtual threads are enabled (using Java 21+ and spring.threads.virtual.enabled set to true ) this will be a SimpleAsyncTaskExecutor that uses virtual threads. Otherwise, it will be a ThreadPoolTaskExecutor with sensible defaults. The auto-configured AsyncTaskExecutor is used for the following integrations unless a custom Executor bean is defined: Execution of asynchronous tasks using @EnableAsync , unless a bean of type AsyncConfigurer is defined. Asynchronous handling of Callable return values from controller methods in Spring for GraphQL. Asynchronous request handling in Spring MVC. Support for blocking execution in Spring WebFlux. Utilized for inbound and outbound message channels in Spring WebSocket. Bootstrap executor for JPA, based on the bootstrap mode of JPA repositories. Bootstrap executor for background initialization of beans in the ApplicationContext . While this approach works in most scenarios, Spring Boot allows you to override the auto-configured AsyncTaskExecutor . By default, when a custom Executor bean is registered, the auto-configured AsyncTaskExecutor backs off, and the custom Executor is used for regular task execution (via @EnableAsync ). However, Spring MVC, Spring WebFlux, and Spring GraphQL all require a bean named applicationTaskExecutor . For Spring MVC and Spring WebFlux, this bean must be of type AsyncTaskExecutor , whereas Spring GraphQL does not enforce this type requirement. Spring WebSocket and JPA will use AsyncTaskExecutor if either a single bean of this type is available or a bean named applicationTaskExecutor is defined. Finally, the boostrap executor of the ApplicationContext uses a bean named applicationTaskExecutor unless a bean named bootstrapExecutor is defined. The following code snippet demonstrates how to register a custom AsyncTaskExecutor to be used with Spring MVC, Spring WebFlux, Spring GraphQL, Spring WebSocket, JPA, and background initialization of beans. Java Kotlin import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration; import org.springframework.core.task.SimpleAsyncTaskExecutor; @Configuration(proxyBeanMethods = false) public class MyTaskExecutorConfiguration { @Bean("applicationTaskExecutor") SimpleAsyncTaskExecutor applicationTaskExecutor() { return new SimpleAsyncTaskExecutor("app-"); } } import org.springframework.context.annotation.Bean import org.springframework.context.annotation.Configuration import org.springframework.core.task.SimpleAsyncTaskExecutor @Configuration(proxyBeanMethods = false) class MyTaskExecutorConfiguration { @Bean("applicationTaskExecutor") fun applicationTaskExecutor(): SimpleAsyncTaskExecutor { return SimpleAsyncTaskExecutor("app-") } } The applicationTaskExecutor bean will also be used for regular task execution if there is no @Primary bean or a bean named taskExecutor of type Executor or AsyncConfigurer present in the application context. If neither the auto-configured AsyncTaskExecutor nor the applicationTaskExecutor bean is defined, the application defaults to a bean named taskExecutor for regular task execution ( @EnableAsync ), following Spring Framework’s behavior. However, this bean will not be used for Spring MVC, Spring WebFlux, Spring GraphQL. It could, however, be used for Spring WebSocket or JPA if the bean’s type is AsyncTaskExecutor . If your application needs multiple Executor beans for different integrations, such as one for regular task execution with @EnableAsync and other for Spring MVC, Spring WebFlux, Spring WebSocket and JPA, you can configure them as follows. Java Kotlin import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration; import org.springframework.core.task.SimpleAsyncTaskExecutor; import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor; @Configuration(proxyBeanMethods = false) public class MyTaskExecutorConfiguration { @Bean("applicationTaskExecutor") SimpleAsyncTaskExecutor applicationTaskExecutor() { return new SimpleAsyncTaskExecutor("app-"); } @Bean("taskExecutor") ThreadPoolTaskExecutor taskExecutor() { ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor(); threadPoolTaskExecutor.setThreadNamePrefix("async-"); return threadPoolTaskExecutor; } } import org.springframework.context.annotation.Bean import org.springframework.context.annotation.Configuration import org.springframework.core.task.SimpleAsyncTaskExecutor import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor @Configuration(proxyBeanMethods = false) class MyTaskExecutorConfiguration { @Bean("applicationTaskExecutor") fun applicationTaskExecutor(): SimpleAsyncTaskExecutor { return SimpleAsyncTaskExecutor("app-") } @Bean("taskExecutor") fun taskExecutor(): ThreadPoolTaskExecutor { val threadPoolTaskExecutor = ThreadPoolTaskExecutor() threadPoolTaskExecutor.setThreadNamePrefix("async-") return threadPoolTaskExecutor } } The auto-configured ThreadPoolTaskExecutorBuilder or SimpleAsyncTaskExecutorBuilder allow you to easily create instances of type AsyncTaskExecutor that replicate the default behavior of auto-configuration. Java Kotlin import org.springframework.boot.task.SimpleAsyncTaskExecutorBuilder; import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration; import org.springframework.core.task.SimpleAsyncTaskExecutor; @Configuration(proxyBeanMethods = false) public class MyTaskExecutorConfiguration { @Bean SimpleAsyncTaskExecutor taskExecutor(SimpleAsyncTaskExecutorBuilder builder) { return builder.build(); } } import org.springframework.boot.task.SimpleAsyncTaskExecutorBuilder import org.springframework.context.annotation.Bean import org.springframework.context.annotation.Configuration import org.springframework.core.task.SimpleAsyncTaskExecutor @Configuration(proxyBeanMethods = false) class MyTaskExecutorConfiguration { @Bean fun taskExecutor(builder: SimpleAsyncTaskExecutorBuilder): SimpleAsyncTaskExecutor { return builder.build() } } If a taskExecutor named bean is not an option, you can mark your bean as @Primary or define an AsyncConfigurer bean to specify the Executor responsible for handling regular task execution with @EnableAsync . The following example demonstrates how to achieve this. Java Kotlin import java.util.concurrent.Executor; import java.util.concurrent.ExecutorService; import java.util.concurrent.Executors; import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration; import org.springframework.scheduling.annotation.AsyncConfigurer; @Configuration(proxyBeanMethods = false) public class MyTaskExecutorConfiguration { @Bean AsyncConfigurer asyncConfigurer(ExecutorService executorService) { return new AsyncConfigurer() { @Override public Executor getAsyncExecutor() { return executorService; } }; } @Bean ExecutorService executorService() { return Executors.newCachedThreadPool(); } } import org.springframework.context.annotation.Bean import org.springframework.context.annotation.Configuration import org.springframework.scheduling.annotation.AsyncConfigurer import java.util.concurrent.Executor import java.util.concurrent.ExecutorService import java.util.concurrent.Executors @Configuration(proxyBeanMethods = false) class MyTaskExecutorConfiguration { @Bean fun asyncConfigurer(executorService: ExecutorService): AsyncConfigurer { return object : AsyncConfigurer { override fun getAsyncExecutor(): Executor { return executorService } } } @Bean fun executorService(): ExecutorService { return Executors.newCachedThreadPool() } } To register a custom Executor while keeping the auto-configured AsyncTaskExecutor , you can create a custom Executor bean and set the defaultCandidate=false attribute in its @Bean annotation, as demonstrated in the following example: Java Kotlin import java.util.concurrent.Executors; import java.util.concurrent.ScheduledExecutorService; import org.springframework.beans.factory.annotation.Qualifier; import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration; @Configuration(proxyBeanMethods = false) public class MyTaskExecutorConfiguration { @Bean(defaultCandidate = false) @Qualifier("scheduledExecutorService") ScheduledExecutorService scheduledExecutorService() { return Executors.newSingleThreadScheduledExecutor(); } } import org.springframework.beans.factory.annotation.Qualifier import org.springframework.context.annotation.Bean import org.springframework.context.annotation.Configuration import java.util.concurrent.Executors import java.util.concurrent.ScheduledExecutorService @Configuration(proxyBeanMethods = false) class MyTaskExecutorConfiguration { @Bean(defaultCandidate = false) @Qualifier("scheduledExecutorService") fun scheduledExecutorService(): ScheduledExecutorService { return Executors.newSingleThreadScheduledExecutor() } } In that case, you will be able to autowire your custom Executor into other components while retaining the auto-configured AsyncTaskExecutor . However, remember to use the @Qualifier annotation alongside @Autowired . If this is not possible for you, you can request Spring Boot to auto-configure an AsyncTaskExecutor anyway, as follows: Properties YAML spring.task.execution.mode=force spring: task: execution: mode: force The auto-configured AsyncTaskExecutor will be used automatically for all integrations, even if a custom Executor bean is registered, including those marked as @Primary . These integrations include: Asynchronous task execution ( @EnableAsync ), unless an AsyncConfigurer bean is present. Spring for GraphQL’s asynchronous handling of Callable return values from controller methods. Spring MVC’s asynchronous request processing. Spring WebFlux’s blocking execution support. Utilized for inbound and outbound message channels in Spring WebSocket. Bootstrap executor for JPA, based on the bootstrap mode of JPA repositories. Bootstrap executor for background initialization of beans in the ApplicationContext , unless a bean named bootstrapExecutor is defined. Depending on your target arrangement, you could set spring.task.execution.mode to force to auto-configure an applicationTaskExecutor , change your Executor into an AsyncTaskExecutor or define both an AsyncTaskExecutor and an AsyncConfigurer wrapping your custom Executor . When force mode is enabled, applicationTaskExecutor will also be configured for regular task execution with @EnableAsync , even if a @Primary bean or a bean named taskExecutor of type Executor is present. The only way to override the Executor for regular tasks is by registering an AsyncConfigurer bean. When a ThreadPoolTaskExecutor is auto-configured, the thread pool uses 8 core threads that can grow and shrink according to the load. Those default settings can be fine-tuned using the spring.task.execution namespace, as shown in the following example: Properties YAML spring.task.execution.pool.max-size=16 spring.task.execution.pool.queue-capacity=100 spring.task.execution.pool.keep-alive=10s spring: task: execution: pool: max-size: 16 queue-capacity: 100 keep-alive: "10s" This changes the thread pool to use a bounded queue so that when the queue is full (100 tasks), the thread pool increases to maximum 16 threads. Shrinking of the pool is more aggressive as threads are reclaimed when they are idle for 10 seconds (rather than 60 seconds by default). A scheduler can also be auto-configured if it needs to be associated with scheduled task execution (using @EnableScheduling for instance). If virtual threads are enabled (using Java 21+ and spring.threads.virtual.enabled set to true ) this will be a SimpleAsyncTaskScheduler that uses virtual threads. This SimpleAsyncTaskScheduler will ignore any pooling related properties. If virtual threads are not enabled, it will be a ThreadPoolTaskScheduler with sensible defaults. The ThreadPoolTaskScheduler uses one thread by default and its settings can be fine-tuned using the spring.task.scheduling namespace, as shown in the following example: Properties YAML spring.task.scheduling.thread-name-prefix=scheduling- spring.task.scheduling.pool.size=2 spring: task: scheduling: thread-name-prefix: "scheduling-" pool: size: 2 A ThreadPoolTaskExecutorBuilder bean, a SimpleAsyncTaskExecutorBuilder bean, a ThreadPoolTaskSchedulerBuilder bean and a SimpleAsyncTaskSchedulerBuilder are made available in the context if a custom executor or scheduler needs to be created. The SimpleAsyncTaskExecutorBuilder and SimpleAsyncTaskSchedulerBuilder beans are auto-configured to use virtual threads if they are enabled (using Java 21+ and spring.threads.virtual.enabled set to true ). JSON Development-time Services Spring Boot Stable 4.1.0 4.0.7 3.5.16 3.4.13 3.3.13 Snapshot 4.2.0-SNAPSHOT 4.1.1-SNAPSHOT 4.0.8-SNAPSHOT Related Spring Documentation Spring Boot Spring Framework Spring Cloud Spring Cloud Build Spring Cloud Bus Spring Cloud Circuit Breaker Spring Cloud Commons Spring Cloud Config Spring Cloud Consul Spring Cloud Contract Spring Cloud Function Spring Cloud Gateway Spring Cloud Kubernetes Spring Cloud Netflix Spring Cloud OpenFeign Spring Cloud Stream Spring Cloud Task Spring Cloud Vault Spring Cloud Zookeeper Spring Data Spring Data Cassandra Spring Data Commons Spring Data Couchbase Spring Data Elasticsearch Spring Data JPA Spring Data KeyValue Spring Data LDAP Spring Data MongoDB Spring Data Neo4j Spring Data Redis Spring Data JDBC & R2DBC Spring Data REST Spring Integration Spring Batch Spring Security Spring Authorization Server Spring LDAP Spring Security Kerberos Spring Session Spring Vault Spring AI Spring AMQP Spring CLI Spring GraphQL Spring for Apache Kafka Spring Modulith Spring for Apache Pulsar Spring Shell All Docs... Copyright © 2005 - Broadcom. All Rights Reserved. The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries. Terms of Use • Privacy • Trademark Guidelines • Thank you • Your California Privacy Rights • Cookie Settings Apache®, Apache Tomcat®, Apache Kafka®, Apache Cassandra™, and Apache Geode™ are trademarks or registered trademarks of the Apache Software Foundation in the United States and/or other countries. Java™, Java™ SE, Java™ EE, and OpenJDK™ are trademarks of Oracle and/or its affiliates. Kubernetes® is a registered trademark of the Linux Foundation in the United States and other countries. Linux® is the registered trademark of Linus Torvalds in the United States and other countries. Windows® and Microsoft® Azure are registered trademarks of Microsoft Corporation. “AWS” and “Amazon Web Services” are trademarks or registered trademarks of Amazon.com Inc. or its affiliates. All other trademarks and copyrights are property of their respective owners and are only mentioned for informative purposes. Other names may be trademarks of their respective owners. Search in all Spring Docs
