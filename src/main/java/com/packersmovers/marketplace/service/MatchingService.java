package com.packersmovers.marketplace.service;

import com.packersmovers.marketplace.entity.Lead;

public interface MatchingService {

    /**
     * Core smart-matching + distribution engine.
     * Filters: service area + service category match -> APPROVED & active providers only
     * -> fair/priority ranking (priorityScore desc, rating desc) -> caps at Lead.maxProviders (default 3).
     * Creates one LeadAssignment(OFFERED) row per matched provider and pushes a notification.
     */
    void matchAndOffer(Lead lead);
}
