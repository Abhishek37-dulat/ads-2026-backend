-- Deterministic dev seed: one agency org, one workspace, one user, and a few platform
-- connections so the dashboard / connections screens have data on first boot.
-- Workspace-scoped inserts require app.workspace under FORCE RLS, so we set it first.

INSERT INTO organization (id, name) VALUES
  ('11111111-1111-1111-1111-111111111111', 'Acme Agency');

INSERT INTO app_user (id, org_id, email, name) VALUES
  ('33333333-3333-3333-3333-333333333333',
   '11111111-1111-1111-1111-111111111111',
   'demo@relay.dev', 'Demo User');

SELECT set_config('app.workspace', '22222222-2222-2222-2222-222222222222', false);

INSERT INTO workspace (id, org_id, name) VALUES
  ('22222222-2222-2222-2222-222222222222',
   '11111111-1111-1111-1111-111111111111', 'Northstar Plumbing');

INSERT INTO membership (workspace_id, user_id, role) VALUES
  ('22222222-2222-2222-2222-222222222222',
   '33333333-3333-3333-3333-333333333333', 'admin');

INSERT INTO connection (id, workspace_id, platform, account_name, ext_account_id, status) VALUES
  ('aaaaaaa1-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222', 'META',     'Northstar — Meta',     'act_100200300', 'connected'),
  ('aaaaaaa1-0000-0000-0000-000000000002', '22222222-2222-2222-2222-222222222222', 'GOOGLE',   'Northstar — Google',   '402-555-1212',  'connected'),
  ('aaaaaaa1-0000-0000-0000-000000000003', '22222222-2222-2222-2222-222222222222', 'TIKTOK',   'Northstar — TikTok',   '7100200300',    'degraded'),
  ('aaaaaaa1-0000-0000-0000-000000000004', '22222222-2222-2222-2222-222222222222', 'LINKEDIN', 'Northstar — LinkedIn', NULL,            'disconnected');

INSERT INTO verification_check (connection_id, workspace_id, kind, status, detail) VALUES
  ('aaaaaaa1-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222', 'oauth',    'pass', 'Token valid'),
  ('aaaaaaa1-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222', 'pixel',    'pass', 'Pixel firing'),
  ('aaaaaaa1-0000-0000-0000-000000000002', '22222222-2222-2222-2222-222222222222', 'oauth',    'pass', 'Token valid'),
  ('aaaaaaa1-0000-0000-0000-000000000003', '22222222-2222-2222-2222-222222222222', 'business', 'warn', 'Business verification pending'),
  ('aaaaaaa1-0000-0000-0000-000000000004', '22222222-2222-2222-2222-222222222222', 'oauth',    'fail', 'Not connected');
