# UCON_KMA

He thong nghien cuu va hien thuc mo hinh UCON cho bai toan dang ky hoc phan tai KMA.

## Muc tieu

Project nay dung DSL rieng de dac ta policy UCON, sinh XMI policy model, va thuc thi policy o runtime tren Spring Boot.

Trong pham vi do an, UCON duoc hien thuc theo 3 pha chinh:

- `PRE_AUTHORIZATION`: chan request truoc khi hanh dong xay ra
- `ONGOING_AUTHORIZATION`: kiem tra lai truoc commit neu state thay doi
- `POST_UPDATE`: cap nhat state sau khi request duoc phep

## Anh xa UCON trong project

- `Subject`: `Student`
- `Object`: `ClassSection`
- `Right/Action`: `REGISTER`, `DROP`
- `Environment`: registration phase, current date, semester, maintenance flag

## Kien truc tong quat

```text
PAP (DSL + XMI)
  dsl/ucon_policy.dsl
  -> parser / transformer
  -> xmi/ucon_policy.xmi

PEP
  RegistrationController
  Nhan request REST /api/register va /api/drop

PDP
  PolicyDecisionPoint + PolicyEngine
  Load model XMI, loc policy theo phase/action, evaluate condition, tra decision

PIP
  StudentRepository, ClassSectionRepository, RegistrationRepository, AuditLogRepository
  Cung cap thuoc tinh subject, object, environment, request

Post-update / Trace
  ExpressionEvaluator
  Cap nhat state, tao/xoa Registration, ghi AuditLog
```

## Ghi chu ve UCON hien tai

Project da the hien ro tinh chat UCON qua:

- chan request truoc hanh dong
- re-check state truoc commit
- cap nhat mutable attributes sau khi permit

Tuy nhien, `ONGOING_AUTHORIZATION` hien tai duoc hien thuc duoi dang `transaction-level re-check`, nghia la kiem tra lai o sat thoi diem commit. No khong phai continuous monitoring cho mot phien su dung dai han.

## Cau truc repo

```text
UCON_KMA/
|- dsl/         Grammar ANTLR, parser, transformer DSL -> XMI
|- engine/      Spring Boot app, policy engine, REST API, tests
|- metamodel/   Ecore metamodel
|- xmi/         Policy model runtime
|- docs/        Tai lieu ly thuyet, huong dan chay, kich ban demo
```

## Cac nhom policy chinh

- `PRE_AUTHORIZATION`
  - hoc phi
  - transaction window
  - trang thai lop
  - duplicate
  - gioi han tin chi
  - tien quyet
  - xung dot lich
- `ONGOING_AUTHORIZATION`
  - suc chua lop
  - trang thai lop thay doi
  - student hold
  - maintenance
- `POST_UPDATE`
  - cap nhat enrolled/currentCredits/tuitionDebt
  - tao/xoa Registration
  - ghi AuditLog

## Cach build nhanh

### Build DSL

```powershell
cd e:\UCON_KMA\dsl
.\apache-maven-3.9.6\bin\mvn.cmd clean install
```

### Chay full test engine

```powershell
cd e:\UCON_KMA\engine
.\apache-maven-3.9.6\bin\mvn.cmd clean test
```

### Chay theo test/policy id

```powershell
cd e:\UCON_KMA\engine
.\run.bat P01
.\run.bat T01
.\run.bat REPORT
```

### Chay app runtime

```powershell
cd e:\UCON_KMA\engine
.\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
```

## REST API runtime

Endpoint chinh:

- `POST /api/register`
- `POST /api/drop`
- `GET /api/demo/state`

Response runtime hien tai tra ve JSON co traceability, vi du:

```json
{
  "requestId": "demo-1",
  "action": "REGISTER",
  "decision": "DENY",
  "phase": "PRE_AUTHORIZATION",
  "studentId": "SV002",
  "classId": "CS102_01",
  "failedPolicy": "P01_TuitionPaid_Pre",
  "denyReason": "TUITION_NOT_PAID",
  "explanation": "Sinh vien chua hoan tat hoc phi nen request bi chan truoc khi dang ky xay ra.",
  "message": "DENIED_PREAUTH: TUITION_NOT_PAID"
}
```

## Tai lieu nen doc

Neu can chay va demo:

1. [docs/guides/HUONG_DAN_CHAY_CHI_TIET.md](docs/guides/HUONG_DAN_CHAY_CHI_TIET.md)
2. [docs/guides/HUONG_DAN_REST_API_CHUAN.md](docs/guides/HUONG_DAN_REST_API_CHUAN.md)
3. [docs/guides/KICH_BAN_NOI_DEMO_RUNTIME_3_5P.md](docs/guides/KICH_BAN_NOI_DEMO_RUNTIME_3_5P.md)

Neu can doc ly thuyet:

1. [docs/chapter1_theory/chapter_1_2_theory.md](docs/chapter1_theory/chapter_1_2_theory.md)
2. [docs/chapter3_kma_model/chapter_3_logic.md](docs/chapter3_kma_model/chapter_3_logic.md)
3. [docs/chapter4_dsl/chapter_4_1_metamodel.md](docs/chapter4_dsl/chapter_4_1_metamodel.md)
4. [docs/chapter4_dsl/chapter_4_2_grammar.md](docs/chapter4_dsl/chapter_4_2_grammar.md)
