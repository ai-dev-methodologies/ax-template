package fixtures.base_controller.fail_no_problemdetail_response;

// FIXTURE: fail_no_problemdetail_response
// EXPECTED_VIOLATION: RAW_STRING_RESPONSE_INSTEAD_OF_PROBLEM_DETAIL
// RULE: error-rfc7807-problem-detail.md (PRACTICES-ERR-002)
//
// This class violates the rule: it returns a raw String error body instead of
// a RFC 7807 ProblemDetail. Clients cannot parse problem.type / title / status
// uniformly when the error payload is freeform.
// A guard scanning for ResponseEntity<String> / Map<String,?> error responses
// will FAIL against this fixture.

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/items", produces = "application/json")
public class Controller {

    @GetMapping("/{id}")
    public ItemResponse getItem(@PathVariable Long id) {
        if (id <= 0) {
            // VIOLATION: raw string error body, not application/problem+json
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id must be positive");
        }
        return new ItemResponse(id, "example");
    }

    record ItemResponse(Long id, String name) {}
}
