package com.packersmovers.marketplace.controller;

import com.packersmovers.marketplace.common.response.ApiResponse;
import com.packersmovers.marketplace.dto.lead.LeadCardResponse;
import com.packersmovers.marketplace.dto.lead.LeadDetailForProviderResponse;
import com.packersmovers.marketplace.security.CustomUserPrincipal;
import com.packersmovers.marketplace.service.ProviderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The provider's lead funnel: masked "New Leads" inbox -> view -> paid unlock -> unmasked
 * "My Leads" with contact tracking. See sections 8-10 of the architecture doc.
 */
@RestController
@RequestMapping("/api/provider/leads")
@RequiredArgsConstructor
@Tag(name = "Provider Leads", description = "New-lead inbox, unlock, and post-unlock lead tracking")
public class ProviderLeadController {

    private final ProviderService providerService;

    @GetMapping("/new")
    public ApiResponse<List<LeadCardResponse>> newLeads(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ApiResponse.success(providerService.listNewLeads(principal.getUserId()));
    }

    @PostMapping("/{assignmentId}/view")
    public ApiResponse<LeadCardResponse> view(@AuthenticationPrincipal CustomUserPrincipal principal,
                                               @PathVariable Long assignmentId) {
        return ApiResponse.success(providerService.viewLead(principal.getUserId(), assignmentId));
    }

    @PostMapping("/{assignmentId}/unlock")
    public ApiResponse<LeadDetailForProviderResponse> unlock(@AuthenticationPrincipal CustomUserPrincipal principal,
                                                              @PathVariable Long assignmentId) {
        return ApiResponse.success("Lead unlocked - contact details revealed",
                providerService.unlockLead(principal.getUserId(), assignmentId));
    }

    @GetMapping("/my")
    public ApiResponse<List<LeadDetailForProviderResponse>> myLeads(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ApiResponse.success(providerService.myLeads(principal.getUserId()));
    }

    @PostMapping("/{assignmentId}/contact")
    public ApiResponse<Void> logContact(@AuthenticationPrincipal CustomUserPrincipal principal,
                                         @PathVariable Long assignmentId) {
        providerService.logContact(principal.getUserId(), assignmentId);
        return ApiResponse.success("Contact logged", null);
    }
}
