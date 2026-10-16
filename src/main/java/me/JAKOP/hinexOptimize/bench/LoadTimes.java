package me.JAKOP.hinexOptimize.bench;

import me.JAKOP.hinexOptimize.HinexOptimize;

import java.lang.management.ManagementFactory;
import java.util.concurrent.CompletableFuture;

public final class LoadTimes {
    private static volatile boolean gameLoaded;
    private static volatile long startupMillis = -1;
    private static volatile BenchCounters.Snapshot startupCounters = BenchCounters.Snapshot.ZERO;

    private static volatile boolean reloading;
    private static volatile long reloadStartNanos;
    private static volatile long lastReloadMillis = -1;
    private static volatile int finishedReloads;

    private LoadTimes() {
    }

    public static void onGameLoadFinished() {
        if (gameLoaded) {
            return;
        }
        gameLoaded = true;
        startupMillis = System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean().getStartTime();
        startupCounters = BenchCounters.snapshot();
        HinexOptimize.LOGGER.info("[HinexBench] Startup took {} ms (since JVM start), {} shaders compiled in {} ms CPU, {} pipelines in {} ms CPU",
            startupMillis, startupCounters.glslCount(), startupCounters.glslNanos() / 1_000_000,
            startupCounters.pipelineCount(), startupCounters.pipelineNanos() / 1_000_000);
    }

    public static void onReloadStart() {
        reloading = true;
        reloadStartNanos = System.nanoTime();
    }

    public static void onReloadReturned(CompletableFuture<?> future) {
        long start = reloadStartNanos;
        future.whenComplete((result, error) -> {
            lastReloadMillis = (System.nanoTime() - start) / 1_000_000;
            reloading = false;
            finishedReloads++;
            HinexOptimize.LOGGER.info("[HinexBench] Resource reload took {} ms{}", lastReloadMillis, error != null ? " (failed)" : "");
        });
    }

    public static boolean isGameLoaded() {
        return gameLoaded;
    }

    public static long startupMillis() {
        return startupMillis;
    }

    public static BenchCounters.Snapshot startupCounters() {
        return startupCounters;
    }

    public static boolean isReloading() {
        return reloading;
    }

    public static long lastReloadMillis() {
        return lastReloadMillis;
    }

    public static int finishedReloads() {
        return finishedReloads;
    }
}
