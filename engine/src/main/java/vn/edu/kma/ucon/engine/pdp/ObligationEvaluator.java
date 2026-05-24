package vn.edu.kma.ucon.engine.pdp;

import org.springframework.stereotype.Component;

import vn.edu.kma.ucon.engine.pep.UconRequest;
import vn.edu.kma.ucon.engine.pip.entity.ClassSection;
import vn.edu.kma.ucon.engine.pip.entity.Student;

@Component
public class ObligationEvaluator {

    private final PolicyEngine policyEngine;

    public ObligationEvaluator(PolicyEngine policyEngine) {
        this.policyEngine = policyEngine;
    }

    public PhaseEvaluationResult evaluate(Phase phase, Student subject, ClassSection object, Environment env, UconRequest request) {
        return policyEngine.evaluateWithTrace(phase, PredicateType.OBLIGATION, subject, object, env, request);
    }
}
