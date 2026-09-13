package com.packersmovers.marketplace.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class DashboardStatsResponse {
    private long totalLeads;
    private long verifiedLeads;
    private long convertedLeads;
    private long activeProviders;
    private long pendingProviderApprovals;
    private BigDecimal totalRevenue;
    private long unlocksToday;
}
