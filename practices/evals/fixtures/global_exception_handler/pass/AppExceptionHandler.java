package fixtures.global_exception_handler.pass;

// FIXTURE: pass
// PATTERN: @RestControllerAdvice extending GlobalExceptionHandler returns ProblemDetail
//          PASSES PRACTICES-ERR-001, PRACTICES-ERR-002

import com.example.app.error.GlobalExceptionHandler;
import com.example.app.error.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// CORRECT: extends GlobalExceptionHandler, returns ProblemDetail for domain exceptions
@RestControllerAdvice
public class AppExceptionHandler extends GlobalExceptionHandler {

    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleItemNotFound(
            ItemNotFoundException ex,
            HttpServletRequest request) {
        // CORRECT: uses ProblemDetailFactory → application/problem+json response
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ProblemDetailFactory.notFound(ex.getMessage(), request));
    }

    static class ItemNotFoundException extends RuntimeException {
        ItemNotFoundException(String msg) { super(msg); }
    }
}
