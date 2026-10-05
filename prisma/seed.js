const { PrismaClient } = require("@prisma/client");
const bcrypt = require("bcryptjs");

const prisma = new PrismaClient();

async function main() {
  const password = await bcrypt.hash("123456", 10);

  const users = {};
  for (const user of [
    ["admin", "admin@iig.local", "System Admin", "ADMIN"],
    ["tc001", "tc001@iig.local", "Teaching Coordinator", "TC"],
    ["cm001", "cm001@iig.local", "Center Manager", "CM"],
    ["teacher001", "teacher001@iig.local", "David Miller", "TEACHER"],
    ["teacher002", "teacher002@iig.local", "Helena Costa", "TEACHER"],
    ["teacher003", "teacher003@iig.local", "Robert Taylor", "TEACHER"],
    ["cs001", "cs001@iig.local", "Current CS", "CS"],
    ["cs002", "cs002@iig.local", "Nguyen Minh Chau", "CS"],
    ["cs003", "cs003@iig.local", "Tran Quoc Huy", "CS"],
  ]) {
    users[user[0]] = await prisma.user.upsert({
      where: { username: user[0] },
      update: { email: user[1], fullName: user[2], role: user[3], status: "ACTIVE" },
      create: {
        username: user[0],
        email: user[1],
        password,
        fullName: user[2],
        role: user[3],
        status: "ACTIVE",
      },
    });
  }

  const courseData = [
    ["IELTS", "IELTS"],
    ["TOEIC", "TOEIC"],
    ["SAT", "SAT"],
    ["TOEFL", "TOEFL iBT"],
  ];
  const courses = {};
  for (const [code, name] of courseData) {
    courses[code] = await prisma.course.upsert({
      where: { code },
      update: { name },
      create: { code, name },
    });
  }

  const classData = [
    ["IELTS-M75-04", "IELTS Mastery 7.5", "IELTS", "2026-10-06", "2027-01-30", "RUNNING", users.admin.id],
    ["TOEIC-850-02", "TOEIC Intensive 850+", "TOEIC", "2026-10-12", "2027-01-15", "READY", users.admin.id],
    ["TOEFL-IBT-01", "TOEFL iBT Complete", "TOEFL", "2026-09-15", "2026-12-20", "RUNNING", users.cs001.id],
    ["SAT-ADV-03", "SAT Math Advanced", "SAT", "2026-11-01", "2027-02-28", "DRAFT", users.admin.id],
    ["IELTS-F65-08", "IELTS Foundation 6.5", "IELTS", "2026-08-01", "2026-11-30", "COMPLETED", users.cs001.id],
    ["IELTS-WR-02", "IELTS Writing Intensive", "IELTS", "2026-10-20", "2026-12-22", "READY", users.admin.id],
  ];

  const classes = {};
  for (const [classCode, name, courseCode, startDate, endDate, status, createdById] of classData) {
    classes[classCode] = await prisma.class.upsert({
      where: { classCode },
      update: {
        name,
        courseId: courses[courseCode].id,
        startDate: new Date(startDate),
        endDate: new Date(endDate),
        status,
        createdById,
      },
      create: {
        classCode,
        name,
        courseId: courses[courseCode].id,
        startDate: new Date(startDate),
        endDate: new Date(endDate),
        status,
        createdById,
      },
    });
  }

  const students = [];
  const baseStudents = [
    ["STU-2024-0891", "Minh Anh Nguyen", "minhanh.nguyen@email.com", "+84 912 345 678", "Active"],
    ["STU-2024-0892", "Duc Thang Tran", "thang.tran@email.com", "+84 913 276 428", "Active"],
    ["STU-2024-0895", "Phuong Linh Vo", "linh.vo@email.com", "+84 983 112 456", "Active"],
    ["STU-2024-0870", "Hoang Nam Le", "nam.le@email.com", "+84 912 466 113", "On Leave"],
    ["STU-2024-0864", "Mai Huong Dang", "huong.dang@email.com", "+84 934 778 202", "Active"],
    ["STU-2024-0752", "Quoc Bao Pham", "bao.pham@email.com", "+84 906 325 445", "Graduated"],
    ["STU-2024-0899", "Thu Ha Nguyen", "ha.nguyen@email.com", "+84 928 349 118", "Active"],
  ];

  for (const [studentCode, fullName, email, phone, status] of baseStudents) {
    students.push(await prisma.student.upsert({
      where: { studentCode },
      update: { fullName, email, phone, status },
      create: { studentCode, fullName, email, phone, status },
    }));
  }

  for (let i = 0; i < Math.min(students.length, 5); i++) {
    await prisma.classStudent.upsert({
      where: {
        classId_studentId: {
          classId: classes["IELTS-M75-04"].id,
          studentId: students[i].id,
        },
      },
      update: { status: "ACTIVE" },
      create: {
        classId: classes["IELTS-M75-04"].id,
        studentId: students[i].id,
        status: "ACTIVE",
      },
    });
  }

  await prisma.classAccessScope.upsert({
    where: {
      userId_classId_role: {
        userId: users.cs001.id,
        classId: classes["IELTS-M75-04"].id,
        role: "CS",
      },
    },
    update: { status: "ACTIVE", source: "ASSIGNED_SCOPE" },
    create: {
      userId: users.cs001.id,
      classId: classes["IELTS-M75-04"].id,
      role: "CS",
      status: "ACTIVE",
      source: "ASSIGNED_SCOPE",
    },
  });

  console.log("Seed completed.");
  console.log("Demo login: admin / 123456");
}

main()
  .catch((error) => {
    console.error(error);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
