# Kich Ban Demo Frontend 5 Phut

Tai lieu nay dung cho mini app `frontend/` khi bao ve do an UCONKMA.

## 1. Chuan bi

Backend:

```powershell
cd engine
mvn spring-boot:run
```

Frontend:

```powershell
cd frontend
npm install
npm run dev
```

Mo:

```text
http://localhost:5173
```

## 2. Dashboard

Mo `Dashboard`.

Noi can noi:

- He thong co `25` policy UCON dang ACTIVE.
- Policy duoc chia theo `AUTHORIZATION`, `OBLIGATION`, `CONDITION`.
- Coverage co cac variant `preA0`, `preA1`, `preB0`, `preC0`, `onA0`, `onA2`, `onB0`, `onC0`, `postA3`, `postB3`.
- App nay chi la demo policy enforcement, khong phai LMS day du.

## 3. Demo Obligation Fail

Mo `Register / Drop Simulator`.

Thiet lap:

- Action: `REGISTER`
- `studentId`: `SV001`
- `classId`: `CS102_01`
- Bo tick `confirmedRegistrationRule`
- `sessionLeaseValid`: tick

Submit.

Ky vong:

- `decision = DENY`
- `phase = PRE`
- `predicate = OBLIGATION`
- `failedPolicy = P17_AgreeRegistrationRule_PreB0`
- `denyReason = REGULATION_NOT_CONFIRMED`

Y nghia:

- Chung minh `preB0 obligation`.
- Request bi chan boi policy, khong phai hard-code trong controller.

## 4. Demo Register Success

Van o `Register / Drop Simulator`.

Thiet lap:

- Tick `confirmedRegistrationRule`
- `studentId = SV001`
- `classId = CS102_01`
- `sessionLeaseValid = true`

Submit.

Ky vong:

- `decision = PERMIT` hoac `COMMITTED`
- Trace co `PRE`, `ONGOING`, `POST`
- Snapshot before/after co thay doi mutable attributes neu backend response tra ve
- Session status la `COMMITTED`

Y nghia:

- Chung minh mutability va post update: enrolled, currentCredits, tuitionDebt, audit.

## 5. Demo Drop Fail

Chuyen action sang `DROP`.

Co the dung sinh vien/lop chua co registration hop le.

Ky vong:

- `decision = DENY`
- `failedPolicy = P16_DropOnlyIfRegistered_PreA0`
- `phase = PRE`
- `predicate = AUTHORIZATION`

Y nghia:

- DROP rule nam trong DSL/PDP, khong hard-code trong controller.

## 6. Demo Monitoring / Revoke

Mo `Monitoring Demo`.

Thu cac thao tac:

1. `Maintenance ON`
2. `Class Status`: `CS102_01 -> LOCKED`
3. `Student Hold`: `SV001 -> ACADEMIC_HOLD`
4. `Manual Recheck`

Ky vong:

- Response co `checkedSessions`
- Response co `revokedSessions`
- Khi ONGOING policy fail, session co the bi `REVOKED`

Y nghia:

- Chung minh UCON continuity: he thong khong chi kiem tra tai thoi diem PRE.
- Khi environment/object/subject thay doi, active sessions duoc re-check.

## 7. Demo Policy Explorer

Mo `Policy Explorer`.

Filter:

- Phase: `ONGOING`

Chi ra:

- `P13_EmergencyMaintenance_OnC0`
- `P20_ReserveSeat_OnA2`
- `P27_SessionLease_OnB0`

Y nghia:

- Policy co phase/predicate/updateTiming/uconVariant ro rang.
- Day la artifact policy, khong phai code controller.

## 8. Demo PAP Lifecycle

Mo `PAP Lifecycle`.

Noi can noi:

- PAP list policy metadata tu backend.
- Runtime chi load `ACTIVE` policies.
- Transition sai status se tra JSON error thong nhat qua `GlobalExceptionHandler`.

Nen thao tac nhe:

- Chi `Reload policy model` neu khong muon thay doi lifecycle policy that trong luc demo.

## 9. Demo Validation Report

Mo `Validation Report`.

Noi can noi:

- DSL policies: `25`
- XMI policies: `25`
- Engine tests: `65 pass`
- DSL tests: `3 pass`
- Line coverage: `83.21%`
- Branch coverage: `62.18%`

## 10. Ket luan khi demo

Thong diep chinh:

- UCONKMA minh hoa duoc `A/B/C`, `PRE/ONGOING/POST`, mutable attributes, rollback hooks, session status va decision trace.
- Mini app khong mo rong thanh LMS day du, ma tap trung dung vao enforcement va explainability cua policy model.
