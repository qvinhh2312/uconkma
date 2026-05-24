# MAPPING POLICY -> UCONABC -> TEST

| Policy | Noi dung | Predicate | Phase | Update timing | UCON type | Test |
|---|---|---|---|---|---|---|
| P01 | Hoc phi | AUTHORIZATION | PRE | NONE | preA0 | T02 |
| P02 | Dot/gio dang ky | CONDITION | PRE | NONE | preC0 | T03 |
| P03 | Lop phai OPEN | AUTHORIZATION | PRE | NONE | preA0 | T04 |
| P04 | Khong dang ky trung | AUTHORIZATION | PRE | NONE | preA0 | T05 |
| P05 | Gioi han tin chi | AUTHORIZATION | PRE | NONE | preA0 | T06 |
| P06 | Tien quyet | AUTHORIZATION | PRE | NONE | preA0 | T07 |
| P07 | Xung dot lich | AUTHORIZATION | PRE | NONE | preA0 | T08 |
| P17 | Xac nhan quy che | OBLIGATION | PRE | NONE | preB0 | T13 |
| P18 | Override phai co ly do | OBLIGATION | PRE | NONE | preB0 | T14 |
| P19 | Tang so lan thu dang ky | AUTHORIZATION | PRE | PRE | preA1 | T01 |
| P13a | Maintenance truoc request | CONDITION | PRE | NONE | preC0 | T11 |
| P08 | Re-check suc chua | AUTHORIZATION | ONGOING | NONE | onA0 | T10 |
| P09 | Re-check trang thai lop | AUTHORIZATION | ONGOING | NONE | onA0 | T04 |
| P10 | Re-check hold | AUTHORIZATION | ONGOING | NONE | onA0 | T09 |
| P13 | Maintenance giua xu ly | CONDITION | ONGOING | NONE | onC0 | T11 |
| P20 | Reserved seat + rollback | AUTHORIZATION | ONGOING | ONGOING | onA2 | T01, T10 |
| P11 | Register state update | AUTHORIZATION | POST | POST | postA3 | T01 |
| P14 | Drop state revert | AUTHORIZATION | POST | POST | postA3 | T12 |
| P12 | Audit trace | OBLIGATION | POST | POST | postB3 | T01, T12, T20 |
| P16 | Drop only if registered | AUTHORIZATION | PRE | NONE | preA0 | T12 |

## Ghi chu

- Ban v2 hien tai da co du dai dien cho: `preA0`, `preA1`, `preB0`, `preC0`, `onA0`, `onA2`, `onC0`, `postA3`, `postB3`.
- `ONGOING` hien tai van la `transaction-level re-check`, chua co `UsageSession` day du.
- `PolicyAnalyzer`, `PolicyValidator`, `attribute-schema.yml`, `DecisionTrace` la cac lop bo sung cho `Verification` va `Traceability`.
