# Final Code Quality Checklist

Tai lieu nay tong hop trang thai ky thuat cua repo truoc khi nop/bao ve.

| Nhom | Tieu chi | Bat buoc? | Trang thai | Ghi chu |
| --- | --- | --- | --- | --- |
| Architecture | Controller mong, service xu ly nghiep vu | Co | `x` | `RegistrationController` chi delegate sang `RegistrationService` |
| Architecture | PEP/PDP/PIP/PAP phan vai ro | Co | `x` | Co `UconPepService`, `PolicyInformationPoint`, `PolicyDecisionPoint`, `PolicyAdministrationPoint` |
| UCON Flow | PRE -> ONGOING -> POST ro rang | Co | `x` | `UconExecutionWorkflow` la pipeline chuan |
| UCON Flow | Fail nao cung co trace | Co | `x` | DENY response gom `DecisionTrace` |
| Domain | Co optimistic locking | Co | `x` | `Student` va `ClassSection` co `@Version` |
| Domain | Registration co unique constraint | Co | `x` | Unique `studentId + classId + semester` |
| DSL | Grammar format dep | Co | `x` | `dsl/UconPolicy.g4` da format nhieu dong |
| DSL | DSL policy format dep | Co | `x` | `dsl/ucon_policy.dsl` da format nhieu dong |
| XMI | XMI explicit required fields | Co | `x` | `predicate/phase/updateTiming/targetAction/effect/entity/operator` explicit |
| XMI | XMI conform Ecore test | Nen | `x` | `test36_XmiPolicyModelConformsToEcoreAndSemanticRules` |
| Validator | Condition khong update | Co | `x` | Semantic validator enforce |
| Validator | Environment/request khong update | Co | `x` | Semantic validator enforce |
| Validator | Mutable schema enforced | Co | `x` | `AttributeSchema` + `PolicyValidator` |
| Analyzer | Unsafe update warning | Nen | `x` | `UNSAFE_UPDATE`, `STATEFUL_MUTATION`, `INCOMPLETE_DROP_FLOW` |
| Trace | Trace co phase/predicate/policy result | Co | `x` | `DecisionTrace`, `PhaseTrace`, `PolicyTraceEntry` |
| Update | Update qua UpdateManager | Co | `x` | PRE/ONGOING/POST update qua `UpdateManager` |
| Rollback | Rollback qua RollbackManager | Co | `x` | ONGOING compensation qua `RollbackManager` |
| Session | UsageSession status cap nhat dung | Co | `x` | `ACTIVE/COMMITTED/REVOKED/FAILED` duoc test |
| Tests | Test UCONABC day du | Co | `x` | preA0/preB0/preC0/onA0/onA2/onB0/onC0/postB3 da co test |
| Tests | Race condition test | Co | `x` | 2-student/1-slot va 10-student/3-slot |
| Tests | Validator test | Co | `x` | Semantic + schema validation tests co san |
| Tests | Trace test | Co | `x` | Decision trace pass/fail/update/rollback da duoc assert |
| Tests | DSL -> XMI test | Nen | `x` | `UconDslToXmiParserTest` |
| Tests | Runtime controller flow test | Co | `x` | `ControllerRuntimeFlowTest` + `test37/test38` |
| Tests | Artifact formatting guard | Co | `x` | `ArtifactFormattingTest` chan file chinh bi minify mot dong |
| Docs | README sach | Co | `x` | Badge, architecture, DSL/XMI, docs, limitations |
| Docs | Mapping UCONABC | Co | `x` | `docs/ucon_mapping.md` |
| Docs | Coverage report | Co | `x` | `docs/ucon_coverage_report.md` |
| Docs | Generated docs tu DSL/XMI | Nen | `x` | `tools/generate-docs.ps1` sinh `docs/generated/*` |
| Docs | Mutation testing notes | Nen | `x` | `docs/mutation_testing.md` |
| CI | GitHub Actions pass | Co | `x` | `.github/workflows/maven.yml` |
| Quality | Khong file minified mot dong | Co | `x` | README/grammar/DSL/XMI/docs da format |
| Quality | Khong hard-code policy ngoai DSL | Co | `x` | Controller khong tu quyet rule; workflow danh gia qua evaluators/PolicyEngine |
| Quality | Formatting guard | Nen | `x` | `.editorconfig` + `ArtifactFormattingTest` + optional Spotless profile `mvn -Pformat-check spotless:check` |

## Kiem tra bo sung

- `DSL` co parser error ro rang va chan `policyId` trung.
- `engine/pom.xml` da khai bao ro `maven-surefire-plugin`, `maven-enforcer-plugin`, `jacoco-maven-plugin`, va optional `spotless-maven-plugin` profile `format-check`.
- `dsl/pom.xml` da khai bao `maven-surefire-plugin`, `maven-enforcer-plugin`, va parser tests.
- Event-driven monitoring da co cho `maintenance`, `class status`, `student hold`.
- `JaCoCo` local coverage:
  - line coverage: `82.68%`
  - branch coverage: `61.58%`

## Ghi chu gioi han con lai

- Test engine van duoc giu trong mot suite tong hop lon; co the tach nho hon neu can toi uu presentation.
- `DecisionTrace` da co `snapshotBefore` va `snapshotAfter` trong API response; snapshot hien dang tap trung vao cac mutable attributes chinh cua subject/object.
- Benchmark hien la micro-benchmark cho pipeline validator/analyzer/runtime filter, khong phai load test phan tan.
