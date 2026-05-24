package vn.edu.kma.ucon.engine.pdp;

public record PolicyTraceEntry(
        String policyId,
        String predicate,
        String effect,
        boolean matched,
        boolean blocked,
        String denyReason) {
}
