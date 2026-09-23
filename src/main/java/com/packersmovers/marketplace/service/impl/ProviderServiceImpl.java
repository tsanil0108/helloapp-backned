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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProviderServiceImpl implements ProviderService {

    private final ProviderRepository providerRepository;

    private final LeadAssignmentRepository leadAssignmentRepository;

    private final LeadRepository leadRepository;

    private final KycDocumentRepository kycDocumentRepository;

    private final WalletService walletService;

    private final AuditService auditService;


    // ============================================================
    // PROFILE
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public ProviderProfileResponse getMyProfile(
            Long userId
    ) {

        Provider provider =
                resolveProvider(userId);

        var wallet =
                walletService.getWallet(
                        provider.getId()
                );

        return ProviderProfileResponse.builder()

                .id(
                        provider.getId()
                )

                .companyName(
                        provider.getCompanyName()
                )

                .ownerName(
                        provider.getOwnerName()
                )

                .email(
                        provider.getUser().getEmail()
                )

                .mobile(
                        provider.getUser().getMobile()
                )

                .gstNumber(
                        provider.getGstNumber()
                )

                .panNumber(
                        provider.getPanNumber()
                )

                .status(
                        provider.getStatus()
                )

                .verifiedBadge(
                        provider.isVerifiedBadge()
                )

                .rating(
                        provider.getRating()
                )

                .reviewCount(
                        provider.getReviewCount()
                )

                .walletBalance(
                        wallet.getBalance()
                )

                .serviceAreas(
                        provider.getServiceAreas()
                                .stream()
                                .map(
                                        a -> a.getCityOrRegion()
                                )
                                .toList()
                )

                .serviceCategories(
                        provider.getServiceCategories()
                                .stream()
                                .map(
                                        c -> c.getName()
                                )
                                .toList()
                )

                .approvedAt(
                        provider.getApprovedAt()
                )

                .kycSubmitted(
                        kycDocumentRepository
                                .existsByProviderId(
                                        provider.getId()
                                )
                )

                .kycDocumentCount(
                        kycDocumentRepository
                                .countByProviderId(
                                        provider.getId()
                                )
                )

                .build();
    }


    // ============================================================
    // PROVIDER ID
    // ============================================================

    @Override
    public Long resolveProviderId(
            Long userId
    ) {

        return resolveProvider(userId)
                .getId();
    }


    // ============================================================
    // KYC FILE UPLOAD
    // ============================================================

    @Override
    @Transactional
    public void uploadKycDocument(
            Long userId,
            KycUploadRequest request,
            MultipartFile file
    ) {

        if (request == null
                || request.getDocType() == null) {

            throw new BadRequestException(
                    "Please select a KYC document type"
            );
        }

        if (file == null
                || file.isEmpty()) {

            throw new BadRequestException(
                    "Please select a KYC document"
            );
        }

        Provider provider =
                resolveProvider(userId);


        // --------------------------------------------------------
        // FILE SIZE
        // --------------------------------------------------------

        long maxFileSize =
                10L * 1024L * 1024L;

        if (file.getSize() > maxFileSize) {

            throw new BadRequestException(
                    "Maximum KYC file size is 10 MB"
            );
        }


        // --------------------------------------------------------
        // FILE NAME
        // --------------------------------------------------------

        String originalName =
                file.getOriginalFilename();

        if (originalName == null
                || originalName.isBlank()) {

            throw new BadRequestException(
                    "Invalid file name"
            );
        }


        String lowerName =
                originalName.toLowerCase();


        boolean isPdf =
                lowerName.endsWith(".pdf");

        boolean isJpg =
                lowerName.endsWith(".jpg")
                        || lowerName.endsWith(".jpeg");

        boolean isPng =
                lowerName.endsWith(".png");


        if (!isPdf
                && !isJpg
                && !isPng) {

            throw new BadRequestException(
                    "Only PDF, JPG, JPEG and PNG files are allowed"
            );
        }


        // --------------------------------------------------------
        // EXTENSION
        // --------------------------------------------------------

        String extension;

        if (isPdf) {

            extension = ".pdf";

        } else if (isPng) {

            extension = ".png";

        } else {

            extension = ".jpg";
        }


        // --------------------------------------------------------
        // GENERATE SAFE FILE NAME
        // --------------------------------------------------------

        String generatedName =
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        + extension;


        // --------------------------------------------------------
        // CREATE PROVIDER DIRECTORY
        // --------------------------------------------------------

        Path directory =
                Paths.get(
                        "uploads",
                        "kyc",
                        "provider-"
                                + provider.getId()
                );


        try {

            Files.createDirectories(
                    directory
            );


            // ----------------------------------------------------
            // SAVE FILE
            // ----------------------------------------------------

            Path destination =
                    directory.resolve(
                            generatedName
                    );


            Files.copy(
                    file.getInputStream(),
                    destination
            );


            // ----------------------------------------------------
            // URL / PATH STORED IN DATABASE
            // ----------------------------------------------------

            String fileUrl =
                    "/uploads/kyc/provider-"
                            + provider.getId()
                            + "/"
                            + generatedName;


            // ----------------------------------------------------
            // SAVE DATABASE RECORD
            // ----------------------------------------------------

            KycDocument document =
                    KycDocument.builder()

                            .provider(provider)

                            .docType(
                                    request.getDocType()
                            )

                            .docUrl(
                                    fileUrl
                            )

                            .verified(false)

                            .remarks(null)

                            .build();


            kycDocumentRepository.save(
                    document
            );


            // ----------------------------------------------------
            // CHANGE PROVIDER STATUS
            // ----------------------------------------------------

            if (provider.getStatus()
                    == ProviderStatus.SUBMITTED) {

                provider.setStatus(
                        ProviderStatus.UNDER_REVIEW
                );

                providerRepository.save(
                        provider
                );
            }


            // ----------------------------------------------------
            // AUDIT LOG
            // ----------------------------------------------------

            auditService.log(

                    "KYC_DOCUMENT_UPLOADED",

                    "Provider",

                    provider.getId(),

                    null,

                    request
                            .getDocType()
                            .name(),

                    "KYC file uploaded: "
                            + fileUrl
            );


        } catch (IOException e) {

            throw new BadRequestException(
                    "Unable to save KYC document"
            );
        }
    }


    // ============================================================
    // NEW LEADS
    // ============================================================

    @Override
    @Transactional
    public List<LeadCardResponse> listNewLeads(
            Long userId
    ) {

        Provider provider =
                resolveProvider(userId);

        List<LeadAssignment> assignments =
                leadAssignmentRepository
                        .findByProviderIdAndStatusInOrderByCreatedAtDesc(
                                provider.getId(),
                                List.of(
                                        AssignmentStatus.OFFERED,
                                        AssignmentStatus.VIEWED
                                )
                        );

        return assignments
                .stream()
                .filter(
                        this::expireIfPastDeadline
                )
                .map(
                        this::toCard
                )
                .toList();
    }


    // ============================================================
    // VIEW LEAD
    // ============================================================

    @Override
    @Transactional
    public LeadCardResponse viewLead(
            Long userId,
            Long leadAssignmentId
    ) {

        Provider provider =
                resolveProvider(userId);

        LeadAssignment assignment =
                findOwnedAssignment(
                        leadAssignmentId,
                        provider
                );


        if (expireIfPastDeadline(
                assignment
        )) {

            throw new BadRequestException(
                    "This lead offer has expired"
            );
        }


        if (assignment.getStatus()
                == AssignmentStatus.OFFERED) {

            assignment.setStatus(
                    AssignmentStatus.VIEWED
            );

            assignment.setViewedAt(
                    Instant.now()
            );

            leadAssignmentRepository.save(
                    assignment
            );
        }


        return toCard(
                assignment
        );
    }


    // ============================================================
    // UNLOCK LEAD
    // ============================================================

    @Override
    @Transactional
    public LeadDetailForProviderResponse unlockLead(
            Long userId,
            Long leadAssignmentId
    ) {

        Provider provider =
                resolveProvider(userId);

        LeadAssignment assignment =
                findOwnedAssignment(
                        leadAssignmentId,
                        provider
                );


        if (assignment.getStatus()
                == AssignmentStatus.UNLOCKED) {

            return toDetail(
                    assignment
            );
        }


        if (assignment.getStatus()
                != AssignmentStatus.OFFERED
                && assignment.getStatus()
                != AssignmentStatus.VIEWED) {

            throw new BadRequestException(
                    "This lead offer is no longer available ("
                            + assignment.getStatus()
                            + ")"
            );
        }


        if (expireIfPastDeadline(
                assignment
        )) {

            throw new BadRequestException(
                    "This lead offer has expired"
            );
        }


        Lead lead =
                assignment.getLead();


        WalletTransaction txn =
                walletService.debitForUnlock(

                        provider.getId(),

                        lead.getUnlockPrice(),

                        lead.getId(),

                        "Unlock fee for lead "
                                + lead.getLeadCode()
                );


        assignment.setStatus(
                AssignmentStatus.UNLOCKED
        );

        assignment.setUnlockedAt(
                Instant.now()
        );

        assignment.setUnlockFeeCharged(
                lead.getUnlockPrice()
        );

        assignment.setWalletTransaction(
                txn
        );


        leadAssignmentRepository.save(
                assignment
        );


        lead.setCurrentUnlockCount(
                lead.getCurrentUnlockCount() + 1
        );


        if (!lead.getStatus()
                .isTerminal()) {

            lead.setStatus(
                    LeadStatus.UNLOCKED
            );
        }


        leadRepository.save(
                lead
        );


        auditService.log(

                "LEAD_UNLOCKED",

                "LeadAssignment",

                assignment.getId(),

                null,

                AssignmentStatus.UNLOCKED
                        .name(),

                "provider="
                        + provider.getId()
                        + " fee="
                        + lead.getUnlockPrice()
        );


        return toDetail(
                assignment
        );
    }


    // ============================================================
    // MY LEADS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<LeadDetailForProviderResponse> listMyLeads(
            Long userId
    ) {

        Provider provider =
                resolveProvider(userId);

        return leadAssignmentRepository
                .findByProviderIdAndStatusInOrderByCreatedAtDesc(
                        provider.getId(),
                        List.of(
                                AssignmentStatus.UNLOCKED
                        )
                )
                .stream()
                .map(
                        this::toDetail
                )
                .toList();
    }


    // ============================================================
    // MARK CONTACTED
    // ============================================================

    @Override
    @Transactional
    public void markContacted(
            Long userId,
            Long leadAssignmentId
    ) {

        Provider provider =
                resolveProvider(userId);

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


        if (assignment.getContactedAt()
                == null) {

            assignment.setContactedAt(
                    Instant.now()
            );

            leadAssignmentRepository.save(
                    assignment
            );
        }


        Lead lead =
                assignment.getLead();


        if (lead.getStatus()
                == LeadStatus.UNLOCKED) {

            lead.setStatus(
                    LeadStatus.CONTACTED
            );

            leadRepository.save(
                    lead
            );
        }


        auditService.log(

                "LEAD_CONTACTED",

                "LeadAssignment",

                assignment.getId(),

                null,

                "CONTACTED",

                "provider="
                        + provider.getId()
        );
    }


    // ============================================================
    // HELPERS
    // ============================================================

    private Provider resolveProvider(
            Long userId
    ) {

        return providerRepository
                .findByUserId(userId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
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
                        .findById(
                                leadAssignmentId
                        )
                        .orElseThrow(
                                () ->
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
                .isAfter(
                        assignment.getExpiresAt()
                )) {

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

        Lead lead =
                assignment.getLead();


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
                        lead.getServiceCategory()
                                != null
                                ? lead.getServiceCategory()
                                .getName()
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

        Lead lead =
                assignment.getLead();


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
                        lead.getServiceCategory()
                                != null
                                ? lead.getServiceCategory()
                                .getName()
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