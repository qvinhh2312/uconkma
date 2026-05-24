package vn.edu.kma.ucon.engine.pdp;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class PolicyCombiner {

    public AuthDecision combine(CombiningAlgorithm algorithm, List<PolicyEvaluation> evaluations) {
        return switch (algorithm) {
            case DENY_OVERRIDES, PRIORITY_ORDER, FIRST_APPLICABLE -> combineDenyOverrides(evaluations);
            case PERMIT_OVERRIDES -> combinePermitOverrides(evaluations);
            case ONLY_ONE_APPLICABLE -> combineOnlyOneApplicable(evaluations);
        };
    }

    private AuthDecision combineDenyOverrides(List<PolicyEvaluation> evaluations) {
        for (PolicyEvaluation evaluation : evaluations) {
            if ("DENY".equals(evaluation.effect()) && evaluation.matched()) {
                return new AuthDecision(false, coalesceReason(evaluation), evaluation.policyId());
            }
            if ("PERMIT".equals(evaluation.effect()) && !evaluation.matched()) {
                return new AuthDecision(false, coalesceReason(evaluation), evaluation.policyId());
            }
        }
        return new AuthDecision(true, null, null);
    }

    private AuthDecision combinePermitOverrides(List<PolicyEvaluation> evaluations) {
        boolean hasPermit = false;
        for (PolicyEvaluation evaluation : evaluations) {
            if ("PERMIT".equals(evaluation.effect()) && evaluation.matched()) {
                hasPermit = true;
            }
            if ("DENY".equals(evaluation.effect()) && evaluation.matched()) {
                return new AuthDecision(false, coalesceReason(evaluation), evaluation.policyId());
            }
        }
        return hasPermit ? new AuthDecision(true, null, null) : new AuthDecision(false, "NO_APPLICABLE_POLICY", null);
    }

    private AuthDecision combineOnlyOneApplicable(List<PolicyEvaluation> evaluations) {
        long applicable = evaluations.stream()
                .filter(e -> ("DENY".equals(e.effect()) && e.matched()) || ("PERMIT".equals(e.effect()) && e.matched()))
                .count();
        if (applicable > 1) {
            return new AuthDecision(false, "MULTIPLE_APPLICABLE_POLICIES", null);
        }
        return combineDenyOverrides(evaluations);
    }

    private String coalesceReason(PolicyEvaluation evaluation) {
        return evaluation.denyReason() != null ? evaluation.denyReason() : evaluation.policyId();
    }
}
