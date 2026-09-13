package com.packersmovers.marketplace.repository;

import com.packersmovers.marketplace.entity.ProviderSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderSubscriptionRepository extends JpaRepository<ProviderSubscription, Long> {
    Optional<ProviderSubscription> findFirstByProviderIdAndActiveTrueOrderByEndDateDesc(Long providerId);
    List<ProviderSubscription> findByProviderIdOrderByCreatedAtDesc(Long providerId);
}
