# HUONG DAN CHAY CHI TIET UCON_KMA

## 1. Muc tieu tai lieu

Tai lieu nay la ban huong dan chay cuoi cung cho project `UCON_KMA`, da duoc dong bo voi:

- code hien tai tren nhanh `master`
- bo policy trong `dsl/ucon_policy.dsl`
- bo test trong `engine/src/test/java/.../UconEngineApplicationTests.java`
- runtime H2 in-memory da co seed data qua `engine/src/main/resources/data.sql`

Tai lieu nay thay cho cac huong dan cu bi lech runtime hoac loi ma hoa.

---

## 2. Cau truc project

```text
UCON_KMA/
├─ dsl/          Parser va DSL policy
├─ engine/       Spring Boot app + policy engine + tests
├─ docs/         Tai lieu ly thuyet
├─ metamodel/    Ecore metamodel
├─ xmi/          Policy model dang XMI
├─ HUONG_DAN_REST_API_CHUAN.md
└─ HƯỚNG_DẪN_CHẠY_CHI_TIẾT.md
```

---

## 3. Yeu cau moi truong

- Java 17+ hoac 21
- Windows PowerShell
- Maven bundled trong project:
  - `dsl/apache-maven-3.9.6/bin/mvn.cmd`
  - `engine/apache-maven-3.9.6/bin/mvn.cmd`

Kiem tra nhanh:

```powershell
java -version
```

---

## 4. Build DSL

```powershell
cd e:\UCON_KMA\dsl
.\apache-maven-3.9.6\bin\mvn.cmd clean install
```

Ket qua mong doi:

```text
BUILD SUCCESS
```

Y nghia:

- ANTLR generate parser tu `UconPolicy.g4`
- compile module DSL
- tao artifact parser

---

## 5. Build va test engine

```powershell
cd e:\UCON_KMA\engine
.\apache-maven-3.9.6\bin\mvn.cmd clean test
```

Ket qua mong doi:

