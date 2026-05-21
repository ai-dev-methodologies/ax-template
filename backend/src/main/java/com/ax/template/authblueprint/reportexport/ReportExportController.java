package com.ax.template.authblueprint.reportexport;

import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * REST surface for the report-export domain.
 *
 * <p>Trace:
 * <ul>
 *   <li>EXPORT-AUTHZ-001 — SecurityConfig maps {@code /api/exports/**} to authenticated()</li>
 *   <li>EXPORT-AUTHZ-002 / 003 — cross-user lookups return 404 (not 403)</li>
 *   <li>EXPORT-LIFECYCLE-001 — POST returns 202 with PENDING status</li>
 *   <li>EXPORT-LIFECYCLE-002 — GET returns one of the 5 enum values</li>
 *   <li>EXPORT-LIFECYCLE-003 — download against non-COMPLETED job returns 409</li>
 *   <li>EXPORT-FORMAT-002 — unsupported format → 400 with errorCode UNSUPPORTED_FORMAT</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/exports")
public class ReportExportController {

    private final ReportExportService service;

    public ReportExportController(ReportExportService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ExportJobResponse> create(Authentication auth,
                                                    @Valid @RequestBody CreateExportRequest body) {
        ExportJobResponse response = service.createJob(auth.getName(), body);
        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .location(URI.create("/api/exports/" + response.jobId()))
            .body(response);
    }

    @GetMapping
    public ExportJobListResponse list(Authentication auth,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return service.listJobs(auth.getName(), page, size);
    }

    @GetMapping("/{jobId}")
    public ExportJobResponse get(Authentication auth, @PathVariable UUID jobId) {
        return service.getJob(auth.getName(), jobId);
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> delete(Authentication auth, @PathVariable UUID jobId) {
        service.deleteJob(auth.getName(), jobId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{jobId}/download")
    public ResponseEntity<byte[]> download(Authentication auth, @PathVariable UUID jobId) {
        ReportExportService.DownloadPayload p = service.download(auth.getName(), jobId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(p.contentType()));
        headers.setContentDisposition(
            org.springframework.http.ContentDisposition
                .attachment()
                .filename(p.filename())
                .build()
        );
        headers.setContentLength(p.bytes().length);
        return new ResponseEntity<>(p.bytes(), headers, HttpStatus.OK);
    }

    // ── Exception → HTTP mapping ─────────────────────────────────────────────

    @ExceptionHandler(ExportJobNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ExportJobNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "JOB_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(JobNotReadyException.class)
    public ResponseEntity<ProblemDetail> handleNotReady(JobNotReadyException ex) {
        return problem(HttpStatus.CONFLICT, "JOB_NOT_READY", ex.getMessage());
    }

    @ExceptionHandler(UnsupportedFormatException.class)
    public ResponseEntity<ProblemDetail> handleUnsupportedFormat(UnsupportedFormatException ex) {
        return problem(HttpStatus.BAD_REQUEST, "UNSUPPORTED_FORMAT", ex.getMessage());
    }

    @ExceptionHandler(TooManyRowsException.class)
    public ResponseEntity<ProblemDetail> handleTooManyRows(TooManyRowsException ex) {
        return problem(HttpStatus.BAD_REQUEST, "TOO_MANY_ROWS", ex.getMessage());
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setProperty("code", code);
        return ResponseEntity.status(status).body(pd);
    }
}
