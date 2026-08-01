# spring-boot-profiles — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://docs.spring.io/spring-boot/reference/features/profiles.html (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T02:24:27Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://docs.spring.io/spring-boot/reference/features/profiles.html`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r136`
**Body SHA-256 (below the `---` divider, header excluded):** b47bf38e7499cc7a16d9b857e045219153b4830f82aa14a1eb6e0c2c729bef59

---

---
snapshot_id: spring-boot-profiles
source: "https://docs.spring.io/spring-boot/reference/features/profiles.html"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 3
bytes: 7495
sha: "fcd280da7c3299b2c561153969c4fe3285da2bb63d51dde9e0597b3e228bd64f"
---

# spring boot profiles — upstream snapshot

Source: https://docs.spring.io/spring-boot/reference/features/profiles.html
Fetched: 2026-07-14

Profiles :: Spring Boot
Edit this Page
 
 
 
 GitHub Project
 
 
 
 Stack Overflow

# Profiles
Spring Profiles provide a way to segregate parts of your application configuration and make it be available only in certain environments.
Any @Component, @Configuration or @ConfigurationProperties can be marked with @Profile to limit when it is loaded, as shown in the following example:
Java
Kotlin
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("production")
public class ProductionConfiguration {

 // ...

}
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration(proxyBeanMethods = false)
@Profile("production")
class ProductionConfiguration {

 // ...

}
If @ConfigurationProperties beans are registered through @EnableConfigurationProperties instead of automatic scanning, the @Profile annotation needs to be specified on the @Configuration class that has the @EnableConfigurationProperties annotation.
In the case where @ConfigurationProperties are scanned, @Profile can be specified on the @ConfigurationProperties class itself.
You can use a spring.profiles.active Environment property to specify which profiles are active.
You can specify the property in any of the ways described earlier in this chapter.
For example, you could include it in your application.properties, as shown in the following example:
Properties
YAML
spring.profiles.active=dev,hsqldb
spring:
 profiles:
 active: "dev,hsqldb"
You could also specify it on the command line by using the following switch: --spring.profiles.active=dev,hsqldb.
If no profile is active, a default profile is enabled.
The name of the default profile is default and it can be tuned using the spring.profiles.default Environment property, as shown in the following example:
Properties
YAML
spring.profiles.default=none
spring:
 profiles:
 default: "none"
spring.profiles.active and spring.profiles.default can only be used in non-profile-specific documents.
This means they cannot be included in profile specific files or documents activated by spring.config.activate.on-profile.
For example, the second document configuration is invalid:
Properties
YAML
spring.profiles.active=prod
#---
spring.config.activate.on-profile=prod
spring.profiles.active=metrics
# this document is valid
spring:
 profiles:
 active: "prod"
---
# this document is invalid
spring:
 config:
 activate:
 on-profile: "prod"
 profiles:
 active: "metrics"
The spring.profiles.active property follows the same ordering rules as other properties.
The highest PropertySource wins.
This means that you can specify active profiles in application.properties and then replace them by using the command line switch.
See the “Externalized Configuration” for more details on the order in which property sources are considered.
By default, profile names in Spring Boot may contain letters, numbers, or permitted characters (-, _, ., +, @).
In addition, they can only start and end with a letter or number.
This restriction helps to prevent common parsing issues.
if, however, you prefer more flexible profile names you can set spring.profiles.validate to false in your application.properties or application.yaml file:
Properties
YAML
spring.profiles.validate=false
spring:
 profiles:
 validate: false

## Adding Active Profiles
Sometimes, it is useful to have properties that add to the active profiles rather than replace them.
The spring.profiles.include property can be used to add active profiles on top of those activated by the spring.profiles.active property.
The SpringApplication entry point also has a Java API for setting additional profiles.
See the setAdditionalProfiles() method in SpringApplication.
For example, when an application with the following properties is run, the common and local profiles will be activated even when it runs using the --spring.profiles.active switch:
Properties
YAML
spring.profiles.include[0]=common
spring.profiles.include[1]=local
spring:
 profiles:
 include:
 - "common"
 - "local"
