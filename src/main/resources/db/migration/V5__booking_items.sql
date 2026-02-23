CREATE TABLE booking_items (
  id uuid PRIMARY KEY,
  booking_id uuid NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
  ticket_type_id uuid NOT NULL REFERENCES ticket_types(id),
  quantity integer NOT NULL,
  unit_price_minor integer NOT NULL,
  currency char(3) NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  CONSTRAINT chk_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX idx_booking_items_booking_id ON booking_items (booking_id);
CREATE INDEX idx_booking_items_ticket_type_id ON booking_items (ticket_type_id);
