package com.ax.template.authblueprint.practices;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    private static final int MAX_PAGE_SIZE = 100;

    private final ParentRepository parents;

    public PracticesDemoController(ParentRepository parents) {
        this.parents = parents;
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
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize);
        return parents.findAll(pageable).map(ParentResponse::from);
    }
}
