package vn.edu.kma.ucon.engine.pdp;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import vn.edu.kma.ucon.engine.pep.UconRequest;
import vn.edu.kma.ucon.engine.pip.entity.ClassSection;
import vn.edu.kma.ucon.engine.pip.entity.Student;
import vn.edu.kma.ucon.engine.update.PlannedPolicyUpdate;
import vn.edu.kma.ucon.engine.update.UpdatePlan;

@Service
/**
 * Core runtime evaluator that filters active policies by phase/predicate/action,
 * executes conditions, combines decisions, and prepares update plans.
 */
public class PolicyEngine {

    private static final Logger log = LoggerFactory.getLogger(PolicyEngine.class);

    private final PolicyDecisionPoint pdp;
    private final ExpressionEvaluator evaluator;
    private final PolicyCombiner policyCombiner;

    public PolicyEngine(PolicyDecisionPoint pdp, ExpressionEvaluator evaluator, PolicyCombiner policyCombiner) {
        this.pdp = pdp;
        this.evaluator = evaluator;
        this.policyCombiner = policyCombiner;
    }

    public AuthDecision evaluatePhase(String phase, Student subject, ClassSection obj, Environment env, UconRequest req) {
        Phase phaseEnum = Phase.valueOf(phase);
        for (PredicateType predicate : orderedPredicatesFor(phaseEnum)) {
            AuthDecision decision = evaluate(phaseEnum, predicate, subject, obj, env, req);
            if (!decision.isPermit()) {
                return decision;
            }
        }
        return new AuthDecision(true, null, null);
    }

    public PhaseEvaluationResult evaluatePhaseWithTrace(String phase, Student subject, ClassSection obj, Environment env, UconRequest req) {
        Phase phaseEnum = Phase.valueOf(phase);
        List<PolicyTraceEntry> mergedEntries = new ArrayList<>();
        for (PredicateType predicate : orderedPredicatesFor(phaseEnum)) {
            PhaseEvaluationResult result = evaluateWithTrace(phaseEnum, predicate, subject, obj, env, req);
            mergedEntries.addAll(result.trace().policies());
            if (!result.decision().isPermit()) {
                PhaseTrace trace = new PhaseTrace(
                        phaseEnum.name(),
                        "ALL",
                        req.getActionType(),
                        "DENY",
                        result.decision().getFailedPolicy(),
                        result.decision().getFailedCode(),
                        mergedEntries,
                        List.of(),
                        List.of());
                return new PhaseEvaluationResult(result.decision(), trace);
            }
        }
        PhaseTrace trace = new PhaseTrace(
                phaseEnum.name(),
                "ALL",
                req.getActionType(),
                "ALLOW",
                null,
                null,
                mergedEntries,
                List.of(),
                List.of());
        return new PhaseEvaluationResult(new AuthDecision(true, null, null), trace);
    }

    public AuthDecision evaluate(Phase phase, PredicateType predicate, Student subject, ClassSection obj, Environment env, UconRequest req) {
        return evaluateWithTrace(phase, predicate, subject, obj, env, req).decision();
    }

