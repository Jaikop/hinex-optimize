package me.JAKOP.hinexOptimize.bench;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * Client command {@code /hinexbench}, shared by both loaders.
 *
 * <ul>
 *   <li>{@code run [seconds]} — spin the camera where you stand and measure frame times, then a resource reload</li>
 *   <li>{@code rdswitch} — change render distance 16 / 8 / 12 and time how long chunks take to settle</li>
 *   <li>{@code reload} — time a resource reload (F3+T) only</li>
 *   <li>{@code stats} — counters for the last second and compile totals</li>
 * </ul>
 */
public final class BenchCommand {
    private static final int DEFAULT_SECONDS = 30;

    private BenchCommand() {
    }

    public static <S> LiteralArgumentBuilder<S> create(BiConsumer<S, Component> feedback) {
        return LiteralArgumentBuilder.<S>literal("hinexbench")
            .executes(c -> help(c.getSource(), feedback))
            .then(LiteralArgumentBuilder.<S>literal("run")
                .executes(c -> run(c.getSource(), feedback, "spin", DEFAULT_SECONDS))
                .then(RequiredArgumentBuilder.<S, Integer>argument("seconds", IntegerArgumentType.integer(5, 600))
                    .executes(c -> run(c.getSource(), feedback, "spin", IntegerArgumentType.getInteger(c, "seconds")))))
            .then(LiteralArgumentBuilder.<S>literal("rdswitch")
                .executes(c -> run(c.getSource(), feedback, "rdswitch", 0)))
            .then(LiteralArgumentBuilder.<S>literal("reload")
                .executes(c -> reload(c.getSource(), feedback)))
            .then(LiteralArgumentBuilder.<S>literal("stats")
                .executes(c -> stats(c.getSource(), feedback)));
    }

    private static <S> int help(S source, BiConsumer<S, Component> feedback) {
        say(source, feedback, "/hinexbench run [seconds] - measure FPS while the camera spins, then a resource reload");
        say(source, feedback, "/hinexbench rdswitch - switch render distance 16 / 8 / 12 and time the chunk rebuild");
        say(source, feedback, "/hinexbench reload - time a resource reload (F3+T)");
        say(source, feedback, "/hinexbench stats - per-frame counters for the last second");
        return 1;
    }

    private static <S> int run(S source, BiConsumer<S, Component> feedback, String scenario, int seconds) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            say(source, feedback, "Join a world first");
            return 0;
        }
        if (BenchRunner.isRunning()) {
            say(source, feedback, "A benchmark is already running");
            return 0;
        }
        BenchRunner.Settings settings = new BenchRunner.Settings("manual", scenario, 5, seconds, scenario.equals("spin"), 0);
        BenchRunner.prepare(mc, settings, message -> feedback.accept(source, message), null).start(mc);
        return 1;
    }

    private static <S> int reload(S source, BiConsumer<S, Component> feedback) {
        Minecraft mc = Minecraft.getInstance();
        BenchCounters.Snapshot before = BenchCounters.snapshot();
        long start = System.nanoTime();
        mc.reloadResourcePacks().whenComplete((result, error) -> mc.execute(() -> {
            BenchCounters.Snapshot spent = BenchCounters.snapshot().minus(before);
            say(source, feedback, String.format(Locale.ROOT, "Reload: %d ms, %d shaders in %.0f ms CPU, %d pipelines in %.0f ms CPU%s",
                (System.nanoTime() - start) / 1_000_000, spent.glslCount(), spent.glslNanos() / 1e6,
                spent.pipelineCount(), spent.pipelineNanos() / 1e6, error != null ? " (failed)" : ""));
        }));
        return 1;
    }

    private static <S> int stats(S source, BiConsumer<S, Component> feedback) {
        FrameRecorder.LiveStats live = FrameRecorder.lastSecond();
        if (live != null && live.frames() > 0) {
            BenchCounters.Snapshot c = live.counters();
            double frames = live.frames();
            say(source, feedback, String.format(Locale.ROOT,
                "%.0f FPS | per frame: %.0f draw calls (%.0f draws), %.0f pipeline binds, %.0f uniform binds, %.0f descriptor updates, %.1f buffer views",
                live.fps(), c.drawCalls() / frames, c.drawItems() / frames, c.pipelineBinds() / frames, c.uniformBinds() / frames,
                c.descriptorUpdates() / frames, c.bufferViews() / frames));
        }
        BenchCounters.Snapshot total = BenchCounters.snapshot();
        say(source, feedback, String.format(Locale.ROOT,
            "Since start: %d shaders (%.0f ms CPU), %d pipelines (%.0f ms CPU), %d lazy pipeline compiles (%.0f ms)",
            total.glslCount(), total.glslNanos() / 1e6, total.pipelineCount(), total.pipelineNanos() / 1e6,
            total.lazyCount(), total.lazyNanos() / 1e6));
        say(source, feedback, "Startup: " + LoadTimes.startupMillis() + " ms, last reload: " + LoadTimes.lastReloadMillis() + " ms");
        return 1;
    }

    private static <S> void say(S source, BiConsumer<S, Component> feedback, String message) {
        feedback.accept(source, Component.literal("[HinexBench] " + message));
    }
}
