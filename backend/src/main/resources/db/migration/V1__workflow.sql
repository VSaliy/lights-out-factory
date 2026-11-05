CREATE TABLE work_items (
    id UUID PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    acceptance_criteria VARCHAR(4000) NOT NULL,
    priority VARCHAR(16) NOT NULL CHECK (priority IN ('P1', 'P2', 'P3')),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE execution_plans (
    id UUID PRIMARY KEY,
    work_item_id UUID NOT NULL REFERENCES work_items(id),
    summary VARCHAR(1000) NOT NULL,
    status VARCHAR(32) NOT NULL,
    approved_by VARCHAR(80),
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_execution_plans_work_item UNIQUE (work_item_id)
);

CREATE TABLE agent_tasks (
    id UUID PRIMARY KEY,
    plan_id UUID NOT NULL REFERENCES execution_plans(id),
    title VARCHAR(200) NOT NULL,
    input VARCHAR(4000) NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    CONSTRAINT uq_agent_tasks_plan UNIQUE (plan_id)
);

CREATE TABLE execution_runs (
    id UUID PRIMARY KEY,
    agent_task_id UUID NOT NULL REFERENCES agent_tasks(id),
    provider_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    simulated BOOLEAN NOT NULL,
    output_summary VARCHAR(2000),
    error_code VARCHAR(80),
    correlation_id UUID NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ
);

CREATE TABLE quality_gate_results (
    id UUID PRIMARY KEY,
    execution_run_id UUID NOT NULL REFERENCES execution_runs(id),
    gate_name VARCHAR(80) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('PASSED', 'FAILED')),
    evidence_summary VARCHAR(500) NOT NULL,
    evaluated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_quality_gate_run_name UNIQUE (execution_run_id, gate_name)
);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL,
    actor VARCHAR(80) NOT NULL,
    action VARCHAR(80) NOT NULL,
    resource_type VARCHAR(40) NOT NULL,
    resource_id UUID NOT NULL,
    work_item_id UUID NOT NULL REFERENCES work_items(id),
    correlation_id UUID NOT NULL
);

CREATE INDEX idx_work_items_created_at ON work_items(created_at DESC);
CREATE INDEX idx_plans_work_item_created ON execution_plans(work_item_id, created_at DESC);
CREATE INDEX idx_runs_task_started ON execution_runs(agent_task_id, started_at DESC);
CREATE INDEX idx_audit_events_work_item_time ON audit_events(work_item_id, occurred_at);
