# UCON Mapping

Tai lieu nay dung de anh xa truc tiep tu policy ID thuc te sang `predicate`, `phase`, `update timing`, bien the UCONABC va test case.

| Policy | Predicate | Phase | Update timing | UCON type | Test |
|---|---|---|---|---|---|
| `P01_TuitionPaid_PreA0` | AUTHORIZATION | PRE | NONE | preA0 | `T02` |
| `P13a_EmergencyMaintenance_PreC0` | CONDITION | PRE | NONE | preC0 | `T11` |
| `P02_TransactionWindow_PreC0` | CONDITION | PRE | NONE | preC0 | `T03` |
| `P03_ClassStatusOpen_PreA0` | AUTHORIZATION | PRE | NONE | preA0 | `T04` |
| `P04_NotAlreadyRegistered_PreA0` | AUTHORIZATION | PRE | NONE | preA0 | `T05` |
| `P16_DropOnlyIfRegistered_PreA0` | AUTHORIZATION | PRE | NONE | preA0 | `T12` |
| `P21_DropWindow_PreC0` | CONDITION | PRE | NONE | preC0 | `T26` |
| `P26_MaxDropTimes_PreA0` | AUTHORIZATION | PRE | NONE | preA0 | `T26` |
| `P05_CreditLimit_PreA0` | AUTHORIZATION | PRE | NONE | preA0 | `T06` |
| `P25_MaxRegisterAttempts_PreA0` | AUTHORIZATION | PRE | NONE | preA0 | `T25` |
| `P06_Prerequisite_PreA0` | AUTHORIZATION | PRE | NONE | preA0 | `T07` |
| `P17_AgreeRegistrationRule_PreB0` | OBLIGATION | PRE | NONE | preB0 | `T13` |
| `P07_ScheduleConflict_PreA0` | AUTHORIZATION | PRE | NONE | preA0 | `T08` |
| `P18_AdminOverrideReason_PreB0` | OBLIGATION | PRE | NONE | preB0 | `T14` |
| `P19_RegisterAttempt_PreA1` | AUTHORIZATION | PRE | PRE | preA1 | `T01` |
| `P13_EmergencyMaintenance_OnC0` | CONDITION | ONGOING | NONE | onC0 | `T11`, `T22` |
| `P08_CapacityRecheck_OnA0` | AUTHORIZATION | ONGOING | NONE | onA0 | `T10`, `T23` |
| `P20_ReserveSeat_OnA2` | AUTHORIZATION | ONGOING | ONGOING | onA2 | `T01`, `T10`, `T24` |
| `P23_DropLockedClass_OnA0` | AUTHORIZATION | ONGOING | NONE | onA0 | `T26` |
| `P09_ClassStatusRecheck_OnA0` | AUTHORIZATION | ONGOING | NONE | onA0 | `T04` |
| `P10_StudentHoldRecheck_OnA0` | AUTHORIZATION | ONGOING | NONE | onA0 | `T09` |
| `P27_SessionLease_OnB0` | OBLIGATION | ONGOING | NONE | onB0 | `T27` |
| `P11_RegisterStateUpdate_PostA3` | AUTHORIZATION | POST | POST | postA3 | `T01` |
| `P14_DropStateRevert_PostA3` | AUTHORIZATION | POST | POST | postA3 | `T12` |
| `P12_AuditAndTrace_PostB3` | OBLIGATION | POST | POST | postB3 | `T01`, `T12`, `T20` |

## Doc bang nay nhu the nao

- Cot `Predicate` cho biet policy thuoc nhom `Authorization`, `Obligation` hay `Condition`.
- Cot `Phase` cho biet policy duoc danh gia o `PRE`, `ONGOING` hay `POST`.
- Cot `Update timing` phan biet policy chi kiem tra (`NONE`) hay co mutation tai `PRE`, `ONGOING`, `POST`.
- Cot `UCON type` la nhan gon theo bien the UCONABC ma do an dang bao phu.
- Cot `Test` la test case thuc te trong `engine/src/test/.../UconEngineApplicationTests.java`.

## Traceability tong quat

```text
Quy che hoc vu
-> UCON concept (A/B/C, PRE/ONGOING/POST)
-> DSL policy trong dsl/ucon_policy.dsl
-> XMI policy model trong xmi/ucon_policy.xmi
-> PDP/PolicyEngine evaluate runtime
-> DecisionTrace + AuditLog
-> JUnit test T01..T27
```
