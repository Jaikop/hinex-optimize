package me.JAKOP.hinexOptimize.bench;

import java.time.Instant;
import java.util.Arrays;

/** Everything one benchmark run measured, plus the environment it ran in. */
public record BenchResult(
    Instant time,
    String tag,
    String scenario,
    Environment environment,
    long[] frameNanos,
    BenchCounters.Snapshot measureCounters,
    long gcCount,
    long gcMillis,
    long reloadMillis,
    BenchCounters.Snapshot reloadCounters,
    String notes
) {
    public record Environment(
        String minecraft, String loader, String modVersion, String backend, String gpu, String driver,
        String disabledOptions, int renderDistance, int width, int height
    ) {
    }

    public int frames() {
        return frameNanos.length;
    }

    public double seconds() {
        return Arrays.stream(frameNanos).sum() / 1e9;
    }

    public FrameStats frameStats() {
        return FrameStats.of(frameNanos);
    }

    /** Frame-time statistics; "1% low" is the FPS of the average of the slowest 1% of frames. */
    public record FrameStats(double avgFps, double low1Fps, double low01Fps, double p50Ms, double p95Ms, double p99Ms,
                             double maxMs, int stutters50Ms) {
        static FrameStats of(long[] frameNanos) {
            if (frameNanos.length == 0) {
                return new FrameStats(0, 0, 0, 0, 0, 0, 0, 0);
            }
            long[] sorted = frameNanos.clone();
            Arrays.sort(sorted);
            long total = Arrays.stream(sorted).sum();
            int stutters = 0;
            for (long frame : sorted) {
                if (frame > 50_000_000L) {
                    stutters++;
                }
            }
            return new FrameStats(
                sorted.length * 1e9 / total,
                worstAverageFps(sorted, 0.01),
                worstAverageFps(sorted, 0.001),
                percentileMs(sorted, 0.50),
                percentileMs(sorted, 0.95),
                percentileMs(sorted, 0.99),
                sorted[sorted.length - 1] / 1e6,
                stutters
            );
        }

        private static double percentileMs(long[] sorted, double p) {
            int index = (int) Math.min(sorted.length - 1, Math.ceil(p * sorted.length) - 1);
            return sorted[Math.max(0, index)] / 1e6;
        }

        private static double worstAverageFps(long[] sorted, double share) {
            int n = (int) Math.max(1, Math.ceil(sorted.length * share));
            long sum = 0;
            for (int i = sorted.length - n; i < sorted.length; i++) {
                sum += sorted[i];
            }
            return n * 1e9 / sum;
        }
    }
}
