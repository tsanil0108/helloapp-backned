package com.packersmovers.marketplace.service;

import com.packersmovers.marketplace.dto.lead.LeadCardResponse;
import com.packersmovers.marketplace.dto.lead.LeadDetailForProviderResponse;
import com.packersmovers.marketplace.dto.provider.KycUploadRequest;
import com.packersmovers.marketplace.dto.provider.ProviderProfileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProviderService {

    ProviderProfileResponse getMyProfile(Long userId);

    Long resolveProviderId(Long userId);

    void uploadKycDocument(
            Long userId,
            KycUploadRequest request,
            MultipartFile file
    );

    List<LeadCardResponse> listNewLeads(Long userId);

    LeadCardResponse viewLead(
            Long userId,
            Long leadAssignmentId
    );

    LeadDetailForProviderResponse unlockLead(
            Long userId,
            Long leadAssignmentId
    );

    void markContacted(
            Long userId,
            Long leadAssignmentId
    );

    List<LeadDetailForProviderResponse> listMyLeads(Long userId);
}