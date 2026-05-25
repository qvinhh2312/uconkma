# Test Result

Tai lieu nay ghi lai ket qua kiem thu tong hop cho ban hien tai cua repo.

## Command

```powershell
cd engine
mvn test
```

```powershell
cd dsl
mvn test
```

## Result

```text
Engine: Tests run: 51, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

DSL: Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Full engine suite completed locally in about `50 s` on the current machine snapshot.

## Coverage

```text
JaCoCo line coverage: 80.39% (1636 covered / 399 missed)
JaCoCo branch coverage: 60.66% (532 covered / 345 missed)
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
- co `ControllerRuntimeFlowTest` rieng cho controller runtime flow
- co `XmiEcoreConformanceTest` rieng cho metamodel/XMI conformance
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

## Luu y

Khi cap nhat them policy hoac metamodel/XMI, file nay can duoc cap nhat lai theo ket qua `mvn clean test` moi nhat.
