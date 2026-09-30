package com.packersmovers.marketplace.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderPerformanceResponse {

    private Long providerId;

    private String companyName;

    private String ownerName;

    private String email;

    private String mobile;

    private String status;

    private long assigned;

    private long viewed;

    private long unlocked;

    private long contacted;

    private long quoteSent;

    private long negotiation;

    private long booked;

    private long serviceInProgress;

    private long completed;

    private long lost;

    private long expired;

    private long cancelled;

    private BigDecimal totalUnlockSpend;

    private long totalContactAttempts;

    private double bookingRate;

    private double completionRate;
}