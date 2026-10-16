package me.JAKOP.hinexOptimize.mixin.render.smoothview;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The flags became plain volatile booleans in 26.4; before that they were AtomicBooleans. */
@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$SectionTask")
public interface SectionTaskAccessor {
    //? if >=26.4 {
    /*@Accessor("isCompleted")
    boolean hinexoptimize$isCompleted();

    @Accessor("isCancelled")
    boolean hinexoptimize$isCancelled();
    *///?} else {
    @Accessor("isCompleted")
    java.util.concurrent.atomic.AtomicBoolean hinexoptimize$isCompleted();

    @Accessor("isCancelled")
    java.util.concurrent.atomic.AtomicBoolean hinexoptimize$isCancelled();
    //?}
}
