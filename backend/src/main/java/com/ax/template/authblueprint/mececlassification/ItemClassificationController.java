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
 * mece-classification-l0 thin controller for the {@link ItemClassification} resource. Delegates to
 * {@link ItemClassificationService}.
 */
@RestController
public class ItemClassificationController {

    public record ClassifyReq(@NotBlank @Size(max = 200) String category,
                              @NotBlank @Size(max = 200) String actor,
                              @NotBlank @Size(max = 500) String reason) {}
    public record ClassifyByAttributeReq(@NotBlank @Size(max = 200) String attributeValue,
                                        @NotBlank @Size(max = 200) String actor,
                                        @NotBlank @Size(max = 500) String reason) {}
    public record ReclassifyReq(@NotBlank @Size(max = 200) String category,
                                @NotBlank @Size(max = 200) String actor,
                                @NotBlank @Size(max = 500) String reason) {}

    public record ClassificationDto(UUID id, String schemeKey, String itemRef, String currentCategory, Instant createdAt) {}
    public record MoveDto(UUID id, String fromCategory, String toCategory, String actor, String reason, Instant movedAt) {
        static MoveDto of(ClassificationMove m) {
            return new MoveDto(m.getId(), m.getFromCategory(), m.getToCategory(), m.getActor(), m.getReason(), m.getMovedAt());
        }
    }

    private final ItemClassificationService service;

    public ItemClassificationController(ItemClassificationService service) {
        this.service = service;
    }

    /** MECE-EXCLUSIVE-001 — the FIRST assignment; a second attempt for the same (scheme, item) is 409. */
    @PostMapping("/api/mece/schemes/{schemeKey}/items/{itemRef}/classify")
    public ResponseEntity<ClassificationDto> classify(@PathVariable String schemeKey, @PathVariable String itemRef,
                                                       @Valid @RequestBody ClassifyReq req) {
        ItemClassification ic = service.classify(schemeKey, itemRef, req.category(), req.actor(), req.reason());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(ic));
    }

    /** MECE-EXHAUSTIVE-002 — rule-based classify; no rule match falls through to the residual category. */
    @PostMapping("/api/mece/schemes/{schemeKey}/items/{itemRef}/classify-by-attribute")
    public ResponseEntity<ClassificationDto> classifyByAttribute(@PathVariable String schemeKey, @PathVariable String itemRef,
                                                                  @Valid @RequestBody ClassifyByAttributeReq req) {
        ItemClassification ic = service.classifyByAttribute(schemeKey, itemRef, req.attributeValue(), req.actor(), req.reason());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(ic));
    }

    /** MECE-RECLASS-003 — append a move; the FROM is derived from the latest move. */
    @PostMapping("/api/mece/schemes/{schemeKey}/items/{itemRef}/reclassify")
    public ClassificationDto reclassify(@PathVariable String schemeKey, @PathVariable String itemRef,
                                        @Valid @RequestBody ReclassifyReq req) {
        return toDto(service.reclassify(schemeKey, itemRef, req.category(), req.actor(), req.reason()));
    }

    @GetMapping("/api/mece/schemes/{schemeKey}/items/{itemRef}")
    public ClassificationDto get(@PathVariable String schemeKey, @PathVariable String itemRef) {
        String current = service.currentCategory(schemeKey, itemRef);
        return new ClassificationDto(null, schemeKey, itemRef, current, null);
    }

    @GetMapping("/api/mece/schemes/{schemeKey}/items/{itemRef}/history")
    public List<MoveDto> history(@PathVariable String schemeKey, @PathVariable String itemRef) {
        return service.history(schemeKey, itemRef).stream().map(MoveDto::of).toList();
    }

    /** MECE-EXHAUSTIVE-002 — the residual (or any) category's current population, visibly queryable. */
    @GetMapping("/api/mece/schemes/{schemeKey}/categories/{category}/count")
    public long count(@PathVariable String schemeKey, @PathVariable String category) {
        return service.countInCategory(schemeKey, category);
    }

    private ClassificationDto toDto(ItemClassification ic) {
        return new ClassificationDto(ic.getId(), ic.getSchemeKey(), ic.getItemRef(),
            service.currentCategory(ic.getSchemeKey(), ic.getItemRef()), ic.getCreatedAt());
    }

    @ExceptionHandler(MeceException.class)
    public ResponseEntity<ProblemDetail> handle(MeceException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
