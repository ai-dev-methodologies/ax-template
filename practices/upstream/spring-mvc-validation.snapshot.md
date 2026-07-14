---
snapshot_id: spring-mvc-validation
source: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 3
bytes: 5870
sha: "142360342ff5c38e5a2d284c13e346a1d3af6950ff27a856a18f130f7d04792c"
---

# spring mvc validation — upstream snapshot

Source: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html
Fetched: 2026-07-14

Validation :: Spring Framework
Edit this Page
 
 
 
 GitHub Project
 
 
 
 Stack Overflow

# Validation
See equivalent in the Reactive stack
Spring MVC has built-in validation for
@RequestMapping methods, including Java Bean Validation.
Validation may be applied at one of two levels:
Java Bean Validation is applied individually to an
@ModelAttribute,
@RequestBody, and
@RequestPart method parameter
annotated with @jakarta.validation.Valid or Spring’s @Validated so long as
it is a command object rather than a container such as Map or Collection, it does not
have Errors or BindingResult immediately after in the method signature, and does not
otherwise require method validation (see next). MethodArgumentNotValidException is the
exception raised when validating a method parameter individually.
Java Bean Validation is applied to the method when @Constraint annotations such as
@Min, @NotBlank and others are declared directly on method parameters, or on the
method for the return value, and it supersedes any validation that would be applied
otherwise to a method parameter individually because method validation covers both
method parameter constraints and nested constraints via @Valid.
HandlerMethodValidationException is the exception raised validation is applied
to the method.
Applications should handle both MethodArgumentNotValidException and
HandlerMethodValidationException since either may be raised depending on the controller
method signature. The two exceptions, however are designed to be very similar, and can be
handled with almost identical code. The main difference is that the former is for a single
object while the latter is for a list of method parameters.
@Valid is not a constraint annotation, but rather for nested constraints within
an Object. Therefore, by itself @Valid does not lead to method validation. @NotNull
on the other hand is a constraint, and adding it to an @Valid parameter leads to method
validation. For nullability specifically, you may also use the required flag of
@RequestBody or @ModelAttribute.
Method validation may be used in combination with Errors or BindingResult method
parameters. However, the controller method is called only if all validation errors are on
method parameters with an Errors immediately after. If there are validation errors on
any other method parameter then HandlerMethodValidationException is raised.
You can configure a Validator globally through the
WebMvc config, or locally through an
@InitBinder method in an
@Controller or @ControllerAdvice. You can also use multiple validators.
If a controller has a class level @Validated, then
method validation is applied
through an AOP proxy. In order to take advantage of the Spring MVC built-in support for
method validation added in Spring Framework 6.1, you need to remove the class level
@Validated annotation from the controller.
The Error Responses section provides further
details on how MethodArgumentNotValidException and HandlerMethodValidationException
are handled, and also how their rendering can be customized through a MessageSource and
locale and language specific resource bundles.
For further custom handling of method validation errors, you can extend
ResponseEntityExceptionHandler or use an @ExceptionHandler method in a controller
or in a @ControllerAdvice, and handle HandlerMethodValidationException directly.
The exception contains a list of ParameterValidationResults that group validation errors
by method parameter. You can either iterate over those, or provide a visitor with callback
methods by controller method parameter type:
Java
Kotlin
HandlerMethodValidationException ex = ... ;

ex.visitResults(new HandlerMethodValidationException.Visitor() {

 @Override
 public void requestHeader(RequestHeader requestHeader, ParameterValidationResult result) {
 // ...
 }

 @Override
 public void requestParam(@Nullable RequestParam requestParam, ParameterValidationResult result) {
 // ...
 }

 @Override
 public void modelAttribute(@Nullable ModelAttribute modelAttribute, ParameterErrors errors) {

 // ...

 @Override
 public void other(ParameterValidationResult result) {
 // ...
 }
});
// HandlerMethodValidationException
val ex

ex.visitResults(object : HandlerMethodValidationException.Visitor {

 override fun requestHeader(requestHeader: RequestHeader, result: ParameterValidationResult) {
 // ...
 }

 override fun requestParam(requestParam: RequestParam?, result: ParameterValidationResult) {
 // ...
 }

 override fun modelAttribute(modelAttribute: ModelAttribute?, errors: ParameterErrors) {
 // ...
 }

 // ...

 override fun other(result: ParameterValidationResult) {
 // ...
 }
})
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
