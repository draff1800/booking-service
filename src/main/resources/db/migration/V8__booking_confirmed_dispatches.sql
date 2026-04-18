CREATE TABLE booking_confirmed_dispatches (
  id UUID PRIMARY KEY,
  booking_id UUID NOT NULL UNIQUE,
  source_event_id UUID NOT NULL UNIQUE,
  user_id UUID NOT NULL,
  status VARCHAR(30) NOT NULL,
  processed_at TIMESTAMPTZ NOT NULL
);
