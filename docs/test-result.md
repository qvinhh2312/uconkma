# Test Result

Tai lieu nay ghi lai ket qua kiem thu tong hop cho ban hien tai cua repo.

Lan chay gan nhat: `2026-05-26`.

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

GitHub Actions also runs the same format check before building DSL and testing engine.

## Result

```text
Engine: Tests run: 60, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

DSL: Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Full engine suite completed locally in about `49 s` on the current machine snapshot.

Spotless format check:

```text
BUILD SUCCESS
Spotless.Format is keeping 79 files clean - 0 needs changes to be clean
```

Raw GitHub format check:

```text
Raw GitHub formatting verification passed.
```

## Coverage

```text
JaCoCo line coverage: 82.68% (1709 covered / 358 missed)
JaCoCo branch coverage: 61.58% (545 covered / 340 missed)
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
