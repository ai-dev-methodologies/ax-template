---
snapshot_id: spring-mvc-controlleradvice
source: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 3
bytes: 3756
sha: "743ce140e7b93c58c8184ec9d1acc23add3d005dc52fb2e7f0e486a7391f4608"
---

# spring mvc controlleradvice — upstream snapshot

Source: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html
Fetched: 2026-07-14

Controller Advice :: Spring Framework
Edit this Page
 
 
 
 GitHub Project
 
 
 
 Stack Overflow

# Controller Advice
See equivalent in the Reactive stack
@ExceptionHandler, @InitBinder, and @ModelAttribute methods apply only to the
@Controller class, or class hierarchy, in which they are declared. If, instead, they
are declared in an @ControllerAdvice or @RestControllerAdvice class, then they apply
to any controller. Moreover, as of 5.3, @ExceptionHandler methods in @ControllerAdvice
can be used to handle exceptions from any @Controller or any other handler.
@ControllerAdvice is meta-annotated with @Component and therefore can be registered as
a Spring bean through component scanning.
@RestControllerAdvice is a shortcut annotation that combines @ControllerAdvice
with @ResponseBody, in effect simply an @ControllerAdvice whose exception handler
methods render to the response body.
On startup, RequestMappingHandlerMapping and ExceptionHandlerExceptionResolver detect
controller advice beans and apply them at runtime. Global @ExceptionHandler methods,
from an @ControllerAdvice, are applied after local ones, from the @Controller.
By contrast, global @ModelAttribute and @InitBinder methods are applied before local ones.
By default, both @ControllerAdvice and @RestControllerAdvice apply to any controller,
including @Controller and @RestController. Use attributes of the annotation to narrow
the set of controllers and handlers that they apply to. For example:
Java
Kotlin
// Target all Controllers annotated with @RestController
@ControllerAdvice(annotations = RestController.class)
public class ExampleAdvice1 {}

// Target all Controllers within specific packages
@ControllerAdvice("org.example.controllers")
public class ExampleAdvice2 {}

// Target all Controllers assignable to specific classes
@ControllerAdvice(assignableTypes = {ControllerInterface.class, AbstractController.class})
public class ExampleAdvice3 {}
// Target all Controllers annotated with @RestController
@ControllerAdvice(annotations = [RestController::class])
class ExampleAdvice1

// Target all Controllers within specific packages
@ControllerAdvice("org.example.controllers")
class ExampleAdvice2

// Target all Controllers assignable to specific classes
@ControllerAdvice(assignableTypes = [ControllerInterface::class, AbstractController::class])
class ExampleAdvice3
The selectors in the preceding example are evaluated at runtime and may negatively impact
performance if used extensively. See the
@ControllerAdvice
javadoc for more details.
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