```text
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Neu muon chay nhanh theo test ID hoac policy ID:

```powershell
cd e:\UCON_KMA\engine
.\run.bat T01
.\run.bat P01
.\run.bat REPORT
.\run.bat ALL
```

---

## 6. Cach doc bo test

### 6.1. Test case goc

Bo test chinh nam o:

- `engine/src/test/java/vn/edu/kma/ucon/engine/UconEngineApplicationTests.java`

Co 16 test:

- `T01` den `T12`: business flow va UCON flow
- `T13` den `T16`: validator va startup trust

### 6.2. Mapping policy -> test

Script:

- `engine/run-test.ps1`

giu metadata cho tung test:

- title
- muc tieu
- pha UCON
- policy lien quan
- running checks
- ket qua mong doi

Vi du:

- `P01` -> `T02`
- `P02` -> `T03`
- `P03` -> `T04`
- `P08` -> `T10`
- `P14` -> `T12`

### 6.3. Bao cao tong hop tren PowerShell

```powershell
cd e:\UCON_KMA\engine
.\run.bat REPORT
```

Report hien:

- Bang 1: test case goc
- Bang 2: policy -> test mapping
- Bang 3: noi dung kiem thu day du cho tung policy

---

## 7. Policy UCON dang co

File nguon:

- `dsl/ucon_policy.dsl`

### PRE_AUTHORIZATION

- `P01_TuitionPaid_Pre`
- `P13a_EmergencyMaintenance_Pre`
- `P02_TransactionWindow_Pre`
- `P03_ClassStatusOpen_Pre`
- `P04_NotAlreadyRegistered_Pre`
- `P16_DropOnlyIfRegistered_Pre`
- `P05_CreditLimit_Pre`
- `P06_Prerequisite_Pre`
- `P07_ScheduleConflict_Pre`

### ONGOING_AUTHORIZATION

- `P08_CapacityRecheck_On`
- `P09_ClassStatusRecheck_On`
- `P10_StudentHoldRecheck_On`
- `P13_EmergencyMaintenance_On`

### POST_UPDATE

- `P11_RegisterStateUpdate_Post`
- `P14_DropStateRevert_Post`
- `P12_AuditAndTrace_Post`

---

## 8. Runtime database

Ung dung dung:

```text
jdbc:h2:mem:ucondb
```

Cau hinh nam trong:

- `engine/src/main/resources/application.properties`

Dong quan trong:

```properties
spring.jpa.defer-datasource-initialization=true
```

Dong nay dam bao `data.sql` duoc nap sau khi Hibernate tao bang.

### Seed data runtime

File:

- `engine/src/main/resources/data.sql`

Du lieu co san khi app khoi dong:

- Students:
  - `SV001`: hop le, da hoc `CS101`, da dong hoc phi
  - `SV002`: chua dong hoc phi
- Courses:
  - `CS101`
  - `CS102`
- Class sections:
  - `CS101_01`
  - `CS102_01`

Neu khong co `data.sql`, REST API se loi:

```text
Student or ClassSection not found.
```

---

## 9. Chay Spring Boot app

```powershell
cd e:\UCON_KMA\engine
.\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
```

Ket qua mong doi:

```text
Tomcat started on port(s): 8080
Started UconEngineApplication
```

Khi app dang chay, console khong chi hien `Successfully enrolled.` hoac `Successfully dropped.` trong phan response.
App hien tai da log them chi tiet theo tung request va tung pha UCON, vi du:

```text
[REQUEST] action=REGISTER requestId=... studentId=SV001 classId=CS102_01
[STATE BEFORE] student={...} class={...}
[ENV PRE] {phase=NORMAL,currentDateTime=2026-03-27,openTime=2026-01-01,closeTime=2026-12-31,semester=2026_FALL,isMaintenance=false}
[PHASE START] phase=PRE_AUTHORIZATION action=REGISTER requestId=... policies=8
[POLICY CHECK] phase=PRE_AUTHORIZATION policy=P01_TuitionPaid_Pre effect=PERMIT matched=true denyReason=TUITION_NOT_PAID
[PHASE RESULT] phase=PRE_AUTHORIZATION permit=true failedCode=null
[ENV ONGOING] {...}
[PHASE RESULT] phase=ONGOING_AUTHORIZATION permit=true failedCode=null
[POST UPDATE] mode=FULL action=REGISTER requestId=... policy=P11_RegisterStateUpdate_Post statements=6
[STATE AFTER] student={...} class={...}
[REQUEST SUCCESS] action=REGISTER requestId=... decision=ALLOW response="Successfully enrolled."
```

Neu request bi tu choi, log se hien ro:

```text
[PHASE RESULT] phase=PRE_AUTHORIZATION permit=false failedCode=TUITION_NOT_PAID
[REQUEST DENIED] action=REGISTER phase=PRE_AUTHORIZATION requestId=... failedCode=TUITION_NOT_PAID
```

Sau khi app len, co the mo:

```text
http://localhost:8080/h2-console
```

JDBC URL:

```text
jdbc:h2:mem:ucondb
```

### 9.1. Neu muon xem log gon hon tren PowerShell

Console mac dinh se co ca:

- log Spring Boot
- log Hibernate SQL
- log UCON chi tiet vua bo sung

Neu ban redirect output cua `spring-boot:run` vao file, co the loc ra cac dong quan trong de demo bang:

```powershell
Get-Content .\target\spring-demo.log -Wait |
  Select-String "\[REQUEST\]|\[STATE|\[ENV|\[PHASE|\[POLICY|\[POST UPDATE\]"
