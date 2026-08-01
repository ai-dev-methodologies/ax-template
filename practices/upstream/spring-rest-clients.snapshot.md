# spring-rest-clients — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://docs.spring.io/spring-framework/reference/integration/rest-clients.html (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T02:24:31Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://docs.spring.io/spring-framework/reference/integration/rest-clients.html`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r150`
**Body SHA-256 (below the `---` divider, header excluded):** 383291fc9473b93bd571eb6ae1ca57dc2bc89ce67c0119d5582acace7ec5936e

---

---
snapshot_id: spring-rest-clients
source: "https://docs.spring.io/spring-framework/reference/integration/rest-clients.html"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 3
bytes: 41300
sha: "0a1613ba876fe6d8b47415036b3335e828d8d24cfd74527aa00e0efa9d837492"
---

# spring rest clients — upstream snapshot

Source: https://docs.spring.io/spring-framework/reference/integration/rest-clients.html
Fetched: 2026-07-14

REST Clients :: Spring Framework
Edit this Page
 
 
 
 GitHub Project
 
 
 
 Stack Overflow

# REST Clients
The Spring Framework provides the following choices for making calls to REST endpoints:
RestClient — synchronous client with a fluent API
WebClient — non-blocking, reactive client with fluent API
RestTemplate — synchronous client with template method API, now deprecated in favor of RestClient
HTTP Service Clients — annotated interface backed by generated proxy

## RestClient
RestClient is a synchronous HTTP client that provides a fluent API to perform requests.
It serves as an abstraction over HTTP libraries, and handles conversion of HTTP request and response content to and from higher level Java objects.

### Create a RestClient
RestClient has static create shortcut methods.
It also exposes a builder() with further options:
select the HTTP library to use, see Client Request Factories
configure message converters, see HTTP Message Conversion
set a baseUrl
set default request headers, cookies, path variables, API version
configure an ApiVersionInserter
register interceptors
register request initializers
Once created, a RestClient is safe to use in multiple threads.
The below shows how to create or build a RestClient:
Java
Kotlin
RestClient defaultClient = RestClient.create();

RestClient customClient = RestClient.builder()
 .requestFactory(new HttpComponentsClientHttpRequestFactory())
 .messageConverters(converters -> converters.add(new MyCustomMessageConverter()))
 .baseUrl("https://example.com")
 .defaultUriVariables(Map.of("variable", "foo"))
 .defaultHeader("My-Header", "Foo")
 .defaultCookie("My-Cookie", "Bar")
 .defaultVersion("1.2")
 .apiVersionInserter(ApiVersionInserter.fromHeader("API-Version").build())
 .requestInterceptor(myCustomInterceptor)
 .requestInitializer(myCustomInitializer)
 .build();
val defaultClient = RestClient.create()

val customClient = RestClient.builder()
 .requestFactory(HttpComponentsClientHttpRequestFactory())
 .messageConverters { converters -> converters.add(MyCustomMessageConverter()) }
 .baseUrl("https://example.com")
 .defaultUriVariables(mapOf("variable" to "foo"))
 .defaultHeader("My-Header", "Foo")
 .defaultCookie("My-Cookie", "Bar")
 .defaultVersion("1.2")
 .apiVersionInserter(ApiVersionInserter.fromHeader("API-Version").build())
 .requestInterceptor(myCustomInterceptor)
 .requestInitializer(myCustomInitializer)
 .build()

### Use the RestClient
To perform an HTTP request, first specify the HTTP method to use.
Use the convenience methods like get(), head(), post(), and others, or method(HttpMethod).

#### Request URL
Next, specify the request URI with the uri methods.
This is optional, and you can skip this step if you configured a baseUrl through the builder.
The URL is typically specified as a String, with optional URI template variables.
The following shows how to perform a request:
Java
Kotlin
int id = 42;
restClient.get()
 .uri("https://example.com/orders/{id}", id)
 // ...
val id = 42
restClient.get()
 .uri("https://example.com/orders/{id}", id)
 // ...
A function can also be used for more controls, such as specifying request parameters.
String URLs are encoded by default, but this can be changed by building a client with a custom uriBuilderFactory.
The URL can also be provided with a function or as a java.net.URI, both of which are not encoded.
For more details on working with and encoding URIs, see URI Links.

#### Request headers and body
If necessary, the HTTP request can be manipulated by adding request headers with header(String, String), headers(Consumer, or with the convenience methods accept(MediaType…​), acceptCharset(Charset…​) and so on.
For HTTP requests that can contain a body (POST, PUT, and PATCH), additional methods are available: contentType(MediaType), and contentLength(long).
You can set an API version for the request if the client is configured with ApiVersionInserter.
The request body itself can be set by body(Object), which internally uses HTTP Message Conversion.
Alternatively, the request body can be set using a ParameterizedTypeReference, allowing you to use generics.
Finally, the body can be set to a callback function that writes to an OutputStream.

#### Retrieving the response
Once the request has been set up, it can be sent by chaining method calls after retrieve().
For example, the response body can be accessed by using retrieve().body(Class) or retrieve().body(ParameterizedTypeReference) for parameterized types like lists.
The body method converts the response contents into various types – for instance, bytes can be converted into a String, JSON can be converted into objects using Jackson, and so on (see HTTP Message Conversion).
The response can also be converted into a ResponseEntity, giving access to the response headers as well as the body, with retrieve().toEntity(Class)
Calling retrieve() by itself is a no-op and returns a ResponseSpec.
Applications must invoke a terminal operation on the ResponseSpec to have any side effect.
If consuming the response has no interest for your use case, you can use retrieve().toBodilessEntity().
This sample shows how RestClient can be used to perform a simple GET request.
Java
Kotlin
String result = restClient.get() (1)
 .uri("https://example.com") (2)
 .retrieve() (3)
 .body(String.class); (4)

System.out.println(result); (5)
1
Set up a GET request
2
Specify the URL to connect to
3
Retrieve the response
4
Convert the response into a string
5
Print the result
val result= restClient.get() (1)
 .uri("https://example.com") (2)
 .retrieve() (3)
 .body() (4)

println(result) (5)
1
Set up a GET request
2
Specify the URL to connect to
3
Retrieve the response
4
Convert the response into a string
5
Print the result
Access to the response status code and headers is provided through ResponseEntity:
Java
Kotlin
ResponseEntity result = restClient.get() (1)
 .uri("https://example.com") (1)
 .retrieve()
 .toEntity(String.class); (2)

System.out.println("Response status: " + result.getStatusCode()); (3)
System.out.println("Response headers: " + result.getHeaders()); (3)
System.out.println("Contents: " + result.getBody()); (3)
1
Set up a GET request for the specified URL
2
Convert the response into a ResponseEntity
3
Print the result
val result = restClient.get() (1)
 .uri("https://example.com") (1)
 .retrieve()
 .toEntity() (2)

println("Response status: " + result.statusCode) (3)
println("Response headers: " + result.headers) (3)
println("Contents: " + result.body) (3)
1
Set up a GET request for the specified URL
2
Convert the response into a ResponseEntity
3
Print the result
RestClient can convert JSON to objects, using the Jackson library.
Note the usage of URI variables in this sample and that the Accept header is set to JSON.
Java
Kotlin
int id = ...;
Pet pet = restClient.get()
 .uri("https://petclinic.example.com/pets/{id}", id) (1)
 .accept(APPLICATION_JSON) (2)
 .retrieve()
 .body(Pet.class); (3)
1
Using URI variables
2
Set the Accept header to application/json
3
Convert the JSON response into a Pet domain object
val id = ...
val pet = restClient.get()
 .uri("https://petclinic.example.com/pets/{id}", id) (1)
 .accept(APPLICATION_JSON) (2)
 .retrieve()
 .body() (3)
1
Using URI variables
2
Set the Accept header to application/json
3
Convert the JSON response into a Pet domain object
In the next sample, RestClient is used to perform a POST request that contains JSON, which again is converted using Jackson.
Java
Kotlin
Pet pet = ... (1)
ResponseEntity response = restClient.post() (2)
 .uri("https://petclinic.example.com/pets/new") (2)
 .contentType(APPLICATION_JSON) (3)
 .body(pet) (4)
 .retrieve()
 .toBodilessEntity(); (5)
1
Create a Pet domain object
2
Set up a POST request, and the URL to connect to
3
Set the Content-Type header to application/json
4
Use pet as the request body
5
Convert the response into a response entity with no body.
val pet: Pet = ... (1)
val response = restClient.post() (2)
 .uri("https://petclinic.example.com/pets/new") (2)
 .contentType(APPLICATION_JSON) (3)
 .body(pet) (4)
 .retrieve()
 .toBodilessEntity() (5)
1
Create a Pet domain object
2
Set up a POST request, and the URL to connect to
3
Set the Content-Type header to application/json
4
Use pet as the request body
5
Convert the response into a response entity with no body.

#### Error handling
By default, RestClient throws a subclass of RestClientException when retrieving a response with a 4xx or 5xx status code.
This behavior can be overridden using onStatus.
Java
Kotlin
String result = restClient.get() (1)
 .uri("https://example.com/this-url-does-not-exist") (1)
 .retrieve()
 .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> { (2)
 throw new MyCustomRuntimeException(response.getStatusCode(), response.getHeaders()); (3)
 })
 .body(String.class);
1
Create a GET request for a URL that returns a 404 status code
2
Set up a status handler for all 4xx status codes
3
Throw a custom exception
val result = restClient.get() (1)
 .uri("https://example.com/this-url-does-not-exist") (1)
 .retrieve()
 .onStatus(HttpStatusCode::is4xxClientError) { _, response -> (2)
 throw MyCustomRuntimeException(response.getStatusCode(), response.getHeaders()) } (3)
 .body()
1
Create a GET request for a URL that returns a 404 status code
2
Set up a status handler for all 4xx status codes
3
Throw a custom exception

#### Exchange
For more advanced scenarios, the RestClient gives access to the underlying HTTP request and response through the exchange() method, which can be used instead of retrieve().
Status handlers are not applied when use exchange(), because the exchange function already provides access to the full response, allowing you to perform any error handling necessary.
Java
Kotlin
Pet result = restClient.get()
 .uri("https://petclinic.example.com/pets/{id}", id)
 .accept(APPLICATION_JSON)
 .exchange((request, response) -> { (1)
 if (response.getStatusCode().is4xxClientError()) { (2)
 throw new MyCustomRuntimeException(response.getStatusCode(), response.getHeaders()); (2)
 }
 else {
 Pet pet = convertResponse(response); (3)
 return pet;
 }
 });
1
exchange provides the request and response
2
Throw an exception when the response has a 4xx status code
3
Convert the response into a Pet domain object
val result = restClient.get()
 .uri("https://petclinic.example.com/pets/{id}", id)
 .accept(MediaType.APPLICATION_JSON)
 .exchange { request, response -> (1)
 if (response.getStatusCode().is4xxClientError()) { (2)
 throw MyCustomRuntimeException(response.getStatusCode(), response.getHeaders()) (2)
 } else {
 val pet: Pet = convertResponse(response) (3)
 pet
 }
 }
