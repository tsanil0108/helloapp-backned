package com.packersmovers.marketplace.entity;

import com.packersmovers.marketplace.common.enums.LeadSource;
import com.packersmovers.marketplace.common.util.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Aggregated marketing attribution for ROI reporting (rolled up from Lead.utm* fields). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "campaigns")
public class Campaign extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeadSource source;

    private String medium;

    @Column(nullable = false, length = 150)
    private String campaignName;

    @Builder.Default
    private long leadCount = 0;

    @Builder.Default
    private long verifiedLeadCount = 0;

    @Builder.Default
    private long conversionCount = 0;

    @Builder.Default
    @Column(precision = 12, scale = 2)
    private java.math.BigDecimal totalSpend = java.math.BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 12, scale = 2)
    private java.math.BigDecimal totalRevenue = java.math.BigDecimal.ZERO;
}
