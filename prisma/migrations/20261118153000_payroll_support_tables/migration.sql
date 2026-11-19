-- Existing payroll service persists per-shift salary calculation details and
-- salary-rank/review history. Older migrations never created these tables.
-- Add only missing tables; preserve all historical salary records.
CREATE TABLE IF NOT EXISTS "salary_calculation_items" (
  "item_id" VARCHAR(50) PRIMARY KEY,
  "payroll_source" VARCHAR(20) NOT NULL,
  "payroll_id" VARCHAR(50) NOT NULL,
  "schedule_id" VARCHAR(50) NOT NULL,
  "work_date" DATE NOT NULL,
  "work_type" VARCHAR(30) NOT NULL,
  "attendance_status" VARCHAR(20),
  "worked_hours" DECIMAL(8,2) NOT NULL,
  "salary_rank_id" VARCHAR(50),
  "hourly_rate" DECIMAL(12,2) NOT NULL,
  "pay_factor" DECIMAL(6,4) NOT NULL DEFAULT 1,
  "amount" DECIMAL(14,2) NOT NULL,
  "created_at" TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS "salary_calculation_items_payroll_source_payroll_id_idx"
  ON "salary_calculation_items" ("payroll_source", "payroll_id");
CREATE INDEX IF NOT EXISTS "salary_calculation_items_schedule_id_idx"
  ON "salary_calculation_items" ("schedule_id");

CREATE TABLE IF NOT EXISTS "salary_rank_history" (
  "history_id" VARCHAR(50) PRIMARY KEY,
  "employee_id" VARCHAR(50) NOT NULL,
  "previous_salary_rank_id" VARCHAR(50),
  "salary_rank_id" VARCHAR(50) NOT NULL,
  "amount" DECIMAL(14,2) NOT NULL,
  "effective_at" TIMESTAMPTZ(6) NOT NULL,
  "review_request_id" VARCHAR(50),
  "approved_by" VARCHAR(50),
  "reason" TEXT,
  "created_at" TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS "salary_rank_history_employee_id_effective_at_idx"
  ON "salary_rank_history" ("employee_id", "effective_at");

CREATE TABLE IF NOT EXISTS "salary_review_requests" (
  "request_id" VARCHAR(50) PRIMARY KEY,
  "employee_id" VARCHAR(50) NOT NULL,
  "current_salary_rank_id" VARCHAR(50),
  "proposed_salary_rank_id" VARCHAR(50) NOT NULL,
  "proposed_amount" DECIMAL(14,2) NOT NULL,
  "requested_by" VARCHAR(50),
  "reason" TEXT NOT NULL,
  "status" VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  "decision_note" TEXT,
  "reviewed_by" VARCHAR(50),
  "requested_at" TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "reviewed_at" TIMESTAMPTZ(6)
);
CREATE INDEX IF NOT EXISTS "salary_review_requests_employee_id_status_idx"
  ON "salary_review_requests" ("employee_id", "status");
