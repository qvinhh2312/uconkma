package vn.edu.kma.ucon.engine.pdp;

public class AuthDecision {
    private boolean permit;
    private String failedCode;

    public AuthDecision(boolean permit, String failedCode) {
        this.permit = permit;
        this.failedCode = failedCode;
    }

    public boolean isPermit() { return permit; }
    public String getFailedCode() { return failedCode; }
}
