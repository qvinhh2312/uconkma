# Test Result

Tai lieu nay ghi lai ket qua kiem thu tong hop cho ban hien tai cua repo.

## Command

```powershell
cd e:\UCON_KMA\engine
.\apache-maven-3.9.6\bin\mvn.cmd clean test
```

```powershell
cd e:\UCON_KMA\dsl
.\apache-maven-3.9.6\bin\mvn.cmd test
```

## Result

```text
Engine: Tests run: 36, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

DSL: Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Full engine suite completed locally in about `44.285 s` on the current machine snapshot.

## Coverage

```text
JaCoCo line coverage: 79.97% (1609 covered / 403 missed)
JaCoCo branch coverage: 60.57% (530 covered / 345 missed)
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
- co parser test rieng cho:
  - `DSL -> XMI`
  - syntax error ro rang
  - duplicate `policyId`
- `JaCoCo` report duoc sinh sau `engine mvn test` tai `engine/target/site/jacoco`

## Benchmark command da xac nhan

```powershell
cd e:\UCON_KMA\engine
.\apache-maven-3.9.6\bin\mvn.cmd -Dtest=PolicyBenchmarkSuite test
```

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Luu y

Khi cap nhat them policy hoac metamodel/XMI, file nay can duoc cap nhat lai theo ket qua `mvn clean test` moi nhat.