1
exchange provides the request and response
2
Throw an exception when the response has a 4xx status code
3
Convert the response into a Pet domain object

### HTTP Message Conversion
See the supported HTTP message converters in the dedicated section.

#### Jackson JSON Views
To serialize only a subset of the object properties, you can specify a Jackson JSON View, as the following example shows:
MappingJacksonValue value = new MappingJacksonValue(new User("eric", "7!jd#h23"));
value.setSerializationView(User.WithoutPasswordView.class);

ResponseEntity response = restClient.post() // or RestTemplate.postForEntity
 .contentType(APPLICATION_JSON)
 .body(value)
 .retrieve()
 .toBodilessEntity();

#### Multipart
To send multipart data, you need to provide a MultiValueMap whose values may be an Object for part content, a Resource for a file part, or an HttpEntity for part content with headers.
For example:
MultiValueMap parts = new LinkedMultiValueMap<>();

parts.add("fieldPart", "fieldValue");
parts.add("filePart", new FileSystemResource("...logo.png"));
parts.add("jsonPart", new Person("Jason"));

HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_XML);
parts.add("xmlPart", new HttpEntity<>(myBean, headers));

// send using RestClient.post or RestTemplate.postForEntity
In most cases, you do not have to specify the Content-Type for each part.
The content type is determined automatically based on the HttpMessageConverter chosen to serialize it or, in the case of a Resource, based on the file extension.
If necessary, you can explicitly provide the MediaType with an HttpEntity wrapper.
Once the MultiValueMap is ready, you can use it as the body of a POST request, using RestClient.post().body(parts) (or RestTemplate.postForObject).
If the MultiValueMap contains at least one non-String value, the Content-Type is set to multipart/form-data by the FormHttpMessageConverter.
If the MultiValueMap has String values, the Content-Type defaults to application/x-www-form-urlencoded.
If necessary the Content-Type may also be set explicitly.

### Client Request Factories
To execute the HTTP request, RestClient uses a client HTTP library.
These libraries are adapted via the ClientRequestFactory interface.
Various implementations are available:
JdkClientHttpRequestFactory for Java’s HttpClient
HttpComponentsClientHttpRequestFactory for use with Apache HTTP Components HttpClient
JettyClientHttpRequestFactory for Jetty’s HttpClient
ReactorNettyClientRequestFactory for Reactor Netty’s HttpClient
SimpleClientHttpRequestFactory as a simple default
If no request factory is specified when the RestClient was built, it will use the Apache or Jetty HttpClient if they are available on the classpath.
Otherwise, if the java.net.http module is loaded, it will use Java’s HttpClient.
Finally, it will resort to the simple default.
Note that the SimpleClientHttpRequestFactory may raise an exception when accessing the status of a response that represents an error (for example, 401).
If this is an issue, use any of the alternative request factories.

## WebClient
WebClient is a non-blocking, reactive client to perform HTTP requests. It was
introduced in 5.0 and offers an alternative to the RestTemplate, with support for
synchronous, asynchronous, and streaming scenarios.
WebClient supports the following:
Non-blocking I/O
Reactive Streams back pressure
High concurrency with fewer hardware resources
Functional-style, fluent API that takes advantage of lambda expressions
Synchronous and asynchronous interactions
Streaming up to or streaming down from a server
See WebClient for more details.

## RestTemplate
The RestTemplate provides a high-level API over HTTP client libraries in the form of a classic Spring Template class.
It exposes the following groups of overloaded methods:
As of Spring Framework 7.0, RestTemplate is deprecated in favor of RestClient and will be removed in a future version,
please use the "Migrating to RestClient" guide.
For asynchronous and streaming scenarios, consider the reactive WebClient.
Table 1. RestTemplate methods
Method group
Description
getForObject
Retrieves a representation via GET.
getForEntity
Retrieves a ResponseEntity (that is, status, headers, and body) by using GET.
headForHeaders
Retrieves all headers for a resource by using HEAD.
postForLocation
Creates a new resource by using POST and returns the Location header from the response.
postForObject
Creates a new resource by using POST and returns the representation from the response.
postForEntity
Creates a new resource by using POST and returns the representation from the response.
put
Creates or updates a resource by using PUT.
patchForObject
Updates a resource by using PATCH and returns the representation from the response.
Note that the JDK HttpURLConnection does not support PATCH, but Apache HttpComponents and others do.
delete
Deletes the resources at the specified URI by using DELETE.
optionsForAllow
Retrieves allowed HTTP methods for a resource by using ALLOW.
exchange
More generalized (and less opinionated) version of the preceding methods that provides extra flexibility when needed.
It accepts a RequestEntity (including HTTP method, URL, headers, and body as input) and returns a ResponseEntity.
These methods allow the use of ParameterizedTypeReference instead of Class to specify
a response type with generics.
execute
The most generalized way to perform a request, with full control over request
preparation and response extraction through callback interfaces.

### Initialization
RestTemplate uses the same HTTP library abstraction as RestClient.
By default, it uses the SimpleClientHttpRequestFactory, but this can be changed via the constructor.
See Client Request Factories.
RestTemplate can be instrumented for observability, in order to produce metrics and traces.
See the RestTemplate Observability support section.

### Body
Objects passed into and returned from RestTemplate methods are converted to and from HTTP messages
with the help of an HttpMessageConverter, see HTTP Message Conversion.

### Migrating to RestClient
Applications can adopt RestClient in a gradual fashion, first focusing on API usage and then on infrastructure setup.
You can consider the following steps:
Create one or more RestClient from existing RestTemplate instances, like: RestClient restClient = RestClient.create(restTemplate).
Gradually replace RestTemplate usage in your application, component by component, by focusing first on issuing requests.
See the table below for API equivalents.
Once all client requests go through RestClient instances, you can now work on replicating your existing
RestTemplate instance creations by using RestClient.Builder. Because RestTemplate and RestClient
share the same infrastructure, you can reuse custom ClientHttpRequestFactory or ClientHttpRequestInterceptor
in your setup. See the RestClient builder API.
If no other library is available on the classpath, RestClient will choose the JdkClientHttpRequestFactory
powered by the modern JDK HttpClient, whereas RestTemplate would pick the SimpleClientHttpRequestFactory that
uses HttpURLConnection. This can explain subtle behavior difference at runtime at the HTTP level.
The following table shows RestClient equivalents for RestTemplate methods.
Table 2. RestClient equivalents for RestTemplate methods
RestTemplate method
RestClient equivalent
getForObject(String, Class, Object…​)
get()
.uri(String, Object…​)
.retrieve()
.body(Class)
getForObject(String, Class, Map)
get()
.uri(String, Map)
.retrieve()
.body(Class)
getForObject(URI, Class)
get()
.uri(URI)
.retrieve()
.body(Class)
getForEntity(String, Class, Object…​)
get()
.uri(String, Object…​)
.retrieve()
.toEntity(Class)
getForEntity(String, Class, Map)
get()
.uri(String, Map)
.retrieve()
.toEntity(Class)
getForEntity(URI, Class)
get()
.uri(URI)
.retrieve()
.toEntity(Class)
headForHeaders(String, Object…​)
head()
.uri(String, Object…​)
.retrieve()
.toBodilessEntity()
.getHeaders()
headForHeaders(String, Map)
head()
.uri(String, Map)
.retrieve()
.toBodilessEntity()
.getHeaders()
headForHeaders(URI)
head()
.uri(URI)
.retrieve()
.toBodilessEntity()
.getHeaders()
postForLocation(String, Object, Object…​)
post()
.uri(String, Object…​)
.body(Object).retrieve()
.toBodilessEntity()
.getLocation()
postForLocation(String, Object, Map)
post()
.uri(String, Map)
.body(Object)
.retrieve()
.toBodilessEntity()
.getLocation()
postForLocation(URI, Object)
post()
.uri(URI)
.body(Object)
.retrieve()
.toBodilessEntity()
.getLocation()
postForObject(String, Object, Class, Object…​)
post()
.uri(String, Object…​)
.body(Object)
.retrieve()
.body(Class)
postForObject(String, Object, Class, Map)
post()
.uri(String, Map)
.body(Object)
.retrieve()
.body(Class)
postForObject(URI, Object, Class)
post()
.uri(URI)
.body(Object)
.retrieve()
.body(Class)
postForEntity(String, Object, Class, Object…​)
post()
.uri(String, Object…​)
.body(Object)
.retrieve()
.toEntity(Class)
postForEntity(String, Object, Class, Map)
post()
.uri(String, Map)
.body(Object)
.retrieve()
.toEntity(Class)
postForEntity(URI, Object, Class)
post()
.uri(URI)
.body(Object)
.retrieve()
.toEntity(Class)
put(String, Object, Object…​)
put()
.uri(String, Object…​)
.body(Object)
.retrieve()
.toBodilessEntity()
put(String, Object, Map)
put()
.uri(String, Map)
.body(Object)
.retrieve()
.toBodilessEntity()
put(URI, Object)
put()
.uri(URI)
.body(Object)
.retrieve()
.toBodilessEntity()
patchForObject(String, Object, Class, Object…​)
patch()
.uri(String, Object…​)
.body(Object)
.retrieve()
.body(Class)
patchForObject(String, Object, Class, Map)
patch()
.uri(String, Map)
.body(Object)
.retrieve()
.body(Class)
patchForObject(URI, Object, Class)
patch()
.uri(URI)
.body(Object)
.retrieve()
.body(Class)
delete(String, Object…​)
delete()
.uri(String, Object…​)
.retrieve()
.toBodilessEntity()
delete(String, Map)
delete()
.uri(String, Map)
.retrieve()
.toBodilessEntity()
delete(URI)
delete()
.uri(URI)
.retrieve()
.toBodilessEntity()
optionsForAllow(String, Object…​)
options()
.uri(String, Object…​)
.retrieve()
.toBodilessEntity()
.getAllow()
optionsForAllow(String, Map)
options()
.uri(String, Map)
.retrieve()
.toBodilessEntity()
.getAllow()
optionsForAllow(URI)
options()
.uri(URI)
.retrieve()
.toBodilessEntity()
.getAllow()
exchange(String, HttpMethod, HttpEntity, Class, Object…​)
method(HttpMethod)
.uri(String, Object…​)
.headers(Consumer)
.body(Object)
.retrieve()
.toEntity(Class) [1]
exchange(String, HttpMethod, HttpEntity, Class, Map)
method(HttpMethod)
.uri(String, Map)
.headers(Consumer)
.body(Object)
.retrieve()
.toEntity(Class) [1]
exchange(URI, HttpMethod, HttpEntity, Class)
method(HttpMethod)
.uri(URI)
.headers(Consumer)
.body(Object)
.retrieve()
.toEntity(Class) [1]
exchange(String, HttpMethod, HttpEntity, ParameterizedTypeReference, Object…​)
method(HttpMethod)
.uri(String, Object…​)
.headers(Consumer)
.body(Object)
.retrieve()
.toEntity(ParameterizedTypeReference) [1]
exchange(String, HttpMethod, HttpEntity, ParameterizedTypeReference, Map)
method(HttpMethod)
.uri(String, Map)
.headers(Consumer)
.body(Object)
.retrieve()
.toEntity(ParameterizedTypeReference) [1]
exchange(URI, HttpMethod, HttpEntity, ParameterizedTypeReference)
method(HttpMethod)
.uri(URI)
.headers(Consumer)
.body(Object)
.retrieve()
.toEntity(ParameterizedTypeReference) [1]
exchange(RequestEntity, Class)
method(HttpMethod)
.uri(URI)
.headers(Consumer)
.body(Object)
.retrieve()
.toEntity(Class) [2]
exchange(RequestEntity, ParameterizedTypeReference)
method(HttpMethod)
.uri(URI)
.headers(Consumer)
.body(Object)
.retrieve()
.toEntity(ParameterizedTypeReference) [2]
execute(String, HttpMethod, RequestCallback, ResponseExtractor, Object…​)
method(HttpMethod)
.uri(String, Object…​)
.exchange(ExchangeFunction)
execute(String, HttpMethod, RequestCallback, ResponseExtractor, Map)
method(HttpMethod)
.uri(String, Map)
.exchange(ExchangeFunction)
execute(URI, HttpMethod, RequestCallback, ResponseExtractor)
method(HttpMethod)
.uri(URI)
.exchange(ExchangeFunction)
RestClient and RestTemplate instances share the same behavior when it comes to throwing exceptions
(with the RestClientException type being at the top of the hierarchy).
When RestTemplate consistently throws HttpClientErrorException for "4xx" response statues,
RestClient allows for more flexibility with custom "status handlers".

