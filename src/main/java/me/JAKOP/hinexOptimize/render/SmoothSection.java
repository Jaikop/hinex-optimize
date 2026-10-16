package me.JAKOP.hinexOptimize.render;

public interface SmoothSection {
    boolean hinexoptimize$isRebuildPending(long now);

    long hinexoptimize$uploadedTime();

    long hinexoptimize$fadeDuration();

    boolean hinexoptimize$wasPreviouslyEmpty();

    void hinexoptimize$copyFadeStateFrom(SmoothSection other);
}
