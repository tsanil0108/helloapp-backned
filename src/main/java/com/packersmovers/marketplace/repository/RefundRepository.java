package com.packersmovers.marketplace.repository;

import com.packersmovers.marketplace.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    List<Refund> findByProviderIdOrderByCreatedAtDesc(Long providerId);
}
