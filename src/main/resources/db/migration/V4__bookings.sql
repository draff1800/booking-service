CREATE TABLE bookings (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  status VARCHAR(30) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_bookings_user_id ON bookings (user_id);
