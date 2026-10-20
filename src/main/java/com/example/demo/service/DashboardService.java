package com.example.demo.service;

import com.example.demo.dto.DashboardDtos.Activity;
import com.example.demo.dto.DashboardDtos.DashboardResponse;
import com.example.demo.dto.DashboardDtos.PanelItem;
import com.example.demo.dto.DashboardDtos.Stat;
import com.example.demo.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final JdbcTemplate db;

    private record UserContext(String username, String role, String employeeId) {}

    public DashboardResponse getDashboard(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        UserContext user = db.query(
                "SELECT u.username, u.role_id, e.employee_id FROM users u " +
                        "LEFT JOIN employees e ON e.username=u.username AND upper(e.status)='ACTIVE' " +
                        "WHERE u.username=? AND upper(u.status)='ACTIVE'",
                rs -> rs.next()
                        ? new UserContext(rs.getString("username"), rs.getString("role_id"), rs.getString("employee_id"))
                        : null,
                authentication.getName());
        if (user == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Active user account required");
        }

        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
        DashboardResponse response = switch (user.role().toUpperCase(Locale.ROOT)) {
            case "ADMIN" -> admin(today, weekStart, user);
            case "TC" -> coordinator(today, weekStart, user);
            case "CM" -> centerManager(today, weekStart, user);
            case "TEACHER" -> teacher(today, weekStart, user);
            case "SALE" -> sale(today, weekStart, user);
            case "CS" -> customerSupport(today, weekStart, user);
            default -> throw new ApiException(HttpStatus.FORBIDDEN, "Dashboard is not available for this role");
        };
        return response;
    }

    private String requireEmployee(UserContext user) {
        if (user.employeeId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Active employee profile required");
        }
        return user.employeeId();
    }

    private DashboardResponse admin(LocalDate today, LocalDate weekStart, UserContext user) {
        List<Stat> stats = asList(
                stat("HỌC VIÊN ĐANG HOẠT ĐỘNG", count("SELECT count(*) FROM students WHERE upper(status)='ACTIVE'"),
                        "Tổng học viên có trạng thái hoạt động", true),
                stat("LỚP ĐANG HOẠT ĐỘNG", count("SELECT count(*) FROM classes WHERE upper(status) IN ('READY','RUNNING')"),
                        "Lớp sẵn sàng hoặc đang học", true),
                stat("GIÁO VIÊN HOẠT ĐỘNG", count("SELECT count(*) FROM employees e JOIN users u ON u.username=e.username " +
                        "WHERE upper(e.status)='ACTIVE' AND upper(u.status)='ACTIVE' AND upper(u.role_id)='TEACHER'"),
                        "Tài khoản giáo viên đang hoạt động", true),
                stat("BUỔI HỌC HÔM NAY", count("SELECT count(*) FROM teaching_schedules WHERE date=? AND upper(status)='ASSIGNED'", today),
                        "Lịch đã phân công hôm nay", true));
        List<List<String>> rows = db.query(
                "SELECT to_char(t.start_time,'HH24:MI') || ' - ' || to_char(t.end_time,'HH24:MI'), " +
                        "c.name, e.full_name, '—', t.status FROM teaching_schedules t " +
                        "JOIN classes c ON c.class_id=t.class_id JOIN employees e ON e.employee_id=t.teacher_id " +
                        "WHERE t.date=? AND upper(t.status)='ASSIGNED' ORDER BY t.start_time LIMIT 8",
                cells(5), today);
        return response(user, stats, "Lịch hoạt động hôm nay",
                asList("Giờ", "Lớp", "Giáo viên", "Phòng", "Trạng thái", ""), rows,
                "Việc cần theo dõi", pendingOverview());
    }

    private DashboardResponse coordinator(LocalDate today, LocalDate weekStart, UserContext user) {
        long pending = count("SELECT count(*) FROM availabilities a JOIN employees e ON e.employee_id=a.employee_id " +
                "JOIN users u ON u.username=e.username WHERE upper(u.role_id)='TEACHER' AND upper(a.status)='PENDING'");
        long conflicts = count("SELECT count(*) FROM teaching_schedules a JOIN teaching_schedules b " +
                "ON a.teacher_id=b.teacher_id AND a.date=b.date AND a.teaching_schedule_id<b.teaching_schedule_id " +
                "AND a.start_time<b.end_time AND a.end_time>b.start_time " +
                "WHERE upper(a.status)='ASSIGNED' AND upper(b.status)='ASSIGNED' AND a.date BETWEEN ? AND ?",
                weekStart, weekStart.plusDays(6));
        List<Stat> stats = asList(
                stat("GIÁO VIÊN HOẠT ĐỘNG", count("SELECT count(*) FROM employees e JOIN users u ON u.username=e.username " +
                        "WHERE upper(e.status)='ACTIVE' AND upper(u.status)='ACTIVE' AND upper(u.role_id)='TEACHER'"),
                        "Giáo viên có tài khoản hoạt động", true),
                stat("LỊCH RẢNH CHỜ DUYỆT", pending, pending == 0 ? "Không có yêu cầu chờ duyệt" : "Cần xem xét", pending == 0),
                stat("XUNG ĐỘT LỊCH TUẦN NÀY", conflicts, conflicts == 0 ? "Không phát hiện xung đột" : "Cần xử lý", conflicts == 0),
                stat("GIỜ DẠY TUẦN NÀY", hours("teaching_schedules", "date", "status", "ASSIGNED", null, weekStart),
                        "Tổng thời lượng lịch đã phân công", true));
        List<List<String>> rows = teachingRows(today, null);
        List<PanelItem> panel = new ArrayList<>();
        if (pending > 0) panel.add(new PanelItem(pending + " yêu cầu lịch rảnh đang chờ duyệt", "Chờ xử lý", true));
        if (conflicts > 0) panel.add(new PanelItem(conflicts + " cặp lịch giáo viên bị trùng", "Trong tuần này", true));
        return response(user, stats, "Lịch dạy hôm nay",
                asList("Giờ", "Lớp", "Giáo viên", "Trạng thái", ""), rows,
                "Cảnh báo lịch", panel);
    }

    private DashboardResponse centerManager(LocalDate today, LocalDate weekStart, UserContext user) {
        long pending = count("SELECT count(*) FROM availabilities a JOIN employees e ON e.employee_id=a.employee_id " +
                "JOIN users u ON u.username=e.username WHERE upper(u.role_id) IN ('SALE','CS') AND upper(a.status)='PENDING'");
        long conflicts = count("SELECT count(*) FROM staff_schedules a JOIN staff_schedules b " +
                "ON a.employee_id=b.employee_id AND a.date=b.date AND a.staff_schedule_id<b.staff_schedule_id " +
                "AND a.start_time<b.end_time AND a.end_time>b.start_time " +
                "WHERE upper(a.status)='ASSIGNED' AND upper(b.status)='ASSIGNED' AND a.date BETWEEN ? AND ?",
                weekStart, weekStart.plusDays(6));
        List<Stat> stats = asList(
                stat("SALE / CS HOẠT ĐỘNG", count("SELECT count(*) FROM employees e JOIN users u ON u.username=e.username " +
                        "WHERE upper(e.status)='ACTIVE' AND upper(u.status)='ACTIVE' AND upper(u.role_id) IN ('SALE','CS')"),
                        "Nhân sự vận hành hoạt động", true),
                stat("LỊCH RẢNH CHỜ DUYỆT", pending, pending == 0 ? "Không có yêu cầu chờ duyệt" : "Cần xem xét", pending == 0),
                stat("XUNG ĐỘT CA TUẦN NÀY", conflicts, conflicts == 0 ? "Không phát hiện xung đột" : "Cần xử lý", conflicts == 0),
                stat("GIỜ LÀM TUẦN NÀY", hours("staff_schedules", "date", "status", "ASSIGNED", null, weekStart),
                        "Tổng thời lượng ca đã phân công", true));
        List<List<String>> rows = staffRows(today, null, null);
        List<PanelItem> panel = new ArrayList<>();
        if (pending > 0) panel.add(new PanelItem(pending + " yêu cầu lịch rảnh đang chờ duyệt", "Chờ xử lý", true));
        if (conflicts > 0) panel.add(new PanelItem(conflicts + " cặp ca làm việc bị trùng", "Trong tuần này", true));
        return response(user, stats, "Ca làm việc hôm nay",
                asList("Ca", "Khung giờ", "Nhân sự", "Vị trí", "Trạng thái", ""), rows,
                "Cần xử lý", panel);
    }

    private DashboardResponse teacher(LocalDate today, LocalDate weekStart, UserContext user) {
        String employeeId = requireEmployee(user);
        long classes = count("SELECT count(DISTINCT c.class_id) FROM classes c JOIN teaching_schedules t ON t.class_id=c.class_id " +
                "WHERE t.teacher_id=? AND upper(t.status)='ASSIGNED' AND upper(c.status) IN ('READY','RUNNING') " +
                "AND current_date BETWEEN c.start_date AND c.end_date", employeeId);
        long students = count("SELECT count(DISTINCT cs.student_id) FROM class_students cs JOIN classes c ON c.class_id=cs.class_id " +
                "JOIN teaching_schedules t ON t.class_id=c.class_id WHERE t.teacher_id=? AND upper(t.status)='ASSIGNED' " +
                "AND upper(cs.status)='ACTIVE' AND upper(c.status) IN ('READY','RUNNING')", employeeId);
        long openAssignments = count("SELECT count(*) FROM assignments WHERE teacher_id=? AND upper(status)='OPEN'", employeeId);
        long overdue = count("SELECT count(*) FROM assignments WHERE teacher_id=? AND upper(status)='OPEN' AND deadline<CURRENT_TIMESTAMP",
                employeeId);
        long sessions = count("SELECT count(*) FROM teaching_schedules WHERE teacher_id=? AND date BETWEEN ? AND ? " +
                "AND upper(status)='ASSIGNED'", employeeId, weekStart, weekStart.plusDays(6));
        List<Stat> stats = asList(
                stat("LỚP ĐANG PHỤ TRÁCH", classes, "Lớp đang trong thời gian đào tạo", true),
                stat("HỌC VIÊN", students, "Trong các lớp được phân công", true),
                stat("BÀI TẬP ĐANG MỞ", openAssignments, overdue + " bài quá hạn", overdue == 0),
                stat("BUỔI DẠY TUẦN NÀY", sessions, "Lịch dạy đã phân công", true));
        List<List<String>> rows = teacherRows(today, employeeId);
        List<PanelItem> panel = new ArrayList<>();
        if (overdue > 0) panel.add(new PanelItem(overdue + " bài tập đã quá hạn", "Cần xử lý", true));
        if (openAssignments > overdue) panel.add(new PanelItem((openAssignments - overdue) + " bài tập đang mở", "Chưa quá hạn", false));
        return response(user, stats, "Lịch dạy hôm nay",
                asList("Giờ", "Lớp", "Phòng", "Sĩ số", "Trạng thái", ""), rows,
                "Việc cần làm", panel);
    }

    private DashboardResponse sale(LocalDate today, LocalDate weekStart, UserContext user) {
        String employeeId = requireEmployee(user);
        long shifts = count("SELECT count(*) FROM staff_schedules WHERE employee_id=? AND date BETWEEN ? AND ? " +
                "AND upper(status)='ASSIGNED'", employeeId, weekStart, weekStart.plusDays(6));
        long hours = countHours("SELECT COALESCE(SUM(EXTRACT(EPOCH FROM (end_time-start_time))/3600),0) " +
                "FROM staff_schedules WHERE employee_id=? AND date BETWEEN ? AND ? AND upper(status)='ASSIGNED'",
                employeeId, weekStart, weekStart.plusDays(6));
        long pending = count("SELECT count(*) FROM availabilities WHERE employee_id=? AND upper(status)='PENDING'", employeeId);
        long todayShifts = count("SELECT count(*) FROM staff_schedules WHERE employee_id=? AND date=? AND upper(status)='ASSIGNED'",
                employeeId, today);
        List<Stat> stats = asList(
                stat("CA TUẦN NÀY", shifts, "Ca đã được phân công", true),
                stat("GIỜ LÀM TUẦN NÀY", formatHours(hours), "Tổng thời lượng ca được phân công", true),
                stat("LỊCH RẢNH CHỜ DUYỆT", pending, pending == 0 ? "Không có yêu cầu chờ duyệt" : "Đang chờ quản lý duyệt", pending == 0),
                stat("CA HÔM NAY", todayShifts, todayShifts == 0 ? "Không có ca được phân công" : "Ca đã được phân công hôm nay", true));
        List<List<String>> rows = staffRows(today, employeeId, null);
        List<PanelItem> panel = pending == 0 ? emptyList() :
                List.of(new PanelItem(pending + " yêu cầu lịch rảnh đang chờ duyệt", "Chờ quản lý duyệt", false));
        return response(user, stats, "Lịch làm việc hôm nay",
                asList("Khung giờ", "Ca", "Vị trí", "Trạng thái", ""), rows,
                "Nhắc việc", panel);
    }

    private DashboardResponse customerSupport(LocalDate today, LocalDate weekStart, UserContext user) {
        String employeeId = requireEmployee(user);
        String scope = "EXISTS (SELECT 1 FROM class_access_scopes s WHERE s.class_id=c.class_id " +
                "AND s.employee_id=? AND upper(s.status)='ACTIVE')";
        long classes = count("SELECT count(*) FROM classes c WHERE " + scope +
                " AND upper(c.status) IN ('READY','RUNNING') AND current_date BETWEEN c.start_date AND c.end_date",
                employeeId);
        long students = count("SELECT count(DISTINCT cs.student_id) FROM classes c JOIN class_students cs ON cs.class_id=c.class_id " +
                "WHERE " + scope + " AND upper(cs.status)='ACTIVE'", employeeId);
        long shifts = count("SELECT count(*) FROM staff_schedules WHERE employee_id=? AND date BETWEEN ? AND ? " +
                "AND upper(status)='ASSIGNED'", employeeId, weekStart, weekStart.plusDays(6));
        long missingTeacher = count("SELECT count(*) FROM classes c WHERE " + scope +
                " AND upper(c.status) IN ('READY','RUNNING') AND current_date BETWEEN c.start_date AND c.end_date " +
                "AND NOT EXISTS (SELECT 1 FROM teaching_schedules t WHERE t.class_id=c.class_id " +
                "AND upper(t.status)='ASSIGNED' AND t.date>=current_date)", employeeId);
        long todayShifts = count("SELECT count(*) FROM staff_schedules WHERE employee_id=? AND date=? AND upper(status)='ASSIGNED'",
                employeeId, today);
        List<Stat> stats = asList(
                stat("LỚP ĐANG HỖ TRỢ", classes, "Lớp đang hoạt động được giao quyền", true),
                stat("HỌC VIÊN", students, "Trong các lớp được giao hỗ trợ", true),
                stat("LỚP THIẾU LỊCH GIÁO VIÊN", missingTeacher, missingTeacher == 0 ? "Các lớp đều có lịch sắp tới" : "Cần kiểm tra lịch dạy", missingTeacher == 0),
                stat("CA HÔM NAY", todayShifts, "Ca làm việc đã phân công", true));
        List<List<String>> rows = db.query(
                "SELECT c.class_code, c.name, count(DISTINCT cs.student_id), c.status " +
                        "FROM classes c LEFT JOIN class_students cs ON cs.class_id=c.class_id AND upper(cs.status)='ACTIVE' " +
                        "WHERE " + scope + " AND upper(c.status) IN ('READY','RUNNING') " +
                        "AND current_date BETWEEN c.start_date AND c.end_date " +
                        "GROUP BY c.class_id,c.class_code,c.name,c.status ORDER BY c.name LIMIT 8",
                (rs, rowNum) -> asList(rs.getString(1), rs.getString(2), "—",
                        rs.getLong(3) + " học viên", classStatus(rs.getString(4)), "Xem"),
                employeeId);
        List<PanelItem> panel = missingTeacher == 0 ? emptyList() :
                List.of(new PanelItem(missingTeacher + " lớp đang hoạt động chưa có lịch dạy sắp tới", "Cần kiểm tra", true));
        return response(user, stats, "Lớp đang hỗ trợ",
                asList("Mã lớp", "Tên lớp", "Phòng", "Sĩ số", "Trạng thái", ""), rows,
                "Cần bổ sung", panel);
    }

    private List<PanelItem> pendingOverview() {
        long pending = count("SELECT count(*) FROM availabilities WHERE upper(status)='PENDING'");
        long classesWithoutTeacher = count("SELECT count(*) FROM classes c WHERE upper(c.status) IN ('READY','RUNNING') " +
                "AND current_date BETWEEN c.start_date AND c.end_date AND NOT EXISTS " +
                "(SELECT 1 FROM teaching_schedules t WHERE t.class_id=c.class_id AND upper(t.status)='ASSIGNED' " +
                "AND t.date>=current_date)");
        List<PanelItem> items = new ArrayList<>();
        if (pending > 0) items.add(new PanelItem(pending + " yêu cầu lịch rảnh chờ duyệt", "Toàn hệ thống", true));
        if (classesWithoutTeacher > 0)
            items.add(new PanelItem(classesWithoutTeacher + " lớp chưa có lịch dạy sắp tới", "Cần kiểm tra", true));
        return items;
    }

    private List<List<String>> teachingRows(LocalDate date, String teacherId) {
        String teacherFilter = teacherId == null ? "" : " AND t.teacher_id=?";
        String sql = "SELECT to_char(t.start_time,'HH24:MI') || ' - ' || to_char(t.end_time,'HH24:MI'), " +
                "c.name, e.full_name, t.status FROM teaching_schedules t " +
                "JOIN classes c ON c.class_id=t.class_id JOIN employees e ON e.employee_id=t.teacher_id " +
                "WHERE t.date=? AND upper(t.status)='ASSIGNED'" + teacherFilter +
                " ORDER BY t.start_time LIMIT 8";
        if (teacherId == null) return db.query(sql, cells(4), date);
        return db.query(sql, cells(4), date, teacherId);
    }

    private List<List<String>> teacherRows(LocalDate date, String employeeId) {
        return db.query(
                "SELECT to_char(t.start_time,'HH24:MI') || ' - ' || to_char(t.end_time,'HH24:MI'), " +
                        "c.name, count(DISTINCT cs.student_id), t.status FROM teaching_schedules t " +
                        "JOIN classes c ON c.class_id=t.class_id " +
                        "LEFT JOIN class_students cs ON cs.class_id=c.class_id AND upper(cs.status)='ACTIVE' " +
                        "WHERE t.date=? AND t.teacher_id=? AND upper(t.status)='ASSIGNED' " +
                        "GROUP BY t.teaching_schedule_id,c.name,t.start_time,t.end_time,t.status " +
                        "ORDER BY t.start_time LIMIT 8",
                (rs, rowNum) -> asList(rs.getString(1), rs.getString(2), "—",
                        rs.getLong(3) + " học viên", scheduleStatus(rs.getString(4)), "Xem"),
                date, employeeId);
    }

    private List<List<String>> staffRows(LocalDate date, String employeeId, String role) {
        StringBuilder sql = new StringBuilder(
                "SELECT to_char(s.start_time,'HH24:MI') || ' - ' || to_char(s.end_time,'HH24:MI'), " +
                        "s.work_type, e.full_name, s.workplace_id, c.class_code, s.status FROM staff_schedules s " +
                        "JOIN employees e ON e.employee_id=s.employee_id JOIN users u ON u.username=e.username " +
                        "LEFT JOIN classes c ON c.class_id=s.class_id " +
                        "WHERE s.date=? AND upper(s.status)='ASSIGNED'");
        List<Object> args = new ArrayList<>(List.of(date));
        if (employeeId != null) {
            sql.append(" AND s.employee_id=?");
            args.add(employeeId);
        }
        if (role != null) {
            sql.append(" AND upper(u.role_id)=?");
            args.add(role.toUpperCase(Locale.ROOT));
        }
        sql.append(" ORDER BY s.start_time LIMIT 8");
        return db.query(sql.toString(), (rs, rowNum) -> {
            String workType = rs.getString(2);
            String position = positionName(rs.getString(4), rs.getString(5));
            return employeeId == null
                    ? asList(shiftName(workType), rs.getString(1), rs.getString(3), position,
                            scheduleStatus(rs.getString(6)), "Xem")
                    : asList(rs.getString(1), shiftName(workType), position, scheduleStatus(rs.getString(6)), "Xem");
        }, args.toArray());
    }

    private DashboardResponse response(UserContext user, List<Stat> stats, String title, List<String> columns,
                                       List<List<String>> rows, String panelTitle, List<PanelItem> panel) {
        return new DashboardResponse(user.role(), stats, title, columns, rows,
                activities(user.username(), user.role().equalsIgnoreCase("ADMIN")), panelTitle, panel);
    }

    private List<Activity> activities(String username, boolean global) {
        String sql = "SELECT username, action, entity_type, description, created_at FROM audit_logs " +
                (global ? "" : "WHERE username=? ") + "ORDER BY created_at DESC LIMIT 4";
        RowMapper<Activity> mapper = (rs, rowNum) -> {
            String description = rs.getString("description");
            String actor = rs.getString("username");
            String action = rs.getString("action");
            String text = description == null || description.isBlank()
                    ? (actor == null ? "Hệ thống" : actor) + " thực hiện " + actionLabel(action) +
                    " (" + rs.getString("entity_type") + ")"
                    : description;
            return new Activity(text, relativeTime(toInstant(rs.getObject("created_at"))));
        };
        return global ? db.query(sql, mapper) : db.query(sql, mapper, username);
    }

    private Stat stat(String label, Object value, String trend, boolean trendUp) {
        return new Stat(label, String.valueOf(value), trend, trendUp);
    }

    private long count(String sql, Object... args) {
        Long value = db.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    private long countHours(String sql, Object... args) {
        Number value = db.queryForObject(sql, Number.class, args);
        return value == null ? 0 : Math.round(value.doubleValue() * 10);
    }

    private String hours(String table, String dateColumn, String statusColumn, String status,
                         String employeeId, LocalDate weekStart) {
        String employeeFilter = employeeId == null ? "" : " AND employee_id=?";
        String sql = "SELECT COALESCE(SUM(EXTRACT(EPOCH FROM (end_time-start_time))/3600),0) " +
                "FROM " + table + " WHERE " + dateColumn + " BETWEEN ? AND ? AND upper(" + statusColumn + ")=?" +
                employeeFilter;
        List<Object> args = new ArrayList<>(List.of(weekStart, weekStart.plusDays(6), status));
        if (employeeId != null) args.add(employeeId);
        Number value = db.queryForObject(sql, Number.class, args.toArray());
        return formatHours(value == null ? 0 : Math.round(value.doubleValue() * 10));
    }

    private String formatHours(long tenths) {
        return String.format(Locale.ROOT, "%.1fh", tenths / 10.0);
    }

    private RowMapper<List<String>> cells(int count) {
        return (rs, rowNum) -> {
            List<String> values = new ArrayList<>(count);
            for (int i = 1; i <= count; i++) {
                String value = rs.getString(i);
                values.add(i == count ? scheduleStatus(value) : value);
            }
            values.add("Xem");
            return values;
        };
    }

    private String relativeTime(Instant instant) {
        if (instant == null) return "Không rõ thời gian";
        Duration age = Duration.between(instant, Instant.now());
        if (age.isNegative() || age.toMinutes() < 1) return "Vừa xong";
        if (age.toHours() < 1) return age.toMinutes() + " phút trước";
        if (age.toDays() < 1) return age.toHours() + " giờ trước";
        return age.toDays() + " ngày trước";
    }

    private Instant toInstant(Object value) {
        if (value instanceof Instant instant) return instant;
        if (value instanceof OffsetDateTime offsetDateTime) return offsetDateTime.toInstant();
        if (value instanceof Timestamp timestamp) return timestamp.toInstant();
        return null;
    }

    private String scheduleStatus(String status) {
        if (status == null) return "—";
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "ASSIGNED", "APPROVED" -> "Đã xác nhận";
            case "PENDING" -> "Chờ duyệt";
            case "CANCELLED", "CANCELED" -> "Đã hủy";
            default -> status;
        };
    }

    private String classStatus(String status) {
        if (status == null) return "—";
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "RUNNING" -> "Đang học";
            case "READY" -> "Sẵn sàng";
            default -> status;
        };
    }

    private String shiftName(String workType) {
        if (workType == null) return "Ca làm";
        return switch (workType.toUpperCase(Locale.ROOT)) {
            case "MORNING" -> "Ca sáng";
            case "AFTERNOON" -> "Ca chiều";
            case "EVENING" -> "Ca tối";
            default -> workType;
        };
    }

    private String positionName(String workplaceId, String classCode) {
        if (workplaceId != null && !workplaceId.isBlank()) {
            return switch (workplaceId) {
                case "center-01" -> "Cơ sở chính";
                case "center-02" -> "Cơ sở Quận 1";
                case "center-03" -> "Cơ sở Quận 3";
                default -> workplaceId;
            };
        }
        return classCode == null || classCode.isBlank() ? "—" : classCode;
    }

    private String actionLabel(String action) {
        if (action == null) return "cập nhật dữ liệu";
        return switch (action.toUpperCase(Locale.ROOT)) {
            case "ASSIGN_TEACHER" -> "phân công giáo viên";
            case "ASSIGN_STAFF" -> "phân công nhân sự";
            case "CREATE_CLASS" -> "tạo lớp học";
            case "UPDATE_CLASS" -> "cập nhật lớp học";
            case "ADD_STUDENTS" -> "thêm học viên";
            default -> action.toLowerCase(Locale.ROOT).replace('_', ' ');
        };
    }
}
