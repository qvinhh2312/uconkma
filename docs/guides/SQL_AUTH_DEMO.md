# SQL Auth Demo

Huong dan nay dung khi muon chay UCONKMA voi database SQL ben vung thay vi H2 in-memory.

## 1. MySQL database

Tao database va user demo bang MySQL account co quyen admin:

```powershell
mysql -u root -p < db/mysql/00-create-database.sql
```

File bootstrap:

```text
db/mysql/00-create-database.sql
```

User demo duoc tao:

```text
database: ucon_kma
username: ucon_app
password: ucon_app_123
```

Neu may co Docker, co the dung compose thay vi cai MySQL local:

```powershell
docker compose -f docker-compose.mysql.yml up -d
```

Chay backend voi profile MySQL:

```powershell
cd engine
$env:SPRING_PROFILES_ACTIVE="mysql"
$env:DB_URL="jdbc:mysql://localhost:3306/ucon_kma?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Ho_Chi_Minh"
$env:DB_USERNAME="ucon_app"
$env:DB_PASSWORD="ucon_app_123"
mvn spring-boot:run
```

Co the copy gia tri tu file:

```text
engine/.env.mysql.example
```

Neu khong set bien moi truong, backend dung default trong `application-mysql.properties`:

```text
DB_URL=jdbc:mysql://localhost:3306/ucon_kma?createDatabaseIfNotExist=true...
DB_USERNAME=root
DB_PASSWORD=
```

Sau khi backend start, Hibernate se tao/cap nhat cac bang can thiet:

- `student`
- `course`
- `class_section`
- `registration`
- `usage_session`
- `audit_log`
- `user_account`
- `student_grade`

## 2. Demo accounts

Backend tu seed cac tai khoan neu chua ton tai:

| Role | Username | Password | Student |
|---|---|---|---|
| ADMIN | `admin` | `admin123` | - |
| STUDENT | `sv001` | `student123` | `SV001` |
| STUDENT | `sv002` | `student123` | `SV002` |

## 3. Auth API

Login:

```http
POST /api/auth/login
```

```json
{
  "username": "sv001",
  "password": "student123"
}
```

Response:

```json
{
  "token": "...",
  "username": "sv001",
  "displayName": "Nguyen Van An",
  "role": "STUDENT",
  "studentId": "SV001"
}
```

Use token:

```text
Authorization: Bearer <token>
```

## 4. Role-scoped APIs

Admin:

```http
GET /api/students
GET /api/students/{studentId}
GET /api/students/{studentId}/grades
```

Student:

```http
GET /api/students/me
GET /api/students/me/grades
POST /api/register
POST /api/drop
```

Student account can only submit REGISTER/DROP for its own `studentId`. Existing no-token demo requests still work for UCON presentation tests.

## 5. Frontend

Run frontend:

```powershell
cd frontend
npm install
npm run dev
```

For Android Emulator browser:

```powershell
$env:VITE_API_BASE_URL="http://10.0.2.2:8080/api"
npm run dev -- --host 0.0.0.0
```

Open:

```text
http://localhost:5173
http://10.0.2.2:5173
```

## 6. What this adds

- Persistent SQL storage through MySQL profile.
- Login accounts with role `ADMIN` or `STUDENT`.
- Admin can view all students and grades.
- Student can view only own profile and grades.
- Student can register/drop through the existing UCON PEP/PDP workflow.
- H2 remains available for automated tests and quick local demo.
