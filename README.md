# Backend - Spring Boot + PostgreSQL + JWT

## Yêu cầu
- Java 17+
- Maven 3.9+
- Docker (để chạy PostgreSQL nhanh) hoặc PostgreSQL cài sẵn

## Chạy database (PostgreSQL) bằng Docker
```bash
docker compose up -d
```
Database sẽ chạy ở `localhost:5432`, user/pass mặc định: `postgres` / `postgres`, database `demo_db`.

> Nếu bạn muốn dùng MySQL thay vì PostgreSQL: mở `pom.xml` bật dependency `mysql-connector-j` (đang bị comment), và sửa `spring.datasource.url` trong `application.yml` thành `jdbc:mysql://localhost:3306/demo_db`.

## Cấu hình biến môi trường
Copy `.env.example` thành `.env` (hoặc set trực tiếp trong IDE Run Configuration):
```bash
cp .env.example .env
```
Nhớ đổi `JWT_SECRET` thành chuỗi random dài khi deploy thật.

## Chạy ứng dụng
```bash
mvn spring-boot:run
```
Server chạy ở `http://localhost:8080`.

## Các API có sẵn

| Method | Endpoint | Auth | Mô tả |
|---|---|---|---|
| POST | `/api/auth/register` | Không | Đăng ký user mới |
| POST | `/api/auth/login` | Không | Đăng nhập, trả về JWT |
| GET | `/api/public/ping` | Không | Test server sống |
| GET | `/api/me` | **Có** (Bearer token) | Lấy thông tin user hiện tại |

### Ví dụ đăng ký
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","email":"admin@test.com","password":"123456"}'
```

### Ví dụ đăng nhập
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```
Response trả về `token` — dùng token này gắn vào header `Authorization: Bearer <token>` cho các API cần đăng nhập.

### Gọi API cần token
```bash
curl http://localhost:8080/api/me \
  -H "Authorization: Bearer <token_vua_nhan_duoc>"
```

## Cấu trúc thư mục
```
src/main/java/com/example/demo/
├── config/          # SecurityConfig (CORS, filter chain)
├── security/         # JwtUtil, JwtAuthFilter
├── entity/           # User (JPA entity)
├── repository/        # UserRepository
├── dto/               # Request/Response DTOs
├── service/           # AuthService, CustomUserDetailsService
├── controller/        # AuthController, TestController
└── exception/          # ApiException, GlobalExceptionHandler
```

## Kết nối với FE (React + Vite)
Trong file `.env` của FE, set:
```
VITE_API_URL=http://localhost:8080/api
```
CORS đã được cấu hình sẵn cho phép `http://localhost:5173` (Vite dev server) trong `SecurityConfig.java`.
