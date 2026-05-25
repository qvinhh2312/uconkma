# HUONG DAN CHAY CHI TIET UCON_KMA

## 1. Muc tieu

Tai lieu nay la huong dan chay cuoi cung cho nhanh `master`, dong bo voi:

- `dsl/ucon_policy.dsl`
- `xmi/ucon_policy.xmi`
- `engine/src/test/java/vn/edu/kma/ucon/engine/UconEngineApplicationTests.java`
- runtime REST API tra ve JSON co traceability

## 2. Cau truc project

```text
UCON_KMA/
|- dsl/         Grammar ANTLR, parser, transformer DSL -> XMI
|- engine/      Spring Boot app, policy engine, REST API, tests
|- metamodel/   Ecore metamodel
|- xmi/         Policy model runtime
|- docs/        Tai lieu ly thuyet va huong dan
```

## 3. Kien truc can nam khi chay

```text
PAP
  dsl/ucon_policy.dsl
  -> parser / transformer
  -> xmi/ucon_policy.xmi

PEP
  RegistrationController

PDP
  PolicyDecisionPoint + PolicyEngine

PIP
  StudentRepository, ClassSectionRepository, RegistrationRepository, AuditLogRepository

Runtime executor
  ExpressionEvaluator
```

Y nghia:

- `PEP` nhan request va chan request
- `PDP` chon policy theo phase/action va tra decision
- `PIP` cap du lieu cho subject/object/environment
- `ExpressionEvaluator` danh gia condition va thuc thi post-update

Luu y quan trong:

- `ONGOING` trong project hien tai la `transaction-level re-check`
- nghia la request duoc kiem tra lai sat luc commit
- no khong phai monitor lien tuc suot mot session dai han

## 4. Yeu cau moi truong

- Java 17+ hoac 21
- Windows PowerShell
- Maven bundled trong project

Kiem tra nhanh:

```powershell
java -version
```

## 5. Build DSL

```powershell
cd dsl
mvn clean install
```

Ket qua mong doi:

```text
BUILD SUCCESS
```

## 6. Build va test engine

```powershell
cd engine
mvn clean test
```

Ket qua mong doi:

```text
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 7. Chay theo test ID / policy ID

```powershell
cd engine
.\run.bat T01
.\run.bat P01
.\run.bat REPORT
.\run.bat ALL
```

`run.bat` dung de chung minh:

- policy nao dang duoc test
- test dang kiem tra dieu gi
- ket qua mong doi cua policy/test do

## 8. Policy UCON dang co

File nguon:

- `dsl/ucon_policy.dsl`

### PRE

- `P01_TuitionPaid_PreA0`
- `P13a_EmergencyMaintenance_PreC0`
- `P02_TransactionWindow_PreC0`
- `P03_ClassStatusOpen_PreA0`
- `P04_NotAlreadyRegistered_PreA0`
- `P16_DropOnlyIfRegistered_PreA0`
- `P05_CreditLimit_PreA0`
- `P06_Prerequisite_PreA0`
- `P07_ScheduleConflict_PreA0`

### ONGOING

- `P08_CapacityRecheck_OnA0`
- `P09_ClassStatusRecheck_OnA0`
- `P10_StudentHoldRecheck_OnA0`
- `P13_EmergencyMaintenance_OnC0`

### POST

- `P11_RegisterStateUpdate_PostA3`
- `P14_DropStateRevert_PostA3`
- `P12_AuditAndTrace_PostB3`

## 9. Runtime database

App dung:

```text
jdbc:h2:mem:ucondb
```

Runtime seed tu:

- `engine/src/main/resources/data.sql`

Cau hinh quan trong:

- `spring.jpa.defer-datasource-initialization=true`

Du lieu mau co san:

- `SV001`: hop le, da dong hoc phi, da hoan thanh `CS101`
- `SV002`: chua dong hoc phi
- `CS101_01`
- `CS102_01`

## 10. Chay Spring Boot app

```powershell
cd engine
mvn spring-boot:run
```

Ket qua mong doi:

```text
Tomcat started on port(s): 8080
Started UconEngineApplication
```

## 11. Log runtime can nhin

Console app se hien cac nhom log:

- `[REQUEST]`
- `[STATE BEFORE]`
- `[ENV PRE]`
- `[PHASE START]`
- `[POLICY CHECK]`
- `[PHASE RESULT]`
- `[POST UPDATE]`
- `[STATE AFTER]`
- `[REQUEST SUCCESS]`
- `[REQUEST DENIED]`

Day la noi thay ro request di qua cac pha UCON nhu the nao.

## 12. Test REST API

Mo them mot terminal moi.

### 12.1. REGISTER thanh cong

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

### 12.2. REGISTER bi tu choi do hoc phi

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

### 12.3. DROP thanh cong

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

## 13. Mau response runtime

### Dang ky bi tu choi

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
  "message": "DENIED_PRE: TUITION_NOT_PAID"
}
```

