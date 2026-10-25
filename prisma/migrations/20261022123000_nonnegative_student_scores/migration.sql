-- Defend grade integrity against non-API writes as well as REST requests.
ALTER TABLE "student_results"
  ADD CONSTRAINT "student_results_nonnegative_score_check" CHECK ("score" >= 0);
