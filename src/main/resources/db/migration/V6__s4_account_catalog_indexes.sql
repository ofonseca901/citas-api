ALTER TABLE user_affiliations ADD COLUMN regime VARCHAR(20) NOT NULL DEFAULT 'PARTICULAR';
CREATE INDEX ix_reset_user_active ON password_reset_tokens(user_id, consumed_at, expires_at);
CREATE INDEX ix_plan_eps_active ON insurance_plans(eps_id, active);
CREATE INDEX ix_reschedule_pending ON appointment_reschedules(status, created_at);
