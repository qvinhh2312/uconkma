package vn.edu.kma.ucon.engine.pdp;

public class AuthDecision {
    private final boolean permit;
    private final String failedCode;
    private final String failedPolicy;

    public AuthDecision(boolean permit, String failedCode) {
        this(permit, failedCode, null);
    }

    public AuthDecision(boolean permit, String failedCode, String failedPolicy) {
        this.permit = permit;
        this.failedCode = failedCode;
        this.failedPolicy = failedPolicy;
    }

    public boolean isPermit() { return permit; }
    public String getFailedCode() { return failedCode; }
    public String getFailedPolicy() { return failedPolicy; }
}
