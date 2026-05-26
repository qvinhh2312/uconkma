# Current vs Maximum Version

Tai lieu nay chot ranh gioi giua ban hien tai cua do an va mot he thong san xuat toi da.
Muc tieu la tranh mo ta qua muc, dong thoi cho thay huong phat trien tiep theo.

| Thanh phan | Ban hien tai | Ban toi da san xuat |
| --- | --- | --- |
| DSL | Grammar va policy DSL du de dac ta UCONABC cho bai toan dang ky/huy hoc phan | Co editor plugin, syntax highlighting, linter va autocomplete rieng |
| XMI / Ecore | Co metamodel, XMI explicit fields va test conformance voi Ecore | Co model transformation pipeline day du hon, versioned model repository va validation automation |
| PAP | Co `policyStatus`, lifecycle service va runtime chi load `ACTIVE` policies | Co persistence policy versioning, UI quan tri, approval workflow va audit PAP rieng |
| PEP / PDP / PIP | Tach controller, service, PEP, PDP, PIP, workflow PRE -> ONGOING -> POST | Chay distributed, co cache, observability va HA deployment |
| Ongoing continuity | Co transaction-level re-check va event-driven revoke cho ACTIVE sessions | Co scheduler, distributed event bus, long-running monitor va recheck history day du |
| Obligation | Co preB0, onB0, postB3 va audit obligation | Co obligation executor rieng, retry, timeout va escalation workflow |
| Mutability | Co pre/ongoing/post updates, rollback, snapshots va invariant check | Co transaction log, compensation workflow va conflict resolution nang cao |
| Analyzer | Heuristic analyzer cho rollback, audit, priority, unsafe update va DROP counterpart | Co formal solver/SMT/OCL, proof/report tu dong va counter-example generation |
| Validator | Co semantic validator, schema validator va mutation-style tests | Co rule catalog chinh thuc, quick-fix suggestions va integration voi PAP |
| Traceability | Response co `DecisionTrace`, phase/predicate/policy result, snapshot va policy metadata | Co centralized trace store, UI explainability va export bao cao tu dong |
| Benchmark | Co policy micro-benchmark va benchmark API nho cho `register/drop` | Co end-to-end load test nhieu client, throughput, resource profile va SLA report |
| App | REST/demo phu hop do an nghien cuu | UI day du cho sinh vien, giao vu, admin va monitoring dashboard |
| CI / release | GitHub Actions build/test/format, docs checklist va test-result | Co staging/prod pipeline, release tags, artifact signing va deployment automation |

## Ket luan

Ban hien tai phu hop muc tieu do an nghien cuu: dac ta policy bang DSL/XMI, enforcement theo UCONABC,
traceability va validation/analyzer ro rang. Ban toi da san xuat can them UI, distributed monitoring,
policy repository/versioning day du va formal verification sau hon.
