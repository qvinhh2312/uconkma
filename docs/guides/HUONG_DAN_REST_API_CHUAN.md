# HUONG DAN REST API CHUAN

## 1. Muc tieu

Tai lieu nay mo ta cach goi REST API runtime dung voi code hien tai tren `master`.

Runtime hien tai:

- seed du lieu bang `engine/src/main/resources/data.sql`
- tra ve JSON response co traceability
- tra ve `decisionTrace` theo phase/policy
- ghi log ro theo pha UCON
- validate runtime state bang `DomainInvariantChecker`

## 2. Du lieu runtime co san

Khi app start, H2 in-memory se co:

- `SV001`: da dong hoc phi, da hoan thanh `CS101`
- `SV002`: chua dong hoc phi, da hoan thanh `CS101`
- `CS101_01`
- `CS102_01`

Neu goi sai ID runtime, app se tra:

```text
Student or ClassSection not found.
```

## 3. Cach chay app

```powershell
cd e:\UCON_KMA\engine
.\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
```

## 4. Kien truc runtime lien quan den REST

Request runtime di theo luong:

```text
Client
  -> RegistrationController (PEP)
  -> PolicyEngine / PolicyDecisionPoint (PDP)
  -> Repositories + Entities + Environment (PIP)
  -> ExpressionEvaluator
  -> Database / AuditLog
```

Y nghia:

- PEP nhan va chan request
- PDP loc policy theo phase/action va tra decision
- PIP cung cap thuoc tinh cho subject/object/environment
- post-update cap nhat state that sau khi request duoc permit

`ONGOING` trong project nay la `transaction-level re-check`, nghia la kiem tra lai sat luc commit, khong phai monitor lien tuc cho mot session dai.

## 5. Mau response runtime

### 5.1. Dang ky thanh cong

```json
{
  "requestId": "demo-register-success",
  "action": "REGISTER",
  "decision": "ALLOW",
  "phase": "POST",
  "studentId": "SV001",
  "classId": "CS102_01",
  "failedPolicy": null,
  "denyReason": null,
  "explanation": "Request da vuot qua PRE, ONGOING va da thuc thi POST updates thanh cong.",
  "message": "Successfully enrolled.",
  "decisionTrace": {
    "requestId": "demo-register-success",
    "action": "REGISTER",
    "decision": "ALLOW"
  }
}
```

### 5.2. Dang ky bi tu choi

```json
{
  "requestId": "demo-register-deny-tuition",
  "action": "REGISTER",
  "decision": "DENY",
  "phase": "PRE",
  "studentId": "SV002",
  "classId": "CS102_01",
  "failedPolicy": "P01_TuitionPaid_PreA0",
  "denyReason": "TUITION_NOT_PAID",
  "explanation": "Sinh vien chua hoan tat hoc phi nen request bi chan truoc khi dang ky xay ra.",
  "message": "DENIED_PREAUTH: TUITION_NOT_PAID",
  "decisionTrace": {
    "requestId": "demo-register-deny-tuition",
    "action": "REGISTER",
    "decision": "DENY"
  }
}
```

## 6. Test 1: REGISTER thanh cong

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
- `decisionTrace.phases` co day du `PRE`, `ONGOING`, `POST`

Y nghia:

- request pass `PRE`
- request pass `ONGOING`
- `POST` da cap nhat state
- response giai thich duoc request di qua nhung policy/phase nao

## 7. Test 2: REGISTER bi tu choi do hoc phi

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
- `decisionTrace.phases[0].failedPolicy = P01_TuitionPaid_PreA0`

Y nghia:

- `SV002` ton tai trong DB runtime
- request bi chan truoc khi dang ky xay ra
- khong co `Registration` moi duoc tao

## 8. Test 3: DROP thanh cong

Buoc 1:

```powershell
$body = @{
    requestId = "demo-register-before-drop"
    studentId = "SV001"
    classId = "CS102_01"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/register" `
  -Method POST `
  -Body $body `
  -ContentType "application/json" | ConvertTo-Json -Depth 6
```

Buoc 2:

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

Y nghia:

- request drop pass PRE va ONGOING
- `POST` da hoan tac state cua lan register truoc

## 9. Cac response quan trong khac

### Bad request

```json
{
  "decision": "DENY",
  "phase": "VALIDATION",
  "denyReason": "BAD_REQUEST",
  "message": "studentId and classId are required."
}
```

### Race condition

```json
{
  "decision": "DENY",
  "phase": "COMMIT",
  "denyReason": "RACE_CONDITION",
  "message": "DENIED_RACE_CONDITION: concurrent enrollment update was detected."
}
```

### Duplicate registration o tang DB

```json
{
  "decision": "DENY",
  "phase": "COMMIT",
  "denyReason": "DUPLICATE_REGISTRATION",
  "message": "DENIED_DUPLICATE_REGISTRATION: active registration already exists."
}
```

## 10. H2 console

Mo:

```text
http://localhost:8080/h2-console
```

JDBC URL:

```text
jdbc:h2:mem:ucondb
```

SQL co the dung:

```sql
select * from student where student_id = 'SV001';
select * from class_section where class_id = 'CS102_01';
select * from registration where student_id = 'SV001' and class_id = 'CS102_01';
select * from audit_log where student_id in ('SV001', 'SV002') order by id desc;
```

## 11. Endpoint snapshot de demo

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/demo/state?studentId=SV001&classId=CS102_01" `
  -Method GET | ConvertTo-Json -Depth 6
```

Endpoint nay giup nhin ro:

- state `Student`
- state `ClassSection`
- `Registration` co ton tai hay khong
- `latestAudit`
- tong so ban ghi runtime

## 12. Ghi chu

- Runtime response hien tai khong con la `String body` don le.
- Response JSON da bo sung:
  - `failedPolicy`
  - `denyReason`
  - `explanation`
  - `message`
- Day la lop traceability giua policy DSL, runtime engine, va ket qua REST tra ve.
