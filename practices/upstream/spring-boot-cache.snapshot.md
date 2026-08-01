# spring-boot-cache — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://docs.spring.io/spring-boot/reference/io/caching.html (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T02:24:27Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://docs.spring.io/spring-boot/reference/io/caching.html`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r134`
**Body SHA-256 (below the `---` divider, header excluded):** 56710b5655127780d64b40deac067df8135321dba2ec5be85fa5fa3f9cde8864

---

---
snapshot_id: spring-boot-cache
source: "https://docs.spring.io/spring-boot/reference/io/caching.html"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 3
bytes: 19861
sha: "6cbc06505febd5e98a28e3ebd3f54d351461f073c3f3714c711e4eea7c1076d8"
---

# spring boot cache — upstream snapshot

Source: https://docs.spring.io/spring-boot/reference/io/caching.html
Fetched: 2026-07-14

Caching :: Spring Boot
Edit this Page
 
 
 
 GitHub Project
 
 
 
 Stack Overflow

# Caching
The Spring Framework provides support for transparently adding caching to an application.
At its core, the abstraction applies caching to methods, thus reducing the number of executions based on the information available in the cache.
The caching logic is applied transparently, without any interference to the invoker.
For more details, check the relevant section of the Spring Framework reference documentation.
Spring Boot auto-configures the cache infrastructure as long as caching support is enabled by using the @EnableCaching annotation.
Avoid adding @EnableCaching to the main method’s application class.
Doing so makes caching a mandatory feature, including when running a test suite.
To add caching to an operation of your service add the relevant annotation to its method, as shown in the following example:
Java
Kotlin
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class MyMathService {

 @Cacheable("piDecimals")
 public int computePiDecimal(int precision) {
 ...
 }

}
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
class MyMathService {

 @Cacheable("piDecimals")
 fun computePiDecimal(precision: Int): Int {
 ...
 }

}
This example demonstrates the use of caching on a potentially costly operation.
Before invoking computePiDecimal, the abstraction looks for an entry in the piDecimals cache that matches the precision argument.
If an entry is found, the content in the cache is immediately returned to the caller, and the method is not invoked.
Otherwise, the method is invoked, and the cache is updated before returning the value.
You can also use the standard JSR-107 (JCache) annotations (such as @CacheResult) transparently.
However, we strongly advise you to not mix and match the Spring Cache and JCache annotations.
If you do not add any specific cache library, Spring Boot auto-configures a simple provider that uses concurrent maps in memory.
When a cache is required (such as piDecimals in the preceding example), this provider creates it for you.
The simple provider is not really recommended for production usage, but it is great for getting started and making sure that you understand the features.
When you have made up your mind about the cache provider to use, please make sure to read its documentation to figure out how to configure the caches that your application uses.
Nearly all providers require you to explicitly configure every cache that you use in the application.
Some offer a way to customize the default caches defined by the spring.cache.cache-names property.
It is also possible to transparently update or evict data from the cache.

## Supported Cache Providers
The cache abstraction does not provide an actual store and relies on abstraction materialized by the Cache and CacheManager interfaces.
If you have not defined a bean of type CacheManager or a CacheResolver named cacheResolver (see CachingConfigurer), Spring Boot tries to detect the following providers (in the indicated order):
Generic
JCache (JSR-107) (EhCache 3, Hazelcast, Infinispan, and others)
Hazelcast
Infinispan
Couchbase
Redis
Caffeine
Cache2k
Simple
If the CacheManager is auto-configured by Spring Boot, it is possible to force a particular cache provider by setting the spring.cache.type property.
Use the spring-boot-starter-cache starter to quickly add basic caching dependencies.
The starter brings in spring-context-support.
If you add dependencies manually, you must include spring-context-support in order to use the JCache or Caffeine support.
If the CacheManager is auto-configured by Spring Boot, you can further tune its configuration before it is fully initialized by exposing a bean that implements the CacheManagerCustomizer interface.
The following example sets a flag to say that null values should not be passed down to the underlying map:
Java
Kotlin
import org.springframework.boot.cache.autoconfigure.CacheManagerCustomizer;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MyCacheManagerConfiguration {

 @Bean
 public CacheManagerCustomizer cacheManagerCustomizer() {
 return (cacheManager) -> cacheManager.setAllowNullValues(false);
 }

}
import org.springframework.boot.cache.autoconfigure.CacheManagerCustomizer
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class MyCacheManagerConfiguration {

 @Bean
 fun cacheManagerCustomizer(): CacheManagerCustomizer {
 return CacheManagerCustomizer { cacheManager ->
 cacheManager.isAllowNullValues = false
 }
 }

}
In the preceding example, an auto-configured ConcurrentMapCacheManager is expected.
If that is not the case (either you provided your own config or a different cache provider was auto-configured), the customizer is not invoked at all.
You can have as many customizers as you want, and you can also order them by using @Order or Ordered.

### Generic
Generic caching is used if the context defines at least one Cache bean.
A CacheManager wrapping all beans of that type is created.

