package vn.edu.kma.ucon.engine.pdp;

import java.util.List;

public record PolicyAnalysisReport(int policyCount, int errors, List<PolicyAnalysisWarning> warnings) {
}
