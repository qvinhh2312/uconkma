package vn.edu.kma.ucon.engine.pdp;

import java.util.List;
import java.util.Map;

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
        Map<String, Object> snapshotBefore,
        Map<String, Object> snapshotAfter,
        List<PhaseTrace> phases) {
}
