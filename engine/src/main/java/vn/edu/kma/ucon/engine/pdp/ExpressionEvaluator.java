package vn.edu.kma.ucon.engine.pdp;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.springframework.stereotype.Component;

import vn.edu.kma.ucon.engine.pep.UconRequest;
import vn.edu.kma.ucon.engine.pip.entity.AuditLog;
import vn.edu.kma.ucon.engine.pip.entity.ClassSection;
import vn.edu.kma.ucon.engine.pip.entity.Registration;
import vn.edu.kma.ucon.engine.pip.entity.Student;
import vn.edu.kma.ucon.engine.pip.repository.AuditLogRepository;
import vn.edu.kma.ucon.engine.pip.repository.RegistrationRepository;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AST-based Expression Evaluator for the UCON Policy Engine.
 *
 * Responsible for:
 * 1. Evaluating boolean conditions (PRE/ONGOING phases).
 * 2. Executing POST_UPDATE statements (property mutations + DB side-effects).
 *
 * All business logic is driven purely by the DSL AST — zero hardcoded mutations.
 */
@Component
public class ExpressionEvaluator {

    private final AuditLogRepository auditLogRepository;
    private final RegistrationRepository registrationRepository;

    public ExpressionEvaluator(AuditLogRepository auditLogRepository,
                               RegistrationRepository registrationRepository) {
        this.auditLogRepository = auditLogRepository;
        this.registrationRepository = registrationRepository;
    }

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    public boolean evaluateCondition(EObject condition, Student subject, ClassSection obj, Environment env, UconRequest req) {
        if (condition == null) return true;
        Object result = evaluateNode(condition, subject, obj, env, req);
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        throw new IllegalArgumentException("Condition must evaluate to a boolean, got: " + result);
    }

    /**
     * Executes all POST_UPDATE statements from a policy.
     * Supports: StandardUpdateStatement (=, ADD_ASSIGN, SUB_ASSIGN, APPEND, REMOVE),
     *           CreateTransactionStatement (creates Registration record in DB),
     *           AuditLogStatement (creates AuditLog record in DB).
     */
    public void executePostUpdates(List<EObject> updateStatements, Student subject, ClassSection obj, Environment env, UconRequest req) {
        if (updateStatements == null) return;
        for (EObject stmt : updateStatements) {
            String className = stmt.eClass().getName();
            switch (className) {
                case "UpdateStatement":
                    executeStandardUpdate(stmt, subject, obj, env, req);
                    break;
                case "CreateTransactionStatement":
                    executeCreateTransaction(stmt, subject, obj, env, req);
                    break;
                case "AuditLogStatement":
                    executeAuditLog(stmt, subject, obj, env, req);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown statement type in postUpdates: " + className);
            }
        }
    }

    // =========================================================================
    // CONDITION EVALUATION (Private)
    // =========================================================================

    private Object evaluateNode(EObject node, Student subject, ClassSection obj, Environment env, UconRequest req) {
        String className = node.eClass().getName();

        switch (className) {
            case "LogicalOperator":
                return evalLogicalOp(node, subject, obj, env, req);
            case "RelationalOperator":
                return evalRelationalOp(node, subject, obj, env, req);
            case "ArithmeticOperator":
                return evalArithmeticOp(node, subject, obj, env, req);
            case "VariableAccess":
                return resolveVariable(node, subject, obj, env, req);
            case "Constant":
                return resolveConstant(node);
            case "ListConstant":
                return resolveListConstant(node);
            case "FunctionCall":
                return evalFunctionCall(node, subject, obj, env, req);
            default:
                throw new UnsupportedOperationException("Unknown expression node type: " + className);
        }
    }

