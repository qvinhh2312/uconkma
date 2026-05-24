package vn.edu.kma.ucon.engine.pep;

public class UconRequest {
    private String requestId;
    private String studentId;
    private String classId;
    private String actionType;
    private String decision;
    private String failedPolicyCodes;
    private Boolean confirmedRegistrationRule;
    private Boolean adminOverride;
    private String overrideReason;

    public UconRequest() {}

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getFailedPolicyCodes() { return failedPolicyCodes; }
    public void setFailedPolicyCodes(String failedPolicyCodes) { this.failedPolicyCodes = failedPolicyCodes; }

    public Boolean getConfirmedRegistrationRule() { return confirmedRegistrationRule; }
    public void setConfirmedRegistrationRule(Boolean confirmedRegistrationRule) { this.confirmedRegistrationRule = confirmedRegistrationRule; }

    public Boolean getAdminOverride() { return adminOverride; }
    public void setAdminOverride(Boolean adminOverride) { this.adminOverride = adminOverride; }

    public String getOverrideReason() { return overrideReason; }
    public void setOverrideReason(String overrideReason) { this.overrideReason = overrideReason; }
}
