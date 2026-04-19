# Chuong 4.2: Grammar cua DSL chinh sach

File grammar hien tai la `dsl/UconPolicy.g4`. Grammar nay duoc thiet ke de bam sat metamodel `ucon.ecore` va policy file thuc te `dsl/ucon_policy.dsl`.

## 1. Root

```antlr
policyModel: policy+ EOF;
```

Mot file DSL phai chua it nhat mot `policy`.

## 2. Cau truc day du cua policy

```antlr
policy: 'policy' ID '{'
        'type:' policyType
        'targetAction:' actionType
        'effect:' policyEffect
        'priority:' INT
        'description:' STRING
        'subjectType:' STRING
        'objectType:' STRING
        'ruleFamily:' ID
        ('denyReason:' STRING)?
        'condition:' expression
        ('postUpdates:' updateStatement+)?
        '}';
```

Diem khac biet so voi ban cu:

- policy bat buoc co `subjectType`
- policy bat buoc co `objectType`
- policy bat buoc co `ruleFamily`
- `postUpdates` la tuy chon ve cu phap, nhung semantic validator buoc moi `POST_UPDATE` phai co it nhat mot statement

## 3. Enum va keyword

### Policy type
```antlr
policyType: 'PRE_AUTHORIZATION' | 'ONGOING_AUTHORIZATION' | 'POST_UPDATE';
```

### Action type
```antlr
actionType: 'REGISTER' | 'DROP' | 'ANY';
```

### Policy effect
```antlr
policyEffect: 'PERMIT' | 'DENY';
```

## 4. Grammar cua expression

```antlr
expression: orExpression;
orExpression: andExpression (OR andExpression)*;
andExpression: notExpression (AND notExpression)*;
notExpression: NOT relationalExpression | relationalExpression;
relationalExpression: arithmeticExpression (relationalOp arithmeticExpression)*;
arithmeticExpression: primaryExpression (arithmeticOp primaryExpression)*;
```

Thiet ke nay giu duoc uu tien toan tu tu nhien:

- `OR`
- `AND`
- `NOT`
- so sanh
- so hoc
- gia tri nguyen thuy

## 5. Primary expression

```antlr
primaryExpression
    : variableAccess
    | constant
    | listConstant
    | functionCall
    | '(' expression ')'
    ;
```

### Variable access
```antlr
variableAccess: entityScope '.' qualifiedName;
entityScope: 'subject' | 'object' | 'environment' | 'request';
qualifiedName: ID ('.' ID)*;
```

Vi du:

- `subject.currentCredits`
- `object.course.credits`
- `environment.isMaintenance`
- `request.requestId`

### Constant
```antlr
constant: STRING | INT | BOOLEAN;
```

### List constant
```antlr
listConstant: '[' STRING (',' STRING)* ']';
```

### Function call
```antlr
functionCall: ID '(' (expression (',' expression)*)? ')';
```

## 6. Statement cho POST_UPDATE

```antlr
updateStatement
    : createTransactionStatement
    | deleteTransactionStatement
    | auditLogStatement
    | standardUpdateStatement
    ;
```

### Create transaction
```antlr
createTransactionStatement: 'create' ID '(' (expression (',' expression)*)? ')';
```

### Delete transaction
```antlr
deleteTransactionStatement: 'delete' ID '(' (expression (',' expression)*)? ')';
```

### Audit log
```antlr
auditLogStatement: 'create' 'AuditLog' '(' (expression (',' expression)*)? ')';
```

### Standard update
```antlr
standardUpdateStatement: variableAccess assignmentOp expression;
```

## 7. Operator hien duoc ho tro

### Logical
```antlr
OR: 'OR';
AND: 'AND';
NOT: 'NOT';
```

### Relational
```antlr
relationalOp: '==' | '!=' | '>' | '>=' | '<' | '<='
            | 'IN' | 'CONTAINS' | 'NOT_CONTAINS'
            | 'SUBSET_OF' | 'OVERLAPS';
```

### Arithmetic
```antlr
arithmeticOp: '+' | '-';
```

### Assignment
```antlr
assignmentOp: '=' | 'ADD_ASSIGN' | 'SUB_ASSIGN' | 'APPEND' | 'REMOVE';
```

## 8. Tep DSL thuc te
Grammar hien tai duoc dung truc tiep de parse tap policy that cua project, bao gom cac chinh sach:

- maintenance o ca `PRE` va `ONGOING`
- transaction window chung cho `ANY`
- dieu kien `DROP` rieng
- mutation + audit o `POST_UPDATE`

Dieu nay co nghia grammar khong con la ban minh hoa nhu luc dau, ma la mo ta sat runtime hien hanh.

## 9. Ket luan
Grammar hien tai dat ba muc tieu:

- du gan ngon ngu nghiep vu de de doc
- du chat de parser sinh duoc AST/XMI on dinh
- du mo rong de gan semantic validator va runtime engine hien tai
