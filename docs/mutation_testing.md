# Mutation Testing Notes

Tai lieu nay ghi lai cac mutation nhe nen dung khi bao cao validator/analyzer.
Muc tieu khong phai mutation framework day du, ma la chung minh policy quality
gates bat duoc cac loi co chu dich.

| Mutation | Expected gate | Expected result |
| --- | --- | --- |
| Them update vao policy `CONDITION` | `PolicyModelSemanticValidator` | Reject: condition policy cannot mutate state |
| Doi target update thanh `ENVIRONMENT.isMaintenance` | `PolicyValidator` + `AttributeSchema` | Reject: immutable environment update |
| Xoa `rollbackUpdates` cua `P20_ReserveSeat_OnA2` | `PolicyAnalyzer` | Warn: ongoing update missing compensation |
| Xoa `P12_AuditAndTrace_PostB3` | `PolicyAnalyzer` | Warn: missing audit/trace |
| Doi DROP post-update tru `object.enrolled` nhung xoa guard P16 | `PolicyAnalyzer` | Warn: incomplete DROP flow / unsafe update |
| Xoa tham so cua `AuditLogStatement` | `PolicyModelSemanticValidator` | Reject: audit log arity invalid |

## Current Automated Coverage

- Validator mutation scenarios are covered in `UconEngineApplicationTests`.
- Analyzer mutation scenarios are covered in `UconEngineApplicationTests`.
- Runtime regression scenarios are covered in `ControllerRuntimeFlowTest`.
- Artifact format regressions are covered in `ArtifactFormattingTest`.

## Limitation

Day la mutation testing muc nhe, chua dung PIT/JaCoCo mutation score. Neu can
lam ban toi da, co the them PIT va bao cao mutation score rieng.
