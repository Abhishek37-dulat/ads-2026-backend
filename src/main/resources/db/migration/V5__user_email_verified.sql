-- Email verification gate. Password signups start unverified; Google/OTP/admin are verified.
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS email_verified boolean NOT NULL DEFAULT false;
