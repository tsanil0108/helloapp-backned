package com.packersmovers.marketplace.dto.lead;

import com.packersmovers.marketplace.common.enums.LeadSource;
import com.packersmovers.marketplace.common.enums.LeadStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Full detail view - used by Admin/Super Admin (never exposed unmasked to a provider pre-unlock). */
@Getter
@Builder
@AllArgsConstructor
public class LeadResponse {
    private Long id;
    private String leadCode;
    private String customerName;
    private String customerMobile;
    private String customerEmail;
    private String pickupLocation;
    private String dropLocation;
    private LocalDate moveDate;
    private String propertyType;
    private String serviceCategory;
    private String inventoryNotes;
    private LeadSource source;
    private String utmSource;
    private String utmCampaign;
    private LeadStatus status;
    private BigDecimal unlockPrice;
    private int maxProviders;
    private int currentOfferCount;
    private int currentUnlockCount;
    private Instant createdAt;
}
