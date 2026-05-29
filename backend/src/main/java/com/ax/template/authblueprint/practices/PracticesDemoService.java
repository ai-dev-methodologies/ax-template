package com.ax.template.authblueprint.practices;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for {@link PracticesDemoController}. Owns the only references to
 * {@link ParentRepository} and {@link SoftDeletedRecordRepository} so the controller
 * routes ALL repository access through this orchestrator — keeping the conventional
 * layering (controller → service → repository) that
 * {@code ArchitectureLayerBoundaryTest} enforces.
 *
 * <p>Behavior is preserved verbatim from the previous controller-direct implementation:
 * the same page-size clamping, the same DTO mapping, and the same soft-delete fixture
 * semantics for PRACTICES-PERS-005.
 */
@Service
public class PracticesDemoService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ParentRepository parents;
    private final SoftDeletedRecordRepository softDeletedRecords;

    public PracticesDemoService(ParentRepository parents,
                                SoftDeletedRecordRepository softDeletedRecords) {
        this.parents = parents;
        this.softDeletedRecords = softDeletedRecords;
    }

    @Transactional(readOnly = true)
    public Page<ParentResponse> listParents(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize);
        return parents.findAll(pageable).map(ParentResponse::from);
    }

    @Transactional
    public Map<String, Object> createSoftDeletedRecord(String label) {
        var record = new SoftDeletedRecord();
        record.setLabel(label);
        var saved = softDeletedRecords.save(record);
        return Map.of("id", saved.getId().toString(), "label", saved.getLabel());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listSoftDeletedRecords() {
        return softDeletedRecords.findAll().stream()
                .map(r -> Map.<String, Object>of(
                        "id", r.getId().toString(),
                        "label", r.getLabel(),
                        "deleted", r.isDeleted()))
                .toList();
    }

    @Transactional
    public void softDeleteRecord(UUID id) {
        softDeletedRecords.deleteById(id);
    }
}
