-- Initial schema based on the approved ERD, then extended to match FE main.
-- PostgreSQL / Supabase compatible.

CREATE TABLE "roles" (
  "role_id" VARCHAR(50) PRIMARY KEY,
  "role_name" VARCHAR(50) NOT NULL UNIQUE,
  "description" VARCHAR(255)
);

CREATE TABLE "functions" (
  "function_id" VARCHAR(50) PRIMARY KEY,
  "function_name" VARCHAR(50) NOT NULL
);

CREATE TABLE "permissions" (
  "role_id" VARCHAR(50) NOT NULL REFERENCES "roles"("role_id") ON DELETE CASCADE,
  "function_id" VARCHAR(50) NOT NULL REFERENCES "functions"("function_id") ON DELETE CASCADE,
  "action" VARCHAR(30) NOT NULL,
  PRIMARY KEY ("role_id","function_id","action")
);

CREATE TABLE "users" (
  "username" VARCHAR(50) PRIMARY KEY,
  "email" VARCHAR(100) NOT NULL UNIQUE,
  "password_hash" VARCHAR(255) NOT NULL,
  "role_id" VARCHAR(50) NOT NULL REFERENCES "roles"("role_id") ON DELETE RESTRICT,
  "status" VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "salary_ranks" (
  "salary_rank_id" VARCHAR(50) PRIMARY KEY,
  "role_id" VARCHAR(50) NOT NULL REFERENCES "roles"("role_id") ON DELETE RESTRICT,
  "rank" VARCHAR(50) NOT NULL,
  "amount" DECIMAL(14,2) NOT NULL,
  "status" VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE ("role_id","rank")
);

CREATE TABLE "employees" (
  "employee_id" VARCHAR(50) PRIMARY KEY,
  "username" VARCHAR(50) NOT NULL UNIQUE REFERENCES "users"("username") ON DELETE RESTRICT,
  "employee_code" VARCHAR(30) NOT NULL UNIQUE,
  "full_name" VARCHAR(100) NOT NULL,
  "phone" VARCHAR(15),
  "specialization" VARCHAR(100),
  "salary_rank_id" VARCHAR(50) REFERENCES "salary_ranks"("salary_rank_id") ON DELETE SET NULL,
  "reviewed_date" TIMESTAMPTZ,
  "status" VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "certificates" (
  "certificate_id" VARCHAR(50) PRIMARY KEY,
  "employee_id" VARCHAR(50) NOT NULL REFERENCES "employees"("employee_id") ON DELETE RESTRICT,
  "certificate_type" VARCHAR(30) NOT NULL,
  "score" DOUBLE PRECISION,
  "score_sw" DOUBLE PRECISION,
  "issue_date" DATE,
  "expiry_date" DATE,
  "issuer" VARCHAR(100),
  "certificate_url" VARCHAR(500),
  "status" VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX "certificates_employee_idx" ON "certificates"("employee_id");

CREATE TABLE "courses" (
  "course_id" VARCHAR(50) PRIMARY KEY,
  "name" VARCHAR(150) NOT NULL,
  "description" TEXT,
  "level" VARCHAR(30),
  "duration" INTEGER,
  "status" VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "classes" (
  "class_id" VARCHAR(50) PRIMARY KEY,
  "course_id" VARCHAR(50) NOT NULL REFERENCES "courses"("course_id") ON DELETE RESTRICT,
  "class_code" VARCHAR(30) NOT NULL UNIQUE,
  "name" VARCHAR(150) NOT NULL,
  "start_date" DATE NOT NULL,
  "end_date" DATE NOT NULL,
  "status" VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  "created_by" VARCHAR(50) REFERENCES "users"("username") ON DELETE SET NULL,
  "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "classes_date_check" CHECK ("end_date" >= "start_date"),
  CONSTRAINT "classes_status_check" CHECK ("status" IN ('DRAFT','READY','RUNNING','COMPLETED','CLOSED'))
);
CREATE INDEX "classes_course_idx" ON "classes"("course_id");
CREATE INDEX "classes_status_idx" ON "classes"("status");

CREATE TABLE "lessons" (
  "lesson_id" VARCHAR(50) PRIMARY KEY,
  "course_id" VARCHAR(50) NOT NULL REFERENCES "courses"("course_id") ON DELETE RESTRICT,
  "class_id" VARCHAR(50) REFERENCES "classes"("class_id") ON DELETE SET NULL,
  "title" VARCHAR(150) NOT NULL,
  "description" TEXT,
  "content_type" VARCHAR(30),
  "content_url" VARCHAR(500),
  "order_index" INTEGER NOT NULL DEFAULT 0,
  "status" VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX "lessons_course_order_idx" ON "lessons"("course_id","order_index");
CREATE INDEX "lessons_class_idx" ON "lessons"("class_id");

CREATE TABLE "availabilities" (
  "availability_id" VARCHAR(50) PRIMARY KEY,
  "employee_id" VARCHAR(50) NOT NULL REFERENCES "employees"("employee_id") ON DELETE RESTRICT,
  "available_date" DATE,
  "day_of_week" VARCHAR(10),
  "start_time" TIME NOT NULL,
  "end_time" TIME NOT NULL,
  "shift" VARCHAR(50),
  "availability_type" VARCHAR(30),
  "campus" VARCHAR(100),
  "note" VARCHAR(255),
  "status" VARCHAR(20) NOT NULL DEFAULT 'pending',
  "reviewed_at" TIMESTAMPTZ,
  "reviewed_by" VARCHAR(50) REFERENCES "employees"("employee_id") ON DELETE SET NULL,
  "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "availability_time_check" CHECK ("end_time" > "start_time"),
  CONSTRAINT "availability_scope_check" CHECK ("available_date" IS NOT NULL OR "day_of_week" IS NOT NULL)
);
CREATE INDEX "availabilities_employee_date_idx" ON "availabilities"("employee_id","available_date");
CREATE INDEX "availabilities_employee_day_shift_idx" ON "availabilities"("employee_id","day_of_week","shift");

CREATE TABLE "teaching_schedules" (
  "teaching_schedule_id" VARCHAR(50) PRIMARY KEY,
  "teacher_id" VARCHAR(50) NOT NULL REFERENCES "employees"("employee_id") ON DELETE RESTRICT,
  "class_id" VARCHAR(50) NOT NULL REFERENCES "classes"("class_id") ON DELETE RESTRICT,
  "date" DATE NOT NULL,
  "start_time" TIME NOT NULL,
  "end_time" TIME NOT NULL,
  "status" VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED',
  "assigned_by" VARCHAR(50) REFERENCES "employees"("employee_id") ON DELETE SET NULL,
  "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "teaching_schedule_time_check" CHECK ("end_time" > "start_time")
);
CREATE INDEX "teaching_teacher_date_idx" ON "teaching_schedules"("teacher_id","date");
CREATE INDEX "teaching_class_date_idx" ON "teaching_schedules"("class_id","date");

CREATE TABLE "staff_schedules" (
  "staff_schedule_id" VARCHAR(50) PRIMARY KEY,
  "employee_id" VARCHAR(50) NOT NULL REFERENCES "employees"("employee_id") ON DELETE RESTRICT,
  "class_id" VARCHAR(50) REFERENCES "classes"("class_id") ON DELETE RESTRICT,
  "workplace_id" VARCHAR(50),
  "date" DATE NOT NULL,
  "start_time" TIME NOT NULL,
  "end_time" TIME NOT NULL,
  "work_type" VARCHAR(30) NOT NULL,
  "status" VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED',
  "assigned_by" VARCHAR(50) REFERENCES "employees"("employee_id") ON DELETE SET NULL,
  "assignment_source" VARCHAR(30) NOT NULL DEFAULT 'STANDARD',
  "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "staff_schedule_time_check" CHECK ("end_time" > "start_time")
);
CREATE INDEX "staff_employee_date_idx" ON "staff_schedules"("employee_id","date");
CREATE INDEX "staff_class_date_idx" ON "staff_schedules"("class_id","date");

CREATE TABLE "students" (
  "student_id" VARCHAR(50) PRIMARY KEY,
  "student_code" VARCHAR(30) NOT NULL UNIQUE,
  "full_name" VARCHAR(100) NOT NULL,
  "phone" VARCHAR(15),
  "email" VARCHAR(100),
  "level" VARCHAR(30),
  "rl_target" INTEGER,
  "sw_target" INTEGER,
  "status" VARCHAR(20) NOT NULL DEFAULT 'Active',
  "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "student_status_check" CHECK ("status" IN ('Active','On Leave','Graduated'))
);

CREATE TABLE "student_targets" (
  "student_target_id" VARCHAR(50) PRIMARY KEY,
  "student_id" VARCHAR(50) NOT NULL REFERENCES "students"("student_id") ON DELETE CASCADE,
  "course_id" VARCHAR(50) NOT NULL REFERENCES "courses"("course_id") ON DELETE RESTRICT,
  "target_type" VARCHAR(40) NOT NULL,
  "target_value" DECIMAL(8,2) NOT NULL,
  "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE ("student_id","course_id","target_type")
);
CREATE INDEX "student_targets_course_type_idx" ON "student_targets"("course_id","target_type");

CREATE TABLE "class_target_requirements" (
  "class_target_requirement_id" VARCHAR(50) PRIMARY KEY,
  "class_id" VARCHAR(50) NOT NULL REFERENCES "classes"("class_id") ON DELETE CASCADE,
  "target_type" VARCHAR(40) NOT NULL,
  "required_target" DECIMAL(8,2) NOT NULL,
  "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE ("class_id","target_type")
);

CREATE TABLE "class_students" (
  "class_student_id" VARCHAR(50) PRIMARY KEY,
  "class_id" VARCHAR(50) NOT NULL REFERENCES "classes"("class_id") ON DELETE RESTRICT,
  "student_id" VARCHAR(50) NOT NULL REFERENCES "students"("student_id") ON DELETE RESTRICT,
  "status" VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  "joined_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "inactive_reason" VARCHAR(80),
  "inactivated_at" TIMESTAMPTZ,
  UNIQUE ("class_id","student_id"),
  CONSTRAINT "class_student_status_check" CHECK ("status" IN ('ACTIVE','INACTIVE'))
);
CREATE INDEX "class_students_student_status_idx" ON "class_students"("student_id","status");

CREATE TABLE "class_access_scopes" (
  "class_access_scope_id" VARCHAR(50) PRIMARY KEY,
  "employee_id" VARCHAR(50) NOT NULL REFERENCES "employees"("employee_id") ON DELETE RESTRICT,
  "class_id" VARCHAR(50) NOT NULL REFERENCES "classes"("class_id") ON DELETE RESTRICT,
  "status" VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  "source" VARCHAR(30) NOT NULL,
  "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE ("employee_id","class_id")
);
CREATE INDEX "class_access_scopes_class_status_idx" ON "class_access_scopes"("class_id","status");

CREATE TABLE "assignments" (
  "assignment_id" VARCHAR(50) PRIMARY KEY,
  "teacher_id" VARCHAR(50) NOT NULL REFERENCES "employees"("employee_id") ON DELETE RESTRICT,
  "class_id" VARCHAR(50) NOT NULL REFERENCES "classes"("class_id") ON DELETE RESTRICT,
  "title" VARCHAR(150) NOT NULL,
  "description" TEXT,
  "deadline" TIMESTAMPTZ NOT NULL,
  "status" VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "assignment_status_check" CHECK ("status" IN ('OPEN','CLOSED','CANCELLED'))
);
CREATE INDEX "assignments_class_status_idx" ON "assignments"("class_id","status");

CREATE TABLE "exams" (
  "exam_id" VARCHAR(50) PRIMARY KEY,
  "teacher_id" VARCHAR(50) NOT NULL REFERENCES "employees"("employee_id") ON DELETE RESTRICT,
  "class_id" VARCHAR(50) NOT NULL REFERENCES "classes"("class_id") ON DELETE RESTRICT,
  "title" VARCHAR(150) NOT NULL,
  "description" TEXT,
  "duration" INTEGER NOT NULL,
  "exam_date" TIMESTAMPTZ NOT NULL,
  "status" VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
  "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "exam_duration_check" CHECK ("duration" > 0),
  CONSTRAINT "exam_status_check" CHECK ("status" IN ('SCHEDULED','COMPLETED','CANCELLED'))
);
CREATE INDEX "exams_class_status_idx" ON "exams"("class_id","status");

CREATE TABLE "student_results" (
  "student_result_id" VARCHAR(50) PRIMARY KEY,
  "student_id" VARCHAR(50) NOT NULL REFERENCES "students"("student_id") ON DELETE RESTRICT,
  "class_id" VARCHAR(50) NOT NULL REFERENCES "classes"("class_id") ON DELETE RESTRICT,
  "assignment_id" VARCHAR(50) REFERENCES "assignments"("assignment_id") ON DELETE RESTRICT,
  "exam_id" VARCHAR(50) REFERENCES "exams"("exam_id") ON DELETE RESTRICT,
  "score" DECIMAL(5,2) NOT NULL,
  "feedback" TEXT,
  "evaluated_by" VARCHAR(50) NOT NULL REFERENCES "employees"("employee_id") ON DELETE RESTRICT,
  "evaluated_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "student_result_activity_check" CHECK (
    ("assignment_id" IS NOT NULL AND "exam_id" IS NULL)
    OR ("assignment_id" IS NULL AND "exam_id" IS NOT NULL)
  )
);
CREATE INDEX "student_results_student_class_idx" ON "student_results"("student_id","class_id");
CREATE INDEX "student_results_assignment_idx" ON "student_results"("assignment_id");
CREATE INDEX "student_results_exam_idx" ON "student_results"("exam_id");
CREATE UNIQUE INDEX "student_results_assignment_unique"
  ON "student_results"("student_id","assignment_id") WHERE "assignment_id" IS NOT NULL;
CREATE UNIQUE INDEX "student_results_exam_unique"
  ON "student_results"("student_id","exam_id") WHERE "exam_id" IS NOT NULL;

CREATE TABLE "employee_salaries" (
  "employee_salary_id" VARCHAR(50) PRIMARY KEY,
  "employee_id" VARCHAR(50) NOT NULL REFERENCES "employees"("employee_id") ON DELETE RESTRICT,
  "period" VARCHAR(20) NOT NULL,
  "total_sessions" INTEGER NOT NULL DEFAULT 0,
  "total_hours" DECIMAL(8,2) NOT NULL DEFAULT 0,
  "rate" DECIMAL(12,2) NOT NULL DEFAULT 0,
  "amount" DECIMAL(14,2) NOT NULL DEFAULT 0,
  "status" VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE ("employee_id","period")
);

CREATE TABLE "employee_salary_details" (
  "employee_salary_id" VARCHAR(50) PRIMARY KEY REFERENCES "employee_salaries"("employee_salary_id") ON DELETE CASCADE,
  "total_sessions" INTEGER NOT NULL DEFAULT 0,
  "salary_per_hours" DECIMAL(14,2) NOT NULL DEFAULT 0,
  "total_hours" DECIMAL(5,2) NOT NULL DEFAULT 0,
  "total_salary" DECIMAL(14,2) NOT NULL DEFAULT 0,
  "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "staff_salaries" (
  "staff_salary_id" VARCHAR(50) PRIMARY KEY,
  "employee_id" VARCHAR(50) NOT NULL REFERENCES "employees"("employee_id") ON DELETE RESTRICT,
  "period" VARCHAR(20) NOT NULL,
  "total_shifts" INTEGER NOT NULL DEFAULT 0,
  "total_hours" DECIMAL(8,2) NOT NULL DEFAULT 0,
  "rate" DECIMAL(12,2) NOT NULL DEFAULT 0,
  "amount" DECIMAL(14,2) NOT NULL DEFAULT 0,
  "status" VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  "managed_by" VARCHAR(50) REFERENCES "employees"("employee_id") ON DELETE SET NULL,
  "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE ("employee_id","period")
);

CREATE TABLE "audit_logs" (
  "audit_log_id" VARCHAR(50) PRIMARY KEY,
  "username" VARCHAR(50) REFERENCES "users"("username") ON DELETE SET NULL,
  "action" VARCHAR(100) NOT NULL,
  "entity_type" VARCHAR(50) NOT NULL,
  "entity_id" VARCHAR(50) NOT NULL,
  "description" TEXT,
  "details" JSONB,
  "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX "audit_logs_entity_idx" ON "audit_logs"("entity_type","entity_id");
CREATE INDEX "audit_logs_user_created_idx" ON "audit_logs"("username","created_at");
