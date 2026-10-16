package me.JAKOP.hinexOptimize.render;


import java.util.Arrays;

/**
 * Decides the dynamic render distance from frame times. Plain Java without game classes, so it can be
 * tested; {@link DynamicViewDistance} feeds it once per frame.
 *
 * <p>Frames are judged in windows of {@link #WINDOW_NANOS} by their 90th percentile. Two slow windows
 * in a row lower the distance by one chunk; three fast windows raise it by one. Inside the target band
 * (VSync or a frame limit pins the frame time) it probes upwards every {@link #STEADY_WINDOWS_TO_PROBE}
 * windows. After every step it waits {@link #COOLDOWN_NANOS} and until the mesher is idle. Lowering
 * right after raising means the higher distance is too much, so raising is blocked for a minute.
 */
public final class ViewDistanceController {
    public static final int NO_CAP = Integer.MAX_VALUE;
    static final long SECOND = 1_000_000_000L;
    static final long WINDOW_NANOS = 2 * SECOND;
    static final long COOLDOWN_NANOS = 3 * SECOND;
    static final long BOUNCE_NANOS = 20 * SECOND;
    static final long BLOCK_RAISE_NANOS = 60 * SECOND;
    static final int UNLIMITED_FPS = 260;
    static final double SLOW = 1.15;
    static final double FAST = 0.80;
    static final int SLOW_WINDOWS_TO_LOWER = 2;
    static final int FAST_WINDOWS_TO_RAISE = 3;
    static final int STEADY_WINDOWS_TO_PROBE = 5;

    public record Settings(int targetFps, int minDistance, int maxReduction) {
    }

    /** What the last evaluated window looked like, for logging. */
    public record Decision(int distance, double p90Nanos, double targetNanos) {
    }

    private long[] frames = new long[512];
    private int frameCount;
    private int cap = NO_CAP;
    private int playerDistance = -1;
    private long lastFrame;
    private long windowStart;
    private long cooldownUntil;
    private long blockRaiseUntil;
    private long lastRaise = Long.MIN_VALUE / 2;
    private int slowWindows;
    private int fastWindows;
    private int steadyWindows;

    /** The current cap; {@link #NO_CAP} when the player's setting is used as is. */
    public int cap() {
        return cap;
    }

    public void reset() {
        cap = NO_CAP;
        playerDistance = -1;
        frameCount = 0;
        lastFrame = 0;
        windowStart = 0;
        slowWindows = 0;
        fastWindows = 0;
        steadyWindows = 0;
    }

    /**
     * Records the frame that starts at {@code now}.
     *
     * @param mesherIdle no chunk sections waiting to be built; decisions wait for it after a step
     * @return the new decision when the distance changed, otherwise null
     */
    public Decision onFrame(long now, int setting, int framerateLimit, boolean mesherIdle, Settings settings) {
        if (setting != playerDistance) {
            // The player (or a benchmark) changed the setting: start over from it
            playerDistance = setting;
            cap = NO_CAP;
            frameCount = 0;
            lastFrame = now;
            windowStart = now;
            cooldownUntil = now + COOLDOWN_NANOS;
            return null;
        }
        if (lastFrame != 0) {
            addFrame(now - lastFrame);
        }
        lastFrame = now;
        if (windowStart == 0) {
            windowStart = now;
        }
        if (now - windowStart < WINDOW_NANOS) {
            return null;
        }
        Decision decision = evaluate(now, framerateLimit, mesherIdle, settings);
        frameCount = 0;
        windowStart = now;
        return decision;
    }

    private Decision evaluate(long now, int framerateLimit, boolean mesherIdle, Settings settings) {
        if (frameCount < 10 || now < cooldownUntil || !mesherIdle) {
            return null;
        }
        int target = framerateLimit < UNLIMITED_FPS ? Math.min(settings.targetFps(), framerateLimit) : settings.targetFps();
        double targetNanos = 1e9 / target;
        double p90 = percentile(0.9);
        int current = Math.min(cap, playerDistance);
        int floor = Math.min(playerDistance, Math.max(settings.minDistance(), playerDistance - settings.maxReduction()));

        if (p90 > targetNanos * SLOW) {
            fastWindows = 0;
            steadyWindows = 0;
            if (++slowWindows >= SLOW_WINDOWS_TO_LOWER && current > floor) {
                if (now - lastRaise < BOUNCE_NANOS) {
                    blockRaiseUntil = now + BLOCK_RAISE_NANOS;
                }
                return step(current - 1, now, p90, targetNanos);
            }
        } else if (p90 < targetNanos * FAST) {
            slowWindows = 0;
            steadyWindows = 0;
            if (++fastWindows >= FAST_WINDOWS_TO_RAISE && current < playerDistance && now >= blockRaiseUntil) {
                lastRaise = now;
                return step(current + 1, now, p90, targetNanos);
            }
        } else {
            slowWindows = 0;
            fastWindows = 0;
            if (++steadyWindows >= STEADY_WINDOWS_TO_PROBE && current < playerDistance && now >= blockRaiseUntil) {
                lastRaise = now;
                return step(current + 1, now, p90, targetNanos);
            }
        }
        return null;
    }

    private Decision step(int distance, long now, double p90, double targetNanos) {
        cap = distance >= playerDistance ? NO_CAP : distance;
        slowWindows = 0;
        fastWindows = 0;
        steadyWindows = 0;
        cooldownUntil = now + COOLDOWN_NANOS;
        return new Decision(Math.min(distance, playerDistance), p90, targetNanos);
    }

    private void addFrame(long nanos) {
        if (frameCount == frames.length) {
            frames = Arrays.copyOf(frames, frames.length * 2);
        }
        frames[frameCount++] = nanos;
    }

    private double percentile(double p) {
        long[] sorted = Arrays.copyOf(frames, frameCount);
        Arrays.sort(sorted);
        int index = (int) Math.min(sorted.length - 1, Math.ceil(p * sorted.length) - 1);
        return sorted[Math.max(0, index)];
    }
}
