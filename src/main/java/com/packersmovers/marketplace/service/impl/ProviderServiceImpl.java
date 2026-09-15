package com.packersmovers.marketplace.service.impl;

import com.packersmovers.marketplace.common.enums.AssignmentStatus;
import com.packersmovers.marketplace.common.enums.LeadStatus;
import com.packersmovers.marketplace.common.enums.ProviderStatus;
import com.packersmovers.marketplace.common.exception.BadRequestException;
import com.packersmovers.marketplace.common.exception.ResourceNotFoundException;
import com.packersmovers.marketplace.common.exception.UnauthorizedActionException;
import com.packersmovers.marketplace.dto.lead.LeadCardResponse;
import com.packersmovers.marketplace.dto.lead.LeadDetailForProviderResponse;
import com.packersmovers.marketplace.dto.provider.KycUploadRequest;
import com.packersmovers.marketplace.dto.provider.ProviderProfileResponse;
import com.packersmovers.marketplace.entity.KycDocument;
import com.packersmovers.marketplace.entity.Lead;
import com.packersmovers.marketplace.entity.LeadAssignment;
import com.packersmovers.marketplace.entity.Provider;
import com.packersmovers.marketplace.entity.WalletTransaction;
import com.packersmovers.marketplace.repository.KycDocumentRepository;
import com.packersmovers.marketplace.repository.LeadAssignmentRepository;
import com.packersmovers.marketplace.repository.LeadRepository;
import com.packersmovers.marketplace.repository.ProviderRepository;
import com.packersmovers.marketplace.service.AuditService;
import com.packersmovers.marketplace.service.ProviderService;
import com.packersmovers.marketplace.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProviderServiceImpl implements ProviderService {

    private final ProviderRepository providerRepository;
    private final LeadAssignmentRepository leadAssignmentRepository;
    private final LeadRepository leadRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final WalletService walletService;
    private final AuditService auditService;

    /**
     * Provider profile.
     *
     * Transaction is intentionally kept open while accessing:
     * - provider.user
     * - provider.serviceAreas
     * - provider.serviceCategories
     *
     * These relationships may be LAZY-loaded by Hibernate.
     */
    @Override
    @Transactional(readOnly = true)
    public ProviderProfileResponse getMyProfile(Long userId) {

        Provider provider = resolveProvider(userId);

        var wallet = walletService.getWallet(provider.getId());

        return ProviderProfileResponse.builder()
                .id(provider.getId())
                .companyName(provider.getCompanyName())
                .ownerName(provider.getOwnerName())

                // Lazy User relationship is now safely initialized
                // inside the active transaction.
                .email(provider.getUser().getEmail())
                .mobile(provider.getUser().getMobile())

                .gstNumber(provider.getGstNumber())
                .panNumber(provider.getPanNumber())
                .status(provider.getStatus())
                .verifiedBadge(provider.isVerifiedBadge())
                .rating(provider.getRating())
                .reviewCount(provider.getReviewCount())
                .walletBalance(wallet.getBalance())

                // Lazy collections are also accessed inside transaction.
                .serviceAreas(
                        provider.getServiceAreas()
                                .stream()
                                .map(a -> a.getCityOrRegion())
                                .toList()
                )

                .serviceCategories(
                        provider.getServiceCategories()
                                .stream()
                                .map(c -> c.getName())
                                .toList()
                )

                .approvedAt(provider.getApprovedAt())
                .build();
    }

    @Override
    public Long resolveProviderId(Long userId) {
        return resolveProvider(userId).getId();
    }

    @Override
    @Transactional
    public void uploadKycDocument(Long userId, KycUploadRequest request) {

        Provider provider = resolveProvider(userId);

        KycDocument doc = KycDocument.builder()
                .provider(provider)
                .docType(request.getDocType())
                .docUrl(request.getDocUrl())
                .verified(false)
                .build();

        kycDocumentRepository.save(doc);

        if (provider.getStatus() == ProviderStatus.SUBMITTED) {
            provider.setStatus(ProviderStatus.UNDER_REVIEW);
            providerRepository.save(provider);
        }

        auditService.log(
                "KYC_DOCUMENT_UPLOADED",
                "Provider",
                provider.getId(),
                null,
                request.getDocType().name(),
                "docUrl=" + request.getDocUrl()
        );
    }

    @Override
    @Transactional
    public List<LeadCardResponse> listNewLeads(Long userId) {

        Provider provider = resolveProvider(userId);

        List<LeadAssignment> assignments =
                leadAssignmentRepository
                        .findByProviderIdAndStatusInOrderByCreatedAtDesc(
                                provider.getId(),
                                List.of(
                                        AssignmentStatus.OFFERED,
                                        AssignmentStatus.VIEWED
                                )
                        );

        return assignments.stream()
                .filter(this::expireIfPastDeadline)
                .map(this::toCard)
                .toList();
    }

    @Override
    @Transactional
    public LeadCardResponse viewLead(
            Long userId,
            Long leadAssignmentId
    ) {

        Provider provider = resolveProvider(userId);

        LeadAssignment assignment =
                findOwnedAssignment(
                        leadAssignmentId,
                        provider
                );

        if (expireIfPastDeadline(assignment)) {
            throw new BadRequestException(
                    "This lead offer has expired"
            );
        }

        if (assignment.getStatus() == AssignmentStatus.OFFERED) {
            assignment.setStatus(AssignmentStatus.VIEWED);
            assignment.setViewedAt(Instant.now());
            leadAssignmentRepository.save(assignment);
        }

        return toCard(assignment);
    }

    @Override
    @Transactional
    public LeadDetailForProviderResponse unlockLead(
            Long userId,
            Long leadAssignmentId
    ) {

        Provider provider = resolveProvider(userId);

        LeadAssignment assignment =
                findOwnedAssignment(
                        leadAssignmentId,
                        provider
                );

        if (assignment.getStatus() == AssignmentStatus.UNLOCKED) {
            return toDetail(assignment);
        }

        if (assignment.getStatus() != AssignmentStatus.OFFERED
                && assignment.getStatus() != AssignmentStatus.VIEWED) {

            throw new BadRequestException(
                    "This lead offer is no longer available ("
                            + assignment.getStatus()
                            + ")"
            );
        }

        if (expireIfPastDeadline(assignment)) {
            throw new BadRequestException(
                    "This lead offer has expired"
            );
        }

        Lead lead = assignment.getLead();

        WalletTransaction txn =
                walletService.debitForUnlock(
                        provider.getId(),
                        lead.getUnlockPrice(),
                        lead.getId(),
                        "Unlock fee for lead "
                                + lead.getLeadCode()
                );

        assignment.setStatus(AssignmentStatus.UNLOCKED);
        assignment.setUnlockedAt(Instant.now());
        assignment.setUnlockFeeCharged(
                lead.getUnlockPrice()
        );
        assignment.setWalletTransaction(txn);

        leadAssignmentRepository.save(assignment);

        lead.setCurrentUnlockCount(
                lead.getCurrentUnlockCount() + 1
        );

        if (!lead.getStatus().isTerminal()) {
            lead.setStatus(LeadStatus.UNLOCKED);
        }

        leadRepository.save(lead);

        auditService.log(
                "LEAD_UNLOCKED",
                "LeadAssignment",
                assignment.getId(),
                null,
                AssignmentStatus.UNLOCKED.name(),
                "provider=" + provider.getId()
                        + " fee=" + lead.getUnlockPrice()
        );

        return toDetail(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeadDetailForProviderResponse> myLeads(
            Long userId
    ) {

        Provider provider = resolveProvider(userId);

        return leadAssignmentRepository
                .findByProviderIdAndStatusInOrderByCreatedAtDesc(
                        provider.getId(),
                        List.of(AssignmentStatus.UNLOCKED)
                )
                .stream()
                .map(this::toDetail)
                .toList();
    }

    @Override
    @Transactional
    public void logContact(
            Long userId,
            Long leadAssignmentId
    ) {

        Provider provider = resolveProvider(userId);

        LeadAssignment assignment =
                findOwnedAssignment(
                        leadAssignmentId,
                        provider
                );

        if (assignment.getStatus()
                != AssignmentStatus.UNLOCKED) {

            throw new BadRequestException(
                    "Unlock this lead before logging a contact"
            );
        }

        if (assignment.getContactedAt() == null) {
            assignment.setContactedAt(Instant.now());
            leadAssignmentRepository.save(assignment);
        }

        Lead lead = assignment.getLead();

        if (lead.getStatus() == LeadStatus.UNLOCKED) {
            lead.setStatus(LeadStatus.CONTACTED);
            leadRepository.save(lead);
        }

        auditService.log(
                "LEAD_CONTACTED",
                "LeadAssignment",
                assignment.getId(),
                null,
                "CONTACTED",
                "provider=" + provider.getId()
        );
    }

    // ---------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------

    private Provider resolveProvider(Long userId) {

        return providerRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No provider profile for this account"
                        )
                );
    }

    private LeadAssignment findOwnedAssignment(
            Long leadAssignmentId,
            Provider provider
    ) {

        LeadAssignment assignment =
                leadAssignmentRepository
                        .findById(leadAssignmentId)
                        .orElseThrow(() ->
                                ResourceNotFoundException.of(
                                        "LeadAssignment",
                                        leadAssignmentId
                                )
                        );

        if (!assignment.getProvider()
                .getId()
                .equals(provider.getId())) {

            throw new UnauthorizedActionException(
                    "This lead was not offered to your account"
            );
        }

        return assignment;
    }

    private boolean expireIfPastDeadline(
            LeadAssignment assignment
    ) {

        boolean isOpen =
                assignment.getStatus()
                        == AssignmentStatus.OFFERED
                        || assignment.getStatus()
                        == AssignmentStatus.VIEWED;

        if (isOpen
                && assignment.getExpiresAt() != null
                && Instant.now()
                .isAfter(assignment.getExpiresAt())) {

            assignment.setStatus(
                    AssignmentStatus.EXPIRED
            );

            leadAssignmentRepository.save(
                    assignment
            );

            return true;
        }

        return assignment.getStatus()
                == AssignmentStatus.EXPIRED;
    }

    private LeadCardResponse toCard(
            LeadAssignment assignment
    ) {

        Lead lead = assignment.getLead();

        return LeadCardResponse.builder()
                .leadAssignmentId(
                        assignment.getId()
                )
                .leadCode(
                        lead.getLeadCode()
                )
                .pickupAreaMasked(
                        maskLocation(
                                lead.getPickupLocation()
                        )
                )
                .dropAreaMasked(
                        maskLocation(
                                lead.getDropLocation()
                        )
                )
                .serviceCategory(
                        lead.getServiceCategory() != null
                                ? lead.getServiceCategory().getName()
                                : null
                )
                .moveDate(
                        lead.getMoveDate()
                )
                .propertyType(
                        lead.getPropertyType()
                )
                .unlockPrice(
                        lead.getUnlockPrice()
                )
                .status(
                        assignment.getStatus()
                )
                .build();
    }

    private LeadDetailForProviderResponse toDetail(
            LeadAssignment assignment
    ) {

        Lead lead = assignment.getLead();

        return LeadDetailForProviderResponse.builder()
                .leadAssignmentId(
                        assignment.getId()
                )
                .leadCode(
                        lead.getLeadCode()
                )
                .customerName(
                        lead.getCustomerName()
                )
                .customerMobile(
                        lead.getCustomerMobile()
                )
                .customerEmail(
                        lead.getCustomerEmail()
                )
                .pickupLocation(
                        lead.getPickupLocation()
                )
                .dropLocation(
                        lead.getDropLocation()
                )
                .moveDate(
                        lead.getMoveDate()
                )
                .propertyType(
                        lead.getPropertyType()
                )
                .inventoryNotes(
                        lead.getInventoryNotes()
                )
                .serviceCategory(
                        lead.getServiceCategory() != null
                                ? lead.getServiceCategory().getName()
                                : null
                )
                .assignmentStatus(
                        assignment.getStatus()
                )
                .unlockedAt(
                        assignment.getUnlockedAt()
                )
                .contactedAt(
                        assignment.getContactedAt()
                )
                .build();
    }

    private String maskLocation(
            String location
    ) {

        if (location == null
                || location.isBlank()) {
            return "***";
        }

        String[] parts =
                location.split(",");

        String visible =
                parts[0].trim();

        return parts.length > 1
                ? visible + ", ***"
                : visible;
    }
}