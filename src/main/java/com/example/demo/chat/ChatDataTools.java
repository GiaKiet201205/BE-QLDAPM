package com.example.demo.chat;

import com.example.demo.exception.ApiException;
import com.example.demo.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** An allowlist of read-only tools. User/model text is never executable SQL or a role. */
@Component
@RequiredArgsConstructor
public class ChatDataTools {
    private final DashboardService dashboard;
    private final StatisticsService statistics;
    private final StudentManagementService students;
    private final ClassManagementService classes;
    private final EmployeeManagementService employees;
    private final PayrollService payroll;
    private final SchedulingAccess access;
    private final JdbcTemplate db;
    private final AssignmentService assignments;
    private final AvailabilityService availability;

    record Result(String answer, ChatDtos.Source source) {}

    private static String canonical(String query) {
        return ChatKnowledge.normalize(query).replace("hoc sinh", "hoc vien").replace("sinh vien", "hoc vien")
                .replace("giang vien", "giao vien").replace("don mua", "don hang");
    }

    static boolean wantsData(String query) {
        String q = canonical(query);
        if (q.matches(".*\\b(cach|huong dan|o dau|the nao|tai sao|vi sao|la gi|ra sao|quy trinh|giai thich|y nghia|phan biet|khac nhau)\\b.*")) return false;
        return q.matches(".*\\b(bao nhieu|so luong|danh sach|liet ke|tra cuu|tim|hien tai|hom nay|hom qua|ngay mai|thang|nam|tuan|tong quan|so lieu|cua toi|doanh thu|bang luong|thong ke)\\b.*");
    }

