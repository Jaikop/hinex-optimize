package me.JAKOP.hinexOptimize.bench;

import it.unimi.dsi.fastutil.longs.LongArrayList;

public final class FrameRecorder {
    private static final long SECOND = 1_000_000_000L;

    private static long lastFrameStart;
    private static LongArrayList recording;

    private static long windowStart;
    private static long windowFrames;
    private static BenchCounters.Snapshot windowCounters = BenchCounters.Snapshot.ZERO;
    private static volatile LiveStats lastSecond;

    private FrameRecorder() {
    }

    public static void onFrameStart(long now) {
        if (recording != null && lastFrameStart != 0) {
            recording.add(now - lastFrameStart);
        }
        lastFrameStart = now;

        if (windowStart == 0) {
            windowStart = now;
            windowCounters = BenchCounters.snapshot();
        } else if (now - windowStart >= SECOND) {
            BenchCounters.Snapshot counters = BenchCounters.snapshot();
            lastSecond = new LiveStats(windowFrames * (double) SECOND / (now - windowStart), windowFrames, counters.minus(windowCounters));
            windowStart = now;
            windowFrames = 0;
            windowCounters = counters;
        }
        windowFrames++;
    }

    public static void begin() {
        recording = new LongArrayList(16384);
    }

    public static long[] end() {
        long[] frames = recording == null ? new long[0] : recording.toLongArray();
        recording = null;
        return frames;
    }

    public static LiveStats lastSecond() {
        return lastSecond;
    }

    public record LiveStats(double fps, long frames, BenchCounters.Snapshot counters) {
    }
}
