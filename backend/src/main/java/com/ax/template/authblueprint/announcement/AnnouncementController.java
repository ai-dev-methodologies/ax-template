package com.ax.template.authblueprint.announcement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
 * announcement-l0 thin controller. Admin write paths live under /api/admin/announcements
 * (SecurityConfig gates /api/admin/** to ROLE_ADMIN — ANN-AUTHZ-001); the active read is
 * /api/announcements/active (any authenticated user). Domain errors -> RFC 9457 ProblemDetail
 * via the single @ExceptionHandler; @Valid bean-validation 400s are handled by
 * common/GlobalProblemDetailAdvice. The author is derived from Authentication.getName().
 */
@RestController
public class AnnouncementController {

    public record CreateRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 5000) String body,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt) {}

    public record AnnouncementDto(UUID id, String title, String body, AnnouncementState state,
                                  Instant startsAt, Instant endsAt, String createdBy, Instant createdAt) {
        static AnnouncementDto of(Announcement a) {
            return new AnnouncementDto(a.getId(), a.getTitle(), a.getBody(), a.getState(),
                a.getStartsAt(), a.getEndsAt(), a.getCreatedBy(), a.getCreatedAt());
        }
    }

    private final AnnouncementService service;

    public AnnouncementController(AnnouncementService service) {
        this.service = service;
    }

    @PostMapping("/api/admin/announcements")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")   // defense-in-depth backstop (SecurityConfig /api/admin/** also gates)
    public ResponseEntity<AnnouncementDto> create(@Valid @RequestBody CreateRequest req, Authentication auth) {
        Announcement a = service.create(auth.getName(), req.title(), req.body(), req.startsAt(), req.endsAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(AnnouncementDto.of(a));
    }

    @PostMapping("/api/admin/announcements/{id}/publish")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")   // defense-in-depth backstop (SecurityConfig /api/admin/** also gates)
    public ResponseEntity<AnnouncementDto> publish(@PathVariable UUID id) {
        return ResponseEntity.ok(AnnouncementDto.of(service.publish(id)));
    }

    @PostMapping("/api/admin/announcements/{id}/archive")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")   // defense-in-depth backstop (SecurityConfig /api/admin/** also gates)
    public ResponseEntity<AnnouncementDto> archive(@PathVariable UUID id) {
        return ResponseEntity.ok(AnnouncementDto.of(service.archive(id)));
    }

    @GetMapping("/api/admin/announcements")
    public List<AnnouncementDto> listAll() {
        return service.listAll().stream().map(AnnouncementDto::of).toList();
    }

    @GetMapping("/api/admin/announcements/{id}")
    public AnnouncementDto getOne(@PathVariable UUID id) {
        return AnnouncementDto.of(service.get(id));   // unknown id -> 404 (ANN-AUTHZ-001)
    }

    @GetMapping("/api/announcements/active")
    public List<AnnouncementDto> listActive() {
        return service.listActive().stream().map(AnnouncementDto::of).toList();
    }

    // ── domain exception -> RFC 9457 ProblemDetail ────────────────────────────
    @ExceptionHandler(AnnouncementException.class)
    public ResponseEntity<ProblemDetail> handle(AnnouncementException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
