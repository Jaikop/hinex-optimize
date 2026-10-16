package me.JAKOP.hinexOptimize.mixin.bench.render;

import me.JAKOP.hinexOptimize.bench.BenchCounters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Times GLSL to SPIR-V compilation through shaderc. The class moved in every release:
 * 26.2 (Vulkan only), 26.3 and 26.4.
 */
@Mixin(targets = {
    "com.mojang.blaze3d.vulkan.glsl.GlslCompiler",
    "com.mojang.renderpearl.frontend.shaders.GlslCompiler",
    "com.mojang.blaze3d.pipeline.GlslCompiler"
})
public abstract class GlslCompileTimerMixin {
    @Inject(method = {"createIntermediary", "compileToSpv"}, at = @At("HEAD"))
    private void hinexoptimize$beginCompile(CallbackInfoReturnable<?> cir) {
        BenchCounters.GLSL.begin();
    }

    @Inject(method = {"createIntermediary", "compileToSpv"}, at = @At("RETURN"))
    private void hinexoptimize$endCompile(CallbackInfoReturnable<?> cir) {
        BenchCounters.GLSL.end();
    }
}
