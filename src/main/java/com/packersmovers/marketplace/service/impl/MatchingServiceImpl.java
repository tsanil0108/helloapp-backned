package com.packersmovers.marketplace.service.impl;

import com.packersmovers.marketplace.common.enums.AssignmentStatus;
import com.packersmovers.marketplace.common.enums.LeadStatus;
import com.packersmovers.marketplace.common.enums.NotificationChannel;
import com.packersmovers.marketplace.config.MarketplaceProperties;
import com.packersmovers.marketplace.entity.Lead;
import com.packersmovers.marketplace.entity.LeadAssignment;
import com.packersmovers.marketplace.entity.Provider;
import com.packersmovers.marketplace.repository.LeadAssignmentRepository;
import com.packersmovers.marketplace.repository.LeadRepository;
import com.packersmovers.marketplace.repository.ProviderRepository;
import com.packersmovers.marketplace.repository.WalletRepository;
import com.packersmovers.marketplace.repository.ProviderSubscriptionRepository;
import com.packersmovers.marketplace.service.AuditService;
import com.packersmovers.marketplace.service.MatchingService;
import com.packersmovers.marketplace.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Maximum 3 provider distribution engine.
 *
 * A lead will never be offered to more than the configured
 * maximum number of providers.
 *
 * Current ranking strategy:
 * priority score -> rating
 *
 * The provider repository is responsible for returning eligible
 * providers according to the configured matching rules.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingServiceImpl implements MatchingService {

    private final ProviderRepository providerRepository;

    private final LeadRepository leadRepository;

    private final LeadAssignmentRepository leadAssignmentRepository;

    private final NotificationService notificationService;

    private final AuditService auditService;

    private final MarketplaceProperties marketplaceProperties;

    private final WalletRepository walletRepository;

    private final ProviderSubscriptionRepository providerSubscriptionRepository;


    // ============================================================
    // MATCH AND OFFER
    // ============================================================

    @Override
    @Transactional
    public void matchAndOffer(Lead lead) {

        if (lead == null) {
            throw new IllegalArgumentException(
                    "Lead cannot be null"
            );
        }

        /*
         * Capture the incoming lead ID BEFORE replacing the
         * method parameter.
         *
         * This ID is final/effectively-final and can safely be
         * used inside lambda expressions.
         */
        final Long leadId = lead.getId();

        if (leadId == null) {
            throw new IllegalArgumentException(
                    "Lead ID cannot be null"
            );
        }

        /*
         * Lock the lead row.
         *
         * This prevents two concurrent matching transactions
         * from distributing more than the configured provider cap.
         */
        Lead lockedLead =
                leadRepository
                        .findByIdForUpdate(leadId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Lead not found: " + leadId
                                )
                        );

        /*
         * Use a final reference from this point onward.
         *
         * Nothing inside this method needs to reassign the
         * locked lead.
         */
        final Lead currentLead = lockedLead;


        // ========================================================
        // BASIC MATCHING VALIDATION
        // ========================================================

        if (currentLead.getServiceCategory() == null
                || currentLead.getPickupServiceArea() == null) {

            log.warn(
                    "Lead {} missing service category/area - cannot match",
                    currentLead.getLeadCode()
            );

            return;
        }


        // ========================================================
        // PROVIDER CANDIDATES
        // ========================================================

        List<Provider> candidates =
                providerRepository.findEligibleProviders(
                        currentLead
                                .getPickupServiceArea()
                                .getId(),

                        currentLead
                                .getServiceCategory()
                                .getId()
                );

        if (candidates == null
                || candidates.isEmpty()) {

            log.info(
                    "No eligible providers found for lead {}",
                    currentLead.getLeadCode()
            );

            currentLead.setStatus(
                    LeadStatus.MATCHED
            );

            leadRepository.save(currentLead);

            auditService.log(
                    "LEAD_MATCHED",
                    "Lead",
                    currentLead.getId(),
                    null,
                    LeadStatus.MATCHED.name(),
                    "No eligible providers available"
            );

            return;
        }


        // ========================================================
        // CALCULATE REMAINING SLOTS
        // ========================================================

        int maxProviders =
                currentLead.getMaxProviders();

        int currentOfferCount =
                currentLead.getCurrentOfferCount();

        int slotsRemaining =
                maxProviders - currentOfferCount;

        if (slotsRemaining <= 0) {

            log.info(
                    "Lead {} already at max distribution ({})",
                    currentLead.getLeadCode(),
                    maxProviders
            );

            return;
        }


        // ========================================================
        // OFFER EXPIRY
        // ========================================================

        Instant now =
                Instant.now();

        Instant expiresAt =
                now.plus(
                        marketplaceProperties
                                .leadOfferExpiryHours(),
                        ChronoUnit.HOURS
                );


        // ========================================================
        // DISTRIBUTE
        // ========================================================

        int offered = 0;

        for (Provider provider : candidates) {

            /*
             * HARD CAP:
             *
             * Once max providers have been offered,
             * absolutely no fourth provider is added.
             */
            if (offered >= slotsRemaining) {

                break;
            }

            if (provider == null
                    || provider.getId() == null) {

                continue;
            }

            Long providerId =
                    provider.getId();


            // ====================================================
            // DUPLICATE ASSIGNMENT CHECK
            // ====================================================

            Optional<LeadAssignment> existingAssignment =
                    leadAssignmentRepository
                            .findByLeadIdAndProviderId(
                                    currentLead.getId(),
                                    providerId
                            );

            if (existingAssignment.isPresent()) {

                log.debug(
                        "Provider {} already has assignment for lead {}",
                        providerId,
                        currentLead.getId()
                );

                continue;
            }


            // ====================================================
            // FINANCIAL ELIGIBILITY
            // ====================================================

            if (!isFinanciallyEligible(
                    providerId,
                    currentLead.getUnlockPrice()
            )) {

                log.debug(
                        "Provider {} is not financially eligible for lead {}",
                        providerId,
                        currentLead.getId()
                );

                continue;
            }


            // ====================================================
            // CREATE ASSIGNMENT
            // ====================================================

            LeadAssignment assignment =
                    LeadAssignment.builder()
                            .lead(currentLead)
                            .provider(provider)
                            .status(
                                    AssignmentStatus.OFFERED
                            )
                            .offeredAt(now)
                            .expiresAt(expiresAt)
                            .build();

            leadAssignmentRepository.save(
                    assignment
            );

            offered++;


            // ====================================================
            // NOTIFY PROVIDER
            // ====================================================

            String serviceName =
                    currentLead
                            .getServiceCategory()
                            .getName();

            String leadReference =
                    "LEAD:" + currentLead.getId();

            notificationService.notify(
                    provider.getUser(),
                    "New matching lead available",
                    "A new "
                            + serviceName
                            + " lead is available in your service area.",
                    NotificationChannel.PUSH,
                    leadReference
            );
        }


        // ========================================================
        // UPDATE LEAD COUNTERS
        // ========================================================

        int newOfferCount =
                currentOfferCount + offered;

        currentLead.setCurrentOfferCount(
                newOfferCount
        );


        // ========================================================
        // UPDATE STATUS
        // ========================================================

        if (offered > 0) {

            currentLead.setStatus(
                    LeadStatus.OFFERED
            );

        } else {

            currentLead.setStatus(
                    LeadStatus.MATCHED
            );
        }


        // ========================================================
        // SAVE LEAD
        // ========================================================

        leadRepository.save(
                currentLead
        );


        // ========================================================
        // AUDIT
        // ========================================================

        auditService.log(
                "LEAD_MATCHED",
                "Lead",
                currentLead.getId(),
                null,
                currentLead.getStatus().name(),
                "Offered to "
                        + offered
                        + " provider(s), cap="
                        + currentLead.getMaxProviders()
        );


        log.info(
                "Lead {} matched to {} provider(s); total offers={}, cap={}",
                currentLead.getLeadCode(),
                offered,
                currentLead.getCurrentOfferCount(),
                currentLead.getMaxProviders()
        );
    }


    // ============================================================
    // FINANCIAL ELIGIBILITY
    // ============================================================

    private boolean isFinanciallyEligible(
            Long providerId,
            BigDecimal price
    ) {

        if (providerId == null) {
            return false;
        }

        if (price == null
                || price.compareTo(BigDecimal.ZERO) < 0) {

            return false;
        }


        // ========================================================
        // WALLET CHECK
        // ========================================================

        boolean walletReady =
                walletRepository
                        .findByProviderId(providerId)
                        .map(wallet -> {

                            if (wallet.getBalance() == null) {
                                return false;
                            }

                            return wallet
                                    .getBalance()
                                    .compareTo(price) >= 0;
                        })
                        .orElse(false);

        if (!walletReady) {

            return false;
        }


        // ========================================================
        // SUBSCRIPTION CHECK
        // ========================================================

        boolean subscriptionReady =
                providerSubscriptionRepository
                        .findFirstByProviderIdAndActiveTrueOrderByEndDateDesc(
                                providerId
                        )
                        .map(subscription -> {

                            /*
                             * If endDate is null, treat the active
                             * subscription as non-expiring.
                             */
                            LocalDate endDate =
                                    subscription.getEndDate();

                            if (endDate == null) {
                                return true;
                            }

                            return !endDate.isBefore(
                                    LocalDate.now()
                            );
                        })
                        .orElse(false);

        return subscriptionReady;
    }
}