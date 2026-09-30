package com.packersmovers.marketplace.entity;

import com.packersmovers.marketplace.common.util.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "coupon_redemptions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_coupon_redemption_coupon_provider",
                        columnNames = {
                                "coupon_id",
                                "provider_id"
                        }
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponRedemption extends BaseEntity {


    // ============================================================
    // COUPON
    // ============================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "coupon_id",
            nullable = false
    )
    private Coupon coupon;


    // ============================================================
    // PROVIDER
    // ============================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "provider_id",
            nullable = false
    )
    private Provider provider;


    // ============================================================
    // REDEEMED AMOUNT
    // ============================================================

    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal amount;


    // ============================================================
    // REDEEMED AT
    // ============================================================

    @Column(
            name = "redeemed_at",
            nullable = false
    )
    private Instant redeemedAt;


    // ============================================================
    // PRE PERSIST
    // ============================================================

    @PrePersist
    protected void onCreate() {

        if (redeemedAt == null) {
            redeemedAt = Instant.now();
        }

    }
}