```

Neu ban chay app truc tiep trong cung console, thi chi can nhin cac dong bat dau bang:

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

---

## 10. Test REST API chuan

Mo terminal moi de khong dong app dang chay.

### 10.1. Dang ky thanh cong

```powershell
$body = @{
    studentId = "SV001"
    classId = "CS102_01"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/register" `
  -Method POST `
  -Body $body `
  -ContentType "application/json"
```

Ket qua mong doi:

```text
Successfully enrolled.
```

### 10.2. Dang ky bi tu choi do chua dong hoc phi

```powershell
$body = @{
    studentId = "SV002"
    classId = "CS102_01"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/register" `
  -Method POST `
  -Body $body `
  -ContentType "application/json"
```

Ket qua mong doi:

```text
DENIED_PREAUTH: TUITION_NOT_PAID
```

### 10.3. Dang ky trung

Goi 2 lan voi cung `SV001` va `CS102_01`:

```powershell
$body = @{
    studentId = "SV001"
    classId = "CS102_01"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/register" `
  -Method POST `
  -Body $body `
  -ContentType "application/json"

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/register" `
  -Method POST `
  -Body $body `
  -ContentType "application/json"
```

Ket qua lan 2 mong doi:

```text
DENIED_PREAUTH: ALREADY_REGISTERED
```

Hoac neu race/collision o tang DB:

```text
DENIED_DUPLICATE_REGISTRATION: active registration already exists.
```

### 10.4. Huy dang ky

Dang ky truoc, roi goi:

```powershell
$body = @{
    studentId = "SV001"
    classId = "CS102_01"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/drop" `
  -Method POST `
  -Body $body `
  -ContentType "application/json"
```

Ket qua mong doi:

```text
Successfully dropped.
```

### 10.5. Vi du sai thuong gap: dung nham ID runtime

Doan sau la vi du sai:

```powershell
$body = @{
    studentId = "2"
    classId = "102"
} | ConvertTo-Json
```

Ly do sai:

- runtime DB seed bang `data.sql` khong co student `"2"`
- runtime DB seed bang `data.sql` khong co class `"102"`
- runtime chi co:
  - `SV001`, `SV002`
  - `CS101_01`, `CS102_01`

Neu goi sai ID nhu tren, app se tra:

```text
Student or ClassSection not found.
```

Vi vay, de test `P01` bang REST API dung runtime that, phai dung:

```powershell
$body = @{
    studentId = "SV002"
    classId = "CS102_01"
} | ConvertTo-Json
```

### 10.6. Them mot so vi du REST chay duoc that

#### Vi du A: request body thieu field

```powershell
$body = @{
    studentId = "SV001"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/register" `
  -Method POST `
  -Body $body `
  -ContentType "application/json"
```

Ket qua mong doi:

```text
studentId and classId are required.
```

#### Vi du B: dang ky trung sau khi da dang ky thanh cong

```powershell
$body = @{
    studentId = "SV001"
    classId = "CS102_01"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/register" `
  -Method POST `
  -Body $body `
  -ContentType "application/json"

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/register" `
  -Method POST `
  -Body $body `
  -ContentType "application/json"
```

Ket qua mong doi o lan goi thu 2:

```text
DENIED_PREAUTH: ALREADY_REGISTERED
```

Hoac trong tinh huong collision o DB:

```text
DENIED_DUPLICATE_REGISTRATION: active registration already exists.
```

#### Vi du C: huy lop khi chua co dang ky

```powershell
$body = @{
    studentId = "SV001"
    classId = "CS102_01"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/drop" `
  -Method POST `
  -Body $body `
  -ContentType "application/json"
```

Ket qua mong doi neu truoc do chua REGISTER:

```text
DENIED_PREAUTH: NOT_REGISTERED
```

### 10.7. Nhung policy nao khong nen demo bang REST truc tiep

Co nhung policy rat phu hop de demo bang `run.bat`, nhung khong phai luc nao cung hop de demo bang REST:

- `P02`:
  - vi `Environment` dang duoc build co dinh trong controller
  - muon thay doi transaction window phai sua environment hoac sua code
- `P08`:
  - can co 2 request dong thoi tranh 1 suat cuoi cung
- `P09`:
  - can thay doi status lop giua PRE va ONGOING
- `P13`:
  - can bat maintenance dung thoi diem giua request

Voi cac policy nhu tren, `run.bat Pxx` la cach demo ro rang va on dinh hon REST thu cong.

### 10.8. Script tu dong ban 3 request mau

Neu muon demo runtime that ro rang hon, co the dung script:

```powershell
cd e:\UCON_KMA\engine
.\run-rest-demo.bat all
```

Hoac chay tung luong:

```powershell
.\run-rest-demo.bat 1
.\run-rest-demo.bat 2
.\run-rest-demo.bat 3
```

Y nghia:

- `1`: REGISTER thanh cong
- `2`: REGISTER bi tu choi do `TUITION_NOT_PAID`
- `3`: DROP thanh cong
- `all`: chay lien tiep ca 3 case

Script nay se tu dong:

- goi endpoint `GET /api/demo/state` truoc request
- gui request REST that
- in response HTTP
- goi lai `GET /api/demo/state` sau request

Nhu vay ban nhin thay ro:

- response tra ve
- state cua `Student`
- state cua `ClassSection`
- `Registration` co duoc tao/xoa hay khong
- `AuditLog` moi nhat
- tong so ban ghi dang co

### 10.9. Endpoint xem state DB de demo

Endpoint:

```text
GET /api/demo/state?studentId=SV001&classId=CS102_01
```

Vi du:

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/demo/state?studentId=SV001&classId=CS102_01" `
  -Method GET | ConvertTo-Json -Depth 6
```

No se tra ve JSON gom:

- `environment`
- `student`
- `classSection`
- `registration`
- `latestAudit`
- `totals`

Day la noi de nhin ro thay doi DB khi demo, thay vi chi nhin chuoi:

- `Successfully enrolled.`
- `Successfully dropped.`
- `DENIED_PREAUTH: ...`

### 10.10. Neu muon xem thang DB trong H2 Console

Mo:

```text
http://localhost:8080/h2-console
```

JDBC URL:

```text
jdbc:h2:mem:ucondb
```

Co the chay cac cau SQL sau:

```sql
select * from student;
select * from class_section;
select * from registration;
select * from audit_log;
```

Neu muon tap trung vao case demo `SV001` va `CS102_01`:

```sql
select * from student where student_id = 'SV001';
select * from class_section where class_id = 'CS102_01';
select * from registration where student_id = 'SV001' and class_id = 'CS102_01';
select * from audit_log where student_id = 'SV001' and class_id = 'CS102_01' order by id desc;
```

Day la noi thay DB doi ro nhat:

- `student.current_credits`
- `student.tuition_debt`
- `student.registered_class_ids`
- `student.registered_schedule_slots`
- `class_section.enrolled`
- bang `registration`
- bang `audit_log`

---

## 11. Response thuc te cua API

### `/api/register`

- Thanh cong:

```text
Successfully enrolled.
```

- That bai PRE:

```text
DENIED_PREAUTH: <FAILED_CODE>
```

- That bai ONGOING:

```text
DENIED_ONGOING: <FAILED_CODE>
```

- Race condition:

```text
DENIED_RACE_CONDITION: concurrent enrollment update was detected.
```

- Duplicate registration:

```text
DENIED_DUPLICATE_REGISTRATION: active registration already exists.
```

### `/api/drop`

- Thanh cong:

```text
Successfully dropped.
```

- That bai PRE:

```text
DENIED_PREAUTH: <FAILED_CODE>
```

- That bai ONGOING:

```text
DENIED_ONGOING: <FAILED_CODE>
```

### Cac loi 400

```text
Request body is required.
studentId and classId are required.
Student or ClassSection not found.
```

---

## 12. Quy trinh xu ly trong code

Controller:

- `engine/src/main/java/vn/edu/kma/ucon/engine/pep/RegistrationController.java`

Flow:

```text
REQUEST
  -> validate body
  -> load Student va ClassSection
  -> build Environment
  -> PRE_AUTHORIZATION
  -> refresh entity
  -> ONGOING_AUTHORIZATION
  -> POST_UPDATE
  -> save entity
  -> RESPONSE
```

Policy engine:

- `PolicyDecisionPoint`
- `PolicyEngine`
- `ExpressionEvaluator`

Policy source of truth:

- `xmi/ucon_policy.xmi`
- duoc sinh tu `dsl/ucon_policy.dsl`

---

## 13. Phan biet `run.bat P01` va chay `spring-boot:run`

Day la 2 cach kiem chung khac nhau, khong trung nhau.

### 13.1. `.\run.bat P01` la gi

Lenh:

```powershell
cd e:\UCON_KMA\engine
.\run.bat P01
```

Y nghia:

- day la cach chay test JUnit da duoc viet san
- script map `P01` -> `T02` -> `test02_RegisterDenied_WhenTuitionNotPaid`
- test tu dong tao du lieu, goi controller/test engine, assert ket qua, va in metadata mo ta test

No kiem tra:

- policy `P01` co chan request hay khong
- failed code co dung la `TUITION_NOT_PAID` hay khong
- co tao `Registration` sai hay khong
- audit log co ghi `DENY` hay khong

No phu hop de:

- chung minh logic policy
- chung minh test case chuan
- bao ve ve mat ky thuat va tinh dung dan

### 13.2. `spring-boot:run` la gi

Lenh:

```powershell
cd e:\UCON_KMA\engine
.\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
```

Y nghia:

- day la cach chay ung dung that
- app mo REST API tren port `8080`
- ban tu gui HTTP request bang PowerShell hoac Postman

No kiem tra:

- controller co nhan request that hay khong
- database runtime co du seed data hay khong
- luong `register/drop` co chay end-to-end hay khong
- response HTTP tra ve co dung khong

No phu hop de:

- demo he thong dang chay
- show tich hop controller + DB + policy engine
- mo ta kha nang trien khai thuc te

### 13.3. Khac nhau o dau

`run.bat P01`:

- la test tu dong
- du lieu va assert da duoc co dinh
- tap trung vao tinh dung dan cua policy/test case
- de lap lai va de bao ve ky thuat

`spring-boot:run` + `Invoke-RestMethod`:

- la chay ung dung that
- nguoi dung tu gui request
- tap trung vao luong tich hop end-to-end
- de demo he thong thuc thi ngoai test framework

### 13.4. Co can ca 2 khong

Co.

Can `run.bat P01` vi:

- no la bang chung kiem thu chinh xac cho tung policy
- no cho thay test case duoc dac ta ro rang
- no giai thich duoc "policy nay dang test dieu gi"

Can `spring-boot:run` vi:

- no cho thay he thong khong chi dung trong test ma con chay duoc that
- no chung minh REST API, DB runtime, va policy engine da noi voi nhau

### 13.5. Cai nao nen show cuoi cung

Neu chi duoc chon mot kieu de bao ve logic policy:

- uu tien `.\run.bat P01`, `.\run.bat P05`, `.\run.bat P10`, ...

vi:

- gon
- on dinh
- mo ta ro test dang kiem tra gi
- de gan voi tung policy trong DSL

Neu muon show mot man "he thong chay that":

- mo app bang `spring-boot:run`
- sau do goi 1 request PASS va 1 request DENY bang REST

Khuyen nghi cach show cuoi cung:

1. Show `run.bat P01` hoac `run.bat REPORT` de giai thich policy va test mapping
2. Show `spring-boot:run` + 1 request `REGISTER` thanh cong
3. Show tiep 1 request `REGISTER` bi tu choi, vi du `SV002` -> `TUITION_NOT_PAID`

Nhu vay ban co ca:

- chung cu kiem thu
- va demo runtime that

---

## 14. Cach tao va dac ta policy UCON bang DSL

File:

- `dsl/ucon_policy.dsl`

Mot policy co dang tong quat:

```text
policy <PolicyName> {
    type: PRE_AUTHORIZATION | ONGOING_AUTHORIZATION | POST_UPDATE
    targetAction: REGISTER | DROP | ANY
    effect: PERMIT
    priority: <so>
    description: "<mo ta>"
    subjectType: "Student"
    objectType: "ClassSection"
    ruleFamily: AUTHORIZATION | MUTATION | TRACE
    denyReason: "<ma loi>"

    condition: <bieu thuc DSL>

    postUpdates:
       <update statements>
}
```

### Cach viet policy

1. Xac dinh bai toan nghiep vu
- vi du: "Sinh vien chua dong hoc phi thi khong duoc dang ky"

2. Chon pha UCON
- truoc hanh dong -> `PRE_AUTHORIZATION`
- gan commit -> `ONGOING_AUTHORIZATION`
- sau khi permit -> `POST_UPDATE`

3. Chon target action
- `REGISTER`
- `DROP`
- `ANY`

4. Viet `condition`
- vi du:

```text
condition: subject.tuitionPaid == true
```

5. Neu la `POST_UPDATE`, viet `postUpdates`
- vi du:

```text
postUpdates:
   object.enrolled ADD_ASSIGN 1
   subject.currentCredits ADD_ASSIGN object.course.credits
```

### Vi du cu the

```text
policy P01_TuitionPaid_Pre {
    type: PRE_AUTHORIZATION
    targetAction: REGISTER
    effect: PERMIT
    priority: 100
    description: "Chi cho phep SV da hoan tat hoc phi"
    subjectType: "Student"
    objectType: "ClassSection"
    ruleFamily: AUTHORIZATION
    denyReason: "TUITION_NOT_PAID"

    condition: subject.tuitionPaid == true
}
```

Y nghia:

- neu `subject.tuitionPaid` la `false`
- request bi tu choi
- failed code se la `TUITION_NOT_PAID`

---

## 14. Ket qua xac minh thuc te cua ban chot

Ban chot hien tai da duoc xac minh:

- `mvn test` pass:

```text
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- `spring-boot:run` start thanh cong
- REST runtime pass voi:
  - `SV001` -> `Successfully enrolled.`
- REST runtime deny dung voi:
  - `SV002` -> `DENIED_PREAUTH: TUITION_NOT_PAID`

Bug da duoc sua:

- app runtime truoc day khong co seed data, gay loi:

```text
Student or ClassSection not found.
```

Da fix bang:

- `engine/src/main/resources/data.sql`
- `spring.jpa.defer-datasource-initialization=true`

---

## 15. Dung app

Trong terminal dang chay app:

```text
Ctrl + C
```

Hoac tu terminal khac:

```powershell
Get-Process | Where-Object {$_.ProcessName -match "java"}
Stop-Process -Name java -Force
```

---

## 16. Tai lieu lien quan

- `HUONG_DAN_REST_API_CHUAN.md`
- `dsl/ucon_policy.dsl`
- `xmi/ucon_policy.xmi`
- `engine/src/test/java/vn/edu/kma/ucon/engine/UconEngineApplicationTests.java`
- `engine/run-test.ps1`
