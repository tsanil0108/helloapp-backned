package com.packersmovers.marketplace.repository;

import com.packersmovers.marketplace.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    Optional<Campaign> findByCampaignNameIgnoreCase(String campaignName);
}
