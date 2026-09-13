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
 * A single moving requirement captured from the customer form.
 * Lifecycle: NEW -> quality checks -> VERIFIED -> smart matching -> MATCHED -> OFFERED
 * -> (provider pays) -> UNLOCKED -> CONTACTED -> QUOTE_SENT -> NEGOTIATION -> CONVERTED/LOST.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "leads", indexes = {
        @Index(name = "idx_leads_status", columnList = "status"),
        @Index(name = "idx_leads_mobile", columnList = "customer_mobile"),
        @Index(name = "idx_leads_code", columnList = "lead_code", unique = true)
})
public class Lead extends BaseEntity {

    /** Human friendly identifier shown in admin/provider UI, e.g. L-1001. */
    @Column(name = "lead_code", nullable = false, unique = true, length = 20)
    private String leadCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // Denormalized snapshot of customer-entered data at submission time (kept even if Customer record changes)
    @Column(nullable = false, length = 120)
    private String customerName;

    @Column(name = "customer_mobile", nullable = false, length = 20)
    private String customerMobile;

    @Column(length = 150)
    private String customerEmail;

    @Column(nullable = false, length = 200)
    private String pickupLocation;

    @Column(nullable = false, length = 200)
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private LeadSource source = LeadSource.OTHER;

    private String utmSource;
    private String utmMedium;
    private String utmCampaign;
    private String utmTerm;
    private String utmContent;

    @Column(length = 60)
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private LeadStatus status = LeadStatus.NEW;

    /** Price a provider pays to unlock this lead. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unlockPrice;

    /** Business rule cap on number of providers this lead is distributed to (default 3). */
    @Builder.Default
    @Column(nullable = false)
    private int maxProviders = 3;

    @Builder.Default
    @Column(nullable = false)
    private int currentOfferCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private int currentUnlockCount = 0;

    private String qualityCheckNotes;
    private String invalidReason;
}
