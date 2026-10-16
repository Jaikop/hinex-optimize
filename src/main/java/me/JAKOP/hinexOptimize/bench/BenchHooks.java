package me.JAKOP.hinexOptimize.bench;

import net.minecraft.client.Minecraft;

public final class BenchHooks {
    private BenchHooks() {
    }

    public static void onFrameStart(Minecraft mc) {
        long now = System.nanoTime();
        FrameRecorder.onFrameStart(now);
        BenchRunner.onFrame(mc, now);
        AutoBench.onFrame(mc, now);
    }

    public static void onPipelineCompiled(long elapsedNanos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.level != null && mc.isSameThread() && !LoadTimes.isReloading()) {
            BenchCounters.LAZY_PIPELINE.add(elapsedNanos);
        }
    }
}
