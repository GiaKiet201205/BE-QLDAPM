const time = (value) => new Date(`1970-01-01T${value}:00Z`);
const date = (value) => new Date(`${value}T00:00:00Z`);

/* =========================================================
   CLEANUP LEGACY AUTO DATA
========================================================= */

async function cleanupLegacyAutoSeed(prisma) {
  console.log("Removing legacy auto-* demo rows...");

  if (prisma.teacherSessionAttendance?.deleteMany) {
    await prisma.teacherSessionAttendance.deleteMany({
      where: {
        teachingScheduleId: {
          startsWith: "auto-ts-",
        },
      },
    });
  }

  if (prisma.studentResult?.deleteMany) {
    await prisma.studentResult.deleteMany({
      where: {
        studentResultId: {
          startsWith: "auto-sr-",
        },
      },
    });
  }

  if (prisma.assignment?.deleteMany) {
    await prisma.assignment.deleteMany({
      where: {
        assignmentId: {
          startsWith: "auto-assign-",
        },
      },
    });
  }

  if (prisma.exam?.deleteMany) {
    await prisma.exam.deleteMany({
      where: {
        examId: {
          startsWith: "auto-exam-",
        },
      },
    });
  }

  if (prisma.salaryCalculationItem?.deleteMany) {
    await prisma.salaryCalculationItem.deleteMany({
      where: {
        OR: [
          {
            payrollId: {
              startsWith: "auto-es-",
            },
          },
          {
            payrollId: {
              startsWith: "auto-staffsal-",
            },
          },
        ],
      },
    });
  }

  if (prisma.employeeSalaryDetail?.deleteMany) {
    await prisma.employeeSalaryDetail.deleteMany({
      where: {
        employeeSalaryId: {
          startsWith: "auto-es-",
        },
      },
    });
  }

  if (prisma.employeeSalary?.deleteMany) {
    await prisma.employeeSalary.deleteMany({
      where: {
        employeeSalaryId: {
          startsWith: "auto-es-",
        },
      },
    });
  }

  if (prisma.staffSalary?.deleteMany) {
    await prisma.staffSalary.deleteMany({
      where: {
        staffSalaryId: {
          startsWith: "auto-staffsal-",
        },
      },
    });
  }

  if (prisma.salaryReviewRequest?.deleteMany) {
    await prisma.salaryReviewRequest.deleteMany({
      where: {
        employeeId: {
          startsWith: "auto-emp-",
        },
      },
    });
  }

  if (prisma.salaryRankHistory?.deleteMany) {
    await prisma.salaryRankHistory.deleteMany({
      where: {
        employeeId: {
          startsWith: "auto-emp-",
        },
      },
    });
  }

  if (prisma.classSession?.deleteMany) {
    await prisma.classSession.deleteMany({
      where: {
        OR: [
          {
            classSessionId: {
              startsWith: "auto-session-",
            },
          },
          {
            classId: {
              startsWith: "auto-class-",
            },
          },
        ],
      },
    });
  }

  if (prisma.teachingSchedule?.deleteMany) {
    await prisma.teachingSchedule.deleteMany({
      where: {
        teachingScheduleId: {
          startsWith: "auto-ts-",
        },
      },
    });
  }

  if (prisma.staffSchedule?.deleteMany) {
    await prisma.staffSchedule.deleteMany({
      where: {
        staffScheduleId: {
          startsWith: "auto-",
        },
      },
    });
  }

  if (prisma.availability?.deleteMany) {
    await prisma.availability.deleteMany({
      where: {
        availabilityId: {
          startsWith: "auto-avail-",
        },
      },
    });
  }

  if (prisma.classAccessScope?.deleteMany) {
    await prisma.classAccessScope.deleteMany({
      where: {
        classAccessScopeId: {
          startsWith: "auto-cas-",
        },
      },
    });
  }

  if (prisma.classStudent?.deleteMany) {
    await prisma.classStudent.deleteMany({
      where: {
        classStudentId: {
          startsWith: "auto-cs-",
        },
      },
    });
  }

  if (prisma.studentTarget?.deleteMany) {
    await prisma.studentTarget.deleteMany({
      where: {
        studentTargetId: {
          startsWith: "auto-st-",
        },
      },
    });
  }

  if (prisma.classTargetRequirement?.deleteMany) {
    await prisma.classTargetRequirement.deleteMany({
      where: {
        classTargetRequirementId: {
          startsWith: "auto-ctr-",
        },
      },
    });
  }

  if (prisma.lesson?.deleteMany) {
    await prisma.lesson.deleteMany({
      where: {
        lessonId: {
          startsWith: "auto-lesson-",
        },
      },
    });
  }

  if (prisma.certificate?.deleteMany) {
    await prisma.certificate.deleteMany({
      where: {
        certificateId: {
          startsWith: "auto-cert-",
        },
      },
    });
  }

  if (prisma.auditLog?.deleteMany) {
    await prisma.auditLog.deleteMany({
      where: {
        OR: [
          {
            auditLogId: {
              startsWith: "auto-log-",
            },
          },
          {
            entityId: {
              startsWith: "auto-",
            },
          },
        ],
      },
    });
  }

  if (prisma.class?.deleteMany) {
    await prisma.class.deleteMany({
      where: {
        classId: {
          startsWith: "auto-class-",
        },
      },
    });
  }

  if (prisma.student?.deleteMany) {
    await prisma.student.deleteMany({
      where: {
        studentId: {
          startsWith: "auto-stu-",
        },
      },
    });
  }

  if (prisma.course?.deleteMany) {
    await prisma.course.deleteMany({
      where: {
        courseId: {
          startsWith: "auto-course-",
        },
      },
    });
  }

  if (prisma.employee?.deleteMany) {
    await prisma.employee.deleteMany({
      where: {
        employeeId: {
          startsWith: "auto-emp-",
        },
      },
    });
  }

  if (prisma.user?.deleteMany) {
    await prisma.user.deleteMany({
      where: {
        username: {
          startsWith: "auto-user-",
        },
      },
    });
  }

  // Existing employee/review/history records may still depend on an old demo rank.
  // These JDBC-owned references are not all represented as Prisma relations.
  // Keep the rank, rather than deleting business records or detaching salary history.
  await prisma.$executeRaw`
    DELETE FROM salary_ranks AS r
    WHERE starts_with(r.salary_rank_id, 'auto-rank-')
      AND NOT EXISTS (SELECT 1 FROM employees e WHERE e.salary_rank_id = r.salary_rank_id)
      AND NOT EXISTS (
        SELECT 1 FROM salary_review_requests q
        WHERE q.current_salary_rank_id = r.salary_rank_id OR q.proposed_salary_rank_id = r.salary_rank_id
      )
      AND NOT EXISTS (
        SELECT 1 FROM salary_rank_history h
        WHERE h.previous_salary_rank_id = r.salary_rank_id OR h.salary_rank_id = r.salary_rank_id
      )
  `;

  if (prisma.permission?.deleteMany) {
    await prisma.permission.deleteMany({
      where: {
        functionId: {
          startsWith: "auto-func-",
        },
      },
    });
  }

  if (prisma.appFunction?.deleteMany) {
    await prisma.appFunction.deleteMany({
      where: {
        functionId: {
          startsWith: "auto-func-",
        },
      },
    });
  }
}

/* =========================================================
   ROLE
========================================================= */

async function seedRoleDetails(prisma) {
  const roles = [
    [
      "ADMIN",
      "Admin",
      "Quản trị toàn bộ hệ thống GK Academy.",
    ],
    [
      "TC",
      "Teaching Coordinator",
      "Điều phối giáo viên, lịch dạy và công giáo viên.",
    ],
    [
      "CM",
      "Center Management",
      "Quản lý nhân sự vận hành CS, Sale và hoạt động trung tâm.",
    ],
    [
      "TEACHER",
      "Teacher",
      "Giáo viên giảng dạy và quản lý lớp được phân công.",
    ],
    [
      "CS",
      "Customer Service",
      "Chăm sóc học viên và hỗ trợ vận hành lớp học.",
    ],
    [
      "SALE",
      "Sale",
      "Tư vấn khóa học và hỗ trợ học viên đăng ký.",
    ],
    [
      "USER",
      "Default User",
      "Tài khoản học viên sử dụng website GK Academy.",
    ],
  ];

  for (const [roleId, roleName, description] of roles) {
    await prisma.role.upsert({
      where: {
        roleId,
      },
      update: {
        roleName,
        description,
      },
      create: {
        roleId,
        roleName,
        description,
      },
    });
  }
}

/* =========================================================
   FUNCTIONS + PERMISSIONS
========================================================= */

async function seedFunctionsAndPermissions(prisma) {
  const functions = [
    ["FUNC_STUDENTS", "Quản lý học viên"],
    ["FUNC_CLASSES", "Quản lý lớp học"],
    ["FUNC_AVAILABILITY", "Đăng ký lịch rảnh"],
    ["FUNC_TEACHING_ASSIGN", "Phân công giáo viên"],
    ["FUNC_STAFF_ASSIGN", "Phân công CS và Sale"],
    ["FUNC_ATTENDANCE", "Điểm danh"],
    ["FUNC_PAYROLL", "Quản lý công lương"],
    ["FUNC_STATISTICS", "Thống kê"],
    ["FUNC_ACCOUNTS", "Quản lý tài khoản"],
  ];

  for (const [functionId, functionName] of functions) {
    await prisma.appFunction.upsert({
      where: {
        functionId,
      },
      update: {
        functionName,
      },
      create: {
        functionId,
        functionName,
      },
    });
  }

  const permissionMap = {
    ADMIN: [
      ["FUNC_STUDENTS", "MANAGE"],
      ["FUNC_CLASSES", "MANAGE"],
      ["FUNC_PAYROLL", "MANAGE"],
      ["FUNC_STATISTICS", "READ"],
      ["FUNC_ACCOUNTS", "MANAGE"],
    ],

    TC: [
      ["FUNC_AVAILABILITY", "REVIEW"],
      ["FUNC_TEACHING_ASSIGN", "MANAGE"],
      ["FUNC_PAYROLL", "MANAGE_TEACHER"],
    ],

    CM: [
      ["FUNC_AVAILABILITY", "REVIEW"],
      ["FUNC_STAFF_ASSIGN", "MANAGE"],
      ["FUNC_PAYROLL", "MANAGE_STAFF"],
    ],

    TEACHER: [
      ["FUNC_AVAILABILITY", "WRITE"],
      ["FUNC_CLASSES", "READ_ASSIGNED"],
      ["FUNC_ATTENDANCE", "READ"],
    ],

    CS: [
      ["FUNC_AVAILABILITY", "WRITE"],
      ["FUNC_CLASSES", "MANAGE_ASSIGNED"],
      ["FUNC_ATTENDANCE", "WRITE"],
    ],

    SALE: [
      ["FUNC_AVAILABILITY", "WRITE"],
      ["FUNC_STUDENTS", "CREATE_PROFILE"],
    ],
  };

  for (const [roleId, permissions] of Object.entries(permissionMap)) {
    for (const [functionId, action] of permissions) {
      await prisma.permission.upsert({
        where: {
          roleId_functionId_action: {
            roleId,
            functionId,
            action,
          },
        },

        update: {},

        create: {
          roleId,
          functionId,
          action,
        },
      });
    }
  }
}

