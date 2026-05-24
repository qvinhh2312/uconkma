package vn.edu.kma.ucon.engine.update;

import java.util.List;

import vn.edu.kma.ucon.engine.pdp.Phase;

public record UpdatePlan(
        Phase phase,
        String section,
        List<PlannedPolicyUpdate> plannedPolicies) {

    public static UpdatePlan empty(Phase phase, String section) {
        return new UpdatePlan(phase, section, List.of());
    }
}
