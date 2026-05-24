package vn.edu.kma.ucon.engine.update;

import java.util.List;

import org.eclipse.emf.ecore.EObject;

public record PlannedPolicyUpdate(
        String policyId,
        String predicate,
        List<EObject> statements) {
}
