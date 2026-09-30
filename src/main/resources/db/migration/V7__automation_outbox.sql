CREATE TABLE automation_outbox (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  event_id VARCHAR(36) NOT NULL UNIQUE,
  event_type VARCHAR(80) NOT NULL,
  appointment_id BIGINT NOT NULL,
  previous_status VARCHAR(20),
  status VARCHAR(20) NOT NULL,
  source VARCHAR(20) NOT NULL,
  occurred_at TIMESTAMP NOT NULL,
  delivery_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  attempts INT NOT NULL DEFAULT 0,
  last_error VARCHAR(250),
  delivered_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_automation_outbox_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id)
);
CREATE INDEX ix_automation_outbox_pending ON automation_outbox(delivery_status, created_at);
