---
snapshot_id: spring-mvc-exception-handler
source: "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 3
bytes: 12566
sha: "0b28cda1dc2eedab6c95ac7c02578ddf46643c5b14f7993dc89e160e386a8898"
---

# spring mvc exception handler — upstream snapshot

Source: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html
Fetched: 2026-07-14

Exceptions :: Spring Framework
Edit this Page
 
 
 
 GitHub Project
 
 
 
 Stack Overflow

# Exceptions
See equivalent in the Reactive stack
@Controller and @ControllerAdvice classes can have
@ExceptionHandler methods to handle exceptions from controller methods, as the following example shows:
Java
Kotlin
import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Controller
public class SimpleController {

 @ExceptionHandler(IOException.class)
 public ResponseEntity handle() {
 return ResponseEntity.internalServerError().body("Could not read file storage");
 }

}
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ExceptionHandler
import java.io.IOException

@Controller
class SimpleController {

 @ExceptionHandler(IOException::class)
 fun handle() : ResponseEntity {
 return ResponseEntity.internalServerError().body("Could not read file storage")
 }
 
}

## Exception Mapping
The exception may match against a top-level exception being propagated (for example, a direct
IOException being thrown) or against a nested cause within a wrapper exception (for example,
an IOException wrapped inside an IllegalStateException). As of 5.3, this can match
at arbitrary cause levels, whereas previously only an immediate cause was considered.
For matching exception types, preferably declare the target exception as a method argument,
as the preceding example shows. When multiple exception methods match, a root exception match is
generally preferred to a cause exception match. More specifically, the ExceptionDepthComparator
is used to sort exceptions based on their depth from the thrown exception type.
Alternatively, the annotation declaration may narrow the exception types to match,
as the following example shows:
Java
Kotlin
@ExceptionHandler({FileSystemException.class, RemoteException.class})
public ResponseEntity handleIOException(IOException ex) {
 return ResponseEntity.internalServerError().body(ex.getMessage());
}
@ExceptionHandler(FileSystemException::class, RemoteException::class)
fun handleIOException(ex: IOException): ResponseEntity {
 return ResponseEntity.internalServerError().body(ex.message)
}
You can even use a list of specific exception types with a very generic argument signature,
as the following example shows:
Java
Kotlin
@ExceptionHandler({FileSystemException.class, RemoteException.class})
public ResponseEntity handleExceptions(Exception ex) {
 return ResponseEntity.internalServerError().body(ex.getMessage());
}
@ExceptionHandler(FileSystemException::class, RemoteException::class)
fun handleExceptions(ex: Exception): ResponseEntity {
 return ResponseEntity.internalServerError().body(ex.message)
}
The distinction between root and cause exception matching can be surprising.
In the IOException variant shown earlier, the method is typically called with
the actual FileSystemException or RemoteException instance as the argument,
since both of them extend from IOException. However, if any such matching
exception is propagated within a wrapper exception which is itself an IOException,
the passed-in exception instance is that wrapper exception.
The behavior is even simpler in the handle(Exception) variant. This is
always invoked with the wrapper exception in a wrapping scenario, with the
actually matching exception to be found through ex.getCause() in that case.
The passed-in exception is the actual FileSystemException or
RemoteException instance only when these are thrown as top-level exceptions.
We generally recommend that you be as specific as possible in the argument signature,
reducing the potential for mismatches between root and cause exception types.
Consider breaking a multi-matching method into individual @ExceptionHandler
methods, each matching a single specific exception type through its signature.
In a multi-@ControllerAdvice arrangement, we recommend declaring your primary root exception
mappings on a @ControllerAdvice prioritized with a corresponding order. While a root
exception match is preferred to a cause, this is defined among the methods of a given
controller or @ControllerAdvice class. This means a cause match on a higher-priority
@ControllerAdvice bean is preferred to any match (for example, root) on a lower-priority
@ControllerAdvice bean.
Last but not least, an @ExceptionHandler method implementation can choose to back
out of dealing with a given exception instance by rethrowing it in its original form.
This is useful in scenarios where you are interested only in root-level matches or in
matches within a specific context that cannot be statically determined. A rethrown
exception is propagated through the remaining resolution chain, as though
the given @ExceptionHandler method would not have matched in the first place.
Support for @ExceptionHandler methods in Spring MVC is built on the DispatcherServlet
level, HandlerExceptionResolver mechanism.

## Media Type Mapping
See equivalent in the Reactive stack
In addition to exception types, @ExceptionHandler methods can also declare producible media types.
This allows to refine error responses depending on the media types requested by HTTP clients, typically in the "Accept" HTTP request header.
Applications can declare producible media types directly on annotations, for the same exception type:
Java
Kotlin
@ExceptionHandler(produces = "application/json")
public ResponseEntity handleJson(IllegalArgumentException exc) {
 return ResponseEntity.badRequest().body(new ErrorMessage(exc.getMessage(), 42));
}