Included profiles are added before any spring.profiles.active profiles.
The spring.profiles.include property is processed for each property source, as such the usual complex type merging rules for lists do not apply.
Similar to spring.profiles.active, spring.profiles.include can only be used in non-profile-specific documents.
This means it cannot be included in profile specific files or documents activated by spring.config.activate.on-profile.
Profile groups, which are described in the next section can also be used to add active profiles if a given profile is active.

## Profile Groups
Occasionally the profiles that you define and use in your application are too fine-grained and become cumbersome to use.
For example, you might have proddb and prodmq profiles that you use to enable database and messaging features independently.
To help with this, Spring Boot lets you define profile groups.
A profile group allows you to define a logical name for a related group of profiles.
For example, we can create a production group that consists of our proddb and prodmq profiles.
Properties
YAML
spring.profiles.group.production[0]=proddb
spring.profiles.group.production[1]=prodmq
spring:
 profiles:
 group:
 production:
 - "proddb"
 - "prodmq"
Our application can now be started using --spring.profiles.active=production to activate the production, proddb and prodmq profiles in one hit.
Similar to spring.profiles.active and spring.profiles.include, spring.profiles.group can only be used in non-profile-specific documents.
This means it cannot be included in profile specific files or documents activated by spring.config.activate.on-profile.

## Programmatically Setting Profiles
You can programmatically set active profiles by calling SpringApplication.setAdditionalProfiles(…​) before your application runs.
It is also possible to activate profiles by using Spring’s ConfigurableEnvironment interface.

## Profile-specific Configuration Files
Profile-specific variants of both application.properties (or application.yaml) and files referenced through @ConfigurationProperties are considered as files and loaded.
See Profile Specific Files for details.
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

