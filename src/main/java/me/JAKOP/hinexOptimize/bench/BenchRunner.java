package me.JAKOP.hinexOptimize.bench;

import com.mojang.blaze3d.systems.RenderSystem;
import me.JAKOP.hinexOptimize.HinexOptimize;
import me.JAKOP.hinexOptimize.core.config.HinexConfig;
import me.JAKOP.hinexOptimize.platform.Platform;
import com.mojang.blaze3d.platform.FramerateLimitTracker;
import net.minecraft.SharedConstants;
import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.time.Instant;
import java.util.function.Consumer;

/**
 * One benchmark run, driven from the start of every frame: warm-up, measurement while the
 * camera spins at a constant speed, then an optional resource reload (F3+T) measurement.
 */
public final class BenchRunner {
    /** Unlimited framerate in {@code Options.framerateLimit}. */
    private static final int UNLIMITED_FPS = 260;
    private static final float DEGREES_PER_SECOND = 24f; // one full turn every 15 s
    private static final float PITCH = 20f;
    private static final double RELOAD_TIMEOUT_SECONDS = 180;

    public record Settings(String tag, String scenario, int warmupSeconds, int measureSeconds, boolean measureReload,
                           int renderDistance) {
    }

    private enum Phase { PREPARED, WARMUP, MEASURE, RELOAD, DONE }

    private static BenchRunner current;

    private final Settings settings;
    private final Consumer<Component> feedback;
    private final Consumer<BenchResult> onDone;
    private final StringBuilder notes = new StringBuilder();

    private Phase phase = Phase.PREPARED;
    private long phaseStart;
    private long spinStart;
    private float baseYaw;

    private boolean savedVsync;
    private int savedFramerate;
    private boolean savedPauseOnLostFocus;
    private InactivityFpsLimit savedInactivityLimit;
    private int savedRenderDistance = -1;
    private long throttledFrames;

    private BenchCounters.Snapshot measureStart;
    private long gcCountStart;
    private long gcMillisStart;
    private long[] frames = new long[0];
    private BenchCounters.Snapshot measureCounters = BenchCounters.Snapshot.ZERO;
    private long gcCount;
    private long gcMillis;

    private BenchCounters.Snapshot reloadStart;
    private int reloadsBefore;
    private long reloadMillis = -1;
    private BenchCounters.Snapshot reloadCounters;

    private BenchRunner(Settings settings, Consumer<Component> feedback, Consumer<BenchResult> onDone) {
        this.settings = settings;
        this.feedback = feedback;
        this.onDone = onDone;
    }

    public static boolean isRunning() {
        return current != null;
    }

    /** Applies the benchmark settings (no vsync, unlimited FPS) without starting to measure. */
    public static BenchRunner prepare(Minecraft mc, Settings settings, Consumer<Component> feedback,
                                      Consumer<BenchResult> onDone) {
        if (current != null) {
            throw new IllegalStateException("A benchmark is already running");
        }
        BenchRunner runner = new BenchRunner(settings, feedback, onDone);
        runner.applyEnvironment(mc);
        current = runner;
        return runner;
    }

    public void start(Minecraft mc) {
        if (mc.player == null) {
            abort(mc, "not in a world");
            return;
        }
        long now = System.nanoTime();
        baseYaw = mc.player.getYRot();
        spinStart = now;
        enter(Phase.WARMUP, now);
        say("Benchmark '%s': warming up %d s, then measuring %d s".formatted(settings.tag(), settings.warmupSeconds(), settings.measureSeconds()));
    }

    public void addNote(String note) {
        if (!notes.isEmpty()) {
            notes.append("; ");
        }
        notes.append(note);
    }

    public static void onFrame(Minecraft mc, long now) {
        if (current != null) {
            current.tick(mc, now);
        }
    }

    private void tick(Minecraft mc, long now) {
        if (phase == Phase.PREPARED || phase == Phase.DONE) {
            return;
        }
        if (mc.player == null || mc.level == null) {
            abort(mc, "left the world");
            return;
        }
        double elapsed = (now - phaseStart) / 1e9;
        switch (phase) {
            case WARMUP -> {
                spin(mc, now);
                if (elapsed >= settings.warmupSeconds()) {
                    measureStart = BenchCounters.snapshot();
                    gcCountStart = gcCount();
                    gcMillisStart = gcMillis();
                    FrameRecorder.begin();
                    enter(Phase.MEASURE, now);
                }
            }
            case MEASURE -> {
                spin(mc, now);
                if (mc.getFramerateLimitTracker().getThrottleReason() != FramerateLimitTracker.FramerateThrottleReason.NONE) {
                    throttledFrames++;
                }
                if (elapsed >= settings.measureSeconds()) {
                    if (throttledFrames > 0) {
                        addNote(throttledFrames + " frames throttled by the game (window minimized?)");
                    }
                    frames = FrameRecorder.end();
                    measureCounters = BenchCounters.snapshot().minus(measureStart);
                    gcCount = gcCount() - gcCountStart;
                    gcMillis = gcMillis() - gcMillisStart;
                    if (settings.measureReload()) {
                        say("Measured %d frames, now timing a resource reload".formatted(frames.length));
                        reloadsBefore = LoadTimes.finishedReloads();
                        reloadStart = BenchCounters.snapshot();
                        enter(Phase.RELOAD, now);
                        mc.reloadResourcePacks();
                    } else {
                        finish(mc);
                    }
                }
            }
            case RELOAD -> {
                if (LoadTimes.finishedReloads() > reloadsBefore) {
                    reloadMillis = LoadTimes.lastReloadMillis();
                    reloadCounters = BenchCounters.snapshot().minus(reloadStart);
                    finish(mc);
                } else if (elapsed > RELOAD_TIMEOUT_SECONDS) {
                    addNote("reload timed out");
                    finish(mc);
                }
            }
            default -> {
            }
        }
    }

