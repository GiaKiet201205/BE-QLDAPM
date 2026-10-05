# Database setup — PostgreSQL / Supabase + Prisma

Backend runtime remains **Spring Boot + Spring Data JPA**. Prisma is used as the
single source of truth for **database schema, migration and seed data**.

This avoids having both Hibernate `ddl-auto` and Prisma changing the schema.
Hibernate is configured with `ddl-auto: validate`.

## 1. Business model aligned with FE

The initial schema is based on the completed Student/Class flow in
`Santadura/FE-QLDAPM-clone`:

- Student management: unique student code, optional email/phone, status.
- Class lifecycle: `DRAFT -> READY -> RUNNING -> COMPLETED -> CLOSED`.
- Course and class are separated.
- Student membership is modeled through `class_students`; removing a student
  from a class changes membership status to `INACTIVE` so history is kept.
- CS access to classes is modeled through `class_access_scopes`.
- Teacher assignment is modeled through `teaching_schedules`.
- CS/class support assignment is modeled through `staff_schedules`.
- Assignments and exams belong to an assigned teacher and class.
- Student result belongs to exactly one Assignment or one Exam.
- Audit log supports important Student/Class operations and admin override flows.

## 2. Local PostgreSQL

Requirements:

- Java 17+
- Maven 3.9+
- Node.js 20+
- Docker (recommended)

Create the local environment file:

```bash
cp .env.example .env
```

Start PostgreSQL:

```bash
docker compose up -d
```

Install Prisma tooling and generate the client:

```bash
npm install
npm run prisma:generate
```

Apply committed migrations:

```bash
npm run db:migrate:deploy
```

Seed demo data:

```bash
npm run db:seed
```

Start Spring Boot:

```bash
mvn spring-boot:run
```

Spring Boot reads the root `.env` file automatically through
`spring.config.import`.

## 3. Supabase

Create a Supabase project and copy the PostgreSQL connection information from
the project database settings.

Fill the following variables in a local `.env` file:

```properties
DB_URL=jdbc:postgresql://<supabase-host>:5432/postgres?sslmode=require
DB_USERNAME=<database-user>
DB_PASSWORD=<database-password>

DATABASE_URL=postgresql://<pooler-user>:<password>@<pooler-host>:6543/postgres?pgbouncer=true
DIRECT_URL=postgresql://<direct-user>:<password>@<direct-host>:5432/postgres?sslmode=require
```

Use `DIRECT_URL` for migrations. `DATABASE_URL` is used by Prisma client
operations/seed.

Then run:

```bash
npm install
npm run prisma:generate
npm run db:migrate:deploy
npm run db:seed
mvn spring-boot:run
```

Never commit a real Supabase password or project secret.

## 4. Migration workflow

After changing `prisma/schema.prisma` during development:

```bash
npm run db:migrate -- --name <migration_name>
```

Commit both:

- `prisma/schema.prisma`
- generated `prisma/migrations/<timestamp>_<migration_name>/migration.sql`

For shared/test/production databases, apply committed migrations only:

```bash
npm run db:migrate:deploy
```

Do not use Hibernate `ddl-auto: update`.

## 5. Current seed account

The seed creates a demo admin account:

```text
username: admin
password: 123456
```

This is for development only. Change/remove demo credentials before deployment.

## 6. Important database constraints

The migration enforces rules that should not rely only on FE validation:

- class code and student code are unique;
- class end date cannot be before start date;
- class status follows the FE-supported set;
- one student has one membership record per class;
- Student/Class history is preserved instead of hard deletion;
- schedule end time must be after start time;
- one result points to exactly one Assignment or one Exam;
- one student can have only one result for the same Assignment or Exam.

Authorization rules such as “Admin can manage all classes”, “CS only manages
assigned classes” and “Teacher only grades assigned classes” are application
service-layer rules and must also be enforced by BE APIs when those modules are
implemented.
