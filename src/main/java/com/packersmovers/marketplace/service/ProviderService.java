package com.packersmovers.marketplace.service;

import com.packersmovers.marketplace.dto.lead.LeadCardResponse;
import com.packersmovers.marketplace.dto.lead.LeadDetailForProviderResponse;
import com.packersmovers.marketplace.dto.provider.KycUploadRequest;
import com.packersmovers.marketplace.dto.provider.ProviderProfileResponse;

import java.util.List;

/**
 * Everything a logged-in Provider does with their own account:
 * profile, KYC, the new-lead inbox (masked), unlocking, and post-unlock lead tracking.
 * All methods take the authenticated User's id and resolve the Provider record internally.
 */
public interface ProviderService {

    ProviderProfileResponse getMyProfile(Long userId);

    /** Resolves Provider.id for the logged-in user - used by controllers that call Wallet/Payment/Quote services directly. */
    Long resolveProviderId(Long userId);

    void uploadKycDocument(Long userId, KycUploadRequest request);

    /** Masked "New Leads" inbox: offers not yet unlocked (OFFERED or VIEWED). */
    List<LeadCardResponse> listNewLeads(Long userId);

    /** Marks an offer as viewed (still masked) - called when the provider opens a lead card. */
    LeadCardResponse viewLead(Long userId, Long leadAssignmentId);

    /** Pays the unlock fee from the wallet and reveals full customer contact details. */
    LeadDetailForProviderResponse unlockLead(Long userId, Long leadAssignmentId);

    /** "My Leads": already-unlocked leads with full detail + status/contact tracking. */
    List<LeadDetailForProviderResponse> myLeads(Long userId);

    /** Logs a contact event (call/WhatsApp/SMS) against an unlocked lead. */
    void logContact(Long userId, Long leadAssignmentId);
}
