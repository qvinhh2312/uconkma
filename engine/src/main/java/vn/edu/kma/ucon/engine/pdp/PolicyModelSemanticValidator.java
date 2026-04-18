package vn.edu.kma.ucon.engine.pdp;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.springframework.stereotype.Component;

import vn.edu.kma.ucon.engine.pep.UconRequest;
import vn.edu.kma.ucon.engine.pip.entity.ClassSection;
import vn.edu.kma.ucon.engine.pip.entity.Student;

@Component
public class PolicyModelSemanticValidator {
    private final PolicyFunctionRegistry functionRegistry;

    private static final Set<String> ALLOWED_SUBJECT_TYPES = Set.of("Student");
    private static final Set<String> ALLOWED_OBJECT_TYPES = Set.of("ClassSection");
    private static final Set<String> ALLOWED_RULE_FAMILIES = Set.of("AUTHORIZATION", "MUTATION", "TRACE");

    private static final Set<String> MUTABLE_SUBJECT_PATHS = Set.of(
            "currentCredits",
            "registeredScheduleSlots",
            "registeredClassIds",
            "tuitionDebt"
    );

    private static final Set<String> MUTABLE_OBJECT_PATHS = Set.of(
            "enrolled",
            "status"
    );

    private static final Set<String> ALLOWED_TRANSACTION_ENTITY_NAMES = Set.of("Transaction");
    private static final Set<String> ALLOWED_ACTION_TYPES = Set.of("REGISTER", "DROP");
    private static final Set<String> ALLOWED_REQUEST_AUDIT_PATHS = Set.of(
            "requestId",
            "decision",
            "failedPolicyCodes"
    );

    public PolicyModelSemanticValidator(PolicyFunctionRegistry functionRegistry) {
        this.functionRegistry = functionRegistry;
    }

    public void validate(EObject policyModelRoot) {
        if (policyModelRoot == null) return;

        @SuppressWarnings("unchecked")
        List<EObject> policies = (List<EObject>) policyModelRoot.eGet(policyModelRoot.eClass().getEStructuralFeature("policies"));

        Map<String, String> policyIds = new HashMap<>();
        Map<String, String> prioritiesPerPhaseAction = new HashMap<>();
        for (EObject policy : policies) {
            validatePolicy(policy, policyIds, prioritiesPerPhaseAction);
        }
    }

    private void validatePolicy(EObject policy,
                                Map<String, String> policyIds,
                                Map<String, String> prioritiesPerPhaseAction) {
        String policyId = stringValue(policy, "policyId");
        String type = enumName(policy, "type");
        String targetAction = enumName(policy, "targetAction");
        String ruleFamily = stringValue(policy, "ruleFamily");
        Integer priority = (Integer) policy.eGet(policy.eClass().getEStructuralFeature("priority"));

        if (policyId == null || policyId.isBlank()) {
            throw new IllegalStateException("Policy id must not be blank.");
        }

        if (policyIds.putIfAbsent(policyId, policyId) != null) {
            throw new IllegalStateException("Duplicate policy id detected: " + policyId);
        }

        if (priority == null || priority < 0) {
            throw new IllegalStateException("Policy " + policyId + " has invalid priority: " + priority);
        }

        ensureBindingPresent(policyId, policy, "subjectType", ALLOWED_SUBJECT_TYPES);
        ensureBindingPresent(policyId, policy, "objectType", ALLOWED_OBJECT_TYPES);
        ensureBindingPresent(policyId, policy, "ruleFamily", ALLOWED_RULE_FAMILIES);
        validateRuleFamily(policyId, type, ruleFamily);

        validatePriorityUniqueness(policyId, type, targetAction, priority, prioritiesPerPhaseAction);

        @SuppressWarnings("unchecked")
        List<EObject> postUpdates = (List<EObject>) policy.eGet(policy.eClass().getEStructuralFeature("postUpdates"));

        if (!"POST_UPDATE".equals(type) && postUpdates != null && !postUpdates.isEmpty()) {
            throw new IllegalStateException("Only POST_UPDATE policies may contain postUpdates: " + policyId);
        }

        EObject condition = (EObject) policy.eGet(policy.eClass().getEStructuralFeature("condition"));
        validateExpression(policyId, condition, type, "condition", true);

        if ("POST_UPDATE".equals(type)) {
            if (postUpdates == null || postUpdates.isEmpty()) {
                throw new IllegalStateException("POST_UPDATE policy " + policyId + " must define at least one postUpdate.");
            }
            validatePostUpdates(policyId, postUpdates);
        }
    }