## HTTP Service Clients
You can define an HTTP Service as a Java interface with @HttpExchange methods, and use
HttpServiceProxyFactory to create a client proxy from it for remote access over HTTP via
RestClient, WebClient, or RestTemplate. On the server side, an @Controller class
can implement the same interface to handle requests with
@HttpExchange
controller methods.
First, create the Java interface:
public interface RepositoryService {

 @GetExchange("/repos/{owner}/{repo}")
 Repository getRepository(@PathVariable String owner, @PathVariable String repo);

 // more HTTP exchange methods...

}
Optionally, use @HttpExchange at the type level to declare common attributes for all methods:
@HttpExchange(url = "/repos/{owner}/{repo}", accept = "application/vnd.github.v3+json")
public interface RepositoryService {

 @GetExchange
 Repository getRepository(@PathVariable String owner, @PathVariable String repo);

 @PatchExchange(contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
 void updateRepository(@PathVariable String owner, @PathVariable String repo,
 @RequestParam String name, @RequestParam String description, @RequestParam String homepage);

}
Next, configure the client and create the HttpServiceProxyFactory:
// Using RestClient...

RestClient restClient = RestClient.create("...");
RestClientAdapter adapter = RestClientAdapter.create(restClient);

// or WebClient...

WebClient webClient = WebClient.create("...");
WebClientAdapter adapter = WebClientAdapter.create(webClient);

// or RestTemplate...

RestTemplate restTemplate = new RestTemplate();
RestTemplateAdapter adapter = RestTemplateAdapter.create(restTemplate);

HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
Now, you’re ready to create client proxies:
RepositoryService service = factory.createClient(RepositoryService.class);
// Use service methods for remote calls...
HTTP service clients is a powerful and expressive choice for remote access over HTTP.
It allows one team to own the knowledge of how a REST API works, what parts are relevant
to a client application, what input and output types to create, what endpoint method
signatures are needed, what Javadoc to have, and so on. The resulting Java API guides and
is ready to use.

### Method Parameters
@HttpExchange methods support flexible method signatures with the following inputs:
Method parameter
Description
URI
Dynamically set the URL for the request, overriding the annotation’s url attribute.
UriBuilderFactory
Provide a UriBuilderFactory to expand the URI template and URI variables with.
 In effect, replaces the UriBuilderFactory (and its base URL) of the underlying client.
HttpMethod
Dynamically set the HTTP method for the request, overriding the annotation’s method attribute
@RequestHeader
Add a request header or multiple headers. The argument may be a single value,
 a Collection of values, Map,MultiValueMap.
 Type conversion is supported for non-String values. Header values are added and
 do not override already added header values.
@PathVariable
Add a variable for expand a placeholder in the request URL. The argument may be a
 Map with multiple variables, or an individual value. Type conversion
 is supported for non-String values.
@RequestAttribute
Provide an Object to add as a request attribute. Only supported by RestClient
 and WebClient.
@RequestBody
Provide the body of the request either as an Object to be serialized, or a
 Reactive Streams Publisher such as Mono, Flux, or any other async type supported
 through the configured ReactiveAdapterRegistry.
@RequestParam
Add a request parameter or multiple parameters. The argument may be a Map
 or MultiValueMap with multiple parameters, a Collection of values, or
 an individual value. Type conversion is supported for non-String values.
When "content-type" is set to "application/x-www-form-urlencoded", request
 parameters are encoded in the request body. Otherwise, they are added as URL query
 parameters.
@RequestPart
Add a request part, which may be a String (form field), Resource (file part),
 Object (entity to be encoded, for example, as JSON), HttpEntity (part content and headers),
 a Spring Part, or Reactive Streams Publisher of any of the above.
MultipartFile
Add a request part from a MultipartFile, typically used in a Spring MVC controller
 where it represents an uploaded file.
@CookieValue
Add a cookie or multiple cookies. The argument may be a Map or
 MultiValueMap with multiple cookies, a Collection of values, or an
 individual value. Type conversion is supported for non-String values.
Method parameters cannot be null unless the required attribute (where available on a
parameter annotation) is set to false, or the parameter is marked optional as determined by
MethodParameter#isOptional.
RestClientAdapter provides additional support for a method parameter of type
StreamingHttpOutputMessage.Body that allows sending the request body by writing to an
OutputStream.

### Custom Arguments
You can configure a custom HttpServiceArgumentResolver. The example interface below
uses a custom Search method parameter type:
Java
Kotlin
public interface RepositoryService {

 @GetExchange("/repos/search")
 List searchRepository(Search search);

}
interface RepositoryService {

 @GetExchange("/repos/search")
 fun searchRepository(search: Search): List

}
A custom argument resolver could be implemented like this:
Java
Kotlin
static class SearchQueryArgumentResolver implements HttpServiceArgumentResolver {
 @Override
 public boolean resolve(Object argument, MethodParameter parameter, HttpRequestValues.Builder requestValues) {
 if (parameter.getParameterType().equals(Search.class)) {
 Search search = (Search) argument;
 requestValues.addRequestParameter("owner", search.owner());
 requestValues.addRequestParameter("language", search.language());
 requestValues.addRequestParameter("query", search.query());
 return true;
 }
 return false;
 }
}
class SearchQueryArgumentResolver : HttpServiceArgumentResolver {
 override fun resolve(
 argument: Any?,
 parameter: MethodParameter,
 requestValues: HttpRequestValues.Builder
 ): Boolean {
 if (parameter.getParameterType() == Search::class.java) {
 val search = argument as Search
 requestValues.addRequestParameter("owner", search.owner)
 .addRequestParameter("language", search.language)
 .addRequestParameter("query", search.query)
 return true
 }
 return false
 }
}
To configure the custom argument resolver:
Java
Kotlin
RestClient restClient = RestClient.builder().baseUrl("https://api.github.com/").build();
RestClientAdapter adapter = RestClientAdapter.create(restClient);
HttpServiceProxyFactory factory = HttpServiceProxyFactory
 .builderFor(adapter)
 .customArgumentResolver(new SearchQueryArgumentResolver())
 .build();
RepositoryService repositoryService = factory.createClient(RepositoryService.class);

Search search = Search.create()
 .owner("spring-projects")
 .language("java")
 .query("rest")
 .build();
List repositories = repositoryService.searchRepository(search);
val restClient = RestClient.builder().baseUrl("https://api.github.com/").build()
val adapter = RestClientAdapter.create(restClient)
val factory = HttpServiceProxyFactory
 .builderFor(adapter)
 .customArgumentResolver(SearchQueryArgumentResolver())
 .build()
val repositoryService = factory.createClient(RepositoryService::class.java)

val search = Search(owner = "spring-projects", language = "java", query = "rest")
val repositories = repositoryService.searchRepository(search)
By default, RequestEntity is not supported as a method parameter, instead encouraging
the use of more fine-grained method parameters for individual parts of the request.

### Return Values
The supported return values depend on the underlying client.
Clients adapted to HttpExchangeAdapter such as RestClient and RestTemplate
support synchronous return values:
Method return value
Description
void
Perform the given request.
HttpHeaders
Perform the given request and return the response headers.
Perform the given request and decode the response content to the declared return type.
ResponseEntity
Perform the given request and return a ResponseEntity with the status and headers.
ResponseEntity
Perform the given request, decode the response content to the declared return type, and
 return a ResponseEntity with the status, headers, and the decoded body.
Clients adapted to ReactorHttpExchangeAdapter such as WebClient, support all of above
as well as reactive variants. The table below shows Reactor types, but you can also use
other reactive types that are supported through the ReactiveAdapterRegistry:
Method return value
Description
Mono
Perform the given request, and release the response content, if any.
Mono
Perform the given request, release the response content, if any, and return the
response headers.
Mono
Perform the given request and decode the response content to the declared return type.
Flux
Perform the given request and decode the response content to a stream of the declared
element type.
Mono>
Perform the given request, and release the response content, if any, and return a
ResponseEntity with the status and headers.
Mono>
Perform the given request, decode the response content to the declared return type, and
return a ResponseEntity with the status, headers, and the decoded body.
Mono>
Perform the given request, decode the response content to a stream of the declared
element type, and return a ResponseEntity with the status, headers, and the decoded
response body stream.
By default, the timeout for synchronous return values with ReactorHttpExchangeAdapter
depends on how the underlying HTTP client is configured. You can set a blockTimeout
value on the adapter level as well, but we recommend relying on timeout settings of the
underlying HTTP client, which operates at a lower level and provides more control.
RestClientAdapter provides supports additional support for a return value of type
InputStream or ResponseEntity that provides access to the raw response
body content.

### Error Handling
To customize error handling for HTTP Service client proxies, you can configure the
underlying client as needed. By default, clients raise an exception for 4xx and 5xx HTTP
status codes. To customize this, register a response status handler that applies to all
responses performed through the client as follows:
// For RestClient
RestClient restClient = RestClient.builder()
 .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> ...)
 .build();
