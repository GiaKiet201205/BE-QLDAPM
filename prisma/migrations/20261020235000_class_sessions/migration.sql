-- Planned class sessions are the source of truth for class time slots.
-- Do not rewrite existing teaching/staff schedules during migration.
CREATE TABLE "class_sessions" (
  "class_session_id" VARCHAR(50) NOT NULL PRIMARY KEY,
  "class_id" VARCHAR(50) NOT NULL REFERENCES "classes"("class_id") ON DELETE RESTRICT,
  "date" DATE NOT NULL,
  "start_time" TIME(0) NOT NULL,
  "end_time" TIME(0) NOT NULL,
  "status" VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
  "created_at" TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "class_sessions_time_valid" CHECK ("end_time" > "start_time"),
  CONSTRAINT "class_sessions_status_valid" CHECK ("status" IN ('PLANNED', 'CANCELLED')),
  CONSTRAINT "class_sessions_unique_slot" UNIQUE ("class_id","date","start_time","end_time")
);
CREATE INDEX "class_sessions_class_date_idx" ON "class_sessions"("class_id","date");
-- Preserve already assigned teaching sessions as planned sessions (legacy compatibility).
INSERT INTO "class_sessions" ("class_session_id","class_id","date","start_time","end_time")
SELECT 'legacy-' || ROW_NUMBER() OVER (ORDER BY class_id,date,start_time,end_time),
       class_id,date,start_time,end_time
FROM (SELECT DISTINCT class_id,date,start_time,end_time FROM teaching_schedules WHERE status='ASSIGNED') AS slots
ON CONFLICT ("class_id","date","start_time","end_time") DO NOTHING;