    @Transactional(readOnly = true)
    public Optional<Result> lookup(Authentication actor, String query) {
        if (!wantsData(query)) return Optional.empty();
        if (actor == null || !actor.isAuthenticated() || "anonymousUser".equals(actor.getPrincipal())) {
            throw new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (actor.getAuthorities().stream().noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            return Optional.of(message("Vai trò hiện tại không được phép tra cứu dữ liệu thật qua trợ lý. Chỉ ADMIN được dùng chức năng này.", actor, "/profile"));
        }
        String q = canonical(query);
        if (q.matches("^(?:(?:hay|ban|giup toi|lam on)\\s+)*(xoa|tao|sua|cap nhat|phe duyet|duyet|xac nhan|tinh lai|tinh luong|huy)\\b.*")) {
            return Optional.of(message("Trợ lý chỉ tra cứu, không thay đổi dữ liệu. Hãy dùng màn hình nghiệp vụ để thực hiện thao tác.", actor, "/profile"));
        }
        try {
            access.requireActiveRole("ADMIN");
            ChatPeriod period = ChatPeriod.parse(query, LocalDate.now(ChatPeriod.ZONE));
            if (q.startsWith("tu ") || q.contains(" tu ") || q.contains(" den ") || q.contains("so sanh")) {
                return Optional.of(message("Hãy hỏi một ngày, tháng hoặc năm cụ thể cho mỗi lần tra cứu; chưa hỗ trợ khoảng tùy chọn hoặc so sánh nhiều kỳ.", actor, "/profile"));
            }
            if (q.contains("don hang") || q.contains("thanh toan") || q.contains("doanh thu")) return Optional.of(orders(actor, q, period));
            if (q.contains("luong") || q.contains("so cong")) return Optional.of(payroll(actor, period));
            if (q.contains("thong ke") || q.contains("bao cao") || q.contains("chi phi")) return Optional.of(statistics(actor, period));
            if (q.contains("lich ranh")) return Optional.of(availability(actor, q, period));
            if (q.contains("lich") || q.contains("ca lam")) return Optional.of(schedules(actor, period));
            if (q.contains("hoc vien")) return Optional.of(students(actor, query, q, period));
            if (q.contains("lop") && !q.contains("lich")) return Optional.of(classes(actor, query, q, period));
            if (q.contains("nhan vien") || q.contains("nhan su") || q.contains("giao vien") && !q.contains("lich")) return Optional.of(employees(actor, query, period));
            if (q.contains("tong quan") || q.contains("so lieu")) return Optional.of(dashboard(actor, period));
            return Optional.empty();
        } catch (DateTimeException | NumberFormatException invalidDate) {
            return Optional.of(message("Ngày/tháng không hợp lệ. Hãy dùng YYYY-MM-DD, DD/MM/YYYY hoặc tháng 11/2026.", actor, "/profile"));
        } catch (ApiException denied) {
            if (denied.getStatus().value() != 403) throw denied;
            return Optional.of(message("Tài khoản hiện tại không được phép tra cứu dữ liệu này. Quyền được kiểm tra từ tài khoản đang đăng nhập.", actor, "/profile"));
        } catch (DataAccessException unavailable) {
            return Optional.of(message("Chưa lấy được dữ liệu từ database. Hãy thử lại hoặc kiểm tra tại màn hình nghiệp vụ; không có số liệu xác nhận lúc này.", actor, "/profile"));
        }
    }

    private Result dashboard(Authentication actor, ChatPeriod period) {
        if (!Set.of("CURRENT", "DAY").contains(period.kind()) || !period.start().equals(LocalDate.now(ChatPeriod.ZONE))) {
            return message("Tra cứu tổng quan/lịch trong chat hiện hỗ trợ hôm nay. Để xem ngày hoặc tuần khác, hãy mở màn hình lịch theo vai trò.", actor, "/");
        }
        var data = dashboard.getDashboard(actor);
        StringBuilder text = new StringBuilder("Tổng quan hiện tại · ngày " + period.label() + ":\n");
        data.stats().forEach(s -> text.append("• ").append(s.label()).append(": ").append(s.value()).append(" — ").append(s.trend()).append('\n'));
        text.append('\n').append(data.tableTitle()).append(" (tối đa 8 dòng, không phải toàn bộ lịch):\n");
        if (data.tableRows().isEmpty()) text.append("Chưa có dòng nào trong kết quả này.\n");
        data.tableRows().stream().limit(8).forEach(row -> text.append("• ").append(String.join(" | ", row)).append('\n'));
        data.panelItems().stream().limit(4).forEach(p -> text.append("• ").append(p.label()).append(" — ").append(p.due()).append('\n'));
        return message(text.toString(), actor, "/");
    }

    private Result statistics(Authentication actor, ChatPeriod requested) {
        access.requireActiveRole("ADMIN");
        ChatPeriod p = requested.kind().equals("CURRENT") ? ChatPeriod.parse("tháng này", requested.start()) : requested;
        if (p.kind().equals("WEEK")) return message("Thống kê hỗ trợ ngày, tháng hoặc năm. Hãy chỉ rõ kỳ cần xem.", actor, "/statistics");
        var data = statistics.getStatistics(p.kind(), p.start().getYear(), p.start().getMonthValue(), p.start());
        String text = "Thống kê kỳ " + p.label() + ":\n"
                + "• Học viên đang hoạt động hiện tại: " + data.overview().activeStudents() + "\n"
                + "• Học viên mới trong kỳ: " + data.overview().newStudents() + "\n"
                + "• Lớp đang hoạt động hiện tại: " + data.overview().activeClasses() + "\n"
                + "• Tổng nhân sự hiện tại: " + data.employees().total() + "\n"
                + "• Lịch dạy đã phân công trong kỳ: " + data.schedules().teachingAssigned() + "\n"
                + "• Ca làm đã phân công trong kỳ: " + data.schedules().staffAssigned() + "\n"
                + "• Chi phí lương đã ghi nhận trong kỳ: " + (data.financial().salaryCost() == null ? "chưa có số liệu cho kỳ này" : data.financial().salaryCost() + " VND") + "\n"
                + "Các chỉ số hiện tại không phải số lượng lịch sử của kỳ. Doanh thu thanh toán được tra riêng bằng câu hỏi doanh thu/đơn hàng.";
        return message(text, actor, "/statistics");
    }

    private Result students(Authentication actor, String query, String q, ChatPeriod period) {
        access.requireActiveRole("ADMIN", "TEACHER");
        if (!period.kind().equals("CURRENT")) return message("Danh sách học viên phản ánh hiện tại. Để xem số học viên mới theo kỳ, hãy hỏi ‘Thống kê tháng 11/2026’ với tài khoản ADMIN.", actor, "/students");
        String term = searchTerm(query);
        String status = q.contains("tot nghiep") ? "GRADUATED" : q.contains("bao luu") || q.contains("tam nghi") ? "ON LEAVE" : q.contains("dang hoat dong") ? "ACTIVE" : null;
        var rows = students.listStudents(actor).stream().filter(s -> matches(term, s.studentCode(), s.fullName()))
                .filter(s -> status == null || status.equalsIgnoreCase(s.status())).toList();
        return message("Học viên hiện tại trong phạm vi được phép" + filterLabel(term) + (status == null ? ", mọi trạng thái" : ", trạng thái " + status)
                + ": " + rows.size() + ".\nHiển thị tối đa 8 kết quả:\n"
                + rows.stream().limit(8).map(s -> "• " + s.studentCode() + " | " + s.fullName() + " | " + s.status()).collect(Collectors.joining("\n")), actor, "/students");
    }

    private Result classes(Authentication actor, String query, String q, ChatPeriod period) {
        access.requireActiveRole("ADMIN", "TEACHER", "CS");
        String path = role(actor).equals("TEACHER") ? "/my-classes" : role(actor).equals("CS") ? "/classes/update" : "/classes";
        if (!period.kind().equals("CURRENT")) return message("Danh sách lớp phản ánh trạng thái hiện tại. Hãy hỏi thống kê theo kỳ hoặc mở chi tiết lớp để kiểm tra thời gian.", actor, path);
        String term = searchTerm(query);
        Set<String> statuses = q.contains("dang hoc") ? Set.of("RUNNING") : q.contains("ban nhap") ? Set.of("DRAFT") : q.contains("da dong") ? Set.of("CLOSED") : q.contains("hoan thanh") ? Set.of("COMPLETED") : q.contains("san sang") ? Set.of("READY") : q.contains("dang hoat dong") ? Set.of("READY", "RUNNING") : Set.of();
        var rows = classes.listClasses(actor).stream().filter(c -> matches(term, c.classCode(), c.name()))
                .filter(c -> statuses.isEmpty() || statuses.contains(c.status().toUpperCase(Locale.ROOT))).toList();
        return message("Lớp hiện tại trong phạm vi được phép" + filterLabel(term) + (statuses.isEmpty() ? ", mọi trạng thái" : ", trạng thái " + String.join("/", new TreeSet<>(statuses)))
                + ": " + rows.size() + ".\nHiển thị tối đa 8 kết quả:\n"
                + rows.stream().limit(8).map(c -> "• " + c.classCode() + " | " + c.name() + " | " + c.status() + " | " + c.studentCount() + " học viên | " + c.startDate() + " đến " + c.endDate()).collect(Collectors.joining("\n")), actor, path);
    }

    private Result employees(Authentication actor, String query, ChatPeriod period) {
        access.requireActiveRole("ADMIN", "TC", "CM");
        String path = role(actor).equals("TC") ? "/teachers" : role(actor).equals("CM") ? "/staff" : "/employees";
        if (!period.kind().equals("CURRENT")) return message("Tra cứu nhân sự hiện phản ánh trạng thái hiện tại, chưa hỗ trợ ảnh chụp nhân sự lịch sử.", actor, path);
        String term = searchTerm(query);
        String q = ChatKnowledge.normalize(query);
        String targetRole = q.contains("giao vien") ? "TEACHER" : q.contains("sale") ? "SALE" : q.contains("cs") ? "CS" : null;
        var rows = employees.list(null, null, targetRole, q.contains("dang hoat dong") ? "ACTIVE" : null, actor).stream()
                .filter(e -> matches(term, String.valueOf(e.get("employeeCode")), String.valueOf(e.get("fullName")))).toList();
        return message("Nhân sự hiện tại trong phạm vi quản lý" + filterLabel(term) + " | Vai trò: " + (targetRole == null ? "mọi vai trò được phép" : targetRole)
                + " | Trạng thái: " + (q.contains("dang hoat dong") ? "ACTIVE" : "tất cả") + ": " + rows.size()
                + ".\nHiển thị tối đa 8 kết quả:\n" + rows.stream().limit(8)
                .map(e -> "• " + e.get("employeeCode") + " | " + e.get("fullName") + " | " + e.get("role") + " | " + e.get("status"))
                .collect(Collectors.joining("\n")), actor, path);
    }

    private Result schedules(Authentication actor, ChatPeriod requested) {
        String r = role(actor);
        ChatPeriod p = requested.kind().equals("CURRENT") ? ChatPeriod.parse("tuần này", requested.start()) : requested;
        String path = switch (r) {
            case "ADMIN" -> "/monitor/schedules";
            case "TC" -> "/schedule/overview";
            case "CM" -> "/shift/overview";
            case "TEACHER" -> "/teaching-schedule";
            case "SALE" -> "/availability/mine";
            default -> "/my-shifts";
        };
        if (p.kind().equals("YEAR")) return message("Lịch trong chat hỗ trợ ngày, tuần hoặc tháng. Hãy chọn kỳ ngắn hơn một năm.", actor, path);
        List<Map<String,Object>> rows = new ArrayList<>();
        for (LocalDate week = p.start().with(DayOfWeek.MONDAY); !week.isAfter(p.end()); week = week.plusWeeks(1)) {
            if (Set.of("TEACHER", "SALE", "CS").contains(r)) rows.addAll(assignments.mine(week));
            else {
                if (Set.of("ADMIN", "TC").contains(r)) rows.addAll(assignments.teaching(week));
                if (Set.of("ADMIN", "CM").contains(r)) rows.addAll(assignments.staff(week, null));
            }
        }
        var selected = rows.stream().filter(row -> {
            LocalDate date = LocalDate.parse(row.get("date").toString());
            return !date.isBefore(p.start()) && !date.isAfter(p.end());
        }).sorted(Comparator.comparing(row -> row.get("date") + " " + row.get("startTime"))).toList();
        return message("Lịch trong phạm vi được phép kỳ " + p.label() + ": " + selected.size() + " bản ghi. "
                + (Set.of("TEACHER", "SALE", "CS").contains(r) ? "Chỉ lịch cá nhân ASSIGNED." : "Gồm mọi trạng thái lịch; kiểm tra trạng thái từng dòng.")
                + "\nHiển thị tối đa 8 kết quả:\n" + selected.stream().limit(8).map(row -> "• " + row.get("date") + " | " + row.get("startTime") + "–" + row.get("endTime")
                + " | " + row.getOrDefault("teacherName", row.getOrDefault("employeeName", ""))
                + " | " + row.getOrDefault("classCode", row.getOrDefault("workType", "")) + " | " + row.get("status")).collect(Collectors.joining("\n")), actor, path);
    }

    private Result availability(Authentication actor, String query, ChatPeriod requested) {
        String r = role(actor);
        ChatPeriod p = requested.kind().equals("CURRENT") ? ChatPeriod.parse("tuần này", requested.start()) : requested;
        String path = switch (r) {
            case "ADMIN" -> "/monitor/schedules";
            case "TC" -> "/assign-teacher";
            case "CM" -> "/assign-sale";
            default -> "/availability/register";
        };
        if (!p.kind().equals("WEEK")) return message("Lịch rảnh trong chat được tra theo tuần. Hãy hỏi ‘Danh sách lịch rảnh tuần này’ hoặc ‘tuần sau’.", actor, path);
        String status = query.contains("cho duyet") ? "PENDING" : query.contains("da duyet") ? "APPROVED" : query.contains("tu choi") ? "REJECTED" : null;
        var found = Set.of("TEACHER", "SALE", "CS").contains(r) ? availability.mine(p.start()) : availability.list(null, status, p.start());
        var rows = found.stream().filter(row -> status == null || status.equalsIgnoreCase(String.valueOf(row.get("status")))).toList();
        return message("Lịch rảnh kỳ " + p.label() + " trong phạm vi được phép: " + rows.size() + " bản ghi, trạng thái " + (status == null ? "tất cả" : status) + ". Lịch rảnh định kỳ có thể chưa được chuyển thành ca phân công.\nHiển thị tối đa 8 kết quả:\n"
                + rows.stream().limit(8).map(row -> "• " + row.get("fullName") + " | " + row.get("day") + " | " + row.get("start") + "–" + row.get("end") + " | " + row.get("status")).collect(Collectors.joining("\n")), actor, path);
    }

    private Result payroll(Authentication actor, ChatPeriod requested) {
        String r = role(actor);
        if (Set.of("TEACHER", "SALE", "CS").contains(r)) access.requireRole(r); // Reject missing employee profiles before payroll's fallback.
        else access.requireActiveRole("ADMIN", "TC", "CM");
        ChatPeriod p = requested.kind().equals("CURRENT") ? ChatPeriod.parse("tháng này", requested.start()) : requested;
        String path = r.equals("ADMIN") || r.equals("TEACHER") ? "/monitor/payroll" : r.equals("TC") ? "/payroll" : "/payroll/history";
        if (!p.kind().equals("MONTH")) return message("Bảng lương được lưu theo tháng. Hãy hỏi ‘Bảng lương tháng 11/2026’.", actor, path);
        var rows = payroll.payrolls(YearMonth.from(p.start()).toString(), null);
        return message("Bảng lương đã lưu kỳ " + YearMonth.from(p.start()) + " trong phạm vi được phép: " + rows.size() + " bản ghi.\n"
                + (rows.isEmpty() ? "Chưa có bảng lương đã lưu; không có nghĩa là lương bằng 0."
                : "Hiển thị tối đa 8 bản ghi:\n" + rows.stream().limit(8).map(e -> "• " + e.get("employeeCode") + " | " + e.get("employeeName") + " | " + e.get("totalAmount") + " VND | " + e.get("totalHours") + " giờ").collect(Collectors.joining("\n")))
                + "\nChat không tính lại hoặc cập nhật lương.", actor, path);
    }

    private Result orders(Authentication actor, String q, ChatPeriod period) {
        access.requireActiveRole("ADMIN"); // Store's admin list relies on HTTP security; enforce here explicitly.
        String clause = period.kind().equals("CURRENT") ? "" : " AND created_at >= ? AND created_at < ?";
        String statusFilter = q.contains("chua thanh toan") ? " AND payment_status<>'PAID'" : q.contains("da thanh toan") ? " AND payment_status='PAID'" : q.contains("cho xac nhan") ? " AND status='PENDING'" : "";
        Object[] args = period.kind().equals("CURRENT") ? new Object[0] : new Object[]{period.start().atStartOfDay(ChatPeriod.ZONE).toOffsetDateTime(), period.end().plusDays(1).atStartOfDay(ChatPeriod.ZONE).toOffsetDateTime()};
        var groups = db.queryForList("SELECT status,payment_status,count(*) AS total FROM storefront_orders WHERE 1=1" + clause + statusFilter + " GROUP BY status,payment_status ORDER BY status,payment_status", args);
        var revenue = db.queryForObject("SELECT COALESCE(sum(amount),0) FROM storefront_orders WHERE payment_status='PAID'"
                        + (period.kind().equals("CURRENT") ? "" : " AND paid_at >= ? AND paid_at < ?"), java.math.BigDecimal.class, args);
        return message("Đơn hàng " + (period.kind().equals("CURRENT") ? "toàn thời gian" : "tạo trong kỳ " + period.label())
                + " | Bộ lọc: " + (statusFilter.isEmpty() ? "mọi trạng thái" : q.contains("chua thanh toan") ? "thanh toán khác PAID" : q.contains("da thanh toan") ? "thanh toán PAID" : "đơn PENDING") + " (nhóm theo trạng thái đơn / thanh toán):\n"
                + (groups.isEmpty() ? "Chưa có đơn hàng trong phạm vi này." : groups.stream().map(g -> "• " + g.get("status") + " / " + g.get("payment_status") + ": " + g.get("total")).collect(Collectors.joining("\n")))
                + "\nTổng tiền tất cả đơn PAID (tính riêng, không áp dụng bộ lọc danh sách ở trên) theo thời điểm thanh toán " + (period.kind().equals("CURRENT") ? "toàn thời gian" : "trong kỳ " + period.label()) + ": " + revenue + " VND."
                + "\nĐây là số tiền đã ghi nhận PAID trong DB, có thể gồm giao dịch sandbox; không phải báo cáo lợi nhuận hoặc doanh thu kế toán đã đối soát.", actor, "/storefront");
    }

    static String searchTerm(String query) {
        var quoted = Pattern.compile("[\"“]([^\"”]{1,100})[\"”]").matcher(query);
        if (quoted.find()) return ChatKnowledge.normalize(quoted.group(1)).strip();
        var named = Pattern.compile("\\b(?:ten|ma)\\s+([\\p{L}\\p{N} _-]{1,100})", Pattern.CASE_INSENSITIVE).matcher(ChatKnowledge.normalize(query));
        return named.find() ? named.group(1).strip() : "";
    }
    private static boolean matches(String term, String... values) {
        return term.isEmpty() || Arrays.stream(values).filter(Objects::nonNull).anyMatch(v -> ChatKnowledge.normalize(v).contains(term));
    }
    private static String filterLabel(String term) { return term.isEmpty() ? " (không lọc tên/mã)" : " (tên/mã chứa ‘" + term + "’)"; }
    private static String role(Authentication actor) {
        return actor.getAuthorities().stream().map(a -> a.getAuthority().replaceFirst("^ROLE_", "")).filter(ChatKnowledge.ROLES::contains).findFirst().orElseThrow();
    }
    private static Result message(String text, Authentication actor, String path) {
        return new Result(text + "\n\nTra cứu lúc " + java.time.ZonedDateTime.now(ChatPeriod.ZONE).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss 'GMT+7'")),
                new ChatDtos.Source("live-data", "Nguồn dữ liệu · mở màn hình kiểm tra", "/" + role(actor).toLowerCase(Locale.ROOT) + path));
    }
}