RestClientAdapter adapter = RestClientAdapter.create(restClient);

// or for WebClient...
WebClient webClient = WebClient.builder()
 .defaultStatusHandler(HttpStatusCode::isError, resp -> ...)
 .build();
WebClientAdapter adapter = WebClientAdapter.create(webClient);

// or for RestTemplate...
RestTemplate restTemplate = new RestTemplate();
restTemplate.setErrorHandler(myErrorHandler);

RestTemplateAdapter adapter = RestTemplateAdapter.create(restTemplate);

HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
For more details and options such as suppressing error status codes, see the reference
documentation for each client, as well as the Javadoc of defaultStatusHandler in
RestClient.Builder or WebClient.Builder, and the setErrorHandler of RestTemplate.

### Decorating the Adapter
HttpExchangeAdapter and ReactorHttpExchangeAdapter are contracts that decouple HTTP
Interface client infrastructure from the details of invoking the underlying
client. There are adapter implementations for RestClient, WebClient, and
RestTemplate.
Occasionally, it may be useful to intercept client invocations through a decorator
configurable in the HttpServiceProxyFactory.Builder. For example, you can apply
built-in decorators to suppress 404 exceptions and return a ResponseEntity with
NOT_FOUND and a null body:
// For RestClient
HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(restClientAdapter)
 .exchangeAdapterDecorator(NotFoundRestClientAdapterDecorator::new)
 .build();

// or for WebClient...
HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory.builderFor(webClientAdapter)
 .exchangeAdapterDecorator(NotFoundWebClientAdapterDecorator::new)
 .build();

### HTTP Service Groups
It’s trivial to create client proxies with HttpServiceProxyFactory, but to have them
declared as beans leads to repetitive configuration. You may also have multiple
target hosts, and therefore multiple clients to configure, and even more client proxy
beans to create.
To make it easier to work with interface clients at scale the Spring Framework provides
dedicated configuration support. It lets applications focus on identifying HTTP Services
by group, and customizing the client for each group, while the framework transparently
creates a registry of client proxies, and declares each proxy as a bean.
An HTTP Service group is simply a set of interfaces that share the same client setup and
HttpServiceProxyFactory instance to create proxies. Typically, that means one group per
host, but you can have more than one group for the same target host in case the
underlying client needs to be configured differently.
One way to declare HTTP Service groups is via @ImportHttpServices annotations in
@Configuration classes as shown below:
@Configuration
@ImportHttpServices(group = "echo", types = {EchoServiceA.class, EchoServiceB.class}) (1)
@ImportHttpServices(group = "greeting", basePackageClasses = GreetServiceA.class) (2)
public class ClientConfig {
}
1
Manually list interfaces for group "echo"
2
Detect interfaces for group "greeting" under a base package
It is also possible to declare groups programmatically by creating an HTTP Service
registrar and then importing it:
public class MyHttpServiceRegistrar extends AbstractHttpServiceRegistrar { (1)

 @Override
 protected void registerHttpServices(GroupRegistry registry, AnnotationMetadata metadata) {
 registry.forGroup("echo").register(EchoServiceA.class, EchoServiceB.class); (2)
 registry.forGroup("greeting").detectInBasePackages(GreetServiceA.class); (3)
 }
}

@Configuration
@Import(MyHttpServiceRegistrar.class) (4)
public class ClientConfig {
}
1
Create extension class of AbstractHttpServiceRegistrar
2
Manually list interfaces for group "echo"
3
Detect interfaces for group "greeting" under a base package
4
Import the registrar
You can mix and match @ImportHttpService annotations with programmatic registrars,
and you can spread the imports across multiple configuration classes. All imports
contribute collaboratively the same, shared HttpServiceProxyRegistry instance.
Once HTTP Service groups are declared, add an HttpServiceGroupConfigurer bean to
customize the client for each group. For example:
@Configuration
@ImportHttpServices(group = "echo", types = {EchoServiceA.class, EchoServiceB.class})
@ImportHttpServices(group = "greeting", basePackageClasses = GreetServiceA.class)
public class ClientConfig {

 @Bean
 public RestClientHttpServiceGroupConfigurer groupConfigurer() {
 return groups -> {
 // configure client for group "echo"
 groups.filterByName("echo").forEachClient((group, clientBuilder) -> ...);

 // configure the clients for all groups
 groups.forEachClient((group, clientBuilder) -> ...);

 // configure client and proxy factory for each group
 groups.forEachGroup((group, clientBuilder, factoryBuilder) -> ...);
 };
 }
}
Spring Boot uses an HttpServiceGroupConfigurer to add support for client properties
by HTTP Service group, Spring Security to add OAuth support, and Spring Cloud to add load
balancing.
As a result of the above, each client proxy is available as a bean that you can
conveniently autowire by type:
@RestController
public class EchoController {

 private final EchoService echoService;

 public EchoController(EchoService echoService) {
 this.echoService = echoService;
 }

 // ...
}
However, if there are multiple client proxies of the same type, e.g. the same interface
in multiple groups, then there is no unique bean of that type, and you cannot autowire by
type only. For such cases, you can work directly with the HttpServiceProxyRegistry that
holds all proxies, and obtain the ones you need by group:
@RestController
public class EchoController {

 private final EchoService echoService1;

 private final EchoService echoService2;

 public EchoController(HttpServiceProxyRegistry registry) {
 this.echoService1 = registry.getClient("echo1", EchoService.class); (1)
 this.echoService2 = registry.getClient("echo2", EchoService.class); (2)
 }

 // ...
}
1
Access the EchoService client proxy for group "echo1"
2
Access the EchoService client proxy for group "echo2"
1. HttpEntity headers and body have to be supplied to the RestClient via headers(Consumer) and body(Object).
2. RequestEntity method, URI, headers and body have to be supplied to the RestClient via method(HttpMethod), uri(URI), headers(Consumer) and body(Object).
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

