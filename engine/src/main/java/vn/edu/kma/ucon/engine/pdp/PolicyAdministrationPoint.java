package vn.edu.kma.ucon.engine.pdp;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.springframework.stereotype.Service;

@Service
public class PolicyAdministrationPoint {

    @SuppressWarnings("unchecked")
    public EObject activateValidatedPolicies(EObject policyModelRoot) {
        if (policyModelRoot == null) {
            return null;
        }

        EObject copy = EcoreUtil.copy(policyModelRoot);
        List<EObject> policies = (List<EObject>) copy.eGet(copy.eClass().getEStructuralFeature("policies"));
        Set<String> activePolicyIds = new HashSet<>();

        policies.removeIf(policy -> {
            String policyStatus = enumName(policy, "policyStatus");
            boolean active = "ACTIVE".equals(policyStatus);
            if (active) {
                activePolicyIds.add(stringValue(policy, "policyId"));
            }
            return !active;
        });

        List<EObject> policySets = (List<EObject>) copy.eGet(copy.eClass().getEStructuralFeature("policySets"));
        for (EObject policySet : policySets) {
            List<String> policyIds = (List<String>) policySet.eGet(policySet.eClass().getEStructuralFeature("policyIds"));
            policyIds.removeIf(policyId -> !activePolicyIds.contains(policyId));
        }

        return copy;
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
