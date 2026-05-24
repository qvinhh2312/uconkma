package vn.edu.kma.ucon.engine.pdp;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.springframework.stereotype.Component;

@Component
public class PolicyAnalyzer {

    @SuppressWarnings("unchecked")
    public PolicyAnalysisReport analyze(EObject policyModelRoot) {
        if (policyModelRoot == null) {
            return new PolicyAnalysisReport(0, 0, List.of());
        }

        List<EObject> policies = (List<EObject>) policyModelRoot.eGet(policyModelRoot.eClass().getEStructuralFeature("policies"));
        List<PolicyAnalysisWarning> warnings = new ArrayList<>();

        boolean hasAuditTrace = false;
        boolean hasDropPreGuard = false;

        for (EObject policy : policies) {
            String policyId = stringValue(policy, "policyId");
            String predicate = enumName(policy, "predicate");
            String phase = enumName(policy, "phase");
            String updateTiming = enumName(policy, "updateTiming");
            String targetAction = enumName(policy, "targetAction");

            List<EObject> postUpdates = statements(policy, "postUpdates");
            List<EObject> rollbackUpdates = statements(policy, "rollbackUpdates");

            if ("ONGOING".equals(phase) && "ONGOING".equals(updateTiming) && rollbackUpdates.isEmpty()) {
                warnings.add(new PolicyAnalysisWarning(
                        policyId,
                        "MISSING_ROLLBACK",
                        "Ongoing update policy should define rollbackUpdates."));
            }

            if ("POST".equals(phase) && "OBLIGATION".equals(predicate) && containsStatement(postUpdates, "AuditLogStatement")) {
                hasAuditTrace = true;
            }

            if ("DROP".equals(targetAction) && "PRE".equals(phase) && "AUTHORIZATION".equals(predicate)) {
                hasDropPreGuard = true;
            }

            if ("POST".equals(phase) && "AUTHORIZATION".equals(predicate) && containsPath(postUpdates, "OBJECT", "enrolled")) {
                warnings.add(new PolicyAnalysisWarning(
                        policyId,
                        "STATEFUL_MUTATION",
                        "Post policy mutates OBJECT.enrolled; ensure runtime invariants are checked after commit."));
            }
        }

        if (!hasAuditTrace) {
            warnings.add(new PolicyAnalysisWarning(
                    "GLOBAL",
                    "MISSING_AUDIT",
                    "No POST obligation creates AuditLog trace for request outcomes."));
        }

        if (!hasDropPreGuard) {
            warnings.add(new PolicyAnalysisWarning(
                    "GLOBAL",
                    "DROP_GUARD_MISSING",
                    "DROP flow should have a PRE authorization guard that checks existing registration."));
        }

        return new PolicyAnalysisReport(policies.size(), 0, warnings);
    }

    private boolean containsStatement(List<EObject> statements, String className) {
        return statements.stream().anyMatch(stmt -> className.equals(stmt.eClass().getName()));
    }

    private boolean containsPath(List<EObject> statements, String scope, String path) {
        return statements.stream()
                .filter(stmt -> "UpdateStatement".equals(stmt.eClass().getName()))
                .map(stmt -> (EObject) stmt.eGet(stmt.eClass().getEStructuralFeature("target")))
                .anyMatch(target -> scope.equals(enumName(target, "entity")) && path.equals(stringValue(target, "path")));
    }

    @SuppressWarnings("unchecked")
    private List<EObject> statements(EObject policy, String featureName) {
        Object value = policy.eGet(policy.eClass().getEStructuralFeature(featureName));
        return value == null ? List.of() : (List<EObject>) value;
    }

    private String stringValue(EObject obj, String featureName) {
        Object val = obj.eGet(obj.eClass().getEStructuralFeature(featureName));
        return val == null ? null : val.toString();
    }

    private String enumName(EObject obj, String featureName) {
        Object val = obj.eGet(obj.eClass().getEStructuralFeature(featureName));
        return val == null ? null : val.toString();
    }
}
