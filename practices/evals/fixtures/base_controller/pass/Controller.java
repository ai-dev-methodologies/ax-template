package fixtures.base_controller.pass;

// FIXTURE: pass
// PATTERN: subclass extends BaseController — PASSES rule PRACTICES-WEB-001
//
// Correct: controller extends BaseController (which carries @RestController),
// declares explicit produces = "application/json" (PRACTICES-WEB-002),
// and returns ResponseEntity<ItemResponse> with ProblemDetail error path
// (PRACTICES-ERR-002).

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Inherits @RestController from BaseController — do NOT repeat @RestController here
@RequestMapping(value = "/api/items", produces = "application/json")
public class Controller extends com.example.app.controllers.BaseController {

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> getItem(@PathVariable Long id,
                                                 jakarta.servlet.http.HttpServletRequest request) {
        if (id <= 0) {
            return ResponseEntity.badRequest()
                    .body(null); // real code: ProblemDetailFactory.badRequest(...)
        }
        return ResponseEntity.ok(new ItemResponse(id, "example item"));
    }

    record ItemResponse(Long id, String name) {}
}
