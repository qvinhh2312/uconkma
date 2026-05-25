Chuan bi

Mo terminal 1:

```powershell
cd engine
mvn spring-boot:run
```

Cho app len, thay cac dong:

```text
Tomcat started on port(s): 8080
Started UconEngineApplication
```

Mo terminal 2 de gui request va xem state.

Case 1: REGISTER thanh cong

Buoc 1. Xem state truoc khi gui request

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/demo/state?studentId=SV001&classId=CS102_01" `
  -Method GET | ConvertTo-Json -Depth 6
```

Diem can nhin:

- `student.currentCredits = 0`
- `student.tuitionDebt = 0`
- `student.registeredClassIds = <empty>`
- `classSection.enrolled = 4`
- `registration.exists = false`
- `latestAudit.exists = false`

Buoc 2. Gui request REGISTER

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
  -ContentType "application/json" | ConvertTo-Json -Depth 6
```

Ket qua mong doi:

- `decision = ALLOW`
- `phase = POST`
- `message = Successfully enrolled.`

Buoc 3. Xem log runtime o terminal 1

Can chi vao:

- `[REQUEST]`
- `[STATE BEFORE]`
- `[ENV PRE]`
- `[PHASE START]`
- `[POLICY CHECK]`
- `[PHASE RESULT]`
- `[POST UPDATE]`
- `[STATE AFTER]`
- `[REQUEST SUCCESS]`

Y nghia:

- `PRE` pass
- `ONGOING` pass
- `POST` chay

Buoc 4. Xem state sau request

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/demo/state?studentId=SV001&classId=CS102_01" `
  -Method GET | ConvertTo-Json -Depth 6
```

Diem can nhin:

- `student.currentCredits = 4`
- `student.tuitionDebt = 4000000`
- `student.registeredClassIds = CS102_01`
- `student.registeredScheduleSlots = T3_1-3,T5_4-6`
- `classSection.enrolled = 5`
- `registration.exists = true`
- `latestAudit.decision = ALLOW`
- `latestAudit.failedPolicyCodes = NONE`

Buoc 5. Check DB trong H2

Mo:

```text
http://localhost:8080/h2-console
```

JDBC:

```text
jdbc:h2:mem:ucondb
```

Chay:

```sql
select * from student where student_id = 'SV001';
select * from class_section where class_id = 'CS102_01';
select * from registration where student_id = 'SV001' and class_id = 'CS102_01';
select * from audit_log where request_id = 'demo-register-success';
```

Diem can trinh bay:

- `student.current_credits` tang
- `student.tuition_debt` tang
- co ban ghi trong `registration`
- co ban ghi `ALLOW` trong `audit_log`

Case 2: REGISTER bi tu choi do chua dong hoc phi

Buoc 1. Xem state truoc request

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/demo/state?studentId=SV002&classId=CS102_01" `
  -Method GET | ConvertTo-Json -Depth 6
```

Diem can nhin:

- `student.tuitionPaid = false`
- `student.currentCredits = 0`
- `registration.exists = false`

Buoc 2. Gui request REGISTER

```powershell
$body = @{
    requestId = "demo-register-deny-tuition"
    studentId = "SV002"
    classId = "CS102_01"
} | ConvertTo-Json

try {
    Invoke-RestMethod `
      -Uri "http://localhost:8080/api/register" `
      -Method POST `
      -Body $body `
      -ContentType "application/json" | ConvertTo-Json -Depth 6
} catch {
    $_.ErrorDetails.Message
}
```

Ket qua mong doi:

- `decision = DENY`
- `phase = PRE`
- `failedPolicy = P01_TuitionPaid_PreA0`
- `denyReason = TUITION_NOT_PAID`

Buoc 3. Xem log runtime

Diem can nhin:

- `[PHASE RESULT] phase=PRE permit=false failedCode=TUITION_NOT_PAID`
- `[REQUEST DENIED] action=REGISTER phase=PRE ...`

Y nghia:

- request bi chan o `PRE`
- hanh dong chua duoc phep xay ra

Buoc 4. Xem state sau request

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/demo/state?studentId=SV002&classId=CS102_01" `
  -Method GET | ConvertTo-Json -Depth 6
