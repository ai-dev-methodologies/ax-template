package com.ax.template.authblueprint.activityfeed;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import com.ax.template.authblueprint.activityfeed.ActivityDtos.ActivityEventResponse;
import com.ax.template.authblueprint.activityfeed.ActivityDtos.ActivityFeedResponse;
import com.ax.template.authblueprint.activityfeed.ActivityDtos.MarkAllReadResponse;
import com.ax.template.authblueprint.activityfeed.ActivityDtos.PublishActivityRequest;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityService service;

    public ActivityController(ActivityService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ActivityEventResponse> publish(Authentication auth,
                                                         @Valid @RequestBody PublishActivityRequest body) {
        ActivityService.PublishResult result = service.publish(auth.getName(), body);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.response());
    }

    @GetMapping
    public ActivityFeedResponse list(Authentication auth,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return service.list(auth.getName(), page, size, unreadOnly);
    }

    @GetMapping("/{id}")
    public ActivityEventResponse get(Authentication auth, @PathVariable UUID id) {
        return service.get(auth.getName(), id);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(Authentication auth, @PathVariable UUID id) {
        service.markRead(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mark-all-read")
    public MarkAllReadResponse markAllRead(Authentication auth) {
        return service.markAllRead(auth.getName());
    }

    @ExceptionHandler(ActivityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ActivityNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setProperty("code", "ACTIVITY_NOT_FOUND");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "validation failed");
        pd.setProperty("code", "VALIDATION_ERROR");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }
}
