# Glossary va Assumptions

## 1. Glossary

- `UCON`: mo hinh kiem soat truy cap theo thuoc tinh, cho phep danh gia truoc, trong va sau khi su dung quyen.
- `Subject`: thuc the khoi tao giao dich. Trong project hien tai la `Student`.
- `Object`: thuc the bi tac dong. Trong project hien tai la `ClassSection`.
- `Right / Action`: quyen thao tac. Trong project hien tai la `REGISTER`, `DROP`, hoac `ANY`.
- `Environment`: ngu canh he thong nhu hoc ky, thoi gian mo cong, trang thai maintenance.
- `PolicyModel`: root model cua file XMI.
- `Policy`: mot chinh sach rieng le trong policy model.
- `Expression`: cay dieu kien cua policy.
- `Statement`: hau lenh duoc dung trong `POST`.
- `PEP`: `Policy Enforcement Point`, o project la `RegistrationController`.
- `PDP`: `Policy Decision Point`, o project la `PolicyDecisionPoint` ket hop voi `PolicyEngine`.
- `PIP`: `Policy Information Point`, o project la cac entity va repository cung cap trang thai hien tai.
- `Metamodel`: schema cua ngon ngu, nam o `metamodel/ucon.ecore`.
- `Model instance`: instance cu the cua metamodel, nam o `xmi/ucon_policy.xmi`.
- `DSL`: file van ban chinh sach, nam o `dsl/ucon_policy.dsl`.

## 2. Assumptions cua project hien tai

### 2.1 Domain scope
Project duoc thiet ke rieng cho bai toan dang ky hoc phan KMA, khong phai framework UCON tong quat cho moi mien nghiep vu.

### 2.2 Time format
Cac gia tri thoi gian trong `Environment` dang duoc bieu dien duoi dang chuoi ISO-8601:

- `yyyy-MM-dd`
- hoac `yyyy-MM-ddTHH:mm:ss`

Runtime chi cho phep so sanh khi parse duoc ve `LocalDate` hoac `LocalDateTime`.

### 2.3 Registration source of truth
Tinh ton tai cua mot dang ky hop le duoc xac dinh boi bang `Registration`, khong phai boi chuoi `registeredClassIds` trong `Student`.

### 2.4 Drop semantics
`DROP` chi hop le neu da co dang ky tuong ung trong `Registration`. Logic nay duoc bao ve boi `P16_DropOnlyIfRegistered_PreA0`.

### 2.5 Tuition semantics
`tuitionPaid` chi chan `REGISTER`, khong chan `DROP`.

### 2.6 Hold semantics
`holds` hien chi chan `REGISTER` o pha ongoing.

### 2.7 Maintenance semantics
Maintenance duoc kiem soat o hai pha:

- `P13a_EmergencyMaintenance_PreC0`
- `P13_EmergencyMaintenance_OnC0`

Dieu nay vua cho phep fail-fast, vua the hien dung tinh than continuity cua UCON.

### 2.8 Audit semantics
Audit hien dung mot request field duy nhat la:

- `request.requestId`

Khong con dung alias `request.id`.

### 2.9 Post-update semantics
Billing va refund da duoc gop vao hai policy mutation chinh:

- `P11_RegisterStateUpdate_PostA3`
- `P14_DropStateRevert_PostA3`

### 2.10 Semantic validation
Project dung Java semantic validator thay cho OCL/WFR thuan tuy, nhung validator dang kiem tra cac invariant chinh:

- path hop le
- binding hop le
- rule family hop le
- statement schema hop le
- mutability hop le
- startup fail-fast neu model sai
