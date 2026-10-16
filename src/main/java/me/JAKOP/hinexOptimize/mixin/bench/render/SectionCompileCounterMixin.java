package me.JAKOP.hinexOptimize.mixin.bench.render;

import me.JAKOP.hinexOptimize.bench.BenchCounters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Counts chunk sections sent to the mesher. NeoForge adds overloads taking extra geometry
 * renderers; the vanilla ones delegate to them and the game calls them directly, so on NeoForge
 * only those are counted (counting both would count twice).
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection")
public abstract class SectionCompileCounterMixin {
    @Inject(method = {
        //? if neoforge {
        /*"compileAsync(Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;Ljava/util/List;)V",
        "compileSync(Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;Ljava/util/List;)V"
        *///?} else {
        "compileAsync(Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;)V",
        "compileSync(Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;)V"
        //?}
    }, at = @At("HEAD"))
    private void hinexoptimize$countSectionCompile(CallbackInfo ci) {
        BenchCounters.sectionCompiles++;
    }
}
