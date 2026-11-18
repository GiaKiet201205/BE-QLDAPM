-- Add optional campus to historical teaching assignments, required by new writes at API layer.
ALTER TABLE "teaching_schedules" ADD COLUMN "campus" VARCHAR(100);

-- Protect assessment upserts against duplicate submissions, including concurrent requests.
-- Existing duplicate results must be reviewed and reconciled before applying this migration;
-- do not delete academic history automatically.
CREATE UNIQUE INDEX "student_result_unique_assignment"
  ON "student_results" ("student_id", "class_id", "assignment_id");
CREATE UNIQUE INDEX "student_result_unique_exam"
  ON "student_results" ("student_id", "class_id", "exam_id");
