# Chương 4: Đặc tả Ngôn ngữ Chính sách (DSL & XMI Metamodel)

Mục tiêu của chương này là triển khai lý thuyết Model-Driven Engineering (MDE) để xây dựng lược đồ dữ liệu cơ sở (Metamodel) biểu diễn các quy tắc kiểm soát truy cập Usage Control (UCON).

## 1. Thiết kế Metamodel Abstract Syntax (`ucon.ecore`) (Bước 4)

Lược đồ (Schema) Ecore là khung sườn "Abstract Syntax Tree (AST)" bắt buộc mà bất kỳ Parser nào khi đọc mã nguồn Text DSL cũng phải tuần tự dịch ra.

File thiết kế `ucon.ecore` được cấu trúc bao gồm 4 khối (Domains) chính đê khớp hoàn toàn với 12 logic của Bước 3:

### 1.1 Khối Core Policy (Gốc)

- **`PolicyModel`**: Gốc của toàn bộ file XMI (root node). Chứa một tập danh sách các Policies.
- **`Policy`**: Node biểu diễn một chính sách độc lập. Gồm các thuộc tính tĩnh:
  - `policyId`, `description`, `denyReason` (String)
  - `priority`: Độ ưu tiên của luật khi System có nhiều luật (Integer). Evaluate từ priority cao xuống thấp.
  - `type`: Enum `PolicyType` (PRE_AUTHORIZATION, ONGOING_AUTHORIZATION, POST_UPDATE)
  - `targetAction`: Enum `ActionType` (REGISTER, DROP, ANY)
  - `effect`: Enum `PolicyEffect` (PERMIT, DENY) quản lý quyết định ngầm định.
  - Hai nhánh con đệ quy: `condition` (chỉ chứa 1 Expression Tree gốc) và `postUpdates` (danh sách các hành động state-mutation).

### 1.2 Khối Abstract Expression (Điều kiện)

Bất kỳ biểu thức điều kiện nào (`subject.tuitionPaid == true`, hay `size(holds) == 0`) cũng là một `Expression`.
- **`LogicalOperator`**: Node cho toán tử logic `AND`, `OR`, và `NOT`. (VD: P02 Registration Window). `NOT` là toán tử một ngôi nên `right` node có thể null (`lowerBound="0"`).
- **`RelationalOperator`**: Node toán tử so sánh (`EQUALS`, `GREATER_THAN`, `LESS_THAN`, `IN`, `SUBSET_OF`, `OVERLAPS` v.v.). Đặc biệt `SUBSET_OF` dùng cho P06 (Môn tiên quyết).
- **`ArithmeticOperator`**: Node tính toán (`ADD`, `SUBTRACT`). Dùng cho P05 (`subject.currentCredits + object.course.credits`).

### 1.3 Khối Values (Giá trị đầu cuối)

- **`VariableAccess`**: Trỏ vào bộ nhớ của PDP Engine. Định vị bằng `EntityScope` (SUBJECT/OBJECT/ENVIRONMENT) gắn với `path` (Nested Path, ví dụ: `currentCredits` hoặc `course.credits`). Việc sử dụng chuỗi nested path giúp dễ dàng phản ánh logic sang hàm getter Reflection dưới Backend.
- **`Constant`**: Chứa giá trị Hard-coded (VD: `24`, `true`).
- **`ListConstant`**: Chứa Array/List giá trị Hard-coded đi kèm với `elementType` (VD: `["NORMAL", "LATE"]` kiểu Enum). Note: Nếu ở DSL grammar không quy định type thì `elementType` sẽ được Engine tự gán ngầm định lúc Parse.
- **`FunctionCall`**: Đại diện cho các hàm ngoại lệ phải được Engine gọi nội bộ như `checkExistsRegistration()`, `isEmpty()`.

### 1.4 Khối Update & Post Action (Hậu cập nhật)

Dùng cho mảng `postUpdates` trong Policy để khai báo các hành động sửa đổi sau cấp phép. Có một class trừu tượng gốc là **`Statement`**.
- **`UpdateStatement`**: Kế thừa từ `Statement`. Khối gán dữ liệu. Yêu cầu có 3 field: `target` (VariableAccess), `operator` (VD: `ADD_ASSIGN`), và `value` (Expression).
- **`CreateTransactionStatement`**: Kế thừa từ `Statement`. Chứa `entityName` và mảng `arguments` (Expressions). Yêu cầu engine tạo một entity mới.
- **`AuditLogStatement`**: Kế thừa từ `Statement`. Chứa mảng `arguments` (Expressions) lưu thông số log. Yêu cầu engine tạo Audit trace-log.

---
*Kết luận Bước 4:* Metamodel này đảm bảo bất kỳ logic nào theo cấu trúc 12 policies của UCON ở KMA đều có thể được ép vào khuôn XML Object. Cấu trúc Ecore này là khung sườn đê tiến hành thiết kế ngữ pháp Text-based (DSL Grammar) ở Bước 5.