    private boolean evalLogicalOp(EObject node, Student subject, ClassSection obj, Environment env, UconRequest req) {
        EEnumLiteral operator = (EEnumLiteral) node.eGet(node.eClass().getEStructuralFeature("operator"));
        EObject leftNode = (EObject) node.eGet(node.eClass().getEStructuralFeature("left"));
        EObject rightNode = (EObject) node.eGet(node.eClass().getEStructuralFeature("right"));

        String op = operator.getName();

        if ("NOT".equals(op)) {
            return !Boolean.TRUE.equals(evaluateNode(leftNode, subject, obj, env, req));
        }

        boolean leftVal = Boolean.TRUE.equals(evaluateNode(leftNode, subject, obj, env, req));

        if ("AND".equals(op)) {
            if (!leftVal) return false; // Short-circuit
            return Boolean.TRUE.equals(evaluateNode(rightNode, subject, obj, env, req));
        } else if ("OR".equals(op)) {
            if (leftVal) return true; // Short-circuit
            return Boolean.TRUE.equals(evaluateNode(rightNode, subject, obj, env, req));
        }
        return false;
    }

    private boolean evalRelationalOp(EObject node, Student subject, ClassSection obj, Environment env, UconRequest req) {
        EEnumLiteral operator = (EEnumLiteral) node.eGet(node.eClass().getEStructuralFeature("operator"));
        EObject leftNode = (EObject) node.eGet(node.eClass().getEStructuralFeature("left"));
        EObject rightNode = (EObject) node.eGet(node.eClass().getEStructuralFeature("right"));

        String op = operator.getName();
        Object leftVal = evaluateNode(leftNode, subject, obj, env, req);
        Object rightVal = evaluateNode(rightNode, subject, obj, env, req);

        if (leftVal == null || rightVal == null) return false;

        switch (op) {
            case "EQUALS":
                return leftVal.equals(rightVal);
            case "NOT_EQUALS":
                return !leftVal.equals(rightVal);
            case "GREATER_THAN":
                if (leftVal instanceof Integer && rightVal instanceof Integer) return (Integer) leftVal > (Integer) rightVal;
                if (leftVal instanceof String && rightVal instanceof String) return ((String) leftVal).compareTo((String) rightVal) > 0;
                return false;
            case "LESS_THAN":
                if (leftVal instanceof Integer && rightVal instanceof Integer) return (Integer) leftVal < (Integer) rightVal;
                if (leftVal instanceof String && rightVal instanceof String) return ((String) leftVal).compareTo((String) rightVal) < 0;
                return false;
            case "GREATER_OR_EQUALS":
                if (leftVal instanceof Integer && rightVal instanceof Integer) return (Integer) leftVal >= (Integer) rightVal;
                if (leftVal instanceof String && rightVal instanceof String) return ((String) leftVal).compareTo((String) rightVal) >= 0;
                return false;
            case "LESS_OR_EQUALS":
                if (leftVal instanceof Integer && rightVal instanceof Integer) return (Integer) leftVal <= (Integer) rightVal;
                if (leftVal instanceof String && rightVal instanceof String) return ((String) leftVal).compareTo((String) rightVal) <= 0;
                return false;
            case "IN":
            case "CONTAINS":
                if (rightVal instanceof Collection) {
                    return ((Collection<?>) rightVal).contains(leftVal.toString().trim());
                } else if (rightVal instanceof String) {
                    return asListOptimized(rightVal).contains(leftVal.toString().trim());
                } else if (leftVal instanceof Collection && rightVal instanceof String) {
                    return ((Collection<?>) leftVal).contains(rightVal.toString().trim());
                } else if (leftVal instanceof String && rightVal instanceof Collection) {
                    return ((Collection<?>) rightVal).contains(leftVal.toString().trim());
                }
                return false;
            case "NOT_CONTAINS":
                if (leftVal instanceof String && rightVal instanceof String) {
                    return !asListOptimized(leftVal).contains(rightVal.toString().trim());
                } else if (leftVal instanceof Collection) {
                    return !((Collection<?>) leftVal).contains(rightVal.toString().trim());
                }
                return false;
            case "SUBSET_OF":
                // Empty set is subset of any set
                if (leftVal.toString().trim().isEmpty()) return true;
                Collection<?> subset = asListOptimized(leftVal);
                Collection<?> superset = asListOptimized(rightVal);
                return superset.containsAll(subset);
            case "OVERLAPS":
                Collection<?> list1 = asListOptimized(leftVal);
                Collection<?> list2 = asListOptimized(rightVal);
                for (Object item : list1) {
                    if (item != null && !item.toString().trim().isEmpty() && list2.contains(item)) return true;
                }
                return false;
            default:
                throw new UnsupportedOperationException("Unknown relational op: " + op);
        }
    }

