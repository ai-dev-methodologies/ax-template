package com.ax.template.authblueprint.crud;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemRepository itemRepository;

    public ItemController(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = UUID.fromString(jwt.getSubject());
        int safeSize = Math.min(size, 50); // max page size enforced
        Page<ItemEntity> result = itemRepository.findByOwnerIdAndDeletedFalse(ownerId, PageRequest.of(page, safeSize));
        return Map.of(
            "content", result.getContent().stream().map(ItemResponse::from).toList(),
            "totalElements", result.getTotalElements(),
            "totalPages", result.getTotalPages(),
            "page", result.getNumber(),
            "size", result.getSize()
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse create(@Valid @RequestBody CreateItemRequest request, @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = UUID.fromString(jwt.getSubject());
        ItemEntity item = new ItemEntity();
        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setOwnerId(ownerId);
        item.setCreatedBy(jwt.getClaimAsString("email"));
        return ItemResponse.from(itemRepository.save(item));
    }

    @GetMapping("/{id}")
    public ItemResponse get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = UUID.fromString(jwt.getSubject());
        return itemRepository.findByIdAndOwnerIdAndDeletedFalse(id, ownerId)
            .map(ItemResponse::from)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public ItemResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateItemRequest request, @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = UUID.fromString(jwt.getSubject());
        ItemEntity item = itemRepository.findByIdAndOwnerIdAndDeletedFalse(id, ownerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (request.getTitle() != null) item.setTitle(request.getTitle());
        if (request.getDescription() != null) item.setDescription(request.getDescription());
        item.setUpdatedBy(jwt.getClaimAsString("email"));
        item.setUpdatedAt(Instant.now());
        return ItemResponse.from(itemRepository.save(item));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = UUID.fromString(jwt.getSubject());
        ItemEntity item = itemRepository.findByIdAndOwnerIdAndDeletedFalse(id, ownerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        item.setDeleted(true);
        item.setDeletedAt(Instant.now());
        itemRepository.save(item);
    }
}
