package com.ax.template.authblueprint.mececlassification;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * mece-classification-l0 thin controller for the {@link ClassificationScheme} resource. Note: a
 * scheme's {@code residualCategory} is a plain (non-`@NotBlank`) field so the blank case reaches the
 * SERVICE and surfaces as the domain-mandated 422 (MECE-EXHAUSTIVE-002), not a generic 400.
 */
@RestController
public class ClassificationSchemeController {

    public record DeclareReq(@NotBlank @Size(max = 200) String schemeKey, String residualCategory) {}
    public record AddRuleReq(@NotBlank @Size(max = 200) String matchValue, @NotBlank @Size(max = 200) String category) {}

    public record SchemeDto(UUID id, String schemeKey, String residualCategory, Instant createdAt) {
        static SchemeDto of(ClassificationScheme s) {
            return new SchemeDto(s.getId(), s.getSchemeKey(), s.getResidualCategory(), s.getCreatedAt());
        }
    }
    public record RuleDto(UUID id, String schemeKey, String matchValue, String category, Instant createdAt) {
        static RuleDto of(ClassificationRule r) {
            return new RuleDto(r.getId(), r.getSchemeKey(), r.getMatchValue(), r.getCategory(), r.getCreatedAt());
        }
    }

    private final ClassificationSchemeService service;

    public ClassificationSchemeController(ClassificationSchemeService service) {
        this.service = service;
    }

    /** MECE-EXHAUSTIVE-002 — declare a scheme; a blank residual category is 422. */
    @PostMapping("/api/mece/schemes")
    public ResponseEntity<SchemeDto> declare(@Valid @RequestBody DeclareReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(SchemeDto.of(service.declare(req.schemeKey(), req.residualCategory())));
    }

    @PostMapping("/api/mece/schemes/{schemeKey}/rules")
    public ResponseEntity<RuleDto> addRule(@PathVariable String schemeKey, @Valid @RequestBody AddRuleReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(RuleDto.of(service.addRule(schemeKey, req.matchValue(), req.category())));
    }

    @GetMapping("/api/mece/schemes/{schemeKey}")
    public SchemeDto get(@PathVariable String schemeKey) {
        return SchemeDto.of(service.getScheme(schemeKey));
    }

    @GetMapping("/api/mece/schemes/{schemeKey}/rules")
    public List<RuleDto> rules(@PathVariable String schemeKey) {
        return service.rules(schemeKey).stream().map(RuleDto::of).toList();
    }

    @ExceptionHandler(MeceException.class)
    public ResponseEntity<ProblemDetail> handle(MeceException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
