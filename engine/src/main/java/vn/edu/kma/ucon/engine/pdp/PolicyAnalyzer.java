package vn.edu.kma.ucon.engine.pdp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.springframework.stereotype.Component;

/**
 * Static quality analysis for policy sets beyond validity checks, such as
 * collisions, redundancy, shadowing, and mutation risks.
 */
@Component
public class PolicyAnalyzer {

    @SuppressWarnings("unchecked")
    public PolicyAnalysisReport analyze(EObject policyModelRoot) {
        if (policyModelRoot == null) {
            return new PolicyAnalysisReport(0, 0, List.of());
        }

        List<EObject> policies = (List<EObject>) policyModelRoot.eGet(policyModelRoot.eClass().getEStructuralFeature("policies"));
        List<PolicyAnalysisWarning> warnings = new ArrayList<>();
        Map<String, List<PolicySummary>> priorities = new HashMap<>();
        Map<String, List<PolicySummary>> fingerprints = new HashMap<>();

        boolean hasAuditTrace = false;
        boolean hasDropPreGuard = false;
        boolean hasMutation = false;
        boolean hasRegisterPostMutation = false;
        boolean hasDropPostMutation = false;

        for (EObject policy : policies) {
            String policyId = stringValue(policy, "policyId");
            String predicate = enumName(policy, "predicate");
            String phase = enumName(policy, "phase");
            String updateTiming = enumName(policy, "updateTiming");
            String targetAction = enumName(policy, "targetAction");
            String effect = enumName(policy, "effect");

            EObject condition = (EObject) policy.eGet(policy.eClass().getEStructuralFeature("condition"));
            List<EObject> preUpdates = statements(policy, "preUpdates");
            List<EObject> ongoingUpdates = statements(policy, "ongoingUpdates");
            List<EObject> postUpdates = statements(policy, "postUpdates");
            List<EObject> rollbackUpdates = statements(policy, "rollbackUpdates");
            boolean mutatesState = !preUpdates.isEmpty() || !ongoingUpdates.isEmpty() || !postUpdates.isEmpty();

            hasMutation |= mutatesState;

            PolicySummary summary = new PolicySummary(policyId, predicate, phase, targetAction, effect,
                    stringValue(policy, "priority"), fingerprint(condition));
            priorities.computeIfAbsent(summary.priorityKey(), key -> new ArrayList<>()).add(summary);
            fingerprints.computeIfAbsent(summary.fingerprintKey(), key -> new ArrayList<>()).add(summary);

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

            if ("POST".equals(phase) && "AUTHORIZATION".equals(predicate) && "REGISTER".equals(targetAction) && mutatesState) {
                hasRegisterPostMutation = true;
            }

            if ("POST".equals(phase) && "AUTHORIZATION".equals(predicate) && "DROP".equals(targetAction) && mutatesState) {
                hasDropPostMutation = true;
            }

            if ("POST".equals(phase) && "AUTHORIZATION".equals(predicate) && containsPath(postUpdates, "OBJECT", "enrolled")) {
                warnings.add(new PolicyAnalysisWarning(
                        policyId,
                        "STATEFUL_MUTATION",
                        "Post policy mutates OBJECT.enrolled; ensure runtime invariants are checked after commit."));
            }

            if ("DROP".equals(targetAction) && "POST".equals(phase) && containsPath(postUpdates, "OBJECT", "enrolled")) {
                warnings.add(new PolicyAnalysisWarning(
                        policyId,
                        "UNSAFE_UPDATE",
                        "DROP post-update mutates OBJECT.enrolled; keep a PRE guard to prevent underflow or duplicate DROP handling."));
            }
        }

        priorities.values().stream()
                .filter(bucket -> bucket.size() > 1)
                .forEach(bucket -> warnings.add(analyzePriorityBucket(bucket)));

        fingerprints.values().stream()
                .filter(bucket -> bucket.size() > 1)
                .forEach(bucket -> warnings.add(analyzeFingerprintBucket(bucket)));

        if (!hasAuditTrace) {
            warnings.add(new PolicyAnalysisWarning(
                    "GLOBAL",
                    "MISSING_AUDIT",
                    "No POST obligation creates AuditLog trace for request outcomes."));
        }

        if (hasMutation && !hasAuditTrace) {
            warnings.add(new PolicyAnalysisWarning(
                    "GLOBAL",
                    "MISSING_TRACE_FOR_MUTATION",
                    "Policy set mutates state but does not define a POST audit/trace obligation."));
        }

        if (!hasDropPreGuard) {
            warnings.add(new PolicyAnalysisWarning(
                    "GLOBAL",
                    "DROP_GUARD_MISSING",
                    "DROP flow should have a PRE authorization guard that checks existing registration."));
        }

        if (hasRegisterPostMutation && !hasDropPostMutation) {
            warnings.add(new PolicyAnalysisWarning(
                    "GLOBAL",
                    "INCOMPLETE_DROP_FLOW",
                    "REGISTER flow mutates post state but no DROP post-update policy restores the paired mutable attributes."));
        }

        return new PolicyAnalysisReport(policies.size(), 0, warnings);
    }

    private PolicyAnalysisWarning analyzePriorityBucket(List<PolicySummary> bucket) {
        String policies = bucket.stream().map(PolicySummary::policyId).collect(Collectors.joining(", "));
        long distinctEffects = bucket.stream().map(PolicySummary::effect).distinct().count();
        if (distinctEffects > 1) {
            return new PolicyAnalysisWarning(
                    "GLOBAL",
                    "CONFLICTING_PRIORITY",
                    "Policies share the same phase/predicate/action/priority but disagree on effect: " + policies);
        }
        return new PolicyAnalysisWarning(
                "GLOBAL",
                "PRIORITY_COLLISION",
                "Policies share the same phase/predicate/action/priority and may become harder to reason about: " + policies);
    }

    private PolicyAnalysisWarning analyzeFingerprintBucket(List<PolicySummary> bucket) {
        String policies = bucket.stream().map(PolicySummary::policyId).collect(Collectors.joining(", "));
        long distinctPriorities = bucket.stream().map(PolicySummary::priority).distinct().count();
        if (distinctPriorities > 1) {
            return new PolicyAnalysisWarning(
                    "GLOBAL",
                    "SHADOWING",
                    "Equivalent policies with different priorities can shadow one another: " + policies);
        }
        return new PolicyAnalysisWarning(
                "GLOBAL",
                "REDUNDANT_POLICY",
                "Policies have the same phase/predicate/action/effect/condition signature: " + policies);
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

    private String fingerprint(EObject expr) {
        if (expr == null) {
            return "null";
        }

        StringBuilder builder = new StringBuilder(expr.eClass().getName());
        expr.eClass().getEAllAttributes().forEach(attribute -> {
            Object value = expr.eGet(attribute);
            builder.append('|').append(attribute.getName()).append('=');
            if (value instanceof List<?> list) {
                builder.append('[')
                        .append(list.stream().map(String::valueOf).collect(Collectors.joining(",")))
                        .append(']');
            } else {
                builder.append(String.valueOf(value));
            }
        });
        for (EObject child : expr.eContents()) {
            builder.append('{').append(fingerprint(child)).append('}');
        }
        return builder.toString();
    }

    private record PolicySummary(
            String policyId,
            String predicate,
            String phase,
            String targetAction,
            String effect,
            String priority,
            String fingerprint) {

        private String priorityKey() {
            return phase + "|" + predicate + "|" + targetAction + "|" + priority;
        }

        private String fingerprintKey() {
            return phase + "|" + predicate + "|" + targetAction + "|" + effect + "|" + fingerprint;
        }
    }
}
