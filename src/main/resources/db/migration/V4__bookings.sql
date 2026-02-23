CREATE TABLE bookings (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL,
  status varchar(30) NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL
);

CREATE INDEX idx_bookings_user_id ON bookings (user_id);