    private void spin(Minecraft mc, long now) {
        float yaw = baseYaw + DEGREES_PER_SECOND * (float) ((now - spinStart) / 1e9);
        mc.player.setYRot(yaw);
        mc.player.yRotO = yaw;
        mc.player.setXRot(PITCH);
        mc.player.xRotO = PITCH;
    }

    private void enter(Phase next, long now) {
        phase = next;
        phaseStart = now;
    }

    private void finish(Minecraft mc) {
        // Captured before restoring, so the report shows the settings the run used
        BenchResult.Environment environment = environment(mc);
        restoreEnvironment(mc);
        phase = Phase.DONE;
        current = null;
        if (HinexConfig.get().isVanillaMode()) {
            addNote("vanilla mode");
        }

        BenchResult result = new BenchResult(Instant.now(), settings.tag(), settings.scenario(), environment, frames,
            measureCounters, gcCount, gcMillis, reloadMillis, reloadCounters, notes.toString());
        String line = BenchReport.summaryLine(result);
        try {
            Path file = BenchReport.write(mc.gameDirectory.toPath(), result);
            HinexOptimize.LOGGER.info("[HinexBench] RESULT {} file={}", line, file.getFileName());
        } catch (Exception e) {
            HinexOptimize.LOGGER.error("[HinexBench] Could not write the report", e);
        }

        BenchResult.FrameStats s = result.frameStats();
        say("Done: %.1f FPS avg, %.1f 1%% low, %.1f 0.1%% low, max frame %.1f ms".formatted(s.avgFps(), s.low1Fps(), s.low01Fps(), s.maxMs()));
        if (reloadMillis >= 0) {
            say("Resource reload: %d ms".formatted(reloadMillis));
        }
        say("Saved to hinexoptimize/bench/");
        if (onDone != null) {
            onDone.accept(result);
        }
    }

    private void abort(Minecraft mc, String reason) {
        restoreEnvironment(mc);
        phase = Phase.DONE;
        current = null;
        FrameRecorder.end();
        HinexOptimize.LOGGER.warn("[HinexBench] FAILED: {}", reason);
        say("Benchmark aborted: " + reason);
        if (onDone != null) {
            onDone.accept(null);
        }
    }

    private void say(String message) {
        HinexOptimize.LOGGER.info("[HinexBench] {}", message);
        feedback.accept(Component.literal("[HinexBench] " + message));
    }

    private void applyEnvironment(Minecraft mc) {
        savedVsync = mc.options.enableVsync().get();
        savedFramerate = mc.options.framerateLimit().get();
        savedPauseOnLostFocus = mc.options.pauseOnLostFocus;
        savedInactivityLimit = mc.options.inactivityFpsLimit().get();
        mc.options.enableVsync().set(false);
        mc.options.framerateLimit().set(UNLIMITED_FPS);
        mc.options.pauseOnLostFocus = false;
        // The default "AFK" limit caps the game at 30 FPS after a minute without input
        mc.options.inactivityFpsLimit().set(InactivityFpsLimit.MINIMIZED);
        if (settings.renderDistance() > 0) {
            savedRenderDistance = mc.options.renderDistance().get();
            mc.options.renderDistance().set(settings.renderDistance());
        }
    }

    private void restoreEnvironment(Minecraft mc) {
        mc.options.enableVsync().set(savedVsync);
        mc.options.framerateLimit().set(savedFramerate);
        mc.options.pauseOnLostFocus = savedPauseOnLostFocus;
        mc.options.inactivityFpsLimit().set(savedInactivityLimit);
        if (savedRenderDistance > 0) {
            mc.options.renderDistance().set(savedRenderDistance);
        }
    }

    static BenchResult.Environment environment(Minecraft mc) {
        var info = RenderSystem.getDevice().getDeviceInfo();
        return new BenchResult.Environment(
            SharedConstants.getCurrentVersion().name(), Platform.INSTANCE.loaderName(), HinexOptimize.VERSION,
            info.backendName(), info.name(), info.driverInfo(),
            String.join(";", HinexConfig.get().disabledKeys()),
            mc.options.renderDistance().get(), mc.getWindow().getWidth(), mc.getWindow().getHeight()
        );
    }

    private static long gcCount() {
        long total = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            total += Math.max(0, gc.getCollectionCount());
        }
        return total;
    }

    private static long gcMillis() {
        long total = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            total += Math.max(0, gc.getCollectionTime());
        }
        return total;
    }
}
