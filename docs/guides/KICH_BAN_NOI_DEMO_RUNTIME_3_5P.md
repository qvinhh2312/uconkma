Chuẩn bị

Mở terminal 1:

```powershell
cd e:\UCON_KMA\engine
.\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
```

Chờ app lên, thấy các dòng kiểu:

```text
Tomcat started on port(s): 8080
Started UconEngineApplication
```

Mở terminal 2 để gửi request và xem state.

Case 1: REGISTER thành công

Bước 1. Xem state trước khi gửi request

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/demo/state?studentId=SV001&classId=CS102_01" `
  -Method GET | ConvertTo-Json -Depth 6
```

Điểm cần nhìn:

- `student.currentCredits = 0`
- `student.tuitionDebt = 0`
- `student.registeredClassIds = <empty>`
- `classSection.enrolled = 4`
- `registration.exists = false`
- `latestAudit.exists = false`

Bước 2. Gửi request REGISTER

```powershell
$body = @{
    requestId = "demo-register-success"
    studentId = "SV001"
    classId = "CS102_01"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/register" `
  -Method POST `
  -Body $body `
  -ContentType "application/json"
```

Kết quả mong đợi:

```text
Successfully enrolled.
```

Bước 3. Xem log runtime ở terminal 1

Các dòng nên chỉ:

- `[REQUEST]`
- `[STATE BEFORE]`
- `[ENV PRE]`
- `[PHASE START]`
- `[POLICY CHECK]`
- `[PHASE RESULT]`
- `[POST UPDATE]`
- `[STATE AFTER]`
- `[REQUEST SUCCESS]`

Ý nghĩa:

- `PRE` pass
- `ONGOING` pass
- `POST_UPDATE` chạy

Bước 4. Xem state sau request

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/demo/state?studentId=SV001&classId=CS102_01" `
  -Method GET | ConvertTo-Json -Depth 6
```

Điểm cần nhìn:

- `student.currentCredits = 4`
- `student.tuitionDebt = 4000000`
- `student.registeredClassIds = CS102_01`
- `student.registeredScheduleSlots = T3_1-3,T5_4-6`
- `classSection.enrolled = 5`
- `registration.exists = true`
- `latestAudit.decision = ALLOW`
- `latestAudit.failedPolicyCodes = NONE`

Bước 5. Check DB trong H2

Mở:

```text
http://localhost:8080/h2-console
```

JDBC:

```text
jdbc:h2:mem:ucondb
```

Chạy:

```sql
select * from student where student_id = 'SV001';
select * from class_section where class_id = 'CS102_01';
select * from registration where student_id = 'SV001' and class_id = 'CS102_01';
select * from audit_log where request_id = 'demo-register-success';
```

Điểm cần trình bày:

- `student.current_credits` tăng
- `student.tuition_debt` tăng
- có bản ghi trong `registration`
- có bản ghi `ALLOW` trong `audit_log`

Case 2: REGISTER bị từ chối do chưa đóng học phí

Bước 1. Xem state trước request

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/demo/state?studentId=SV002&classId=CS102_01" `
  -Method GET | ConvertTo-Json -Depth 6
```

Điểm cần nhìn:

- `student.tuitionPaid = false`
- `student.currentCredits = 0`
- `registration.exists = false`

Bước 2. Gửi request REGISTER

```powershell
$body = @{
    requestId = "demo-register-deny-tuition"
    studentId = "SV002"
    classId = "CS102_01"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/register" `
  -Method POST `
  -Body $body `
  -ContentType "application/json"
```

Nếu PowerShell quăng exception vì 403, dùng:

```powershell
try {
    Invoke-RestMethod `
      -Uri "http://localhost:8080/api/register" `
      -Method POST `
      -Body $body `
      -ContentType "application/json"
} catch {
    $_.ErrorDetails.Message
}
```

Kết quả mong đợi:

```text
DENIED_PREAUTH: TUITION_NOT_PAID
```

Bước 3. Xem log runtime

Điểm cần nhìn:

- `[PHASE RESULT] phase=PRE_AUTHORIZATION permit=false failedCode=TUITION_NOT_PAID`
- `[REQUEST DENIED] action=REGISTER phase=PRE_AUTHORIZATION ...`

Ý nghĩa:

- request bị chặn ở `PRE`
- hành động chưa được phép xảy ra

Bước 4. Xem state sau request

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/demo/state?studentId=SV002&classId=CS102_01" `
  -Method GET | ConvertTo-Json -Depth 6
```

Điểm cần nhìn:

- `student.currentCredits` không đổi
- `student.tuitionDebt` không đổi
- `classSection.enrolled` không đổi
- `registration.exists = false`
- `latestAudit.decision = DENY`
- `latestAudit.failedPolicyCodes = TUITION_NOT_PAID`

Bước 5. Check DB trong H2

```sql
select * from student where student_id = 'SV002';
select * from class_section where class_id = 'CS102_01';
select * from registration where student_id = 'SV002' and class_id = 'CS102_01';
select * from audit_log where request_id = 'demo-register-deny-tuition';
```

Điểm cần trình bày:

- không có `registration`
- không có mutation trên `student` hay `class_section`
- nhưng vẫn có `audit_log` để truy vết

Case 3: DROP thành công

Case này phải làm sau khi `SV001` đã đăng ký thành công, hoặc bạn chạy lại Case 1 trước.

Bước 1. Xem state trước DROP

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/demo/state?studentId=SV001&classId=CS102_01" `
  -Method GET | ConvertTo-Json -Depth 6
```

Điểm cần nhìn:

- `student.currentCredits = 4`
- `student.tuitionDebt = 4000000`
- `registration.exists = true`
- `classSection.enrolled = 5`

Bước 2. Gửi request DROP

```powershell
$body = @{
    requestId = "demo-drop-success"
    studentId = "SV001"
    classId = "CS102_01"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/drop" `
  -Method POST `
  -Body $body `
  -ContentType "application/json"
```

Kết quả mong đợi:

```text
Successfully dropped.
```

Bước 3. Xem log runtime

Điểm cần nhìn:

- `PRE` pass
- `ONGOING` pass
- `POST_UPDATE` chạy
- `[REQUEST SUCCESS] action=DROP ...`

Bước 4. Xem state sau DROP

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/demo/state?studentId=SV001&classId=CS102_01" `
  -Method GET | ConvertTo-Json -Depth 6
```

Điểm cần nhìn:

- `student.currentCredits = 0`
- `student.tuitionDebt = 0`
- `student.registeredClassIds = <empty>`
- `student.registeredScheduleSlots = <empty>`
- `classSection.enrolled = 4`
- `registration.exists = false`
- `latestAudit.decision = ALLOW`

Bước 5. Check DB trong H2

```sql
select * from student where student_id = 'SV001';
select * from class_section where class_id = 'CS102_01';
select * from registration where student_id = 'SV001' and class_id = 'CS102_01';
select * from audit_log where request_id = 'demo-drop-success';
```

Điểm cần trình bày:

- `registration` đã bị xóa
- `current_credits` giảm
- `tuition_debt` giảm
- `enrolled` giảm
- có `audit_log` mới cho request `DROP`

Cách demo nhanh nhất bằng script

Nếu không muốn gõ từng request:

```powershell
cd e:\UCON_KMA\engine
.\run-rest-demo.bat all
```

Nó sẽ tự:

- show state trước
- bắn request
- show response
- show state sau

Cho cả 3 case:

- `REGISTER` thành công
- `REGISTER` bị deny do học phí
- `DROP` thành công

Nên nói gì khi trình bày

Case 1

- request được cho phép
- qua đủ `PRE`, `ONGOING`, `POST_UPDATE`
- state hệ thống thay đổi
- DB có `registration` và `audit`

Case 2

- request bị chặn ở `PRE_AUTHORIZATION`
- không có mutation state
- nhưng `audit` vẫn ghi lại để truy vết

Case 3

- `DROP` không chỉ trả success
- mà còn hoàn tác toàn bộ state liên quan
- thể hiện UCON có `post-update` thật sự

Kết luận nên show

Nếu demo đầy đủ, mỗi case nên có:

- request
- response
- log UCON
- snapshot before/after
- H2 query xác nhận DB thật