    private void validatePriorityUniqueness(String policyId,
                                            String type,
                                            String targetAction,
                                            Integer priority,
                                            Map<String, String> prioritiesPerPhaseAction) {
        String baseKey = type + "|" + priority + "|";

        if ("ANY".equals(targetAction)) {
            for (String action : Set.of("ANY", "REGISTER", "DROP")) {
                String existingPolicyId = prioritiesPerPhaseAction.get(baseKey + action);
                if (existingPolicyId != null) {
                    throw new IllegalStateException("Policies " + existingPolicyId + " and " + policyId
                            + " share overlapping priority " + priority + " for " + type + " with action scope ANY.");
                }
            }
        } else {
            String sameActionPolicyId = prioritiesPerPhaseAction.get(baseKey + targetAction);
            if (sameActionPolicyId != null) {
                throw new IllegalStateException("Policies " + sameActionPolicyId + " and " + policyId
                        + " share the same priority " + priority + " for " + type + "/" + targetAction + ".");
            }

            String anyPolicyId = prioritiesPerPhaseAction.get(baseKey + "ANY");
            if (anyPolicyId != null) {
                throw new IllegalStateException("Policies " + anyPolicyId + " and " + policyId
                        + " share overlapping priority " + priority + " for " + type + " because ANY overlaps "
                        + targetAction + ".");
            }
        }

        prioritiesPerPhaseAction.put(baseKey + targetAction, policyId);
    }

    private void validateRuleFamily(String policyId, String type, String ruleFamily) {
        boolean valid = switch (type) {
            case "PRE_AUTHORIZATION", "ONGOING_AUTHORIZATION" -> "AUTHORIZATION".equals(ruleFamily);
            case "POST_UPDATE" -> "MUTATION".equals(ruleFamily) || "TRACE".equals(ruleFamily);
            default -> false;
        };

        if (!valid) {
            throw new IllegalStateException("Policy " + policyId + " has incompatible type " + type
                    + " and ruleFamily " + ruleFamily + ".");
        }
    }

    private void validatePostUpdates(String policyId, List<EObject> postUpdates) {
        for (EObject stmt : postUpdates) {
            String stmtType = stmt.eClass().getName();
            switch (stmtType) {
                case "UpdateStatement" -> validateUpdateStatement(policyId, stmt);
                case "CreateTransactionStatement" -> validateCreateTransactionStatement(policyId, stmt);
                case "DeleteTransactionStatement" -> validateDeleteTransactionStatement(policyId, stmt);
                case "AuditLogStatement" -> validateAuditLogStatement(policyId, stmt);
                default -> throw new IllegalStateException(
                        "Policy " + policyId + " contains unsupported postUpdate statement type: " + stmtType);
            }
        }
    }

    private void validateUpdateStatement(String policyId, EObject stmt) {
        EObject target = (EObject) stmt.eGet(stmt.eClass().getEStructuralFeature("target"));
        String entity = enumName(target, "entity");
        String path = stringValue(target, "path");
        EObject value = (EObject) stmt.eGet(stmt.eClass().getEStructuralFeature("value"));

        switch (entity) {
            case "ENVIRONMENT" ->
                    throw new IllegalStateException("Policy " + policyId + " updates ENVIRONMENT path '" + path + "', but environment must stay immutable.");
            case "REQUEST" ->
                    throw new IllegalStateException("Policy " + policyId + " updates REQUEST path '" + path + "', but request state must stay runtime-managed.");
            case "SUBJECT" -> {
                if (!MUTABLE_SUBJECT_PATHS.contains(path)) {
                    throw new IllegalStateException("Policy " + policyId + " updates non-mutable SUBJECT path '" + path + "'.");
                }
                validatePathExists(policyId, entity, path, "update target");
            }
            case "OBJECT" -> {
                if (!MUTABLE_OBJECT_PATHS.contains(path)) {
                    throw new IllegalStateException("Policy " + policyId + " updates non-mutable OBJECT path '" + path + "'.");
                }
                validatePathExists(policyId, entity, path, "update target");
            }
            default -> throw new IllegalStateException("Policy " + policyId + " uses unsupported update target entity " + entity);
        }

        validateExpression(policyId, value, "POST_UPDATE", "updateValue:" + path, false);
    }

    private void validateCreateTransactionStatement(String policyId, EObject stmt) {
        String entityName = stringValue(stmt, "entityName");
        if (!ALLOWED_TRANSACTION_ENTITY_NAMES.contains(entityName)) {
            throw new IllegalStateException("Policy " + policyId + " creates unsupported entity '" + entityName + "'.");
        }

        List<EObject> arguments = childExpressions(stmt, "arguments");
        if (arguments.size() != 4) {
            throw new IllegalStateException("Policy " + policyId + " create Transaction(...) must have exactly 4 arguments.");
        }

        validateScopedVariableAccess(policyId, arguments.get(0), Set.of("SUBJECT"), Set.of("studentId"), "create Transaction arg#1");
        validateScopedVariableAccess(policyId, arguments.get(1), Set.of("OBJECT"), Set.of("classId"), "create Transaction arg#2");
        validateScopedVariableAccess(policyId, arguments.get(2), Set.of("ENVIRONMENT"), Set.of("semester"), "create Transaction arg#3");
        validateActionLiteral(policyId, arguments.get(3), "create Transaction arg#4");
    }

