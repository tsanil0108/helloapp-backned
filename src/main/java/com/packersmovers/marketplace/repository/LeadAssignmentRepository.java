package com.packersmovers.marketplace.repository;

import com.packersmovers.marketplace.common.enums.AssignmentStatus;
import com.packersmovers.marketplace.entity.LeadAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LeadAssignmentRepository
        extends JpaRepository<LeadAssignment, Long> {

    // ============================================================
    // LEAD
    // ============================================================

    List<LeadAssignment> findByLeadId(
            Long leadId
    );

    Optional<LeadAssignment> findByLeadIdAndProviderId(
            Long leadId,
            Long providerId
    );

    long countByLeadIdAndStatusIn(
            Long leadId,
            List<AssignmentStatus> statuses
    );


    // ============================================================
    // PROVIDER LEADS
    // ============================================================

    List<LeadAssignment>
    findByProviderIdAndStatusInOrderByCreatedAtDesc(
            Long providerId,
            List<AssignmentStatus> statuses
    );

    List<LeadAssignment>
    findByProviderIdOrderByCreatedAtDesc(
            Long providerId
    );

    List<LeadAssignment>
    findByProviderIdAndStatusOrderByCreatedAtDesc(
            Long providerId,
            AssignmentStatus status
    );


    // ============================================================
    // PROVIDER STATISTICS
    // ============================================================

    long countByProviderId(
            Long providerId
    );

    long countByProviderIdAndStatus(
            Long providerId,
            AssignmentStatus status
    );


    // ============================================================
    // ADMIN / DASHBOARD
    // ============================================================

    long countByStatusAndUnlockedAtBetween(
            AssignmentStatus status,
            Instant start,
            Instant end
    );


    // ============================================================
    // ADMIN PROVIDER LEAD HISTORY
    // ============================================================

    Page<LeadAssignment> findByProviderIdOrderByCreatedAtDesc(
            Long providerId,
            Pageable pageable
    );


    // ============================================================
    // EXPLICIT PROVIDER + STATUS QUERY
    // ============================================================

    @Query("""
            SELECT a
            FROM LeadAssignment a
            WHERE a.provider.id = :providerId
              AND a.status = :status
            ORDER BY a.createdAt DESC
            """)
    List<LeadAssignment> findByProviderAndStatus(
            @Param("providerId")
            Long providerId,

            @Param("status")
            AssignmentStatus status
    );
}