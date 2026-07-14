---
title: Use RestAssured + @LocalServerPort for practice tests, not MockMvc
impact: MEDIUM
impactDescription: "Black-box HTTP keeps tests portable across implementations"
tags:
  - testing
  - rest-assured
  - portability
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-TEST-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-TEST-001
upstream:
  - "https://rest-assured.io/"
  - "https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#features.testing.spring-boot-applications"
evidence:
  - upstream_id: rest-assured-usage
    section: REST-assured given/when/then DSL
    quote: 'given(). config(RestAssured.config().jsonConfig(jsonConfig().numberReturnType(BIG_DECIMAL))). when(). get("/price"). then().'
  - upstream_id: spring-boot-testing
    section: WebEnvironment.RANDOM_PORT and @LocalServerPort
    quote: "The @LocalServerPort annotation can be used to inject the actual port used into your test. Tests that need to make REST calls to the started server can autowire a RestTestClient"
  - source_type: external
    citation: 'REST-assured — Usage Guide'
    url: 'https://github.com/rest-assured/rest-assured/wiki/Usage'
  - source_type: external
    citation: 'Spring Boot Reference — §Testing: WebEnvironment.RANDOM_PORT + @LocalServerPort'
    url: 'https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html#testing.spring-boot-applications.with-running-server'
---

## Use RestAssured + @LocalServerPort for practice tests, not MockMvc

**Impact: MEDIUM — Black-box HTTP keeps tests portable across implementations**

MockMvc couples a test to Spring's internal dispatcher and its bean configuration. The same test cannot run against a different implementation of the same contract — it ties verification to the framework's internals. RestAssured against `@LocalServerPort` exercises the real HTTP stack: filter chain, serialization, headers, status codes. The same test JAR is portable to any conforming implementation.

**Incorrect — MockMvc binds the test to the dispatcher servlet:**

```java
@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired MockMvc mvc;

    @Test void getUser() throws Exception {
        mvc.perform(get("/users/1")).andExpect(status().isOk());
    }
}
```

**Correct — RestAssured against the real HTTP server:**

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserApiTest {
    @LocalServerPort int port;

    @BeforeEach void setup() { RestAssured.port = port; }

    @Test void getUser() {
        given().when().get("/users/1").then().statusCode(200);
    }
}
```

Verification: `./gradlew testPractices --tests "*RestAssured*"` hits `/actuator/health` over a real port and asserts the test class itself contains no MockMvc references.

Reference: [RestAssured](https://rest-assured.io/) · [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#features.testing.spring-boot-applications)
