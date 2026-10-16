package me.JAKOP.hinexOptimize.render;

/** Added to {@code SectionRenderDispatcher.RenderSection} by {@code RenderSectionMixin}. */
public interface SmoothSection {
    /**
     * True when a rebuild of this section was requested and its result may not have reached
     * the section yet. Such a section must be rebuilt again after a move.
     */
    boolean hinexoptimize$isRebuildPending(long now);

    /** Fade-in state: upload time, plus in 26.2 the fade duration and the "was empty" flag. */
    long hinexoptimize$uploadedTime();

    long hinexoptimize$fadeDuration();

    boolean hinexoptimize$wasPreviouslyEmpty();

    /** Copies fade-in state so a moved section does not fade in again. */
    void hinexoptimize$copyFadeStateFrom(SmoothSection other);
}
