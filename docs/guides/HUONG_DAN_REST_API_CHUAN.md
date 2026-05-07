# HUONG DAN REST API CHAY CHUAN

## Nguyen nhan loi "Student or ClassSection not found."

Loi nay xay ra vi app runtime truoc day khong seed san du lieu mau vao H2 in-memory database.

Du lieu `SV001`, `SV002`, `CS101`, `CS102`, `CS101_01`, `CS102_01` chi ton tai trong bo test JUnit, nhung tai lieu cu lai dung chinh cac ma nay de goi REST API runtime. Khi app start binh thuong, database rong nen controller tra ve:

```text
Student or ClassSection not found.
```

Ban da duoc sua bang cach them `data.sql` vao `engine/src/main/resources/`, vi vay khi app khoi dong se co san du lieu mau dung nhu trong test.

## Du lieu mau runtime

Sau khi app start, H2 se co san:

- Student:
  - `SV001`: da dong hoc phi, da hoan thanh `CS101`
  - `SV002`: chua dong hoc phi, da hoan thanh `CS101`
- Course:
  - `CS101`
  - `CS102`
- ClassSection:
  - `CS101_01`
  - `CS102_01`

## Cach chay app

```powershell
cd e:\UCON_KMA\engine
.\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
```

## Test 1: Dang ky thanh cong

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

Y nghia:

- `SV001` hop le
- `CS102_01` dang `OPEN`
- du trong transaction window
- du dieu kien tien quyet
- khong vuot tran tin chi

## Test 2: Tu choi do chua dong hoc phi

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

Y nghia:

- `SV002` ton tai
- nhung `tuitionPaid = false`
- request bi chan o `PRE_AUTHORIZATION`

## Test 3: Huy dang ky

Buoc 1: dang ky truoc

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

Buoc 2: goi DROP

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

## Response thuc te cua 2 endpoint

### /api/register

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

### /api/drop

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

## Quy trinh xu ly dung voi code hien tai

```text
REQUEST -> RegistrationController
  -> validate request body
  -> load Student va ClassSection tu database
  -> PRE_AUTHORIZATION
  -> refresh entity
  -> ONGOING_AUTHORIZATION
  -> POST_UPDATE
  -> save Student va ClassSection
  -> response
```

## Ghi chu quan trong

- Tai lieu cu ghi response dang `permit/failedCode` la khong dung voi runtime hien tai.
- Runtime hien tai tra ve `String body`, vi du:
  - `Successfully enrolled.`
  - `DENIED_PREAUTH: TUITION_NOT_PAID`
- Neu muon kiem tra database, co the vao:

```text
http://localhost:8080/h2-console
```

JDBC URL:

```text
jdbc:h2:mem:ucondb
```