/* =========================================================
   SALARY RANK
========================================================= */

async function seedSalaryRanks(prisma) {
  const ranks = [
    [
      "rank-teacher-junior",
      "TEACHER",
      "Giáo viên Junior",
      180000,
    ],
    [
      "rank-teacher-standard",
      "TEACHER",
      "Giáo viên Standard",
      230000,
    ],
    [
      "rank-teacher-senior",
      "TEACHER",
      "Giáo viên Senior",
      300000,
    ],

    [
      "rank-cs-standard",
      "CS",
      "CS Standard",
      55000,
    ],
    [
      "rank-cs-senior",
      "CS",
      "CS Senior",
      70000,
    ],

    [
      "rank-sale-standard",
      "SALE",
      "Sale Standard",
      55000,
    ],
    [
      "rank-sale-senior",
      "SALE",
      "Sale Senior",
      70000,
    ],

    [
      "rank-tc",
      "TC",
      "Teaching Coordinator",
      90000,
    ],

    [
      "rank-cm",
      "CM",
      "Center Manager",
      95000,
    ],
  ];

  for (
    const [
      salaryRankId,
      roleId,
      rank,
      amount,
    ] of ranks
  ) {
    await prisma.salaryRank.upsert({
      where: {
        salaryRankId,
      },

      update: {
        roleId,
        rank,
        amount,
        status: "ACTIVE",
      },

      create: {
        salaryRankId,
        roleId,
        rank,
        amount,
        status: "ACTIVE",
      },
    });
  }
}

/* =========================================================
   ADMIN
========================================================= */

async function seedAdmin(prisma, passwordHash) {
  await prisma.user.upsert({
    where: {
      username: "admin",
    },

    update: {
      email: "admin@gk.local",
      passwordLoginEnabled: true,
      displayName: "Quản trị viên GK",
      phone: "0908000001",
      contactEmail: "admin@gk.local",
      roleId: "ADMIN",
      status: "ACTIVE",
    },

    create: {
      username: "admin",
      email: "admin@gk.local",
      passwordHash,
      passwordLoginEnabled: true,
      displayName: "Quản trị viên GK",
      phone: "0908000001",
      contactEmail: "admin@gk.local",
      roleId: "ADMIN",
      status: "ACTIVE",
    },
  });
}

/* =========================================================
   EMPLOYEE + USER
========================================================= */

async function seedEmployees(prisma, passwordHash) {
  const people = [
    /* =========================
       TC / CM
    ========================= */

    {
      username: "tc001",
      email: "tc001@gk.local",
      roleId: "TC",
      employeeId: "emp-tc-001",
      employeeCode: "TC-001",
      fullName: "Trần Minh Quân",
      phone: "0908000101",
      specialization: "Điều phối giảng dạy",
      salaryRankId: "rank-tc",
      createdAt: "2023-01-10",
    },

    {
      username: "cm001",
      email: "cm001@gk.local",
      roleId: "CM",
      employeeId: "emp-cm-001",
      employeeCode: "CM-001",
      fullName: "Nguyễn Ngọc Hân",
      phone: "0908000102",
      specialization: "Quản lý vận hành trung tâm",
      salaryRankId: "rank-cm",
      createdAt: "2022-08-15",
    },

    /* =========================
       TEACHER BASE
    ========================= */

    {
      username: "teacher001",
      email: "teacher001@gk.local",
      roleId: "TEACHER",
      employeeId: "teacher-001",
      employeeCode: "GV-001",
      fullName: "Lê Hoàng Nam",
      phone: "0908111101",
      specialization: "IELTS Academic",
      salaryRankId: "rank-teacher-senior",
      createdAt: "2022-03-12",
    },

    {
      username: "teacher002",
      email: "teacher002@gk.local",
      roleId: "TEACHER",
      employeeId: "teacher-002",
      employeeCode: "GV-002",
      fullName: "Phạm Thu Trang",
      phone: "0908111102",
      specialization: "TOEIC Listening & Reading",
      salaryRankId: "rank-teacher-standard",
      createdAt: "2023-04-20",
    },

    {
      username: "teacher003",
      email: "teacher003@gk.local",
      roleId: "TEACHER",
      employeeId: "teacher-003",
      employeeCode: "GV-003",
      fullName: "Nguyễn Đức Khải",
      phone: "0908111103",
      specialization: "SAT Math & English",
      salaryRankId: "rank-teacher-senior",
      createdAt: "2022-11-05",
    },

    /* =========================
       CS BASE
    ========================= */

    {
      username: "cs001",
      email: "cs001@gk.local",
      roleId: "CS",
      employeeId: "cs-001",
      employeeCode: "CS-001",
      fullName: "Võ Minh Châu",
      phone: "0908222201",
      specialization: "Chăm sóc học viên",
      salaryRankId: "rank-cs-senior",
      createdAt: "2023-02-10",
    },

    {
      username: "cs002",
      email: "cs002@gk.local",
      roleId: "CS",
      employeeId: "cs-002",
      employeeCode: "CS-002",
      fullName: "Đỗ Thanh Huyền",
      phone: "0908222202",
      specialization: "Vận hành lớp học",
      salaryRankId: "rank-cs-standard",
      createdAt: "2024-01-17",
    },

    {
      username: "cs003",
      email: "cs003@gk.local",
      roleId: "CS",
      employeeId: "cs-003",
      employeeCode: "CS-003",
      fullName: "Trần Quốc Huy",
      phone: "0908222203",
      specialization: "Chăm sóc học viên",
      salaryRankId: "rank-cs-standard",
      createdAt: "2024-05-08",
    },

    /* =========================
       SALE BASE
    ========================= */

    {
      username: "sale001",
      email: "sale001@gk.local",
      roleId: "SALE",
      employeeId: "sale-001",
      employeeCode: "SALE-001",
      fullName: "Nguyễn Thu Hà",
      phone: "0908333301",
      specialization: "Tư vấn IELTS",
      salaryRankId: "rank-sale-senior",
      createdAt: "2023-06-14",
    },

    {
      username: "sale002",
      email: "sale002@gk.local",
      roleId: "SALE",
      employeeId: "sale-002",
      employeeCode: "SALE-002",
      fullName: "Lê Minh Khoa",
      phone: "0908333302",
      specialization: "Tư vấn TOEIC",
      salaryRankId: "rank-sale-standard",
      createdAt: "2024-02-01",
    },

    {
      username: "sale003",
      email: "sale003@gk.local",
      roleId: "SALE",
      employeeId: "sale-003",
      employeeCode: "SALE-003",
      fullName: "Phạm Gia Hân",
      phone: "0908333303",
      specialization: "Tư vấn SAT và TOEFL",
      salaryRankId: "rank-sale-standard",
      createdAt: "2024-04-11",
    },

    /* =========================
       TEACHER DEMO
    ========================= */

    {
      username: "teacher004",
      email: "teacher004@gk-demo.local",
      roleId: "TEACHER",
      employeeId: "demo-teacher-004",
      employeeCode: "GV-004",
      fullName: "Nguyễn Hoàng Anh",
      phone: "0908123404",
      specialization: "IELTS Speaking & Writing",
      salaryRankId: "rank-teacher-standard",
      createdAt: "2024-03-18",
    },

    {
      username: "teacher005",
      email: "teacher005@gk-demo.local",
      roleId: "TEACHER",
      employeeId: "demo-teacher-005",
      employeeCode: "GV-005",
      fullName: "Trần Thảo Vy",
      phone: "0908123405",
      specialization: "TOEIC Listening & Reading",
      salaryRankId: "rank-teacher-standard",
      createdAt: "2023-08-10",
    },

    {
      username: "teacher006",
      email: "teacher006@gk-demo.local",
      roleId: "TEACHER",
      employeeId: "demo-teacher-006",
      employeeCode: "GV-006",
      fullName: "Lê Đức Minh",
      phone: "0908123406",
      specialization: "IELTS Academic",
      salaryRankId: "rank-teacher-senior",
      createdAt: "2022-11-02",
    },

    {
      username: "teacher007",
      email: "teacher007@gk-demo.local",
      roleId: "TEACHER",
      employeeId: "demo-teacher-007",
      employeeCode: "GV-007",
      fullName: "Phạm Khánh Linh",
      phone: "0908123407",
      specialization: "SAT Math",
      salaryRankId: "rank-teacher-standard",
      createdAt: "2024-06-25",
    },

    {
      username: "teacher008",
      email: "teacher008@gk-demo.local",
      roleId: "TEACHER",
      employeeId: "demo-teacher-008",
      employeeCode: "GV-008",
      fullName: "Võ Gia Huy",
      phone: "0908123408",
      specialization: "TOEIC Communication",
      salaryRankId: "rank-teacher-junior",
      createdAt: "2025-04-15",
    },

    {
      username: "teacher009",
      email: "teacher009@gk-demo.local",
      roleId: "TEACHER",
      employeeId: "demo-teacher-009",
      employeeCode: "GV-009",
      fullName: "Bùi Ngọc Mai",
      phone: "0908123409",
      specialization: "IELTS Foundation",
      salaryRankId: "rank-teacher-standard",
      createdAt: "2023-12-01",
    },

    /* =========================
       CS DEMO
    ========================= */

    {
      username: "cs004",
      email: "cs004@gk-demo.local",
      roleId: "CS",
      employeeId: "demo-cs-004",
      employeeCode: "CS-004",
      fullName: "Nguyễn Mỹ Duyên",
      phone: "0908234504",
      specialization: "Chăm sóc lớp học",
      salaryRankId: "rank-cs-standard",
      createdAt: "2024-02-12",
    },

    {
      username: "cs005",
      email: "cs005@gk-demo.local",
      roleId: "CS",
      employeeId: "demo-cs-005",
      employeeCode: "CS-005",
      fullName: "Trương Quốc Bảo",
      phone: "0908234505",
      specialization: "Vận hành lớp",
      salaryRankId: "rank-cs-senior",
      createdAt: "2023-05-08",
    },

    {
      username: "cs006",
      email: "cs006@gk-demo.local",
      roleId: "CS",
      employeeId: "demo-cs-006",
      employeeCode: "CS-006",
      fullName: "Lâm Thanh Tâm",
      phone: "0908234506",
      specialization: "Chăm sóc học viên",
      salaryRankId: "rank-cs-standard",
      createdAt: "2024-09-20",
    },

    /* =========================
       SALE DEMO
    ========================= */

    {
      username: "sale004",
      email: "sale004@gk-demo.local",
      roleId: "SALE",
      employeeId: "demo-sale-004",
      employeeCode: "SALE-004",
      fullName: "Đặng Minh Thư",
      phone: "0908345604",
      specialization: "Tư vấn IELTS",
      salaryRankId: "rank-sale-standard",
      createdAt: "2024-01-05",
    },

    {
      username: "sale005",
      email: "sale005@gk-demo.local",
      roleId: "SALE",
      employeeId: "demo-sale-005",
      employeeCode: "SALE-005",
      fullName: "Phan Tuấn Kiệt",
      phone: "0908345605",
      specialization: "Tư vấn TOEIC",
      salaryRankId: "rank-sale-senior",
      createdAt: "2023-03-14",
    },

    {
      username: "sale006",
      email: "sale006@gk-demo.local",
      roleId: "SALE",
      employeeId: "demo-sale-006",
      employeeCode: "SALE-006",
      fullName: "Nguyễn Hồng Nhung",
      phone: "0908345606",
      specialization: "Tư vấn SAT & TOEFL",
      salaryRankId: "rank-sale-standard",
      createdAt: "2024-07-01",
    },
  ];

  for (const person of people) {
    await prisma.user.upsert({
      where: {
        username: person.username,
      },

      update: {
        email: person.email,
        passwordLoginEnabled: true,
        displayName: person.fullName,
        phone: person.phone,
        contactEmail: person.email,
        roleId: person.roleId,
        status: "ACTIVE",
      },

      create: {
        username: person.username,
        email: person.email,
        passwordHash,
        passwordLoginEnabled: true,
        displayName: person.fullName,
        phone: person.phone,
        contactEmail: person.email,
        roleId: person.roleId,
        status: "ACTIVE",
        createdAt: date(person.createdAt),
      },
    });

    await prisma.employee.upsert({
      where: {
        employeeId: person.employeeId,
      },

      update: {
        username: person.username,
        employeeCode: person.employeeCode,
        fullName: person.fullName,
        phone: person.phone,
        specialization: person.specialization,
        salaryRankId: person.salaryRankId,
        reviewedDate: date("2026-11-15"),
        status: "ACTIVE",
      },

      create: {
        employeeId: person.employeeId,
        username: person.username,
        employeeCode: person.employeeCode,
        fullName: person.fullName,
        phone: person.phone,
        specialization: person.specialization,
        salaryRankId: person.salaryRankId,
        reviewedDate: date("2026-11-15"),
        status: "ACTIVE",
        createdAt: date(person.createdAt),
      },
    });
  }
}

