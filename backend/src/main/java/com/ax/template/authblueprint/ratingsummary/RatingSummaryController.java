package com.ax.template.authblueprint.ratingsummary;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

import com.ax.template.authblueprint.ratingsummary.RatingSummaryDtos.AddReviewRequest;
import com.ax.template.authblueprint.ratingsummary.RatingSummaryDtos.ReviewResponse;
import com.ax.template.authblueprint.ratingsummary.RatingSummaryDtos.SummaryResponse;
import com.ax.template.authblueprint.ratingsummary.RatingSummaryExceptions.InvalidStarsException;
import com.ax.template.authblueprint.ratingsummary.RatingSummaryExceptions.ReviewNotFoundException;

@RestController
@RequestMapping("/api/rating")
public class RatingSummaryController {

    private final RatingSummaryService service;

    public RatingSummaryController(RatingSummaryService service) {
        this.service = service;
    }

    /** POST /api/rating/reviews — adds a PENDING review; returns 201. */
    @PostMapping("/reviews")
    public ResponseEntity<ReviewResponse> addReview(@Valid @RequestBody AddReviewRequest body) {
        ReviewResponse resp = service.addReview(body.productId(), body.stars());
        return ResponseEntity.status(HttpStatus.CREATED)
            .location(URI.create("/api/rating/reviews/" + resp.id()))
            .body(resp);
    }

    /** POST /api/rating/reviews/{id}/approve — approves a PENDING review; returns 200. */
    @PostMapping("/reviews/{id}/approve")
    public ReviewResponse approve(@PathVariable UUID id) {
        return service.approveReview(id);
    }

    /** POST /api/rating/reviews/{id}/reject — rejects a PENDING review; returns 200. */
    @PostMapping("/reviews/{id}/reject")
    public ReviewResponse reject(@PathVariable UUID id) {
        return service.rejectReview(id);
    }

    /** GET /api/rating/summaries/{productId} — returns the cached aggregate. */
    @GetMapping("/summaries/{productId}")
    public SummaryResponse getSummary(@PathVariable UUID productId) {
        return service.getSummary(productId);
    }

    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ReviewNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(InvalidStarsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidStars(InvalidStarsException ex) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_STARS", ex.getMessage());
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "validation failed");
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setProperty("code", code);
        return ResponseEntity.status(status).body(pd);
    }
}
