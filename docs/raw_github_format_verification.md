# Raw GitHub Format Verification

Muc tieu cua file nay la ghi lai cach kiem tra truc tiep `raw.githubusercontent.com`
de tranh nham lan do browser/CDN cache khi xem file raw tren GitHub.

## Command

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\verify-raw-format.ps1
```

Script them tham so cache-bust vao raw URL va dem so dong theo ky tu `LF`.

## Latest Verified Result

Lan kiem tra gan nhat: `2026-05-26`.

| File | Raw line count |
| --- | ---: |
| `dsl/UconPolicy.g4` | 242 |
| `dsl/ucon_policy.dsl` | 540 |
| `.github/workflows/maven.yml` | 31 |
| `README.md` | 329 |
| `docs/test-result.md` | 97 |
| `docs/ucon_mapping.md` | 57 |
| `docs/ucon_coverage_report.md` | 73 |
| `docs/policy_catalog.md` | 44 |
| `docs/validation_rules.md` | 76 |
| `docs/decision_trace_examples.md` | 163 |
| `docs/benchmark_result.md` | 73 |
| `RegistrationController.java` | 33 |
| `RegistrationService.java` | 121 |
| `UconPepService.java` | 26 |
| `UconExecutionWorkflow.java` | 527 |
| `PolicyEngine.java` | 287 |
| `PolicyValidator.java` | 71 |
| `PolicyAnalyzer.java` | 233 |
| `UpdateManager.java` | 48 |
| `RollbackManager.java` | 44 |

## Ket luan

Tat ca artifact quan trong deu duoc format thanh nhieu dong tren raw GitHub.
Neu browser van hien 1 dong, hay refresh voi cache-bust hoac mo lai raw URL sau khi GitHub CDN het cache.
