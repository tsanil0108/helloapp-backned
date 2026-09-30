package com.packersmovers.marketplace.entity;

import com.packersmovers.marketplace.common.enums.LeadSource;
import com.packersmovers.marketplace.common.enums.LeadStatus;
import com.packersmovers.marketplace.common.util.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single moving requirement captured from a customer.
 *
 * Customer-side lead lifecycle:
 *
 * NEW
 * -> VERIFIED
 * -> MATCHED
 *
 * Provider-side activity is tracked separately in LeadAssignment.
 *
 * LeadAssignment lifecycle:
 *
 * OFFERED
 * -> VIEWED
 * -> UNLOCKED
 * -> CONTACTED
 * -> QUOTE_SENT
 * -> NEGOTIATION
 * -> BOOKED
 * -> SERVICE_IN_PROGRESS
 * -> COMPLETED
 *
 * Possible terminal outcomes:
 *
 * INVALID
 * DUPLICATE
 * EXPIRED
 * CANCELLED
 * LOST
 * REFUNDED
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(
        name = "leads",
        indexes = {
                @Index(
                        name = "idx_leads_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_leads_mobile",
                        columnList = "customer_mobile"
                ),
                @Index(
                        name = "idx_leads_code",
                        columnList = "lead_code",
                        unique = true
                )
        }
)
public class Lead extends BaseEntity {

    /**
     * Human-friendly identifier shown in
     * admin/provider UI.
     *
     * Example:
     * L-1001
     */
    @Column(
            name = "lead_code",
            nullable = false,
            unique = true,
            length = 20
    )
    private String leadCode;

    /**
     * Optional registered customer relation.
     *
     * Website/Google/Meta leads may be
     * anonymous, so this remains nullable.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    /**
     * Denormalized customer snapshot.
     *
     * Kept even if the Customer record changes later.
     */
    @Column(
            nullable = false,
            length = 120
    )
    private String customerName;

    @Column(
            name = "customer_mobile",
            nullable = false,
            length = 20
    )
    private String customerMobile;

    @Column(length = 150)
    private String customerEmail;

    @Column(
            nullable = false,
            length = 200
    )
    private String pickupLocation;

    @Column(
            nullable = false,
            length = 200
    )
    private String dropLocation;

    private LocalDate moveDate;

    @Column(length = 60)
    private String propertyType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_category_id")
    private ServiceCategory serviceCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_service_area_id")
    private ServiceArea pickupServiceArea;

    @Column(columnDefinition = "TEXT")
    private String inventoryNotes;

    @Builder.Default
    @Column(nullable = false)
    private boolean consentGiven = true;

    /**
     * Original acquisition source.
     *
     * Examples:
     * WEBSITE
     * GOOGLE
     * META
     * OTHER
     */
    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    @Builder.Default
    private LeadSource source = LeadSource.OTHER;

    /**
     * Marketing attribution.
     */
    private String utmSource;

    private String utmMedium;

    private String utmCampaign;

    private String utmTerm;

    private String utmContent;

    @Column(length = 60)
    private String ipAddress;

    /**
     * Overall lead lifecycle.
     *
     * Provider-specific lifecycle is tracked
     * independently inside LeadAssignment.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    @Builder.Default
    private LeadStatus status = LeadStatus.NEW;

    /**
     * Price a provider pays to unlock this lead.
     */
    @Column(
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal unlockPrice;

    /**
     * Maximum number of providers that may
     * receive this lead.
     *
     * Default = 3.
     */
    @Builder.Default
    @Column(nullable = false)
    private int maxProviders = 3;

    /**
     * Number of provider assignments/offers
     * created for this lead.
     */
    @Builder.Default
    @Column(nullable = false)
    private int currentOfferCount = 0;

    /**
     * Number of providers that successfully
     * unlocked this lead.
     */
    @Builder.Default
    @Column(nullable = false)
    private int currentUnlockCount = 0;

    /**
     * Internal quality-control information.
     */
    private String qualityCheckNotes;

    /**
     * Reason when a lead is rejected/invalid.
     */
    private String invalidReason;

    /**
     * Optional customer notification destination.
     *
     * Useful for anonymous website/Google/Meta
     * leads where there is no Customer account.
     */
    @Column(
            name = "customer_notification_email",
            length = 255
    )
    private String customerNotificationEmail;

    /**
     * Notification mobile number.
     *
     * Normally this will contain the same
     * customer mobile submitted with the lead,
     * but keeping it separately allows the
     * notification channel to evolve later.
     */
    @Column(
            name = "customer_notification_mobile",
            length = 30
    )
    private String customerNotificationMobile;
}