Source: https://docs.spring.io/spring-framework/reference/integration/rest-clients.html
HTTP status: 200 · extracted bytes: 53397 · sha256: 7a55ab496a5762d2c8e1889b0bdb07fc8e96888b280bd902229b4b2d1b091cb8
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r150`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

REST Clients :: Spring Framework Why Spring Overview Trending Generative AI Cloud Architecture Patterns Microservices Reactive Event Driven Application Types Web Applications Serverless Batch Learn Getting Started Quickstart Guides Academy Courses Get Certified Projects Overview Projects Spring Boot Spring Framework Spring Cloud Spring AI Spring Data Spring Integration Spring Batch Spring Security Foundational Projects Micrometer Reactor Development Tools Spring Tools Spring Initializr Resources Blog Release Calendar Version Mappings Release Highlights Security Advisories GitHub Orgs Spring Projects Spring Cloud Community Overview Events Authors Enterprise Overview Long-term Support Automated Upgrades Governance and Compliance Modern App Development light Spring Framework 7.0.8 Search Overview Core Technologies The IoC Container Introduction to the Spring IoC Container and Beans Container Overview Bean Overview Dependencies Dependency Injection Dependencies and Configuration in Detail Using depends-on Lazy-initialized Beans Autowiring Collaborators Method Injection Bean Scopes Customizing the Nature of a Bean Bean Definition Inheritance Container Extension Points Annotation-based Container Configuration Using @Autowired Fine-tuning Annotation-based Autowiring with @Primary or @Fallback Fine-tuning Annotation-based Autowiring with Qualifiers Using Generics as Autowiring Qualifiers Using CustomAutowireConfigurer Injection with @Resource Using @Value Using @PostConstruct and @PreDestroy Classpath Scanning and Managed Components Using JSR-330 Standard Annotations Java-based Container Configuration Basic Concepts: @Bean and @Configuration Instantiating the Spring Container by Using AnnotationConfigApplicationContext Using the @Bean Annotation Using the @Configuration annotation Composing Java-based Configurations Programmatic Bean Registration Environment Abstraction Registering a LoadTimeWeaver Additional Capabilities of the ApplicationContext The BeanFactory API Resources Validation, Data Binding, and Type Conversion Validation Using Spring’s Validator Interface Data Binding Resolving Error Codes to Error Messages Spring Type Conversion Spring Field Formatting Configuring a Global Date and Time Format Java Bean Validation Spring Expression Language (SpEL) Evaluation Expressions in Bean Definitions Language Reference Literal Expressions Properties, Arrays, Lists, Maps, and Indexers Inline Lists Inline Maps Array Construction Methods Operators Types Constructors Variables Functions Varargs Invocations Bean References Ternary Operator (If-Then-Else) The Elvis Operator Safe Navigation Operator Collection Selection Collection Projection Expression Templating Classes Used in the Examples Aspect Oriented Programming with Spring AOP Concepts Spring AOP Capabilities and Goals AOP Proxies @AspectJ support Enabling @AspectJ Support Declaring an Aspect Declaring a Pointcut Declaring Advice Introductions Aspect Instantiation Models An AOP Example Schema-based AOP Support Choosing which AOP Declaration Style to Use Mixing Aspect Types Proxying Mechanisms Programmatic Creation of @AspectJ Proxies Using AspectJ with Spring Applications Further Resources Spring AOP APIs Pointcut API in Spring Advice API in Spring The Advisor API in Spring Using the ProxyFactoryBean to Create AOP Proxies Concise Proxy Definitions Creating AOP Proxies Programmatically with the ProxyFactory Manipulating Advised Objects Using the "auto-proxy" facility Using TargetSource Implementations Defining New Advice Types Resilience Features Null-safety Data Buffers and Codecs Ahead of Time Optimizations Appendix XML Schemas XML Schema Authoring Application Startup Steps Data Access Transaction Management Advantages of the Spring Framework’s Transaction Support Model Understanding the Spring Framework Transaction Abstraction Synchronizing Resources with Transactions Declarative Transaction Management Understanding the Spring Framework’s Declarative Transaction Implementation Example of Declarative Transaction Implementation Rolling Back a Declarative Transaction Configuring Different Transactional Semantics for Different Beans <tx:advice/> Settings Using @Transactional Transaction Propagation Advising Transactional Operations Using @Transactional with AspectJ Programmatic Transaction Management Choosing Between Programmatic and Declarative Transaction Management Transaction-bound Events Application server-specific integration Solutions to Common Problems Further Resources DAO Support Data Access with JDBC Choosing an Approach for JDBC Database Access Package Hierarchy Using the JDBC Core Classes to Control Basic JDBC Processing and Error Handling Controlling Database Connections JDBC Batch Operations Simplifying JDBC Operations with the SimpleJdbc Classes Modeling JDBC Operations as Java Objects Common Problems with Parameter and Data Value Handling Embedded Database Support Initializing a DataSource Data Access with R2DBC Object Relational Mapping (ORM) Data Access Introduction to ORM with Spring General ORM Integration Considerations Hibernate JPA Marshalling XML by Using Object-XML Mappers Appendix Web on Servlet Stack Spring Web MVC DispatcherServlet Context Hierarchy Special Bean Types Web MVC Config Servlet Config Processing Path Matching Interception Exceptions View Resolution Locale Multipart Resolver Logging Filters HTTP Message Conversion Annotated Controllers Declaration Mapping Requests Handler Methods Method Arguments Return Values Type Conversion Matrix Variables @RequestParam @RequestHeader @CookieValue @ModelAttribute @SessionAttributes @SessionAttribute @RequestAttribute Redirect Attributes Flash Attributes Multipart @RequestBody HttpEntity @ResponseBody ResponseEntity Jackson JSON Model @InitBinder Validation Exceptions Controller Advice Functional Endpoints URI Links Asynchronous Requests Range Requests Data Binding CORS API Versioning Error Responses Web Security HTTP Caching View Technologies Thymeleaf FreeMarker Groovy Markup Script Views HTML Fragments JSP and JSTL RSS and Atom PDF and Excel Jackson XML Marshalling XSLT Views MVC Config Enable MVC Configuration MVC Config API Type Conversion Validation Interceptors Content Types Message Converters View Controllers View Resolvers Static Resources Default Servlet Path Matching API Version Advanced Java Config Advanced XML Config HTTP/2 REST Clients Testing WebSockets WebSocket API SockJS Fallback STOMP Overview Benefits Enable STOMP WebSocket Transport Flow of Messages Annotated Controllers Sending Messages Simple Broker External Broker Connecting to a Broker Dots as Separators Authentication Token Authentication Authorization User Destinations Order of Messages Events Interception STOMP Client WebSocket Scope Performance Monitoring Testing Web on Reactive Stack Spring WebFlux Overview Reactive Core DispatcherHandler Annotated Controllers @Controller Mapping Requests Handler Methods Method Arguments Return Values Type Conversion Matrix Variables @RequestParam @RequestHeader @CookieValue @ModelAttribute @SessionAttributes @SessionAttribute @RequestAttribute Multipart Content @RequestBody HttpEntity @ResponseBody ResponseEntity Jackson JSON Model DataBinder Validation Exceptions Controller Advice Functional Endpoints URI Links Range Requests Data Binding CORS API Versioning Error Responses Web Security HTTP Caching View Technologies WebFlux Config HTTP/2 WebClient Configuration retrieve() Exchange Request Body Filters Attributes Context Synchronous Use Testing HTTP Service Client WebSockets Testing RSocket Reactive Libraries Testing Introduction to Spring Testing Unit Testing Integration Testing JDBC Testing Support Spring TestContext Framework Key Abstractions Bootstrapping the TestContext Framework TestExecutionListener Configuration Application Events Test Execution Events Context Management Context Configuration with Component Classes Context Configuration with XML Resources Context Configuration with Groovy Scripts Default Context Configuration Mixing Component Classes, XML, and Groovy Scripts Context Configuration with Context Customizers Context Configuration with Context Initializers Context Configuration Inheritance Context Configuration with Environment Profiles Context Configuration with Test Property Sources Context Configuration with Dynamic Property Sources Loading a WebApplicationContext Working with Web Mocks Context Caching Context Pausing Context Failure Threshold Context Hierarchies Dependency Injection of Test Fixtures Bean Overriding in Tests Testing Request- and Session-scoped Beans Transaction Management Executing SQL Scripts Parallel Test Execution TestContext Framework Support Classes Ahead of Time Support for Tests WebTestClient RestTestClient MockMvc Overview Setup Options Hamcrest Integration Static Imports Configuring MockMvc Setup Features Performing Requests Defining Expectations Async Requests Streaming Responses Filter Registrations AssertJ Integration Configuring MockMvcTester Performing Requests Defining Expectations MockMvc integration HtmlUnit Integration Why HtmlUnit Integration? MockMvc and HtmlUnit MockMvc and WebDriver MockMvc and Geb MockMvc vs End-to-End Tests Further Examples Testing Client Applications Appendix Annotations Standard Annotation Support Spring Testing Annotations @BootstrapWith @ContextConfiguration @WebAppConfiguration @ContextHierarchy @ContextCustomizerFactories @ActiveProfiles @TestPropertySource @DynamicPropertySource @TestBean @MockitoBean and @MockitoSpyBean @DirtiesContext @TestExecutionListeners @RecordApplicationEvents @Commit @Rollback @BeforeTransaction @AfterTransaction @Sql @SqlConfig @SqlMergeMode @SqlGroup @DisabledInAotMode Spring JUnit 4 Testing Annotations Spring JUnit Jupiter Testing Annotations Meta-Annotation Support for Testing Further Resources Integration REST Clients JMS (Java Message Service) Using Spring JMS Sending a Message Receiving a Message Support for JCA Message Endpoints Annotation-driven Listener Endpoints JMS Namespace Support JMX Exporting Your Beans to JMX Controlling the Management Interface of Your Beans Controlling ObjectName Instances for Your Beans Using JSR-160 Connectors Accessing MBeans through Proxies Notifications Further Resources Email Task Execution and Scheduling Cache Abstraction Understanding the Cache Abstraction Declarative Annotation-based Caching JCache (JSR-107) Annotations Declarative XML-based Caching Configuring the Cache Storage Plugging-in Different Back-end Caches How can I Set the TTL/TTI/Eviction policy/XXX feature? Observability Support JVM AOT Cache JVM Checkpoint Restore Appendix Language Support Kotlin Requirements Extensions Null-safety Classes and Interfaces Annotations Bean Registration DSL Web Coroutines Spring Projects in Kotlin Getting Started Resources Apache Groovy Appendix Java API Kotlin API Wiki Search Edit this Page GitHub Project Stack Overflow Spring Framework Integration REST Clients REST Clients The Spring Framework provides the following choices for making calls to REST endpoints: RestClient — synchronous client with a fluent API WebClient — non-blocking, reactive client with fluent API RestTemplate — synchronous client with template method API, now deprecated in favor of RestClient HTTP Service Clients — annotated interface backed by generated proxy RestClient RestClient is a synchronous HTTP client that provides a fluent API to perform requests. It serves as an abstraction over HTTP libraries, and handles conversion of HTTP request and response content to and from higher level Java objects. Create a RestClient RestClient has static create shortcut methods. It also exposes a builder() with further options: select the HTTP library to use, see Client Request Factories configure message converters, see HTTP Message Conversion set a baseUrl set default request headers, cookies, path variables, API version configure an ApiVersionInserter register interceptors register request initializers Once created, a RestClient is safe to use in multiple threads. The below shows how to create or build a RestClient : Java Kotlin RestClient defaultClient = RestClient.create(); RestClient customClient = RestClient.builder() .requestFactory(new HttpComponentsClientHttpRequestFactory()) .messageConverters(converters -> converters.add(new MyCustomMessageConverter())) .baseUrl("https://example.com") .defaultUriVariables(Map.of("variable", "foo")) .defaultHeader("My-Header", "Foo") .defaultCookie("My-Cookie", "Bar") .defaultVersion("1.2") .apiVersionInserter(ApiVersionInserter.fromHeader("API-Version").build()) .requestInterceptor(myCustomInterceptor) .requestInitializer(myCustomInitializer) .build(); val defaultClient = RestClient.create() val customClient = RestClient.builder() .requestFactory(HttpComponentsClientHttpRequestFactory()) .messageConverters { converters -> converters.add(MyCustomMessageConverter()) } .baseUrl("https://example.com") .defaultUriVariables(mapOf("variable" to "foo")) .defaultHeader("My-Header", "Foo") .defaultCookie("My-Cookie", "Bar") .defaultVersion("1.2") .apiVersionInserter(ApiVersionInserter.fromHeader("API-Version").build()) .requestInterceptor(myCustomInterceptor) .requestInitializer(myCustomInitializer) .build() Use the RestClient To perform an HTTP request, first specify the HTTP method to use. Use the convenience methods like get() , head() , post() , and others, or method(HttpMethod) . Request URL Next, specify the request URI with the uri methods. This is optional, and you can skip this step if you configured a baseUrl through the builder. The URL is typically specified as a String , with optional URI template variables. The following shows how to perform a request: Java Kotlin int id = 42; restClient.get() .uri("https://example.com/orders/{id}", id) // ... val id = 42 restClient.get() .uri("https://example.com/orders/{id}", id) // ... A function can also be used for more controls, such as specifying request parameters . String URLs are encoded by default, but this can be changed by building a client with a custom uriBuilderFactory . The URL can also be provided with a function or as a java.net.URI , both of which are not encoded. For more details on working with and encoding URIs, see URI Links . Request headers and body If necessary, the HTTP request can be manipulated by adding request headers with header(String, String) , headers(Consumer<HttpHeaders> , or with the convenience methods accept(MediaType…​) , acceptCharset(Charset…​) and so on. For HTTP requests that can contain a body ( POST , PUT , and PATCH ), additional methods are available: contentType(MediaType) , and contentLength(long) . You can set an API version for the request if the client is configured with ApiVersionInserter . The request body itself can be set by body(Object) , which internally uses HTTP Message Conversion . Alternatively, the request body can be set using a ParameterizedTypeReference , allowing you to use generics. Finally, the body can be set to a callback function that writes to an OutputStream . Retrieving the response Once the request has been set up, it can be sent by chaining method calls after retrieve() . For example, the response body can be accessed by using retrieve().body(Class) or retrieve().body(ParameterizedTypeReference) for parameterized types like lists. The body method converts the response contents into various types – for instance, bytes can be converted into a String , JSON can be converted into objects using Jackson, and so on (see HTTP Message Conversion ). The response can also be converted into a ResponseEntity , giving access to the response headers as well as the body, with retrieve().toEntity(Class) Calling retrieve() by itself is a no-op and returns a ResponseSpec . Applications must invoke a terminal operation on the ResponseSpec to have any side effect. If consuming the response has no interest for your use case, you can use retrieve().toBodilessEntity() . This sample shows how RestClient can be used to perform a simple GET request. Java Kotlin String result = restClient.get() (1) .uri("https://example.com") (2) .retrieve() (3) .body(String.class); (4) System.out.println(result); (5) 1 Set up a GET request 2 Specify the URL to connect to 3 Retrieve the response 4 Convert the response into a string 5 Print the result val result= restClient.get() (1) .uri("https://example.com") (2) .retrieve() (3) .body<String>() (4) println(result) (5) 1 Set up a GET request 2 Specify the URL to connect to 3 Retrieve the response 4 Convert the response into a string 5 Print the result Access to the response status code and headers is provided through ResponseEntity : Java Kotlin ResponseEntity<String> result = restClient.get() (1) .uri("https://example.com") (1) .retrieve() .toEntity(String.class); (2) System.out.println("Response status: " + result.getStatusCode()); (3) System.out.println("Response headers: " + result.getHeaders()); (3) System.out.println("Contents: " + result.getBody()); (3) 1 Set up a GET request for the specified URL 2 Convert the response into a ResponseEntity 3 Print the result val result = restClient.get() (1) .uri("https://example.com") (1) .retrieve() .toEntity<String>() (2) println("Response status: " + result.statusCode) (3) println("Response headers: " + result.headers) (3) println("Contents: " + result.body) (3) 1 Set up a GET request for the specified URL 2 Convert the response into a ResponseEntity 3 Print the result RestClient can convert JSON to objects, using the Jackson library. Note the usage of URI variables in this sample and that the Accept header is set to JSON. Java Kotlin int id = ...; Pet pet = restClient.get() .uri("https://petclinic.example.com/pets/{id}", id) (1) .accept(APPLICATION_JSON) (2) .retrieve() .body(Pet.class); (3) 1 Using URI variables 2 Set the Accept header to application/json 3 Convert the JSON response into a Pet domain object val id = ... val pet = restClient.get() .uri("https://petclinic.example.com/pets/{id}", id) (1) .accept(APPLICATION_JSON) (2) .retrieve() .body<Pet>() (3) 1 Using URI variables 2 Set the Accept header to application/json 3 Convert the JSON response into a Pet domain object In the next sample, RestClient is used to perform a POST request that contains JSON, which again is converted using Jackson. Java Kotlin Pet pet = ... (1) ResponseEntity<Void> response = restClient.post() (2) .uri("https://petclinic.example.com/pets/new") (2) .contentType(APPLICATION_JSON) (3) .body(pet) (4) .retrieve() .toBodilessEntity(); (5) 1 Create a Pet domain object 2 Set up a POST request, and the URL to connect to 3 Set the Content-Type header to application/json 4 Use pet as the request body 5 Convert the response into a response entity with no body. val pet: Pet = ... (1) val response = restClient.post() (2) .uri("https://petclinic.example.com/pets/new") (2) .contentType(APPLICATION_JSON) (3) .body(pet) (4) .retrieve() .toBodilessEntity() (5) 1 Create a Pet domain object 2 Set up a POST request, and the URL to connect to 3 Set the Content-Type header to application/json 4 Use pet as the request body 5 Convert the response into a response entity with no body. Error handling By default, RestClient throws a subclass of RestClientException when retrieving a response with a 4xx or 5xx status code. This behavior can be overridden using onStatus . Java Kotlin String result = restClient.get() (1) .uri("https://example.com/this-url-does-not-exist") (1) .retrieve() .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> { (2) throw new MyCustomRuntimeException(response.getStatusCode(), response.getHeaders()); (3) }) .body(String.class); 1 Create a GET request for a URL that returns a 404 status code 2 Set up a status handler for all 4xx status codes 3 Throw a custom exception val result = restClient.get() (1) .uri("https://example.com/this-url-does-not-exist") (1) .retrieve() .onStatus(HttpStatusCode::is4xxClientError) { _, response -> (2) throw MyCustomRuntimeException(response.getStatusCode(), response.getHeaders()) } (3) .body<String>() 1 Create a GET request for a URL that returns a 404 status code 2 Set up a status handler for all 4xx status codes 3 Throw a custom exception Exchange For more advanced scenarios, the RestClient gives access to the underlying HTTP request and response through the exchange() method, which can be used instead of retrieve() . Status handlers are not applied when use exchange() , because the exchange function already provides access to the full response, allowing you to perform any error handling necessary. Java Kotlin Pet result = restClient.get() .uri("https://petclinic.example.com/pets/{id}", id) .accept(APPLICATION_JSON) .exchange((request, response) -> { (1) if (response.getStatusCode().is4xxClientError()) { (2) throw new MyCustomRuntimeException(response.getStatusCode(), response.getHeaders()); (2) } else { Pet pet = convertResponse(response); (3) return pet; } }); 1 exchange provides the request and response 2 Throw an exception when the response has a 4xx status code 3 Convert the response into a Pet domain object val result = restClient.get() .uri("https://petclinic.example.com/pets/{id}", id) .accept(MediaType.APPLICATION_JSON) .exchange { request, response -> (1) if (response.getStatusCode().is4xxClientError()) { (2) throw MyCustomRuntimeException(response.getStatusCode(), response.getHeaders()) (2) } else { val pet: Pet = convertResponse(response) (3) pet } } 1 exchange provides the request and response 2 Throw an exception when the response has a 4xx status code 3 Convert the response into a Pet domain object HTTP Message Conversion See the supported HTTP message converters in the dedicated section . Jackson JSON Views To serialize only a subset of the object properties, you can specify a Jackson JSON View , as the following example shows: MappingJacksonValue value = new MappingJacksonValue(new User("eric", "7!jd#h23")); value.setSerializationView(User.WithoutPasswordView.class); ResponseEntity<Void> response = restClient.post() // or RestTemplate.postForEntity .contentType(APPLICATION_JSON) .body(value) .retrieve() .toBodilessEntity(); Multipart To send multipart data, you need to provide a MultiValueMap<String, Object> whose values may be an Object for part content, a Resource for a file part, or an HttpEntity for part content with headers. For example: MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>(); parts.add("fieldPart", "fieldValue"); parts.add("filePart", new FileSystemResource("...logo.png")); parts.add("jsonPart", new Person("Jason")); HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_XML); parts.add("xmlPart", new HttpEntity<>(myBean, headers)); // send using RestClient.post or RestTemplate.postForEntity In most cases, you do not have to specify the Content-Type for each part. The content type is determined automatically based on the HttpMessageConverter chosen to serialize it or, in the case of a Resource , based on the file extension. If necessary, you can explicitly provide the MediaType with an HttpEntity wrapper. Once the MultiValueMap is ready, you can use it as the body of a POST request, using RestClient.post().body(parts) (or RestTemplate.postForObject ). If the MultiValueMap contains at least one non- String value, the Content-Type is set to multipart/form-data by the FormHttpMessageConverter . If the MultiValueMap has String values, the Content-Type defaults to application/x-www-form-urlencoded . If necessary the Content-Type may also be set explicitly. Client Request Factories To execute the HTTP request, RestClient uses a client HTTP library. These libraries are adapted via the ClientRequestFactory interface. Various implementations are available: JdkClientHttpRequestFactory for Java’s HttpClient HttpComponentsClientHttpRequestFactory for use with Apache HTTP Components HttpClient JettyClientHttpRequestFactory for Jetty’s HttpClient ReactorNettyClientRequestFactory for Reactor Netty’s HttpClient SimpleClientHttpRequestFactory as a simple default If no request factory is specified when the RestClient was built, it will use the Apache or Jetty HttpClient if they are available on the classpath. Otherwise, if the java.net.http module is loaded, it will use Java’s HttpClient . Finally, it will resort to the simple default. Note that the SimpleClientHttpRequestFactory may raise an exception when accessing the status of a response that represents an error (for example, 401). If this is an issue, use any of the alternative request factories. WebClient WebClient is a non-blocking, reactive client to perform HTTP requests. It was introduced in 5.0 and offers an alternative to the RestTemplate , with support for synchronous, asynchronous, and streaming scenarios. WebClient supports the following: Non-blocking I/O Reactive Streams back pressure High concurrency with fewer hardware resources Functional-style, fluent API that takes advantage of lambda expressions Synchronous and asynchronous interactions Streaming up to or streaming down from a server See WebClient for more details. RestTemplate The RestTemplate provides a high-level API over HTTP client libraries in the form of a classic Spring Template class. It exposes the following groups of overloaded methods: As of Spring Framework 7.0, RestTemplate is deprecated in favor of RestClient and will be removed in a future version, please use the "Migrating to RestClient" guide. For asynchronous and streaming scenarios, consider the reactive WebClient . Table 1. RestTemplate methods Method group Description getForObject Retrieves a representation via GET. getForEntity Retrieves a ResponseEntity (that is, status, headers, and body) by using GET. headForHeaders Retrieves all headers for a resource by using HEAD. postForLocation Creates a new resource by using POST and returns the Location header from the response. postForObject Creates a new resource by using POST and returns the representation from the response. postForEntity Creates a new resource by using POST and returns the representation from the response. put Creates or updates a resource by using PUT. patchForObject Updates a resource by using PATCH and returns the representation from the response. Note that the JDK HttpURLConnection does not support PATCH , but Apache HttpComponents and others do. delete Deletes the resources at the specified URI by using DELETE. optionsForAllow Retrieves allowed HTTP methods for a resource by using ALLOW. exchange More generalized (and less opinionated) version of the preceding methods that provides extra flexibility when needed. It accepts a RequestEntity (including HTTP method, URL, headers, and body as input) and returns a ResponseEntity . These methods allow the use of ParameterizedTypeReference instead of Class to specify a response type with generics. execute The most generalized way to perform a request, with full control over request preparation and response extraction through callback interfaces. Initialization RestTemplate uses the same HTTP library abstraction as RestClient . By default, it uses the SimpleClientHttpRequestFactory , but this can be changed via the constructor. See Client Request Factories . RestTemplate can be instrumented for observability, in order to produce metrics and traces. See the RestTemplate Observability support section. Body Objects passed into and returned from RestTemplate methods are converted to and from HTTP messages with the help of an HttpMessageConverter , see HTTP Message Conversion . Migrating to RestClient Applications can adopt RestClient in a gradual fashion, first focusing on API usage and then on infrastructure setup. You can consider the following steps: Create one or more RestClient from existing RestTemplate instances, like: RestClient restClient = RestClient.create(restTemplate) . Gradually replace RestTemplate usage in your application, component by component, by focusing first on issuing requests. See the table below for API equivalents. Once all client requests go through RestClient instances, you can now work on replicating your existing RestTemplate instance creations by using RestClient.Builder . Because RestTemplate and RestClient share the same infrastructure, you can reuse custom ClientHttpRequestFactory or ClientHttpRequestInterceptor in your setup. See the RestClient builder API . If no other library is available on the classpath, RestClient will choose the JdkClientHttpRequestFactory powered by the modern JDK HttpClient , whereas RestTemplate would pick the SimpleClientHttpRequestFactory that uses HttpURLConnection . This can explain subtle behavior difference at runtime at the HTTP level. The following table shows RestClient equivalents for RestTemplate methods. Table 2. RestClient equivalents for RestTemplate methods RestTemplate method RestClient equivalent getForObject(String, Class, Object…​) get() .uri(String, Object…​) .retrieve() .body(Class) getForObject(String, Class, Map) get() .uri(String, Map) .retrieve() .body(Class) getForObject(URI, Class) get() .uri(URI) .retrieve() .body(Class) getForEntity(String, Class, Object…​) get() .uri(String, Object…​) .retrieve() .toEntity(Class) getForEntity(String, Class, Map) get() .uri(String, Map) .retrieve() .toEntity(Class) getForEntity(URI, Class) get() .uri(URI) .retrieve() .toEntity(Class) headForHeaders(String, Object…​) head() .uri(String, Object…​) .retrieve() .toBodilessEntity() .getHeaders() headForHeaders(String, Map) head() .uri(String, Map) .retrieve() .toBodilessEntity() .getHeaders() headForHeaders(URI) head() .uri(URI) .retrieve() .toBodilessEntity() .getHeaders() postForLocation(String, Object, Object…​) post() .uri(String, Object…​) .body(Object).retrieve() .toBodilessEntity() .getLocation() postForLocation(String, Object, Map) post() .uri(String, Map) .body(Object) .retrieve() .toBodilessEntity() .getLocation() postForLocation(URI, Object) post() .uri(URI) .body(Object) .retrieve() .toBodilessEntity() .getLocation() postForObject(String, Object, Class, Object…​) post() .uri(String, Object…​) .body(Object) .retrieve() .body(Class) postForObject(String, Object, Class, Map) post() .uri(String, Map) .body(Object) .retrieve() .body(Class) postForObject(URI, Object, Class) post() .uri(URI) .body(Object) .retrieve() .body(Class) postForEntity(String, Object, Class, Object…​) post() .uri(String, Object…​) .body(Object) .retrieve() .toEntity(Class) postForEntity(String, Object, Class, Map) post() .uri(String, Map) .body(Object) .retrieve() .toEntity(Class) postForEntity(URI, Object, Class) post() .uri(URI) .body(Object) .retrieve() .toEntity(Class) put(String, Object, Object…​) put() .uri(String, Object…​) .body(Object) .retrieve() .toBodilessEntity() put(String, Object, Map) put() .uri(String, Map) .body(Object) .retrieve() .toBodilessEntity() put(URI, Object) put() .uri(URI) .body(Object) .retrieve() .toBodilessEntity() patchForObject(String, Object, Class, Object…​) patch() .uri(String, Object…​) .body(Object) .retrieve() .body(Class) patchForObject(String, Object, Class, Map) patch() .uri(String, Map) .body(Object) .retrieve() .body(Class) patchForObject(URI, Object, Class) patch() .uri(URI) .body(Object) .retrieve() .body(Class) delete(String, Object…​) delete() .uri(String, Object…​) .retrieve() .toBodilessEntity() delete(String, Map) delete() .uri(String, Map) .retrieve() .toBodilessEntity() delete(URI) delete() .uri(URI) .retrieve() .toBodilessEntity() optionsForAllow(String, Object…​) options() .uri(String, Object…​) .retrieve() .toBodilessEntity() .getAllow() optionsForAllow(String, Map) options() .uri(String, Map) .retrieve() .toBodilessEntity() .getAllow() optionsForAllow(URI) options() .uri(URI) .retrieve() .toBodilessEntity() .getAllow() exchange(String, HttpMethod, HttpEntity, Class, Object…​) method(HttpMethod) .uri(String, Object…​) .headers(Consumer<HttpHeaders>) .body(Object) .retrieve() .toEntity(Class) [ 1 ] exchange(String, HttpMethod, HttpEntity, Class, Map) method(HttpMethod) .uri(String, Map) .headers(Consumer<HttpHeaders>) .body(Object) .retrieve() .toEntity(Class) [ 1 ] exchange(URI, HttpMethod, HttpEntity, Class) method(HttpMethod) .uri(URI) .headers(Consumer<HttpHeaders>) .body(Object) .retrieve() .toEntity(Class) [ 1 ] exchange(String, HttpMethod, HttpEntity, ParameterizedTypeReference, Object…​) method(HttpMethod) .uri(String, Object…​) .headers(Consumer<HttpHeaders>) .body(Object) .retrieve() .toEntity(ParameterizedTypeReference) [ 1 ] exchange(String, HttpMethod, HttpEntity, ParameterizedTypeReference, Map) method(HttpMethod) .uri(String, Map) .headers(Consumer<HttpHeaders>) .body(Object) .retrieve() .toEntity(ParameterizedTypeReference) [ 1 ] exchange(URI, HttpMethod, HttpEntity, ParameterizedTypeReference) method(HttpMethod) .uri(URI) .headers(Consumer<HttpHeaders>) .body(Object) .retrieve() .toEntity(ParameterizedTypeReference) [ 1 ] exchange(RequestEntity, Class) method(HttpMethod) .uri(URI) .headers(Consumer<HttpHeaders>) .body(Object) .retrieve() .toEntity(Class) [ 2 ] exchange(RequestEntity, ParameterizedTypeReference) method(HttpMethod) .uri(URI) .headers(Consumer<HttpHeaders>) .body(Object) .retrieve() .toEntity(ParameterizedTypeReference) [ 2 ] execute(String, HttpMethod, RequestCallback, ResponseExtractor, Object…​) method(HttpMethod) .uri(String, Object…​) .exchange(ExchangeFunction) execute(String, HttpMethod, RequestCallback, ResponseExtractor, Map) method(HttpMethod) .uri(String, Map) .exchange(ExchangeFunction) execute(URI, HttpMethod, RequestCallback, ResponseExtractor) method(HttpMethod) .uri(URI) .exchange(ExchangeFunction) RestClient and RestTemplate instances share the same behavior when it comes to throwing exceptions (with the RestClientException type being at the top of the hierarchy). When RestTemplate consistently throws HttpClientErrorException for "4xx" response statues, RestClient allows for more flexibility with custom "status handlers" . HTTP Service Clients You can define an HTTP Service as a Java interface with @HttpExchange methods, and use HttpServiceProxyFactory to create a client proxy from it for remote access over HTTP via RestClient , WebClient , or RestTemplate . On the server side, an @Controller class can implement the same interface to handle requests with @HttpExchange controller methods. First, create the Java interface: public interface RepositoryService { @GetExchange("/repos/{owner}/{repo}") Repository getRepository(@PathVariable String owner, @PathVariable String repo); // more HTTP exchange methods... } Optionally, use @HttpExchange at the type level to declare common attributes for all methods: @HttpExchange(url = "/repos/{owner}/{repo}", accept = "application/vnd.github.v3+json") public interface RepositoryService { @GetExchange Repository getRepository(@PathVariable String owner, @PathVariable String repo); @PatchExchange(contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE) void updateRepository(@PathVariable String owner, @PathVariable String repo, @RequestParam String name, @RequestParam String description, @RequestParam String homepage); } Next, configure the client and create the HttpServiceProxyFactory : // Using RestClient... RestClient restClient = RestClient.create("..."); RestClientAdapter adapter = RestClientAdapter.create(restClient); // or WebClient... WebClient webClient = WebClient.create("..."); WebClientAdapter adapter = WebClientAdapter.create(webClient); // or RestTemplate... RestTemplate restTemplate = new RestTemplate(); RestTemplateAdapter adapter = RestTemplateAdapter.create(restTemplate); HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build(); Now, you’re ready to create client proxies: RepositoryService service = factory.createClient(RepositoryService.class); // Use service methods for remote calls... HTTP service clients is a powerful and expressive choice for remote access over HTTP. It allows one team to own the knowledge of how a REST API works, what parts are relevant to a client application, what input and output types to create, what endpoint method signatures are needed, what Javadoc to have, and so on. The resulting Java API guides and is ready to use. Method Parameters @HttpExchange methods support flexible method signatures with the following inputs: Method parameter Description URI Dynamically set the URL for the request, overriding the annotation’s url attribute. UriBuilderFactory Provide a UriBuilderFactory to expand the URI template and URI variables with. In effect, replaces the UriBuilderFactory (and its base URL) of the underlying client. HttpMethod Dynamically set the HTTP method for the request, overriding the annotation’s method attribute @RequestHeader Add a request header or multiple headers. The argument may be a single value, a Collection<?> of values, Map<String, ?> , MultiValueMap<String, ?> . Type conversion is supported for non-String values. Header values are added and do not override already added header values. @PathVariable Add a variable for expand a placeholder in the request URL. The argument may be a Map<String, ?> with multiple variables, or an individual value. Type conversion is supported for non-String values. @RequestAttribute Provide an Object to add as a request attribute. Only supported by RestClient and WebClient . @RequestBody Provide the body of the request either as an Object to be serialized, or a Reactive Streams Publisher such as Mono , Flux , or any other async type supported through the configured ReactiveAdapterRegistry . @RequestParam Add a request parameter or multiple parameters. The argument may be a Map<String, ?> or MultiValueMap<String, ?> with multiple parameters, a Collection<?> of values, or an individual value. Type conversion is supported for non-String values. When "content-type" is set to "application/x-www-form-urlencoded" , request parameters are encoded in the request body. Otherwise, they are added as URL query parameters. @RequestPart Add a request part, which may be a String (form field), Resource (file part), Object (entity to be encoded, for example, as JSON), HttpEntity (part content and headers), a Spring Part , or Reactive Streams Publisher of any of the above. MultipartFile Add a request part from a MultipartFile , typically used in a Spring MVC controller where it represents an uploaded file. @CookieValue Add a cookie or multiple cookies. The argument may be a Map<String, ?> or MultiValueMap<String, ?> with multiple cookies, a Collection<?> of values, or an individual value. Type conversion is supported for non-String values. Method parameters cannot be null unless the required attribute (where available on a parameter annotation) is set to false , or the parameter is marked optional as determined by MethodParameter#isOptional . RestClientAdapter provides additional support for a method parameter of type StreamingHttpOutputMessage.Body that allows sending the request body by writing to an OutputStream . Custom Arguments You can configure a custom HttpServiceArgumentResolver . The example interface below uses a custom Search method parameter type: Java Kotlin public interface RepositoryService { @GetExchange("/repos/search") List<Repository> searchRepository(Search search); } interface RepositoryService { @GetExchange("/repos/search") fun searchRepository(search: Search): List<Repository> } A custom argument resolver could be implemented like this: Java Kotlin static class SearchQueryArgumentResolver implements HttpServiceArgumentResolver { @Override public boolean resolve(Object argument, MethodParameter parameter, HttpRequestValues.Builder requestValues) { if (parameter.getParameterType().equals(Search.class)) { Search search = (Search) argument; requestValues.addRequestParameter("owner", search.owner()); requestValues.addRequestParameter("language", search.language()); requestValues.addRequestParameter("query", search.query()); return true; } return false; } } class SearchQueryArgumentResolver : HttpServiceArgumentResolver { override fun resolve( argument: Any?, parameter: MethodParameter, requestValues: HttpRequestValues.Builder ): Boolean { if (parameter.getParameterType() == Search::class.java) { val search = argument as Search requestValues.addRequestParameter("owner", search.owner) .addRequestParameter("language", search.language) .addRequestParameter("query", search.query) return true } return false } } To configure the custom argument resolver: Java Kotlin RestClient restClient = RestClient.builder().baseUrl("https://api.github.com/").build(); RestClientAdapter adapter = RestClientAdapter.create(restClient); HttpServiceProxyFactory factory = HttpServiceProxyFactory .builderFor(adapter) .customArgumentResolver(new SearchQueryArgumentResolver()) .build(); RepositoryService repositoryService = factory.createClient(RepositoryService.class); Search search = Search.create() .owner("spring-projects") .language("java") .query("rest") .build(); List<Repository> repositories = repositoryService.searchRepository(search); val restClient = RestClient.builder().baseUrl("https://api.github.com/").build() val adapter = RestClientAdapter.create(restClient) val factory = HttpServiceProxyFactory .builderFor(adapter) .customArgumentResolver(SearchQueryArgumentResolver()) .build() val repositoryService = factory.createClient<RepositoryService>(RepositoryService::class.java) val search = Search(owner = "spring-projects", language = "java", query = "rest") val repositories = repositoryService.searchRepository(search) By default, RequestEntity is not supported as a method parameter, instead encouraging the use of more fine-grained method parameters for individual parts of the request. Return Values The supported return values depend on the underlying client. Clients adapted to HttpExchangeAdapter such as RestClient and RestTemplate support synchronous return values: Method return value Description void Perform the given request. HttpHeaders Perform the given request and return the response headers. <T> Perform the given request and decode the response content to the declared return type. ResponseEntity<Void> Perform the given request and return a ResponseEntity with the status and headers. ResponseEntity<T> Perform the given request, decode the response content to the declared return type, and return a ResponseEntity with the status, headers, and the decoded body. Clients adapted to ReactorHttpExchangeAdapter such as WebClient , support all of above as well as reactive variants. The table below shows Reactor types, but you can also use other reactive types that are supported through the ReactiveAdapterRegistry : Method return value Description Mono<Void> Perform the given request, and release the response content, if any. Mono<HttpHeaders> Perform the given request, release the response content, if any, and return the response headers. Mono<T> Perform the given request and decode the response content to the declared return type. Flux<T> Perform the given request and decode the response content to a stream of the declared element type. Mono<ResponseEntity<Void>> Perform the given request, and release the response content, if any, and return a ResponseEntity with the status and headers. Mono<ResponseEntity<T>> Perform the given request, decode the response content to the declared return type, and return a ResponseEntity with the status, headers, and the decoded body. Mono<ResponseEntity<Flux<T>> Perform the given request, decode the response content to a stream of the declared element type, and return a ResponseEntity with the status, headers, and the decoded response body stream. By default, the timeout for synchronous return values with ReactorHttpExchangeAdapter depends on how the underlying HTTP client is configured. You can set a blockTimeout value on the adapter level as well, but we recommend relying on timeout settings of the underlying HTTP client, which operates at a lower level and provides more control. RestClientAdapter provides supports additional support for a return value of type InputStream or ResponseEntity<InputStream> that provides access to the raw response body content. Error Handling To customize error handling for HTTP Service client proxies, you can configure the underlying client as needed. By default, clients raise an exception for 4xx and 5xx HTTP status codes. To customize this, register a response status handler that applies to all responses performed through the client as follows: // For RestClient RestClient restClient = RestClient.builder() .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> ...) .build(); RestClientAdapter adapter = RestClientAdapter.create(restClient); // or for WebClient... WebClient webClient = WebClient.builder() .defaultStatusHandler(HttpStatusCode::isError, resp -> ...) .build(); WebClientAdapter adapter = WebClientAdapter.create(webClient); // or for RestTemplate... RestTemplate restTemplate = new RestTemplate(); restTemplate.setErrorHandler(myErrorHandler); RestTemplateAdapter adapter = RestTemplateAdapter.create(restTemplate); HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build(); For more details and options such as suppressing error status codes, see the reference documentation for each client, as well as the Javadoc of defaultStatusHandler in RestClient.Builder or WebClient.Builder , and the setErrorHandler of RestTemplate . Decorating the Adapter HttpExchangeAdapter and ReactorHttpExchangeAdapter are contracts that decouple HTTP Interface client infrastructure from the details of invoking the underlying client. There are adapter implementations for RestClient , WebClient , and RestTemplate . Occasionally, it may be useful to intercept client invocations through a decorator configurable in the HttpServiceProxyFactory.Builder . For example, you can apply built-in decorators to suppress 404 exceptions and return a ResponseEntity with NOT_FOUND and a null body: // For RestClient HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(restClientAdapter) .exchangeAdapterDecorator(NotFoundRestClientAdapterDecorator::new) .build(); // or for WebClient... HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory.builderFor(webClientAdapter) .exchangeAdapterDecorator(NotFoundWebClientAdapterDecorator::new) .build(); HTTP Service Groups It’s trivial to create client proxies with HttpServiceProxyFactory , but to have them declared as beans leads to repetitive configuration. You may also have multiple target hosts, and therefore multiple clients to configure, and even more client proxy beans to create. To make it easier to work with interface clients at scale the Spring Framework provides dedicated configuration support. It lets applications focus on identifying HTTP Services by group, and customizing the client for each group, while the framework transparently creates a registry of client proxies, and declares each proxy as a bean. An HTTP Service group is simply a set of interfaces that share the same client setup and HttpServiceProxyFactory instance to create proxies. Typically, that means one group per host, but you can have more than one group for the same target host in case the underlying client needs to be configured differently. One way to declare HTTP Service groups is via @ImportHttpServices annotations in @Configuration classes as shown below: @Configuration @ImportHttpServices(group = "echo", types = {EchoServiceA.class, EchoServiceB.class}) (1) @ImportHttpServices(group = "greeting", basePackageClasses = GreetServiceA.class) (2) public class ClientConfig { } 1 Manually list interfaces for group "echo" 2 Detect interfaces for group "greeting" under a base package It is also possible to declare groups programmatically by creating an HTTP Service registrar and then importing it: public class MyHttpServiceRegistrar extends AbstractHttpServiceRegistrar { (1) @Override protected void registerHttpServices(GroupRegistry registry, AnnotationMetadata metadata) { registry.forGroup("echo").register(EchoServiceA.class, EchoServiceB.class); (2) registry.forGroup("greeting").detectInBasePackages(GreetServiceA.class); (3) } } @Configuration @Import(MyHttpServiceRegistrar.class) (4) public class ClientConfig { } 1 Create extension class of AbstractHttpServiceRegistrar 2 Manually list interfaces for group "echo" 3 Detect interfaces for group "greeting" under a base package 4 Import the registrar You can mix and match @ImportHttpService annotations with programmatic registrars, and you can spread the imports across multiple configuration classes. All imports contribute collaboratively the same, shared HttpServiceProxyRegistry instance. Once HTTP Service groups are declared, add an HttpServiceGroupConfigurer bean to customize the client for each group. For example: @Configuration @ImportHttpServices(group = "echo", types = {EchoServiceA.class, EchoServiceB.class}) @ImportHttpServices(group = "greeting", basePackageClasses = GreetServiceA.class) public class ClientConfig { @Bean public RestClientHttpServiceGroupConfigurer groupConfigurer() { return groups -> { // configure client for group "echo" groups.filterByName("echo").forEachClient((group, clientBuilder) -> ...); // configure the clients for all groups groups.forEachClient((group, clientBuilder) -> ...); // configure client and proxy factory for each group groups.forEachGroup((group, clientBuilder, factoryBuilder) -> ...); }; } } Spring Boot uses an HttpServiceGroupConfigurer to add support for client properties by HTTP Service group, Spring Security to add OAuth support, and Spring Cloud to add load balancing. As a result of the above, each client proxy is available as a bean that you can conveniently autowire by type: @RestController public class EchoController { private final EchoService echoService; public EchoController(EchoService echoService) { this.echoService = echoService; } // ... } However, if there are multiple client proxies of the same type, e.g. the same interface in multiple groups, then there is no unique bean of that type, and you cannot autowire by type only. For such cases, you can work directly with the HttpServiceProxyRegistry that holds all proxies, and obtain the ones you need by group: @RestController public class EchoController { private final EchoService echoService1; private final EchoService echoService2; public EchoController(HttpServiceProxyRegistry registry) { this.echoService1 = registry.getClient("echo1", EchoService.class); (1) this.echoService2 = registry.getClient("echo2", EchoService.class); (2) } // ... } 1 Access the EchoService client proxy for group "echo1" 2 Access the EchoService client proxy for group "echo2" 1 . HttpEntity headers and body have to be supplied to the RestClient via headers(Consumer<HttpHeaders>) and body(Object) . 2 . RequestEntity method, URI, headers and body have to be supplied to the RestClient via method(HttpMethod) , uri(URI) , headers(Consumer<HttpHeaders>) and body(Object) . Integration JMS (Java Message Service) Spring Framework Stable 7.0.8 6.2.19 Snapshot 7.1.0-SNAPSHOT 7.0.9-SNAPSHOT 6.2.20-SNAPSHOT Related Spring Documentation Spring Boot Spring Framework Spring Cloud Spring Cloud Build Spring Cloud Bus Spring Cloud Circuit Breaker Spring Cloud Commons Spring Cloud Config Spring Cloud Consul Spring Cloud Contract Spring Cloud Function Spring Cloud Gateway Spring Cloud Kubernetes Spring Cloud Netflix Spring Cloud OpenFeign Spring Cloud Stream Spring Cloud Task Spring Cloud Vault Spring Cloud Zookeeper Spring Data Spring Data Cassandra Spring Data Commons Spring Data Couchbase Spring Data Elasticsearch Spring Data JPA Spring Data KeyValue Spring Data LDAP Spring Data MongoDB Spring Data Neo4j Spring Data Redis Spring Data JDBC & R2DBC Spring Data REST Spring Integration Spring Batch Spring Security Spring Authorization Server Spring LDAP Spring Security Kerberos Spring Session Spring Vault Spring AI Spring AMQP Spring CLI Spring GraphQL Spring for Apache Kafka Spring Modulith Spring for Apache Pulsar Spring Shell All Docs... Copyright © 2005 - Broadcom. All Rights Reserved. The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries. Terms of Use • Privacy • Trademark Guidelines • Thank you • Your California Privacy Rights • Cookie Settings Apache®, Apache Tomcat®, Apache Kafka®, Apache Cassandra™, and Apache Geode™ are trademarks or registered trademarks of the Apache Software Foundation in the United States and/or other countries. Java™, Java™ SE, Java™ EE, and OpenJDK™ are trademarks of Oracle and/or its affiliates. Kubernetes® is a registered trademark of the Linux Foundation in the United States and other countries. Linux® is the registered trademark of Linus Torvalds in the United States and other countries. Windows® and Microsoft® Azure are registered trademarks of Microsoft Corporation. “AWS” and “Amazon Web Services” are trademarks or registered trademarks of Amazon.com Inc. or its affiliates. All other trademarks and copyrights are property of their respective owners and are only mentioned for informative purposes. Other names may be trademarks of their respective owners. Search in all Spring Docs
