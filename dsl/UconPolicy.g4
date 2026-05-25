grammar UconPolicy;

// Root
policyModel: policy+ policySet* EOF;

policy: 'policy' ID '{'
        'predicate:' predicateType
        'phase:' phaseType
        'updateTiming:' updateTiming
        'targetAction:' actionType
        'effect:' policyEffect
        'priority:' INT
        'description:' descriptionValue=STRING
        'subjectType:' subjectTypeValue=STRING
        'objectType:' objectTypeValue=STRING
        'source:' sourceValue=STRING
        'version:' versionValue=STRING
        'policyStatus:' policyStatusType
        'uconVariant:' uconVariantValue=STRING
        ('denyReason:' denyReasonValue=STRING)?
        'condition:' expression
        ('preUpdates:' preUpdates+=updateStatement+)?
        ('ongoingUpdates:' ongoingUpdates+=updateStatement+)?
        ('postUpdates:' postUpdates+=updateStatement+)?
        ('rollbackUpdates:' rollbackUpdates+=updateStatement+)?
        '}';

predicateType: 'AUTHORIZATION' | 'OBLIGATION' | 'CONDITION';
phaseType: 'PRE' | 'ONGOING' | 'POST';
updateTiming: 'NONE' | 'PRE' | 'ONGOING' | 'POST';
actionType: 'REGISTER' | 'DROP' | 'ANY';
policyEffect: 'PERMIT' | 'DENY';
policyStatusType: 'DRAFT' | 'VALIDATED' | 'ACTIVE' | 'DEPRECATED' | 'ARCHIVED';
policySet: 'policySet' ID '{'
           'combiningAlgorithm:' combiningAlgorithm
           'policies:' ID (',' ID)*
           '}';

combiningAlgorithm: 'DENY_OVERRIDES' | 'PERMIT_OVERRIDES' | 'FIRST_APPLICABLE' | 'PRIORITY_ORDER' | 'ONLY_ONE_APPLICABLE';

// Expression tree
expression: orExpression;

orExpression: andExpression (OR andExpression)*;
andExpression: notExpression (AND notExpression)*;
notExpression: NOT relationalExpression | relationalExpression;

relationalExpression: arithmeticExpression (relationalOp arithmeticExpression)*;
arithmeticExpression: primaryExpression (arithmeticOp primaryExpression)*;

primaryExpression 
    : variableAccess 
    | constant 
    | listConstant 
    | functionCall 
    | '(' expression ')'
    ;

variableAccess: entityScope '.' qualifiedName;

entityScope: 'subject' | 'object' | 'environment' | 'request';

constant: STRING | INT | BOOLEAN;

listConstant: '[' STRING (',' STRING)* ']';

functionCall: ID '(' (expression (',' expression)*)? ')';

// Update Statements
updateStatement: createTransactionStatement | deleteTransactionStatement | auditLogStatement | standardUpdateStatement;


createTransactionStatement: 'create' ID '(' (expression (',' expression)*)? ')';
auditLogStatement: 'create' 'AuditLog' '(' (expression (',' expression)*)? ')';
deleteTransactionStatement: 'delete' ID '(' (expression (',' expression)*)? ')';

standardUpdateStatement: variableAccess assignmentOp expression;

// Operators
OR: 'OR';
AND: 'AND';
NOT: 'NOT';

relationalOp: '==' | '!=' | '>' | '>=' | '<' | '<=' | 'IN' | 'CONTAINS' | 'NOT_CONTAINS' | 'SUBSET_OF' | 'OVERLAPS';
arithmeticOp: '+' | '-';
assignmentOp: '=' | 'ADD_ASSIGN' | 'SUB_ASSIGN' | 'APPEND' | 'REMOVE';

qualifiedName: ID ('.' ID)*;

// Lexer Rules
BOOLEAN: 'true' | 'false';
ID: [a-zA-Z_][a-zA-Z0-9_]*;
INT: [0-9]+;
STRING: '"' (~["\r\n\\] | '\\' .)* '"';

WS: [ \t\r\n]+ -> skip;
LINE_COMMENT: '//' ~[\r\n]* -> skip;
BLOCK_COMMENT: '/*' .*? '*/' -> skip;
