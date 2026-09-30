ALTER TABLE lead_assignments
    ADD COLUMN IF NOT EXISTS contact_method VARCHAR(30);

ALTER TABLE lead_assignments
    ADD COLUMN IF NOT EXISTS contact_attempt_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE lead_assignments
    ADD COLUMN IF NOT EXISTS last_contacted_at TIMESTAMPTZ;

ALTER TABLE lead_assignments
    ADD COLUMN IF NOT EXISTS quote_sent_at TIMESTAMPTZ;

ALTER TABLE lead_assignments
    ADD COLUMN IF NOT EXISTS booked_at TIMESTAMPTZ;

ALTER TABLE lead_assignments
    ADD COLUMN IF NOT EXISTS service_started_at TIMESTAMPTZ;

ALTER TABLE lead_assignments
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ;

ALTER TABLE lead_assignments
    ADD COLUMN IF NOT EXISTS lost_at TIMESTAMPTZ;

ALTER TABLE lead_assignments
    ADD COLUMN IF NOT EXISTS lost_reason VARCHAR(100);

ALTER TABLE lead_assignments
    ADD COLUMN IF NOT EXISTS completion_notes TEXT;

CREATE INDEX IF NOT EXISTS idx_lead_assignments_provider_status
    ON lead_assignments(provider_id, status);

CREATE INDEX IF NOT EXISTS idx_lead_assignments_lead_status
    ON lead_assignments(lead_id, status);

CREATE INDEX IF NOT EXISTS idx_lead_assignments_contacted_at
    ON lead_assignments(contacted_at);

CREATE INDEX IF NOT EXISTS idx_lead_assignments_completed_at
    ON lead_assignments(completed_at);