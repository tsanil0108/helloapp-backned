package com.packersmovers.marketplace.service;

import com.packersmovers.marketplace.dto.lead.LeadAssignmentHistoryResponse;
import com.packersmovers.marketplace.dto.lead.LeadAssignmentStatusRequest;
import com.packersmovers.marketplace.dto.lead.LeadCardResponse;
import com.packersmovers.marketplace.dto.lead.LeadDetailForProviderResponse;
import com.packersmovers.marketplace.dto.lead.ProviderLeadStatsResponse;
import com.packersmovers.marketplace.dto.provider.KycUploadRequest;
import com.packersmovers.marketplace.dto.provider.ProviderProfileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProviderService {

    // ============================================================
    // PROFILE
    // ============================================================

    ProviderProfileResponse getMyProfile(
            Long userId
    );

    Long resolveProviderId(
            Long userId
    );


    // ============================================================
    // KYC
    // ============================================================

    void uploadKycDocument(
            Long userId,
            KycUploadRequest request,
            MultipartFile file
    );


    // ============================================================
    // NEW LEADS
    // ============================================================

    List<LeadCardResponse> listNewLeads(
            Long userId
    );

    LeadCardResponse viewLead(
            Long userId,
            Long leadAssignmentId
    );


    // ============================================================
    // LEAD UNLOCK
    // ============================================================

    LeadDetailForProviderResponse unlockLead(
            Long userId,
            Long leadAssignmentId
    );


    // ============================================================
    // CONTACT
    // ============================================================

    void markContacted(
            Long userId,
            Long leadAssignmentId
    );


    // ============================================================
    // COMPLETE LEAD LIFECYCLE
    // ============================================================

    void updateLeadAssignmentStatus(
            Long userId,
            Long leadAssignmentId,
            LeadAssignmentStatusRequest request
    );


    // ============================================================
    // MY LEADS
    // ============================================================

    List<LeadDetailForProviderResponse> listMyLeads(
            Long userId
    );


    // ============================================================
    // LEAD HISTORY
    // ============================================================

    List<LeadAssignmentHistoryResponse> listMyLeadHistory(
            Long userId
    );


    // ============================================================
    // LEAD STATISTICS
    // ============================================================

    ProviderLeadStatsResponse getMyLeadStats(
            Long userId
    );
}