    private void validateDeleteTransactionStatement(String policyId, EObject stmt) {
        String entityName = stringValue(stmt, "entityName");
        if (!ALLOWED_TRANSACTION_ENTITY_NAMES.contains(entityName)) {
            throw new IllegalStateException("Policy " + policyId + " deletes unsupported entity '" + entityName + "'.");
        }

        List<EObject> arguments = childExpressions(stmt, "arguments");
        if (arguments.size() != 3) {
            throw new IllegalStateException("Policy " + policyId + " delete Transaction(...) must have exactly 3 arguments.");
        }

        validateScopedVariableAccess(policyId, arguments.get(0), Set.of("SUBJECT"), Set.of("studentId"), "delete Transaction arg#1");
        validateScopedVariableAccess(policyId, arguments.get(1), Set.of("OBJECT"), Set.of("classId"), "delete Transaction arg#2");
        validateScopedVariableAccess(policyId, arguments.get(2), Set.of("ENVIRONMENT"), Set.of("semester"), "delete Transaction arg#3");
    }

    private void validateAuditLogStatement(String policyId, EObject stmt) {
        List<EObject> arguments = childExpressions(stmt, "arguments");
        if (arguments.size() != 5) {
            throw new IllegalStateException("Policy " + policyId + " create AuditLog(...) must have exactly 5 arguments.");
        }

        validateScopedVariableAccess(policyId, arguments.get(0), Set.of("REQUEST"), Set.of("requestId"), "create AuditLog arg#1");
        validateScopedVariableAccess(policyId, arguments.get(1), Set.of("SUBJECT"), Set.of("studentId"), "create AuditLog arg#2");
        validateScopedVariableAccess(policyId, arguments.get(2), Set.of("OBJECT"), Set.of("classId"), "create AuditLog arg#3");
        validateScopedVariableAccess(policyId, arguments.get(3), Set.of("REQUEST"), Set.of("decision"), "create AuditLog arg#4");
        validateScopedVariableAccess(policyId, arguments.get(4), Set.of("REQUEST"), Set.of("failedPolicyCodes"), "create AuditLog arg#5");
    }

    private void validateExpression(String policyId,
                                    EObject expr,
                                    String phase,
                                    String usageLabel,
                                    boolean mustBeBooleanAtRoot) {
        if (expr == null) {
            throw new IllegalStateException("Policy " + policyId + " has null expression at " + usageLabel);
        }

        String className = expr.eClass().getName();
        switch (className) {
            case "LogicalOperator", "RelationalOperator", "ArithmeticOperator" -> validateChildExpressions(policyId, expr, phase, usageLabel);
            case "FunctionCall" -> validateFunctionCall(policyId, expr, phase, usageLabel, mustBeBooleanAtRoot);
            case "VariableAccess" -> validateVariableAccess(policyId, expr, usageLabel);
            case "Constant" -> validateConstant(policyId, expr, usageLabel, mustBeBooleanAtRoot);
            case "ListConstant" -> {
            }
            default -> throw new IllegalStateException(
                    "Policy " + policyId + " contains unsupported expression type " + className + " at " + usageLabel);
        }
    }

    private void validateChildExpressions(String policyId, EObject expr, String phase, String usageLabel) {
        expr.eContents().forEach(child -> validateExpression(policyId, child, phase, usageLabel, false));
    }

    private void validateFunctionCall(String policyId,
                                      EObject expr,
                                      String phase,
                                      String usageLabel,
                                      boolean mustBeBooleanAtRoot) {
        String functionName = stringValue(expr, "functionName");
        PolicyFunctionRegistry.FunctionSpec spec = functionRegistry.getRequired(functionName);
        if (!spec.allowedPhases().contains(phase)) {
            throw new IllegalStateException("Policy " + policyId + " uses function " + functionName + " in disallowed phase " + phase);
        }

        List<EObject> arguments = childExpressions(expr, "arguments");
        if (arguments.size() != spec.arity()) {
            throw new IllegalStateException("Policy " + policyId + " function " + functionName + " expects " + spec.arity() + " arguments.");
        }

        for (EObject argument : arguments) {
            validateExpression(policyId, argument, phase, usageLabel + ":" + functionName, false);
        }

        if (mustBeBooleanAtRoot && spec.returnType() != PolicyFunctionRegistry.ReturnType.BOOLEAN) {
            throw new IllegalStateException("Policy " + policyId + " function " + functionName + " must return BOOLEAN at " + usageLabel);
        }
    }

