package com.packersmovers.marketplace.dto.lead;

import com.packersmovers.marketplace.common.enums.AssignmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

/** Returned to a provider ONLY after successful unlock - contact details are now revealed. */
@Getter
@Builder
@AllArgsConstructor
public class LeadDetailForProviderResponse {
    private Long leadAssignmentId;
    private String leadCode;
    private String customerName;
    private String customerMobile;
    private String customerEmail;
    private String pickupLocation;
    private String dropLocation;
    private LocalDate moveDate;
    private String propertyType;
    private String inventoryNotes;
    private String serviceCategory;
    private AssignmentStatus assignmentStatus;
    private Instant unlockedAt;
    private Instant contactedAt;
}
