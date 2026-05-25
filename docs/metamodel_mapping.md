# Metamodel Mapping

Tai lieu nay giai thich cach `DSL -> Metamodel -> XMI -> Engine` duoc anh xa trong project.

## 1. Pipeline thuc thi

```text
dsl/ucon_policy.dsl
-> ANTLR grammar trong dsl/UconPolicy.g4
-> UconDslToXmiParser
-> metamodel/ucon.ecore
-> xmi/ucon_policy.xmi
-> PolicyDecisionPoint load XMI
-> PolicyEngine evaluate runtime
```

## 2. Anh xa class chinh trong `ucon.ecore`

| Metamodel element | Vai tro |
|---|---|
| `PolicyModel` | Root chua danh sach `policies` va `policySets` |
| `PolicySet` | Gom nhom policy va `combiningAlgorithm` |
| `Policy` | Don vi policy chinh, mang `predicate`, `phase`, `updateTiming`, `targetAction`, `effect`, `source`, `version`, `policyStatus`, `uconVariant`, `condition`, updates |
| `Expression` | Cay bieu thuc cho condition |
| `VariableAccess` | Truy cap `subject.*`, `object.*`, `environment.*`, `request.*` |
| `LogicalOperator` | `AND`, `OR`, `NOT` |
| `RelationalOperator` | `==`, `!=`, `<`, `<=`, `IN`, `SUBSET_OF`, `OVERLAPS`, ... |
| `ArithmeticOperator` | `+`, `-` |
| `Constant` / `ListConstant` | Gia tri hang trong DSL |
| `FunctionCall` | Goi cac function da whitelist trong engine |
| `UpdateStatement` | Mutation tren attribute |
| `CreateTransactionStatement` | Tao `Registration` runtime |
| `DeleteTransactionStatement` | Xoa `Registration` runtime |
| `AuditLogStatement` | Ghi audit trace |

## 3. Enum chinh trong metamodel

| Enum | Gia tri |
|---|---|
| `PredicateType` | `AUTHORIZATION`, `OBLIGATION`, `CONDITION` |
| `PhaseType` | `PRE`, `ONGOING`, `POST` |
| `UpdateTiming` | `NONE`, `PRE`, `ONGOING`, `POST` |
| `ActionType` | `REGISTER`, `DROP`, `ANY` |
| `PolicyEffect` | `PERMIT`, `DENY` |
| `PolicyStatus` | `DRAFT`, `VALIDATED`, `ACTIVE`, `DEPRECATED`, `ARCHIVED` |
| `CombiningAlgorithm` | `DENY_OVERRIDES`, `PERMIT_OVERRIDES`, `FIRST_APPLICABLE`, `PRIORITY_ORDER`, `ONLY_ONE_APPLICABLE` |

## 4. Anh xa sang runtime Java

| UCON concept | Runtime Java |
|---|---|
| Subject | `Student` |
| Object | `ClassSection` |
| Right/Action | `REGISTER`, `DROP` |
| Environment | `Environment` |
| Decision | `AuthDecision` |
| Trace | `DecisionTrace`, `PhaseTrace`, `PolicyTraceEntry` |
| Session | `UsageSession` |
| PAP / lifecycle gate | `PolicyAdministrationPoint` |

## 5. Metadata va lifecycle

`Policy` hien tai mang them 4 metadata truc tiep trong DSL, Ecore va XMI:

| Field | Y nghia |
|---|---|
| `source` | Nguon nghiep vu / quy che cua policy |
| `version` | Phien ban policy |
| `policyStatus` | Trang thai lifecycle de PAP quyet dinh co dua vao runtime hay khong |
| `uconVariant` | Nhan mapping truc tiep sang bien the UCONABC |

Tai runtime:

1. `PolicyDecisionPoint` load `XMI`
2. `PolicyValidator` kiem tra tinh hop le
3. `PolicyAnalyzer` sinh canh bao chat luong
4. `PolicyAdministrationPoint` loai bo moi policy khong o trang thai `ACTIVE`
5. `OngoingMonitor` + `SessionRecheckService` co the re-evaluate `ACTIVE` sessions khi event moi truong / subject / object xay ra

Kiem chung conformity tren repo hien tai:

- `test36_XmiPolicyModelConformsToEcoreAndSemanticRules`
- test nay load truc tiep `ucon.ecore` va `ucon_policy.xmi` bang EMF `ResourceSet`
- sau do chay `semanticValidator` va `policyValidator` de xac nhan model co the dua vao runtime

## 6. Vi sao metamodel nay quan trong

Metamodel la cau noi hoc thuat giua:

- quy che hoc vu / policy text
- mo hinh UCONABC
- parser / XMI
- runtime enforcement

Neu khong co metamodel va XMI thuc, DSL chi la text parser. Co metamodel va XMI, project co the trinh bay duoc tinh chat `MDE-inspired` dung huong bai bao.