/* =========================================================
   CERTIFICATES
========================================================= */

async function seedCertificates(prisma) {
  const certificates = [
    [
      "cert-teacher-001",
      "teacher-001",
      "IELTS",
      8.5,
      8.0,
      "British Council",
      "/certificates/teacher-001-ielts.pdf",
    ],

    [
      "cert-teacher-002",
      "teacher-002",
      "TOEIC",
      970,
      360,
      "IIG Vietnam",
      "/certificates/teacher-002-toeic.pdf",
    ],

    [
      "cert-teacher-003",
      "teacher-003",
      "SAT",
      1540,
      760,
      "College Board",
      "/certificates/teacher-003-sat.pdf",
    ],

    [
      "demo-cert-004",
      "demo-teacher-004",
      "IELTS",
      8.0,
      8.0,
      "British Council",
      "/certificates/demo-cert-004.pdf",
    ],

    [
      "demo-cert-005",
      "demo-teacher-005",
      "TOEIC",
      965,
      350,
      "IIG Vietnam",
      "/certificates/demo-cert-005.pdf",
    ],

    [
      "demo-cert-006",
      "demo-teacher-006",
      "IELTS",
      8.5,
      8.0,
      "IDP Education",
      "/certificates/demo-cert-006.pdf",
    ],

    [
      "demo-cert-007",
      "demo-teacher-007",
      "SAT",
      1510,
      720,
      "College Board",
      "/certificates/demo-cert-007.pdf",
    ],

    [
      "demo-cert-008",
      "demo-teacher-008",
      "TOEIC",
      925,
      340,
      "IIG Vietnam",
      "/certificates/demo-cert-008.pdf",
    ],

    [
      "demo-cert-009",
      "demo-teacher-009",
      "IELTS",
      7.5,
      7.5,
      "British Council",
      "/certificates/demo-cert-009.pdf",
    ],
  ];

  for (
    const [
      certificateId,
      employeeId,
      certificateType,
      score,
      scoreSW,
      issuer,
      certificateUrl,
    ] of certificates
  ) {
    await prisma.certificate.upsert({
      where: {
        certificateId,
      },

      update: {
        employeeId,
        certificateType,
        score,
        scoreSW,
        issueDate: date("2025-01-15"),
        expiryDate: date("2027-01-15"),
        issuer,
        certificateUrl,
        status: "ACTIVE",
      },

      create: {
        certificateId,
        employeeId,
        certificateType,
        score,
        scoreSW,
        issueDate: date("2025-01-15"),
        expiryDate: date("2027-01-15"),
        issuer,
        certificateUrl,
        status: "ACTIVE",
      },
    });
  }
}

/* =========================================================
   COURSE
========================================================= */

async function seedCourses(prisma) {
  const courses = [
    {
      courseId: "course-ielts",
      name: "IELTS",
      category: "IELTS",
      price: 12500000,
      imageUrl: "/images/ielts.jpg",
      target: "IELTS 6.5 - 7.5+",
      entryDescription:
        "Phù hợp học viên có nền tảng tiếng Anh từ IELTS 4.5 trở lên.",
      description:
        "Khóa luyện thi IELTS toàn diện Listening, Reading, Writing và Speaking.",
      level: "Intermediate - Advanced",
      duration: 48,
      lessonCount: 4,
      outcomes: [
        "Nắm chắc cấu trúc bài thi IELTS.",
        "Cải thiện đồng đều bốn kỹ năng.",
        "Xây dựng chiến lược làm bài hiệu quả.",
        "Sẵn sàng cho bài thi IELTS thực tế.",
      ],
      syllabus: [
        "IELTS Overview & Diagnostic Test",
        "Listening & Reading Strategies",
        "Writing Task 1 & Task 2",
        "Speaking Practice & Mock Test",
      ],
      featured: true,
    },

    {
      courseId: "course-toeic",
      name: "TOEIC",
      category: "TOEIC",
      price: 7900000,
      imageUrl: "/images/toeic.jpg",
      target: "TOEIC 750 - 900+",
      entryDescription:
        "Phù hợp học viên có nền tảng TOEIC từ 450 điểm trở lên.",
      description:
        "Khóa TOEIC Listening, Reading, Speaking và Writing theo định dạng IIG.",
      level: "Foundation - Advanced",
      duration: 36,
      lessonCount: 4,
      outcomes: [
        "Nắm cấu trúc các Part của TOEIC.",
        "Cải thiện tốc độ nghe và đọc.",
        "Củng cố ngữ pháp và từ vựng.",
        "Tăng khả năng đạt mục tiêu TOEIC.",
      ],
      syllabus: [
        "TOEIC Part 1 - 2",
        "TOEIC Part 3 - 4",
        "TOEIC Part 5 - 6",
        "TOEIC Part 7 & Mock Test",
      ],
      featured: true,
    },

    {
      courseId: "course-sat",
      name: "SAT",
      category: "SAT",
      price: 14900000,
      imageUrl: "/images/sat.jpg",
      target: "SAT 1300 - 1500+",
      entryDescription:
        "Phù hợp học sinh có nền tảng Toán và tiếng Anh khá.",
      description:
        "Khóa luyện SAT Math, Reading và Writing theo cấu trúc Digital SAT.",
      level: "Intermediate - Advanced",
      duration: 48,
      lessonCount: 4,
      outcomes: [
        "Nắm chắc kiến thức SAT Math.",
        "Cải thiện Reading & Writing.",
        "Làm quen Digital SAT.",
        "Tăng điểm qua các bài Mock Test.",
      ],
      syllabus: [
        "SAT Algebra Foundations",
        "SAT Advanced Math",
        "SAT Reading & Writing",
        "SAT Full Practice Review",
      ],
      featured: true,
    },

    {
      courseId: "course-toefl",
      name: "TOEFL iBT",
      category: "TOEFL",
      price: 13900000,
      imageUrl: "/images/toefl.jpg",
      target: "TOEFL iBT 80 - 100+",
      entryDescription:
        "Phù hợp học viên có trình độ tiếng Anh từ B1 trở lên.",
      description:
        "Khóa TOEFL iBT Academic toàn diện Reading, Listening, Speaking và Writing.",
      level: "Intermediate - Advanced",
      duration: 48,
      lessonCount: 4,
      outcomes: [
        "Nắm định dạng TOEFL iBT.",
        "Phát triển kỹ năng tiếng Anh học thuật.",
        "Cải thiện Speaking và Writing.",
        "Sẵn sàng cho bài thi TOEFL thực tế.",
      ],
      syllabus: [
        "TOEFL Academic Reading",
        "TOEFL Integrated Listening",
        "TOEFL Speaking Tasks",
        "TOEFL Academic Writing",
      ],
      featured: true,
    },
  ];

  for (const course of courses) {
    await prisma.course.upsert({
      where: {
        courseId: course.courseId,
      },

      update: {
        name: course.name,
        category: course.category,
        price: course.price,
        imageUrl: course.imageUrl,
        target: course.target,
        entryDescription: course.entryDescription,
        description: course.description,
        level: course.level,
        duration: course.duration,
        lessonCount: course.lessonCount,
        outcomes: course.outcomes,
        syllabus: course.syllabus,
        published: true,
        featured: course.featured,
        status: "ACTIVE",
      },

      create: {
        courseId: course.courseId,
        name: course.name,
        category: course.category,
        price: course.price,
        imageUrl: course.imageUrl,
        target: course.target,
        entryDescription: course.entryDescription,
        description: course.description,
        level: course.level,
        duration: course.duration,
        lessonCount: course.lessonCount,
        outcomes: course.outcomes,
        syllabus: course.syllabus,
        published: true,
        featured: course.featured,
        status: "ACTIVE",
      },
    });
  }
}

