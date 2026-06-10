-- Replace the seeded shared demo tenant with per-user tenant provisioning.
-- This intentionally removes all users and data owned by the known demo organization.
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS default_workspace_id uuid;

DELETE FROM audit_log
WHERE workspace_id = '22222222-2222-2222-2222-222222222222';

DELETE FROM organization
WHERE id = '11111111-1111-1111-1111-111111111111';

ALTER TABLE app_user
  ADD CONSTRAINT fk_app_user_default_workspace
  FOREIGN KEY (default_workspace_id) REFERENCES workspace(id) ON DELETE CASCADE;

ALTER TABLE app_user ALTER COLUMN default_workspace_id SET NOT NULL;
