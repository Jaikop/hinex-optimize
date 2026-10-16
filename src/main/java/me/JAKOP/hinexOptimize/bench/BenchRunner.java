package me.JAKOP.hinexOptimize.bench;

import com.mojang.blaze3d.systems.RenderSystem;
import me.JAKOP.hinexOptimize.HinexOptimize;
import me.JAKOP.hinexOptimize.core.config.HinexConfig;
import me.JAKOP.hinexOptimize.platform.Platform;
import com.mojang.blaze3d.platform.FramerateLimitTracker;
import net.minecraft.SharedConstants;
import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public final class BenchRunner {
    private static final int UNLIMITED_FPS = 260;
    private static final float DEGREES_PER_SECOND = 24f;
    private static final float PITCH = 20f;
    private static final double RELOAD_TIMEOUT_SECONDS = 180;

    private static final int[] RD_STEPS = {16, 8, 12};
    private static final long SECOND = 1_000_000_000L;
    private static final long SETTLE_QUIET_NANOS = 3 * SECOND / 2;
    private static final long STEP_TIMEOUT_NANOS = 90 * SECOND;
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());

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

    private int step = -1;
    private long stepStart;
    private long stepCompilesStart;
    private long lastCompileCount;
    private long lastCompileChange;
    private long settleMillis;
    private long maxSettleMillis;
    private long blankFrames;
    private final StringBuilder stepNotes = new StringBuilder();

    private BenchRunner(Settings settings, Consumer<Component> feedback, Consumer<BenchResult> onDone) {
        this.settings = settings;
        this.feedback = feedback;
        this.onDone = onDone;
    }

    public static boolean isRunning() {
        return current != null;
    }

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
        if (isRdSwitch()) {
            say("Benchmark '%s': warming up %d s, then switching render distance %d -> 16 -> 8 -> 12"
                .formatted(settings.tag(), settings.warmupSeconds(), mc.options.renderDistance().get()));
        } else {
            say("Benchmark '%s': warming up %d s, then measuring %d s".formatted(settings.tag(), settings.warmupSeconds(), settings.measureSeconds()));
        }
    }

    private boolean isRdSwitch() {
        return "rdswitch".equals(settings.scenario());
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
                aim(mc, now);
                if (elapsed >= settings.warmupSeconds()) {
                    measureStart = BenchCounters.snapshot();
                    gcCountStart = gcCount();
                    gcMillisStart = gcMillis();
                    FrameRecorder.begin();
                    enter(Phase.MEASURE, now);
                }
            }
            case MEASURE -> {
                aim(mc, now);
                if (mc.getFramerateLimitTracker().getThrottleReason() != FramerateLimitTracker.FramerateThrottleReason.NONE) {
                    throttledFrames++;
                }
                boolean measured = isRdSwitch() ? tickRdSwitch(mc, now) : elapsed >= settings.measureSeconds();
                if (measured) {
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

    private boolean tickRdSwitch(Minecraft mc, long now) {
        long compiles = BenchCounters.sectionCompiles;
        if (step >= 0 && mc.levelRenderer.visibleSections().isEmpty()) {
            blankFrames++;
        }
        if (step < 0) {
            startStep(mc, 0, now, compiles);
            return false;
        }
        if (compiles != lastCompileCount) {
            lastCompileCount = compiles;
            lastCompileChange = now;
        }
        boolean quiet = now - lastCompileChange >= SETTLE_QUIET_NANOS && mc.levelRenderer.hasRenderedAllSections();
        boolean timedOut = now - stepStart > STEP_TIMEOUT_NANOS;
        if (now - stepStart < SECOND || !(quiet || timedOut)) {
            return false;
        }

        long settle = Math.max(0, lastCompileChange - stepStart) / 1_000_000;
        settleMillis += settle;
        maxSettleMillis = Math.max(maxSettleMillis, settle);
        if (!stepNotes.isEmpty()) {
            stepNotes.append(' ');
        }
        stepNotes.append(RD_STEPS[step]).append(":").append(settle).append("ms/").append(compiles - stepCompilesStart).append("c");
        if (timedOut) {
            stepNotes.append("(timeout)");
        }
        if (step + 1 < RD_STEPS.length) {
            startStep(mc, step + 1, now, compiles);
            return false;
        }
        addNote("steps " + stepNotes);
        addNote("blank frames " + blankFrames);
        takeScreenshot(mc);
        return true;
    }

    private void startStep(Minecraft mc, int index, long now, long compiles) {
        step = index;
        stepStart = now;
        stepCompilesStart = compiles;
        lastCompileCount = compiles;
        lastCompileChange = now;
        mc.options.renderDistance().set(RD_STEPS[index]);
    }

    private void takeScreenshot(Minecraft mc) {
        String name = "hinexbench_" + settings.tag() + "_" + FILE_STAMP.format(Instant.now()) + ".png";
        try {
            Screenshot.grab(mc.gameDirectory, name, mc.gameRenderer.mainRenderTarget(), 1, message -> {
            });
            addNote("screenshot " + name);
        } catch (Exception e) {
            HinexOptimize.LOGGER.warn("[HinexBench] Screenshot failed", e);
        }
    }

    private void aim(Minecraft mc, long now) {
        if (isRdSwitch()) {
            hold(mc);
        } else {
            spin(mc, now);
        }
    }

    private void hold(Minecraft mc) {
        mc.player.setYRot(baseYaw);
        mc.player.yRotO = baseYaw;
        mc.player.setXRot(PITCH);
        mc.player.xRotO = PITCH;
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
        BenchResult.Environment environment = environment(mc);
        int effectiveDistance = mc.options.getEffectiveRenderDistance();
        if (effectiveDistance != environment.renderDistance()) {
            addNote("rendered distance " + effectiveDistance);
        }
        restoreEnvironment(mc);
        phase = Phase.DONE;
        current = null;
        if (HinexConfig.get().isVanillaMode()) {
            addNote("vanilla mode");
        }

        BenchResult result = new BenchResult(Instant.now(), settings.tag(), settings.scenario(), environment, frames,
            measureCounters, gcCount, gcMillis, reloadMillis, reloadCounters, settleMillis, maxSettleMillis, notes.toString());
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
        mc.options.inactivityFpsLimit().set(InactivityFpsLimit.MINIMIZED);
        savedRenderDistance = mc.options.renderDistance().get();
        if (settings.renderDistance() > 0) {
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
