package com.ax.template.authblueprint.caching;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Caching reference controller. The authenticated principal is the tenant (so cache keys are
 * tenant-isolated, CACHE-KEY-001). GET emits an explicit Cache-Control (CACHE-CONTROL-001) + a strong
 * ETag and honours If-None-Match → 304 (CACHE-ETAG-001); POST version-bumps the resource so the next
 * read is fresh (CACHE-INVALIDATION-001). Spec: specs/caching-l0.yaml.
 */
@RestController
@RequestMapping("/api/cache/items")
public class CacheController {

    private final CachedItemService service;

    public CacheController(CachedItemService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> get(
            @PathVariable String id,
            @RequestParam(defaultValue = "private") String visibility,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch,
            Authentication auth) {
        String tenant = auth.getName();
        String body = service.get(tenant, id);
        String etag = EtagSupport.strongEtag(body);
        String cacheControl = cacheControl(visibility);

        if (EtagSupport.matches(ifNoneMatch, etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                .eTag(etag)
                .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                .build();
        }
        return ResponseEntity.ok()
            .eTag(etag)
            .header(HttpHeaders.CACHE_CONTROL, cacheControl)
            .body(body);
    }

    @PostMapping("/{id}")
    public ResponseEntity<Void> mutate(@PathVariable String id, Authentication auth) {
        service.invalidate(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/recomputes")
    public Map<String, Integer> recomputes(@PathVariable String id, Authentication auth) {
        return Map.of("recomputes", service.recomputeCount(auth.getName(), id));
    }

    private static String cacheControl(String visibility) {
        return switch (visibility) {
            case "public" -> "public, max-age=" + CachingConfig.BASE_TTL_SECONDS;
            case "no-store" -> "no-store";
            default -> "private, max-age=" + CachingConfig.BASE_TTL_SECONDS;
        };
    }
}
