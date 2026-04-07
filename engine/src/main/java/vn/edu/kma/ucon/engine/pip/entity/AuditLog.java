package vn.edu.kma.ucon.engine.pip.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String requestId;
    private String studentId;
    private String classId;
    
    // e.g. "ALLOW" or "DENY"
    private String decision;
    
    // Comma-separated rule IDs that failed
    private String failedPolicyCodes;
    
    public AuditLog() {}

    public Long getId() { return id; }
    
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getFailedPolicyCodes() { return failedPolicyCodes; }
    public void setFailedPolicyCodes(String failedPolicyCodes) { this.failedPolicyCodes = failedPolicyCodes; }
}
