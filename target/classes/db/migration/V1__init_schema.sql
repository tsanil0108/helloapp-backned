-- ============================================================================
-- Packers & Movers Lead Marketplace - initial schema
-- Mirrors every JPA entity under com.packersmovers.marketplace.entity exactly.
-- spring.jpa.hibernate.ddl-auto=validate -> THIS FILE is the schema source of truth.
--
-- Note on *_code columns (lead_code, quote_code): the application inserts the row first
-- (to obtain the generated id), then updates it with a human-friendly code derived from
-- that id (see LeadServiceImpl/QuoteServiceImpl). They are therefore UNIQUE but NULLable
-- at the database level even though the JPA field is annotated nullable=false for later reads.
-- ============================================================================

-- ---------- 1. Identity ----------

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 0,
    full_name       VARCHAR(120) NOT NULL,
    email           VARCHAR(150) NOT NULL,
    mobile          VARCHAR(20)  NOT NULL,
    password_hash   TEXT NOT NULL,
    role            VARCHAR(30)  NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT true,
    locked          BOOLEAN NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX idx_users_email ON users (email);
CREATE UNIQUE INDEX idx_users_mobile ON users (mobile);

CREATE TABLE customers (
    id              BIGSERIAL PRIMARY KEY,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 0,
    full_name       VARCHAR(120) NOT NULL,
    mobile          VARCHAR(20)  NOT NULL,
    email           VARCHAR(150)
);
CREATE INDEX idx_customers_mobile ON customers (mobile);

-- ---------- 2. Catalog ----------

