package com.packersmovers.marketplace.service;

import com.packersmovers.marketplace.dto.admin.AuditLogResponse;
import com.packersmovers.marketplace.dto.admin.DashboardStatsResponse;
import com.packersmovers.marketplace.dto.admin.ProviderLeadHistoryResponse;
import com.packersmovers.marketplace.dto.admin.ProviderPerformanceResponse;
import com.packersmovers.marketplace.dto.admin.SettingResponse;
import com.packersmovers.marketplace.dto.admin.SettingUpdateRequest;
import com.packersmovers.marketplace.dto.common.PageResponse;
import com.packersmovers.marketplace.dto.provider.ProviderApprovalRequest;
import com.packersmovers.marketplace.dto.provider.ProviderProfileResponse;

import java.util.List;

/** Super Admin / Admin control-plane operations: provider approval, settings, dashboard, audit trail. */
public interface AdminService {

    DashboardStatsResponse getDashboardStats();

    /** @param status optional ProviderStatus filter (null/blank = all providers) */
    List<ProviderProfileResponse> listProviders(String status);

    /** Approve / reject / request changes / suspend / reactivate a provider. */
    ProviderProfileResponse actOnProvider(
            Long adminUserId,
            Long providerId,
            ProviderApprovalRequest request
    );

    /**
     * Get complete performance statistics for a provider.
     */
    ProviderPerformanceResponse getProviderPerformance(Long providerId);

    /**
     * Get paginated lead history handled by a provider.
     */
    PageResponse<ProviderLeadHistoryResponse> getProviderLeadHistory(
            Long providerId,
            int page,
            int size
    );

    List<SettingResponse> listSettings();

    SettingResponse upsertSetting(SettingUpdateRequest request);

    PageResponse<AuditLogResponse> listAuditLogs(int page, int size);
}