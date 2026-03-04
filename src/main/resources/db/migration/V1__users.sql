CREATE TABLE users (
  id uuid PRIMARY KEY,
  email VARCHAR(320) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(50) NOT NULL,
  handle VARCHAR(50),
  display_name VARCHAR(80),
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX ux_users_handle
  ON users(handle)
  WHERE handle IS NOT NULL;
