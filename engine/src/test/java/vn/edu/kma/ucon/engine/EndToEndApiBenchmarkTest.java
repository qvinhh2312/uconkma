package vn.edu.kma.ucon.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Small end-to-end API benchmark for the public register/drop controller path.
 * This is intentionally lightweight and evidence-oriented, not a load test.
 */
class EndToEndApiBenchmarkTest extends AbstractUconIntegrationTest {

    private static final int WARMUP = 2;
    private static final int SAMPLES = 5;

    @Test
    @DisplayName("Benchmark end-to-end register/drop API latency")
    void benchmarkRegisterAndDropApiLatency() {
        BenchmarkStats registerStats = benchmarkRegister();
        BenchmarkStats dropStats = benchmarkDrop();

        System.out.println("| Endpoint | Avg ms | P95 ms | P99 ms | Notes |");
        System.out.println("|---|---:|---:|---:|---|");
        System.out.printf("| POST /api/register | %.3f | %.3f | %.3f | controller + PEP/PDP + DB + trace |%n",
                registerStats.avgMs(), registerStats.p95Ms(), registerStats.p99Ms());
        System.out.printf("| POST /api/drop | %.3f | %.3f | %.3f | controller + PEP/PDP + DB + trace |%n",
                dropStats.avgMs(), dropStats.p95Ms(), dropStats.p99Ms());
    }

    private BenchmarkStats benchmarkRegister() {
        List<Long> samples = new ArrayList<>();
        for (int i = 0; i < WARMUP + SAMPLES; i++) {
            resetDomainState();
            long start = System.nanoTime();
            assertEquals(200, registrationController.register(registerRequest()).getStatusCode().value());
            long elapsed = System.nanoTime() - start;
            if (i >= WARMUP) {
                samples.add(elapsed);
            }
        }
        return BenchmarkStats.fromNanos(samples);
    }

    private BenchmarkStats benchmarkDrop() {
        List<Long> samples = new ArrayList<>();
        for (int i = 0; i < WARMUP + SAMPLES; i++) {
            resetDomainState();
            assertEquals(200, registrationController.register(registerRequest()).getStatusCode().value());
            long start = System.nanoTime();
            assertEquals(200, registrationController.drop(dropRequest()).getStatusCode().value());
            long elapsed = System.nanoTime() - start;
            if (i >= WARMUP) {
                samples.add(elapsed);
            }
        }
        return BenchmarkStats.fromNanos(samples);
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
