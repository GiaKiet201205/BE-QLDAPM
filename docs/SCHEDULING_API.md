# API lịch rảnh và phân công

Nhánh: feature/availability-staff-scheduling. Base URL: http://localhost:8080/api. Tất cả API yêu cầu Bearer JWT.

| Method | Route | Quyền |
|---|---|---|
| GET | /employees?role=TEACHER | TC/CM/ADMIN, giới hạn vai trò |
| GET | /availabilities/me?weekStart=2026-10-19 | TEACHER/CS/SALE |
| GET | /availabilities?role=CS&status=PENDING&weekStart=2026-10-19 | TC/CM/ADMIN |
| POST, PUT, DELETE | /availabilities, /availabilities/{id} | chủ lịch |
| PATCH | /availabilities/{id}/review | TC duyệt Teacher, CM duyệt CS/Sale |
| GET, POST, PUT, DELETE | /teaching-schedules, /teaching-schedules/{id} | TC ghi, TC/ADMIN đọc |
| GET, POST, PUT, DELETE | /staff-schedules, /staff-schedules/{id} | CM ghi, CM/ADMIN đọc |
| GET | /schedules/me?weekStart=2026-10-19 | TEACHER/CS/SALE |

WeekStart chuẩn hoá về Thứ Hai; mặc định tuần hiện tại theo Asia/Ho_Chi_Minh. Ngày YYYY-MM-DD, giờ HH:mm.

## Đăng ký lịch rảnh

```json
{"dayOfWeek":"mon","shift":"evening","startTime":"18:30","endTime":"20:30","availabilityType":"RECURRING","campus":"Cơ sở chính","note":"Có thể dạy buổi tối"}
```

RECURRING: lịch lặp theo thứ, không có availableDate. WEEKLY: yêu cầu availableDate, dayOfWeek nếu có phải khớp ngày. Tạo mới luôn PENDING. Duyệt: PATCH với body `{"status":"APPROVED"}` hoặc `{"status":"REJECTED"}`.

## Phân công Teacher

```json
{"teacherId":"teacher-001","classId":"class-0001","date":"2026-10-19","startTime":"18:30","endTime":"20:30","campus":"Cơ sở chính"}
```

## Phân công CS/Sale

```json
{"employeeId":"cs-001","staffRole":"CS","workplaceId":"center-01","classId":"class-0001","date":"2026-10-19","startTime":"17:30","endTime":"20:30","workType":"CLASS_SUPPORT"}
```

CS: CLASS_SUPPORT (classId hợp lệ, lớp READY hoặc RUNNING), OPERATIONS_SUPPORT, FRONT_DESK. Sale: SALES_SHIFT, LEAD_FOLLOWUP, CONSULTATION.

Yêu cầu lịch rảnh APPROVED bao phủ toàn bộ khoảng giờ, không trùng ca nhân sự/lớp. Giao dịch khóa bản ghi nhân sự trước khi phân công. Owner và người phân công được lấy từ JWT, không nhận từ FE. DELETE phân công là hủy mềm (CANCELLED). Mọi thao tác ghi audit_logs.

Lưu ý: teaching_schedules hiện chưa có cột campus/room. Trường campus chỉ dùng kiểm tra lịch rảnh, chưa được lưu. FE phải bỏ tuần mẫu cố định, map employee_id thành ID nhân sự, chuẩn hóa pending/verified sang PENDING/APPROVED. Chưa chạy được integration tests với PostgreSQL tại đây.
