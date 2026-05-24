package vn.edu.kma.ucon.engine.pdp;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import vn.edu.kma.ucon.engine.pep.UconRequest;
import vn.edu.kma.ucon.engine.pip.entity.ClassSection;
import vn.edu.kma.ucon.engine.pip.entity.Student;

@Service
public class PolicyEngine {

    private static final Logger log = LoggerFactory.getLogger(PolicyEngine.class);

    private final PolicyDecisionPoint pdp;
    private final ExpressionEvaluator evaluator;

    public PolicyEngine(PolicyDecisionPoint pdp, ExpressionEvaluator evaluator) {
        this.pdp = pdp;
        this.evaluator = evaluator;
    }

    @SuppressWarnings("unchecked")
    public AuthDecision evaluatePhase(String phase, Student subject, ClassSection obj, Environment env, UconRequest req) {
        return evaluatePhaseWithTrace(phase, subject, obj, env, req).decision();
    }

    @SuppressWarnings("unchecked")
    public PhaseEvaluationResult evaluatePhaseWithTrace(String phase, Student subject, ClassSection obj, Environment env, UconRequest req) {
        EObject root = pdp.getPolicyModelRoot();
        if (root == null) {
            PhaseTrace emptyTrace = new PhaseTrace(phase, req.getActionType(), "ALLOW", null, null, List.of(), List.of(), List.of());
            return new PhaseEvaluationResult(new AuthDecision(true, null, null), emptyTrace);
        }

        List<EObject> policies = (List<EObject>) root.eGet(root.eClass().getEStructuralFeature("policies"));

        List<EObject> phasePolicies = collectPhasePolicies(policies, phase, req);
        List<PolicyTraceEntry> entries = new ArrayList<>();

        log.info("[PHASE START] phase={} action={} requestId={} policies={}",
                phase, req.getActionType(), req.getRequestId(), phasePolicies.size());

        for (EObject policy : phasePolicies) {
            String ruleId = (String) policy.eGet(policy.eClass().getEStructuralFeature("policyId"));
            EEnumLiteral predicate = (EEnumLiteral) policy.eGet(policy.eClass().getEStructuralFeature("predicate"));
            EObject condition = (EObject) policy.eGet(policy.eClass().getEStructuralFeature("condition"));
            EEnumLiteral effect = (EEnumLiteral) policy.eGet(policy.eClass().getEStructuralFeature("effect"));
            String denyReason = (String) policy.eGet(policy.eClass().getEStructuralFeature("denyReason"));

            boolean match = evaluator.evaluateCondition(condition, subject, obj, env, req);
            log.info("[POLICY CHECK] phase={} predicate={} policy={} effect={} matched={} denyReason={}",
                    phase, predicate.getName(), ruleId, effect.getName(), match, denyReason);

            if (match && "DENY".equals(effect.getName())) {
                log.warn("Policy {} blocked request.", ruleId);
                entries.add(new PolicyTraceEntry(ruleId, predicate.getName(), effect.getName(), true, true, denyReason));
                AuthDecision decision = new AuthDecision(false, denyReason != null ? denyReason : ruleId, ruleId);
                PhaseTrace trace = new PhaseTrace(phase, req.getActionType(), "DENY", ruleId, decision.getFailedCode(), entries, List.of(), List.of());
                return new PhaseEvaluationResult(decision, trace);
            }
            if (!match && "PERMIT".equals(effect.getName())) {
                log.warn("[POLICY BLOCK] phase={} policy={} failedCode={}",
                        phase, ruleId, denyReason != null ? denyReason : ruleId);
                entries.add(new PolicyTraceEntry(ruleId, predicate.getName(), effect.getName(), false, true, denyReason));
                AuthDecision decision = new AuthDecision(false, denyReason != null ? denyReason : ruleId, ruleId);
                PhaseTrace trace = new PhaseTrace(phase, req.getActionType(), "DENY", ruleId, decision.getFailedCode(), entries, List.of(), List.of());
                return new PhaseEvaluationResult(decision, trace);
            }
            entries.add(new PolicyTraceEntry(ruleId, predicate.getName(), effect.getName(), match, false, denyReason));
        }

        log.info("[PHASE PASS] phase={} action={} requestId={}", phase, req.getActionType(), req.getRequestId());
        AuthDecision decision = new AuthDecision(true, null, null);
        PhaseTrace trace = new PhaseTrace(phase, req.getActionType(), "ALLOW", null, null, entries, List.of(), List.of());
        return new PhaseEvaluationResult(decision, trace);
    }

    public List<String> executeUpdatesForPhase(String phase, Student subject, ClassSection obj, Environment env, UconRequest req) {
        return executeUpdateSection(phase, subject, obj, env, req, false);
    }

    public List<String> executeAuditLogOnly(Student subject, ClassSection obj, Environment env, UconRequest req) {
        return executeUpdateSection("POST", subject, obj, env, req, true);
    }

