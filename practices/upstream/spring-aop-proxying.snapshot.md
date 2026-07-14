---
snapshot_id: spring-aop-proxying
source: "https://docs.spring.io/spring-framework/reference/core/aop/proxying.html"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 3
bytes: 9786
sha: "eb561e06bdd7707749b1588662679165c044fdc690d3ff3676a03a388a0bb910"
---

# spring aop proxying — upstream snapshot

Source: https://docs.spring.io/spring-framework/reference/core/aop/proxying.html
Fetched: 2026-07-14

Proxying Mechanisms :: Spring Framework
Edit this Page
 
 
 
 GitHub Project
 
 
 
 Stack Overflow

# Proxying Mechanisms
Spring AOP uses either JDK dynamic proxies or CGLIB to create the proxy for a given
target object. JDK dynamic proxies are built into the JDK, whereas CGLIB is a common
open-source class definition library (repackaged into spring-core).
If the target object to be proxied implements at least one interface, a JDK dynamic
proxy is used, and all of the interfaces implemented by the target type are proxied.
If the target object does not implement any interfaces, a CGLIB proxy is created which
is a runtime-generated subclass of the target type.
If you want to force the use of CGLIB proxying (for example, to proxy every method
defined for the target object, not only those implemented by its interfaces),
you can do so. However, you should consider the following issues:
final classes cannot be proxied, because they cannot be extended.
final methods cannot be advised, because they cannot be overridden.
private methods cannot be advised, because they cannot be overridden.
Methods that are not visible – for example, package-private methods in a parent class
from a different package – cannot be advised because they are effectively private.
The constructor of your proxied object will not be called twice, since the CGLIB proxy
instance is created through Objenesis. However, if your JVM does not allow for
constructor bypassing, you might see double invocations and corresponding debug log
entries from Spring’s AOP support.
Your CGLIB proxy usage may face limitations with the Java Module System. As a typical
case, you cannot create a CGLIB proxy for a class from the java.lang package when
deploying on the module path. Such cases require a JVM bootstrap flag
--add-opens=java.base/java.lang=ALL-UNNAMED which is not available for modules.

## Forcing Specific AOP Proxy Types
To force the use of CGLIB proxies, set the value of the proxy-target-class attribute
of the element to true, as follows:
To force CGLIB proxying when you use the @AspectJ auto-proxy support, set the
proxy-target-class attribute of the element to true,
as follows:
Multiple sections are collapsed into a single unified auto-proxy creator
at runtime, which applies the strongest proxy settings that any of the
 sections (typically from different XML bean definition files) specified.
This also applies to the and 
elements.
To be clear, using proxy-target-class="true" on ,
, or elements forces the use of CGLIB
proxies for all three of them.
@EnableAspectJAutoProxy, @EnableTransactionManagement and related configuration
annotations offer a corresponding proxyTargetClass attribute. These are collapsed
into a single unified auto-proxy creator too, effectively applying the strongest
proxy settings at runtime. As of 7.0, this applies to individual proxy processors
as well, for example @EnableAsync, consistently participating in unified global
default settings for all auto-proxying attempts in a given application.
The global default proxy type may differ between setups. While the core framework
suggests interface-based proxies by default, Spring Boot may - depending on
configuration properties - enable class-based proxies by default.
As of 7.0, forcing a specific proxy type for individual beans is possible through
the @Proxyable annotation on a given @Bean method or @Component class, with
@Proxyable(INTERFACES) or @Proxyable(TARGET_CLASS) overriding any globally
configured default. For very specific purposes, you may even specify the proxy
interface(s) to use through @Proxyable(interfaces=…​), limiting the exposure
to selected interfaces rather than all interfaces that the target bean implements.

## Understanding AOP Proxies
Spring AOP is proxy-based. It is vitally important that you grasp the semantics of
what that last statement actually means before you write your own aspects or use any of
the Spring AOP-based aspects supplied with the Spring Framework.
Consider first the scenario where you have a plain-vanilla, un-proxied object reference,
as the following code snippet shows:
Java
Kotlin
public class SimplePojo implements Pojo {

 public void foo() {
 // this next method invocation is a direct call on the 'this' reference
 this.bar();
 }

