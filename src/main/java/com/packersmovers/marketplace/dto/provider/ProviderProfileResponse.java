package com.packersmovers.marketplace.dto.provider;

import com.packersmovers.marketplace.common.enums.ProviderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ProviderProfileResponse {
    private Long id;
    private String companyName;
    private String ownerName;
    private String email;
    private String mobile;
    private String gstNumber;
    private String panNumber;
    private ProviderStatus status;
    private boolean verifiedBadge;
    private BigDecimal rating;
    private int reviewCount;
    private BigDecimal walletBalance;
    private List<String> serviceAreas;
    private List<String> serviceCategories;
    private Instant approvedAt;
}
