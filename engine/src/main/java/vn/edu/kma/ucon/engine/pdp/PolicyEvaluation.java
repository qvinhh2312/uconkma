package vn.edu.kma.ucon.engine.pdp;

public record PolicyEvaluation(
        String policyId,
        String predicate,
        String effect,
        boolean matched,
        String denyReason) {
}
