-- Making multiple identical API requests should produce the same effect as making a single request.
-- These keys support that behaviour.

-- Events: Idempotency per creator
ALTER TABLE events
  ADD COLUMN idempotency_key VARCHAR(100);

CREATE UNIQUE INDEX ux_events_creator_idempotency
  ON events (created_by, idempotency_key)
  WHERE idempotency_key IS NOT NULL;

-- Ticket Types: Idempotency per event
ALTER TABLE ticket_types
  ADD COLUMN idempotency_key VARCHAR(100);

CREATE UNIQUE INDEX ux_ticket_types_event_idempotency
  ON ticket_types (event_id, idempotency_key)
  WHERE idempotency_key IS NOT NULL;

-- Bookings: Idempotency per user
ALTER TABLE bookings
  ADD COLUMN idempotency_key VARCHAR(100);

CREATE UNIQUE INDEX ux_bookings_user_idempotency
  ON bookings (user_id, idempotency_key)
  WHERE idempotency_key IS NOT NULL;