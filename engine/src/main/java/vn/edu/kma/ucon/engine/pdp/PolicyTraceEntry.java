package vn.edu.kma.ucon.engine.pdp;

public record PolicyTraceEntry(
        String policyId,
        String predicate,
        String phase,
        String updateTiming,
        String effect,
        String source,
        String version,
        String uconVariant,
        String policyStatus,
        boolean conditionResult,
        boolean matched,
        boolean blocked,
        String denyReason) {
}