/* =========================================================
   CLASS
========================================================= */

async function seedClasses(prisma) {
  const classes = [
    /* =========================
       BASE CLASSES
    ========================= */

    {
      classId: "class-0001",
      courseId: "course-ielts",
      classCode: "IELTS-M75-04",
      name: "IELTS Mastery 7.5",
      startDate: "2026-10-06",
      endDate: "2027-01-30",
      status: "RUNNING",
      createdByUsername: "admin",
      location: "GK Academy - Cơ sở chính",
      scheduleLabel: "Thứ 2 - 4 - 6 | 18:30 - 20:30",
      capacity: 30,
    },

    {
      classId: "class-0002",
      courseId: "course-toeic",
      classCode: "TOEIC-850-02",
      name: "TOEIC Intensive 850+",
      startDate: "2026-10-12",
      endDate: "2027-01-15",
      status: "READY",
      createdByUsername: "admin",
      location: "GK Academy - Cơ sở Quận 1",
      scheduleLabel: "Thứ 3 - 5 | 18:30 - 20:30",
      capacity: 28,
    },

    {
      classId: "class-0003",
      courseId: "course-toefl",
      classCode: "TOEFL-IBT-01",
      name: "TOEFL iBT Complete",
      startDate: "2026-09-15",
      endDate: "2026-12-20",
      status: "RUNNING",
      createdByUsername: "cs001",
      location: "GK Academy - Cơ sở Quận 3",
      scheduleLabel: "Thứ 3 - 5 | 18:30 - 20:30",
      capacity: 25,
    },

    {
      classId: "class-0004",
      courseId: "course-sat",
      classCode: "SAT-ADV-03",
      name: "SAT Math Advanced",
      startDate: "2026-11-01",
      endDate: "2027-02-28",
      status: "DRAFT",
      createdByUsername: "admin",
      location: "GK Academy - Cơ sở chính",
      scheduleLabel: "Thứ 7 - Chủ nhật | 09:00 - 11:00",
      capacity: 25,
    },

    {
      classId: "class-0005",
      courseId: "course-ielts",
      classCode: "IELTS-F65-08",
      name: "IELTS Foundation 6.5",
      startDate: "2026-08-01",
      endDate: "2026-11-30",
      status: "COMPLETED",
      createdByUsername: "cs001",
      location: "GK Academy - Cơ sở chính",
      scheduleLabel: "Thứ 2 - 4 - 6 | 18:30 - 20:30",
      capacity: 30,
    },

    {
      classId: "class-0006",
      courseId: "course-ielts",
      classCode: "IELTS-WR-02",
      name: "IELTS Writing Intensive",
      startDate: "2026-10-20",
      endDate: "2026-12-22",
      status: "READY",
      createdByUsername: "admin",
      location: "GK Academy - Cơ sở Quận 1",
      scheduleLabel: "Thứ 3 - 5 | 18:30 - 20:30",
      capacity: 24,
    },

    {
      classId: "class-0007",
      courseId: "course-toeic",
      classCode: "TOEIC-650-01",
      name: "TOEIC Foundation 650",
      startDate: "2026-07-01",
      endDate: "2026-09-30",
      status: "CLOSED",
      createdByUsername: "admin",
      location: "GK Academy - Cơ sở Quận 3",
      scheduleLabel: "Thứ 2 - 4 - 6 | 18:30 - 20:30",
      capacity: 30,
    },

    {
      classId: "class-0008",
      courseId: "course-sat",
      classCode: "SAT-FOUND-01",
      name: "SAT Foundation",
      startDate: "2026-10-25",
      endDate: "2027-02-10",
      status: "DRAFT",
      createdByUsername: "cs001",
      location: "GK Academy - Cơ sở Quận 1",
      scheduleLabel: "Thứ 7 - Chủ nhật | 13:30 - 15:30",
      capacity: 25,
    },

    /* =========================
       DEMO CLASSES
    ========================= */

    {
      classId: "demo-class-001",
      courseId: "course-ielts",
      classCode: "IELTS-65-EVE-01",
      name: "IELTS 6.5 Evening",
      startDate: "2026-11-02",
      endDate: "2027-02-12",
      status: "RUNNING",
      createdByUsername: "admin",
      location: "GK Academy - Cơ sở chính",
      scheduleLabel: "Thứ 2 - 4 | 18:30 - 20:30",
      capacity: 30,
    },

    {
      classId: "demo-class-002",
      courseId: "course-toeic",
      classCode: "TOEIC-750-EVE-02",
      name: "TOEIC 750+ Evening",
      startDate: "2026-11-09",
      endDate: "2027-01-29",
      status: "RUNNING",
      createdByUsername: "admin",
      location: "GK Academy - Cơ sở Quận 1",
      scheduleLabel: "Thứ 3 - 5 | 18:30 - 20:30",
      capacity: 28,
    },

    {
      classId: "demo-class-003",
      courseId: "course-ielts",
      classCode: "IELTS-70-WKD-01",
      name: "IELTS 7.0 Weekend",
      startDate: "2026-11-14",
      endDate: "2027-03-06",
      status: "RUNNING",
      createdByUsername: "admin",
      location: "GK Academy - Cơ sở chính",
      scheduleLabel: "Thứ 4 - Thứ 7 | 09:00 - 11:00",
      capacity: 26,
    },

    {
      classId: "demo-class-004",
      courseId: "course-sat",
      classCode: "SAT-1300-WKD-01",
      name: "SAT 1300+ Weekend",
      startDate: "2026-11-21",
      endDate: "2027-03-20",
      status: "READY",
      createdByUsername: "admin",
      location: "GK Academy - Cơ sở chính",
      scheduleLabel: "Thứ 7 | 09:00 - 11:00",
      capacity: 24,
    },

    {
      classId: "demo-class-005",
      courseId: "course-toefl",
      classCode: "TOEFL-45-EVE-01",
      name: "TOEFL iBT 4.5+ Evening",
      startDate: "2026-11-23",
      endDate: "2027-02-26",
      status: "READY",
      createdByUsername: "admin",
      location: "GK Academy - Cơ sở Quận 3",
      scheduleLabel: "Thứ 2 - 6 | 18:30 - 20:30",
      capacity: 25,
    },

    {
      classId: "demo-class-006",
      courseId: "course-toeic",
      classCode: "TOEIC-900-INT-01",
      name: "TOEIC Intensive 900+",
      startDate: "2026-11-30",
      endDate: "2027-02-05",
      status: "READY",
      createdByUsername: "admin",
      location: "GK Academy - Cơ sở Quận 1",
      scheduleLabel: "Thứ 2 - 5 | 18:30 - 20:30",
      capacity: 24,
    },

    {
      classId: "demo-class-007",
      courseId: "course-ielts",
      classCode: "IELTS-55-FOUND-02",
      name: "IELTS Foundation 5.5",
      startDate: "2026-12-01",
      endDate: "2027-03-30",
      status: "DRAFT",
      createdByUsername: "admin",
      location: "GK Academy - Cơ sở chính",
      scheduleLabel: "Thứ 3 - 5 | 18:30 - 20:30",
      capacity: 30,
    },

    {
      classId: "demo-class-008",
      courseId: "course-sat",
      classCode: "SAT-MATH-ADV-02",
      name: "SAT Math Advanced",
      startDate: "2026-12-05",
      endDate: "2027-03-27",
      status: "DRAFT",
      createdByUsername: "admin",
      location: "GK Academy - Cơ sở Quận 1",
      scheduleLabel: "Thứ 7 | 13:30 - 15:30",
      capacity: 24,
    },

    {
      classId: "demo-class-009",
      courseId: "course-toeic",
      classCode: "TOEIC-650-MOR-01",
      name: "TOEIC Foundation 650",
      startDate: "2026-10-05",
      endDate: "2026-12-23",
      status: "RUNNING",
      createdByUsername: "admin",
      location: "GK Academy - Cơ sở Quận 3",
      scheduleLabel: "Thứ 3 - 6 | 18:30 - 20:30",
      capacity: 30,
    },

    {
      classId: "demo-class-010",
      courseId: "course-ielts",
      classCode: "IELTS-WRITE-01",
      name: "IELTS Writing Focus",
      startDate: "2026-10-12",
      endDate: "2026-12-28",
      status: "RUNNING",
      createdByUsername: "admin",
      location: "GK Academy - Cơ sở chính",
      scheduleLabel: "Thứ 5 | 18:30 - 20:30",
      capacity: 22,
    },
  ];

  for (const item of classes) {
    const storefrontOpen = [
      "READY",
      "RUNNING",
    ].includes(item.status);

    await prisma.class.upsert({
      where: {
        classId: item.classId,
      },

      update: {
        courseId: item.courseId,
        classCode: item.classCode,
        name: item.name,
        startDate: date(item.startDate),
        endDate: date(item.endDate),
        status: item.status,
        createdByUsername: item.createdByUsername,
        storefrontOpen,
        capacity: item.capacity,
        deliveryMode: "Tại trung tâm",
        location: item.location,
        scheduleLabel: item.scheduleLabel,
      },

      create: {
        classId: item.classId,
        courseId: item.courseId,
        classCode: item.classCode,
        name: item.name,
        startDate: date(item.startDate),
        endDate: date(item.endDate),
        status: item.status,
        createdByUsername: item.createdByUsername,
        storefrontOpen,
        capacity: item.capacity,
        deliveryMode: "Tại trung tâm",
        location: item.location,
        scheduleLabel: item.scheduleLabel,
      },
    });
  }

  const targets = [
    ["demo-ctr-001", "demo-class-001", "OVERALL_BAND", 6.5],

    ["demo-ctr-002a", "demo-class-002", "LR_TOTAL", 750],
    ["demo-ctr-002b", "demo-class-002", "SW_TOTAL", 240],

    ["demo-ctr-003", "demo-class-003", "OVERALL_BAND", 7.0],

    ["demo-ctr-004", "demo-class-004", "TOTAL", 1300],

    ["demo-ctr-005", "demo-class-005", "OVERALL_1_6", 4.5],

    ["demo-ctr-006a", "demo-class-006", "LR_TOTAL", 900],
    ["demo-ctr-006b", "demo-class-006", "SW_TOTAL", 300],

    ["demo-ctr-007", "demo-class-007", "OVERALL_BAND", 5.5],

    ["demo-ctr-008", "demo-class-008", "TOTAL", 1450],

    ["demo-ctr-009a", "demo-class-009", "LR_TOTAL", 650],
    ["demo-ctr-009b", "demo-class-009", "SW_TOTAL", 180],

    ["demo-ctr-010", "demo-class-010", "OVERALL_BAND", 6.5],
  ];

  for (
    const [
      classTargetRequirementId,
      classId,
      targetType,
      requiredTarget,
    ] of targets
  ) {
    await prisma.classTargetRequirement.upsert({
      where: {
        classTargetRequirementId,
      },

      update: {
        classId,
        targetType,
        requiredTarget,
      },

      create: {
        classTargetRequirementId,
        classId,
        targetType,
        requiredTarget,
      },
    });
  }
}