    private Integer evalArithmeticOp(EObject node, Student subject, ClassSection obj, Environment env, UconRequest req) {
        EEnumLiteral operator = (EEnumLiteral) node.eGet(node.eClass().getEStructuralFeature("operator"));
        EObject leftNode = (EObject) node.eGet(node.eClass().getEStructuralFeature("left"));
        EObject rightNode = (EObject) node.eGet(node.eClass().getEStructuralFeature("right"));

        Object leftValObj = evaluateNode(leftNode, subject, obj, env, req);
        Object rightValObj = evaluateNode(rightNode, subject, obj, env, req);

        // Null-safe unboxing: treat null as 0 to avoid NullPointerException
        Integer leftVal = leftValObj instanceof Integer ? (Integer) leftValObj : 0;
        Integer rightVal = rightValObj instanceof Integer ? (Integer) rightValObj : 0;

        if (operator.getName().equals("ADD")) return leftVal + rightVal;
        if (operator.getName().equals("SUBTRACT")) return leftVal - rightVal;
        return 0;
    }

    private Object evalFunctionCall(EObject node, Student subject, ClassSection obj, Environment env, UconRequest req) {
        String funcName = (String) node.eGet(node.eClass().getEStructuralFeature("functionName"));
        List<?> args = (List<?>) node.eGet(node.eClass().getEStructuralFeature("arguments"));

        if ("isEmpty".equals(funcName)) {
            if (args == null || args.isEmpty()) return true;
            Object argVal = evaluateNode((EObject) args.get(0), subject, obj, env, req);
            return argVal == null || argVal.toString().trim().isEmpty();
        }

        if ("checkExistsRegistration".equals(funcName)) {
            // DSL: checkExistsRegistration(subject.studentId, object.classId, environment.semester)
            // We check if classId is already in the student's registeredClassIds list.
            Object classIdObj = evaluateNode((EObject) args.get(1), subject, obj, env, req);
            if (classIdObj != null && subject != null && subject.getRegisteredClassIds() != null) {
                return asListOptimized(subject.getRegisteredClassIds()).contains(classIdObj.toString().trim());
            }
            return false;
        }

        throw new UnsupportedOperationException("Unknown function in DSL condition: " + funcName);
    }

    // =========================================================================
    // POST-UPDATE STATEMENT EXECUTORS (Private)
    // =========================================================================

