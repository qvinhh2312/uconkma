package vn.edu.kma.ucon.engine.pdp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.springframework.stereotype.Service;

@Service
public class PolicyLifecycleService {

    private static final Map<String, List<String>> ALLOWED_TRANSITIONS = Map.of(
            "DRAFT", List.of("VALIDATED"),
            "VALIDATED", List.of("ACTIVE"),
            "ACTIVE", List.of("DEPRECATED"),
            "DEPRECATED", List.of("ARCHIVED"),
            "ARCHIVED", List.of());

    private final PolicyDecisionPoint policyDecisionPoint;

    public PolicyLifecycleService(PolicyDecisionPoint policyDecisionPoint) {
        this.policyDecisionPoint = policyDecisionPoint;
    }

    @SuppressWarnings("unchecked")
    public synchronized List<PolicyLifecycleInfo> listPolicies() {
        EObject root = policyDecisionPoint.getAuthoringPolicyModelRoot();
        if (root == null) {
            return List.of();
        }
        List<EObject> policies = (List<EObject>) root.eGet(root.eClass().getEStructuralFeature("policies"));
        return policies.stream()
                .map(this::toInfo)
                .toList();
    }

    public synchronized Map<String, Long> summarizeStatuses() {
        Map<String, Long> summary = new LinkedHashMap<>();
        for (String status : List.of("DRAFT", "VALIDATED", "ACTIVE", "DEPRECATED", "ARCHIVED")) {
            summary.put(status, listPolicies().stream().filter(policy -> status.equals(policy.status())).count());
        }
        summary.put("RUNTIME_ACTIVE_POLICIES", (long) listRuntimePolicyIds().size());
        return summary;
    }

    public synchronized PolicyLifecycleInfo transitionPolicy(String policyId, String targetStatus) {
        String normalizedTargetStatus = normalizeTargetStatus(targetStatus);
        EObject root = policyDecisionPoint.getAuthoringPolicyModelRoot();
        if (root == null) {
            throw new IllegalStateException("No authoring policy model is loaded.");
        }

        EObject workingRoot = EcoreUtil.copy(root);
        EObject policy = findPolicyById(workingRoot, policyId);
        if (policy == null) {
            throw new IllegalArgumentException("Policy not found: " + policyId);
        }

        String currentStatus = enumName(policy, "policyStatus");
        if (normalizedTargetStatus.equals(currentStatus)) {
            policyDecisionPoint.replacePolicyModel(workingRoot);
            return toInfo(findPolicyById(policyDecisionPoint.getAuthoringPolicyModelRoot(), policyId));
        }

        List<String> allowedTargets = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, List.of());
        if (!allowedTargets.contains(normalizedTargetStatus)) {
            throw new IllegalStateException("Invalid lifecycle transition for policy " + policyId
                    + ": " + currentStatus + " -> " + normalizedTargetStatus);
        }

        policy.eSet(policy.eClass().getEStructuralFeature("policyStatus"), enumLiteral(policy, "PolicyStatus", normalizedTargetStatus));
        policyDecisionPoint.replacePolicyModel(workingRoot);
        return toInfo(findPolicyById(policyDecisionPoint.getAuthoringPolicyModelRoot(), policyId));
    }

    private String normalizeTargetStatus(String targetStatus) {
        if (targetStatus == null || targetStatus.isBlank()) {
            throw new IllegalArgumentException("targetStatus is required.");
        }
        String normalized = targetStatus.trim().toUpperCase();
        if (!ALLOWED_TRANSITIONS.containsKey(normalized)) {
            throw new IllegalArgumentException("Invalid policy status: " + targetStatus);
        }
        return normalized;
    }

    @SuppressWarnings("unchecked")
    public synchronized List<String> listRuntimePolicyIds() {
        EObject runtimeRoot = policyDecisionPoint.getPolicyModelRoot();
        if (runtimeRoot == null) {
            return List.of();
        }
        List<EObject> policies = (List<EObject>) runtimeRoot.eGet(runtimeRoot.eClass().getEStructuralFeature("policies"));
        return policies.stream().map(policy -> stringValue(policy, "policyId")).toList();
    }

    public synchronized void reloadFromDisk() {
        policyDecisionPoint.reload();
    }

    @SuppressWarnings("unchecked")
    private EObject findPolicyById(EObject root, String policyId) {
        List<EObject> policies = (List<EObject>) root.eGet(root.eClass().getEStructuralFeature("policies"));
        return policies.stream()
                .filter(policy -> policyId.equals(stringValue(policy, "policyId")))
                .findFirst()
                .orElse(null);
    }

    private PolicyLifecycleInfo toInfo(EObject policy) {
        return new PolicyLifecycleInfo(
                stringValue(policy, "policyId"),
                enumName(policy, "policyStatus"),
                stringValue(policy, "source"),
                stringValue(policy, "version"),
                stringValue(policy, "uconVariant"));
    }

    private Object enumLiteral(EObject obj, String enumName, String literalName) {
        EClass eClass = obj.eClass();
        EEnum eEnum = (EEnum) eClass.getEPackage().getEClassifier(enumName);
        return eEnum.getEEnumLiteral(literalName).getInstance();
    }

    private String stringValue(EObject obj, String featureName) {
        Object value = obj.eGet(obj.eClass().getEStructuralFeature(featureName));
        return value == null ? null : value.toString();
    }

    private String enumName(EObject obj, String featureName) {
        Object value = obj.eGet(obj.eClass().getEStructuralFeature(featureName));
        return value == null ? null : value.toString();
    }
}