CREATE TABLE service_areas (
    id              BIGSERIAL PRIMARY KEY,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 0,
    city_or_region  VARCHAR(100) NOT NULL,
    pincode         VARCHAR(10),
    state           VARCHAR(60),
    active          BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE service_categories (
    id              BIGSERIAL PRIMARY KEY,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 0,
    name            VARCHAR(100) NOT NULL UNIQUE,
    description     VARCHAR(255),
    active          BOOLEAN NOT NULL DEFAULT true
);

-- ---------- 3. Providers ----------

CREATE TABLE providers (
    id                  BIGSERIAL PRIMARY KEY,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    version             BIGINT NOT NULL DEFAULT 0,
    user_id             BIGINT NOT NULL UNIQUE REFERENCES users (id),
    company_name        VARCHAR(150) NOT NULL,
    owner_name          VARCHAR(120) NOT NULL,
    gst_number          VARCHAR(20),
    pan_number          VARCHAR(20),
    status              VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    verified_badge      BOOLEAN NOT NULL DEFAULT false,
    rating              NUMERIC(3,2) NOT NULL DEFAULT 0,
    review_count        INT NOT NULL DEFAULT 0,
    priority_score      INT NOT NULL DEFAULT 0,
    approved_at         TIMESTAMPTZ,
    approved_by_user_id BIGINT REFERENCES users (id),
    rejection_reason    VARCHAR(1000)
);
CREATE INDEX idx_providers_status ON providers (status);

CREATE TABLE provider_service_areas (
    provider_id      BIGINT NOT NULL REFERENCES providers (id) ON DELETE CASCADE,
    service_area_id  BIGINT NOT NULL REFERENCES service_areas (id) ON DELETE CASCADE,
    PRIMARY KEY (provider_id, service_area_id)
);

CREATE TABLE provider_service_categories (
    provider_id          BIGINT NOT NULL REFERENCES providers (id) ON DELETE CASCADE,
    service_category_id  BIGINT NOT NULL REFERENCES service_categories (id) ON DELETE CASCADE,
    PRIMARY KEY (provider_id, service_category_id)
);

CREATE TABLE kyc_documents (
    id           BIGSERIAL PRIMARY KEY,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    version      BIGINT NOT NULL DEFAULT 0,
    provider_id  BIGINT NOT NULL REFERENCES providers (id) ON DELETE CASCADE,
    doc_type     VARCHAR(30) NOT NULL,
    doc_url      VARCHAR(500) NOT NULL,
    verified     BOOLEAN NOT NULL DEFAULT false,
    remarks      VARCHAR(1000)
);

-- ---------- 4. Wallet ----------

CREATE TABLE wallets (
    id           BIGSERIAL PRIMARY KEY,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    version      BIGINT NOT NULL DEFAULT 0,
    provider_id  BIGINT NOT NULL UNIQUE REFERENCES providers (id) ON DELETE CASCADE,
    balance      NUMERIC(12,2) NOT NULL DEFAULT 0
);

-- ---------- 5. Leads ----------

CREATE TABLE leads (
    id                      BIGSERIAL PRIMARY KEY,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    version                 BIGINT NOT NULL DEFAULT 0,
    lead_code               VARCHAR(20) UNIQUE,
    customer_id             BIGINT REFERENCES customers (id),
    customer_name           VARCHAR(120) NOT NULL,
    customer_mobile         VARCHAR(20) NOT NULL,
    customer_email          VARCHAR(150),
    pickup_location         VARCHAR(200) NOT NULL,
    drop_location           VARCHAR(200) NOT NULL,
    move_date               DATE,
    property_type           VARCHAR(60),
    service_category_id     BIGINT REFERENCES service_categories (id),
    pickup_service_area_id  BIGINT REFERENCES service_areas (id),
    inventory_notes         TEXT,
    consent_given           BOOLEAN NOT NULL DEFAULT true,
    source                  VARCHAR(20) NOT NULL DEFAULT 'OTHER',
    utm_source              VARCHAR(255),
    utm_medium              VARCHAR(255),
    utm_campaign            VARCHAR(255),
    utm_term                VARCHAR(255),
    utm_content             VARCHAR(255),
    ip_address              VARCHAR(60),
    status                  VARCHAR(20) NOT NULL DEFAULT 'NEW',
    unlock_price            NUMERIC(10,2) NOT NULL,
    max_providers           INT NOT NULL DEFAULT 3,
    current_offer_count     INT NOT NULL DEFAULT 0,
    current_unlock_count    INT NOT NULL DEFAULT 0,
    quality_check_notes     TEXT,
    invalid_reason          TEXT
);
CREATE INDEX idx_leads_status ON leads (status);
CREATE INDEX idx_leads_mobile ON leads (customer_mobile);

-- ---------- 6. Wallet ledger (must exist before lead_assignments, which references it) ----------

CREATE TABLE wallet_transactions (
    id              BIGSERIAL PRIMARY KEY,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 0,
    wallet_id       BIGINT NOT NULL REFERENCES wallets (id),
    type            VARCHAR(20) NOT NULL,
    reference_type  VARCHAR(30) NOT NULL,
    reference_id    BIGINT,
    amount          NUMERIC(12,2) NOT NULL,
    balance_after   NUMERIC(12,2) NOT NULL,
    description     VARCHAR(1000)
);
CREATE INDEX idx_wallet_txn_wallet ON wallet_transactions (wallet_id);

-- ---------- 7. Lead distribution (max-3 offers per lead) ----------

CREATE TABLE lead_assignments (
    id                    BIGSERIAL PRIMARY KEY,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    version               BIGINT NOT NULL DEFAULT 0,
    lead_id               BIGINT NOT NULL REFERENCES leads (id),
    provider_id           BIGINT NOT NULL REFERENCES providers (id),
    status                VARCHAR(20) NOT NULL DEFAULT 'OFFERED',
    offered_at            TIMESTAMPTZ,
    viewed_at             TIMESTAMPTZ,
    unlocked_at           TIMESTAMPTZ,
    expires_at            TIMESTAMPTZ,
    contacted_at          TIMESTAMPTZ,
    unlock_fee_charged    NUMERIC(10,2),
    wallet_transaction_id BIGINT REFERENCES wallet_transactions (id),
    CONSTRAINT uq_lead_provider UNIQUE (lead_id, provider_id)
);
CREATE INDEX idx_assignments_status ON lead_assignments (status);
CREATE INDEX idx_assignments_provider ON lead_assignments (provider_id);

-- ---------- 8. Quotes ----------

CREATE TABLE quotes (
    id                  BIGSERIAL PRIMARY KEY,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    version             BIGINT NOT NULL DEFAULT 0,
    quote_code          VARCHAR(20) UNIQUE,
    lead_id             BIGINT NOT NULL REFERENCES leads (id),
    lead_assignment_id  BIGINT NOT NULL REFERENCES lead_assignments (id),
    provider_id         BIGINT NOT NULL REFERENCES providers (id),
    total_amount        NUMERIC(12,2) NOT NULL,
    valid_until         DATE,
    notes               TEXT,
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
);

CREATE TABLE quote_items (
    id         BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version    BIGINT NOT NULL DEFAULT 0,
    quote_id   BIGINT NOT NULL REFERENCES quotes (id) ON DELETE CASCADE,
    label      VARCHAR(100) NOT NULL,
    amount     NUMERIC(12,2) NOT NULL,
    remarks    VARCHAR(1000)
);

-- ---------- 9. Payments & refunds ----------

CREATE TABLE payments (
    id                   BIGSERIAL PRIMARY KEY,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    version              BIGINT NOT NULL DEFAULT 0,
    provider_id          BIGINT NOT NULL REFERENCES providers (id),
    razorpay_order_id    VARCHAR(100),
    razorpay_payment_id  VARCHAR(100),
    razorpay_signature   VARCHAR(255),
    amount               NUMERIC(12,2) NOT NULL,
    purpose              VARCHAR(20) NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    verified_at          TIMESTAMPTZ,
    failure_reason       VARCHAR(1000)
);
CREATE INDEX idx_payments_razorpay_order ON payments (razorpay_order_id);
CREATE INDEX idx_payments_status ON payments (status);

CREATE TABLE refunds (
    id                    BIGSERIAL PRIMARY KEY,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    version               BIGINT NOT NULL DEFAULT 0,
    provider_id           BIGINT NOT NULL REFERENCES providers (id),
    lead_assignment_id    BIGINT REFERENCES lead_assignments (id),
    amount                NUMERIC(12,2) NOT NULL,
    reason                VARCHAR(500) NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    approved_by_user_id   BIGINT REFERENCES users (id),
    processed_at          TIMESTAMPTZ,
    wallet_transaction_id BIGINT REFERENCES wallet_transactions (id)
);

-- ---------- 10. Marketing attribution ----------

CREATE TABLE campaigns (
    id                   BIGSERIAL PRIMARY KEY,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    version              BIGINT NOT NULL DEFAULT 0,
    source               VARCHAR(20) NOT NULL,
    medium               VARCHAR(255),
    campaign_name        VARCHAR(150) NOT NULL,
    lead_count           BIGINT NOT NULL DEFAULT 0,
    verified_lead_count  BIGINT NOT NULL DEFAULT 0,
    conversion_count     BIGINT NOT NULL DEFAULT 0,
    total_spend          NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_revenue        NUMERIC(12,2) NOT NULL DEFAULT 0
);

-- ---------- 11. Audit & notifications ----------

CREATE TABLE audit_logs (
    id           BIGSERIAL PRIMARY KEY,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    version      BIGINT NOT NULL DEFAULT 0,
    actor_id     BIGINT,
    actor_role   VARCHAR(30),
    actor_name   VARCHAR(150),
    action       VARCHAR(100) NOT NULL,
    entity_type  VARCHAR(60) NOT NULL,
    entity_id    BIGINT,
    old_value    TEXT,
    new_value    TEXT,
    ip_address   VARCHAR(60),
    remarks      VARCHAR(500)
);
CREATE INDEX idx_audit_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_actor ON audit_logs (actor_id);

CREATE TABLE notifications (
    id               BIGSERIAL PRIMARY KEY,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    version          BIGINT NOT NULL DEFAULT 0,
    user_id          BIGINT REFERENCES users (id),
    title            VARCHAR(150) NOT NULL,
    message          VARCHAR(1000) NOT NULL,
    channel          VARCHAR(20) NOT NULL,
    is_read          BOOLEAN NOT NULL DEFAULT false,
    sent_at          TIMESTAMPTZ,
    delivery_status  VARCHAR(255),
    related_entity   VARCHAR(255)
);

-- ---------- 12. Reviews & complaints ----------

CREATE TABLE reviews (
    id           BIGSERIAL PRIMARY KEY,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    version      BIGINT NOT NULL DEFAULT 0,
    lead_id      BIGINT NOT NULL REFERENCES leads (id),
    provider_id  BIGINT NOT NULL REFERENCES providers (id),
    rating       INT NOT NULL,
    comment      VARCHAR(1000)
);

CREATE TABLE complaints (
    id                BIGSERIAL PRIMARY KEY,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    version           BIGINT NOT NULL DEFAULT 0,
    raised_by_type    VARCHAR(20) NOT NULL,
    lead_id           BIGINT REFERENCES leads (id),
    provider_id       BIGINT REFERENCES providers (id),
    subject           VARCHAR(200) NOT NULL,
    description       VARCHAR(2000) NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    resolution_notes  VARCHAR(2000),
    resolved_at       TIMESTAMPTZ
);

-- ---------- 13. Settings & subscriptions ----------

CREATE TABLE settings (
    id             BIGSERIAL PRIMARY KEY,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    version        BIGINT NOT NULL DEFAULT 0,
    setting_key    VARCHAR(100) NOT NULL UNIQUE,
    setting_value  VARCHAR(500) NOT NULL,
    description    VARCHAR(255)
);

CREATE TABLE subscription_plans (
    id                    BIGSERIAL PRIMARY KEY,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    version               BIGINT NOT NULL DEFAULT 0,
    name                  VARCHAR(60) NOT NULL UNIQUE,
    price                 NUMERIC(10,2) NOT NULL,
    duration_days         INT NOT NULL,
    max_leads_per_month   INT NOT NULL DEFAULT 0,
    featured_listing      BOOLEAN NOT NULL DEFAULT false,
    features              VARCHAR(1000),
    active                BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE provider_subscriptions (
    id           BIGSERIAL PRIMARY KEY,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    version      BIGINT NOT NULL DEFAULT 0,
    provider_id  BIGINT NOT NULL REFERENCES providers (id),
    plan_id      BIGINT NOT NULL REFERENCES subscription_plans (id),
    start_date   DATE,
    end_date     DATE,
    active       BOOLEAN NOT NULL DEFAULT true,
    auto_renew   BOOLEAN NOT NULL DEFAULT false
);

-- ---------- 14. RBAC ----------

CREATE TABLE admin_permissions (
    id             BIGSERIAL PRIMARY KEY,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    version        BIGINT NOT NULL DEFAULT 0,
    admin_user_id  BIGINT NOT NULL REFERENCES users (id),
    module         VARCHAR(30) NOT NULL,
    can_view       BOOLEAN NOT NULL DEFAULT true,
    can_edit       BOOLEAN NOT NULL DEFAULT false,
    can_delete     BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uq_admin_module UNIQUE (admin_user_id, module)
);
