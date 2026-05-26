package vn.edu.kma.ucon.engine.pdp;

public record PolicyTraceEntry(
        String policyId,
        String predicate,
        String effect,
        String source,
        String version,
        String uconVariant,
        String policyStatus,
        boolean matched,
        boolean blocked,
        String denyReason) {
}
