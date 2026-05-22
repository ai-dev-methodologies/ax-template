package com.ax.template.authblueprint.favoritesbookmarks;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
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

import com.ax.template.authblueprint.favoritesbookmarks.FavoriteDtos.AddFavoriteRequest;
import com.ax.template.authblueprint.favoritesbookmarks.FavoriteDtos.CheckFavoriteResponse;
import com.ax.template.authblueprint.favoritesbookmarks.FavoriteDtos.CountFavoriteResponse;
import com.ax.template.authblueprint.favoritesbookmarks.FavoriteDtos.FavoriteListResponse;
import com.ax.template.authblueprint.favoritesbookmarks.FavoriteDtos.FavoriteResponse;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService service;

    public FavoriteController(FavoriteService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<FavoriteResponse> add(Authentication auth,
                                                @Valid @RequestBody AddFavoriteRequest body) {
        FavoriteService.AddResult result = service.add(auth.getName(), body);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.response());
    }

    @GetMapping
    public FavoriteListResponse list(Authentication auth,
                                     @RequestParam(required = false) String entityType) {
        return service.list(auth.getName(), entityType);
    }

    @DeleteMapping("/{entityType}/{entityId}")
    public ResponseEntity<Void> remove(Authentication auth,
                                        @PathVariable String entityType,
                                        @PathVariable String entityId) {
        service.remove(auth.getName(), entityType, entityId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check/{entityType}/{entityId}")
    public CheckFavoriteResponse check(Authentication auth,
                                       @PathVariable String entityType,
                                       @PathVariable String entityId) {
        return new CheckFavoriteResponse(service.isFavorited(auth.getName(), entityType, entityId));
    }

    @GetMapping("/count/{entityType}/{entityId}")
    public CountFavoriteResponse count(@PathVariable String entityType,
                                       @PathVariable String entityId) {
        return new CountFavoriteResponse(service.count(entityType, entityId));
    }

    @ExceptionHandler(FavoritesQuotaExceededException.class)
    public ResponseEntity<ProblemDetail> handleQuota(FavoritesQuotaExceededException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setProperty("code", "FAVORITES_QUOTA_EXCEEDED");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    /**
     * Spring Boot's DefaultHandlerExceptionResolver maps {@code MethodArgumentNotValidException}
     * to 400, but the response can be re-classified to 403 by upstream filters when the JWT
     * filter chain re-enters the dispatch on resolved exceptions. Explicit handler here pins
     * the 400 status and serializes a stable error code for FAV-VALID-001 / FAV-VALID-003.
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "validation failed");
        pd.setProperty("code", "VALIDATION_ERROR");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }
}