/* =========================================================
   LESSON
========================================================= */

async function seedCourseLessons(prisma) {
  const lessons = [
    [
      "demo-ielts-01",
      "course-ielts",
      "demo-class-001",
      "IELTS Overview & Diagnostic Test",
      1,
    ],
    [
      "demo-ielts-02",
      "course-ielts",
      "demo-class-001",
      "Listening: Maps and Multiple Choice",
      2,
    ],
    [
      "demo-ielts-03",
      "course-ielts",
      "demo-class-001",
      "Reading: Matching Headings",
      3,
    ],
    [
      "demo-ielts-04",
      "course-ielts",
      "demo-class-001",
      "Writing Task 2: Opinion Essay",
      4,
    ],

    [
      "demo-toeic-01",
      "course-toeic",
      "demo-class-002",
      "TOEIC Part 1–2 Fundamentals",
      1,
    ],
    [
      "demo-toeic-02",
      "course-toeic",
      "demo-class-002",
      "TOEIC Part 3–4 Conversations",
      2,
    ],
    [
      "demo-toeic-03",
      "course-toeic",
      "demo-class-002",
      "TOEIC Part 5 Grammar Review",
      3,
    ],
    [
      "demo-toeic-04",
      "course-toeic",
      "demo-class-002",
      "TOEIC Part 7 Reading Strategies",
      4,
    ],

    [
      "demo-sat-01",
      "course-sat",
      "demo-class-004",
      "SAT Algebra Foundations",
      1,
    ],
    [
      "demo-sat-02",
      "course-sat",
      "demo-class-004",
      "SAT Advanced Math",
      2,
    ],
    [
      "demo-sat-03",
      "course-sat",
      "demo-class-004",
      "SAT Reading & Writing",
      3,
    ],
    [
      "demo-sat-04",
      "course-sat",
      "demo-class-004",
      "SAT Full Practice Review",
      4,
    ],

    [
      "demo-toefl-01",
      "course-toefl",
      "demo-class-005",
      "TOEFL Academic Reading",
      1,
    ],
    [
      "demo-toefl-02",
      "course-toefl",
      "demo-class-005",
      "TOEFL Integrated Listening",
      2,
    ],
    [
      "demo-toefl-03",
      "course-toefl",
      "demo-class-005",
      "TOEFL Speaking Tasks",
      3,
    ],
    [
      "demo-toefl-04",
      "course-toefl",
      "demo-class-005",
      "TOEFL Academic Writing",
      4,
    ],
  ];

  for (
    const [
      lessonId,
      courseId,
      classId,
      title,
      orderIndex,
    ] of lessons
  ) {
    const description =
      `Bài ${orderIndex}: ${title}. Nội dung học mẫu phục vụ kiểm thử nghiệp vụ lớp học.`;

    const contentUrl =
      `/materials/${lessonId}.pdf`;

    await prisma.lesson.upsert({
      where: {
        lessonId,
      },

      update: {
        courseId,
        classId,
        title,
        description,
        contentType: "LESSON",
        contentUrl,
        orderIndex,
        status: "ACTIVE",
      },

      create: {
        lessonId,
        courseId,
        classId,
        title,
        description,
        contentType: "LESSON",
        contentUrl,
        orderIndex,
        status: "ACTIVE",
      },
    });
  }
}

/* =========================================================
   STUDENTS
========================================================= */

async function seedStudents(prisma, passwordHash) {
  const baseStudents = [
    {
      studentId: "student-0001",
      username: "student101",
      studentCode: "STU-2024-0891",
      fullName: "Nguyễn Minh Anh",
      phone: "0912345678",
      email: "minhanh.nguyen@email.com",
      level: "Upper-Intermediate",
      rlTarget: 850,
      swTarget: 320,
      status: "Active",
    },

    {
      studentId: "student-0002",
      username: "student102",
      studentCode: "STU-2024-0892",
      fullName: "Trần Đức Thắng",
      phone: "0913276428",
      email: "thang.tran@email.com",
      level: "Intermediate",
      rlTarget: 850,
      swTarget: 300,
      status: "Active",
    },

    {
      studentId: "student-0003",
      username: "student103",
      studentCode: "STU-2024-0895",
      fullName: "Võ Phương Linh",
      phone: "0983112456",
      email: "linh.vo@email.com",
      level: "Intermediate",
      rlTarget: 750,
      swTarget: 250,
      status: "Active",
    },

    {
      studentId: "student-0004",
      username: "student104",
      studentCode: "STU-2024-0870",
      fullName: "Lê Hoàng Nam",
      phone: "0912466113",
      email: "nam.le@email.com",
      level: "Foundation",
      rlTarget: 650,
      swTarget: 200,
      status: "On Leave",
    },

    {
      studentId: "student-0005",
      username: "student105",
      studentCode: "STU-2024-0864",
      fullName: "Đặng Mai Hương",
      phone: "0934778202",
      email: "huong.dang@email.com",
      level: "Upper-Intermediate",
      rlTarget: 850,
      swTarget: 300,
      status: "Active",
    },

    {
      studentId: "student-0006",
      username: "student106",
      studentCode: "STU-2024-0752",
      fullName: "Phạm Quốc Bảo",
      phone: "0906325445",
      email: "bao.pham@email.com",
      level: "Advanced",
      rlTarget: 900,
      swTarget: 320,
      status: "Graduated",
    },

    {
      studentId: "student-0007",
      username: "student107",
      studentCode: "STU-2024-0899",
      fullName: "Nguyễn Thu Hà",
      phone: "0928349118",
      email: "ha.nguyen@email.com",
      level: "Upper-Intermediate",
      rlTarget: 900,
      swTarget: 330,
      status: "Active",
    },
  ];

  const demoNames = [
    "Nguyễn Minh Anh",
    "Trần Gia Bảo",
    "Lê Khánh Vy",
    "Phạm Quốc Huy",
    "Võ Ngọc Hà",
    "Đặng Tuấn Kiệt",
    "Bùi Thanh Trúc",
    "Hoàng Đức Anh",
    "Nguyễn Phương Linh",
    "Trương Minh Khang",
    "Lâm Bảo Ngọc",
    "Phan Hoàng Nam",
    "Đỗ Mỹ Duyên",
    "Huỳnh Quang Huy",
    "Ngô Anh Thư",
    "Dương Quốc Việt",
    "Mai Thảo Nhi",
    "Tạ Minh Quân",
    "Đinh Gia Hân",
    "Lý Tuấn Anh",
    "Cao Ngọc Mai",
    "Vũ Đức Long",
    "Hồ Khánh An",
    "Trịnh Thanh Hà",
  ];

  const students = [...baseStudents];

  for (let index = 0; index < demoNames.length; index += 1) {
    const number = index + 1;

    const studentId =
      `demo-student-${String(number).padStart(3, "0")}`;

    const username =
      `student${String(number).padStart(3, "0")}`;

    const studentCode =
      `HV-2026-${String(100 + number).padStart(4, "0")}`;

    const phone =
      `0912${String(340000 + number).slice(-6)}`;

    const email =
      `hocvien${String(number).padStart(2, "0")}@student.demo.local`;

    let status = "Active";

    if (number === 7 || number === 16) {
      status = "On Leave";
    }

    if (number === 23) {
      status = "Graduated";
    }

    const level =
      number % 3 === 0
        ? "Intermediate"
        : number % 3 === 1
          ? "Foundation"
          : "Upper-Intermediate";

    /*
      rlTarget / swTarget là field legacy.
      Với khóa không dùng TOEIC target, dùng 0 để biểu thị N/A
      thay vì NULL.
    */
    let rlTarget = 0;
    let swTarget = 0;

    if (number >= 7 && number <= 12) {
      rlTarget =
        [700, 750, 800, 850, 900, 950][number - 7];

      swTarget =
        [200, 240, 260, 280, 300, 320][number - 7];
    }

    students.push({
      studentId,
      username,
      studentCode,
      fullName: demoNames[index],
      phone,
      email,
      level,
      rlTarget,
      swTarget,
      status,
    });
  }

  for (const student of students) {
    await prisma.user.upsert({
      where: {
        username: student.username,
      },

      update: {
        email: student.email,
        passwordLoginEnabled: true,
        displayName: student.fullName,
        phone: student.phone,
        contactEmail: student.email,
        roleId: "USER",
        status: "ACTIVE",
      },

      create: {
        username: student.username,
        email: student.email,
        passwordHash,
        passwordLoginEnabled: true,
        displayName: student.fullName,
        phone: student.phone,
        contactEmail: student.email,
        roleId: "USER",
        status: "ACTIVE",
      },
    });

    await prisma.student.upsert({
      where: {
        studentId: student.studentId,
      },

      update: {
        username: student.username,
        studentCode: student.studentCode,
        fullName: student.fullName,
        phone: student.phone,
        email: student.email,
        level: student.level,
        rlTarget: student.rlTarget,
        swTarget: student.swTarget,
        status: student.status,
      },

      create: {
        studentId: student.studentId,
        username: student.username,
        studentCode: student.studentCode,
        fullName: student.fullName,
        phone: student.phone,
        email: student.email,
        level: student.level,
        rlTarget: student.rlTarget,
        swTarget: student.swTarget,
        status: student.status,
      },
    });
  }

  /* =========================
     BASE TARGETS
  ========================= */

  const baseTargets = [
    [
      "st-001-ielts",
      "student-0001",
      "course-ielts",
      "OVERALL_BAND",
      7.5,
    ],

    [
      "st-001-toeic-lr",
      "student-0001",
      "course-toeic",
      "LR_TOTAL",
      850,
    ],

    [
      "st-001-toeic-sw",
      "student-0001",
      "course-toeic",
      "SW_TOTAL",
      320,
    ],

    [
      "st-002-toeic-lr",
      "student-0002",
      "course-toeic",
      "LR_TOTAL",
      850,
    ],

    [
      "st-002-toeic-sw",
      "student-0002",
      "course-toeic",
      "SW_TOTAL",
      300,
    ],

    [
      "st-002-ielts",
      "student-0002",
      "course-ielts",
      "OVERALL_BAND",
      7.5,
    ],

    [
      "st-003-toefl",
      "student-0003",
      "course-toefl",
      "OVERALL_1_6",
      4.5,
    ],

    [
      "st-005-ielts",
      "student-0005",
      "course-ielts",
      "OVERALL_BAND",
      7.5,
    ],

    [
      "st-007-ielts",
      "student-0007",
      "course-ielts",
      "OVERALL_BAND",
      8.0,
    ],
  ];

  /* =========================
     DEMO TARGETS
  ========================= */

  const demoTargets = [];

  const targetGroups = [
    {
      range: [1, 6],
      courseId: "course-ielts",
      targetType: "OVERALL_BAND",
      values: [6.5, 7.0, 7.5, 6.5, 7.0, 7.5],
    },

    {
      range: [7, 12],
      courseId: "course-toeic",
      targetType: "LR_TOTAL",
      values: [700, 750, 800, 850, 900, 950],
    },

    {
      range: [7, 12],
      courseId: "course-toeic",
      targetType: "SW_TOTAL",
      values: [200, 240, 260, 280, 300, 320],
    },

    {
      range: [13, 18],
      courseId: "course-sat",
      targetType: "TOTAL",
      values: [1250, 1300, 1350, 1400, 1450, 1500],
    },

    {
      range: [19, 24],
      courseId: "course-toefl",
      targetType: "OVERALL_1_6",
      values: [4.0, 4.5, 5.0, 5.0, 5.5, 6.0],
    },
  ];

  for (const group of targetGroups) {
    const [start, end] = group.range;

    for (let number = start; number <= end; number += 1) {
      const studentId =
        `demo-student-${String(number).padStart(3, "0")}`;

      const studentTargetId =
        `demo-target-${number}-${group.targetType.toLowerCase()}`;

      const targetValue =
        group.values[number - start];

      demoTargets.push([
        studentTargetId,
        studentId,
        group.courseId,
        group.targetType,
        targetValue,
      ]);
    }
  }

  const allTargets = [
    ...baseTargets,
    ...demoTargets,
  ];

  for (
    const [
      studentTargetId,
      studentId,
      courseId,
      targetType,
      targetValue,
    ] of allTargets
  ) {
    await prisma.studentTarget.upsert({
      where: {
        studentTargetId,
      },

      update: {
        studentId,
        courseId,
        targetType,
        targetValue,
      },

      create: {
        studentTargetId,
        studentId,
        courseId,
        targetType,
        targetValue,
      },
    });
  }

  /* =========================
     CLASS MEMBERSHIP
  ========================= */

  const memberships = [
    ["class-0001", "student-0001"],
    ["class-0001", "student-0002"],
    ["class-0001", "student-0005"],

    ["demo-class-001", "demo-student-001"],
    ["demo-class-001", "demo-student-002"],
    ["demo-class-001", "demo-student-003"],
    ["demo-class-001", "demo-student-004"],

    ["demo-class-003", "demo-student-003"],
    ["demo-class-003", "demo-student-005"],
    ["demo-class-003", "demo-student-006"],

    ["demo-class-002", "demo-student-008"],
    ["demo-class-002", "demo-student-009"],
    ["demo-class-002", "demo-student-010"],

    ["demo-class-009", "demo-student-007"],
    ["demo-class-009", "demo-student-011"],
    ["demo-class-009", "demo-student-012"],

    ["demo-class-004", "demo-student-013"],
    ["demo-class-004", "demo-student-014"],
    ["demo-class-004", "demo-student-015"],

    ["demo-class-005", "demo-student-020"],
    ["demo-class-005", "demo-student-021"],
    ["demo-class-005", "demo-student-022"],

    ["demo-class-010", "demo-student-001"],
    ["demo-class-010", "demo-student-004"],
    ["demo-class-010", "demo-student-005"],
  ];

  let membershipIndex = 1;

  for (const [classId, studentId] of memberships) {
    const classStudentId =
      `demo-membership-${String(membershipIndex).padStart(3, "0")}`;

    membershipIndex += 1;

    /*
      inactiveReason và inactivatedAt phải NULL khi membership ACTIVE.
      Đây là NULL có ý nghĩa nghiệp vụ.
    */
    await prisma.classStudent.upsert({
      where: {
        classId_studentId: {
          classId,
          studentId,
        },
      },

      update: {
        status: "ACTIVE",
        inactiveReason: null,
        inactivatedAt: null,
      },

      create: {
        classStudentId,
        classId,
        studentId,
        status: "ACTIVE",
      },
    });
  }
}

