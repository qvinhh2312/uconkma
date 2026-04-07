# Chương 4 (Tiếp): Thiết kế Ngôn ngữ DSL và Grammar (Bước 5)

Dựa trên cấu trúc Abstract Syntax Tree của `ucon.ecore` (Bước 4 được nâng cấp theo cấu trúc phẳng), chương này định nghĩa cấu trúc ngữ pháp Concrete Syntax dưới dạng văn bản (Text-based DSL). Đây là ngôn ngữ kỹ sư (hoặc Admin) dùng đê viết Policy.

## 1. Mục tiêu thiết kế Grammar
- **Domain-Specific:** Cú pháp phải cực kỳ gần gũi với nghiệp vụ đăng ký tín chỉ (Dùng các từ khóa trực quan như `policy`, `type`, `effect`, `condition`, `denyReason`).
- **Parser-Friendly:** Ánh xạ 1-1 với kiến trúc phẳng của lớp `Policy` trong `ucon.ecore` đê việc viết Parser bằng ANTLR / Xtext trở nên đơn giản nhất. Mỗi keyword sẽ sinh ra một EAttribute/EReference tương ứng trên AST.

## 2. Đặc tả Xtext / ANTLR Grammar (BNF Format)

Dưới đây là thiết kế cốt lõi của bộ Grammar (dạng xtext/BNF) định hình cho file `ucon_policy.dsl`. Khối `rule` lồng ghép đã được loại bỏ hoàn toàn đê tối ưu cấu trúc của AST.

### 2.1 Cấu trúc Root (PolicyModel)
```xtext
PolicyModel:
    (policies+=Policy)*;
```
*Giải thích:* Một file DSL có thể chứa một hoặc nhiều khối `Policy` đặt liên tiếp nhau.

### 2.2 Cấu trúc khối Policy chính (Đã Flattened)
```xtext
Policy:
    'policy' policyId=ID '{'
        'type:' type=PolicyType
        'targetAction:' targetAction=ActionType
        'effect:' effect=PolicyEffect
        'priority:' priority=INT
        'description:' description=STRING
        ('denyReason:' denyReason=STRING)?
        
        'condition:' condition=Expression
        
        ('postUpdates:'
            (postUpdates+=UpdateStatement)*
        )?
    '}';
```
*Giải thích:* 
- Mọi thuộc tính như `effect`, `priority`, hay `denyReason` nay nằm ngang hàng tại root của khối Policy, ánh xạ thẳng thành các Attribute trong Ecore class `Policy`.
- `condition` ánh xạ trực tiếp với Expression root tree.
- Khối `postUpdates:` chứa một mảng liên tiếp các phép toán mutate state (chỉ dùng cho POST_UPDATE).

### 2.3 Phân tách Biểu thức (Expression)
Grammar xử lý độ ưu tiên xếp hạng từ thấp lên cao (OR -> AND -> NOT -> So sánh -> Toán học -> Giá trị).

```xtext
Expression: OrExpression;

OrExpression returns Expression:
    AndExpression ({LogicalOperator.left=current} operator=LogicalOp_OR right=AndExpression)*;

AndExpression returns Expression:
    NotExpression ({LogicalOperator.left=current} operator=LogicalOp_AND right=NotExpression)*;

NotExpression returns Expression:
    {LogicalOperator} operator=LogicalOp_NOT right=RelationalExpression
    | RelationalExpression;

RelationalExpression returns Expression:
    ArithmeticExpression ({RelationalOperator.left=current} operator=RelationalOp right=ArithmeticExpression)*;

ArithmeticExpression returns Expression:
    PrimaryExpression ({ArithmeticOperator.left=current} operator=ArithmeticOp right=PrimaryExpression)*;
```

### 2.4 Giá trị Cốt lõi (Primary)
```xtext
PrimaryExpression returns Expression:
    VariableAccess | Constant | ListConstant | FunctionCall | '(' Expression ')';

VariableAccess:
    entity=EntityScope '.' path=QualifiedName; 
    // e.g., object.course.credits -> entity: OBJECT, path: "course.credits"

Constant:
    value=STRING_LITERAL | INT_LITERAL | BOOLEAN_LITERAL;

ListConstant:
    '[' (values+=STRING_LITERAL (',' values+=STRING_LITERAL)*) ']';

FunctionCall:
    functionName=ID '(' (arguments+=Expression (',' arguments+=Expression)*)? ')';
```

## 3. Khối lệnh cập nhật trạng thái (UpdateStatement)

Dùng riêng cho Post-Update:

```xtext
UpdateStatement:
    CreateTransactionStatement | StandardUpdateStatement | AuditLogStatement;

CreateTransactionStatement:
    'create' entityName=ID '(' (arguments+=Expression (',' arguments+=Expression)*)? ')';

StandardUpdateStatement:
    target=VariableAccess operator=AssignmentOp value=Expression;

AuditLogStatement:
    'create' 'AuditLog' '(' (arguments+=Expression (',' arguments+=Expression)*)? ')';
```

---
*Kết luận:* Bộ Grammar này định hình 100% tệp mẫu `e:\UCON_KMA\dsl\ucon_policy.dsl`. Việc loại bỏ khối `rule` trung gian giúp Grammar cực kỳ sạch, dễ thao tác và tối ưu hóa thời gian parse của Engine (Step 7).
