package com.packersmovers.marketplace.service.impl;

import com.packersmovers.marketplace.common.enums.AssignmentStatus;
import com.packersmovers.marketplace.common.enums.LeadStatus;
import com.packersmovers.marketplace.common.enums.NotificationChannel;
import com.packersmovers.marketplace.common.enums.ProviderStatus;
import com.packersmovers.marketplace.common.exception.BadRequestException;
import com.packersmovers.marketplace.common.exception.ResourceNotFoundException;
import com.packersmovers.marketplace.dto.admin.AuditLogResponse;
import com.packersmovers.marketplace.dto.admin.DashboardStatsResponse;
import com.packersmovers.marketplace.dto.admin.SettingResponse;
import com.packersmovers.marketplace.dto.admin.SettingUpdateRequest;
import com.packersmovers.marketplace.dto.common.PageResponse;
import com.packersmovers.marketplace.dto.provider.ProviderApprovalRequest;
import com.packersmovers.marketplace.dto.provider.ProviderProfileResponse;
import com.packersmovers.marketplace.entity.Provider;
import com.packersmovers.marketplace.entity.SettingConfig;
import com.packersmovers.marketplace.entity.User;
import com.packersmovers.marketplace.repository.AuditLogRepository;
import com.packersmovers.marketplace.repository.KycDocumentRepository;
import com.packersmovers.marketplace.repository.LeadAssignmentRepository;
import com.packersmovers.marketplace.repository.LeadRepository;
import com.packersmovers.marketplace.repository.ProviderRepository;
import com.packersmovers.marketplace.repository.SettingConfigRepository;
import com.packersmovers.marketplace.repository.UserRepository;
import com.packersmovers.marketplace.repository.WalletTransactionRepository;
import com.packersmovers.marketplace.service.AdminService;
import com.packersmovers.marketplace.service.AuditService;
import com.packersmovers.marketplace.service.NotificationService;
import com.packersmovers.marketplace.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final List<LeadStatus> QUALITY_REJECTED_STATUSES =
            List.of(
                    LeadStatus.NEW,
                    LeadStatus.DUPLICATE,
                    LeadStatus.INVALID
            );

    private static final List<ProviderStatus> PENDING_STATUSES =
            List.of(
                    ProviderStatus.SUBMITTED,
                    ProviderStatus.UNDER_REVIEW
            );

    private final ProviderRepository providerRepository;
    private final LeadRepository leadRepository;
    private final LeadAssignmentRepository leadAssignmentRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final SettingConfigRepository settingConfigRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final AuditService auditService;
    private final NotificationService notificationService;


    // ============================================================
    // DASHBOARD
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {

        Instant startOfToday =
                LocalDate.now(ZoneOffset.UTC)
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant();

        Instant now = Instant.now();

        BigDecimal revenue =
                walletTransactionRepository.sumLeadUnlockRevenue();

        return DashboardStatsResponse.builder()
                .totalLeads(
                        leadRepository.count()
                )
                .verifiedLeads(
                        leadRepository.countByStatusNotIn(
                                QUALITY_REJECTED_STATUSES
                        )
                )
                .convertedLeads(
                        leadRepository.countByStatus(
                                LeadStatus.CONVERTED
                        )
                )
                .activeProviders(
                        providerRepository.countByStatus(
                                ProviderStatus.APPROVED
                        )
                )
                .pendingProviderApprovals(
                        providerRepository.countByStatusIn(
                                PENDING_STATUSES
                        )
                )
                .totalRevenue(
                        revenue != null
                                ? revenue
                                : BigDecimal.ZERO
                )
                .unlocksToday(
                        leadAssignmentRepository
                                .countByStatusAndUnlockedAtBetween(
                                        AssignmentStatus.UNLOCKED,
                                        startOfToday,
                                        now
                                )
                )
                .build();
    }


    // ============================================================
    // PROVIDERS
    // ============================================================

    /*
     * IMPORTANT:
     *
     * Provider has lazy relationships:
     *   provider.user
     *   provider.serviceAreas
     *   provider.serviceCategories
     *
     * listProviders() previously ran without a transaction.
     * toProfile() then accessed those lazy relationships after
     * the Hibernate session had already closed.
     *
     * Keeping the complete provider mapping inside a read-only
     * transaction prevents LazyInitializationException.
     */

    @Override
    @Transactional(readOnly = true)
    public List<ProviderProfileResponse> listProviders(
            String status
    ) {

        List<Provider> providers =
                (status == null || status.isBlank())
                        ? providerRepository.findAll()
                        : providerRepository.findByStatus(
                        ProviderStatus.valueOf(
                                status.toUpperCase()
                        )
                );

        return providers
                .stream()
                .map(this::toProfile)
                .toList();
    }


    // ============================================================
    // PROVIDER ACTION
    // ============================================================

    @Override
    @Transactional
    public ProviderProfileResponse actOnProvider(
            Long adminUserId,
            Long providerId,
            ProviderApprovalRequest request
    ) {

        Provider provider =
                providerRepository.findById(providerId)
                        .orElseThrow(
                                () -> ResourceNotFoundException.of(
                                        "Provider",
                                        providerId
                                )
                        );

        User admin =
                userRepository.findById(adminUserId)
                        .orElseThrow(
                                () -> ResourceNotFoundException.of(
                                        "User",
                                        adminUserId
                                )
                        );

        ProviderStatus oldStatus =
                provider.getStatus();

        String notifyTitle;
        String notifyMessage;


        switch (request.getAction()) {

            case APPROVE -> {

                if (!kycDocumentRepository
                        .existsByProviderId(providerId)) {

                    throw new BadRequestException(
                            "Provider has not submitted any KYC documents yet"
                    );
                }

                provider.setStatus(
                        ProviderStatus.APPROVED
                );

                provider.setVerifiedBadge(
                        true
                );

                provider.setApprovedAt(
                        Instant.now()
                );

                provider.setApprovedBy(
                        admin
                );

                provider.setRejectionReason(
                        null
                );

                notifyTitle =
                        "You're approved!";

                notifyMessage =
                        "Your provider account is verified and active. "
                                + "You can now receive matching leads.";
            }


            case REJECT -> {

                provider.setStatus(
                        ProviderStatus.REJECTED
                );

                provider.setVerifiedBadge(
                        false
                );

                provider.setRejectionReason(
                        request.getReason()
                );

                notifyTitle =
                        "Registration rejected";

                notifyMessage =
                        "Your provider registration was rejected. Reason: "
                                + (
                                request.getReason() != null
                                        ? request.getReason()
                                        : "Not specified"
                        );
            }


            case REQUEST_CHANGES -> {

                provider.setStatus(
                        ProviderStatus.UNDER_REVIEW
                );

                provider.setRejectionReason(
                        request.getReason()
                );

                notifyTitle =
                        "Changes requested";

                notifyMessage =
                        "Please update your profile/KYC: "
                                + (
                                request.getReason() != null
                                        ? request.getReason()
                                        : "See admin remarks"
                        );
            }


            case SUSPEND -> {

                provider.setStatus(
                        ProviderStatus.SUSPENDED
                );

                provider.setVerifiedBadge(
                        false
                );

                provider.setRejectionReason(
                        request.getReason()
                );

                notifyTitle =
                        "Account suspended";

                notifyMessage =
                        "Your account has been suspended. Reason: "
                                + (
                                request.getReason() != null
                                        ? request.getReason()
                                        : "Not specified"
                        );
            }


            case REACTIVATE -> {

                if (oldStatus != ProviderStatus.SUSPENDED) {

                    throw new BadRequestException(
                            "Only a suspended provider can be reactivated"
                    );
                }

                provider.setStatus(
                        ProviderStatus.APPROVED
                );

                provider.setVerifiedBadge(
                        true
                );

                provider.setRejectionReason(
                        null
                );

                notifyTitle =
                        "Account reactivated";

                notifyMessage =
                        "Your account has been reactivated. "
                                + "You can receive leads again.";
            }


            default -> throw new BadRequestException(
                    "Unknown action: "
                            + request.getAction()
            );
        }


        provider =
                providerRepository.save(provider);


        auditService.log(
                "PROVIDER_" + request.getAction().name(),
                "Provider",
                provider.getId(),
                oldStatus.name(),
                provider.getStatus().name(),
                request.getReason()
        );


        notificationService.notify(
                provider.getUser(),
                notifyTitle,
                notifyMessage,
                NotificationChannel.IN_APP,
                "PROVIDER:" + provider.getId()
        );


        return toProfile(provider);
    }


    // ============================================================
    // SETTINGS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<SettingResponse> listSettings() {

        return settingConfigRepository
                .findAll()
                .stream()
                .map(
                        s -> SettingResponse.builder()
                                .id(s.getId())
                                .settingKey(
                                        s.getSettingKey()
                                )
                                .settingValue(
                                        s.getSettingValue()
                                )
                                .description(
                                        s.getDescription()
                                )
                                .build()
                )
                .toList();
    }


    // ============================================================
    // UPSERT SETTING
    // ============================================================

    @Override
    @Transactional
    public SettingResponse upsertSetting(
            SettingUpdateRequest request
    ) {

        SettingConfig setting =
                settingConfigRepository
                        .findBySettingKey(
                                request.getSettingKey()
                        )
                        .orElseGet(
                                () -> SettingConfig.builder()
                                        .settingKey(
                                                request.getSettingKey()
                                        )
                                        .build()
                        );


        String oldValue =
                setting.getSettingValue();


        setting.setSettingValue(
                request.getSettingValue()
        );


        if (request.getDescription() != null) {

            setting.setDescription(
                    request.getDescription()
            );
        }


        setting =
                settingConfigRepository.save(
                        setting
                );


        auditService.log(
                "SETTING_UPDATED",
                "SettingConfig",
                setting.getId(),
                oldValue,
                setting.getSettingValue(),
                setting.getSettingKey()
        );


        return SettingResponse.builder()
                .id(setting.getId())
                .settingKey(
                        setting.getSettingKey()
                )
                .settingValue(
                        setting.getSettingValue()
                )
                .description(
                        setting.getDescription()
                )
                .build();
    }


    // ============================================================
    // AUDIT LOGS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> listAuditLogs(
            int page,
            int size
    ) {

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
                auditLogRepository
                        .findAllByOrderByCreatedAtDesc(
                                pageable
                        )
                        .map(
                                log ->
                                        AuditLogResponse.builder()
                                                .id(log.getId())
                                                .actorId(
                                                        log.getActorId()
                                                )
                                                .actorRole(
                                                        log.getActorRole()
                                                )
                                                .actorName(
                                                        log.getActorName()
                                                )
                                                .action(
                                                        log.getAction()
                                                )
                                                .entityType(
                                                        log.getEntityType()
                                                )
                                                .entityId(
                                                        log.getEntityId()
                                                )
                                                .oldValue(
                                                        log.getOldValue()
                                                )
                                                .newValue(
                                                        log.getNewValue()
                                                )
                                                .remarks(
                                                        log.getRemarks()
                                                )
                                                .createdAt(
                                                        log.getCreatedAt()
                                                )
                                                .build()
                        );


        return PageResponse.from(result);
    }


    // ============================================================
    // PROVIDER PROFILE MAPPER
    // ============================================================

    private ProviderProfileResponse toProfile(
            Provider provider
    ) {

        /*
         * This method is intentionally called from methods that
         * have an active transaction.
         *
         * Therefore Hibernate can safely initialize:
         *
         * provider.getUser()
         * provider.getServiceAreas()
         * provider.getServiceCategories()
         */

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

                .build();
    }
}