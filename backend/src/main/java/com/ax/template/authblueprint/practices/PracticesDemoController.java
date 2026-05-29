package com.ax.template.authblueprint.practices;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demo endpoints used by PRACTICES-ERR-*, PRACTICES-VAL-*, and PRACTICES-API-* integration tests.
 * Permitted under SecurityConfig at `/practices/demo/**`. GET endpoints throw exceptions
 * that {@link PracticesProblemDetailAdvice} maps to RFC-7807 ProblemDetail. POST /users
 * exercises Jakarta Bean Validation. GET /v1/parents exercises Pageable + DTO mapping
 * + URI versioning under the /v1/ namespace.
 */
@RestController
@RequestMapping(value = "/practices/demo", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
public class PracticesDemoController {

    private final PracticesDemoService demoService;

    public PracticesDemoController(PracticesDemoService demoService) {
        this.demoService = demoService;
    }

    @GetMapping("/bad")
    public Map<String, String> bad() {
        throw new IllegalArgumentException("invalid input shape");
    }

    @GetMapping("/missing")
    public Map<String, String> missing() {
        throw new NoSuchElementException("user not found");
    }

    @PostMapping("/users")
    public Map<String, String> createUser(@Valid @RequestBody UserCreateRequest req) {
        return Map.of(
                "name", req.name(),
                "email", req.email(),
                "username", req.username()
        );
    }

    @GetMapping("/v1/parents")
    public Page<ParentResponse> listParents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return demoService.listParents(page, size);
    }

    // ─── PRACTICES-PERS-005: soft-delete fixture endpoints ─────────────────────

    /**
     * Create a new SoftDeletedRecord.
     * Used by BaseEntitySoftDeleteIT to verify the @SQLDelete + @Where contract.
     */
    @PostMapping("/soft-deleted-records")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createSoftDeletedRecord(@RequestBody Map<String, String> body) {
        return demoService.createSoftDeletedRecord(body.getOrDefault("label", "test"));
    }

    /**
     * List all active (non-soft-deleted) SoftDeletedRecords.
     * The @Where(clause = "deleted_at IS NULL") on the entity automatically filters deleted rows.
     */
    @GetMapping("/soft-deleted-records")
    public List<Map<String, Object>> listSoftDeletedRecords() {
        return demoService.listSoftDeletedRecords();
    }

    /**
     * Soft-delete a record by ID.
     * Triggers the @SQLDelete UPDATE (sets deleted_at = CURRENT_TIMESTAMP).
     * The row is retained in the database; subsequent findAll() will exclude it.
     */
    @DeleteMapping("/soft-deleted-records/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void softDeleteRecord(@PathVariable UUID id) {
        demoService.softDeleteRecord(id);
    }
}
