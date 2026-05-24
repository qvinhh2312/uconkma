package vn.edu.kma.ucon.engine.pdp;

import java.util.List;

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
