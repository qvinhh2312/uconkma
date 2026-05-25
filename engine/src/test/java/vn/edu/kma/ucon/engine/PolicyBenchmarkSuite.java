package vn.edu.kma.ucon.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import vn.edu.kma.ucon.engine.pdp.PolicyAdministrationPoint;
import vn.edu.kma.ucon.engine.pdp.PolicyAnalyzer;
import vn.edu.kma.ucon.engine.pdp.PolicyDecisionPoint;
import vn.edu.kma.ucon.engine.pdp.PolicyValidator;

@SpringBootTest
class PolicyBenchmarkSuite {

    @Autowired
    PolicyDecisionPoint policyDecisionPoint;
    @Autowired
    PolicyValidator policyValidator;
    @Autowired
    PolicyAnalyzer policyAnalyzer;
    @Autowired
    PolicyAdministrationPoint policyAdministrationPoint;

    @Test
    void benchmarkPolicyModelPipeline() {
        List<Integer> sizes = List.of(25, 50, 100, 500);
        System.out.println("| Policy count | Avg ms | P95 ms | P99 ms | Notes |");
        System.out.println("|---|---:|---:|---:|---|");
        for (int size : sizes) {
            BenchmarkStats stats = runForSize(size);
            System.out.printf("| %d | %.3f | %.3f | %.3f | validate + analyze + PAP filter |%n",
                    size, stats.avgMs(), stats.p95Ms(), stats.p99Ms());
        }
    }

    private BenchmarkStats runForSize(int targetPolicyCount) {
        EObject duplicatedRoot = duplicatePolicies(policyDecisionPoint.getAuthoringPolicyModelRoot(), targetPolicyCount);
        List<Long> samplesNanos = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            executePipeline(duplicatedRoot);
        }
        for (int i = 0; i < 10; i++) {
            long start = System.nanoTime();
            executePipeline(duplicatedRoot);
            long end = System.nanoTime();
            samplesNanos.add(end - start);
        }

        return BenchmarkStats.fromNanos(samplesNanos);
    }

    @SuppressWarnings("unchecked")
    private EObject duplicatePolicies(EObject originalRoot, int targetPolicyCount) {
        EObject rootCopy = EcoreUtil.copy(originalRoot);
        List<EObject> policies = (List<EObject>) rootCopy.eGet(rootCopy.eClass().getEStructuralFeature("policies"));
        List<EObject> policySets = (List<EObject>) rootCopy.eGet(rootCopy.eClass().getEStructuralFeature("policySets"));
        List<EObject> basePolicies = new ArrayList<>(policies);

        int copyIndex = 1;
        while (policies.size() < targetPolicyCount) {
            for (EObject basePolicy : basePolicies) {
                if (policies.size() >= targetPolicyCount) {
                    break;
                }
                EObject duplicate = EcoreUtil.copy(basePolicy);
                String originalId = stringValue(duplicate, "policyId");
                int originalPriority = intValue(duplicate, "priority");
                duplicate.eSet(duplicate.eClass().getEStructuralFeature("policyId"), originalId + "_BM" + copyIndex);
                duplicate.eSet(duplicate.eClass().getEStructuralFeature("priority"), originalPriority + (copyIndex * 1000));
                policies.add(duplicate);
                copyIndex++;
            }
        }

        if (!policySets.isEmpty()) {
            EObject policySet = policySets.get(0);
            List<String> policyIds = (List<String>) policySet.eGet(policySet.eClass().getEStructuralFeature("policyIds"));
            policyIds.clear();
            for (EObject policy : policies) {
                policyIds.add(stringValue(policy, "policyId"));
            }
        }
        return rootCopy;
    }

    private void executePipeline(EObject root) {
        EObject workingCopy = EcoreUtil.copy(root);
        policyValidator.validate(workingCopy);
        policyAnalyzer.analyze(workingCopy);
        policyAdministrationPoint.activateValidatedPolicies(workingCopy);
    }

    private String stringValue(EObject obj, String featureName) {
        Object value = obj.eGet(obj.eClass().getEStructuralFeature(featureName));
        return value == null ? null : value.toString();
    }

    private int intValue(EObject obj, String featureName) {
        Object value = obj.eGet(obj.eClass().getEStructuralFeature(featureName));
        return value == null ? 0 : ((Number) value).intValue();
    }

    private record BenchmarkStats(double avgMs, double p95Ms, double p99Ms) {
        private static BenchmarkStats fromNanos(List<Long> samples) {
            List<Long> sorted = samples.stream().sorted(Comparator.naturalOrder()).toList();
            double avg = sorted.stream().mapToLong(Long::longValue).average().orElse(0.0) / 1_000_000.0;
            double p95 = percentile(sorted, 0.95) / 1_000_000.0;
            double p99 = percentile(sorted, 0.99) / 1_000_000.0;
            return new BenchmarkStats(avg, p95, p99);
        }

        private static long percentile(List<Long> sorted, double percentile) {
            if (sorted.isEmpty()) {
                return 0L;
            }
            int index = (int) Math.ceil(percentile * sorted.size()) - 1;
            index = Math.max(0, Math.min(index, sorted.size() - 1));
            return sorted.get(index);
        }
    }
}
