CREATE TABLE outbox_events (
  id UUID PRIMARY KEY,
  aggregate_type VARCHAR(100) NOT NULL,
  aggregate_id UUID NOT NULL,
  event_type VARCHAR(100) NOT NULL,
  exchange_name VARCHAR(200) NOT NULL,
  routing_key VARCHAR(200) NOT NULL,
  payload TEXT NOT NULL,
  occurred_at TIMESTAMPTZ NOT NULL,
  published_at TIMESTAMPTZ NULL,
  publish_attempts INTEGER NOT NULL DEFAULT 0,
  last_error TEXT NULL
);

CREATE INDEX idx_outbox_events_unpublished
  ON outbox_events (published_at, occurred_at, id);
