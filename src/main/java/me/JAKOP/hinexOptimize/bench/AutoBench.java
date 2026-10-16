package me.JAKOP.hinexOptimize.bench;

import me.JAKOP.hinexOptimize.HinexOptimize;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.util.List;

/**
 * Unattended benchmark for comparing builds: {@code -Dhinexoptimize.autobench=spin} opens a
 * fixed-seed world (created on the first run, reused afterwards), freezes time, weather and
 * mob spawning, waits until every section is rendered, runs {@link BenchRunner} and quits.
 *
 * <p>Other properties ({@code hinexoptimize.autobench.<name>}): {@code tag}, {@code seconds},
 * {@code warmup}, {@code renderDistance}, {@code reload} and {@code exit}.
 */
public final class AutoBench {
    private static final String PREFIX = "hinexoptimize.autobench";
    private static final long SEED = 20261015L;
    private static final long SECOND = 1_000_000_000L;
    private static final List<String> SETUP_COMMANDS = List.of(
        "gamerule advance_time false",
        "gamerule advance_weather false",
        "gamerule spawn_mobs false",
        "time set 6000",
        "weather clear",
        "gamemode spectator @a",
        "tp @a 0 140 0 0 20"
    );

    private enum Step { OFF, WAIT_TITLE, WAIT_WORLD, WAIT_CHUNKS, RUNNING, EXIT }

    private static Step step = System.getProperty(PREFIX) != null ? Step.WAIT_TITLE : Step.OFF;
    private static long stepStart;
    private static long playerSince;
    private static long chunksReadySince;
    private static BenchRunner runner;

    private AutoBench() {
    }

    public static boolean isEnabled() {
        return step != Step.OFF;
    }

    public static void onFrame(Minecraft mc, long now) {
        if (step == Step.OFF) {
            return;
        }
        if (stepStart == 0) {
            stepStart = now;
            // An unfocused window would otherwise pause the integrated server
            mc.options.pauseOnLostFocus = false;
        }
        long elapsed = now - stepStart;

        switch (step) {
            case WAIT_TITLE -> {
                if (!LoadTimes.isGameLoaded() || elapsed < 2 * SECOND) {
                    return;
                }
                if (mc.gui.screen() instanceof TitleScreen title) {
                    openWorld(mc, title);
                    next(Step.WAIT_WORLD, now);
                } else if (mc.level == null) {
                    // First launches show onboarding/notice screens before the title screen
                    HinexOptimize.LOGGER.info("[HinexBench] Skipping start-up screen {}",
                        mc.gui.screen() == null ? "none" : mc.gui.screen().getClass().getName());
                    mc.gui.setScreen(new TitleScreen());
                }
            }
            case WAIT_WORLD -> {
                if (mc.player != null && mc.level != null) {
                    if (playerSince == 0) {
                        playerSince = now;
                    }
                    // Loading screens close by themselves; anything still open after 5 s is in the way
                    if (mc.gui.screen() != null && now - playerSince > 5 * SECOND) {
                        mc.gui.setScreen(null);
                    }
                    if (mc.gui.screen() == null) {
                        setUpWorld(mc);
                        next(Step.WAIT_CHUNKS, now);
                    }
                } else if (elapsed > 300 * SECOND) {
                    fail(mc, "world did not load in 300 s");
                }
            }
            case WAIT_CHUNKS -> {
                if (elapsed < 10 * SECOND) {
                    return;
                }
                if (mc.levelRenderer.hasRenderedAllSections()) {
                    if (chunksReadySince == 0) {
                        chunksReadySince = now;
                    }
                    if (now - chunksReadySince >= 3 * SECOND) {
                        HinexOptimize.LOGGER.info("[HinexBench] All sections rendered after {} s", elapsed / SECOND);
                        runner.start(mc);
                        next(Step.RUNNING, now);
                    }
                } else {
                    chunksReadySince = 0;
                }
                if (step == Step.WAIT_CHUNKS && elapsed > 180 * SECOND) {
                    runner.addNote("sections not fully rendered after 180 s");
                    runner.start(mc);
                    next(Step.RUNNING, now);
                }
            }
            case EXIT -> {
                step = Step.OFF;
                if (bool("exit", true)) {
                    HinexOptimize.LOGGER.info("[HinexBench] Autobench finished, closing the game");
                    mc.stop();
                }
            }
            default -> {
            }
        }
    }

    private static void openWorld(Minecraft mc, TitleScreen title) {
        String world = "HinexBench-" + SharedConstants.getCurrentVersion().name().replaceAll("[^A-Za-z0-9.-]", "-");
        if (mc.getLevelSource().levelExists(world)) {
            HinexOptimize.LOGGER.info("[HinexBench] Opening benchmark world {}", world);
            mc.createWorldOpenFlows().openWorld(world, () -> fail(mc, "opening the world was cancelled"));
        } else {
            HinexOptimize.LOGGER.info("[HinexBench] Creating benchmark world {} (seed {})", world, SEED);
            LevelSettings settings = new LevelSettings(world, GameType.SPECTATOR,
                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel(world, settings, new WorldOptions(SEED, true, false),
                WorldPresets::createNormalWorldDimensions, title);
        }
    }

    private static void setUpWorld(Minecraft mc) {
        String scenario = System.getProperty(PREFIX, "spin");
        BenchRunner.Settings settings = new BenchRunner.Settings(
            string("tag", "dev"), scenario, integer("warmup", 5), integer("seconds", 30),
            bool("reload", scenario.equals("spin")), integer("renderDistance", 12));
        runner = BenchRunner.prepare(mc, settings, message -> {
        }, result -> step = Step.EXIT);

        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) {
            runner.addNote("no integrated server, world not prepared");
            return;
        }
        server.execute(() -> {
            var source = server.createCommandSourceStack();
            for (String command : SETUP_COMMANDS) {
                server.getCommands().performPrefixedCommand(source, command);
            }
        });
    }

    private static void fail(Minecraft mc, String reason) {
        HinexOptimize.LOGGER.error("[HinexBench] FAILED: {}", reason);
        step = Step.EXIT;
    }

    private static void next(Step nextStep, long now) {
        step = nextStep;
        stepStart = now;
    }

    private static String string(String name, String fallback) {
        return System.getProperty(PREFIX + "." + name, fallback);
    }

    private static int integer(String name, int fallback) {
        try {
            return Integer.parseInt(string(name, Integer.toString(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean bool(String name, boolean fallback) {
        return Boolean.parseBoolean(string(name, Boolean.toString(fallback)).trim());
    }
}