    @SuppressWarnings("unchecked")
    public List<String> executeRollbackUpdatesForPhase(String phase, Student subject, ClassSection obj, Environment env, UconRequest req) {
        EObject root = pdp.getPolicyModelRoot();
        if (root == null) return List.of();

        List<EObject> policies = (List<EObject>) root.eGet(root.eClass().getEStructuralFeature("policies"));
        List<EObject> phasePolicies = collectPhasePolicies(policies, phase, req);
        List<String> appliedPolicies = new ArrayList<>();
        for (EObject policy : phasePolicies) {
            String ruleId = (String) policy.eGet(policy.eClass().getEStructuralFeature("policyId"));
            EObject condition = (EObject) policy.eGet(policy.eClass().getEStructuralFeature("condition"));
            boolean match = evaluator.evaluateCondition(condition, subject, obj, env, req);
            if (match) {
                List<EObject> rollbackUpdates = statements(policy, "rollbackUpdates");
                if (!rollbackUpdates.isEmpty()) {
                    log.info("[ROLLBACK UPDATE] phase={} action={} requestId={} policy={} statements={}",
                            phase, req.getActionType(), req.getRequestId(), ruleId, rollbackUpdates.size());
                    evaluator.executeStatements(rollbackUpdates, subject, obj, env, req);
                    appliedPolicies.add(ruleId);
                }
            }
        }
        return appliedPolicies;
    }

    @SuppressWarnings("unchecked")
    private List<String> executeUpdateSection(String phase, Student subject, ClassSection obj, Environment env, UconRequest req, boolean auditLogOnly) {
        EObject root = pdp.getPolicyModelRoot();
        if (root == null) return List.of();

        List<EObject> policies = (List<EObject>) root.eGet(root.eClass().getEStructuralFeature("policies"));
        List<EObject> phasePolicies = collectPhasePolicies(policies, phase, req);
        List<String> appliedPolicies = new ArrayList<>();

        for (EObject policy : phasePolicies) {
            String ruleId = (String) policy.eGet(policy.eClass().getEStructuralFeature("policyId"));
            EEnumLiteral predicate = (EEnumLiteral) policy.eGet(policy.eClass().getEStructuralFeature("predicate"));
            EObject condition = (EObject) policy.eGet(policy.eClass().getEStructuralFeature("condition"));

            boolean match = evaluator.evaluateCondition(condition, subject, obj, env, req);

            if (match) {
                List<EObject> phaseUpdates = statements(policy, updateFeatureName(phase));
                if (auditLogOnly) {
                    List<EObject> auditOnly = phaseUpdates.stream()
                        .filter(s -> "AuditLogStatement".equals(s.eClass().getName()))
                        .collect(Collectors.toList());
                    if (!auditOnly.isEmpty()) {
                        log.info("[UPDATES] mode=AUDIT_ONLY phase={} action={} predicate={} requestId={} policy={} statements={}",
                                phase, req.getActionType(), predicate.getName(), req.getRequestId(), ruleId, auditOnly.size());
                        evaluator.executeStatements(auditOnly, subject, obj, env, req);
                        appliedPolicies.add(ruleId);
                    }
                } else if (!phaseUpdates.isEmpty()) {
                    log.info("[UPDATES] mode=FULL phase={} action={} predicate={} requestId={} policy={} statements={}",
                            phase, req.getActionType(), predicate.getName(), req.getRequestId(), ruleId, phaseUpdates.size());
                    evaluator.executeStatements(phaseUpdates, subject, obj, env, req);
                    appliedPolicies.add(ruleId);
                }
            }
        }
        return appliedPolicies;
    }

    private List<EObject> collectPhasePolicies(List<EObject> policies, String phase, UconRequest req) {
        return policies.stream()
                .filter(p -> {
                    EEnumLiteral phaseLiteral = (EEnumLiteral) p.eGet(p.eClass().getEStructuralFeature("phase"));
                    EEnumLiteral targetAction = (EEnumLiteral) p.eGet(p.eClass().getEStructuralFeature("targetAction"));
                    boolean phaseMatch = phase.equals(phaseLiteral.getName());
                    boolean actionMatch = "ANY".equals(targetAction.getName()) ||
                            (req.getActionType() != null && targetAction.getName().equalsIgnoreCase(req.getActionType()));
                    return phaseMatch && actionMatch;
                })
                .sorted((p1, p2) -> {
                    Integer prio1 = (Integer) p1.eGet(p1.eClass().getEStructuralFeature("priority"));
                    Integer prio2 = (Integer) p2.eGet(p2.eClass().getEStructuralFeature("priority"));
                    int byPriority = prio2.compareTo(prio1);
                    if (byPriority != 0) {
                        return byPriority;
                    }
                    String id1 = (String) p1.eGet(p1.eClass().getEStructuralFeature("policyId"));
                    String id2 = (String) p2.eGet(p2.eClass().getEStructuralFeature("policyId"));
                    return id1.compareTo(id2);
                })
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<EObject> statements(EObject policy, String featureName) {
        Object value = policy.eGet(policy.eClass().getEStructuralFeature(featureName));
        return value == null ? List.of() : (List<EObject>) value;
    }

    private String updateFeatureName(String phase) {
        return switch (phase) {
            case "PRE" -> "preUpdates";
            case "ONGOING" -> "ongoingUpdates";
            case "POST" -> "postUpdates";
            default -> throw new IllegalArgumentException("Unsupported phase for updates: " + phase);
        };
    }
}