@ExceptionHandler(produces = "text/html")
public String handle(IllegalArgumentException exc, Model model) {
 model.addAttribute("error", new ErrorMessage(exc.getMessage(), 42));
 return "errorView";
}
@ExceptionHandler(produces = ["application/json"])
fun handleJson(exc: IllegalArgumentException): ResponseEntity {
 return ResponseEntity.badRequest().body(ErrorMessage(exc.message, 42))
}

@ExceptionHandler(produces = ["text/html"])
fun handle(exc: IllegalArgumentException, model: Model): String {
 model.addAttribute("error", ErrorMessage(exc.message, 42))
 return "errorView"
}
Here, methods handle the same exception type but will not be rejected as duplicates.
Instead, API clients requesting "application/json" will receive a JSON error, and browsers will get an HTML error view.
Each @ExceptionHandler annotation can declare several producible media types,
the content negotiation during the error handling phase will decide which content type will be used.

## Method Arguments
See equivalent in the Reactive stack
@ExceptionHandler methods support the following arguments:
Method argument
Description
Exception type
For access to the raised exception.
HandlerMethod
For access to the controller method that raised the exception.
WebRequest, NativeWebRequest
Generic access to request parameters and request and session attributes without direct
 use of the Servlet API.
jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse
Choose any specific request or response type (for example, ServletRequest or
 HttpServletRequest or Spring’s MultipartRequest or MultipartHttpServletRequest).
jakarta.servlet.http.HttpSession
Enforces the presence of a session. As a consequence, such an argument is never null.

 Note that session access is not thread-safe. Consider setting the
 RequestMappingHandlerAdapter instance’s synchronizeOnSession flag to true if multiple
 requests are allowed to access a session concurrently.
java.security.Principal
Currently authenticated user — possibly a specific Principal implementation class if known.
HttpMethod
The HTTP method of the request.
java.util.Locale
The current request locale, determined by the most specific LocaleResolver available — in
 effect, the configured LocaleResolver or LocaleContextResolver.
java.util.TimeZone, java.time.ZoneId
The time zone associated with the current request, as determined by a LocaleContextResolver.
java.io.OutputStream, java.io.Writer
For access to the raw response body, as exposed by the Servlet API.
java.util.Map, org.springframework.ui.Model, org.springframework.ui.ModelMap
For access to the model for an error response. Always empty.
RedirectAttributes
Specify attributes to use in case of a redirect — (that is to be appended to the query
 string) and flash attributes to be stored temporarily until the request after the redirect.
 See Redirect Attributes and Flash Attributes.
@SessionAttribute
For access to any session attribute, in contrast to model attributes stored in the
 session as a result of a class-level @SessionAttributes declaration.
 See @SessionAttribute for more details.
@RequestAttribute
For access to request attributes. See @RequestAttribute for more details.

## Return Values
See equivalent in the Reactive stack
@ExceptionHandler methods support the following return values:
Return value
Description
@ResponseBody
The return value is converted through HttpMessageConverter instances and written to the
 response. See @ResponseBody.
HttpEntity, ResponseEntity
The return value specifies that the full response (including the HTTP headers and the body)
 be converted through HttpMessageConverter instances and written to the response.
 See ResponseEntity.
ErrorResponse, ProblemDetail
To render an RFC 9457 error response with details in the body,
 see Error Responses
String
A view name to be resolved with ViewResolver implementations and used together with the
 implicit model — determined through command objects and @ModelAttribute methods.
 The handler method can also programmatically enrich the model by declaring a Model
 argument (described earlier).
View
A View instance to use for rendering together with the implicit model — determined
 through command objects and @ModelAttribute methods. The handler method may also
 programmatically enrich the model by declaring a Model argument (described earlier).
java.util.Map, org.springframework.ui.Model
Attributes to be added to the implicit model with the view name implicitly determined
 through a RequestToViewNameTranslator.
@ModelAttribute
An attribute to be added to the model with the view name implicitly determined through
 a RequestToViewNameTranslator.
Note that @ModelAttribute is optional. See “Any other return value” at the end of
 this table.
ModelAndView object
The view and model attributes to use and, optionally, a response status.
void
A method with a void return type (or null return value) is considered to have fully
 handled the response if it also has a ServletResponse an OutputStream argument, or
 a @ResponseStatus annotation. The same is also true if the controller has made a positive
 ETag or lastModified timestamp check (see Controllers for details).
If none of the above is true, a void return type can also indicate “no response body” for
 REST controllers or default view name selection for HTML controllers.
Any other return value
If a return value is not matched to any of the above and is not a simple type (as determined by
 BeanUtils#isSimpleProperty),
 by default, it is treated as a model attribute to be added to the model. If it is a simple type,
 it remains unresolved.
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
