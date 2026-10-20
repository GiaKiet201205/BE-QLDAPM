# Integrated Student, Class, Availability & Assignment Workflow

This workflow is the cross-role operating contract for the MVP. Class lesson times are planned independently of individual teachers and class-support staff.

## Sequence and owners

1. **Admin / authorized CS** creates a class in `DRAFT`, defining course, class code, target requirements and overall date range. A Draft class may have no lessons.
2. **Admin / managing CS** enters exact dated **planned class sessions** (one-off or recurring weekday generation on FE). Each active session contains `date`, `startTime`, `endTime`. Save with `PUT /api/classes/{id}/planned-sessions`.
3. **Admin / managing CS** moves `DRAFT -> READY` only after at least one planned session exists.
4. **Teacher / CS / Sale** registers availability. **TC** reviews Teacher availability; **CM** reviews CS/Sale availability.
5. **TC** assigns Teacher to a planned session. **CM** independently assigns CS to planned `CLASS_SUPPORT` sessions; Sale shifts use their own schedule. Assignment must match a planned slot exactly and fit approved availability without overlap. Batch endpoints allow up to 50 lesson IDs in one DB transaction.
6. **Admin / managing CS** moves `READY -> RUNNING` only when **every active planned lesson has an assigned Teacher**. CS support is tracked separately, not a blanket prerequisite to Running.
7. **Teacher** sees assigned classes/schedules, creates assignments/exams and records student results under class/roster lifecycle rules. **Admin / managing CS** manages students and roster eligibility.
8. For `RUNNING`, a **future lesson** may be cancelled or rescheduled only by Admin / managing CS with a reason (at least five characters). The prior lesson and its Teacher/CS assignments are cancelled; a replacement lesson, if specified, is unassigned and TC/CM must staff it. Audit history is retained.
9. Close or cancel pending teaching activities, then move `RUNNING -> COMPLETED -> CLOSED`.

## Ownership of timetable and access

- `class_sessions`: planned lesson times, owned by Admin / managing CS; distinct from personnel.
- `teaching_schedules`: a Teacher's assigned date/time/class, owned by TC.
- `staff_schedules`: an employee's assigned work, owned by CM. Only `CS + CLASS_SUPPORT` connects directly to a planned class lesson.
- `class_access_scopes` grants explicit CS class access, and an **active** `CLASS_SUPPORT` assignment also grants dynamic class-management scope. Cancelling the final support assignment removes only that dynamic scope, not a separately granted explicit scope.
- Student participation remains tracked in `class_students`. Removing a student sets the membership inactive, preserving academic history.

## API checklist

| Actor | Action | Endpoint |
|---|---|---|
| Admin, managing CS | List & edit class plan | `GET/PUT /api/classes/{id}/planned-sessions` |
| Admin, managing CS | Cancel or reschedule future running lesson | `POST /api/classes/{id}/reschedule` |
| TC, CM, Admin | List class planning metadata | `GET /api/class-planning` |
| TC | Assign one / many teacher sessions | `POST /api/teaching-schedules`, `POST /api/teaching-schedules/batch` |
| CM | Assign one / many staff sessions | `POST /api/staff-schedules`, `POST /api/staff-schedules/batch` |
| Teacher, CS, Sale | Maintain personal availability | `/api/availabilities/me`, `/api/availabilities` |
| TC / CM | Approve or reject availability | `PATCH /api/availabilities/{id}/review` |
| Teacher, CS, Sale | Personal assigned timetable | `GET /api/schedules/me` |
| Admin, CS, Teacher | Read their authorized class details | `GET /api/classes/{id}` |

## Test scenarios (requires PostgreSQL + running Spring Boot + FE)

1. Create a Draft class without a plan. **READY must fail**.
2. Add weekday recurring lessons or a single lesson; save; reload page and verify `class_sessions`.
3. Make class READY; TC tries to staff without approved Teacher availability: **409**. Approve Teacher availability; staff the exact slot successfully.
4. Attempt a Teacher assignment for a time **not** in the class plan: **409**. Try conflicting teaching duties: **409**.
5. Attempt to move to RUNNING with any unstaffed planned lesson: **409**. Assign all active lessons; RUNNING succeeds.
6. CM approves CS availability and assigns a matching `CLASS_SUPPORT` slot. Login as the assigned CS: class appears in scoped Class Management; cancel that support shift and verify dynamic access disappears (unless explicit scope exists).
7. As Admin / managing CS, reschedule a future RUNNING lesson. Verify original planned session = CANCELLED, old Teacher/CS duties = CANCELLED, replacement planned session = PLANNED, audit exists. New lesson requires a fresh assignment. Try rescheduling a past lesson: reject.
8. Remove student from roster; ensure `class_students.status = INACTIVE` and active student count falls without deleting the history.
9. Teacher records an Assignment/Exam result; verify persistence in `student_results`.
10. Test weekly availability vs recurring overlapping slot: it must be rejected for the same employee and weekday.

## Deployment and checks

**Migration must execute before BE starts against the new schema:**

```powershell
npm ci
npm run db:migrate:deploy
# On an empty development database only:
npm run db:seed
mvn clean test
mvn spring-boot:run
```

FE:

```powershell
npm ci
npm run build
npm run dev
```

**Do not rerun the development seed on a populated environment without reviewing its upserts.** For seeded accounts use password `123456` locally; this is not a production credential.

The older schema's assigned TeachingSchedule slots are copied into `class_sessions` by the migration, so previously staffed sessions remain visible without rewriting the old staff assignments.

No browser-driven end-to-end run is claimed by this document; verify the scenarios above against local PostgreSQL.

## Troubleshooting: "bad SQL grammar FROM class_sessions"

This usually means the **running Spring Boot API points at a PostgreSQL database
where the new Prisma migration has not been applied**. The repository containing
the code does not automatically change an existing database when Spring Boot is
started locally.

From the **BE clone root**, with the same `DATABASE_URL` pointing to the
database configured in `DB_URL`:

```powershell
npm ci
npx prisma migrate status
npm run db:migrate:deploy
npx prisma migrate status
```

Confirm `20261020235000_class_sessions` is applied, then restart Spring Boot.
If the API uses Docker Compose instead, make sure `db-init` completed successfully
against the Compose PostgreSQL volume (e.g. `docker compose up --build -d`).
Do not run `prisma migrate reset` or delete a volume containing important data.

The BE now reports a friendly 503 with migration instructions when this table is
missing, rather than surfacing a raw SQL grammar exception.

## Creating a class with lessons

`POST /api/classes` accepts the existing class fields plus an optional
`plannedSessions` array:

```json
{
  "courseId": "course-ielts",
  "classCode": "IELTS-DEMO-01",
  "name": "IELTS Evening",
  "startDate": "2026-11-01",
  "endDate": "2026-12-01",
  "targetRequirements": [
    {"targetType": "OVERALL_BAND", "requiredTarget": 7.0}
  ],
  "plannedSessions": [
    {"date": "2026-11-02", "startTime": "18:00", "endTime": "20:00"}
  ]
}
```

The entire operation is transactional: if a planned session fails validation,
the class and its associated scope/targets are rolled back. Omit
`plannedSessions` to create a DRAFT class and add lessons later from Schedule.
Lesson planning cannot be omitted when transitioning to READY.
