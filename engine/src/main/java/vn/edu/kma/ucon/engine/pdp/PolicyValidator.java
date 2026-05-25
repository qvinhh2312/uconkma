package vn.edu.kma.ucon.engine.pdp;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.springframework.stereotype.Component;

@Component
/**
 * Enforces semantic, schema, and mutability rules so only valid policy models
 * are accepted into the runtime PDP.
 */
public class PolicyValidator {

    private final PolicyModelSemanticValidator semanticValidator;
    private final AttributeSchema attributeSchema;

    public PolicyValidator(PolicyModelSemanticValidator semanticValidator, AttributeSchema attributeSchema) {
        this.semanticValidator = semanticValidator;
        this.attributeSchema = attributeSchema;
    }

    public void validate(EObject policyModelRoot) {
        semanticValidator.validate(policyModelRoot);
        validateAgainstAttributeSchema(policyModelRoot);
    }

    @SuppressWarnings("unchecked")
    private void validateAgainstAttributeSchema(EObject policyModelRoot) {
        if (policyModelRoot == null) {
            return;
        }

        List<EObject> policies = (List<EObject>) policyModelRoot.eGet(policyModelRoot.eClass().getEStructuralFeature("policies"));
        for (EObject policy : policies) {
            String policyId = String.valueOf(policy.eGet(policy.eClass().getEStructuralFeature("policyId")));
            validateStatements(policyId, (List<EObject>) policy.eGet(policy.eClass().getEStructuralFeature("preUpdates")));
            validateStatements(policyId, (List<EObject>) policy.eGet(policy.eClass().getEStructuralFeature("ongoingUpdates")));
            validateStatements(policyId, (List<EObject>) policy.eGet(policy.eClass().getEStructuralFeature("postUpdates")));
            validateStatements(policyId, (List<EObject>) policy.eGet(policy.eClass().getEStructuralFeature("rollbackUpdates")));
        }
    }

    private void validateStatements(String policyId, List<EObject> statements) {
        if (statements == null) {
            return;
        }
        for (EObject stmt : statements) {
            if (!"UpdateStatement".equals(stmt.eClass().getName())) {
                continue;
            }
            EObject target = (EObject) stmt.eGet(stmt.eClass().getEStructuralFeature("target"));
            String scope = enumName(target, "entity");
            String path = String.valueOf(target.eGet(target.eClass().getEStructuralFeature("path")));

            if (!attributeSchema.hasAttribute(scope, path)) {
                throw new IllegalStateException("Policy " + policyId + " updates unknown path " + scope + "." + path + " according to attribute-schema.yml");
            }
            if (!attributeSchema.isMutable(scope, path)) {
                throw new IllegalStateException("Policy " + policyId + " updates immutable path " + scope + "." + path + " according to attribute-schema.yml");
            }
        }
    }

    private String enumName(EObject obj, String featureName) {
        Object val = obj.eGet(obj.eClass().getEStructuralFeature(featureName));
        return val == null ? null : val.toString();
    }
}
