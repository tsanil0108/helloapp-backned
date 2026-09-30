package com.packersmovers.marketplace.dto.lead;

import com.packersmovers.marketplace.common.enums.AssignmentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class LeadAssignmentHistoryResponse {

    private Long leadAssignmentId;

    private Long leadId;

    private String leadCode;

    private AssignmentStatus status;

    private BigDecimal unlockFeeCharged;

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

    private Instant createdAt;
    private Instant updatedAt;
}