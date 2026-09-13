package com.packersmovers.marketplace.entity;

import com.packersmovers.marketplace.common.enums.TransactionReferenceType;
import com.packersmovers.marketplace.common.enums.TransactionType;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Immutable financial ledger entry. Rows are never updated or deleted -
 * this table is the audit-grade source of truth for every credit/debit/refund.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "wallet_transactions", indexes = {
        @Index(name = "idx_wallet_txn_wallet", columnList = "wallet_id")
})
public class WalletTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionReferenceType referenceType;

    /** e.g. Lead.id for LEAD_UNLOCK, Payment.id for RAZORPAY_TOPUP */
    private Long referenceId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** Wallet balance immediately after this transaction was applied (for point-in-time audit). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceAfter;

    private String description;
}
