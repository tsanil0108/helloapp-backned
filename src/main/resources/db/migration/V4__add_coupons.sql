CREATE TABLE coupons (
    id BIGSERIAL PRIMARY KEY,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    code VARCHAR(60) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    expires_at TIMESTAMPTZ NULL,
    max_uses INTEGER NULL,
    used_count INTEGER NOT NULL DEFAULT 0,

    created_by_user_id BIGINT NOT NULL,

    CONSTRAINT uk_coupons_code
        UNIQUE (code),

    CONSTRAINT fk_coupons_created_by_user
        FOREIGN KEY (created_by_user_id)
        REFERENCES users(id),

    CONSTRAINT chk_coupons_amount_positive
        CHECK (amount > 0),

    CONSTRAINT chk_coupons_used_count_non_negative
        CHECK (used_count >= 0),

    CONSTRAINT chk_coupons_max_uses_positive
        CHECK (max_uses IS NULL OR max_uses > 0)
);

CREATE TABLE coupon_redemptions (
    id BIGSERIAL PRIMARY KEY,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    coupon_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,

    amount NUMERIC(12, 2) NOT NULL,
    redeemed_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_coupon_redemptions_coupon
        FOREIGN KEY (coupon_id)
        REFERENCES coupons(id),

    CONSTRAINT fk_coupon_redemptions_provider
        FOREIGN KEY (provider_id)
        REFERENCES providers(id),

    CONSTRAINT uk_coupon_redemption_provider
        UNIQUE (provider_id),

    CONSTRAINT uk_coupon_redemption_coupon_provider
        UNIQUE (coupon_id, provider_id),

    CONSTRAINT chk_coupon_redemption_amount_positive
        CHECK (amount > 0)
);

CREATE INDEX idx_coupons_active
    ON coupons(active);

CREATE INDEX idx_coupons_expires_at
    ON coupons(expires_at);

CREATE INDEX idx_coupon_redemptions_coupon_id
    ON coupon_redemptions(coupon_id);

CREATE INDEX idx_coupon_redemptions_provider_id
    ON coupon_redemptions(provider_id);