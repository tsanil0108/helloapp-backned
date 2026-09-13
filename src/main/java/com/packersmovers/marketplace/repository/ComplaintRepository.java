package com.packersmovers.marketplace.repository;

import com.packersmovers.marketplace.common.enums.ComplaintStatus;
import com.packersmovers.marketplace.entity.Complaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    Page<Complaint> findByStatus(ComplaintStatus status, Pageable pageable);
}
