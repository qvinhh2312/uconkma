# Chuong 1 va 2: Co so ly thuyet va phan tich bai toan KMA theo UCON

## 1. Vi sao UCON phu hop voi dang ky hoc phan
RBAC truyen thong phu hop voi viec gan quyen theo vai tro, nhung bai toan dang ky hoc phan tai KMA phu thuoc manh vao trang thai dong:

- si so lop thay doi theo tung giao dich
- tin chi hien tai cua sinh vien thay doi sau moi lan dang ky hoac huy
- trang thai hold, hoc phi, bao tri he thong co the thay doi giua luc request bat dau va luc commit
- trang thai sau khi cap quyen phai duoc cap nhat ngay de anh huong toi cac request tiep theo

Vi vay, mo hinh phu hop hon la UCON, noi quyet dinh duoc danh gia theo thuoc tinh va co the bi tu choi o nhieu pha:

- `PRE`
- `ONGOING`
- `POST`

Trong project nay, UCON duoc hien thuc hoa theo kien truc:

- DSL chinh sach trong `dsl/ucon_policy.dsl`
- metamodel EMF trong `metamodel/ucon.ecore`
- XMI thuc thi trong `xmi/ucon_policy.xmi`
- runtime engine theo mo hinh `PEP/PDP/PIP`

## 2. Anh xa bai toan KMA sang cac thanh phan UCON

### 2.1 Subject
`Student` la chu the thuc hien giao dich. Cac thuoc tinh chinh dang duoc dung trong policy:

- `studentId`
- `tuitionPaid`
- `currentCredits`
- `maxCreditsEffective`
- `completedCourses`
- `registeredScheduleSlots`
- `registeredClassIds`
- `holds`
- `tuitionDebt`

### 2.2 Object
`ClassSection` la doi tuong bi tac dong. Cac thuoc tinh chinh:

- `classId`
- `capacity`
- `enrolled`
- `status`
- `scheduleSlots`
- `course.credits`
- `course.prerequisites`
- `course.tuitionFee`

### 2.3 Right / Action
Project hien thuc quyen duoi dang `targetAction` trong policy:

- `REGISTER`
- `DROP`
- `ANY`

`ANY` duoc dung cho cac policy ap dung chung cho ca hai luong, vi du maintenance hoac transaction window.

### 2.4 Environment
`Environment` mo ta ngu canh he thong:

- `registrationPhase`
- `currentDateTime`
- `openTime`
- `closeTime`
- `semester`
- `isMaintenance`

Gia tri thoi gian hien duoc bieu dien bang chuoi ISO-8601 va runtime chi cho phep so sanh khi parse duoc thanh `LocalDate` hoac `LocalDateTime`.

## 3. Cau truc policy dang dung trong project
Project hien tai co 25 ACTIVE policy, chia thanh 3 nhom:

### 3.1 PRE
- `P01_TuitionPaid_PreA0`
- `P13a_EmergencyMaintenance_PreC0`
- `P02_TransactionWindow_PreC0`
- `P03_ClassStatusOpen_PreA0`
- `P04_NotAlreadyRegistered_PreA0`
- `P16_DropOnlyIfRegistered_PreA0`
- `P05_CreditLimit_PreA0`
- `P06_Prerequisite_PreA0`
- `P07_ScheduleConflict_PreA0`

### 3.2 ONGOING
- `P08_CapacityRecheck_OnA0`
- `P09_ClassStatusRecheck_OnA0`
- `P10_StudentHoldRecheck_OnA0`
- `P13_EmergencyMaintenance_OnC0`

### 3.3 POST
- `P11_RegisterStateUpdate_PostA3`
- `P14_DropStateRevert_PostA3`
- `P12_AuditAndTrace_PostB3`

Luu y: project hien khong con tach rieng `P15a/P15b`. Phan billing va refund da duoc gop vao:

- `P11_RegisterStateUpdate_PostA3`
- `P14_DropStateRevert_PostA3`

## 4. Tinh than UCON duoc giu trong do an
Project khong co gang hien thuc toan bo taxonomy cua paper, nhung giu dung cac diem co gia tri nhat cho domain dang ky hoc phan:

- quyet dinh khong chi xay ra mot lan o dau request
- trang thai duoc re-check o pha ongoing
- quyet dinh duoc gan voi mutation state ngay sau khi cho phep
- audit duoc ghi lai nhu mot concern rieng

## 5. Pham vi hien tai cua do an
Day la mot he UCON chuyen biet cho dang ky hoc phan KMA, khong phai framework UCON tong quat cho moi mien nghiep vu. Vi vay:

- policy model duoc thiet ke xoay quanh `Student`, `ClassSection`, `Environment`
- function registry hien chi chua cac ham can cho domain hien tai
- semantic constraints duoc hien thuc bang Java validator thay vi OCL/WFR thuan tuy

Pham vi nay phu hop voi muc tieu do an: nghien cuu va xay dung chinh sach UCON cho quy trinh dang ky hoc phan.