### JCache (JSR-107)
JCache is bootstrapped through the presence of a CachingProvider on the classpath (that is, a JSR-107 compliant caching library exists on the classpath), and the JCacheCacheManager is provided by the spring-boot-starter-cache starter.
Various compliant libraries are available, and Spring Boot provides dependency management for Ehcache 3, Hazelcast, and Infinispan.
Any other compliant library can be added as well.
It might happen that more than one provider is present, in which case the provider must be explicitly specified.
Even if the JSR-107 standard does not enforce a standardized way to define the location of the configuration file, Spring Boot does its best to accommodate setting a cache with implementation details, as shown in the following example:
Properties
YAML
spring.cache.jcache.provider=com.example.MyCachingProvider
spring.cache.jcache.config=classpath:example.xml
# Only necessary if more than one provider is present
spring:
 cache:
 jcache:
 provider: "com.example.MyCachingProvider"
 config: "classpath:example.xml"
When a cache library offers both a native implementation and JSR-107 support, Spring Boot prefers the JSR-107 support, so that the same features are available if you switch to a different JSR-107 implementation.
Spring Boot has general support for Hazelcast.
If a single HazelcastInstance is available, it is automatically reused for the CacheManager as well, unless the spring.cache.jcache.config property is specified.
There are two ways to customize the underlying CacheManager:
Caches can be created on startup by setting the spring.cache.cache-names property.
If a custom Configuration bean is defined, it is used to customize them.
CacheManagerCustomizer beans are invoked with the reference of the CacheManager for full customization.
If a standard CacheManager bean is defined, it is wrapped automatically in an CacheManager implementation that the abstraction expects.
No further customization is applied to it.

### Hazelcast
Spring Boot has general support for Hazelcast.
If a HazelcastInstance has been auto-configured and com.hazelcast:hazelcast-spring is on the classpath, it is automatically wrapped in a CacheManager.
Hazelcast can be used as a JCache compliant cache or as a Spring CacheManager compliant cache.
When setting spring.cache.type to hazelcast, Spring Boot will use the CacheManager based implementation.
If you want to use Hazelcast as a JCache compliant cache, set spring.cache.type to jcache.
If you have multiple JCache compliant cache providers and want to force the use of Hazelcast, you have to explicitly set the JCache provider.

### Infinispan
Infinispan has no default configuration file location, so it must be specified explicitly.
Otherwise, the default bootstrap is used.
Properties
YAML
spring.cache.infinispan.config=infinispan.xml
spring:
 cache:
 infinispan:
 config: "infinispan.xml"
Caches can be created on startup by setting the spring.cache.cache-names property.
If a custom ConfigurationBuilder bean is defined, it is used to customize the caches.
For more details, see the documentation.

### Couchbase
If Spring Data Couchbase is available and Couchbase is configured, a CouchbaseCacheManager is auto-configured.
It is possible to create additional caches on startup by setting the spring.cache.cache-names property and cache defaults can be configured by using spring.cache.couchbase.* properties.
For instance, the following configuration creates cache1 and cache2 caches with an entry expiration of 10 minutes:
Properties
YAML
spring.cache.cache-names=cache1,cache2
spring.cache.couchbase.expiration=10m
spring:
 cache:
 cache-names: "cache1,cache2"
 couchbase:
 expiration: "10m"
If you need more control over the configuration, consider registering a CouchbaseCacheManagerBuilderCustomizer bean.
The following example shows a customizer that configures a specific entry expiration for cache1 and cache2:
Java
Kotlin
import java.time.Duration;

import org.springframework.boot.cache.autoconfigure.CouchbaseCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.cache.CouchbaseCacheConfiguration;

@Configuration(proxyBeanMethods = false)
public class MyCouchbaseCacheManagerConfiguration {

 @Bean
 public CouchbaseCacheManagerBuilderCustomizer myCouchbaseCacheManagerBuilderCustomizer() {
 return (builder) -> builder
 .withCacheConfiguration("cache1", CouchbaseCacheConfiguration
 .defaultCacheConfig().entryExpiry(Duration.ofSeconds(10)))
 .withCacheConfiguration("cache2", CouchbaseCacheConfiguration
 .defaultCacheConfig().entryExpiry(Duration.ofMinutes(1)));

 }

}
import org.springframework.boot.cache.autoconfigure.CouchbaseCacheManagerBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.couchbase.cache.CouchbaseCacheConfiguration
import java.time.Duration

@Configuration(proxyBeanMethods = false)
class MyCouchbaseCacheManagerConfiguration {

 @Bean
 fun myCouchbaseCacheManagerBuilderCustomizer(): CouchbaseCacheManagerBuilderCustomizer {
 return CouchbaseCacheManagerBuilderCustomizer { builder ->
 builder
 .withCacheConfiguration(
 "cache1", CouchbaseCacheConfiguration
 .defaultCacheConfig().entryExpiry(Duration.ofSeconds(10))
 )
 .withCacheConfiguration(
 "cache2", CouchbaseCacheConfiguration
 .defaultCacheConfig().entryExpiry(Duration.ofMinutes(1))
 )
 }
 }

}

### Redis
If Redis is available and configured, a RedisCacheManager is auto-configured.
It is possible to create additional caches on startup by setting the spring.cache.cache-names property and cache defaults can be configured by using spring.cache.redis.* properties.
For instance, the following configuration creates cache1 and cache2 caches with a time to live of 10 minutes:
Properties
YAML
spring.cache.cache-names=cache1,cache2
spring.cache.redis.time-to-live=10m
spring:
 cache:
 cache-names: "cache1,cache2"
 redis:
 time-to-live: "10m"