 public void bar() {
 // some logic...
 }
}
class SimplePojo : Pojo {

 fun foo() {
 // this next method invocation is a direct call on the 'this' reference
 this.bar()
 }

 fun bar() {
 // some logic...
 }
}
If you invoke a method on an object reference, the method is invoked directly on
that object reference, as the following image and listing show:
Java
Kotlin
public class Main {

 public static void main(String[] args) {
 Pojo pojo = new SimplePojo();
 // this is a direct method call on the 'pojo' reference
 pojo.foo();
 }
}
fun main() {
 val pojo = SimplePojo()
 // this is a direct method call on the 'pojo' reference
 pojo.foo()
}
Things change slightly when the reference that client code has is a proxy. Consider the
following diagram and code snippet:
Java
Kotlin
public class Main {

 public static void main(String[] args) {
 ProxyFactory factory = new ProxyFactory(new SimplePojo());
 factory.addInterface(Pojo.class);
 factory.addAdvice(new RetryAdvice());

 Pojo pojo = (Pojo) factory.getProxy();
 // this is a method call on the proxy!
 pojo.foo();
 }
}
fun main() {
 val factory = ProxyFactory(SimplePojo())
 factory.addInterface(Pojo::class.java)
 factory.addAdvice(RetryAdvice())

 val pojo = factory.proxy as Pojo
 // this is a method call on the proxy!
 pojo.foo()
}
The key thing to understand here is that the client code inside the main(..) method
of the Main class has a reference to the proxy. This means that method calls on that
object reference are calls on the proxy. As a result, the proxy can delegate to all of
the interceptors (advice) that are relevant to that particular method call. However,
once the call has finally reached the target object (the SimplePojo reference in
this case), any method calls that it may make on itself, such as this.bar() or
this.foo(), are going to be invoked against the this reference, and not the proxy.
This has important implications. It means that self invocation is not going to result
in the advice associated with a method invocation getting a chance to run. In other words,
self invocation via an explicit or implicit this reference will bypass the advice.
To address that, you have the following options.
Avoid self invocation
The best approach (the term "best" is used loosely here) is to refactor your code such
that the self invocation does not happen. This does entail some work on your part, but
it is the best, least-invasive approach.
Inject a self reference
An alternative approach is to make use of
self injection,
and invoke methods on the proxy via the self reference instead of via this.
Use AopContext.currentProxy()
This last approach is highly discouraged, and we hesitate to point it out, in favor of
the previous options. However, as a last resort you can choose to tie the logic within
your class to Spring AOP, as the following example shows.
Java
Kotlin
public class SimplePojo implements Pojo {

 public void foo() {
 // This works, but it should be avoided if possible.
 ((Pojo) AopContext.currentProxy()).bar();
 }

 public void bar() {
 // some logic...
 }
}
class SimplePojo : Pojo {

 fun foo() {
 // This works, but it should be avoided if possible.
 (AopContext.currentProxy() as Pojo).bar()
 }

 fun bar() {
 // some logic...
 }
}
The use of AopContext.currentProxy() totally couples your code to Spring AOP, and it
makes the class itself aware of the fact that it is being used in an AOP context, which
reduces some of the benefits of AOP. It also requires that the ProxyFactory is
configured to expose the proxy, as the following example shows:
Java
Kotlin
public class Main {

 public static void main(String[] args) {
 ProxyFactory factory = new ProxyFactory(new SimplePojo());
 factory.addInterface(Pojo.class);
 factory.addAdvice(new RetryAdvice());
 factory.setExposeProxy(true);

 Pojo pojo = (Pojo) factory.getProxy();
 // this is a method call on the proxy!
 pojo.foo();
 }
}
fun main() {
 val factory = ProxyFactory(SimplePojo())
 factory.addInterface(Pojo::class.java)
 factory.addAdvice(RetryAdvice())
 factory.isExposeProxy = true

 val pojo = factory.proxy as Pojo
 // this is a method call on the proxy!
 pojo.foo()
}
AspectJ compile-time weaving and load-time weaving do not have this self-invocation
issue because they apply advice within the bytecode instead of via a proxy.
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