    @SuppressWarnings("unchecked")
    public PhaseEvaluationResult evaluateWithTrace(Phase phase, PredicateType predicate, Student subject, ClassSection obj, Environment env, UconRequest req) {
        EObject root = pdp.getPolicyModelRoot();
        if (root == null) {
            PhaseTrace emptyTrace = new PhaseTrace(phase.name(), predicate.name(), req.getActionType(), "ALLOW", null, null, List.of(), List.of(), List.of());
            return new PhaseEvaluationResult(new AuthDecision(true, null, null), emptyTrace);
        }

        List<EObject> policies = (List<EObject>) root.eGet(root.eClass().getEStructuralFeature("policies"));
        List<EObject> matchingPolicies = collectPolicies(root, policies, phase, predicate, req);
        List<PolicyTraceEntry> entries = new ArrayList<>();
        List<PolicyEvaluation> evaluations = new ArrayList<>();

        log.info("[PHASE START] phase={} predicate={} action={} requestId={} policies={}",
                phase.name(), predicate.name(), req.getActionType(), req.getRequestId(), matchingPolicies.size());

        for (EObject policy : matchingPolicies) {
            String ruleId = stringValue(policy, "policyId");
            EObject condition = (EObject) policy.eGet(policy.eClass().getEStructuralFeature("condition"));
            EEnumLiteral effect = (EEnumLiteral) policy.eGet(policy.eClass().getEStructuralFeature("effect"));
            String denyReason = stringValue(policy, "denyReason");

            boolean match = evaluator.evaluateCondition(condition, subject, obj, env, req);
            boolean blocked = ("DENY".equals(effect.getName()) && match) || ("PERMIT".equals(effect.getName()) && !match);

            log.info("[POLICY CHECK] phase={} predicate={} policy={} effect={} matched={} denyReason={}",
                    phase.name(), predicate.name(), ruleId, effect.getName(), match, denyReason);

            entries.add(new PolicyTraceEntry(ruleId, predicate.name(), effect.getName(), match, blocked, denyReason));
            evaluations.add(new PolicyEvaluation(ruleId, predicate.name(), effect.getName(), match, denyReason));
        }

        AuthDecision decision = policyCombiner.combine(resolveCombiningAlgorithm(root), evaluations);
        if (!decision.isPermit()) {
            log.warn("[POLICY BLOCK] phase={} predicate={} failedPolicy={} failedCode={}",
                    phase.name(), predicate.name(), decision.getFailedPolicy(), decision.getFailedCode());
        } else {
            log.info("[PHASE PASS] phase={} predicate={} action={} requestId={}",
                    phase.name(), predicate.name(), req.getActionType(), req.getRequestId());
        }

        PhaseTrace trace = new PhaseTrace(
                phase.name(),
                predicate.name(),
                req.getActionType(),
                decision.isPermit() ? "ALLOW" : "DENY",
                decision.getFailedPolicy(),
                decision.getFailedCode(),
                entries,
                List.of(),
                List.of());
        return new PhaseEvaluationResult(decision, trace);
    }

    public UpdatePlan planUpdatesForPhase(Phase phase, Student subject, ClassSection obj, Environment env, UconRequest req, boolean auditLogOnly) {
        return planSection(phase, sectionName(phase), subject, obj, env, req, auditLogOnly);
    }

    public UpdatePlan planRollbackUpdatesForPhase(Phase phase, Student subject, ClassSection obj, Environment env, UconRequest req) {
        return planSection(phase, "rollbackUpdates", subject, obj, env, req, false);
    }

    @SuppressWarnings("unchecked")
    private UpdatePlan planSection(Phase phase,
                                   String featureName,
                                   Student subject,
                                   ClassSection obj,
                                   Environment env,
                                   UconRequest req,
                                   boolean auditLogOnly) {
        EObject root = pdp.getPolicyModelRoot();
        if (root == null) {
            return UpdatePlan.empty(phase, featureName);
        }

        List<EObject> policies = (List<EObject>) root.eGet(root.eClass().getEStructuralFeature("policies"));
        List<EObject> phasePolicies = collectPolicies(root, policies, phase, null, req);
        List<PlannedPolicyUpdate> plannedPolicies = new ArrayList<>();

        for (EObject policy : phasePolicies) {
            EObject condition = (EObject) policy.eGet(policy.eClass().getEStructuralFeature("condition"));
            if (!evaluator.evaluateCondition(condition, subject, obj, env, req)) {
                continue;
            }

            List<EObject> statements = statements(policy, featureName);
            if (auditLogOnly) {
                statements = statements.stream()
                        .filter(stmt -> "AuditLogStatement".equals(stmt.eClass().getName()))
                        .collect(Collectors.toList());
            }
            if (statements.isEmpty()) {
                continue;
            }

            plannedPolicies.add(new PlannedPolicyUpdate(
                    stringValue(policy, "policyId"),
                    enumName(policy, "predicate"),
                    List.copyOf(statements)));
        }

        return plannedPolicies.isEmpty() ? UpdatePlan.empty(phase, featureName) : new UpdatePlan(phase, featureName, plannedPolicies);
    }

