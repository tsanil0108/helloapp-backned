package com.packersmovers.marketplace.entity;

import com.packersmovers.marketplace.common.enums.ProviderStatus;
import com.packersmovers.marketplace.common.util.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Providers are the controlled, monetized side of the marketplace:
 * register -> KYC -> Super Admin approval -> eligible for lead matching.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "providers", indexes = {
        @Index(name = "idx_providers_status", columnList = "status")
})
public class Provider extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 150)
    private String companyName;

    @Column(nullable = false, length = 120)
    private String ownerName;

    @Column(length = 20)
    private String gstNumber;

    @Column(length = 20)
    private String panNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProviderStatus status = ProviderStatus.SUBMITTED;

    @Builder.Default
    @Column(nullable = false)
    private boolean verifiedBadge = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "provider_service_areas",
            joinColumns = @JoinColumn(name = "provider_id"),
            inverseJoinColumns = @JoinColumn(name = "service_area_id"))
    @Builder.Default
    private Set<ServiceArea> serviceAreas = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "provider_service_categories",
            joinColumns = @JoinColumn(name = "provider_id"),
            inverseJoinColumns = @JoinColumn(name = "service_category_id"))
    @Builder.Default
    private Set<ServiceCategory> serviceCategories = new HashSet<>();

    @Builder.Default
    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private int reviewCount = 0;

    /** Higher priority score wins more offers under the fair/priority distribution rule. */
    @Builder.Default
    @Column(nullable = false)
    private int priorityScore = 0;

    private Instant approvedAt;

    @jakarta.persistence.ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedBy;

    private String rejectionReason;
}
