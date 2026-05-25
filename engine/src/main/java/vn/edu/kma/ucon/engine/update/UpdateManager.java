package vn.edu.kma.ucon.engine.update;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import vn.edu.kma.ucon.engine.pep.UconRequest;
import vn.edu.kma.ucon.engine.pdp.Environment;
import vn.edu.kma.ucon.engine.pdp.ExpressionEvaluator;
import vn.edu.kma.ucon.engine.pdp.Phase;
import vn.edu.kma.ucon.engine.pdp.PolicyEngine;
import vn.edu.kma.ucon.engine.pip.entity.ClassSection;
import vn.edu.kma.ucon.engine.pip.entity.Student;

@Service
/**
 * Builds and executes mutable update plans emitted by the policy engine for a
 * given UCON phase.
 */
public class UpdateManager {

    private final PolicyEngine policyEngine;
    private final ExpressionEvaluator evaluator;

    public UpdateManager(PolicyEngine policyEngine, ExpressionEvaluator evaluator) {
        this.policyEngine = policyEngine;
        this.evaluator = evaluator;
    }

    public UpdatePlan buildPlan(Phase phase, Student subject, ClassSection object, Environment env, UconRequest request) {
        return policyEngine.planUpdatesForPhase(phase, subject, object, env, request, false);
    }

    public UpdatePlan buildAuditOnlyPlan(Phase phase, Student subject, ClassSection object, Environment env, UconRequest request) {
        return policyEngine.planUpdatesForPhase(phase, subject, object, env, request, true);
    }

    public List<String> apply(UpdatePlan plan, Student subject, ClassSection object, Environment env, UconRequest request) {
        List<String> appliedPolicies = new ArrayList<>();
        for (PlannedPolicyUpdate policyUpdate : plan.plannedPolicies()) {
            evaluator.executeStatements(policyUpdate.statements(), subject, object, env, request);
            appliedPolicies.add(policyUpdate.policyId());
        }
        return appliedPolicies;
    }
}
