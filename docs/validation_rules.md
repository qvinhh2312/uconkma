# Validation Rules

Tai lieu nay tong hop cac luat validation va invariant hien co trong project.

## 1. Semantic validation cho policy model

`PolicyModelSemanticValidator` dang kiem tra cac nhom luat sau.

### Rule UCON-generic

- `CONDITION` khong duoc co update block.
- `REQUEST` va `ENVIRONMENT` khong duoc update.
- Chi cac path mutable moi duoc cap nhat.
- `updateTiming` phai phu hop voi update block:
  - `NONE` -> khong co update
  - `PRE` -> chi co `preUpdates`
  - `ONGOING` -> phai co `ongoingUpdates` va `rollbackUpdates`
  - `POST` -> phai co `postUpdates`
- `policyId` khong duoc trung.
- `policySet` khong duoc tham chieu policy khong ton tai.
- function call phai nam trong whitelist va dung arity.
- root `condition` phai tra ve boolean.
- `AuditLogStatement` va `Transaction` statement phai dung so luong tham so.

### Rule domain cho dang ky hoc phan

- `DROP` phai co guard truoc (`P16`).
- `Registration` khong duoc tao trung.
- path typo trong `subject/object/environment/request` bi chan ngay.
- path cap nhat vao field bat bien nhu `capacity`, `studentId`, `isMaintenance` bi chan.

## 2. Attribute schema

File:

- `engine/src/main/resources/attribute-schema.yml`

Schema nay khai bao:

- thuoc tinh nao la mutable
- kieu du lieu mong doi
- scope `subject`, `object`, `environment`, `request`

Schema giup `PolicyValidator` bat cac loi cap nhat vao field immutable.

## 3. Runtime invariant

`DomainInvariantChecker` dang bao ve cac bat bien runtime chinh:

- `object.enrolled >= 0`
- `object.enrolled <= object.capacity`
- `object.reservedSeats >= 0`
- `subject.currentCredits >= 0`
- `subject.currentCredits <= subject.maxCreditsEffective`
- `subject.tuitionDebt >= 0`

## 4. Policy analyzer

`PolicyAnalyzer` hien tai sinh canh bao cho:

- `MISSING_ROLLBACK`
- `MISSING_AUDIT`
- `DROP_GUARD_MISSING`
- `STATEFUL_MUTATION`

Day la lop phan tich chat luong policy, khac voi validator dung/sai.
