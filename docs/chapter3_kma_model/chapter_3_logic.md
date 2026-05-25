# Chuong 3: Mo hinh logic nghiep vu va tap policy UCON hien tai

Chuong nay mo ta domain model va tap chinh sach dang thuc su duoc dung trong project. Noi dung da duoc dong bo voi:

- `dsl/ucon_policy.dsl`
- `metamodel/ucon.ecore`
- `xmi/ucon_policy.xmi`
- runtime engine trong module `engine`

## 1. Domain model o muc nghiep vu

### 1.1 Student
Thuoc tinh chinh:

- `studentId`
- `tuitionPaid`
- `currentCredits`
- `maxCreditsEffective`
- `completedCourses`
- `registeredScheduleSlots`
- `registeredClassIds`
- `holds`
- `tuitionDebt`

### 1.2 Course
Thuoc tinh chinh:

- `courseId`
- `credits`
- `prerequisites`
- `tuitionFee`

### 1.3 ClassSection
Thuoc tinh chinh:

- `classId`
- `course`
- `capacity`
- `enrolled`
- `status`
- `scheduleSlots`

Trang thai lop hien dung trong policy la:

- `OPEN`
- `LOCKED`
- `CANCELLED`

### 1.4 Registration
Trong DSL, entity tru tuong duoc goi la `Transaction`. Trong persistence layer, project luu bang entity `Registration` de tranh dung ten voi transaction cua framework.

Thuoc tinh chinh:

- `studentId`
- `classId`
- `semester`
- `actionType`

Project hien co unique constraint theo bo:

- `(studentId, classId, semester)`

### 1.5 Environment
Thuoc tinh chinh:

- `registrationPhase`
- `currentDateTime`
- `openTime`
- `closeTime`
- `semester`
- `isMaintenance`

## 2. Tap policy hien hanh
Project hien co 25 ACTIVE policy. Ma policy khong lien tuc tuyet doi vi tap chinh sach da duoc mo rong va tai cau truc theo qua trinh phat trien.

## 3. PRE

### P01_TuitionPaid_PreA0
- Muc dich: chan dang ky moi neu sinh vien chua hoan tat hoc phi.
- Pham vi: chi ap cho `REGISTER`.
- Dieu kien: `subject.tuitionPaid == true`

### P13a_EmergencyMaintenance_PreC0
- Muc dich: fail-fast ngay tu dau neu he thong dang bao tri.
- Pham vi: ap cho `ANY`.
- Dieu kien: `environment.isMaintenance == false`

### P02_TransactionWindow_PreC0
- Muc dich: chi cho phep giao dich trong dot va khung gio hop le.
- Pham vi: ap cho `ANY`.
- Deny code: `OUTSIDE_TRANSACTION_WINDOW`
- Dieu kien:
  - `registrationPhase IN ["NORMAL", "LATE"]`
  - `currentDateTime >= openTime`
  - `currentDateTime <= closeTime`

### P03_ClassStatusOpen_PreA0
- Muc dich: chi cho dang ky vao lop dang mo.
- Pham vi: `REGISTER`
- Dieu kien: `object.status == "OPEN"`

### P04_NotAlreadyRegistered_PreA0
- Muc dich: chan dang ky trung.
- Pham vi: `REGISTER`
- Dieu kien: `NOT checkExistsRegistration(subject.studentId, object.classId, environment.semester)`
- Ghi chu: project hien dung `RegistrationRepository` lam nguon su that, khong dua vao chuoi `registeredClassIds`.

### P16_DropOnlyIfRegistered_PreA0
- Muc dich: chi cho phep huy lop khi sinh vien thuc su da co dang ky hop le.
- Pham vi: `DROP`
- Dieu kien: `checkExistsRegistration(subject.studentId, object.classId, environment.semester)`

### P05_CreditLimit_PreA0
- Muc dich: chan vuot tran tin chi.
- Pham vi: `REGISTER`
- Dieu kien: `(subject.currentCredits + object.course.credits) <= subject.maxCreditsEffective`

