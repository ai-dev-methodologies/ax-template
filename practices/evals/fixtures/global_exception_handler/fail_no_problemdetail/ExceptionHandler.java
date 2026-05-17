package fixtures.global_exception_handler.fail_no_problemdetail;

// FIXTURE: fail_no_problemdetail
// EXPECTED_VIOLATION: EXCEPTION_HANDLER_RETURNS_MAP_NOT_PROBLEM_DETAIL
// RULE: error-rfc7807-problem-detail.md (PRACTICES-ERR-002)
//       error-controller-advice.md (PRACTICES-ERR-001)
//
// This handler violates both rules:
// 1. It returns Map<String, String> instead of ProblemDetail (RFC 7807).
// 2. The status code is embedded via @ResponseStatus and not expressed
//    uniformly through ProblemDetail.status.
// A guard asserting @ExceptionHandler returns ResponseEntity<ProblemDetail>
// will FAIL against this fixture.

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class ExceptionHandler {

    // VIOLATION: returns Map — not application/problem+json
    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgument(IllegalArgumentException ex) {
        return Map.of("error", ex.getMessage());
    }

    // VIOLATION: generic Exception swallowed, no traceId, no type URI
    @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleGeneric(Exception ex) {
        return Map.of("error", "Internal server error");
    }
}
