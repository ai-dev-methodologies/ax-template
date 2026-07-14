---
snapshot_id: spring-mvc-modelattribute
source: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/modelattrib-method-args.html"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 3
bytes: 8066
sha: "30f1b9630b0762f326487cbdcb8384ffbc72029be4e46306e60633b77275eabd"
---

# spring mvc modelattribute — upstream snapshot

Source: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/modelattrib-method-args.html
Fetched: 2026-07-14

@ModelAttribute :: Spring Framework
Edit this Page
 
 
 
 GitHub Project
 
 
 
 Stack Overflow

# @ModelAttribute
See equivalent in the Reactive stack
The @ModelAttribute method parameter annotation binds request parameters, URI path variables,
and request headers onto a model object. For example:
Java
Kotlin
@PostMapping("/owners/{ownerId}/pets/{petId}/edit")
public String processSubmit(@ModelAttribute Pet pet) { (1)
 // method logic...
}
1
Bind to an instance of Pet.
@PostMapping("/owners/{ownerId}/pets/{petId}/edit")
fun processSubmit(@ModelAttribute pet: Pet): String { (1)
 // method logic...
}
1
Bind to an instance of Pet.
Request parameters are a Servlet API concept that includes form data from the request body,
and query parameters. URI variables and headers are also included, but only if they don’t
override request parameters with the same name. Dashes are stripped from header names.
The Pet instance above may be:
Accessed from the model where it could have been added by a
@ModelAttribute method.
Accessed from the HTTP session if the model attribute was listed in
the class-level @SessionAttributes annotation.
Obtained through a Converter if the model attribute name matches the name of a
request value such as a path variable or a request parameter (example follows).
Instantiated through a default constructor.
Instantiated through a “primary constructor” with arguments that match to Servlet
request parameters. Argument names are determined through runtime-retained parameter
names in the bytecode.
As mentioned above, a Converter may be used to obtain the model object if
the model attribute name matches to the name of a request value such as a path variable or a
request parameter, and there is a compatible Converter. In the below example,
the model attribute name account matches URI path variable account, and there is a
registered Converter that perhaps retrieves it from a persistence store:
Java
Kotlin
@PutMapping("/accounts/{account}")
public String save(@ModelAttribute("account") Account account) { (1)
 // ...
}
@PutMapping("/accounts/{account}")
fun save(@ModelAttribute("account") account: Account): String { (1)
 // ...
}
By default, both constructor and property
data binding are applied. However,
model object design requires careful consideration, and for security reasons it is
recommended either to use an object tailored specifically for web binding, or to apply
constructor binding only. If property binding must still be used, then allowedFields
patterns should be set to limit which properties can be set. For further details on this
and example configuration, see
model design.
When using constructor binding, you can customize request parameter names through an
@BindParam annotation. For example:
Java
Kotlin
class Account {

 private final String firstName;

 public Account(@BindParam("first-name") String firstName) {
 this.firstName = firstName;
 }
}
class Account(@BindParam("first-name") val firstName: String)
The @BindParam may also be placed on the fields that correspond to constructor
parameters. While @BindParam is supported out of the box, you can also use a
different annotation by setting a DataBinder.NameResolver on DataBinder
Constructor binding supports List, Map, and array arguments either converted from
a single string, for example, comma-separated list, or based on indexed keys such as
accounts[2].name or account[KEY].name.
In some cases, you may want access to a model attribute without data binding. For such
cases, you can inject the Model into the controller and access it directly or,
alternatively, set @ModelAttribute(binding=false), as the following example shows:
Java
Kotlin
@ModelAttribute
public AccountForm setUpForm() {
 return new AccountForm();
}

@ModelAttribute
public Account findAccount(@PathVariable String accountId) {
 return accountRepository.findOne(accountId);
}

@PostMapping("update")
public String update(AccountForm form, BindingResult result,
 @ModelAttribute(binding=false) Account account) { (1)
 // ...
}
1
Setting @ModelAttribute(binding=false).
@ModelAttribute
fun setUpForm(): AccountForm {
 return AccountForm()
}

@ModelAttribute
fun findAccount(@PathVariable accountId: String): Account {
 return accountRepository.findOne(accountId)
}

@PostMapping("update")
fun update(form: AccountForm, result: BindingResult,
 @ModelAttribute(binding = false) account: Account): String { (1)
 // ...
}
1
Setting @ModelAt\tribute(binding=false).
If data binding results in errors, by default a MethodArgumentNotValidException is raised,
but you can also add a BindingResult argument immediately next to the @ModelAttribute
in order to handle such errors in the controller method. For example:
Java
Kotlin
@PostMapping("/owners/{ownerId}/pets/{petId}/edit")
public String processSubmit(@ModelAttribute("pet") Pet pet, BindingResult result) { (1)
 if (result.hasErrors()) {
 return "petForm";
 }
 // ...
}
1
Adding a BindingResult next to the @ModelAttribute.
@PostMapping("/owners/{ownerId}/pets/{petId}/edit")
fun processSubmit(@ModelAttribute("pet") pet: Pet, result: BindingResult): String { (1)
 if (result.hasErrors()) {
 return "petForm"
 }
 // ...
}
1
Adding a BindingResult next to the @ModelAttribute.
You can automatically apply validation after data binding by adding the
jakarta.validation.Valid annotation or Spring’s @Validated annotation.
See Bean Validation and
Spring validation. For example:
Java
Kotlin
@PostMapping("/owners/{ownerId}/pets/{petId}/edit")
public String processSubmit(@Valid @ModelAttribute("pet") Pet pet, BindingResult result) { (1)
 if (result.hasErrors()) {
 return "petForm";
 }
 // ...
}
1
Validate the Pet instance.
@PostMapping("/owners/{ownerId}/pets/{petId}/edit")
fun processSubmit(@Valid @ModelAttribute("pet") pet: Pet, result: BindingResult): String { (1)
 if (result.hasErrors()) {
 return "petForm"
 }
 // ...
}
1
Validate the Pet instance.
If there is no BindingResult parameter after the @ModelAttribute, then
a MethodArgumentNotValidException is raised with the validation errors. However, if method
validation applies because other parameters have @jakarta.validation.Constraint annotations,
then HandlerMethodValidationException is raised instead. For more details, see the section
Validation.
Using @ModelAttribute is optional. By default, any parameter that is not a simple
value type as determined by
BeanUtils#isSimpleProperty
AND that is not resolved by any other argument resolver is treated as an implicit @ModelAttribute.
When compiling to a native image with GraalVM, the implicit @ModelAttribute
support described above does not allow proper ahead-of-time inference of related data
binding reflection hints. As a consequence, it is recommended to explicitly annotate
method parameters with @ModelAttribute for use in a GraalVM native image.
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
