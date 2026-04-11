package vn.edu.kma.ucon.engine.pdp;

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
        log.info("Evaluating policies for phase: {}", phase);
        
        EObject root = pdp.getPolicyModelRoot();
        if (root == null) return new AuthDecision(true, null); // Default allow if no policies

        List<EObject> policies = (List<EObject>) root.eGet(root.eClass().getEStructuralFeature("policies"));

        List<EObject> phasePolicies = policies.stream()
            .filter(p -> {
                EEnumLiteral type = (EEnumLiteral) p.eGet(p.eClass().getEStructuralFeature("type"));
                EEnumLiteral targetAction = (EEnumLiteral) p.eGet(p.eClass().getEStructuralFeature("targetAction"));
                boolean phaseMatch = phase.equals(type.getName());
                boolean actionMatch = "ANY".equals(targetAction.getName()) || 
                                      (req.getActionType() != null && targetAction.getName().equalsIgnoreCase(req.getActionType()));
                return phaseMatch && actionMatch;
            })
            .sorted((p1, p2) -> {
                Integer prio1 = (Integer) p1.eGet(p1.eClass().getEStructuralFeature("priority"));
                Integer prio2 = (Integer) p2.eGet(p2.eClass().getEStructuralFeature("priority"));
                return prio2.compareTo(prio1); // High priority first
            })
            .collect(Collectors.toList());

        for (EObject policy : phasePolicies) {
            String ruleId = (String) policy.eGet(policy.eClass().getEStructuralFeature("policyId"));
            EObject condition = (EObject) policy.eGet(policy.eClass().getEStructuralFeature("condition"));
            EEnumLiteral effect = (EEnumLiteral) policy.eGet(policy.eClass().getEStructuralFeature("effect"));
            String denyReason = (String) policy.eGet(policy.eClass().getEStructuralFeature("denyReason"));

            log.debug("Evaluating Policy: {}", ruleId);
            boolean match = evaluator.evaluateCondition(condition, subject, obj, env, req);

            if (match && "DENY".equals(effect.getName())) {
                log.warn("Policy Engine blocked request: violated rule {}", ruleId);
                return new AuthDecision(false, denyReason != null ? denyReason : ruleId);
            }
            if (!match && "PERMIT".equals(effect.getName())) {
               return new AuthDecision(false, denyReason != null ? denyReason : ruleId);
            }
        }
        
        return new AuthDecision(true, null);
    }

    public void executePostUpdates(Student subject, ClassSection obj, Environment env, UconRequest req) {
        log.info("Executing POST_UPDATE phase...");
        executePostUpdateInternal(subject, obj, env, req, false);
    }

    public void executeAuditLogOnly(Student subject, ClassSection obj, Environment env, UconRequest req) {
        log.info("Executing AuditLog-only POST_UPDATE for DENY outcome...");
        executePostUpdateInternal(subject, obj, env, req, true);
    }

    @SuppressWarnings("unchecked")
    private void executePostUpdateInternal(Student subject, ClassSection obj, Environment env, UconRequest req, boolean auditLogOnly) {
        EObject root = pdp.getPolicyModelRoot();
        if (root == null) return;

        List<EObject> policies = (List<EObject>) root.eGet(root.eClass().getEStructuralFeature("policies"));

        List<EObject> phasePolicies = policies.stream()
            .filter(p -> {
                EEnumLiteral type = (EEnumLiteral) p.eGet(p.eClass().getEStructuralFeature("type"));
                EEnumLiteral targetAction = (EEnumLiteral) p.eGet(p.eClass().getEStructuralFeature("targetAction"));
                boolean phaseMatch = "POST_UPDATE".equals(type.getName());
                boolean actionMatch = "ANY".equals(targetAction.getName()) ||
                                      (req.getActionType() != null && targetAction.getName().equalsIgnoreCase(req.getActionType()));
                return phaseMatch && actionMatch;
            })
            .sorted((p1, p2) -> {
                Integer prio1 = (Integer) p1.eGet(p1.eClass().getEStructuralFeature("priority"));
                Integer prio2 = (Integer) p2.eGet(p2.eClass().getEStructuralFeature("priority"));
                return prio2.compareTo(prio1); // High priority first
            })
            .collect(Collectors.toList());

        for (EObject policy : phasePolicies) {
            String ruleId = (String) policy.eGet(policy.eClass().getEStructuralFeature("policyId"));
            EObject condition = (EObject) policy.eGet(policy.eClass().getEStructuralFeature("condition"));

            boolean match = evaluator.evaluateCondition(condition, subject, obj, env, req);

            if (match) {
                List<EObject> postUpdates = (List<EObject>) policy.eGet(policy.eClass().getEStructuralFeature("postUpdates"));
                if (auditLogOnly) {
                    List<EObject> auditOnly = postUpdates.stream()
                        .filter(s -> "AuditLogStatement".equals(s.eClass().getName()))
                        .collect(Collectors.toList());
                    if (!auditOnly.isEmpty()) {
                        log.debug("Executing AuditLogStatement for Policy: {}", ruleId);
                        evaluator.executePostUpdates(auditOnly, subject, obj, env, req);
                    }
                } else {
                    log.debug("Executing full postUpdates for Policy: {}", ruleId);
                    evaluator.executePostUpdates(postUpdates, subject, obj, env, req);
                }
            }
        }
    }
}
