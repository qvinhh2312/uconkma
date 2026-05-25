package vn.edu.kma.ucon.engine.pdp;

import java.util.List;

/**
 * End-to-end explainability payload for a single UCON request evaluation.
 */
public record DecisionTrace(
        String requestId,
        String action,
        String decision,
        String studentId,
        String classId,
        String sessionId,
        String sessionStatus,
        List<PhaseTrace> phases) {
}
