package vn.edu.kma.ucon.engine.pdp;

import java.util.List;

/**
 * Detailed result for one phase/predicate slice of a UCON decision.
 */
public record PhaseTrace(
        String phase,
        String predicate,
        String action,
        String decision,
        String failedPolicy,
        String failedReason,
        List<PolicyTraceEntry> policies,
        List<String> updatesApplied,
        List<String> rollbackApplied) {
}