### P06_Prerequisite_PreA0
- Muc dich: kiem tra mon tien quyet.
- Pham vi: `REGISTER`
- Dieu kien: `object.course.prerequisites SUBSET_OF subject.completedCourses`

### P07_ScheduleConflict_PreA0
- Muc dich: chan trung lich hoc.
- Pham vi: `REGISTER`
- Dieu kien: `NOT (object.scheduleSlots OVERLAPS subject.registeredScheduleSlots)`

## 4. ONGOING

### P08_CapacityRecheck_OnA0
- Muc dich: chong race condition o suat cuoi.
- Pham vi: `REGISTER`
- Dieu kien: `object.enrolled < object.capacity`

### P09_ClassStatusRecheck_OnA0
- Muc dich: re-check trang thai lop o sat thoi diem commit.
- Pham vi: `REGISTER`
- Dieu kien: `object.status == "OPEN"`

### P10_StudentHoldRecheck_OnA0
- Muc dich: chan sinh vien dang bi hold o pha ongoing.
- Pham vi: `REGISTER`
- Dieu kien: `isEmpty(subject.holds)`

### P13_EmergencyMaintenance_OnC0
- Muc dich: ngat giao dich dang lo lung neu he thong chuyen sang trang thai bao tri giua chung.
- Pham vi: `ANY`
- Dieu kien: `environment.isMaintenance == false`

## 5. POST

### P11_RegisterStateUpdate_PostA3
- Muc dich: commit toan bo hau qua cua mot lan dang ky thanh cong.
- Pham vi: `REGISTER`
- Rule family: `MUTATION`
- Hanh dong:
  - tao `Transaction`
  - tang `object.enrolled`
  - tang `subject.currentCredits`
  - them `scheduleSlots`
  - them `classId`
  - tang `subject.tuitionDebt`

### P14_DropStateRevert_PostA3
- Muc dich: hoan tac trang thai khi huy lop.
- Pham vi: `DROP`
- Rule family: `MUTATION`
- Hanh dong:
  - xoa `Transaction`
  - giam `object.enrolled`
  - giam `subject.currentCredits`
  - xoa `scheduleSlots`
  - xoa `classId`
  - giam `subject.tuitionDebt`

### P12_AuditAndTrace_PostB3
- Muc dich: ghi audit log cho moi request.
- Pham vi: `ANY`
- Rule family: `TRACE`
- Hanh dong:
  - `create AuditLog(request.requestId, subject.studentId, object.classId, request.decision, request.failedPolicyCodes)`

## 6. Phan nhom theo muc tieu kiem soat

### 6.1 Nhom kiem soat dieu kien nghiep vu
- `P01`
- `P02`
- `P03`
- `P05`
- `P06`
- `P07`
- `P10`

### 6.2 Nhom kiem soat tinh nhat quan va an toan giao dich
- `P04`
- `P08`
- `P09`
- `P13a`
- `P13`
- `P16`

### 6.3 Nhom cap nhat trang thai va truy vet
- `P11`
- `P12`
- `P14`

## 7. Diem dang chu y cua tap policy hien tai
- `P02` da duoc doi nghia thanh transaction window chung, khong con chi la registration window.
- `P01` chi chan `REGISTER`, khong chan `DROP`.
- `P10` chi chan `REGISTER`, khong chan `DROP`.
- `P11` va `P14` da hap thu phan billing/refund thay cho cap `P15a/P15b` truoc day.
- `P13a` va `P13` tao thanh cap kiem soat maintenance o ca dau request va ongoing.
- `P16` lam luong `DROP` doi xung hon voi `P04`.

## 8. Ket luan
Tap policy hien tai phan anh dung pham vi do an o thoi diem chot:

- 25 ACTIVE policy dang hoat dong thuc te
- co day du `PRE`, `ONGOING`, `POST`
- co mutation state, audit, kiem soat race condition va maintenance
- co semantic binding ro cho `subjectType`, `objectType`, `ruleFamily`
