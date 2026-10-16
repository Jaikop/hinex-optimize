package me.JAKOP.hinexOptimize.bench;

import java.util.concurrent.atomic.LongAdder;

/** Counters fed by the {@code mixin.bench} hooks. Read through {@link #snapshot()} and diff. */
public final class BenchCounters {
    // Render-pass calls; only touched from the render thread
    public static long drawCalls;
    public static long drawItems;
    public static long pipelineBinds;
    public static long uniformBinds;
    public static long descriptorUpdates;
    public static long bufferViews;
    /** Chunk sections handed to the mesher (async or sync); render thread. */
    public static long sectionCompiles;

    // Compile timings; shaders and pipelines compile on worker threads too
    public static final Timer GLSL = new Timer();
    public static final Timer PIPELINE = new Timer();
    /** Pipelines compiled on the render thread while in a world, outside of a resource reload: stutters. */
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

    /** Sums the duration of a method across threads; begin/end pair up per thread. */
    public static final class Timer {
        private final LongAdder count = new LongAdder();
        private final LongAdder nanos = new LongAdder();
        private final ThreadLocal<long[]> start = ThreadLocal.withInitial(() -> new long[1]);

        public void begin() {
            start.get()[0] = System.nanoTime();
        }

        /** @return the measured duration in nanoseconds */
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