By default, a key prefix is added so that, if two separate caches use the same key, Redis does not have overlapping keys and cannot return invalid values.
We strongly recommend keeping this setting enabled if you create your own RedisCacheManager.
You can take full control of the default configuration by adding a RedisCacheConfiguration @Bean of your own.
This can be useful if you need to customize the default serialization strategy.
If you need more control over the configuration, consider registering a RedisCacheManagerBuilderCustomizer bean.
The following example shows a customizer that configures a specific time to live for cache1 and cache2:
Java
Kotlin
import java.time.Duration;

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

@Configuration(proxyBeanMethods = false)
public class MyRedisCacheManagerConfiguration {

 @Bean
 public RedisCacheManagerBuilderCustomizer myRedisCacheManagerBuilderCustomizer() {
 return (builder) -> builder
 .withCacheConfiguration("cache1", RedisCacheConfiguration
 .defaultCacheConfig().entryTtl(Duration.ofSeconds(10)))
 .withCacheConfiguration("cache2", RedisCacheConfiguration
 .defaultCacheConfig().entryTtl(Duration.ofMinutes(1)));

 }

}
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import java.time.Duration

@Configuration(proxyBeanMethods = false)
class MyRedisCacheManagerConfiguration {

 @Bean
 fun myRedisCacheManagerBuilderCustomizer(): RedisCacheManagerBuilderCustomizer {
 return RedisCacheManagerBuilderCustomizer { builder ->
 builder
 .withCacheConfiguration(
 "cache1", RedisCacheConfiguration
 .defaultCacheConfig().entryTtl(Duration.ofSeconds(10))
 )
 .withCacheConfiguration(
 "cache2", RedisCacheConfiguration
 .defaultCacheConfig().entryTtl(Duration.ofMinutes(1))
 )
 }
 }

}

### Caffeine
Caffeine is a Java 8 rewrite of Guava’s cache that supersedes support for Guava.
If Caffeine is present, a CaffeineCacheManager (provided by the spring-boot-starter-cache starter) is auto-configured.
Caches can be created on startup by setting the spring.cache.cache-names property and can be customized by one of the following (in the indicated order):
A cache spec defined by spring.cache.caffeine.spec
A CaffeineSpec bean is defined
A Caffeine bean is defined
For instance, the following configuration creates cache1 and cache2 caches with a maximum size of 500 and a time to live of 10 minutes
Properties
YAML
spring.cache.cache-names=cache1,cache2
spring.cache.caffeine.spec=maximumSize=500,expireAfterAccess=600s
spring:
 cache:
 cache-names: "cache1,cache2"
 caffeine:
 spec: "maximumSize=500,expireAfterAccess=600s"
If a CacheLoader bean is defined, it is automatically associated to the CaffeineCacheManager.
Since the CacheLoader is going to be associated with all caches managed by the cache manager, it must be defined as CacheLoader.
The auto-configuration ignores any other generic type.

### Cache2k
Cache2k is an in-memory cache.
If the Cache2k spring integration is present, a SpringCache2kCacheManager is auto-configured.
Caches can be created on startup by setting the spring.cache.cache-names property.
Cache defaults can be customized using a Cache2kBuilderCustomizer bean.
The following example shows a customizer that configures the capacity of the cache to 200 entries, with an expiration of 5 minutes:
Java
Kotlin
import java.util.concurrent.TimeUnit;

import org.springframework.boot.cache.autoconfigure.Cache2kBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MyCache2kDefaultsConfiguration {

 @Bean
 public Cache2kBuilderCustomizer myCache2kDefaultsCustomizer() {
 return (builder) -> builder.entryCapacity(200)
 .expireAfterWrite(5, TimeUnit.MINUTES);
 }

}
import org.springframework.boot.cache.autoconfigure.Cache2kBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration(proxyBeanMethods = false)
class MyCache2kDefaultsConfiguration {

 @Bean
 fun myCache2kDefaultsCustomizer(): Cache2kBuilderCustomizer {
 return Cache2kBuilderCustomizer { builder ->
 builder.entryCapacity(200)
 .expireAfterWrite(5, TimeUnit.MINUTES)
 }
 }
}

### Simple
If none of the other providers can be found, a simple implementation using a ConcurrentHashMap as the cache store is configured.
This is the default if no caching library is present in your application.
By default, caches are created as needed, but you can restrict the list of available caches by setting the cache-names property.
For instance, if you want only cache1 and cache2 caches, set the cache-names property as follows:
Properties
YAML
spring.cache.cache-names=cache1,cache2
spring:
 cache:
 cache-names: "cache1,cache2"
If you do so and your application uses a cache not listed, then it fails at runtime when the cache is needed, but not on startup.
This is similar to the way the "real" cache providers behave if you use an undeclared cache.

### None
If you need to use a no-op cache rather than the auto-configured cache manager in a certain environment, set the cache type to none, as shown in the following example:
Properties
YAML
spring.cache.type=none
spring:
 cache:
 type: "none"

## Testing
It is generally useful to use a no-op implementation when running a test suite.
This section lists a number of strategies that are useful for tests.
When a custom CacheManager is defined, the best option is to make sure that caching configuration is defined in an isolated @Configuration class.
Doing so makes sure that caching is not required by slice tests.
For tests that enable a full context, such as @SpringBootTest, an explicit configuration overriding the regular configuration is required.
If caching is auto-configured, more options are available.
Tests can be annotated with @AutoConfigureCache to replace the auto-configured CacheManager by a no-op implementation.
Java
Kotlin
import org.springframework.boot.cache.test.autoconfigure.AutoConfigureCache;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@AutoConfigureCache
public class MyIntegrationTests {

