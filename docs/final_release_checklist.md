# Final Release Checklist

Checklist nay chot trang thai repo theo cac nhom yeu cau A-O truoc khi nop/bao ve.

## Release Gates

| Muc | Trang thai | Bang chung |
| --- | --- | --- |
| GitHub Actions xanh | `[x]` | `.github/workflows/maven.yml` chay format check, DSL build, engine test |
| Engine tests pass | `[x]` | `mvn clean test`: 51 tests pass |
| DSL tests pass | `[x]` | `mvn clean test`: 3 tests pass |
| Spotless check pass | `[x]` | `mvn -Pformat-check spotless:check` |
| JaCoCo coverage >= 80% line | `[x]` | line coverage `80.39%`, branch coverage `60.66%` |
| No minified artifact | `[x]` | `ArtifactFormattingTest` + Spotless profile |
| XMI/Ecore conformance test pass | `[x]` | `XmiEcoreConformanceTest` |
| Controller runtime flow test pass | `[x]` | `ControllerRuntimeFlowTest` |
| Race condition test pass | `[x]` | race cases in engine integration suite |
| PAP lifecycle test pass | `[x]` | active-only policy loading and lifecycle tests |
| Ongoing monitor test pass | `[x]` | event-driven revoke tests in integration suite |
| README updated | `[x]` | README has goal, UCON mapping, architecture, run/test commands, docs links, limitations |
| Docs generated updated | `[x]` | `docs/generated/policies.md`, `docs/generated/ucon_coverage.md`, `docs/generated/validation_report.md` |

## A-O Coverage Summary

| Nhom | Trang thai | Bang chung |
| --- | --- | --- |
| A. Formatting/readability | `PASS` | `ArtifactFormattingTest`, `.editorconfig`, Spotless profile `format-check`, cac artifact chinh da nhieu dong |
| B. Runtime architecture | `PASS` | `RegistrationController -> RegistrationService -> UconPepService -> UconExecutionWorkflow` |
| C. UconContext | `PASS` | `UconContext` gom request, subject, object, pre/ongoing environment, session, snapshots, traces |
| D. Transaction/concurrency/database | `PASS` | `@Transactional`, `@Version`, unique registration constraint, race tests |
| E. Update/rollback | `PASS` | `UpdateManager`, `RollbackManager`, `UpdatePlan`, invariant check sau mutation |
| F. DecisionTrace/explainability | `PASS` | `DecisionTrace`, `PhaseTrace`, `PolicyTraceEntry`, updates/rollback/snapshot |
| G. API/error handling | `PASS` | `ApiDecisionResponse`, `ErrorResponse`, `GlobalExceptionHandler` |
| H. Validator/analyzer | `PASS` | semantic validator, schema validator, analyzer warnings, mutation-style docs |
| I. DSL/XMI/Ecore | `PASS` | formatted grammar/DSL, explicit XMI, metadata, `XmiEcoreConformanceTest` |
| J. PAP/lifecycle | `PASS` | `policyStatus`, `PolicyAdministrationPoint`, `PolicyLifecycleService`, active-only runtime |
| K. Continuous monitoring | `PASS` | `OngoingMonitor`, `SessionRecheckService`, maintenance/class/hold events, documented limits |
| L. Test organization | `PASS` | focused tests plus integration suite: controller flow, XMI/Ecore, formatting, benchmark, DSL parser |
| M. Static quality/CI | `PASS` | GitHub Actions YAML, CI badge, Maven Enforcer, JaCoCo, Spotless profile |
| N. Domain integrity | `PASS` | safe defaults, `DomainInvariantChecker`, optimistic locking, unique registration |
| O. Documentation/code traceability | `PASS` | README, mapping, coverage report, test result, benchmark, generated docs |

## Commands Verified Locally

```powershell
cd engine
mvn clean test
```

Result:

```text
Tests run: 51, Failures: 0, Errors: 0, Skipped: 0
```

```powershell
cd dsl
mvn clean test
```

Result:

```text
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

```powershell
cd engine
mvn -Pformat-check spotless:check
```

Result:

```text
BUILD SUCCESS
Spotless.Format is keeping 78 files clean - 0 needs changes to be clean
```
