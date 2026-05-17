package com.ax.template.authblueprint.practices;

import static io.restassured.RestAssured.given;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("PRACTICES")
@Tag("PRACTICES-API-002")
class ApiNoEntityLeakTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void practices_API_002_responseExposesOnlyDtoFieldsNotEntityFields() {
        Response r = given().when().get("/practices/demo/v1/parents").then().extract().response();
        String body = r.asString();
        // The JPA entity Parent has a `children` field (List<Child>). The DTO ParentResponse
        // collapses it to a single `childCount` integer. The entity field MUST NOT appear
        // in the body — its presence would mean we are leaking the entity directly.
        // We assert this on the negative side because the page may be empty in this test
        // context (no seeded data) and we don't want the test to depend on a populated DB.
        Assertions.assertThat(body)
                .as("Parent entity's `children` collection must not appear in the API body")
                .doesNotContain("\"children\":");

        // Reflection check: the controller method's return type must be Page<ParentResponse>,
        // not Page<Parent>. This is the static guarantee the rule needs even when the page
        // happens to be empty at runtime.
        var listParents = java.util.Arrays.stream(PracticesDemoController.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("listParents"))
                .findFirst().orElseThrow();
        var generic = listParents.getGenericReturnType().getTypeName();
        Assertions.assertThat(generic)
                .as("listParents() must return Page<ParentResponse>, never Page<Parent>")
                .contains("ParentResponse")
                .doesNotContain("Page<com.ax.template.authblueprint.practices.Parent>");
    }
}
