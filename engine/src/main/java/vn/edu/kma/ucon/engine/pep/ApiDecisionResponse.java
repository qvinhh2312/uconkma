package vn.edu.kma.ucon.engine.pep;

import vn.edu.kma.ucon.engine.pdp.DecisionTrace;

/**
 * Stable API response for policy decisions. Policy deny responses and successful
 * commits both use this payload so clients can inspect the UCON phase,
 * predicate, failed policy, session status and full decision trace consistently.
 */
public class ApiDecisionResponse {
    private final String requestId;
    private final String action;
    private final String decision;
    private final String phase;
    private final String predicate;
    private final String studentId;
    private final String classId;
    private final String failedPolicy;
    private final String denyReason;
    private final String sessionStatus;
    private final String explanation;
    private final String message;
    private final DecisionTrace decisionTrace;

    public ApiDecisionResponse(String requestId,
                               String action,
                               String decision,
                               String phase,
                               String studentId,
                               String classId,
                               String failedPolicy,
                               String denyReason,
                               String explanation,
                               String message,
                               DecisionTrace decisionTrace) {
        this(requestId,
                action,
                decision,
                phase,
                null,
                studentId,
                classId,
                failedPolicy,
                denyReason,
                null,
                explanation,
                message,
                decisionTrace);
    }

    public ApiDecisionResponse(String requestId,
                               String action,
                               String decision,
                               String phase,
                               String predicate,
                               String studentId,
                               String classId,
                               String failedPolicy,
                               String denyReason,
                               String sessionStatus,
                               String explanation,
                               String message,
                               DecisionTrace decisionTrace) {
        this.requestId = requestId;
        this.action = action;
        this.decision = decision;
        this.phase = phase;
        this.predicate = predicate;
        this.studentId = studentId;
        this.classId = classId;
        this.failedPolicy = failedPolicy;
        this.denyReason = denyReason;
        this.sessionStatus = sessionStatus;
        this.explanation = explanation;
        this.message = message;
        this.decisionTrace = decisionTrace;
    }

    public String getRequestId() { return requestId; }
    public String getAction() { return action; }
    public String getDecision() { return decision; }
    public String getPhase() { return phase; }
    public String getPredicate() { return predicate; }
    public String getStudentId() { return studentId; }
    public String getClassId() { return classId; }
    public String getFailedPolicy() { return failedPolicy; }
    public String getDenyReason() { return denyReason; }
    public String getSessionStatus() { return sessionStatus; }
    public String getExplanation() { return explanation; }
    public String getMessage() { return message; }
    public DecisionTrace getDecisionTrace() { return decisionTrace; }
}
