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
        name = "coupons",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_coupons_code",
                        columnNames = "code"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon extends BaseEntity {

    // ============================================================
    // COUPON CODE
    // ============================================================

    @Column(
            nullable = false,
            length = 60
    )
    private String code;


    // ============================================================
    // CREDIT AMOUNT
    // ============================================================

    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal amount;


    // ============================================================
    // WELCOME COUPON
    // ============================================================


    @Column(
            name = "welcome_coupon",
            nullable = false
    )
    private boolean welcomeCoupon;


    // ============================================================
    // ACTIVE
    // ============================================================

    @Column(
            nullable = false
    )
    private boolean active;


    // ============================================================
    // EXPIRY
    // ============================================================

    @Column(
            name = "expires_at"
    )
    private Instant expiresAt;


    // ============================================================
    // MAXIMUM USES
    // ============================================================

    @Column(
            name = "max_uses"
    )
    private Integer maxUses;


    // ============================================================
    // USED COUNT
    // ============================================================

    @Column(
            name = "used_count",
            nullable = false
    )
    private int usedCount;


    // ============================================================
    // CREATED BY
    // ============================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "created_by_user_id",
            nullable = false
    )
    private User createdBy;


    // ============================================================
    // PRE PERSIST
    // ============================================================

    @PrePersist
    protected void onCreate() {

        if (usedCount < 0) {
            usedCount = 0;
        }

    }
}