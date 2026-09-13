package com.packersmovers.marketplace.dto.lead;

import com.packersmovers.marketplace.common.enums.AssignmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Masked "New Lead" card shown to a provider BEFORE unlock.
 * Only route (city-level), service, date, property type and price are visible - no name/phone.
 */
@Getter
@Builder
@AllArgsConstructor
public class LeadCardResponse {
    private Long leadAssignmentId;
    private String leadCode;
    private String pickupAreaMasked;   // e.g. "Mumbai (Andheri) -> ***"
    private String dropAreaMasked;
    private String serviceCategory;
    private LocalDate moveDate;
    private String propertyType;
    private BigDecimal unlockPrice;
    private AssignmentStatus status;
}
