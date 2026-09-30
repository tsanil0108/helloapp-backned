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
 * One row per (lead, provider) offer.
 *
 * This record tracks the complete provider-side lifecycle
 * of a lead assignment:
 *
 * OFFERED
 * → VIEWED
 * → UNLOCKED
 * → CONTACTED
 * → QUOTE_SENT
 * → NEGOTIATION
 * → BOOKED
 * → SERVICE_IN_PROGRESS
 * → COMPLETED
 *
 * It also supports terminal outcomes such as:
 * LOST, EXPIRED, CANCELLED, INVALID, DUPLICATE, REFUNDED.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(
        name = "lead_assignments",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"lead_id", "provider_id"}
        ),
        indexes = {
                @Index(
                        name = "idx_assignments_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_assignments_provider",
                        columnList = "provider_id"
                )
        }
)
public class LeadAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "lead_id",
            nullable = false
    )
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "provider_id",
            nullable = false
    )
    private Provider provider;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    @Builder.Default
    private AssignmentStatus status =
            AssignmentStatus.OFFERED;

    private Instant offeredAt;

    private Instant viewedAt;

    private Instant unlockedAt;

    private Instant expiresAt;

    private Instant contactedAt;

    @Column(
            name = "contact_method",
            length = 30
    )
    private String contactMethod;

    @Column(
            name = "contact_attempt_count",
            nullable = false
    )
    @Builder.Default
    private Integer contactAttemptCount = 0;

    @Column(
            name = "last_contacted_at"
    )
    private Instant lastContactedAt;

    @Column(
            name = "quote_sent_at"
    )
    private Instant quoteSentAt;

    @Column(
            name = "booked_at"
    )
    private Instant bookedAt;

    @Column(
            name = "service_started_at"
    )
    private Instant serviceStartedAt;

    @Column(
            name = "completed_at"
    )
    private Instant completedAt;

    @Column(
            name = "lost_at"
    )
    private Instant lostAt;

    @Column(
            name = "lost_reason",
            length = 100
    )
    private String lostReason;

    @Column(
            name = "completion_notes",
            columnDefinition = "TEXT"
    )
    private String completionNotes;

    @Column(
            precision = 10,
            scale = 2
    )
    private BigDecimal unlockFeeCharged;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "wallet_transaction_id"
    )
    private WalletTransaction walletTransaction;
}