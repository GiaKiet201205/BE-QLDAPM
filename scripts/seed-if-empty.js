// Only seed an initially empty local database. Existing records must never be
// reset when someone restarts containers with docker compose up.
const { PrismaClient } = require("@prisma/client");
const { spawnSync } = require("node:child_process");

async function main() {
  const prisma = new PrismaClient();
  let isEmpty;

  try {
    const counts = await Promise.all([
      prisma.role.count(),
      prisma.user.count(),
      prisma.course.count(),
      prisma.class.count(),
      prisma.student.count(),
    ]);
    isEmpty = counts.every((count) => count === 0);

    if (!isEmpty) {
      console.log(
        "Existing database detected; skipping demo seed to preserve data. Migrations have been applied."
      );
      return;
    }
  } finally {
    await prisma.$disconnect();
  }

  console.log("Fresh database detected; loading demo seed...");
  const result = spawnSync(process.execPath, ["prisma/seed.js"], {
    stdio: "inherit",
    env: process.env,
  });

  if (result.error) throw result.error;
  if (result.status !== 0) {
    throw new Error(`Demo seed failed (exit code ${result.status ?? "unknown"})`);
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
