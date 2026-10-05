-- Initial database schema for IIG QLDAPM
-- Managed by Prisma migrations. Spring Boot/Hibernate validates this schema at runtime.

CREATE TABLE "users" (
    "id" BIGSERIAL PRIMARY KEY,
    "username" VARCHAR(50) NOT NULL UNIQUE,
    "email" VARCHAR(255) NOT NULL UNIQUE,
    "password" VARCHAR(255) NOT NULL,
    "full_name" VARCHAR(120),
    "role" VARCHAR(20) NOT NULL DEFAULT 'USER',
    "status" VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "students" (
    "id" BIGSERIAL PRIMARY KEY,
    "student_code" VARCHAR(30) NOT NULL UNIQUE,
    "full_name" VARCHAR(120) NOT NULL,
    "email" VARCHAR(255),
    "phone" VARCHAR(30),
    "status" VARCHAR(20) NOT NULL DEFAULT 'Active',
    "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "courses" (
    "id" BIGSERIAL PRIMARY KEY,
    "code" VARCHAR(30) NOT NULL UNIQUE,
    "name" VARCHAR(120) NOT NULL,
    "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "classes" (
    "id" BIGSERIAL PRIMARY KEY,
    "course_id" BIGINT NOT NULL REFERENCES "courses"("id") ON DELETE RESTRICT,
    "class_code" VARCHAR(40) NOT NULL UNIQUE,
    "name" VARCHAR(150) NOT NULL,
    "start_date" DATE NOT NULL,
    "end_date" DATE NOT NULL,
    "status" VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    "created_by" BIGINT REFERENCES "users"("id") ON DELETE SET NULL,
    "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "classes_date_range_check" CHECK ("end_date" >= "start_date"),
    CONSTRAINT "classes_status_check" CHECK ("status" IN ('DRAFT','READY','RUNNING','COMPLETED','CLOSED'))
);

CREATE INDEX "classes_course_id_idx" ON "classes"("course_id");
CREATE INDEX "classes_status_idx" ON "classes"("status");

CREATE TABLE "class_students" (
    "id" BIGSERIAL PRIMARY KEY,
    "class_id" BIGINT NOT NULL REFERENCES "classes"("id") ON DELETE RESTRICT,
    "student_id" BIGINT NOT NULL REFERENCES "students"("id") ON DELETE RESTRICT,
    "status" VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "class_students_unique" UNIQUE ("class_id", "student_id"),
    CONSTRAINT "class_students_status_check" CHECK ("status" IN ('ACTIVE','INACTIVE'))
);

CREATE INDEX "class_students_student_status_idx" ON "class_students"("student_id","status");

CREATE TABLE "class_access_scopes" (
    "id" BIGSERIAL PRIMARY KEY,
    "user_id" BIGINT NOT NULL REFERENCES "users"("id") ON DELETE RESTRICT,
    "class_id" BIGINT NOT NULL REFERENCES "classes"("id") ON DELETE RESTRICT,
    "role" VARCHAR(20) NOT NULL,
    "status" VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    "source" VARCHAR(30) NOT NULL,
    "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "class_access_scopes_unique" UNIQUE ("user_id","class_id","role"),
    CONSTRAINT "class_access_scopes_status_check" CHECK ("status" IN ('ACTIVE','INACTIVE'))
);

CREATE INDEX "class_access_scopes_class_status_idx" ON "class_access_scopes"("class_id","status");

CREATE TABLE "teaching_schedules" (
    "id" BIGSERIAL PRIMARY KEY,
    "teacher_id" BIGINT NOT NULL REFERENCES "users"("id") ON DELETE RESTRICT,
    "class_id" BIGINT NOT NULL REFERENCES "classes"("id") ON DELETE RESTRICT,
    "date" DATE NOT NULL,
    "start_time" TIME NOT NULL,
    "end_time" TIME NOT NULL,
    "status" VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED',
    "assigned_by" BIGINT REFERENCES "users"("id") ON DELETE SET NULL,
    "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "teaching_schedule_time_check" CHECK ("end_time" > "start_time")
);

CREATE INDEX "teaching_schedules_teacher_date_idx" ON "teaching_schedules"("teacher_id","date");
CREATE INDEX "teaching_schedules_class_date_idx" ON "teaching_schedules"("class_id","date");

CREATE TABLE "staff_schedules" (
    "id" BIGSERIAL PRIMARY KEY,
    "user_id" BIGINT NOT NULL REFERENCES "users"("id") ON DELETE RESTRICT,
    "staff_role" VARCHAR(20) NOT NULL,
    "workplace_id" VARCHAR(50),
    "class_id" BIGINT REFERENCES "classes"("id") ON DELETE RESTRICT,
    "date" DATE NOT NULL,
    "start_time" TIME NOT NULL,
    "end_time" TIME NOT NULL,
    "work_type" VARCHAR(30) NOT NULL,
    "status" VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED',
    "assigned_by" BIGINT REFERENCES "users"("id") ON DELETE SET NULL,
    "assignment_source" VARCHAR(30) NOT NULL DEFAULT 'STANDARD',
    "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "staff_schedule_time_check" CHECK ("end_time" > "start_time")
);

CREATE INDEX "staff_schedules_user_date_idx" ON "staff_schedules"("user_id","date");
CREATE INDEX "staff_schedules_class_date_idx" ON "staff_schedules"("class_id","date");

CREATE TABLE "assignments" (
    "id" BIGSERIAL PRIMARY KEY,
    "teacher_id" BIGINT NOT NULL REFERENCES "users"("id") ON DELETE RESTRICT,
    "class_id" BIGINT NOT NULL REFERENCES "classes"("id") ON DELETE RESTRICT,
    "title" VARCHAR(180) NOT NULL,
    "description" TEXT,
    "deadline" DATE NOT NULL,
    "status" VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX "assignments_class_status_idx" ON "assignments"("class_id","status");

CREATE TABLE "exams" (
    "id" BIGSERIAL PRIMARY KEY,
    "teacher_id" BIGINT NOT NULL REFERENCES "users"("id") ON DELETE RESTRICT,
    "class_id" BIGINT NOT NULL REFERENCES "classes"("id") ON DELETE RESTRICT,
    "title" VARCHAR(180) NOT NULL,
    "description" TEXT,
    "duration" INTEGER,
    "exam_date" DATE NOT NULL,
    "status" VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "exam_duration_check" CHECK ("duration" IS NULL OR "duration" > 0)
);

CREATE INDEX "exams_class_status_idx" ON "exams"("class_id","status");

CREATE TABLE "student_results" (
    "id" BIGSERIAL PRIMARY KEY,
    "student_id" BIGINT NOT NULL REFERENCES "students"("id") ON DELETE RESTRICT,
    "class_id" BIGINT NOT NULL REFERENCES "classes"("id") ON DELETE RESTRICT,
    "assignment_id" BIGINT REFERENCES "assignments"("id") ON DELETE RESTRICT,
    "exam_id" BIGINT REFERENCES "exams"("id") ON DELETE RESTRICT,
    "score" DECIMAL(5,2) NOT NULL,
    "feedback" TEXT,
    "evaluated_by" BIGINT NOT NULL REFERENCES "users"("id") ON DELETE RESTRICT,
    "evaluated_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "student_result_activity_check"
      CHECK (
        ("assignment_id" IS NOT NULL AND "exam_id" IS NULL)
        OR ("assignment_id" IS NULL AND "exam_id" IS NOT NULL)
      )
);

CREATE INDEX "student_results_student_class_idx" ON "student_results"("student_id","class_id");
CREATE INDEX "student_results_assignment_idx" ON "student_results"("assignment_id");
CREATE INDEX "student_results_exam_idx" ON "student_results"("exam_id");

CREATE UNIQUE INDEX "student_results_assignment_unique"
ON "student_results"("student_id","assignment_id")
WHERE "assignment_id" IS NOT NULL;

CREATE UNIQUE INDEX "student_results_exam_unique"
ON "student_results"("student_id","exam_id")
WHERE "exam_id" IS NOT NULL;

CREATE TABLE "audit_logs" (
    "id" BIGSERIAL PRIMARY KEY,
    "user_id" BIGINT REFERENCES "users"("id") ON DELETE SET NULL,
    "action" VARCHAR(80) NOT NULL,
    "entity_type" VARCHAR(40) NOT NULL,
    "entity_id" VARCHAR(80) NOT NULL,
    "details" JSONB,
    "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX "audit_logs_entity_idx" ON "audit_logs"("entity_type","entity_id");
CREATE INDEX "audit_logs_user_created_idx" ON "audit_logs"("user_id","created_at");