    /**
     * Handles: object.enrolled ADD_ASSIGN 1
     *          subject.currentCredits ADD_ASSIGN object.course.credits
     *          subject.registeredScheduleSlots APPEND object.scheduleSlots
     *          subject.registeredClassIds APPEND object.classId
     */
    private void executeStandardUpdate(EObject node, Student subject, ClassSection obj, Environment env, UconRequest req) {
        EObject targetNode = (EObject) node.eGet(node.eClass().getEStructuralFeature("target"));
        EEnumLiteral operator = (EEnumLiteral) node.eGet(node.eClass().getEStructuralFeature("operator"));
        EObject valueNode = (EObject) node.eGet(node.eClass().getEStructuralFeature("value"));

        Object value = evaluateNode(valueNode, subject, obj, env, req);

        EEnumLiteral entityScope = (EEnumLiteral) targetNode.eGet(targetNode.eClass().getEStructuralFeature("entity"));
        String pathOrig = (String) targetNode.eGet(targetNode.eClass().getEStructuralFeature("path"));

        Object targetInstance = null;
        switch (entityScope.getName()) {
            case "SUBJECT":     targetInstance = subject; break;
            case "OBJECT":      targetInstance = obj; break;
            case "ENVIRONMENT": targetInstance = env; break;
            case "REQUEST":     targetInstance = req; break;
        }

        if (targetInstance == null) return;

        // Traverse nested path e.g. "course.credits" — stop before the last segment
        String[] props = pathOrig.split("\\.");
        for (int i = 0; i < props.length - 1; i++) {
            targetInstance = getProperty(targetInstance, props[i]);
            if (targetInstance == null) return;
        }

        String finalProp = props[props.length - 1];
        Object currentValue = getProperty(targetInstance, finalProp);
        Object newValue = null;

        String opName = operator.getName();
        switch (opName) {
            case "ASSIGN":
                newValue = value;
                break;
            case "ADD_ASSIGN":
                if (currentValue instanceof Integer && value instanceof Integer) {
                    newValue = (Integer) currentValue + (Integer) value;
                }
                break;
            case "SUB_ASSIGN":
                if (currentValue instanceof Integer && value instanceof Integer) {
                    newValue = (Integer) currentValue - (Integer) value;
                }
                break;
            case "APPEND": {
                // Comma-separated string list append
                String currentStr = (currentValue == null) ? "" : currentValue.toString();
                String appendStr  = (value == null) ? "" : value.toString();
                if (appendStr.isEmpty()) {
                    newValue = currentStr; // nothing to append
                } else if (currentStr.isEmpty()) {
                    newValue = appendStr;
                } else {
                    newValue = currentStr + "," + appendStr;
                }
                break;
            }
            case "REMOVE": {
                // Remove a value from a comma-separated string list
                if (currentValue != null && value != null) {
                    final String removeTarget = value.toString().trim();
                    List<String> existing = asListOptimized(currentValue).stream()
                            .map(Object::toString)
                            .filter(s -> !s.equals(removeTarget))
                            .collect(Collectors.toList());
                    newValue = String.join(",", existing);
                }
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown assignment operator: " + opName);
        }

        if (newValue != null) {
            setProperty(targetInstance, finalProp, newValue);
        }
    }

    /**
     * Handles: create Transaction(subject.studentId, object.classId, environment.semester, "REGISTER")
     *
     * The DSL calls this entity "Transaction" (from chapter_3_logic.md).
     * The engine maps it to the Registration entity to avoid naming conflicts.
     * Argument order: (studentId, classId, semester, actionType)
     */
    private void executeCreateTransaction(EObject node, Student subject, ClassSection obj, Environment env, UconRequest req) {
        List<?> args = (List<?>) node.eGet(node.eClass().getEStructuralFeature("arguments"));
        if (args == null || args.size() < 4) {
            throw new IllegalArgumentException("create Transaction(...) requires 4 arguments: studentId, classId, semester, actionType");
        }

        String studentId  = resolveArg(args, 0, subject, obj, env, req);
        String classId    = resolveArg(args, 1, subject, obj, env, req);
        String semester   = resolveArg(args, 2, subject, obj, env, req);
        String actionType = resolveArg(args, 3, subject, obj, env, req);

        Registration reg = new Registration(studentId, classId, semester, actionType);
        registrationRepository.save(reg);
    }

    /**
     * Handles: create AuditLog(request.id, subject.studentId, object.classId, request.decision, request.failedPolicyCodes)
     *
     * Argument order: (requestId, studentId, classId, decision, failedPolicyCodes)
     */
    private void executeAuditLog(EObject node, Student subject, ClassSection obj, Environment env, UconRequest req) {
        List<?> args = (List<?>) node.eGet(node.eClass().getEStructuralFeature("arguments"));
        if (args == null || args.size() < 5) {
            throw new IllegalArgumentException("create AuditLog(...) requires 5 arguments: requestId, studentId, classId, decision, failedPolicyCodes");
        }

        String requestId         = resolveArg(args, 0, subject, obj, env, req);
        String studentId         = resolveArg(args, 1, subject, obj, env, req);
        String classId           = resolveArg(args, 2, subject, obj, env, req);
        String decision          = resolveArg(args, 3, subject, obj, env, req);
        String failedPolicyCodes = resolveArg(args, 4, subject, obj, env, req);

        AuditLog al = new AuditLog();
        al.setRequestId(requestId);
        al.setStudentId(studentId);
        al.setClassId(classId);
        al.setDecision(decision != null ? decision : "UNKNOWN");
        al.setFailedPolicyCodes(failedPolicyCodes != null ? failedPolicyCodes : "");
        auditLogRepository.save(al);
    }

    /**
     * Safely evaluates an argument from a DSL argument list and returns it as a String.
     */
    private String resolveArg(List<?> args, int index, Student subject, ClassSection obj, Environment env, UconRequest req) {
        Object val = evaluateNode((EObject) args.get(index), subject, obj, env, req);
        return val == null ? null : val.toString();
    }

    // =========================================================================
    // VARIABLE RESOLUTION (Private)
    // =========================================================================

    private Object resolveVariable(EObject node, Student subject, ClassSection obj, Environment env, UconRequest req) {
        EEnumLiteral entity = (EEnumLiteral) node.eGet(node.eClass().getEStructuralFeature("entity"));
        String path = (String) node.eGet(node.eClass().getEStructuralFeature("path"));

        Object current = null;
        switch (entity.getName()) {
            case "SUBJECT":     current = subject; break;
            case "OBJECT":      current = obj; break;
            case "ENVIRONMENT": current = env; break;
            case "REQUEST":     current = req; break;
        }

        if (current == null) return null;

        String[] props = path.split("\\.");
        for (String prop : props) {
            current = getProperty(current, prop);
            if (current == null) return null;
        }
        return current;
    }

    private Object resolveConstant(EObject node) {
        EEnumLiteral type = (EEnumLiteral) node.eGet(node.eClass().getEStructuralFeature("type"));
        String value = (String) node.eGet(node.eClass().getEStructuralFeature("value"));
        switch (type.getName()) {
            case "INTEGER": return Integer.parseInt(value);
            case "BOOLEAN": return Boolean.parseBoolean(value);
            default: return value; // STRING or ENUM
        }
    }

    @SuppressWarnings("unchecked")
    private Object resolveListConstant(EObject node) {
        return (List<String>) node.eGet(node.eClass().getEStructuralFeature("values"));
    }

    // =========================================================================
    // REFLECTION HELPERS (Private)
    // =========================================================================

    private Object getProperty(Object instance, String propName) {
        try {
            String getterName = "get" + propName.substring(0, 1).toUpperCase() + propName.substring(1);
            Method method;
            try {
                method = instance.getClass().getMethod(getterName);
            } catch (NoSuchMethodException e) {
                // Boolean properties may use "is" prefix
                String isName = "is" + propName.substring(0, 1).toUpperCase() + propName.substring(1);
                method = instance.getClass().getMethod(isName);
            }
            return method.invoke(instance);
        } catch (Exception e) {
            throw new RuntimeException("Could not get property '" + propName + "' from " + instance.getClass().getSimpleName(), e);
        }
    }

    private void setProperty(Object instance, String propName, Object value) {
        try {
            String setterName = "set" + propName.substring(0, 1).toUpperCase() + propName.substring(1);
            Method[] methods = instance.getClass().getMethods();
            for (Method m : methods) {
                if (m.getName().equals(setterName) && m.getParameterCount() == 1) {
                    m.invoke(instance, value);
                    return;
                }
            }
            throw new NoSuchMethodException("No setter found for property: " + propName);
        } catch (Exception e) {
            throw new RuntimeException("Could not set property '" + propName + "' on " + instance.getClass().getSimpleName(), e);
        }
    }

    private Collection<?> asListOptimized(Object val) {
        if (val == null) return Collections.emptyList();
        if (val instanceof Collection) return (Collection<?>) val;
        return Arrays.stream(val.toString().split(","))
                     .map(String::trim)
                     .filter(s -> !s.isEmpty())
                     .collect(Collectors.toList());
    }
}
