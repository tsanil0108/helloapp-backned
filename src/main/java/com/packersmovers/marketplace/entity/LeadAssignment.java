package com.packersmovers.marketplace.entity;

import com.packersmovers.marketplace.common.enums.AssignmentStatus;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One row per (lead, provider) offer. This is the "unlock" record -
 * a lead can have at most Lead.maxProviders rows that reach UNLOCKED.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "lead_assignments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"lead_id", "provider_id"}),
        indexes = {
                @Index(name = "idx_assignments_status", columnList = "status"),
                @Index(name = "idx_assignments_provider", columnList = "provider_id")
        })
public class LeadAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.OFFERED;

    private Instant offeredAt;
    private Instant viewedAt;
    private Instant unlockedAt;
    private Instant expiresAt;
    private Instant contactedAt;

    @Column(precision = 10, scale = 2)
    private BigDecimal unlockFeeCharged;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_transaction_id")
    private WalletTransaction walletTransaction;
}
