package me.JAKOP.hinexOptimize.mixin.bench.render;

import me.JAKOP.hinexOptimize.bench.BenchCounters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection")
public abstract class SectionCompileCounterMixin {
    // NeoForge adds overloads that the vanilla methods delegate to; count only the ones the game calls
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
