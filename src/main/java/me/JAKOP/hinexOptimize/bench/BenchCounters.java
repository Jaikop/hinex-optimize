package me.JAKOP.hinexOptimize.bench;

import java.util.concurrent.atomic.LongAdder;

public final class BenchCounters {
    public static long drawCalls;
    public static long drawItems;
    public static long pipelineBinds;
    public static long uniformBinds;
    public static long descriptorUpdates;
    public static long bufferViews;
    public static long sectionCompiles;

    public static final Timer GLSL = new Timer();
    public static final Timer PIPELINE = new Timer();
    public static final Timer LAZY_PIPELINE = new Timer();

    private BenchCounters() {
    }

    public static Snapshot snapshot() {
        return new Snapshot(
            drawCalls, drawItems, pipelineBinds, uniformBinds, descriptorUpdates, bufferViews, sectionCompiles,
            GLSL.count(), GLSL.nanos(), PIPELINE.count(), PIPELINE.nanos(), LAZY_PIPELINE.count(), LAZY_PIPELINE.nanos()
        );
    }

    public record Snapshot(
        long drawCalls, long drawItems, long pipelineBinds, long uniformBinds, long descriptorUpdates, long bufferViews,
        long sectionCompiles,
        long glslCount, long glslNanos, long pipelineCount, long pipelineNanos, long lazyCount, long lazyNanos
    ) {
        public static final Snapshot ZERO = new Snapshot(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        public Snapshot minus(Snapshot o) {
            return new Snapshot(
                drawCalls - o.drawCalls, drawItems - o.drawItems, pipelineBinds - o.pipelineBinds,
                uniformBinds - o.uniformBinds, descriptorUpdates - o.descriptorUpdates, bufferViews - o.bufferViews,
                sectionCompiles - o.sectionCompiles,
                glslCount - o.glslCount, glslNanos - o.glslNanos, pipelineCount - o.pipelineCount,
                pipelineNanos - o.pipelineNanos, lazyCount - o.lazyCount, lazyNanos - o.lazyNanos
            );
        }
    }

    public static final class Timer {
        private final LongAdder count = new LongAdder();
        private final LongAdder nanos = new LongAdder();
        private final ThreadLocal<long[]> start = ThreadLocal.withInitial(() -> new long[1]);

        public void begin() {
            start.get()[0] = System.nanoTime();
        }

        public long end() {
            long elapsed = System.nanoTime() - start.get()[0];
            add(elapsed);
            return elapsed;
        }

        public void add(long elapsedNanos) {
            count.increment();
            nanos.add(elapsedNanos);
        }

        public long count() {
            return count.sum();
        }

        public long nanos() {
            return nanos.sum();
        }
    }
}