/* =========================================================
   AVAILABILITY
========================================================= */

async function seedAvailabilities(prisma) {
  const rows = [
    /* =========================
       BASE TEACHERS
    ========================= */

    [
      "avail-teacher001-mon",
      "teacher-001",
      "mon",
      "18:00",
      "21:30",
      "evening",
      "Cơ sở chính",
      "TC",
    ],

    [
      "avail-teacher001-wed",
      "teacher-001",
      "wed",
      "18:00",
      "21:30",
      "evening",
      "Cơ sở chính",
      "TC",
    ],

    [
      "avail-teacher002-tue",
      "teacher-002",
      "tue",
      "18:00",
      "21:30",
      "evening",
      "Cơ sở Quận 1",
      "TC",
    ],

    [
      "avail-teacher002-thu",
      "teacher-002",
      "thu",
      "18:00",
      "21:30",
      "evening",
      "Cơ sở Quận 1",
      "TC",
    ],

    [
      "avail-teacher003-sat",
      "teacher-003",
      "sat",
      "08:00",
      "12:00",
      "morning",
      "Cơ sở chính",
      "TC",
    ],

    /* =========================
       DEMO TEACHERS
    ========================= */

    [
      "demo-avail-t004-mon",
      "demo-teacher-004",
      "mon",
      "18:00",
      "21:30",
      "evening",
      "Cơ sở chính",
      "TC",
    ],

    [
      "demo-avail-t004-wed",
      "demo-teacher-004",
      "wed",
      "18:00",
      "21:30",
      "evening",
      "Cơ sở chính",
      "TC",
    ],

    [
      "demo-avail-t004-sat",
      "demo-teacher-004",
      "sat",
      "08:00",
      "12:00",
      "morning",
      "Cơ sở chính",
      "TC",
    ],

    [
      "demo-avail-t005-tue",
      "demo-teacher-005",
      "tue",
      "18:00",
      "21:30",
      "evening",
      "Cơ sở Quận 1",
      "TC",
    ],

    [
      "demo-avail-t005-thu",
      "demo-teacher-005",
      "thu",
      "18:00",
      "21:30",
      "evening",
      "Cơ sở Quận 1",
      "TC",
    ],

    [
      "demo-avail-t005-sun",
      "demo-teacher-005",
      "sun",
      "08:00",
      "12:00",
      "morning",
      "Cơ sở Quận 1",
      "TC",
    ],

    [
      "demo-avail-t006-mon",
      "demo-teacher-006",
      "mon",
      "18:00",
      "21:30",
      "evening",
      "Cơ sở Quận 3",
      "TC",
    ],

    [
      "demo-avail-t006-fri",
      "demo-teacher-006",
      "fri",
      "18:00",
      "21:30",
      "evening",
      "Cơ sở Quận 3",
      "TC",
    ],

    [
      "demo-avail-t007-wed",
      "demo-teacher-007",
      "wed",
      "18:00",
      "21:30",
      "evening",
      "Cơ sở chính",
      "TC",
    ],

    [
      "demo-avail-t007-sat",
      "demo-teacher-007",
      "sat",
      "08:00",
      "12:30",
      "morning",
      "Cơ sở chính",
      "TC",
    ],

    [
      "demo-avail-t008-tue",
      "demo-teacher-008",
      "tue",
      "18:00",
      "21:30",
      "evening",
      "Cơ sở Quận 3",
      "TC",
    ],

    [
      "demo-avail-t008-fri",
      "demo-teacher-008",
      "fri",
      "18:00",
      "21:30",
      "evening",
      "Cơ sở Quận 3",
      "TC",
    ],

    [
      "demo-avail-t009-thu",
      "demo-teacher-009",
      "thu",
      "18:00",
      "21:30",
      "evening",
      "Cơ sở chính",
      "TC",
    ],

    [
      "demo-avail-t009-sun",
      "demo-teacher-009",
      "sun",
      "08:00",
      "12:30",
      "morning",
      "Cơ sở chính",
      "TC",
    ],

    /* =========================
       CS
    ========================= */

    [
      "demo-avail-cs004-mon",
      "demo-cs-004",
      "mon",
      "17:00",
      "21:30",
      "evening",
      "Cơ sở chính",
      "CM",
    ],

    [
      "demo-avail-cs004-wed",
      "demo-cs-004",
      "wed",
      "17:00",
      "21:30",
      "evening",
      "Cơ sở chính",
      "CM",
    ],

    [
      "demo-avail-cs005-tue",
      "demo-cs-005",
      "tue",
      "17:00",
      "21:30",
      "evening",
      "Cơ sở Quận 1",
      "CM",
    ],

    [
      "demo-avail-cs005-thu",
      "demo-cs-005",
      "thu",
      "17:00",
      "21:30",
      "evening",
      "Cơ sở Quận 1",
      "CM",
    ],

    [
      "demo-avail-cs006-fri",
      "demo-cs-006",
      "fri",
      "17:00",
      "21:30",
      "evening",
      "Cơ sở Quận 3",
      "CM",
    ],

    [
      "demo-avail-cs006-sat",
      "demo-cs-006",
      "sat",
      "08:00",
      "17:00",
      "day",
      "Cơ sở Quận 3",
      "CM",
    ],

    /* =========================
       SALE
    ========================= */

    [
      "demo-avail-sale004-mon",
      "demo-sale-004",
      "mon",
      "08:00",
      "17:30",
      "day",
      "Cơ sở chính",
      "CM",
    ],

    [
      "demo-avail-sale004-tue",
      "demo-sale-004",
      "tue",
      "08:00",
      "17:30",
      "day",
      "Cơ sở chính",
      "CM",
    ],

    [
      "demo-avail-sale005-wed",
      "demo-sale-005",
      "wed",
      "08:00",
      "17:30",
      "day",
      "Cơ sở Quận 1",
      "CM",
    ],

    [
      "demo-avail-sale005-thu",
      "demo-sale-005",
      "thu",
      "08:00",
      "17:30",
      "day",
      "Cơ sở Quận 1",
      "CM",
    ],

    [
      "demo-avail-sale006-fri",
      "demo-sale-006",
      "fri",
      "08:00",
      "17:30",
      "day",
      "Cơ sở Quận 3",
      "CM",
    ],

    [
      "demo-avail-sale006-sat",
      "demo-sale-006",
      "sat",
      "08:00",
      "17:00",
      "day",
      "Cơ sở Quận 3",
      "CM",
    ],
  ];

  for (
    const [
      availabilityId,
      employeeId,
      dayOfWeek,
      startTime,
      endTime,
      shift,
      campus,
      reviewerRole,
    ] of rows
  ) {
    const reviewedById =
      reviewerRole === "TC"
        ? "emp-tc-001"
        : "emp-cm-001";

    const data = {
      employeeId,

      /*
        RECURRING bắt buộc availableDate = null.
        AvailabilityService hiện tại reject nếu RECURRING có ngày cụ thể.
      */
      availableDate: null,

      dayOfWeek,
      startTime: time(startTime),
      endTime: time(endTime),
      shift,
      availabilityType: "RECURRING",
      campus,
      note:
        "Lịch rảnh định kỳ phục vụ dữ liệu demo nghiệp vụ.",
      status: "APPROVED",
      reviewedAt: date("2026-11-15"),
      reviewedById,
    };

    await prisma.availability.upsert({
      where: {
        availabilityId,
      },

      update: data,

      create: {
        availabilityId,
        ...data,
      },
    });
  }
}