Source: https://docs.spring.io/spring-boot/reference/features/profiles.html
HTTP status: 200 · extracted bytes: 15799 · sha256: 3e9cf0f50fa094aef0d4d2b3c761233d202a71d90714f81d30bbb8bf4374ba23
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r136`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

Profiles :: Spring Boot Why Spring Overview Trending Generative AI Cloud Architecture Patterns Microservices Reactive Event Driven Application Types Web Applications Serverless Batch Learn Getting Started Quickstart Guides Academy Courses Get Certified Projects Overview Projects Spring Boot Spring Framework Spring Cloud Spring AI Spring Data Spring Integration Spring Batch Spring Security Foundational Projects Micrometer Reactor Development Tools Spring Tools Spring Initializr Resources Blog Release Calendar Version Mappings Release Highlights Security Advisories GitHub Orgs Spring Projects Spring Cloud Community Overview Events Authors Enterprise Overview Long-term Support Automated Upgrades Governance and Compliance Modern App Development light Spring Boot 4.1.0 Search Overview Documentation Community System Requirements Installing Spring Boot Upgrading Spring Boot Tutorials Developing Your First Spring Boot Application Reference Developing with Spring Boot Build Systems Structuring Your Code Configuration Classes Auto-configuration Spring Beans and Dependency Injection Using the @SpringBootApplication Annotation Running Your Application Developer Tools Packaging Your Application for Production Core Features SpringApplication Externalized Configuration Profiles Logging Internationalization Aspect-Oriented Programming JSON Task Execution and Scheduling Development-time Services Creating Your Own Auto-configuration Kotlin Support SSL Web Servlet Web Applications Reactive Web Applications Graceful Shutdown Spring Security Spring Session Spring for GraphQL Spring HATEOAS Data SQL Databases Working with NoSQL Technologies IO Caching Spring Batch gRPC Hazelcast Quartz Scheduler Sending Email Validation Calling REST Services Web Services Distributed Transactions With JTA Messaging JMS AMQP Apache Kafka Support Apache Pulsar Support RSocket Spring Integration WebSockets Security OAuth2 SAML 2.0 Testing Test Modules Test Scope Dependencies Testing Spring Applications Testing Spring Boot Applications Testcontainers Test Utilities Packaging Spring Boot Applications Efficient Deployments AOT Cache Ahead-of-Time Processing With the JVM GraalVM Native Images Introducing GraalVM Native Images Advanced Native Images Topics Checkpoint and Restore With the JVM Container Images Efficient Container Images Dockerfiles Cloud Native Buildpacks Production-ready Features Enabling Production-ready Features Endpoints Monitoring and Management Over HTTP Monitoring and Management over JMX Observability Loggers Metrics Tracing Auditing Recording HTTP Exchanges Process Monitoring Cloud Foundry Support How-to Guides Spring Boot Application Properties and Configuration Embedded Web Servers Spring MVC Jersey HTTP Clients Logging Data Access Database Initialization NoSQL Messaging Batch Applications Actuator Security Hot Swapping Testing Build Ahead-of-Time Processing GraalVM Native Applications Developing Your First GraalVM Native Application Testing GraalVM Native Images AOT Cache Deploying Spring Boot Applications Traditional Deployment Deploying to the Cloud Installing Spring Boot Applications Docker Compose Build Tool Plugins Maven Plugin Getting Started Using the Plugin Goals Packaging Executable Archives Packaging OCI Images Running your Application with Maven Ahead-of-Time Processing Running Integration Tests Integrating with Actuator Help Information Gradle Plugin Getting Started Managing Dependencies Packaging Executable Archives Packaging OCI Images Publishing your Application Running your Application with Gradle Ahead-of-Time Processing Integrating with Actuator Reacting to Other Plugins Spring Boot AntLib Module Supporting Other Build Systems Spring Boot CLI Installing the CLI Using the CLI Rest APIs Actuator Audit Events ( auditevents ) Beans ( beans ) Caches ( caches ) Conditions Evaluation Report ( conditions ) Configuration Properties ( configprops ) Environment ( env ) Flyway ( flyway ) Health ( health ) Heap Dump ( heapdump ) HTTP Exchanges ( httpexchanges ) Info ( info ) Spring Integration Graph ( integrationgraph ) Liquibase ( liquibase ) Log File ( logfile ) Loggers ( loggers ) Mappings ( mappings ) Metrics ( metrics ) Prometheus ( prometheus ) Quartz ( quartz ) Software Bill of Materials ( sbom ) Scheduled Tasks ( scheduledtasks ) Sessions ( sessions ) Shutdown ( shutdown ) Application Startup ( startup ) Thread Dump ( threaddump ) Java APIs Spring Boot Gradle Plugin Maven Plugin Kotlin APIs Spring Boot Specifications Configuration Metadata Metadata Format Providing Manual Hints Generating Your Own Metadata by Using the Annotation Processor The Executable Jar Format Nested JARs Spring Boot’s “NestedJarFile” Class Launching Executable Jars PropertiesLauncher Features Executable Jar Restrictions Alternative Single Jar Solutions Appendix Common Application Properties Deprecated Application Properties Auto-configuration Classes spring-boot-activemq spring-boot-actuator-autoconfigure spring-boot-amqp spring-boot-artemis spring-boot-autoconfigure spring-boot-batch spring-boot-batch-data-mongodb spring-boot-batch-jdbc spring-boot-cache spring-boot-cassandra spring-boot-cloudfoundry spring-boot-couchbase spring-boot-data-cassandra spring-boot-data-commons spring-boot-data-couchbase spring-boot-data-elasticsearch spring-boot-data-jdbc spring-boot-data-jpa spring-boot-data-ldap spring-boot-data-mongodb spring-boot-data-neo4j spring-boot-data-r2dbc spring-boot-data-redis spring-boot-data-rest spring-boot-devtools spring-boot-elasticsearch spring-boot-flyway spring-boot-freemarker spring-boot-graphql spring-boot-groovy-templates spring-boot-grpc-client spring-boot-grpc-server spring-boot-gson spring-boot-h2console spring-boot-hateoas spring-boot-hazelcast spring-boot-health spring-boot-hibernate spring-boot-http-client spring-boot-http-codec spring-boot-http-converter spring-boot-integration spring-boot-jackson spring-boot-jackson2 spring-boot-jdbc spring-boot-jersey spring-boot-jetty spring-boot-jms spring-boot-jooq spring-boot-jsonb spring-boot-kafka spring-boot-kotlinx-serialization-json spring-boot-ldap spring-boot-liquibase spring-boot-mail spring-boot-micrometer-metrics spring-boot-micrometer-observation spring-boot-micrometer-tracing spring-boot-micrometer-tracing-brave spring-boot-micrometer-tracing-opentelemetry spring-boot-mongodb spring-boot-mustache spring-boot-neo4j spring-boot-netty spring-boot-opentelemetry spring-boot-persistence spring-boot-pulsar spring-boot-quartz spring-boot-r2dbc spring-boot-reactor spring-boot-reactor-netty spring-boot-restclient spring-boot-resttestclient spring-boot-rsocket spring-boot-security spring-boot-security-oauth2-authorization-server spring-boot-security-oauth2-client spring-boot-security-oauth2-resource-server spring-boot-security-saml2 spring-boot-sendgrid spring-boot-servlet spring-boot-session spring-boot-session-data-redis spring-boot-session-jdbc spring-boot-testcontainers spring-boot-thymeleaf spring-boot-tomcat spring-boot-transaction spring-boot-validation spring-boot-webclient spring-boot-webflux spring-boot-webmvc spring-boot-webservices spring-boot-websocket spring-boot-zipkin Test Auto-configuration Annotations Test Slices Dependency Versions Managed Dependency Coordinates Version Properties Search Edit this Page GitHub Project Stack Overflow Spring Boot Reference Core Features Profiles Profiles Spring Profiles provide a way to segregate parts of your application configuration and make it be available only in certain environments. Any @Component , @Configuration or @ConfigurationProperties can be marked with @Profile to limit when it is loaded, as shown in the following example: Java Kotlin import org.springframework.context.annotation.Configuration; import org.springframework.context.annotation.Profile; @Configuration(proxyBeanMethods = false) @Profile("production") public class ProductionConfiguration { // ... } import org.springframework.context.annotation.Configuration import org.springframework.context.annotation.Profile @Configuration(proxyBeanMethods = false) @Profile("production") class ProductionConfiguration { // ... } If @ConfigurationProperties beans are registered through @EnableConfigurationProperties instead of automatic scanning, the @Profile annotation needs to be specified on the @Configuration class that has the @EnableConfigurationProperties annotation. In the case where @ConfigurationProperties are scanned, @Profile can be specified on the @ConfigurationProperties class itself. You can use a spring.profiles.active Environment property to specify which profiles are active. You can specify the property in any of the ways described earlier in this chapter. For example, you could include it in your application.properties , as shown in the following example: Properties YAML spring.profiles.active=dev,hsqldb spring: profiles: active: "dev,hsqldb" You could also specify it on the command line by using the following switch: --spring.profiles.active=dev,hsqldb . If no profile is active, a default profile is enabled. The name of the default profile is default and it can be tuned using the spring.profiles.default Environment property, as shown in the following example: Properties YAML spring.profiles.default=none spring: profiles: default: "none" spring.profiles.active and spring.profiles.default can only be used in non-profile-specific documents. This means they cannot be included in profile specific files or documents activated by spring.config.activate.on-profile . For example, the second document configuration is invalid: Properties YAML spring.profiles.active=prod #--- spring.config.activate.on-profile=prod spring.profiles.active=metrics # this document is valid spring: profiles: active: "prod" --- # this document is invalid spring: config: activate: on-profile: "prod" profiles: active: "metrics" The spring.profiles.active property follows the same ordering rules as other properties. The highest PropertySource wins. This means that you can specify active profiles in application.properties and then replace them by using the command line switch. See the “Externalized Configuration” for more details on the order in which property sources are considered. By default, profile names in Spring Boot may contain letters, numbers, or permitted characters ( - , _ , . , + , @ ). In addition, they can only start and end with a letter or number. This restriction helps to prevent common parsing issues. if, however, you prefer more flexible profile names you can set spring.profiles.validate to false in your application.properties or application.yaml file: Properties YAML spring.profiles.validate=false spring: profiles: validate: false Adding Active Profiles Sometimes, it is useful to have properties that add to the active profiles rather than replace them. The spring.profiles.include property can be used to add active profiles on top of those activated by the spring.profiles.active property. The SpringApplication entry point also has a Java API for setting additional profiles. See the setAdditionalProfiles() method in SpringApplication . For example, when an application with the following properties is run, the common and local profiles will be activated even when it runs using the --spring.profiles.active switch: Properties YAML spring.profiles.include[0]=common spring.profiles.include[1]=local spring: profiles: include: - "common" - "local" Included profiles are added before any spring.profiles.active profiles. The spring.profiles.include property is processed for each property source, as such the usual complex type merging rules for lists do not apply. Similar to spring.profiles.active , spring.profiles.include can only be used in non-profile-specific documents. This means it cannot be included in profile specific files or documents activated by spring.config.activate.on-profile . Profile groups, which are described in the next section can also be used to add active profiles if a given profile is active. Profile Groups Occasionally the profiles that you define and use in your application are too fine-grained and become cumbersome to use. For example, you might have proddb and prodmq profiles that you use to enable database and messaging features independently. To help with this, Spring Boot lets you define profile groups. A profile group allows you to define a logical name for a related group of profiles. For example, we can create a production group that consists of our proddb and prodmq profiles. Properties YAML spring.profiles.group.production[0]=proddb spring.profiles.group.production[1]=prodmq spring: profiles: group: production: - "proddb" - "prodmq" Our application can now be started using --spring.profiles.active=production to activate the production , proddb and prodmq profiles in one hit. Similar to spring.profiles.active and spring.profiles.include , spring.profiles.group can only be used in non-profile-specific documents. This means it cannot be included in profile specific files or documents activated by spring.config.activate.on-profile . Programmatically Setting Profiles You can programmatically set active profiles by calling SpringApplication.setAdditionalProfiles(…​) before your application runs. It is also possible to activate profiles by using Spring’s ConfigurableEnvironment interface. Profile-specific Configuration Files Profile-specific variants of both application.properties (or application.yaml ) and files referenced through @ConfigurationProperties are considered as files and loaded. See Profile Specific Files for details. Externalized Configuration Logging Spring Boot Stable 4.1.0 4.0.7 3.5.16 3.4.13 3.3.13 Snapshot 4.2.0-SNAPSHOT 4.1.1-SNAPSHOT 4.0.8-SNAPSHOT Related Spring Documentation Spring Boot Spring Framework Spring Cloud Spring Cloud Build Spring Cloud Bus Spring Cloud Circuit Breaker Spring Cloud Commons Spring Cloud Config Spring Cloud Consul Spring Cloud Contract Spring Cloud Function Spring Cloud Gateway Spring Cloud Kubernetes Spring Cloud Netflix Spring Cloud OpenFeign Spring Cloud Stream Spring Cloud Task Spring Cloud Vault Spring Cloud Zookeeper Spring Data Spring Data Cassandra Spring Data Commons Spring Data Couchbase Spring Data Elasticsearch Spring Data JPA Spring Data KeyValue Spring Data LDAP Spring Data MongoDB Spring Data Neo4j Spring Data Redis Spring Data JDBC & R2DBC Spring Data REST Spring Integration Spring Batch Spring Security Spring Authorization Server Spring LDAP Spring Security Kerberos Spring Session Spring Vault Spring AI Spring AMQP Spring CLI Spring GraphQL Spring for Apache Kafka Spring Modulith Spring for Apache Pulsar Spring Shell All Docs... Copyright © 2005 - Broadcom. All Rights Reserved. The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries. Terms of Use • Privacy • Trademark Guidelines • Thank you • Your California Privacy Rights • Cookie Settings Apache®, Apache Tomcat®, Apache Kafka®, Apache Cassandra™, and Apache Geode™ are trademarks or registered trademarks of the Apache Software Foundation in the United States and/or other countries. Java™, Java™ SE, Java™ EE, and OpenJDK™ are trademarks of Oracle and/or its affiliates. Kubernetes® is a registered trademark of the Linux Foundation in the United States and other countries. Linux® is the registered trademark of Linus Torvalds in the United States and other countries. Windows® and Microsoft® Azure are registered trademarks of Microsoft Corporation. “AWS” and “Amazon Web Services” are trademarks or registered trademarks of Amazon.com Inc. or its affiliates. All other trademarks and copyrights are property of their respective owners and are only mentioned for informative purposes. Other names may be trademarks of their respective owners. Search in all Spring Docs
