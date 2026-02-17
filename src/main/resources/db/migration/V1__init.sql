CREATE TABLE users (
  id uuid PRIMARY KEY,
  email varchar(320) NOT NULL UNIQUE,
  password_hash varchar(255) NOT NULL,
  role varchar(50) NOT NULL,
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL
);