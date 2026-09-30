-- ============================================================
-- CUSTOMER NOTIFICATION FIELDS
-- ============================================================

ALTER TABLE leads
    ADD COLUMN IF NOT EXISTS customer_notification_email VARCHAR(255);

ALTER TABLE leads
    ADD COLUMN IF NOT EXISTS customer_notification_mobile VARCHAR(30);