/* =========================================================
   CLASS SESSION + TEACHING + STAFF
========================================================= */

async function seedClassSessionsAndSchedules(prisma) {
  const sessions = [
    [
      "session-base-001",
      "class-0001",
      "2026-10-06",
      "18:30",
      "20:30",
    ],

    [
      "demo-session-001",
      "demo-class-001",
      "2026-11-23",
      "18:30",
      "20:30",
    ],

    [
      "demo-session-002",
      "demo-class-002",
      "2026-11-24",
      "18:30",
      "20:30",
    ],

    [
      "demo-session-003",
      "demo-class-003",
      "2026-11-25",
      "18:30",
      "20:30",
    ],

    [
      "demo-session-004",
      "demo-class-005",
      "2026-11-27",
      "18:30",
      "20:30",
    ],

    [
      "demo-session-005",
      "demo-class-004",
      "2026-11-28",
      "09:00",
      "11:00",
    ],

    [
      "demo-session-006",
      "demo-class-009",
      "2026-11-27",
      "18:30",
      "20:30",
    ],

    [
      "demo-session-007",
      "demo-class-010",
      "2026-11-26",
      "18:30",
      "20:30",
    ],

    [
      "demo-session-008",
      "demo-class-001",
      "2026-11-25",
      "18:30",
      "20:30",
    ],

    [
      "demo-session-009",
      "demo-class-002",
      "2026-11-26",
      "18:30",
      "20:30",
    ],

    [
      "demo-session-010",
      "demo-class-003",
      "2026-11-28",
      "09:00",
      "11:00",
    ],

    [
      "demo-session-011",
      "demo-class-005",
      "2026-11-30",
      "18:30",
      "20:30",
    ],

    [
      "demo-session-012",
      "demo-class-004",
      "2026-12-02",
      "18:30",
      "20:30",
    ],
  ];

  for (
    const [
      classSessionId,
      classId,
      sessionDate,
      startTime,
      endTime,
    ] of sessions
  ) {
    await prisma.classSession.upsert({
      where: {
        classId_date_startTime_endTime: {
          classId,
          date: date(sessionDate),
          startTime: time(startTime),
          endTime: time(endTime),
        },
      },

      update: {
        status: "PLANNED",
      },

      create: {
        classSessionId,
        classId,
        date: date(sessionDate),
        startTime: time(startTime),
        endTime: time(endTime),
        status: "PLANNED",
      },
    });
  }

  /* =========================
     TEACHING
  ========================= */

  const teaching = [
    [
      "teaching-001",
      "teacher-001",
      "class-0001",
      "2026-10-06",
      "18:30",
      "20:30",
      "Cơ sở chính",
    ],

    [
      "demo-teaching-001",
      "demo-teacher-004",
      "demo-class-001",
      "2026-11-23",
      "18:30",
      "20:30",
      "Cơ sở chính",
    ],

    [
      "demo-teaching-002",
      "demo-teacher-005",
      "demo-class-002",
      "2026-11-24",
      "18:30",
      "20:30",
      "Cơ sở Quận 1",
    ],

    [
      "demo-teaching-003",
      "demo-teacher-007",
      "demo-class-003",
      "2026-11-25",
      "18:30",
      "20:30",
      "Cơ sở chính",
    ],

    [
      "demo-teaching-004",
      "demo-teacher-008",
      "demo-class-009",
      "2026-11-27",
      "18:30",
      "20:30",
      "Cơ sở Quận 3",
    ],

    [
      "demo-teaching-005",
      "demo-teacher-009",
      "demo-class-010",
      "2026-11-26",
      "18:30",
      "20:30",
      "Cơ sở chính",
    ],
  ];

  for (
    const [
      teachingScheduleId,
      teacherId,
      classId,
      scheduleDate,
      startTime,
      endTime,
      campus,
    ] of teaching
  ) {
    await prisma.teachingSchedule.upsert({
      where: {
        teachingScheduleId,
      },

      update: {
        teacherId,
        classId,
        date: date(scheduleDate),
        startTime: time(startTime),
        endTime: time(endTime),
        campus,
        status: "ASSIGNED",
        assignedById: "emp-tc-001",
      },

      create: {
        teachingScheduleId,
        teacherId,
        classId,
        date: date(scheduleDate),
        startTime: time(startTime),
        endTime: time(endTime),
        campus,
        status: "ASSIGNED",
        assignedById: "emp-tc-001",
      },
    });
  }

  /* =========================
     STAFF SCHEDULE
  ========================= */

  const staffSchedules = [
    [
      "demo-staff-001",
      "demo-cs-004",
      "demo-class-001",
      "center-01",
      "2026-11-23",
      "18:30",
      "20:30",
      "CLASS_SUPPORT",
    ],

    [
      "demo-staff-002",
      "demo-cs-005",
      "demo-class-002",
      "center-02",
      "2026-11-24",
      "18:30",
      "20:30",
      "CLASS_SUPPORT",
    ],

    [
      "demo-staff-003",
      "demo-cs-004",
      "demo-class-003",
      "center-01",
      "2026-11-25",
      "18:30",
      "20:30",
      "CLASS_SUPPORT",
    ],

    /*
      Ca Sale không thuộc một lớp cụ thể.
      classId = null là đúng nghiệp vụ.
    */
    [
      "demo-staff-004",
      "demo-sale-004",
      null,
      "center-01",
      "2026-11-23",
      "08:30",
      "12:00",
      "SALES_SHIFT",
    ],

    [
      "demo-staff-005",
      "demo-sale-005",
      null,
      "center-02",
      "2026-11-25",
      "13:00",
      "17:00",
      "LEAD_FOLLOWUP",
    ],

    [
      "demo-staff-006",
      "demo-sale-006",
      null,
      "center-03",
      "2026-11-27",
      "08:30",
      "12:00",
      "CONSULTATION",
    ],
  ];

  for (
    const [
      staffScheduleId,
      employeeId,
      classId,
      workplaceId,
      scheduleDate,
      startTime,
      endTime,
      workType,
    ] of staffSchedules
  ) {
    await prisma.staffSchedule.upsert({
      where: {
        staffScheduleId,
      },

      update: {
        employeeId,
        classId,
        workplaceId,
        date: date(scheduleDate),
        startTime: time(startTime),
        endTime: time(endTime),
        workType,
        status: "ASSIGNED",
        assignedById: "emp-cm-001",
        assignmentSource: "STANDARD",
      },

      create: {
        staffScheduleId,
        employeeId,
        classId,
        workplaceId,
        date: date(scheduleDate),
        startTime: time(startTime),
        endTime: time(endTime),
        workType,
        status: "ASSIGNED",
        assignedById: "emp-cm-001",
        assignmentSource: "STANDARD",
      },
    });
  }

  /* =========================
     CLASS ACCESS
  ========================= */

  const scopes = [
    [
      "class-scope-001",
      "cs-001",
      "class-0001",
    ],

    [
      "demo-scope-001",
      "demo-cs-004",
      "demo-class-001",
    ],

    [
      "demo-scope-002",
      "demo-cs-005",
      "demo-class-002",
    ],

    [
      "demo-scope-003",
      "demo-cs-004",
      "demo-class-003",
    ],

    [
      "demo-scope-004",
      "demo-cs-006",
      "demo-class-009",
    ],

    [
      "demo-scope-005",
      "demo-cs-006",
      "demo-class-010",
    ],
  ];

  for (
    const [
      classAccessScopeId,
      employeeId,
      classId,
    ] of scopes
  ) {
    await prisma.classAccessScope.upsert({
      where: {
        classAccessScopeId,
      },

      update: {
        employeeId,
        classId,
        status: "ACTIVE",
        source: "ASSIGNED_SCOPE",
      },

      create: {
        classAccessScopeId,
        employeeId,
        classId,
        status: "ACTIVE",
        source: "ASSIGNED_SCOPE",
      },
    });
  }
}

