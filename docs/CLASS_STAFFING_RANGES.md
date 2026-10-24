# Whole-course / date-range staffing for TC and CM

## Scope and roles

- **TC** assigns Teachers to a class, defaulting to its **entire planned course**.
- **CM** assigns CS to support the class, also defaulting to **the entire planned course**.
- Both have an alternate date range (weeks, months, start date onward) and a single-session option.
- The timetable belongs to `class_sessions`; no scheduling mutation invents new class lessons.
- Each valid selected lesson is stored separately in `teaching_schedules` or `staff_schedules`, rather than one giant recurring shift. Thus Teacher/CS can be replaced in one lesson without changing the rest of the course.
- The old weekly batch endpoints still work (up to 50 sessions) for compatibility. New range API accepts up to **400 planned sessions** (matching class plan cap).

## API

`GET /api/class-planning/{classId}/staffing?role=TEACHER|CS`

TC can request TEACHER; CM can request CS. Returns every active planned lesson with matching active assignment ID, person, assignment count, and teacher-attendance state (if any). The FE uses this to preview the whole course, occupied sessions, and the expected write count.

`POST /api/class-planning/{classId}/staffing`

```json
{
  "role": "TEACHER",
  "employeeId": "teacher-001",
  "campus": "Cơ sở chính",
  "mode": "FILL_EMPTY",
  "sessionIds": ["class-session-a", "class-session-b"]
}
```

For CS, set `"role":"CS"` and `"workplaceId":"center-01"`. Use `FILL_EMPTY` for first assignment and open slots; a slot already assigned to another employee results in an error if included. The FE excludes occupied slots by default.

Use `REPLACE` when replacing existing staff for the selected lessons. The FE requires an explicit checkbox and confirmation. The operation:
1. Locks the relevant employee and class, checks target role and all session IDs.
2. Checks every chosen session against its actual `class_sessions` row.
3. Leaves already-owned sessions untouched.
4. For a replacement, cancels the old assignment (retaining ID, prior CS/Teacher, dates and any linked attendance) and inserts a **new** assignment row; never moves `teacher_session_attendance` to the substitute.
5. Checks existing approved availability, same-time staff/class conflicts, and class life-cycle constraints using the existing assignment services.
6. **All-or-nothing transaction**; one invalid session causes the whole request to roll back. Errors show the failing date/time.

For the historical rule, a replacement of a lesson that has already begun is rejected, **except** that TC can substitute one teacher for a previous lesson with the original teacher explicitly marked `ABSENT`. A teacher recorded `PRESENT` or `LATE` cannot be replaced retroactively. A CS takeover does not backdate replacement coverage.

## Historical CS teacher attendance

`teacher_session_attendance` remains the payroll input, separate from assignment status.

- CS can mark `PRESENT` / `ABSENT` / `LATE` on any **past** lesson they were specifically scheduled to support as `CLASS_SUPPORT`, including earlier weeks/months of a whole-course assignment.
- Lesson has to have started. The currently assigned CS for the exact class + date + start/end can record or correct it. This retains `recorded_by`, timestamps, and an audit log.
- A CS assigned as substitute from a later date is **not** implicitly authorized to edit earlier lessons: permissions follow each dated support assignment.
- No `TEACHER` self-attendance, student attendance, check-in/out time tracking, or payroll calculation is added.

## Practical test matrix (requires local PostgreSQL)

1. TC picks a Ready class of ~20 lessons. Select Teacher001, default **Toàn bộ khóa**. All eligible lessons become assigned and visible in teaching_schedules without adding/removing planned lessons.
2. CM repeats for CS001. Confirm the resulting staff_schedules rows are CLASS_SUPPORT and mirror each class lesson.
3. Pick "Khoảng ngày" (e.g. 4-5 weeks). Check preview counts; confirm only the selected dates receive assignments.
4. Select REPLACE for Teacher002 for one **future** lesson. Old teaching schedule becomes CANCELLED and remains in DB; substitute gets a new ASSIGNED row. Other lessons stay Teacher001. Repeat with CS002.
5. Replace Teacher001 with Teacher003 for future date onward. Already present Teacher001 lessons remain unchanged; only chosen future dates are replaced.
6. Mark a **past** lesson as PRESENT, reload, update to LATE: the same attendance row persists and keeps the correct Teacher ID via teaching_schedule_id.
7. Mark one past Teacher001 session ABSENT, then have TC replace Teacher001 only for that session; the ABSENT record remains against the cancelled old assignment, substitute receives a new assignment.
8. Attempt to replace a past PRESENT/LATE teacher assignment: 409, no changes.
9. With CS002 now assigned for later dates, attempt to mark an earlier Teacher lesson formerly covered by CS001: 403.
10. Select a month with one teacher unavailable or one staff time conflict. Expect a date-specific error and **no partial saved assignments**.
11. Try direct legacy PUT to change an existing Teacher or CLASS_SUPPORT CS to a different employee. Expect 409 directing caller to the replacement API.
12. Verify old Payroll screens/data have not changed; the separate payroll module can join attendance to teaching_schedules by teaching_schedule_id.

## Deployment

- The previous teacher-attendance migration `20261024150000_teacher_session_attendance` is required before using the new endpoints.
- `npx prisma migrate deploy` and `npx prisma generate` against the correct BE database.
- Build and verify FE/BE before merge. The GitHub changes are **not** claimed to have been integration tested locally.
- Currently, newly added class lessons are **not automatically staffed**. The TC/CM can use FILL_EMPTY to assign the new lessons; this avoids assigning personnel without re-checking their availability.
