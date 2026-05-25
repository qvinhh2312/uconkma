# Generated Policy Catalog

Generated from xmi/ucon_policy.xmi.

| Policy | Predicate | Phase | UpdateTiming | Action | Effect | Variant | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| P01_TuitionPaid_PreA0 | AUTHORIZATION | PRE | NONE | REGISTER | PERMIT | preA0 | ACTIVE |
| P13a_EmergencyMaintenance_PreC0 | CONDITION | PRE | NONE | ANY | PERMIT | preC0 | ACTIVE |
| P02_TransactionWindow_PreC0 | CONDITION | PRE | NONE | ANY | PERMIT | preC0 | ACTIVE |
| P03_ClassStatusOpen_PreA0 | AUTHORIZATION | PRE | NONE | REGISTER | PERMIT | preA0 | ACTIVE |
| P04_NotAlreadyRegistered_PreA0 | AUTHORIZATION | PRE | NONE | REGISTER | PERMIT | preA0 | ACTIVE |
| P16_DropOnlyIfRegistered_PreA0 | AUTHORIZATION | PRE | NONE | DROP | PERMIT | preA0 | ACTIVE |
| P21_DropWindow_PreC0 | CONDITION | PRE | NONE | DROP | PERMIT | preC0 | ACTIVE |
| P26_MaxDropTimes_PreA0 | AUTHORIZATION | PRE | NONE | DROP | PERMIT | preA0 | ACTIVE |
| P05_CreditLimit_PreA0 | AUTHORIZATION | PRE | NONE | REGISTER | PERMIT | preA0 | ACTIVE |
| P25_MaxRegisterAttempts_PreA0 | AUTHORIZATION | PRE | NONE | REGISTER | PERMIT | preA0 | ACTIVE |
| P06_Prerequisite_PreA0 | AUTHORIZATION | PRE | NONE | REGISTER | PERMIT | preA0 | ACTIVE |
| P17_AgreeRegistrationRule_PreB0 | OBLIGATION | PRE | NONE | REGISTER | PERMIT | preB0 | ACTIVE |
| P07_ScheduleConflict_PreA0 | AUTHORIZATION | PRE | NONE | REGISTER | PERMIT | preA0 | ACTIVE |
| P18_AdminOverrideReason_PreB0 | OBLIGATION | PRE | NONE | REGISTER | PERMIT | preB0 | ACTIVE |
| P19_RegisterAttempt_PreA1 | AUTHORIZATION | PRE | PRE | REGISTER | PERMIT | preA1 | ACTIVE |
| P13_EmergencyMaintenance_OnC0 | CONDITION | ONGOING | NONE | ANY | PERMIT | onC0 | ACTIVE |
| P08_CapacityRecheck_OnA0 | AUTHORIZATION | ONGOING | NONE | REGISTER | PERMIT | onA0 | ACTIVE |
| P20_ReserveSeat_OnA2 | AUTHORIZATION | ONGOING | ONGOING | REGISTER | PERMIT | onA2 | ACTIVE |
| P23_DropLockedClass_OnA0 | AUTHORIZATION | ONGOING | NONE | DROP | PERMIT | onA0 | ACTIVE |
| P09_ClassStatusRecheck_OnA0 | AUTHORIZATION | ONGOING | NONE | REGISTER | PERMIT | onA0 | ACTIVE |
| P27_SessionLease_OnB0 | OBLIGATION | ONGOING | NONE | ANY | PERMIT | onB0 | ACTIVE |
| P10_StudentHoldRecheck_OnA0 | AUTHORIZATION | ONGOING | NONE | REGISTER | PERMIT | onA0 | ACTIVE |
| P11_RegisterStateUpdate_PostA3 | AUTHORIZATION | POST | POST | REGISTER | PERMIT | postA3 | ACTIVE |
| P14_DropStateRevert_PostA3 | AUTHORIZATION | POST | POST | DROP | PERMIT | postA3 | ACTIVE |
| P12_AuditAndTrace_PostB3 | OBLIGATION | POST | POST | ANY | PERMIT | postB3 | ACTIVE |