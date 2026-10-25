-- CS records whether an assigned teacher actually attended a specific class lesson.
-- Teaching assignment status (ASSIGNED/CANCELLED) is never treated as attendance.
CREATE TABLE "teacher_session_attendance" (
    "attendance_id" VARCHAR(50) NOT NULL PRIMARY KEY,
    "teaching_schedule_id" VARCHAR(50) NOT NULL,
    "status" VARCHAR(20) NOT NULL,
    "recorded_by" VARCHAR(50) NOT NULL,
    "recorded_at" TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMPTZ(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "teacher_attendance_schedule_fk"
        FOREIGN KEY ("teaching_schedule_id") REFERENCES "teaching_schedules"("teaching_schedule_id") ON DELETE RESTRICT,
    CONSTRAINT "teacher_attendance_recorder_fk"
        FOREIGN KEY ("recorded_by") REFERENCES "employees"("employee_id") ON DELETE RESTRICT,
    CONSTRAINT "teacher_attendance_status_check"
        CHECK ("status" IN ('PRESENT','ABSENT','LATE')),
    CONSTRAINT "teacher_attendance_unique_schedule"
        UNIQUE ("teaching_schedule_id")
);

CREATE INDEX "teacher_attendance_recorded_by_idx"
    ON "teacher_session_attendance" ("recorded_by", "recorded_at");
