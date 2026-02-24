CREATE TABLE ticket_types (
  id UUID PRIMARY KEY,
  event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
  name VARCHAR(100) NOT NULL,
  price_minor INTEGER NOT NULL,
  currency VARCHAR(3) NOT NULL,
  capacity_total INTEGER NOT NULL,
  capacity_remaining INTEGER NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT chk_capacity_non_negative CHECK (capacity_total >= 0 AND capacity_remaining >= 0),
  CONSTRAINT chk_capacity_remaining_less_than_total CHECK (capacity_remaining <= capacity_total)
);

CREATE INDEX idx_ticket_types_event_id ON ticket_types (event_id);