```

Diem can nhin:

- `student.currentCredits` khong doi
- `student.tuitionDebt` khong doi
- `classSection.enrolled` khong doi
- `registration.exists = false`
- `latestAudit.decision = DENY`
- `latestAudit.failedPolicyCodes = TUITION_NOT_PAID`

Buoc 5. Check DB trong H2

```sql
select * from student where student_id = 'SV002';
select * from class_section where class_id = 'CS102_01';
select * from registration where student_id = 'SV002' and class_id = 'CS102_01';
select * from audit_log where request_id = 'demo-register-deny-tuition';
```

Diem can trinh bay:

- khong co `registration`
- khong co mutation tren `student` hay `class_section`
- nhung van co `audit_log` de truy vet

Case 3: DROP thanh cong

Case nay phai lam sau khi `SV001` da dang ky thanh cong, hoac ban chay lai Case 1 truoc.

Buoc 1. Xem state truoc DROP

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/demo/state?studentId=SV001&classId=CS102_01" `
  -Method GET | ConvertTo-Json -Depth 6
```

Diem can nhin:

- `student.currentCredits = 4`
- `student.tuitionDebt = 4000000`
- `registration.exists = true`
- `classSection.enrolled = 5`

Buoc 2. Gui request DROP

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
  -ContentType "application/json" | ConvertTo-Json -Depth 6
```

Ket qua mong doi:

- `decision = ALLOW`
- `phase = POST`
- `message = Successfully dropped.`

Buoc 3. Xem log runtime

Diem can nhin:

- `PRE` pass
- `ONGOING` pass
- `POST` chay
- `[REQUEST SUCCESS] action=DROP ...`

Buoc 4. Xem state sau DROP

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/demo/state?studentId=SV001&classId=CS102_01" `
  -Method GET | ConvertTo-Json -Depth 6
```

Diem can nhin:

- `student.currentCredits = 0`
- `student.tuitionDebt = 0`
- `student.registeredClassIds = <empty>`
- `student.registeredScheduleSlots = <empty>`
- `classSection.enrolled = 4`
- `registration.exists = false`
- `latestAudit.decision = ALLOW`

Buoc 5. Check DB trong H2

```sql
select * from student where student_id = 'SV001';
select * from class_section where class_id = 'CS102_01';
select * from registration where student_id = 'SV001' and class_id = 'CS102_01';
select * from audit_log where request_id = 'demo-drop-success';
```

Diem can trinh bay:

- `registration` da bi xoa
- `current_credits` giam
- `tuition_debt` giam
- `enrolled` giam
- co `audit_log` moi cho request `DROP`

Cach demo nhanh nhat bang script

Neu khong muon go tung request:

```powershell
cd engine
.\run-rest-demo.bat all
```

No se tu:

- show state truoc
- ban request
- show response JSON
- show state sau

Cho ca 3 case:

- `REGISTER` thanh cong
- `REGISTER` bi deny do hoc phi
- `DROP` thanh cong

Nen noi gi khi trinh bay

Case 1

- request duoc cho phep
- qua du `PRE`, `ONGOING`, `POST`
- state he thong thay doi
- DB co `registration` va `audit`

Case 2

- request bi chan o `PRE`
- khong co mutation state
- nhung `audit` van ghi lai de truy vet

Case 3

- `DROP` khong chi tra success
- ma con hoan tac toan bo state lien quan
- the hien UCON co `post-update` that su

Ket luan nen show

Neu demo day du, moi case nen co:

- request
- response JSON
- log UCON
- snapshot before/after
- H2 query xac nhan DB that
