-- Platform admin flag (Ops console access). Tenant users are non-admin.
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS is_admin boolean NOT NULL DEFAULT false;
