-- ============================================================
-- COUPON SYSTEM UPDATE
-- ============================================================

-- Add Welcome Coupon flag
ALTER TABLE coupons
    ADD COLUMN welcome_coupon BOOLEAN NOT NULL DEFAULT FALSE;


-- ============================================================
-- COUPON REDEMPTION RULE UPDATE
-- ============================================================

-- Previously one provider could redeem only one coupon globally.
-- Remove that restriction.
--
-- The coupon_id + provider_id unique constraint remains,
-- so the SAME coupon can still be redeemed only once
-- by the SAME provider.

ALTER TABLE coupon_redemptions
    DROP CONSTRAINT IF EXISTS uk_coupon_redemption_provider;


-- ============================================================
-- INDEX
-- ============================================================

-- Useful for checking whether a provider has already
-- redeemed any welcome coupon.

CREATE INDEX idx_coupon_redemptions_provider_welcome
    ON coupon_redemptions(provider_id, coupon_id);