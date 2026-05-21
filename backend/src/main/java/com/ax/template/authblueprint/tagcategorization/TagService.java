package com.ax.template.authblueprint.tagcategorization;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import com.ax.template.authblueprint.tagcategorization.TagDtos.AttachTagRequest;
import com.ax.template.authblueprint.tagcategorization.TagDtos.CreateTagRequest;
import com.ax.template.authblueprint.tagcategorization.TagDtos.TagAttachmentResponse;
import com.ax.template.authblueprint.tagcategorization.TagDtos.TagListResponse;
import com.ax.template.authblueprint.tagcategorization.TagDtos.TagResponse;
import com.ax.template.authblueprint.tagcategorization.TagDtos.UpdateTagRequest;

/**
 * Orchestration for the tag-categorization domain.
 *
 * <p>Trace:
 * <ul>
 *   <li>TAG-CRUD-001..004 — slug generation, list, update name+color only, cascade delete</li>
 *   <li>TAG-ATTACH-001..003 — idempotent attach/detach + ordered by-entity listing</li>
 *   <li>TAG-HIER-001..003 — parent immutable, missing parent rejected, cascade query param</li>
 * </ul>
 */
@Service
public class TagService {

    private final TagRepository tagRepository;
    private final TagAttachmentRepository attachmentRepository;

    public TagService(TagRepository tagRepository, TagAttachmentRepository attachmentRepository) {
        this.tagRepository = tagRepository;
        this.attachmentRepository = attachmentRepository;
    }

    @Transactional
    public TagResponse create(String callerUserId, CreateTagRequest request) {
        if (request.parentTagId() != null) {
            tagRepository.findById(request.parentTagId())
                .orElseThrow(() -> new ParentTagNotFoundException(request.parentTagId()));
        }
        String slug = TagSlugger.slugify(request.name());
        if (tagRepository.findBySlug(slug).isPresent()) {
            throw new DuplicateSlugException("slug already exists: " + slug);
        }
        Tag tag = Tag.builder()
            .name(request.name())
            .slug(slug)
            .parentTagId(request.parentTagId())
            .color(request.color())
            .createdByUserId(callerUserId)
            .build();
        try {
            return TagResponse.from(tagRepository.save(tag));
        } catch (DataIntegrityViolationException ex) {
            // Race condition — two concurrent POSTs computing the same slug.
            throw new DuplicateSlugException("slug already exists (race): " + slug);
        }
    }

    @Transactional(readOnly = true)
    public TagListResponse list(UUID parentTagId) {
        List<Tag> rows = (parentTagId == null)
            ? tagRepository.findByParentTagIdIsNullOrderByNameAsc()
            : tagRepository.findByParentTagIdOrderByNameAsc(parentTagId);
        List<TagResponse> items = rows.stream().map(TagResponse::from).toList();
        return new TagListResponse(items, items.size());
    }

    @Transactional(readOnly = true)
    public TagResponse get(UUID id) {
        return TagResponse.from(load(id));
    }

    @Transactional
    public TagResponse update(UUID id, UpdateTagRequest request) {
        Tag tag = load(id);
        if (request.name() != null && !request.name().isBlank()) {
            tag.setName(request.name());
        }
        if (request.color() != null) {
            tag.setColor(request.color().isBlank() ? null : request.color());
        }
        return TagResponse.from(tagRepository.save(tag));
    }

    @Transactional
    public void delete(UUID id, boolean cascade) {
        Tag tag = load(id);
        long childCount = tagRepository.countByParentTagId(id);
        if (childCount > 0 && !cascade) {
            throw new TagHasChildrenException(
                "tag " + id + " has " + childCount + " children — pass ?cascade=true to delete the subtree");
        }
        deleteRecursive(tag.getId());
    }

    private void deleteRecursive(UUID id) {
        for (Tag child : tagRepository.findByParentTagIdOrderByNameAsc(id)) {
            deleteRecursive(child.getId());
        }
        attachmentRepository.deleteByTagId(id);
        tagRepository.deleteById(id);
    }

    @Transactional
    public AttachResult attach(String callerUserId, UUID tagId, AttachTagRequest request) {
        load(tagId); // validates existence; throws TagNotFoundException → 404
        return attachmentRepository
            .findByTagIdAndEntityTypeAndEntityId(tagId, request.entityType(), request.entityId())
            .map(existing -> new AttachResult(TagAttachmentResponse.from(existing), false))
            .orElseGet(() -> {
                TagAttachment created = attachmentRepository.save(TagAttachment.builder()
                    .tagId(tagId)
                    .entityType(request.entityType())
                    .entityId(request.entityId())
                    .attachedByUserId(callerUserId)
                    .build());
                return new AttachResult(TagAttachmentResponse.from(created), true);
            });
    }

    @Transactional
    public void detach(UUID tagId, String entityType, String entityId) {
        attachmentRepository.deleteByTagAndEntity(tagId, entityType, entityId);
        // No-op on missing — TAG-ATTACH-002 idempotency.
    }

    @Transactional(readOnly = true)
    public TagListResponse byEntity(String entityType, String entityId) {
        List<Tag> rows = attachmentRepository.findTagsByEntity(entityType, entityId);
        List<TagResponse> items = rows.stream().map(TagResponse::from).toList();
        return new TagListResponse(items, items.size());
    }

    private Tag load(UUID id) {
        return tagRepository.findById(id).orElseThrow(() -> new TagNotFoundException(id));
    }

    /** Convey to the controller whether the attach call created a new row or returned existing. */
    public record AttachResult(TagAttachmentResponse response, boolean created) {}
}
