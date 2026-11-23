const { test } = require('node:test');
const assert = require('node:assert/strict');
const { randomUUID } = require('node:crypto');
const { readFileSync } = require('node:fs');
const { spawnSync } = require('node:child_process');
const { PrismaClient } = require('@prisma/client');
const { parse } = require('dotenv');
const { seedDatabase } = require('../prisma/seed');

// Opt in: all writes happen in a randomly named temporary database, never the configured DB.
test('fresh seed, legacy refresh, idempotency and rollback preserve non-demo data', {
  skip: process.env.GK_SEED_DB_TEST !== 'true', timeout: 180000,
}, async () => {
  const file = parse(readFileSync('.env'));
  const source = new URL(process.env.DIRECT_URL || file.DIRECT_URL || process.env.DATABASE_URL || file.DATABASE_URL);
  const name = 'qlapm_seed_test_' + randomUUID().replaceAll('-', '');
  const admin = new PrismaClient({ datasourceUrl: source.toString() });
  const target = new URL(source);
  target.pathname = '/' + name;
  const db = new PrismaClient({ datasourceUrl: target.toString() });
  let created = false;
  try {
    await admin.$executeRawUnsafe('CREATE DATABASE "' + name + '"');
    created = true;
    const migrate = spawnSync(process.execPath, [require.resolve('prisma/build/index.js'), 'migrate', 'deploy'], {
      env: { ...process.env, DATABASE_URL: target.toString(), DIRECT_URL: target.toString() }, encoding: 'utf8', timeout: 60000,
    });
    assert.equal(migrate.status, 0, migrate.stdout + migrate.stderr);
    const run = () => db.$transaction(tx => seedDatabase(tx), { timeout: 120000 });
    await run();
    assert.equal(await db.class.count({ where: { classId: { startsWith: 'demo-class-' } } }), 10);
    const students = await db.student.count({ where: { studentId: { startsWith: 'demo-student-' } } });
    assert.ok(students > 0);
    assert.equal(await db.student.count({ where: { studentId: { startsWith: 'auto-' } } }), 0);

    await db.student.create({ data: { studentId: 'auto-stu-old', studentCode: 'TEST-LEGACY', fullName: 'Old demo' } });
    await db.student.create({ data: { studentId: 'custom-student', studentCode: 'TEST-CUSTOM', fullName: 'User-created student' } });
    await db.user.update({ where: { username: 'admin' }, data: { passwordHash: 'preserve-current-password' } });
    // Reproduce JDBC-owned FKs present in existing databases, including the reported proposed-rank FK.
    await db.$executeRawUnsafe('ALTER TABLE salary_review_requests ADD CONSTRAINT test_review_proposed_rank_fk FOREIGN KEY (proposed_salary_rank_id) REFERENCES salary_ranks(salary_rank_id) ON DELETE RESTRICT');
    await db.$executeRawUnsafe('ALTER TABLE salary_review_requests ADD CONSTRAINT test_review_current_rank_fk FOREIGN KEY (current_salary_rank_id) REFERENCES salary_ranks(salary_rank_id) ON DELETE RESTRICT');
    await db.$executeRawUnsafe('ALTER TABLE salary_rank_history ADD CONSTRAINT test_history_rank_fk FOREIGN KEY (salary_rank_id) REFERENCES salary_ranks(salary_rank_id) ON DELETE RESTRICT');
    await db.$executeRawUnsafe('ALTER TABLE salary_rank_history ADD CONSTRAINT test_history_previous_rank_fk FOREIGN KEY (previous_salary_rank_id) REFERENCES salary_ranks(salary_rank_id) ON DELETE RESTRICT');
    const rankIds = ['auto-rank-18', 'auto-rank-current', 'auto-rank-history', 'auto-rank-previous', 'auto-rank-employee', 'auto-rank-unused'];
    for (const id of rankIds) await db.salaryRank.create({ data: { salaryRankId: id, roleId: 'TEACHER', rank: id, amount: 100 } });
    await db.salaryReviewRequest.create({ data: {
      requestId: 'custom-review', employeeId: 'teacher-001', currentSalaryRankId: 'auto-rank-current',
      proposedSalaryRankId: 'auto-rank-18', proposedAmount: 100, reason: 'Preserve user review',
    } });
    await db.salaryRankHistory.create({ data: {
      historyId: 'custom-history', employeeId: 'teacher-001', previousSalaryRankId: 'auto-rank-previous',
      salaryRankId: 'auto-rank-history', amount: 100, effectiveAt: new Date('2026-11-01T00:00:00Z'),
    } });
    await db.user.create({ data: { username: 'custom-worker', email: 'custom-worker@example.invalid', passwordHash: 'test', roleId: 'TEACHER' } });
    await db.employee.create({ data: { employeeId: 'custom-worker', username: 'custom-worker', employeeCode: 'CUSTOM-WORKER', fullName: 'Custom worker', salaryRankId: 'auto-rank-employee' } });
    const reviewBefore = await db.salaryReviewRequest.findUnique({ where: { requestId: 'custom-review' } });
    const historyBefore = await db.salaryRankHistory.findUnique({ where: { historyId: 'custom-history' } });
    await assert.rejects(db.$transaction(async tx => {
      await seedDatabase(tx);
      throw new Error('injected failure after cleanup');
    }, { timeout: 120000 }), /injected failure/);
    assert.ok(await db.student.findUnique({ where: { studentId: 'auto-stu-old' } }));
    await run();
    assert.equal(await db.student.findUnique({ where: { studentId: 'auto-stu-old' } }), null);
    assert.equal(await db.student.count({ where: { studentId: { startsWith: 'demo-student-' } } }), students);
    assert.equal((await db.student.findUnique({ where: { studentId: 'custom-student' } })).fullName, 'User-created student');
    assert.equal((await db.user.findUnique({ where: { username: 'admin' } })).passwordHash, 'preserve-current-password');
    assert.equal(await db.class.count({ where: { classId: { startsWith: 'auto-' } } }), 0);
    assert.equal(await db.salaryRank.findUnique({ where: { salaryRankId: 'auto-rank-unused' } }), null);
    for (const id of rankIds.slice(0, -1)) assert.ok(await db.salaryRank.findUnique({ where: { salaryRankId: id } }), id);
    assert.deepEqual(await db.salaryReviewRequest.findUnique({ where: { requestId: 'custom-review' } }), reviewBefore);
    assert.deepEqual(await db.salaryRankHistory.findUnique({ where: { historyId: 'custom-history' } }), historyBefore);
    assert.equal((await db.employee.findUnique({ where: { employeeId: 'custom-worker' } })).salaryRankId, 'auto-rank-employee');
  } finally {
    await db.$disconnect();
    if (created) await admin.$executeRawUnsafe('DROP DATABASE "' + name + '"');
    await admin.$disconnect();
  }
});