/* =========================================================
   ACADEMIC ACTIVITIES
========================================================= */

async function seedAcademicActivities(prisma) {
  const assignments = [
    [
      "demo-assignment-001",
      "demo-teacher-004",
      "demo-class-001",
      "Writing Task 2 - Education",
      "2026-11-29T23:59:00Z",
    ],

    [
      "demo-assignment-002",
      "demo-teacher-005",
      "demo-class-002",
      "TOEIC Part 5 Practice Set",
      "2026-11-30T23:59:00Z",
    ],

    [
      "demo-assignment-003",
      "demo-teacher-007",
      "demo-class-003",
      "Reading Practice - Matching Headings",
      "2026-12-02T23:59:00Z",
    ],

    [
      "demo-assignment-004",
      "demo-teacher-009",
      "demo-class-010",
      "Writing Task 1 - Line Graph",
      "2026-12-03T23:59:00Z",
    ],
  ];

  for (
    const [
      assignmentId,
      teacherId,
      classId,
      title,
      deadline,
    ] of assignments
  ) {
    const description =
      `Bài tập ${title} phục vụ đánh giá tiến độ học tập của lớp.`;

    await prisma.assignment.upsert({
      where: {
        assignmentId,
      },

      update: {
        teacherId,
        classId,
        title,
        description,
        deadline: new Date(deadline),
        status: "OPEN",
      },

      create: {
        assignmentId,
        teacherId,
        classId,
        title,
        description,
        deadline: new Date(deadline),
        status: "OPEN",
      },
    });
  }

  const exams = [
    [
      "demo-exam-001",
      "demo-teacher-004",
      "demo-class-001",
      "IELTS Progress Test 1",
      90,
      "2026-12-05T09:00:00Z",
    ],

    [
      "demo-exam-002",
      "demo-teacher-005",
      "demo-class-002",
      "TOEIC Mini Test 1",
      75,
      "2026-12-06T09:00:00Z",
    ],

    [
      "demo-exam-003",
      "demo-teacher-007",
      "demo-class-003",
      "IELTS Weekend Mock Test",
      120,
      "2026-12-12T08:00:00Z",
    ],
  ];

  for (
    const [
      examId,
      teacherId,
      classId,
      title,
      duration,
      examDate,
    ] of exams
  ) {
    const description =
      `${title} dùng để kiểm tra và đánh giá tiến độ học viên.`;

    await prisma.exam.upsert({
      where: {
        examId,
      },

      update: {
        teacherId,
        classId,
        title,
        description,
        duration,
        examDate: new Date(examDate),
        status: "SCHEDULED",
      },

      create: {
        examId,
        teacherId,
        classId,
        title,
        description,
        duration,
        examDate: new Date(examDate),
        status: "SCHEDULED",
      },
    });
  }
}

/* =========================================================
   PAYROLL
========================================================= */

async function seedPayroll(prisma) {
  const teacherPayroll = [
    [
      "demo-pay-teacher-001",
      "teacher-001",
      12,
      24,
      300000,
    ],

    [
      "demo-pay-teacher-002",
      "teacher-002",
      10,
      20,
      230000,
    ],

    [
      "demo-pay-teacher-003",
      "teacher-003",
      11,
      22,
      300000,
    ],

    [
      "demo-pay-teacher-004",
      "demo-teacher-004",
      8,
      16,
      230000,
    ],

    [
      "demo-pay-teacher-005",
      "demo-teacher-005",
      10,
      20,
      230000,
    ],

    [
      "demo-pay-teacher-006",
      "demo-teacher-006",
      12,
      24,
      300000,
    ],

    [
      "demo-pay-teacher-007",
      "demo-teacher-007",
      8,
      16,
      230000,
    ],

    [
      "demo-pay-teacher-008",
      "demo-teacher-008",
      6,
      12,
      180000,
    ],

    [
      "demo-pay-teacher-009",
      "demo-teacher-009",
      9,
      18,
      230000,
    ],
  ];

  for (
    const [
      employeeSalaryId,
      employeeId,
      totalSessions,
      totalHours,
      rate,
    ] of teacherPayroll
  ) {
    const amount =
      totalHours * rate;

    await prisma.employeeSalary.upsert({
      where: {
        employeeSalaryId,
      },

      update: {
        employeeId,
        period: "2026-10",
        totalSessions,
        totalHours,
        rate,
        amount,
        status: "APPROVED",
      },

      create: {
        employeeSalaryId,
        employeeId,
        period: "2026-10",
        totalSessions,
        totalHours,
        rate,
        amount,
        status: "APPROVED",
      },
    });

    await prisma.employeeSalaryDetail.upsert({
      where: {
        employeeSalaryId,
      },

      update: {
        totalSessions,
        salaryPerHours: rate,
        totalHours,
        totalSalary: amount,
      },

      create: {
        employeeSalaryId,
        totalSessions,
        salaryPerHours: rate,
        totalHours,
        totalSalary: amount,
      },
    });
  }

  const staffPayroll = [
    [
      "demo-pay-cs001",
      "cs-001",
      22,
      90,
      70000,
    ],

    [
      "demo-pay-cs002",
      "cs-002",
      20,
      82,
      55000,
    ],

    [
      "demo-pay-cs003",
      "cs-003",
      19,
      78,
      55000,
    ],

    [
      "demo-pay-cs004",
      "demo-cs-004",
      20,
      82,
      55000,
    ],

    [
      "demo-pay-cs005",
      "demo-cs-005",
      22,
      90,
      70000,
    ],

    [
      "demo-pay-cs006",
      "demo-cs-006",
      18,
      74,
      55000,
    ],

    [
      "demo-pay-sale001",
      "sale-001",
      22,
      90,
      70000,
    ],

    [
      "demo-pay-sale002",
      "sale-002",
      20,
      82,
      55000,
    ],

    [
      "demo-pay-sale003",
      "sale-003",
      19,
      78,
      55000,
    ],

    [
      "demo-pay-sale004",
      "demo-sale-004",
      21,
      86,
      55000,
    ],

    [
      "demo-pay-sale005",
      "demo-sale-005",
      22,
      90,
      70000,
    ],

    [
      "demo-pay-sale006",
      "demo-sale-006",
      19,
      78,
      55000,
    ],
  ];

  for (
    const [
      staffSalaryId,
      employeeId,
      totalShifts,
      totalHours,
      rate,
    ] of staffPayroll
  ) {
    const amount =
      totalHours * rate;

    await prisma.staffSalary.upsert({
      where: {
        staffSalaryId,
      },

      update: {
        employeeId,
        period: "2026-10",
        totalShifts,
        totalHours,
        rate,
        amount,
        status: "APPROVED",
        managedById: "emp-cm-001",
      },

      create: {
        staffSalaryId,
        employeeId,
        period: "2026-10",
        totalShifts,
        totalHours,
        rate,
        amount,
        status: "APPROVED",
        managedById: "emp-cm-001",
      },
    });
  }
}

/* =========================================================
   AUDIT LOG DEMO
========================================================= */

async function seedAuditLogs(prisma) {
  const logs = [
    {
      auditLogId: "demo-log-001",
      username: "admin",
      action: "SEED_DEMO_DATA",
      entityType: "SYSTEM",
      entityId: "demo-dataset",
      description:
        "Khởi tạo bộ dữ liệu demo đầy đủ cho GK Academy.",
      details: {
        source: "realistic-demo-seed",
        status: "SUCCESS",
      },
    },

    {
      auditLogId: "demo-log-002",
      username: "tc001",
      action: "ASSIGN_TEACHER",
      entityType: "TEACHING_SCHEDULE",
      entityId: "demo-teaching-001",
      description:
        "Phân công giáo viên Nguyễn Hoàng Anh vào lớp IELTS 6.5 Evening.",
      details: {
        classId: "demo-class-001",
        teacherId: "demo-teacher-004",
      },
    },

    {
      auditLogId: "demo-log-003",
      username: "cm001",
      action: "ASSIGN_STAFF",
      entityType: "STAFF_SCHEDULE",
      entityId: "demo-staff-001",
      description:
        "Phân công CS hỗ trợ lớp IELTS 6.5 Evening.",
      details: {
        classId: "demo-class-001",
        employeeId: "demo-cs-004",
      },
    },
  ];

  for (const log of logs) {
    await prisma.auditLog.upsert({
      where: {
        auditLogId: log.auditLogId,
      },

      update: {
        username: log.username,
        action: log.action,
        entityType: log.entityType,
        entityId: log.entityId,
        description: log.description,
        details: log.details,
      },

      create: {
        auditLogId: log.auditLogId,
        username: log.username,
        action: log.action,
        entityType: log.entityType,
        entityId: log.entityId,
        description: log.description,
        details: log.details,
      },
    });
  }
}

/* =========================================================
   MAIN REALISTIC DEMO SEED
========================================================= */

async function seedRealisticDemoData(
  prisma,
  passwordHash,
) {
  await cleanupLegacyAutoSeed(prisma);

  /*
    Reference data
  */
  await seedRoleDetails(prisma);
  await seedFunctionsAndPermissions(prisma);
  await seedSalaryRanks(prisma);

  /*
    Accounts
  */
  await seedAdmin(
    prisma,
    passwordHash,
  );

  await seedEmployees(
    prisma,
    passwordHash,
  );

  await seedCertificates(prisma);

  /*
    Academic structure

    Course phải có trước Class.
    Class phải có trước Lesson vì Lesson demo có classId.
  */
  await seedCourses(prisma);
  await seedClasses(prisma);
  await seedCourseLessons(prisma);

  /*
    Students
  */
  await seedStudents(
    prisma,
    passwordHash,
  );

  /*
    Scheduling
  */
  await seedAvailabilities(prisma);

  await seedClassSessionsAndSchedules(
    prisma,
  );

  /*
    Academic activities
  */
  await seedAcademicActivities(prisma);

  /*
    Payroll
  */
  await seedPayroll(prisma);

  /*
    Audit
  */
  await seedAuditLogs(prisma);

  console.log(
    "Realistic demo dataset loaded.",
  );

  console.log(
    "Staff demo accounts use password: 123456",
  );

  console.log(
    "Student demo accounts use password: 123456",
  );
}

/* =========================================================
   EXPORT
========================================================= */

module.exports = {
  cleanupLegacyAutoSeed,
  seedRealisticDemoData,
};