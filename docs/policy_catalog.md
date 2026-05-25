# Policy Catalog

Bang nay giu nguyen `policyId` thuc te trong repo de tranh lam vo test va trace runtime.
Tat ca policy runtime hien tai deu co metadata chung:

- `source = "Quy che dang ky hoc phan KMA"`
- `version = "1.0"`
- `policyStatus = ACTIVE`

| Policy ID thuc te | Vai tro nghiep vu | UCON variant | Status |
|---|---|---|---|
| `P01_TuitionPaid_PreA0` | Chan dang ky khi chua dong hoc phi | `preA0` | `ACTIVE` |
| `P13a_EmergencyMaintenance_PreC0` | Chan giao dich neu he thong dang maintenance truoc request | `preC0` | `ACTIVE` |
| `P02_TransactionWindow_PreC0` | Chi cho giao dich trong dot/gio hop le | `preC0` | `ACTIVE` |
| `P03_ClassStatusOpen_PreA0` | Lop phai dang `OPEN` moi duoc dang ky | `preA0` | `ACTIVE` |
| `P04_NotAlreadyRegistered_PreA0` | Khong cho dang ky trung | `preA0` | `ACTIVE` |
| `P16_DropOnlyIfRegistered_PreA0` | Chi cho DROP khi da ton tai dang ky | `preA0` | `ACTIVE` |
| `P21_DropWindow_PreC0` | DROP chi hop le trong khung thoi gian cho phep | `preC0` | `ACTIVE` |
| `P26_MaxDropTimes_PreA0` | Gioi han so lan DROP trong hoc ky | `preA0` | `ACTIVE` |
| `P05_CreditLimit_PreA0` | Gioi han tong tin chi | `preA0` | `ACTIVE` |
| `P25_MaxRegisterAttempts_PreA0` | Gioi han so lan thu REGISTER | `preA0` | `ACTIVE` |
| `P06_Prerequisite_PreA0` | Kiem tra mon tien quyet | `preA0` | `ACTIVE` |
| `P17_AgreeRegistrationRule_PreB0` | Sinh vien phai xac nhan quy che | `preB0` | `ACTIVE` |
| `P07_ScheduleConflict_PreA0` | Khong cho trung lich | `preA0` | `ACTIVE` |
| `P18_AdminOverrideReason_PreB0` | Override hoc vu phai co ly do | `preB0` | `ACTIVE` |
| `P19_RegisterAttempt_PreA1` | Tang bo dem register attempt | `preA1` | `ACTIVE` |
| `P13_EmergencyMaintenance_OnC0` | Maintenance trong luc xu ly | `onC0` | `ACTIVE` |
| `P08_CapacityRecheck_OnA0` | Re-check suc chua o sat commit | `onA0` | `ACTIVE` |
| `P20_ReserveSeat_OnA2` | Giu tam cho va rollback neu can | `onA2` | `ACTIVE` |
| `P23_DropLockedClass_OnA0` | Chan DROP neu lop bi khoa giua xu ly | `onA0` | `ACTIVE` |
| `P09_ClassStatusRecheck_OnA0` | Re-check trang thai lop | `onA0` | `ACTIVE` |
| `P10_StudentHoldRecheck_OnA0` | Re-check hold cua sinh vien | `onA0` | `ACTIVE` |
| `P27_SessionLease_OnB0` | Usage session lease phai con hop le | `onB0` | `ACTIVE` |
| `P11_RegisterStateUpdate_PostA3` | Commit REGISTER vao state he thong | `postA3` | `ACTIVE` |
| `P14_DropStateRevert_PostA3` | Commit DROP va hoan tac state | `postA3` | `ACTIVE` |
| `P12_AuditAndTrace_PostB3` | Audit/trace bat buoc sau moi request | `postB3` | `ACTIVE` |

## Luu y ve ten goi

- ID `P12` la policy audit/trace vi giu lai su tuong thich voi test va trace da co.
- ID `P16` dang duoc dung cho guard `DROP only if registered`.
- ID `P17`, `P18`, `P27` la cac policy thuoc nhom `Obligation`.
- Repo uu tien tinh on dinh cua runtime va traceability theo ID thuc te hon la doi lai so thu tu cho dep.