 // Tests use a no-op cache manager

}
import org.springframework.boot.cache.test.autoconfigure.AutoConfigureCache
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureCache
class MyIntegrationTests {

 // Tests use a no-op cache manager

}
Another option is to force a no-op implementation for the auto-configured CacheManager:
Properties
YAML
spring.cache.type=none
spring:
 cache:
 type: "none"
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

Source: https://docs.spring.io/spring-boot/reference/io/caching.html
HTTP status: 200 · extracted bytes: 28067 · sha256: 4f8e99ed7c2f49a40fd19ac56d36360d87897117247605c4562d2582f46ff340
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r134`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

Caching :: Spring Boot Why Spring Overview Trending Generative AI Cloud Architecture Patterns Microservices Reactive Event Driven Application Types Web Applications Serverless Batch Learn Getting Started Quickstart Guides Academy Courses Get Certified Projects Overview Projects Spring Boot Spring Framework Spring Cloud Spring AI Spring Data Spring Integration Spring Batch Spring Security Foundational Projects Micrometer Reactor Development Tools Spring Tools Spring Initializr Resources Blog Release Calendar Version Mappings Release Highlights Security Advisories GitHub Orgs Spring Projects Spring Cloud Community Overview Events Authors Enterprise Overview Long-term Support Automated Upgrades Governance and Compliance Modern App Development light Spring Boot 4.1.0 Search Overview Documentation Community System Requirements Installing Spring Boot Upgrading Spring Boot Tutorials Developing Your First Spring Boot Application Reference Developing with Spring Boot Build Systems Structuring Your Code Configuration Classes Auto-configuration Spring Beans and Dependency Injection Using the @SpringBootApplication Annotation Running Your Application Developer Tools Packaging Your Application for Production Core Features SpringApplication Externalized Configuration Profiles Logging Internationalization Aspect-Oriented Programming JSON Task Execution and Scheduling Development-time Services Creating Your Own Auto-configuration Kotlin Support SSL Web Servlet Web Applications Reactive Web Applications Graceful Shutdown Spring Security Spring Session Spring for GraphQL Spring HATEOAS Data SQL Databases Working with NoSQL Technologies IO Caching Spring Batch gRPC Hazelcast Quartz Scheduler Sending Email Validation Calling REST Services Web Services Distributed Transactions With JTA Messaging JMS AMQP Apache Kafka Support Apache Pulsar Support RSocket Spring Integration WebSockets Security OAuth2 SAML 2.0 Testing Test Modules Test Scope Dependencies Testing Spring Applications Testing Spring Boot Applications Testcontainers Test Utilities Packaging Spring Boot Applications Efficient Deployments AOT Cache Ahead-of-Time Processing With the JVM GraalVM Native Images Introducing GraalVM Native Images Advanced Native Images Topics Checkpoint and Restore With the JVM Container Images Efficient Container Images Dockerfiles Cloud Native Buildpacks Production-ready Features Enabling Production-ready Features Endpoints Monitoring and Management Over HTTP Monitoring and Management over JMX Observability Loggers Metrics Tracing Auditing Recording HTTP Exchanges Process Monitoring Cloud Foundry Support How-to Guides Spring Boot Application Properties and Configuration Embedded Web Servers Spring MVC Jersey HTTP Clients Logging Data Access Database Initialization NoSQL Messaging Batch Applications Actuator Security Hot Swapping Testing Build Ahead-of-Time Processing GraalVM Native Applications Developing Your First GraalVM Native Application Testing GraalVM Native Images AOT Cache Deploying Spring Boot Applications Traditional Deployment Deploying to the Cloud Installing Spring Boot Applications Docker Compose Build Tool Plugins Maven Plugin Getting Started Using the Plugin Goals Packaging Executable Archives Packaging OCI Images Running your Application with Maven Ahead-of-Time Processing Running Integration Tests Integrating with Actuator Help Information Gradle Plugin Getting Started Managing Dependencies Packaging Executable Archives Packaging OCI Images Publishing your Application Running your Application with Gradle Ahead-of-Time Processing Integrating with Actuator Reacting to Other Plugins Spring Boot AntLib Module Supporting Other Build Systems Spring Boot CLI Installing the CLI Using the CLI Rest APIs Actuator Audit Events ( auditevents ) Beans ( beans ) Caches ( caches ) Conditions Evaluation Report ( conditions ) Configuration Properties ( configprops ) Environment ( env ) Flyway ( flyway ) Health ( health ) Heap Dump ( heapdump ) HTTP Exchanges ( httpexchanges ) Info ( info ) Spring Integration Graph ( integrationgraph ) Liquibase ( liquibase ) Log File ( logfile ) Loggers ( loggers ) Mappings ( mappings ) Metrics ( metrics ) Prometheus ( prometheus ) Quartz ( quartz ) Software Bill of Materials ( sbom ) Scheduled Tasks ( scheduledtasks ) Sessions ( sessions ) Shutdown ( shutdown ) Application Startup ( startup ) Thread Dump ( threaddump ) Java APIs Spring Boot Gradle Plugin Maven Plugin Kotlin APIs Spring Boot Specifications Configuration Metadata Metadata Format Providing Manual Hints Generating Your Own Metadata by Using the Annotation Processor The Executable Jar Format Nested JARs Spring Boot’s “NestedJarFile” Class Launching Executable Jars PropertiesLauncher Features Executable Jar Restrictions Alternative Single Jar Solutions Appendix Common Application Properties Deprecated Application Properties Auto-configuration Classes spring-boot-activemq spring-boot-actuator-autoconfigure spring-boot-amqp spring-boot-artemis spring-boot-autoconfigure spring-boot-batch spring-boot-batch-data-mongodb spring-boot-batch-jdbc spring-boot-cache spring-boot-cassandra spring-boot-cloudfoundry spring-boot-couchbase spring-boot-data-cassandra spring-boot-data-commons spring-boot-data-couchbase spring-boot-data-elasticsearch spring-boot-data-jdbc spring-boot-data-jpa spring-boot-data-ldap spring-boot-data-mongodb spring-boot-data-neo4j spring-boot-data-r2dbc spring-boot-data-redis spring-boot-data-rest spring-boot-devtools spring-boot-elasticsearch spring-boot-flyway spring-boot-freemarker spring-boot-graphql spring-boot-groovy-templates spring-boot-grpc-client spring-boot-grpc-server spring-boot-gson spring-boot-h2console spring-boot-hateoas spring-boot-hazelcast spring-boot-health spring-boot-hibernate spring-boot-http-client spring-boot-http-codec spring-boot-http-converter spring-boot-integration spring-boot-jackson spring-boot-jackson2 spring-boot-jdbc spring-boot-jersey spring-boot-jetty spring-boot-jms spring-boot-jooq spring-boot-jsonb spring-boot-kafka spring-boot-kotlinx-serialization-json spring-boot-ldap spring-boot-liquibase spring-boot-mail spring-boot-micrometer-metrics spring-boot-micrometer-observation spring-boot-micrometer-tracing spring-boot-micrometer-tracing-brave spring-boot-micrometer-tracing-opentelemetry spring-boot-mongodb spring-boot-mustache spring-boot-neo4j spring-boot-netty spring-boot-opentelemetry spring-boot-persistence spring-boot-pulsar spring-boot-quartz spring-boot-r2dbc spring-boot-reactor spring-boot-reactor-netty spring-boot-restclient spring-boot-resttestclient spring-boot-rsocket spring-boot-security spring-boot-security-oauth2-authorization-server spring-boot-security-oauth2-client spring-boot-security-oauth2-resource-server spring-boot-security-saml2 spring-boot-sendgrid spring-boot-servlet spring-boot-session spring-boot-session-data-redis spring-boot-session-jdbc spring-boot-testcontainers spring-boot-thymeleaf spring-boot-tomcat spring-boot-transaction spring-boot-validation spring-boot-webclient spring-boot-webflux spring-boot-webmvc spring-boot-webservices spring-boot-websocket spring-boot-zipkin Test Auto-configuration Annotations Test Slices Dependency Versions Managed Dependency Coordinates Version Properties Search Edit this Page GitHub Project Stack Overflow Spring Boot Reference IO Caching Caching The Spring Framework provides support for transparently adding caching to an application. At its core, the abstraction applies caching to methods, thus reducing the number of executions based on the information available in the cache. The caching logic is applied transparently, without any interference to the invoker. For more details, check the relevant section of the Spring Framework reference documentation. Spring Boot auto-configures the cache infrastructure as long as caching support is enabled by using the @EnableCaching annotation. Avoid adding @EnableCaching to the main method’s application class. Doing so makes caching a mandatory feature, including when running a test suite . To add caching to an operation of your service add the relevant annotation to its method, as shown in the following example: Java Kotlin import org.springframework.cache.annotation.Cacheable; import org.springframework.stereotype.Component; @Component public class MyMathService { @Cacheable("piDecimals") public int computePiDecimal(int precision) { ... } } import org.springframework.cache.annotation.Cacheable import org.springframework.stereotype.Component @Component class MyMathService { @Cacheable("piDecimals") fun computePiDecimal(precision: Int): Int { ... } } This example demonstrates the use of caching on a potentially costly operation. Before invoking computePiDecimal , the abstraction looks for an entry in the piDecimals cache that matches the precision argument. If an entry is found, the content in the cache is immediately returned to the caller, and the method is not invoked. Otherwise, the method is invoked, and the cache is updated before returning the value. You can also use the standard JSR-107 (JCache) annotations (such as @CacheResult ) transparently. However, we strongly advise you to not mix and match the Spring Cache and JCache annotations. If you do not add any specific cache library, Spring Boot auto-configures a simple provider that uses concurrent maps in memory. When a cache is required (such as piDecimals in the preceding example), this provider creates it for you. The simple provider is not really recommended for production usage, but it is great for getting started and making sure that you understand the features. When you have made up your mind about the cache provider to use, please make sure to read its documentation to figure out how to configure the caches that your application uses. Nearly all providers require you to explicitly configure every cache that you use in the application. Some offer a way to customize the default caches defined by the spring.cache.cache-names property. It is also possible to transparently update or evict data from the cache. Supported Cache Providers The cache abstraction does not provide an actual store and relies on abstraction materialized by the Cache and CacheManager interfaces. If you have not defined a bean of type CacheManager or a CacheResolver named cacheResolver (see CachingConfigurer ), Spring Boot tries to detect the following providers (in the indicated order): Generic JCache (JSR-107) (EhCache 3, Hazelcast, Infinispan, and others) Hazelcast Infinispan Couchbase Redis Caffeine Cache2k Simple If the CacheManager is auto-configured by Spring Boot, it is possible to force a particular cache provider by setting the spring.cache.type property. Use the spring-boot-starter-cache starter to quickly add basic caching dependencies. The starter brings in spring-context-support . If you add dependencies manually, you must include spring-context-support in order to use the JCache or Caffeine support. If the CacheManager is auto-configured by Spring Boot, you can further tune its configuration before it is fully initialized by exposing a bean that implements the CacheManagerCustomizer interface. The following example sets a flag to say that null values should not be passed down to the underlying map: Java Kotlin import org.springframework.boot.cache.autoconfigure.CacheManagerCustomizer; import org.springframework.cache.concurrent.ConcurrentMapCacheManager; import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration; @Configuration(proxyBeanMethods = false) public class MyCacheManagerConfiguration { @Bean public CacheManagerCustomizer<ConcurrentMapCacheManager> cacheManagerCustomizer() { return (cacheManager) -> cacheManager.setAllowNullValues(false); } } import org.springframework.boot.cache.autoconfigure.CacheManagerCustomizer import org.springframework.cache.concurrent.ConcurrentMapCacheManager import org.springframework.context.annotation.Bean import org.springframework.context.annotation.Configuration @Configuration(proxyBeanMethods = false) class MyCacheManagerConfiguration { @Bean fun cacheManagerCustomizer(): CacheManagerCustomizer<ConcurrentMapCacheManager> { return CacheManagerCustomizer { cacheManager -> cacheManager.isAllowNullValues = false } } } In the preceding example, an auto-configured ConcurrentMapCacheManager is expected. If that is not the case (either you provided your own config or a different cache provider was auto-configured), the customizer is not invoked at all. You can have as many customizers as you want, and you can also order them by using @Order or Ordered . Generic Generic caching is used if the context defines at least one Cache bean. A CacheManager wrapping all beans of that type is created. JCache (JSR-107) JCache is bootstrapped through the presence of a CachingProvider on the classpath (that is, a JSR-107 compliant caching library exists on the classpath), and the JCacheCacheManager is provided by the spring-boot-starter-cache starter. Various compliant libraries are available, and Spring Boot provides dependency management for Ehcache 3, Hazelcast, and Infinispan. Any other compliant library can be added as well. It might happen that more than one provider is present, in which case the provider must be explicitly specified. Even if the JSR-107 standard does not enforce a standardized way to define the location of the configuration file, Spring Boot does its best to accommodate setting a cache with implementation details, as shown in the following example: Properties YAML spring.cache.jcache.provider=com.example.MyCachingProvider spring.cache.jcache.config=classpath:example.xml # Only necessary if more than one provider is present spring: cache: jcache: provider: "com.example.MyCachingProvider" config: "classpath:example.xml" When a cache library offers both a native implementation and JSR-107 support, Spring Boot prefers the JSR-107 support, so that the same features are available if you switch to a different JSR-107 implementation. Spring Boot has general support for Hazelcast . If a single HazelcastInstance is available, it is automatically reused for the CacheManager as well, unless the spring.cache.jcache.config property is specified. There are two ways to customize the underlying CacheManager : Caches can be created on startup by setting the spring.cache.cache-names property. If a custom Configuration bean is defined, it is used to customize them. CacheManagerCustomizer beans are invoked with the reference of the CacheManager for full customization. If a standard CacheManager bean is defined, it is wrapped automatically in an CacheManager implementation that the abstraction expects. No further customization is applied to it. Hazelcast Spring Boot has general support for Hazelcast . If a HazelcastInstance has been auto-configured and com.hazelcast:hazelcast-spring is on the classpath, it is automatically wrapped in a CacheManager . Hazelcast can be used as a JCache compliant cache or as a Spring CacheManager compliant cache. When setting spring.cache.type to hazelcast , Spring Boot will use the CacheManager based implementation. If you want to use Hazelcast as a JCache compliant cache, set spring.cache.type to jcache . If you have multiple JCache compliant cache providers and want to force the use of Hazelcast, you have to explicitly set the JCache provider . Infinispan Infinispan has no default configuration file location, so it must be specified explicitly. Otherwise, the default bootstrap is used. Properties YAML spring.cache.infinispan.config=infinispan.xml spring: cache: infinispan: config: "infinispan.xml" Caches can be created on startup by setting the spring.cache.cache-names property. If a custom ConfigurationBuilder bean is defined, it is used to customize the caches. For more details, see the documentation . Couchbase If Spring Data Couchbase is available and Couchbase is configured , a CouchbaseCacheManager is auto-configured. It is possible to create additional caches on startup by setting the spring.cache.cache-names property and cache defaults can be configured by using spring.cache.couchbase.* properties. For instance, the following configuration creates cache1 and cache2 caches with an entry expiration of 10 minutes: Properties YAML spring.cache.cache-names=cache1,cache2 spring.cache.couchbase.expiration=10m spring: cache: cache-names: "cache1,cache2" couchbase: expiration: "10m" If you need more control over the configuration, consider registering a CouchbaseCacheManagerBuilderCustomizer bean. The following example shows a customizer that configures a specific entry expiration for cache1 and cache2 : Java Kotlin import java.time.Duration; import org.springframework.boot.cache.autoconfigure.CouchbaseCacheManagerBuilderCustomizer; import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration; import org.springframework.data.couchbase.cache.CouchbaseCacheConfiguration; @Configuration(proxyBeanMethods = false) public class MyCouchbaseCacheManagerConfiguration { @Bean public CouchbaseCacheManagerBuilderCustomizer myCouchbaseCacheManagerBuilderCustomizer() { return (builder) -> builder .withCacheConfiguration("cache1", CouchbaseCacheConfiguration .defaultCacheConfig().entryExpiry(Duration.ofSeconds(10))) .withCacheConfiguration("cache2", CouchbaseCacheConfiguration .defaultCacheConfig().entryExpiry(Duration.ofMinutes(1))); } } import org.springframework.boot.cache.autoconfigure.CouchbaseCacheManagerBuilderCustomizer import org.springframework.context.annotation.Bean import org.springframework.context.annotation.Configuration import org.springframework.data.couchbase.cache.CouchbaseCacheConfiguration import java.time.Duration @Configuration(proxyBeanMethods = false) class MyCouchbaseCacheManagerConfiguration { @Bean fun myCouchbaseCacheManagerBuilderCustomizer(): CouchbaseCacheManagerBuilderCustomizer { return CouchbaseCacheManagerBuilderCustomizer { builder -> builder .withCacheConfiguration( "cache1", CouchbaseCacheConfiguration .defaultCacheConfig().entryExpiry(Duration.ofSeconds(10)) ) .withCacheConfiguration( "cache2", CouchbaseCacheConfiguration .defaultCacheConfig().entryExpiry(Duration.ofMinutes(1)) ) } } } Redis If Redis is available and configured, a RedisCacheManager is auto-configured. It is possible to create additional caches on startup by setting the spring.cache.cache-names property and cache defaults can be configured by using spring.cache.redis.* properties. For instance, the following configuration creates cache1 and cache2 caches with a time to live of 10 minutes: Properties YAML spring.cache.cache-names=cache1,cache2 spring.cache.redis.time-to-live=10m spring: cache: cache-names: "cache1,cache2" redis: time-to-live: "10m" By default, a key prefix is added so that, if two separate caches use the same key, Redis does not have overlapping keys and cannot return invalid values. We strongly recommend keeping this setting enabled if you create your own RedisCacheManager . You can take full control of the default configuration by adding a RedisCacheConfiguration @Bean of your own. This can be useful if you need to customize the default serialization strategy. If you need more control over the configuration, consider registering a RedisCacheManagerBuilderCustomizer bean. The following example shows a customizer that configures a specific time to live for cache1 and cache2 : Java Kotlin import java.time.Duration; import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer; import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration; import org.springframework.data.redis.cache.RedisCacheConfiguration; @Configuration(proxyBeanMethods = false) public class MyRedisCacheManagerConfiguration { @Bean public RedisCacheManagerBuilderCustomizer myRedisCacheManagerBuilderCustomizer() { return (builder) -> builder .withCacheConfiguration("cache1", RedisCacheConfiguration .defaultCacheConfig().entryTtl(Duration.ofSeconds(10))) .withCacheConfiguration("cache2", RedisCacheConfiguration .defaultCacheConfig().entryTtl(Duration.ofMinutes(1))); } } import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer import org.springframework.context.annotation.Bean import org.springframework.context.annotation.Configuration import org.springframework.data.redis.cache.RedisCacheConfiguration import java.time.Duration @Configuration(proxyBeanMethods = false) class MyRedisCacheManagerConfiguration { @Bean fun myRedisCacheManagerBuilderCustomizer(): RedisCacheManagerBuilderCustomizer { return RedisCacheManagerBuilderCustomizer { builder -> builder .withCacheConfiguration( "cache1", RedisCacheConfiguration .defaultCacheConfig().entryTtl(Duration.ofSeconds(10)) ) .withCacheConfiguration( "cache2", RedisCacheConfiguration .defaultCacheConfig().entryTtl(Duration.ofMinutes(1)) ) } } } Caffeine Caffeine is a Java 8 rewrite of Guava’s cache that supersedes support for Guava. If Caffeine is present, a CaffeineCacheManager (provided by the spring-boot-starter-cache starter) is auto-configured. Caches can be created on startup by setting the spring.cache.cache-names property and can be customized by one of the following (in the indicated order): A cache spec defined by spring.cache.caffeine.spec A CaffeineSpec bean is defined A Caffeine bean is defined For instance, the following configuration creates cache1 and cache2 caches with a maximum size of 500 and a time to live of 10 minutes Properties YAML spring.cache.cache-names=cache1,cache2 spring.cache.caffeine.spec=maximumSize=500,expireAfterAccess=600s spring: cache: cache-names: "cache1,cache2" caffeine: spec: "maximumSize=500,expireAfterAccess=600s" If a CacheLoader bean is defined, it is automatically associated to the CaffeineCacheManager . Since the CacheLoader is going to be associated with all caches managed by the cache manager, it must be defined as CacheLoader<Object, Object> . The auto-configuration ignores any other generic type. Cache2k Cache2k is an in-memory cache. If the Cache2k spring integration is present, a SpringCache2kCacheManager is auto-configured. Caches can be created on startup by setting the spring.cache.cache-names property. Cache defaults can be customized using a Cache2kBuilderCustomizer bean. The following example shows a customizer that configures the capacity of the cache to 200 entries, with an expiration of 5 minutes: Java Kotlin import java.util.concurrent.TimeUnit; import org.springframework.boot.cache.autoconfigure.Cache2kBuilderCustomizer; import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration; @Configuration(proxyBeanMethods = false) public class MyCache2kDefaultsConfiguration { @Bean public Cache2kBuilderCustomizer myCache2kDefaultsCustomizer() { return (builder) -> builder.entryCapacity(200) .expireAfterWrite(5, TimeUnit.MINUTES); } } import org.springframework.boot.cache.autoconfigure.Cache2kBuilderCustomizer import org.springframework.context.annotation.Bean import org.springframework.context.annotation.Configuration import java.util.concurrent.TimeUnit @Configuration(proxyBeanMethods = false) class MyCache2kDefaultsConfiguration { @Bean fun myCache2kDefaultsCustomizer(): Cache2kBuilderCustomizer { return Cache2kBuilderCustomizer { builder -> builder.entryCapacity(200) .expireAfterWrite(5, TimeUnit.MINUTES) } } } Simple If none of the other providers can be found, a simple implementation using a ConcurrentHashMap as the cache store is configured. This is the default if no caching library is present in your application. By default, caches are created as needed, but you can restrict the list of available caches by setting the cache-names property. For instance, if you want only cache1 and cache2 caches, set the cache-names property as follows: Properties YAML spring.cache.cache-names=cache1,cache2 spring: cache: cache-names: "cache1,cache2" If you do so and your application uses a cache not listed, then it fails at runtime when the cache is needed, but not on startup. This is similar to the way the "real" cache providers behave if you use an undeclared cache. None If you need to use a no-op cache rather than the auto-configured cache manager in a certain environment, set the cache type to none , as shown in the following example: Properties YAML spring.cache.type=none spring: cache: type: "none" Testing It is generally useful to use a no-op implementation when running a test suite. This section lists a number of strategies that are useful for tests. When a custom CacheManager is defined, the best option is to make sure that caching configuration is defined in an isolated @Configuration class. Doing so makes sure that caching is not required by slice tests. For tests that enable a full context, such as @SpringBootTest , an explicit configuration overriding the regular configuration is required. If caching is auto-configured, more options are available. Tests can be annotated with @AutoConfigureCache to replace the auto-configured CacheManager by a no-op implementation. Java Kotlin import org.springframework.boot.cache.test.autoconfigure.AutoConfigureCache; import org.springframework.boot.test.context.SpringBootTest; @SpringBootTest @AutoConfigureCache public class MyIntegrationTests { // Tests use a no-op cache manager } import org.springframework.boot.cache.test.autoconfigure.AutoConfigureCache import org.springframework.boot.test.context.SpringBootTest @SpringBootTest @AutoConfigureCache class MyIntegrationTests { // Tests use a no-op cache manager } Another option is to force a no-op implementation for the auto-configured CacheManager : Properties YAML spring.cache.type=none spring: cache: type: "none" IO Spring Batch Spring Boot Stable 4.1.0 4.0.7 3.5.16 3.4.13 3.3.13 Snapshot 4.2.0-SNAPSHOT 4.1.1-SNAPSHOT 4.0.8-SNAPSHOT Related Spring Documentation Spring Boot Spring Framework Spring Cloud Spring Cloud Build Spring Cloud Bus Spring Cloud Circuit Breaker Spring Cloud Commons Spring Cloud Config Spring Cloud Consul Spring Cloud Contract Spring Cloud Function Spring Cloud Gateway Spring Cloud Kubernetes Spring Cloud Netflix Spring Cloud OpenFeign Spring Cloud Stream Spring Cloud Task Spring Cloud Vault Spring Cloud Zookeeper Spring Data Spring Data Cassandra Spring Data Commons Spring Data Couchbase Spring Data Elasticsearch Spring Data JPA Spring Data KeyValue Spring Data LDAP Spring Data MongoDB Spring Data Neo4j Spring Data Redis Spring Data JDBC & R2DBC Spring Data REST Spring Integration Spring Batch Spring Security Spring Authorization Server Spring LDAP Spring Security Kerberos Spring Session Spring Vault Spring AI Spring AMQP Spring CLI Spring GraphQL Spring for Apache Kafka Spring Modulith Spring for Apache Pulsar Spring Shell All Docs... Copyright © 2005 - Broadcom. All Rights Reserved. The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries. Terms of Use • Privacy • Trademark Guidelines • Thank you • Your California Privacy Rights • Cookie Settings Apache®, Apache Tomcat®, Apache Kafka®, Apache Cassandra™, and Apache Geode™ are trademarks or registered trademarks of the Apache Software Foundation in the United States and/or other countries. Java™, Java™ SE, Java™ EE, and OpenJDK™ are trademarks of Oracle and/or its affiliates. Kubernetes® is a registered trademark of the Linux Foundation in the United States and other countries. Linux® is the registered trademark of Linus Torvalds in the United States and other countries. Windows® and Microsoft® Azure are registered trademarks of Microsoft Corporation. “AWS” and “Amazon Web Services” are trademarks or registered trademarks of Amazon.com Inc. or its affiliates. All other trademarks and copyrights are property of their respective owners and are only mentioned for informative purposes. Other names may be trademarks of their respective owners. Search in all Spring Docs
