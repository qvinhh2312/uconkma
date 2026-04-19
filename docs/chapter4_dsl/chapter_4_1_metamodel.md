# Chuong 4.1: Metamodel EMF cua DSL UCON

Muc tieu cua metamodel trong `metamodel/ucon.ecore` la mo ta chinh xac cau truc du lieu ma parser tao ra va runtime engine tieu thu. Ban hien tai khong con la phien ban toi gian ban dau; no da duoc mo rong de phan anh tot hon binding cua policy va semantic validation o runtime.

## 1. Root model

### PolicyModel
`PolicyModel` la root cua file XMI, chua danh sach:

- `policies: Policy[*]`

## 2. Lop Policy
`Policy` la thuc the trung tam cua metamodel. Cac truong chinh:

- `policyId`
- `name`
- `description`
- `subjectType`
- `objectType`
- `ruleFamily`
- `type`
- `targetAction`
- `priority`
- `effect`
- `denyReason`
- `condition`
- `postUpdates`

Ba truong moi co y nghia quan trong voi ban hien tai:

- `subjectType`
- `objectType`
- `ruleFamily`

Chung giup policy noi ro:

- dang kiem soat loai subject nao
- dang tac dong toi loai object nao
- thuoc ho rule nao: `AUTHORIZATION`, `MUTATION`, `TRACE`

Nho do, validator co the kiem tra chat hon thay vi chi nhin `type`.

## 3. Enum chinh

### PolicyType
- `PRE_AUTHORIZATION`
- `ONGOING_AUTHORIZATION`
- `POST_UPDATE`

### ActionType
- `REGISTER`
- `DROP`
- `ANY`

### PolicyEffect
- `PERMIT`
- `DENY`

### EntityScope
- `SUBJECT`
- `OBJECT`
- `ENVIRONMENT`
- `REQUEST`

### DataType
- `STRING`
- `INTEGER`
- `BOOLEAN`
- `ENUM`

### LogicalOp
- `AND`
- `OR`
- `NOT`

### RelationalOp
- `EQUALS`
- `NOT_EQUALS`
- `GREATER_THAN`
- `LESS_THAN`
- `GREATER_OR_EQUALS`
- `LESS_OR_EQUALS`
- `IN`
- `CONTAINS`
- `NOT_CONTAINS`
- `SUBSET_OF`
- `OVERLAPS`

### ArithmeticOp
- `ADD`
- `SUBTRACT`

### AssignmentOp
- `ASSIGN`
- `ADD_ASSIGN`
- `SUB_ASSIGN`
- `APPEND`
- `REMOVE`

## 4. Khoi Expression
Metamodel bieu dien dieu kien policy bang cay bieu thuc.

### Expression
Lop tru tuong goc.

### LogicalOperator
Bieu dien `AND`, `OR`, `NOT`.

### RelationalOperator
Bieu dien cac phep so sanh va tap hop nhu:

- `==`
- `!=`
- `>`
- `>=`
- `<`
- `<=`
- `IN`
- `SUBSET_OF`
- `OVERLAPS`

### ArithmeticOperator
Bieu dien:

- cong
- tru

### VariableAccess
Moi `VariableAccess` gom:

- `entity`
- `path`

Vi du:

- `subject.currentCredits`
- `object.course.credits`
- `environment.semester`
- `request.requestId`

### Constant
Bieu dien hang so don.

### ListConstant
Bieu dien danh sach hang so, vi du:

- `["NORMAL", "LATE"]`

### FunctionCall
Bieu dien loi goi ham DSL, hien duoc runtime ho tro qua function registry:

- `isEmpty(...)`
- `checkExistsRegistration(...)`

## 5. Khoi Statement cho POST_UPDATE

### Statement
Lop tru tuong goc cho cac hau lenh.

### UpdateStatement
Mo ta cap nhat trang thai:

- `target`
- `operator`
- `value`

### CreateTransactionStatement
Dung de sinh ban ghi giao dich tru tuong `Transaction`.

### DeleteTransactionStatement
Dung de xoa ban ghi giao dich tru tuong `Transaction`.

### AuditLogStatement
Dung de tao audit log o cuoi request.

## 6. Y nghia cua metamodel hien tai doi voi runtime
Metamodel dang dong dung vai tro nguon su that cho engine:

- DSL duoc parse sang EObject theo `ucon.ecore`
- XMI duoc sinh ra tu cung cau truc do
- PDP load lai XMI
- semantic validator kiem tra policy model truoc khi engine cho phep khoi dong

Vi vay, `ucon.ecore` khong chi la so do tai lieu, ma la schema thuc thi cua project.

## 7. Semantic constraints gan voi metamodel
Project khong dung OCL thuan tuy, nhung dang hien thuc semantic constraints bang Java validator theo dung tinh than WFR:

- binding invariants:
  - `subjectType`, `objectType`, `ruleFamily` phai ton tai va thuoc allowlist
- compatibility invariants:
  - `PRE/ONGOING` phai di voi `AUTHORIZATION`
  - `POST_UPDATE` phai di voi `MUTATION` hoac `TRACE`
- mutability invariants:
  - `ENVIRONMENT` va `REQUEST` khong duoc update
  - chi mot so path cua `SUBJECT` va `OBJECT` duoc phep mutate
- statement-schema invariants:
  - `Transaction` va `AuditLog` phai co dung arity va dung scope
- path invariants:
  - `VariableAccess` trong condition va update phai tro toi getter hop le
- startup invariants:
  - neu model sai semantic thi PDP fail-fast

## 8. Ket luan
Metamodel hien tai da tien them mot buoc so voi ban dau:

- khong chi mo hinh hoa expression tree
- ma con mo ta ro binding cua policy
- va ho tro semantic validation du chat de dung truc tiep trong runtime
