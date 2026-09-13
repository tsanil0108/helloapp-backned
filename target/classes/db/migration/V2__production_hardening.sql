-- Production hardening: idempotency, financial uniqueness and refresh-session storage.
CREATE TABLE IF NOT EXISTS refresh_token_sessions (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by_hash VARCHAR(64)
);
CREATE INDEX IF NOT EXISTS idx_refresh_user ON refresh_token_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_active ON refresh_token_sessions(user_id, revoked_at);

CREATE UNIQUE INDEX IF NOT EXISTS uq_payments_razorpay_order ON payments(razorpay_order_id) WHERE razorpay_order_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_payments_razorpay_payment ON payments(razorpay_payment_id) WHERE razorpay_payment_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_leads_route_window ON leads(customer_mobile, pickup_location, drop_location, created_at);
CREATE INDEX IF NOT EXISTS idx_assignments_lead_status ON lead_assignments(lead_id, status);
CREATE UNIQUE INDEX IF NOT EXISTS uq_wallet_txn_reference ON wallet_transactions(wallet_id, reference_type, reference_id, type);
CREATE INDEX IF NOT EXISTS idx_provider_sub_active_dates ON provider_subscriptions(provider_id, active, end_date);

