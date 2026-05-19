package vn.edu.kma.ucon.engine.pep;

public class ApiDecisionResponse {
    private final String requestId;
    private final String action;
    private final String decision;
    private final String phase;
    private final String studentId;
    private final String classId;
    private final String failedPolicy;
    private final String denyReason;
    private final String explanation;
    private final String message;

    public ApiDecisionResponse(String requestId,
                               String action,
                               String decision,
                               String phase,
                               String studentId,
                               String classId,
                               String failedPolicy,
                               String denyReason,
                               String explanation,
                               String message) {
        this.requestId = requestId;
        this.action = action;
        this.decision = decision;
        this.phase = phase;
        this.studentId = studentId;
        this.classId = classId;
        this.failedPolicy = failedPolicy;
        this.denyReason = denyReason;
        this.explanation = explanation;
        this.message = message;
    }

    public String getRequestId() { return requestId; }
    public String getAction() { return action; }
    public String getDecision() { return decision; }
    public String getPhase() { return phase; }
    public String getStudentId() { return studentId; }
    public String getClassId() { return classId; }
    public String getFailedPolicy() { return failedPolicy; }
    public String getDenyReason() { return denyReason; }
    public String getExplanation() { return explanation; }
    public String getMessage() { return message; }
}
