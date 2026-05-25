# Final Release Checklist

Checklist nay chot trang thai repo theo cac nhom yeu cau A-O truoc khi nop/bao ve.

| Nhom | Trang thai | Bang chung |
| --- | --- | --- |
| A. Formatting/readability | `PASS` | `ArtifactFormattingTest`, `.editorconfig`, optional Spotless profile `format-check`, cac artifact chinh da nhieu dong |
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
| M. Static quality/CI | `PASS` | GitHub Actions YAML, CI badge, Maven Enforcer, JaCoCo, optional Spotless profile |
| N. Domain integrity | `PASS` | safe defaults, `DomainInvariantChecker`, optimistic locking, unique registration |
| O. Documentation/code traceability | `PASS` | README, mapping, coverage report, test result, benchmark, generated docs |

## Commands Verified Locally

```powershell
cd engine
mvn test
```

Result:

```text
Tests run: 51, Failures: 0, Errors: 0, Skipped: 0
```

```powershell
cd dsl
mvn test
```

Result:

```text
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

## Optional Quality Command

Spotless is configured as an optional profile to avoid breaking offline `mvn test`.

```powershell
cd engine
mvn -Pformat-check spotless:check
```

The mandatory no-minified-file guard is still enforced by `ArtifactFormattingTest` during the normal engine test suite.
