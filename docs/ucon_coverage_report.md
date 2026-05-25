# UCON Coverage Report

Tai lieu nay tong hop do bao phu hien tai cua `UCONKMA` theo bien the UCONABC.

## Covered variants

- `preA0`
  - hoc phi, duplicate, gioi han tin chi, tien quyet, xung dot lich, gioi han so lan thu, gioi han so lan DROP
- `preA1`
  - tang `registerAttemptCount` truoc khi request tiep tuc
- `preB0`
  - xac nhan quy che dang ky
  - override phai co ly do
- `preC0`
  - dot dang ky / transaction window
  - maintenance truoc request
  - drop window
- `onA0`
  - re-check suc chua lop
  - re-check trang thai lop
  - re-check hold
  - drop bi chan neu lop bi khoa giua xu ly
- `onA2`
  - `reservedSeats` duoc giu tam o pha ongoing
  - co `rollbackUpdates` de hoan tac
- `onB0`
  - `P27_SessionLease_OnB0` yeu cau usage session van hop le trong luc xu ly
- `onC0`
  - maintenance giua qua trinh xu ly
- `postA3`
  - cap nhat `enrolled`, `currentCredits`, `registeredClassIds`, `registeredScheduleSlots`, `tuitionDebt`
  - xoa / tao `Registration`
  - hoan tac state khi `DROP`
- `postB3`
  - tao `AuditLog` cho ca `ALLOW` va `DENY`

## Covered concepts

- `Authorization`
- `Obligation`
- `Condition`
- mutable attributes
- rollback cho ongoing update
- `UsageSession` voi `ACTIVE / COMMITTED / REVOKED / FAILED`
- decision trace va audit trace
- semantic validation va runtime invariant check

## Not covered / partial

- full `onB1`, `onB2`, `onB3`
- full `preC1/2/3` va cac bien the UCONABC khong can thiet cho bai toan dang ky hoc phan
- continuous long-running monitoring cho mot session dai han
- event-driven revoke sau khi request da roi khoi transaction runtime
- benchmark so luong policy lon
- policy lifecycle/PAP day du

## Ket luan

Do an hien tai khong co tham vong bao phu may moc toan bo 24 bien the con cua UCONABC. Thay vao do, no bao phu nhung bien the co y nghia nhat cho bai toan dang ky hoc phan:

- `preA0`, `preA1`
- `preB0`
- `preC0`
- `onA0`, `onA2`
- `onB0`
- `onC0`
- `postA3`
- `postB3`

Day la muc bao phu du de bao ve mot he thong UCON co `Authorization`, `Obligation`, `Condition`, `mutability`, `continuity`, `traceability` va `verification`.
