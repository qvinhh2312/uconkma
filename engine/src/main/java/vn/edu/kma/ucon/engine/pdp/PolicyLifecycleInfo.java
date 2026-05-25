package vn.edu.kma.ucon.engine.pdp;

public record PolicyLifecycleInfo(
        String policyId,
        String status,
        String source,
        String version,
        String uconVariant) {
}
