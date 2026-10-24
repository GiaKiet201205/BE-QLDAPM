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

## Class date changes and lesson history (verified in the same feature PR)

| Class status | Change start date | Change end date | Timetable policy |
|---|---|---|---|
| DRAFT | Allowed | Allowed | Edit and save the plan explicitly; never silently rewrite assigned work |
| READY | Conditionally allowed | Conditionally allowed | Every active lesson and active staff assignment must fit new range |
| RUNNING | Locked | Conditionally allowed | Append future sessions only, or cancel/move an individual future session via the reschedule API |
| COMPLETED / CLOSED | Locked | Locked | Historical dates and assignments are immutable |

**Shrinking:** `PUT /api/classes/{id}` rejects dates that exclude planned lessons or active assignments. For READY, TC and CM can soft-cancel assignments for affected future lessons, Admin/CS removes those future slots from the Schedule plan, then the class range can be shortened. For RUNNING, use the existing future lesson cancel/reschedule workflow first; class start date is locked. Past lessons must not be deleted.

**Extending:** change the permitted class end date first, then add future planned lessons (the class timetable editor supports append-only on RUNNING). TC/CM explicitly staff the new lessons after availability and conflict checks. Previously assigned sessions stay attached to their original dates. The frontend reloads timetable data when either class date changes.

**Regression checks:** reject RUNNING start-date edits; reject COMPLETED/CLOSED date edits; reject shortening across planned or assigned sessions; append new future lessons on RUNNING; reject deletion/edit of existing RUNNING lessons or past READY lessons; verify extending refreshes Schedule without moving pre-existing assignments.


## CS visibility, course management and attendance permissions

The CS Class Management list intentionally includes classes a CS **created**, classes a CM assigned them to manage **for the full course**, and classes where they support **at least one currently assigned lesson**. Visibility is **not** proof of assignment to every lesson.

The class list and detail payloads expose `csAccessLevel` for the authenticated CS:

- `CREATOR`: CS created the class and retains an active `CREATED_BY_CS` scope. Can edit class information, student roster and timetable while DRAFT/READY, but not when RUNNING. **Does not automatically earn teacher attendance rights.**
- `MANAGER`: CM explicitly assigned the CS to the entire course and all planned lessons are covered. This grants class-level management rights via `class_access_scopes.source='CM_ASSIGNED_COURSE'`. A replacement for one lesson does not automatically replace the course manager.
- `SUPPORT`: CS has at least one active `staff_schedules` CLASS_SUPPORT entry. Can view the class and sessions, but **cannot** edit class metadata, roster, status or full timetable.
- No scope and no active support assignment: not listed to that CS.

CM assigns `scope:'COURSE'` through the range API to establish a course manager. The server validates all active planned lessons are covered by the chosen CS **before** granting the scope, within the same transaction. An existing complete support timetable can be confirmed again without duplicating shifts. Assigning `scope:'PERIOD'` or `'SINGLE'` does not elevate permissions. Legacy range clients with no scope use PERIOD behavior.

**Attendance is always narrower than class visibility:** an authenticated CS must have an active CLASS_SUPPORT assignment with matching `classId/date/startTime/endTime` to record PRESENT/ABSENT/LATE for that lesson, including past lessons. This is enforced server-side; a CREATOR or MANAGER title alone does not permit marks on uncovered lessons.

**Revocation:** CM replaces a whole-course manager by performing another validated COURSE assignment. On course replacement the previous CM-assigned scope is disabled. If a CM cancels the final class-support shift held by a CM-assigned manager, their course-manager scope is also deactivated. A one-off substitute does not inherit class-level permissions.

### Key regression scenarios

1. CS who originally created a DRAFT class sees **CS tạo lớp** and can edit its draft details, but cannot mark a teacher without an exact dated support shift.
2. CS who only supports one lesson sees **Hỗ trợ từng buổi**, can view Schedule, but gets 403 from PUT class details, student roster edits and planned-session replace.
3. CM selects full-course staffing for CS001; once all active lessons have CS001, Class API reports **MANAGER**. CS001 can manage the class and mark any past lesson they actually covered.
4. CM selects only one lesson for CS002. CS002 receives support visibility, but has no ability to edit the whole class or mark an unrelated lesson.
5. A class creator cannot keep editing a RUNNING course unless CM has separately assigned them as course manager.
6. CM reconfirms a pre-existing fully staffed course for CS001; no duplicate shift records are inserted.
7. Cancelling CS001's final CLASS_SUPPORT shift revokes CM course-manager scope.
8. Other CS roles, Teacher, Admin and Payroll remain outside the new permission model.
