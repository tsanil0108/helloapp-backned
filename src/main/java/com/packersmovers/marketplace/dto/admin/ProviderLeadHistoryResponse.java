package com.packersmovers.marketplace.dto.admin;

import com.packersmovers.marketplace.common.enums.AssignmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderLeadHistoryResponse {

    private Long leadAssignmentId;

    private Long leadId;

    private String leadCode;

    private String customerName;

    private String customerMobile;

    private String pickupLocation;

    private String dropLocation;

    private String serviceCategory;

    private String propertyType;

    private AssignmentStatus status;

    private BigDecimal unlockFeeCharged;

    private Instant createdAt;

    private Instant viewedAt;

    private Instant unlockedAt;

    private Instant contactedAt;

    private String contactMethod;

    private Integer contactAttemptCount;

    private Instant quoteSentAt;

    private Instant bookedAt;

    private Instant serviceStartedAt;

    private Instant completedAt;

    private Instant lostAt;

    private String lostReason;

    private String completionNotes;

    private Instant updatedAt;
}