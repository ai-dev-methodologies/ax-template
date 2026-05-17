package fixtures.base_controller.fail_missing_restcontroller;

// FIXTURE: fail_missing_restcontroller
// EXPECTED_VIOLATION: BASE_CONTROLLER_MISSING_ANNOTATION
// RULE: web-rest-controller-annotation.md (PRACTICES-WEB-001)
//
// This class violates the rule: it handles HTTP requests but lacks @RestController.
// Bare @Controller treats the return value as a view name → silent 404 for DTO returns.
// An archunit rule asserting @RestController on BaseController subclasses will FAIL
// against this fixture.

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
// NOTE: @RestController is intentionally absent — this is the FAILING case.

@RequestMapping("/api/items")
public class Controller {

    @GetMapping
    public String list() {
        return "items";  // Would be treated as view name, not JSON body
    }
}
