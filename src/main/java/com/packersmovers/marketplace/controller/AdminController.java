package com.packersmovers.marketplace.controller;

import com.packersmovers.marketplace.common.response.ApiResponse;
import com.packersmovers.marketplace.dto.admin.AuditLogResponse;
import com.packersmovers.marketplace.dto.admin.DashboardStatsResponse;
import com.packersmovers.marketplace.dto.admin.SettingResponse;
import com.packersmovers.marketplace.dto.admin.SettingUpdateRequest;
import com.packersmovers.marketplace.dto.common.PageResponse;
import com.packersmovers.marketplace.dto.lead.LeadResponse;
import com.packersmovers.marketplace.dto.lead.LeadStatusUpdateRequest;
import com.packersmovers.marketplace.dto.provider.ProviderApprovalRequest;
import com.packersmovers.marketplace.dto.provider.ProviderProfileResponse;
import com.packersmovers.marketplace.dto.quote.QuoteResponse;
import com.packersmovers.marketplace.security.CustomUserPrincipal;
import com.packersmovers.marketplace.service.AdminService;
import com.packersmovers.marketplace.service.LeadService;
import com.packersmovers.marketplace.service.QuoteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin + Super Admin control plane. Fine-grained per-module RBAC (AdminPermission) is enforced
 * at the service layer for write actions in a fuller build; path-level access here is
 * ROLE_ADMIN or ROLE_SUPER_ADMIN (see SecurityConfig) with SUPER_ADMIN-only actions under /api/super-admin.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Dashboard, provider approvals, lead operations, settings, audit trail")
public class AdminController {

    private final AdminService adminService;
    private final LeadService leadService;
    private final QuoteService quoteService;

    @GetMapping("/dashboard")
    public ApiResponse<DashboardStatsResponse> dashboard() {
        return ApiResponse.success(adminService.getDashboardStats());
    }

    // ---- Provider Ops ----

    @GetMapping("/providers")
    public ApiResponse<List<ProviderProfileResponse>> listProviders(
            @RequestParam(required = false) String status) {
        return ApiResponse.success(adminService.listProviders(status));
    }

    @PostMapping("/providers/{providerId}/action")
    public ApiResponse<ProviderProfileResponse> actOnProvider(@AuthenticationPrincipal CustomUserPrincipal principal,
                                                               @PathVariable Long providerId,
                                                               @Valid @RequestBody ProviderApprovalRequest request) {
        return ApiResponse.success(adminService.actOnProvider(principal.getUserId(), providerId, request));
    }

    // ---- Lead Ops ----

    @GetMapping("/leads")
    public ApiResponse<PageResponse<LeadResponse>> listLeads(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(leadService.search(status, page, size));
    }

    @GetMapping("/leads/code/{leadCode}")
    public ApiResponse<LeadResponse> getLead(@PathVariable String leadCode) {
        return ApiResponse.success(leadService.getByCode(leadCode));
    }

    @PutMapping("/leads/{leadId}/status")
    public ApiResponse<LeadResponse> updateLeadStatus(@PathVariable Long leadId,
                                                       @Valid @RequestBody LeadStatusUpdateRequest request) {
        return ApiResponse.success(leadService.updateStatus(leadId, request));
    }

    @GetMapping("/leads/{leadId}/quotes")
    public ApiResponse<List<QuoteResponse>> quotesForLead(@PathVariable Long leadId) {
        return ApiResponse.success(quoteService.getQuotesForLead(leadId));
    }

    // ---- Settings ----

    @GetMapping("/settings")
    public ApiResponse<List<SettingResponse>> listSettings() {
        return ApiResponse.success(adminService.listSettings());
    }

    @PutMapping("/settings")
    public ApiResponse<SettingResponse> upsertSetting(@Valid @RequestBody SettingUpdateRequest request) {
        return ApiResponse.success("Setting saved", adminService.upsertSetting(request));
    }

    // ---- Audit ----

    @GetMapping("/audit-logs")
    public ApiResponse<PageResponse<AuditLogResponse>> auditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(adminService.listAuditLogs(page, size));
    }
}
