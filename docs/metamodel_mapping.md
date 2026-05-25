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
| `Policy` | Don vi policy chinh, mang `predicate`, `phase`, `updateTiming`, `targetAction`, `effect`, `condition`, updates |
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

## 5. Vi sao metamodel nay quan trong

Metamodel la cau noi hoc thuat giua:

- quy che hoc vu / policy text
- mo hinh UCONABC
- parser / XMI
- runtime enforcement

Neu khong co metamodel va XMI thuc, DSL chi la text parser. Co metamodel va XMI, project co the trinh bay duoc tinh chat `MDE-inspired` dung huong bai bao.