### Dang ky thanh cong

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
  "explanation": "Request da vuot qua PRE, ONGOING va da thuc thi POST thanh cong.",
  "message": "Successfully enrolled."
}
```

## 14. Xem snapshot state runtime

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/demo/state?studentId=SV001&classId=CS102_01" `
  -Method GET | ConvertTo-Json -Depth 6
```

Snapshot tra ve:

- `environment`
- `student`
- `classSection`
- `registration`
- `latestAudit`
- `totals`

Day la cach nhin thay doi state de demo ro hon response chuoi don le.

## 15. Chay script demo runtime

```powershell
cd engine
.\run-rest-demo.bat 1
.\run-rest-demo.bat 2
.\run-rest-demo.bat 3
.\run-rest-demo.bat all
```

Script nay tu dong:

- lay state truoc request
- gui request runtime that
- in JSON response
- lay state sau request

## 16. H2 console

Mo:

```text
http://localhost:8080/h2-console
```

JDBC URL:

```text
jdbc:h2:mem:ucondb
```

SQL de quan sat thay doi:

```sql
select * from student where student_id = 'SV001';
select * from class_section where class_id = 'CS102_01';
select * from registration where student_id = 'SV001' and class_id = 'CS102_01';
select * from audit_log where student_id in ('SV001', 'SV002') order by id desc;
```

## 17. Phan biet `run.bat P01` va `spring-boot:run`

### `.\run.bat P01`

- la test JUnit da co san
- dung de chung minh policy nao dang duoc kiem tra
- phu hop cho bao ve logic policy

### `spring-boot:run`

- la app runtime that tren localhost
- dung de chung minh he thong chay end-to-end
- phu hop cho demo tich hop REST + DB + policy engine

Khuyen nghi khi bao ve:

1. show `.\run.bat P01` hoac `.\run.bat REPORT`
2. show `spring-boot:run`
3. goi 1 request pass va 1 request deny
4. mo H2 hoac endpoint snapshot de cho thay DB thay doi

## 18. Dung app

Trong terminal dang chay app:

```text
Ctrl + C
```

Hoac tu terminal khac:

```powershell
Get-Process | Where-Object {$_.ProcessName -match "java"}
Stop-Process -Name java -Force
```

## 19. Tai lieu lien quan

- [README.md](../../README.md)
- [HUONG_DAN_REST_API_CHUAN.md](./HUONG_DAN_REST_API_CHUAN.md)
- `dsl/ucon_policy.dsl`
- `xmi/ucon_policy.xmi`
- `engine/src/main/java/vn/edu/kma/ucon/engine/pep/RegistrationController.java`
- `engine/src/main/java/vn/edu/kma/ucon/engine/pdp/PolicyEngine.java`
- `engine/src/main/java/vn/edu/kma/ucon/engine/pdp/ExpressionEvaluator.java`
