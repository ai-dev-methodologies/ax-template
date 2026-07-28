package com.ax.template.authblueprint.approvalworkflow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {

    List<ApprovalRequest> findByRequesterUserIdOrderByCreatedAtDesc(String requesterUserId);

    /**
     * Inbox query — PENDING steps assigned to {@code approverUserId} on
     * {@link ApprovalRequestStatus#SUBMITTED} requests, oldest request first.
     */
    @Query(
        "SELECT s FROM ApprovalStep s " +
        "JOIN s.request r " +
        "WHERE s.approverUserId = :approverUserId " +
        "AND s.status = com.ax.template.authblueprint.approvalworkflow.ApprovalStepStatus.PENDING " +
        "AND r.status = com.ax.template.authblueprint.approvalworkflow.ApprovalRequestStatus.SUBMITTED " +
        "ORDER BY r.createdAt ASC"
    )
    List<ApprovalStep> findInboxFor(@Param("approverUserId") String approverUserId);
}
