package me.JAKOP.hinexOptimize.bench;

import net.minecraft.client.Minecraft;

/** Entry points called from the {@code mixin.bench} mixins. */
public final class BenchHooks {
    private BenchHooks() {
    }

    /** Start of {@code Minecraft.runTick}, once per rendered frame. */
    public static void onFrameStart(Minecraft mc) {
        long now = System.nanoTime();
        FrameRecorder.onFrameStart(now);
        BenchRunner.onFrame(mc, now);
        AutoBench.onFrame(mc, now);
    }

    /** End of a backend pipeline compile; counts it as lazy if it stalled the render thread in a world. */
    public static void onPipelineCompiled(long elapsedNanos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.level != null && mc.isSameThread() && !LoadTimes.isReloading()) {
            BenchCounters.LAZY_PIPELINE.add(elapsedNanos);
        }
    }
}
