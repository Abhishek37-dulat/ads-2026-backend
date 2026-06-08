-- Relay core transactional schema (Postgres). Mirrors tech-design.html §05 / §09.
-- Every workspace-scoped table carries workspace_id and enforces tenant isolation via RLS:
--   USING (workspace_id = current_setting('app.workspace')::uuid)
-- The app sets app.workspace per transaction (see RlsAspect).

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ------------------------------------------------------------------ identity
CREATE TABLE organization (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name        text NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE workspace (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id      uuid NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
    name        text NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE app_user (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id      uuid NOT NULL REFERENCES organization(id) ON DELETE CASCADE,
    email       text NOT NULL UNIQUE,
    name        text NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE membership (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id  uuid NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    user_id       uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role          text NOT NULL DEFAULT 'editor',   -- admin|editor|viewer
    UNIQUE (workspace_id, user_id)
);

-- ------------------------------------------------------------------ connections
CREATE TABLE connection (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id  uuid NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    platform      text NOT NULL,
    account_name  text NOT NULL,
    ext_account_id text,
    status        text NOT NULL DEFAULT 'disconnected',  -- connected|degraded|disconnected
    created_at    timestamptz NOT NULL DEFAULT now(),
    UNIQUE (workspace_id, platform, ext_account_id)
);

CREATE TABLE verification_check (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    connection_id uuid NOT NULL REFERENCES connection(id) ON DELETE CASCADE,
    workspace_id  uuid NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    kind          text NOT NULL,    -- oauth|business|domain|payment|pixel
    status        text NOT NULL,    -- pass|warn|fail
    detail        text,
    checked_at    timestamptz NOT NULL DEFAULT now()
);

-- ------------------------------------------------------------------ campaign (canonical brief)
CREATE TABLE campaign (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id  uuid NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    name          text NOT NULL,
    objective     text NOT NULL,                       -- AWARENESS|SALES|LEADS|CALLS…
    destination   jsonb NOT NULL DEFAULT '{}'::jsonb,  -- {type,value,…}
    status        text NOT NULL DEFAULT 'draft',       -- draft|preflight|launching|live|paused|failed
    budget_mode   text NOT NULL DEFAULT 'daily',       -- daily|lifetime
    budget_amount numeric(12,2) NOT NULL DEFAULT 0,
    split         text NOT NULL DEFAULT 'auto',        -- auto|manual
    all_or_nothing boolean NOT NULL DEFAULT false,
    created_by    uuid REFERENCES app_user(id),
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE targeting_spec (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id   uuid NOT NULL UNIQUE REFERENCES campaign(id) ON DELETE CASCADE,
    workspace_id  uuid NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    geo           jsonb NOT NULL DEFAULT '[]'::jsonb,
    age_min       int,
    age_max       int,
    audiences     jsonb NOT NULL DEFAULT '[]'::jsonb,
    interests     jsonb NOT NULL DEFAULT '[]'::jsonb
);

-- ------------------------------------------------------------------ creative
CREATE TABLE creative (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id  uuid NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    name          text NOT NULL,
    master_key    text,                  -- S3 key of the master asset
    headline      text,
    body          text,
    status        text NOT NULL DEFAULT 'processing',  -- processing|ready|failed
    created_at    timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE creative_derivative (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    creative_id   uuid NOT NULL REFERENCES creative(id) ON DELETE CASCADE,
    workspace_id  uuid NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    ratio         text NOT NULL,         -- 1:1|4:5|9:16|16:9|2:3
    s3_key        text NOT NULL,
    validation    jsonb NOT NULL DEFAULT '{}'::jsonb
);

-- ------------------------------------------------------------------ deployments (campaign × connection)
CREATE TABLE platform_deployment (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id    uuid NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    campaign_id     uuid NOT NULL REFERENCES campaign(id) ON DELETE CASCADE,
    connection_id   uuid NOT NULL REFERENCES connection(id),
    platform        text NOT NULL,
    ext_campaign_id text,                              -- native id once created
    status          text NOT NULL DEFAULT 'queued',    -- queued|submitting|live|failed|paused
    budget_share    numeric(5,4) NOT NULL DEFAULT 0,
    last_error      jsonb,
    updated_at      timestamptz NOT NULL DEFAULT now(),
    UNIQUE (campaign_id, connection_id)
);

-- ------------------------------------------------------------------ compliance + launch + audit
CREATE TABLE compliance_check (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id  uuid NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    campaign_id   uuid NOT NULL REFERENCES campaign(id) ON DELETE CASCADE,
    platform      text NOT NULL,
    rule_code     text NOT NULL,
    severity      text NOT NULL,    -- pass|warn|block
    message       text,
    created_at    timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE launch_job (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id  uuid NOT NULL REFERENCES workspace(id) ON DELETE CASCADE,
    campaign_id   uuid NOT NULL REFERENCES campaign(id) ON DELETE CASCADE,
    workflow_id   text NOT NULL,
    status        text NOT NULL DEFAULT 'running',   -- running|completed|rejected|failed
    created_at    timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE audit_log (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id  uuid NOT NULL,
    user_id       uuid,
    action        text NOT NULL,
    target        text,
    detail        jsonb,
    created_at    timestamptz NOT NULL DEFAULT now()
);

-- ------------------------------------------------------------------ indexes
CREATE INDEX idx_campaign_ws        ON campaign(workspace_id);
CREATE INDEX idx_connection_ws      ON connection(workspace_id);
CREATE INDEX idx_deployment_ws      ON platform_deployment(workspace_id);
CREATE INDEX idx_deployment_campaign ON platform_deployment(campaign_id);

-- ------------------------------------------------------------------ row-level security
DO $$
DECLARE t text;
BEGIN
  FOREACH t IN ARRAY ARRAY[
    'workspace','membership','connection','verification_check','campaign','targeting_spec',
    'creative','creative_derivative','platform_deployment','compliance_check','launch_job'
  ] LOOP
    EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
    EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
  END LOOP;
END $$;

-- workspace table keys on its own id; the rest key on workspace_id
CREATE POLICY tenant ON workspace
  USING (id = current_setting('app.workspace', true)::uuid);

DO $$
DECLARE t text;
BEGIN
  FOREACH t IN ARRAY ARRAY[
    'membership','connection','verification_check','campaign','targeting_spec',
    'creative','creative_derivative','platform_deployment','compliance_check','launch_job'
  ] LOOP
    EXECUTE format(
      'CREATE POLICY tenant ON %I USING (workspace_id = current_setting(''app.workspace'', true)::uuid)',
      t);
  END LOOP;
END $$;
