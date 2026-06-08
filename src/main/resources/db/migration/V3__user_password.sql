-- Email/password sign-in. OAuth and OTP users keep a NULL hash.
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS password_hash text;
