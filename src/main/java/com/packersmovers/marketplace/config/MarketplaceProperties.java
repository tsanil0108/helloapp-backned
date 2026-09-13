package com.packersmovers.marketplace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app.marketplace")
public record MarketplaceProperties(
        int defaultMaxProvidersPerLead,
        int leadOfferExpiryHours,
        BigDecimal defaultLeadUnlockFee,
        int duplicateLeadWindowHours,
        int rateLimitPerMobilePerDay
) {
}
