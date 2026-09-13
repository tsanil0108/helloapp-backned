package com.packersmovers.marketplace.service.impl;

import com.packersmovers.marketplace.common.enums.LeadSource;
import com.packersmovers.marketplace.common.enums.LeadStatus;
import com.packersmovers.marketplace.common.exception.BadRequestException;
import com.packersmovers.marketplace.common.exception.DuplicateLeadException;
import com.packersmovers.marketplace.common.exception.ResourceNotFoundException;
import com.packersmovers.marketplace.config.MarketplaceProperties;
import com.packersmovers.marketplace.dto.common.PageResponse;
import com.packersmovers.marketplace.dto.lead.CustomerLeadRequest;
import com.packersmovers.marketplace.dto.lead.LeadResponse;
import com.packersmovers.marketplace.dto.lead.LeadStatusUpdateRequest;
import com.packersmovers.marketplace.entity.Customer;
import com.packersmovers.marketplace.entity.Lead;
import com.packersmovers.marketplace.entity.ServiceArea;
import com.packersmovers.marketplace.entity.ServiceCategory;
import com.packersmovers.marketplace.repository.CustomerRepository;
import com.packersmovers.marketplace.repository.LeadRepository;
import com.packersmovers.marketplace.repository.ServiceAreaRepository;
import com.packersmovers.marketplace.repository.ServiceCategoryRepository;
import com.packersmovers.marketplace.service.AuditService;
import com.packersmovers.marketplace.service.CaptchaVerificationService;
import com.packersmovers.marketplace.service.LeadService;
import com.packersmovers.marketplace.service.MatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadServiceImpl implements LeadService {

    private final LeadRepository leadRepository;

    private final CustomerRepository customerRepository;

    private final ServiceAreaRepository serviceAreaRepository;

    private final ServiceCategoryRepository serviceCategoryRepository;

    private final MatchingService matchingService;

    private final AuditService auditService;

    private final MarketplaceProperties marketplaceProperties;

    private final CaptchaVerificationService captchaVerificationService;


    // ============================================================
    // CREATE LEAD
    // ============================================================

    @Override
    @Transactional
    public LeadResponse createLead(
            CustomerLeadRequest request,
            String ipAddress
    ) {

        if (request == null) {
            throw new BadRequestException(
                    "Lead request is required"
            );
        }

        // ========================================================
        // CAPTCHA
        // ========================================================

        verifyCaptcha(
                request.getCaptchaToken(),
                ipAddress
        );

        // ========================================================
        // RATE LIMIT
        // ========================================================

        enforceRateLimit(
                request.getMobile()
        );

        // ========================================================
        // DUPLICATE CHECK
        // ========================================================

        checkDuplicate(request);


        // ========================================================
        // SERVICE CATEGORY
        // ========================================================

        ServiceCategory serviceCategory =
                serviceCategoryRepository
                        .findByNameIgnoreCase(
                                request.getServiceCategory()
                        )
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Unknown service: "
                                                + request.getServiceCategory()
                                )
                        );


        // ========================================================
        // PICKUP SERVICE AREA
        // ========================================================

        ServiceArea pickupArea =
                resolveServiceArea(
                        request.getPickupLocation()
                );


        // ========================================================
        // CREATE CUSTOMER
        // ========================================================

        Customer customer =
                Customer.builder()
                        .fullName(
                                request.getFullName()
                        )
                        .mobile(
                                request.getMobile()
                        )
                        .email(
                                request.getEmail()
                        )
                        .build();

        customer =
                customerRepository.save(customer);


        // ========================================================
        // CREATE LEAD
        // ========================================================

        Lead lead =
                Lead.builder()
                        .customer(customer)
                        .customerName(
                                request.getFullName()
                        )
                        .customerMobile(
                                request.getMobile()
                        )
                        .customerEmail(
                                request.getEmail()
                        )
                        .pickupLocation(
                                request.getPickupLocation()
                        )
                        .dropLocation(
                                request.getDropLocation()
                        )
                        .moveDate(
                                request.getMoveDate()
                        )
                        .propertyType(
                                request.getPropertyType()
                        )
                        .serviceCategory(
                                serviceCategory
                        )
                        .pickupServiceArea(
                                pickupArea
                        )
                        .inventoryNotes(
                                request.getInventoryNotes()
                        )
                        .consentGiven(
                                request.isConsentGiven()
                        )
                        .source(
                                parseSource(
                                        request.getSource()
                                )
                        )
                        .utmSource(
                                request.getUtmSource()
                        )
                        .utmMedium(
                                request.getUtmMedium()
                        )
                        .utmCampaign(
                                request.getUtmCampaign()
                        )
                        .utmTerm(
                                request.getUtmTerm()
                        )
                        .utmContent(
                                request.getUtmContent()
                        )
                        .ipAddress(
                                ipAddress
                        )
                        .status(
                                LeadStatus.NEW
                        )
                        .unlockPrice(
                                marketplaceProperties
                                        .defaultLeadUnlockFee()
                        )
                        .maxProviders(
                                marketplaceProperties
                                        .defaultMaxProvidersPerLead()
                        )
                        .build();

        lead =
                leadRepository.save(lead);


        // ========================================================
        // LEAD CODE
        // ========================================================

        lead.setLeadCode(
                "L-" + (1000 + lead.getId())
        );


        // ========================================================
        // VERIFIED
        // ========================================================

        lead.setStatus(
                LeadStatus.VERIFIED
        );

        lead =
                leadRepository.save(lead);


        // ========================================================
        // AUDIT
        // ========================================================

        auditService.log(
                "LEAD_CREATED",
                "Lead",
                lead.getId(),
                null,
                LeadStatus.VERIFIED.name(),
                "Source=" + lead.getSource()
        );


        // ========================================================
        // MATCHING
        // ========================================================

        if (pickupArea != null) {

            /*
             * IMPORTANT FIX
             *
             * `lead` is reassigned multiple times above.
             * Therefore it cannot be directly referenced inside
             * the lambda used by orElseThrow().
             *
             * Capture the ID in a separate final variable.
             */
            final Long savedLeadId =
                    lead.getId();

            matchingService.matchAndOffer(
                    lead
            );

            lead =
                    leadRepository
                            .findById(
                                    savedLeadId
                            )
                            .orElseThrow(() ->
                                    ResourceNotFoundException.of(
                                            "Lead",
                                            savedLeadId
                                    )
                            );

        } else {

            log.warn(
                    "Lead {} has no resolvable service area yet; matching deferred",
                    lead.getLeadCode()
            );
        }


        // ========================================================
        // RESPONSE
        // ========================================================

        return toResponse(lead);
    }


    // ============================================================
    // GET LEAD BY CODE
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public LeadResponse getByCode(
            String leadCode
    ) {

        if (leadCode == null
                || leadCode.isBlank()) {

            throw new BadRequestException(
                    "Lead code is required"
            );
        }

        Lead lead =
                leadRepository
                        .findByLeadCode(
                                leadCode.trim()
                        )
                        .orElseThrow(() ->
                                ResourceNotFoundException.of(
                                        "Lead",
                                        leadCode
                                )
                        );

        return toResponse(lead);
    }


    // ============================================================
    // SEARCH
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LeadResponse> search(
            String status,
            int page,
            int size
    ) {

        if (page < 0) {
            page = 0;
        }

        if (size < 1) {
            size = 20;
        }

        if (size > 100) {
            size = 100;
        }

        var pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                );

        var result =
                (status == null || status.isBlank())
                        ? leadRepository.findAll(pageable)
                        : leadRepository.findByStatus(
                        LeadStatus.valueOf(
                                status.trim().toUpperCase()
                        ),
                        pageable
                );

        return PageResponse.from(
                result.map(
                        this::toResponse
                )
        );
    }


    // ============================================================
    // MY REQUESTS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<LeadResponse> myRequests(
            String mobile
    ) {

        if (mobile == null
                || mobile.isBlank()) {

            throw new BadRequestException(
                    "Mobile number is required"
            );
        }

        return leadRepository
                .findByCustomerMobileOrderByCreatedAtDesc(
                        mobile.trim()
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }


    // ============================================================
    // UPDATE STATUS
    // ============================================================

    @Override
    @Transactional
    public LeadResponse updateStatus(
            Long leadId,
            LeadStatusUpdateRequest request
    ) {

        if (leadId == null) {

            throw new BadRequestException(
                    "Lead ID is required"
            );
        }

        if (request == null) {

            throw new BadRequestException(
                    "Status update request is required"
            );
        }

        if (request.getStatus() == null) {

            throw new BadRequestException(
                    "Lead status is required"
            );
        }

        Lead lead =
                leadRepository
                        .findById(leadId)
                        .orElseThrow(() ->
                                ResourceNotFoundException.of(
                                        "Lead",
                                        leadId
                                )
                        );

        LeadStatus oldStatus =
                lead.getStatus();

        LeadStatus newStatus =
                request.getStatus();

        lead.setStatus(
                newStatus
        );

        if (request.getNotes() != null) {

            lead.setQualityCheckNotes(
                    request.getNotes()
            );
        }

        lead =
                leadRepository.save(lead);

        auditService.log(
                "LEAD_STATUS_CHANGED",
                "Lead",
                lead.getId(),
                oldStatus != null
                        ? oldStatus.name()
                        : null,
                newStatus.name(),
                request.getNotes()
        );

        return toResponse(lead);
    }


    // ============================================================
    // CAPTCHA
    // ============================================================

    private void verifyCaptcha(
            String captchaToken,
            String ipAddress
    ) {

        captchaVerificationService.verify(
                captchaToken,
                ipAddress
        );
    }


    // ============================================================
    // RATE LIMIT
    // ============================================================

    private void enforceRateLimit(
            String mobile
    ) {

        if (mobile == null
                || mobile.isBlank()) {

            throw new BadRequestException(
                    "Mobile number is required"
            );
        }

        Instant since =
                Instant.now().minus(
                        1,
                        ChronoUnit.DAYS
                );

        long countToday =
                leadRepository
                        .countByCustomerMobileAndCreatedAtAfter(
                                mobile.trim(),
                                since
                        );

        if (countToday >=
                marketplaceProperties
                        .rateLimitPerMobilePerDay()) {

            throw new BadRequestException(
                    "Too many requests from this mobile number today. "
                            + "Please try again tomorrow."
            );
        }
    }


    // ============================================================
    // DUPLICATE CHECK
    // ============================================================

    private void checkDuplicate(
            CustomerLeadRequest request
    ) {

        Instant since =
                Instant.now().minus(
                        marketplaceProperties
                                .duplicateLeadWindowHours(),
                        ChronoUnit.HOURS
                );

        List<Lead> duplicates =
                leadRepository.findPossibleDuplicates(
                        request.getMobile(),
                        request.getPickupLocation(),
                        request.getDropLocation(),
                        since
                );

        if (!duplicates.isEmpty()) {

            throw new DuplicateLeadException(
                    "A similar request was already submitted recently. "
                            + "Our team / matched providers will reach out soon."
            );
        }
    }


    // ============================================================
    // SERVICE AREA RESOLUTION
    // ============================================================

    private ServiceArea resolveServiceArea(
            String pickupLocation
    ) {

        if (pickupLocation == null
                || pickupLocation.isBlank()) {

            return null;
        }

        String normalizedLocation =
                pickupLocation
                        .trim()
                        .toLowerCase();

        List<ServiceArea> activeAreas =
                serviceAreaRepository
                        .findByActiveTrue();

        for (ServiceArea area : activeAreas) {

            if (area.getCityOrRegion() == null) {
                continue;
            }

            String cityOrRegion =
                    area.getCityOrRegion()
                            .trim()
                            .toLowerCase();

            if (normalizedLocation.contains(
                    cityOrRegion
            )) {

                return area;
            }
        }

        return null;
    }


    // ============================================================
    // LEAD SOURCE
    // ============================================================

    private LeadSource parseSource(
            String source
    ) {

        if (source == null
                || source.isBlank()) {

            return LeadSource.OTHER;
        }

        try {

            return LeadSource.valueOf(
                    source.trim().toUpperCase()
            );

        } catch (IllegalArgumentException exception) {

            return LeadSource.OTHER;
        }
    }


    // ============================================================
    // RESPONSE MAPPER
    // ============================================================

    private LeadResponse toResponse(
            Lead lead
    ) {

        return LeadResponse.builder()
                .id(
                        lead.getId()
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
                .serviceCategory(
                        lead.getServiceCategory() != null
                                ? lead.getServiceCategory().getName()
                                : null
                )
                .inventoryNotes(
                        lead.getInventoryNotes()
                )
                .source(
                        lead.getSource()
                )
                .utmSource(
                        lead.getUtmSource()
                )
                .utmCampaign(
                        lead.getUtmCampaign()
                )
                .status(
                        lead.getStatus()
                )
                .unlockPrice(
                        lead.getUnlockPrice()
                )
                .maxProviders(
                        lead.getMaxProviders()
                )
                .currentOfferCount(
                        lead.getCurrentOfferCount()
                )
                .currentUnlockCount(
                        lead.getCurrentUnlockCount()
                )
                .createdAt(
                        lead.getCreatedAt()
                )
                .build();
    }
}