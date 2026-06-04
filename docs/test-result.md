# Test Result

Tai lieu nay ghi lai ket qua kiem thu tong hop cho ban hien tai cua repo.

Lan chay gan nhat: `2026-06-04`.

## Command

```powershell
cd engine
mvn clean test
```

```powershell
cd dsl
mvn clean test
```

```powershell
cd engine
mvn -Pformat-check spotless:check
```

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\verify-raw-format.ps1
```

```powershell
cd frontend
npm install
npm run build
```

GitHub Actions also runs the same format check before building DSL and testing engine.

## Result

```text
Engine: Tests run: 65, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

DSL: Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Full engine suite completed locally in about `51 s` on the current machine snapshot.

Spotless format check:

```text
BUILD SUCCESS
Spotless.Format is keeping 90 files clean - 0 needs changes to be clean
```

Raw GitHub format check:

```text
Raw GitHub formatting verification passed.
```

Frontend demo app:

```text
Node.js: v24.16.0
npm install: 159 packages audited, 0 vulnerabilities
npm audit --audit-level=moderate: 0 vulnerabilities
npm run build: Vite production build SUCCESS
Vite dev server: http://127.0.0.1:5173 returned HTTP 200
```

API smoke test with backend running on `http://localhost:8080`:

```text
POST /api/register without confirmedRegistrationRule -> 403 DENY P17_AgreeRegistrationRule_PreB0
POST /api/drop before registration -> 403 DENY P16_DropOnlyIfRegistered_PreA0
POST /api/demo/monitor/maintenance?active=true -> JSON with checkedSessions/revokedSessions
POST /api/demo/monitor/maintenance?active=false -> JSON with checkedSessions/revokedSessions
POST /api/demo/monitor/class-status?classId=CS102_01&status=LOCKED -> JSON with checkedSessions/revokedSessions
POST /api/demo/monitor/student-hold?studentId=SV001&holdCode=ACADEMIC_HOLD -> JSON with checkedSessions/revokedSessions
POST /api/demo/monitor/recheck -> JSON with checkedSessions/revokedSessions
```

Auth / SQL-backed portal smoke test:

```text
POST /api/auth/login sv001/student123 -> STUDENT token
GET /api/students/me with STUDENT token -> SV001 profile
GET /api/students/me/grades with STUDENT token -> 2 grades
GET /api/students with STUDENT token -> 400 INVALID_ARGUMENT
POST /api/auth/login admin/admin123 -> ADMIN token
GET /api/students with ADMIN token -> 2 students
GET /api/students/SV001/grades with ADMIN token -> 2 grades
POST /api/register for SV002 with SV001 token -> 400 INVALID_ARGUMENT
```

SQL database readiness:

```text
MySQL client on current machine: not found
localhost:3306: closed
Docker on current machine: not found

Repo-provided SQL setup:
- db/mysql/00-create-database.sql
- docker-compose.mysql.yml
- engine/.env.mysql.example
- docs/guides/SQL_AUTH_DEMO.md
```

Ket luan: backend da co profile MySQL va schema se duoc tao/cap nhat boi Hibernate khi MySQL chay. May kiem thu hien tai chua co MySQL/Docker nen SQL-profile runtime smoke test chua the chay tren local nay.

## Coverage

```text
JaCoCo line coverage: 83.21% (1734 covered / 350 missed)
JaCoCo branch coverage: 62.18% (554 covered / 337 missed)
```

## Y nghia

- policy engine van chay dung sau khi nang cap theo huong UCONABC
- `Authorization`, `Condition`, `Obligation`
- `preUpdates`, `ongoingUpdates`, `rollbackUpdates`, `postUpdates`
- `UsageSession`
- semantic validation
- invariant check
- decision trace
- race condition tests
- `onB0` ongoing obligation
- `PolicyAnalyzer` warnings cho `shadowing` va `conflicting priority`
- `policy metadata` trong DSL / Ecore / XMI: `source`, `version`, `policyStatus`, `uconVariant`
- `PolicyAdministrationPoint` loc chi giu `ACTIVE` policies o runtime
- `PolicyLifecycleService` ho tro chuoi `DRAFT -> VALIDATED -> ACTIVE -> DEPRECATED -> ARCHIVED`
- `OngoingMonitor` / `SessionRecheckService` cho event-driven revoke
- XMI explicit hon va nhat quan hon voi Ecore metamodel
- co test rieng xac nhan `xmi/ucon_policy.xmi` load dung theo `metamodel/ucon.ecore` va vuot qua semantic validation
- co test hoi quy xac nhan endpoint public dung `PRE/ONGOING/POST`, khong dung phase legacy
- co test hoi quy xac nhan `DROP` chua dang ky bi tu choi boi `P16_DropOnlyIfRegistered_PreA0`, khong hard-code trong controller
- co `ControllerRuntimeFlowTest` rieng cho controller runtime flow va policy metadata trong trace
- co `XmiEcoreConformanceTest` rieng cho metamodel/XMI conformance
- co `EndToEndApiBenchmarkTest` do `POST /api/register` va `POST /api/drop`
- co `ArtifactFormattingTest` chan artifact quan trong bi minify mot dong hoac quay lai phase legacy
- co `docs/generated/*` sinh tu `tools/generate-docs.ps1` de giam lech giua XMI/DSL va docs
- co parser test rieng cho:
  - `DSL -> XMI`
  - syntax error ro rang
  - duplicate `policyId`
- `JaCoCo` report duoc sinh sau `engine mvn test` tai `engine/target/site/jacoco`

## Benchmark command da xac nhan

```powershell
cd engine
mvn -Dtest=PolicyBenchmarkSuite test
```

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

```powershell
cd engine
mvn -Dtest=EndToEndApiBenchmarkTest test
```

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

| Endpoint | Avg ms | P95 ms | P99 ms | Notes |
|---|---:|---:|---:|---|
| POST /api/register | 19.945 | 22.910 | 22.910 | controller + PEP/PDP + DB + trace |
| POST /api/drop | 18.137 | 20.113 | 20.113 | controller + PEP/PDP + DB + trace |
```

## Luu y

Khi cap nhat them policy hoac metamodel/XMI, file nay can duoc cap nhat lai theo ket qua `mvn clean test` moi nhat.