    private String sectionName(Phase phase) {
        return switch (phase) {
            case PRE -> "preUpdates";
            case ONGOING -> "ongoingUpdates";
            case POST -> "postUpdates";
        };
    }

    private List<PredicateType> orderedPredicatesFor(Phase phase) {
        if (phase == Phase.POST) {
            return List.of(PredicateType.AUTHORIZATION, PredicateType.OBLIGATION);
        }
        return List.of(PredicateType.CONDITION, PredicateType.AUTHORIZATION, PredicateType.OBLIGATION);
    }

    private List<EObject> collectPolicies(EObject root, List<EObject> policies, Phase phase, PredicateType predicate, UconRequest req) {
        return policies.stream()
                .filter(policy -> phaseMatches(policy, phase) && actionMatches(policy, req) && predicateMatches(policy, predicate))
                .filter(policy -> belongsToActivePolicySet(root, policy))
                .sorted((left, right) -> {
                    Integer leftPriority = (Integer) left.eGet(left.eClass().getEStructuralFeature("priority"));
                    Integer rightPriority = (Integer) right.eGet(right.eClass().getEStructuralFeature("priority"));
                    int byPriority = rightPriority.compareTo(leftPriority);
                    if (byPriority != 0) {
                        return byPriority;
                    }
                    return stringValue(left, "policyId").compareTo(stringValue(right, "policyId"));
                })
                .collect(Collectors.toList());
    }

    private boolean phaseMatches(EObject policy, Phase phase) {
        EEnumLiteral phaseLiteral = (EEnumLiteral) policy.eGet(policy.eClass().getEStructuralFeature("phase"));
        return phase.name().equals(phaseLiteral.getName());
    }

    private boolean predicateMatches(EObject policy, PredicateType predicate) {
        if (predicate == null) {
            return true;
        }
        EEnumLiteral predicateLiteral = (EEnumLiteral) policy.eGet(policy.eClass().getEStructuralFeature("predicate"));
        return predicate.name().equals(predicateLiteral.getName());
    }

    private boolean belongsToActivePolicySet(EObject root, EObject policy) {
        if (root == null || root.eClass().getEStructuralFeature("policySets") == null) {
            return true;
        }
        @SuppressWarnings("unchecked")
        List<EObject> policySets = (List<EObject>) root.eGet(root.eClass().getEStructuralFeature("policySets"));
        if (policySets == null || policySets.isEmpty()) {
            return true;
        }
        EObject activeSet = policySets.get(0);
        @SuppressWarnings("unchecked")
        List<String> policyIds = (List<String>) activeSet.eGet(activeSet.eClass().getEStructuralFeature("policyIds"));
        return policyIds.contains(stringValue(policy, "policyId"));
    }

    private CombiningAlgorithm resolveCombiningAlgorithm(EObject root) {
        if (root == null || root.eClass().getEStructuralFeature("policySets") == null) {
            return CombiningAlgorithm.DENY_OVERRIDES;
        }
        @SuppressWarnings("unchecked")
        List<EObject> policySets = (List<EObject>) root.eGet(root.eClass().getEStructuralFeature("policySets"));
        if (policySets == null || policySets.isEmpty()) {
            return CombiningAlgorithm.DENY_OVERRIDES;
        }
        Object value = policySets.get(0).eGet(policySets.get(0).eClass().getEStructuralFeature("combiningAlgorithm"));
        if (value instanceof EEnumLiteral literal) {
            return CombiningAlgorithm.valueOf(literal.getName());
        }
        return CombiningAlgorithm.valueOf(value.toString());
    }

    private boolean actionMatches(EObject policy, UconRequest req) {
        EEnumLiteral targetAction = (EEnumLiteral) policy.eGet(policy.eClass().getEStructuralFeature("targetAction"));
        return "ANY".equals(targetAction.getName())
                || (req.getActionType() != null && targetAction.getName().equalsIgnoreCase(req.getActionType()));
    }

    @SuppressWarnings("unchecked")
    private List<EObject> statements(EObject policy, String featureName) {
        Object value = policy.eGet(policy.eClass().getEStructuralFeature(featureName));
        return value == null ? List.of() : (List<EObject>) value;
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
