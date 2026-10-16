package me.JAKOP.hinexOptimize.mixin.render.smoothview;

import me.JAKOP.hinexOptimize.render.SmoothSection;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tracks whether the latest rebuild of a section has landed. The result of a compile task is
 * applied by an upload callback some frames after the task finished, so "task completed" is not
 * enough: a rebuild is pending until {@code setSectionMesh} ran after the latest request.
 */
@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class RenderSectionMixin implements SmoothSection {
    /** Requests younger than this are always treated as pending (covers late uploads of cancelled tasks). */
    @Unique
    private static final long hinexoptimize$RECENT_NANOS = 500_000_000L;

    @Shadow
    private long uploadedTime;
    //? if <26.3 {
    /*@Shadow
    private long fadeDuration;
    @Shadow
    private boolean wasPreviouslyEmpty;
    *///?}

    @Unique
    private volatile long hinexoptimize$lastRequest;
    @Unique
    private volatile long hinexoptimize$lastApplied;
    @Unique
    private volatile SectionRenderDispatcher.RenderSection.SectionTask hinexoptimize$latestTask;

    // NeoForge adds an overload with extra geometry renderers that the vanilla one delegates to;
    // both are hooked, recording the same task twice is harmless
    @Inject(method = {
        "createCompileTask(Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;)Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection$SectionTask;",
        "createCompileTask(Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection$SectionTask;"
    }, at = @At("RETURN"))
    private void hinexoptimize$onCompileRequested(CallbackInfoReturnable<SectionRenderDispatcher.RenderSection.SectionTask> cir) {
        this.hinexoptimize$latestTask = cir.getReturnValue();
        this.hinexoptimize$lastRequest = System.nanoTime();
    }

    @Inject(method = "setSectionMesh", at = @At("HEAD"))
    private void hinexoptimize$onMeshApplied(CallbackInfoReturnable<?> cir) {
        this.hinexoptimize$lastApplied = System.nanoTime();
    }

    @Override
    public boolean hinexoptimize$isRebuildPending(long now) {
        long requested = this.hinexoptimize$lastRequest;
        if (requested == 0L) {
            return false;
        }
        if (now - requested < hinexoptimize$RECENT_NANOS || requested > this.hinexoptimize$lastApplied) {
            return true;
        }
        SectionRenderDispatcher.RenderSection.SectionTask task = this.hinexoptimize$latestTask;
        if (task == null) {
            return false;
        }
        SectionTaskAccessor flags = (SectionTaskAccessor) task;
        //? if >=26.4 {
        /*return !flags.hinexoptimize$isCompleted() && !flags.hinexoptimize$isCancelled();
        *///?} else
        return !flags.hinexoptimize$isCompleted().get() && !flags.hinexoptimize$isCancelled().get();
    }

    @Override
    public long hinexoptimize$uploadedTime() {
        return this.uploadedTime;
    }

    @Override
    public long hinexoptimize$fadeDuration() {
        //? if <26.3 {
        /*return this.fadeDuration;
        *///?} else
        return 0L;
    }

    @Override
    public boolean hinexoptimize$wasPreviouslyEmpty() {
        //? if <26.3 {
        /*return this.wasPreviouslyEmpty;
        *///?} else
        return false;
    }

    @Override
    public void hinexoptimize$copyFadeStateFrom(SmoothSection other) {
        this.uploadedTime = other.hinexoptimize$uploadedTime();
        //? if <26.3 {
        /*this.fadeDuration = other.hinexoptimize$fadeDuration();
        this.wasPreviouslyEmpty = other.hinexoptimize$wasPreviouslyEmpty();
        *///?}
    }
}
