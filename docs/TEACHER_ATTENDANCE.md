# Teacher attendance per class session (CS verification)

## Scope

CS records one of `PRESENT` (Có mặt), `ABSENT` (Vắng mặt), or `LATE` (Đi muộn) for a teacher **assigned to a particular planned class lesson**. Neither QR check-in nor actual start/end time nor wage calculation is implemented.

- `class_sessions`: class timetable (`PLANNED`, `CANCELLED`)
- `teaching_schedules`: teacher assignment (`ASSIGNED`, `CANCELLED`)
- `teacher_session_attendance`: **actual teacher attendance**, one row per `teaching_schedule_id`; no row means *not yet recorded*

The new table has `attendance_id`, `teaching_schedule_id` (FK, UNIQUE), `status` (PRESENT/ABSENT/LATE), `recorded_by` (CS employee FK), `recorded_at`, `updated_at`. Prior attendance is not deleted if a teacher assignment is cancelled or the class lesson is rescheduled. Old records remain as history; consumers must filter currently valid teaching assignments when deriving eligible paid lessons.

## Rights and validation

1. A CS can open the **Điểm danh GV** tab for a class they manage or support.
2. A CS can **save** attendance only for a specific lesson where they have an active `staff_schedules` assignment with `work_type='CLASS_SUPPORT'`, matching class/date/start/end.
3. The teacher must have an `ASSIGNED` teaching schedule matching a `PLANNED` class session.
4. Attendance may only be recorded after the lesson's scheduled start, using Asia/Ho_Chi_Minh time.
5. Only `PRESENT`, `ABSENT`, `LATE` are accepted. Subsequent corrections update the same row and create an `audit_logs` event. Unrecorded does **not** mean absent.
6. These endpoints do not write salary or payroll tables.

## API (Bearer JWT, role CS)

`GET /api/classes/{classId}/teacher-attendance`

Lists active teacher assignments for planned sessions, with `attendanceStatus` (nullable) and `assignedToMe` for the current CS. Useful for the class attendance tab.

`PUT /api/classes/{classId}/teacher-attendance/{teachingScheduleId}`

Body: `{"status":"PRESENT"}` (or `ABSENT`, `LATE`). Returns the persisted attendance record. Repeat same status is idempotent.

## Read contract for the separate payroll module

Do not equate a teaching schedule's `ASSIGNED` state with actual attendance. Read the new table, JOIN its `teaching_schedule_id` to the original assignment. Example **data retrieval** query, *not* a payroll formula:

```sql
SELECT t.teacher_id, t.class_id, t.date,
       t.start_time, t.end_time,
       a.status AS attendance_status, a.recorded_at, a.updated_at
FROM teacher_session_attendance a
JOIN teaching_schedules t
  ON t.teaching_schedule_id = a.teaching_schedule_id
WHERE t.status = 'ASSIGNED';
```

`ABSENT` does not count as presence. Whether `LATE` earns a full session, partial session, or no paid session is a separate **Payroll business rule** and is intentionally not decided here.

## Deploy and smoke tests

1. Apply migration `20261024150000_teacher_session_attendance` using `npx prisma migrate deploy`. Run `npx prisma generate` after schema changes.
2. Start Spring Boot and FE from the matching feature branches.
3. Create a class with planned lessons, approve Teacher and CS availability, assign Teacher through TC and CS support through CM to the **same lesson**.
4. Log in as that CS, open Class Management > class > **Điểm danh GV**. Before the scheduled start, saving must be disabled/409.
5. After the start, save `PRESENT`, reload, then change to `LATE`. Exactly one attendance row should remain, with updated status; audit records should show both saves.
6. Another CS not assigned to that exact lesson must get 403 for PUT.
7. An unauthenticated user or Teacher/TC/CM must not save attendance.
8. Cancel or reschedule the teaching assignment; the old attendance row stays for history but cannot be changed and should not be counted as a current assigned teaching session.

No database integration or browser E2E tests are claimed without an actual running PostgreSQL + API.
