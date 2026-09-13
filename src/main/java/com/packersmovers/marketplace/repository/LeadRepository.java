package com.packersmovers.marketplace.repository;

import com.packersmovers.marketplace.common.enums.LeadStatus;
import com.packersmovers.marketplace.entity.Lead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    Optional<Lead> findByLeadCode(String leadCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Lead l where l.id = :id")
    Optional<Lead> findByIdForUpdate(@Param("id") Long id);

    Page<Lead> findByStatus(LeadStatus status, Pageable pageable);

    List<Lead> findByCustomerMobileOrderByCreatedAtDesc(String mobile);

    /** Duplicate-lead check: same mobile + same route submitted within the configured rolling window. */
    @Query("""
           select l from Lead l
           where l.customerMobile = :mobile
             and l.pickupLocation = :pickup
             and l.dropLocation = :drop
             and l.createdAt >= :since
           """)
    List<Lead> findPossibleDuplicates(@Param("mobile") String mobile,
                                       @Param("pickup") String pickup,
                                       @Param("drop") String drop,
                                       @Param("since") Instant since);

    /** Rate-limit check: how many leads this mobile number has submitted since a cutoff time. */
    long countByCustomerMobileAndCreatedAtAfter(String mobile, Instant since);

    @Query("select count(l) from Lead l where l.status = :status")
    long countByStatus(@Param("status") LeadStatus status);

    long countByStatusNotIn(List<LeadStatus> statuses);

    long countByCreatedAtBetween(Instant start, Instant end);
}
