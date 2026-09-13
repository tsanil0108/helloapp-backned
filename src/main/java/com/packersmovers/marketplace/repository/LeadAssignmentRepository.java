package com.packersmovers.marketplace.repository;

import com.packersmovers.marketplace.common.enums.AssignmentStatus;
import com.packersmovers.marketplace.entity.LeadAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LeadAssignmentRepository extends JpaRepository<LeadAssignment, Long> {

    List<LeadAssignment> findByLeadId(Long leadId);

    List<LeadAssignment> findByProviderIdOrderByCreatedAtDesc(Long providerId);

    List<LeadAssignment> findByProviderIdAndStatusInOrderByCreatedAtDesc(Long providerId, List<AssignmentStatus> statuses);

    Optional<LeadAssignment> findByLeadIdAndProviderId(Long leadId, Long providerId);

    long countByLeadIdAndStatusIn(Long leadId, List<AssignmentStatus> statuses);

    long countByStatusAndUnlockedAtBetween(AssignmentStatus status, Instant start, Instant end);

    @Query("""
           select a from LeadAssignment a
           where a.provider.id = :providerId and a.status = :status
           order by a.createdAt desc
           """)
    List<LeadAssignment> findByProviderAndStatus(@Param("providerId") Long providerId,
                                                  @Param("status") AssignmentStatus status);
}
