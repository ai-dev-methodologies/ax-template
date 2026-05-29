package com.ax.template.authblueprint.crud;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ItemService {

    private static final int MAX_PAGE_SIZE = 50; // max page size enforced

    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(UUID ownerId, int page, int size) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        Page<ItemEntity> result = itemRepository.findByOwnerIdAndDeletedFalse(ownerId, PageRequest.of(page, safeSize));
        return Map.of(
            "content", result.getContent().stream().map(ItemResponse::from).toList(),
            "totalElements", result.getTotalElements(),
            "totalPages", result.getTotalPages(),
            "page", result.getNumber(),
            "size", result.getSize()
        );
    }

    public ItemResponse create(UUID ownerId, String createdBy, CreateItemRequest request) {
        ItemEntity item = new ItemEntity();
        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setOwnerId(ownerId);
        item.setCreatedBy(createdBy);
        return ItemResponse.from(itemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public ItemResponse get(UUID id, UUID ownerId) {
        return itemRepository.findByIdAndOwnerIdAndDeletedFalse(id, ownerId)
            .map(ItemResponse::from)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public ItemResponse update(UUID id, UUID ownerId, String updatedBy, UpdateItemRequest request) {
        ItemEntity item = itemRepository.findByIdAndOwnerIdAndDeletedFalse(id, ownerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (request.getTitle() != null) item.setTitle(request.getTitle());
        if (request.getDescription() != null) item.setDescription(request.getDescription());
        item.setUpdatedBy(updatedBy);
        item.setUpdatedAt(Instant.now());
        return ItemResponse.from(itemRepository.save(item));
    }

    public void delete(UUID id, UUID ownerId) {
        ItemEntity item = itemRepository.findByIdAndOwnerIdAndDeletedFalse(id, ownerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        item.setDeleted(true);
        item.setDeletedAt(Instant.now());
        itemRepository.save(item);
    }
}