    private void validateVariableAccess(String policyId, EObject expr, String usageLabel) {
        String entity = enumName(expr, "entity");
        String path = stringValue(expr, "path");

        if ("REQUEST".equals(entity) && !ALLOWED_REQUEST_AUDIT_PATHS.contains(path)
                && !Set.of("studentId", "classId", "actionType").contains(path)) {
            throw new IllegalStateException("Policy " + policyId + " uses unsupported REQUEST path '" + path + "' at " + usageLabel);
        }

        validatePathExists(policyId, entity, path, usageLabel);
    }

    private void validateConstant(String policyId, EObject expr, String usageLabel, boolean mustBeBooleanAtRoot) {
        if (!mustBeBooleanAtRoot) return;

        String type = enumName(expr, "type");
        if (!"BOOLEAN".equals(type)) {
            throw new IllegalStateException("Policy " + policyId + " root constant at " + usageLabel + " must be BOOLEAN.");
        }
    }

    @SuppressWarnings("unchecked")
    private List<EObject> childExpressions(EObject obj, String featureName) {
        return (List<EObject>) obj.eGet(obj.eClass().getEStructuralFeature(featureName));
    }

    private void validateScopedVariableAccess(String policyId,
                                              EObject expr,
                                              Set<String> allowedEntities,
                                              Set<String> allowedPaths,
                                              String usageLabel) {
        ensureClass(policyId, expr, "VariableAccess", usageLabel);
        String entity = enumName(expr, "entity");
        String path = stringValue(expr, "path");

        if (!allowedEntities.contains(entity) || !allowedPaths.contains(path)) {
            throw new IllegalStateException("Policy " + policyId + " has invalid " + usageLabel + ": " + entity + "." + path);
        }

        validatePathExists(policyId, entity, path, usageLabel);
    }

    private void validateActionLiteral(String policyId, EObject expr, String usageLabel) {
        ensureClass(policyId, expr, "Constant", usageLabel);
        String action = stringValue(expr, "value");
        if (!ALLOWED_ACTION_TYPES.contains(action)) {
            throw new IllegalStateException("Policy " + policyId + " has invalid " + usageLabel + ": " + action);
        }
    }

    private void validatePathExists(String policyId, String entity, String path, String usageLabel) {
        if (path == null || path.isBlank()) {
            throw new IllegalStateException("Policy " + policyId + " has blank path at " + usageLabel);
        }

        Class<?> rootClass = switch (entity) {
            case "SUBJECT" -> Student.class;
            case "OBJECT" -> ClassSection.class;
            case "ENVIRONMENT" -> Environment.class;
            case "REQUEST" -> UconRequest.class;
            default -> throw new IllegalStateException("Policy " + policyId + " uses unsupported entity scope " + entity + " at " + usageLabel);
        };

        Class<?> currentClass = rootClass;
        for (String segment : path.split("\\.")) {
            currentClass = getterReturnType(currentClass, segment);
            if (currentClass == null) {
                throw new IllegalStateException("Policy " + policyId + " references unknown path " + entity + "." + path + " at " + usageLabel);
            }
        }
    }

    private Class<?> getterReturnType(Class<?> type, String property) {
        String suffix = property.substring(0, 1).toUpperCase() + property.substring(1);
        try {
            Method getter = type.getMethod("get" + suffix);
            return getter.getReturnType();
        } catch (NoSuchMethodException ignored) {
        }

        try {
            Method getter = type.getMethod("is" + suffix);
            return getter.getReturnType();
        } catch (NoSuchMethodException ignored) {
        }

        return null;
    }

    private void ensureClass(String policyId, EObject obj, String expectedClass, String usageLabel) {
        EClass eClass = obj.eClass();
        if (!expectedClass.equals(eClass.getName())) {
            throw new IllegalStateException("Policy " + policyId + " expects " + expectedClass + " in " + usageLabel + " but found " + eClass.getName());
        }
    }

    private void ensureBindingPresent(String policyId, EObject policy, String featureName, Set<String> allowedValues) {
        String value = stringValue(policy, featureName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Policy " + policyId + " must define " + featureName + ".");
        }
        if (!allowedValues.contains(value)) {
            throw new IllegalStateException("Policy " + policyId + " has unsupported " + featureName + ": " + value);
        }
    }

    private String stringValue(EObject obj, String featureName) {
        Object val = obj.eGet(obj.eClass().getEStructuralFeature(featureName));
        return val == null ? null : val.toString();
    }

    private String enumName(EObject obj, String featureName) {
        Object val = obj.eGet(obj.eClass().getEStructuralFeature(featureName));
        if (val instanceof EEnumLiteral literal) {
            return literal.getName();
        }
        return val == null ? null : val.toString();
    }
}
