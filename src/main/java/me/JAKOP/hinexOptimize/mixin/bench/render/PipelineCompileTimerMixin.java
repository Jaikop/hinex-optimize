package me.JAKOP.hinexOptimize.mixin.bench.render;

import me.JAKOP.hinexOptimize.bench.BenchCounters;
import me.JAKOP.hinexOptimize.bench.BenchHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Times backend pipeline creation ({@code compilePipeline} on the OpenGL and Vulkan devices).
 * In 26.2 this includes the GLSL compile; in 26.3+ GLSL is compiled by the frontend beforehand.
 */
@Mixin(targets = {
    "com.mojang.blaze3d.vulkan.VulkanDevice",
    "com.mojang.blaze3d.opengl.GlDevice",
    "com.mojang.renderpearl.backend.vulkan.VulkanDevice",
    "com.mojang.renderpearl.backend.opengl.GlDevice"
})
public abstract class PipelineCompileTimerMixin {
    @Inject(method = "compilePipeline", at = @At("HEAD"))
    private void hinexoptimize$beginCompile(CallbackInfoReturnable<?> cir) {
        BenchCounters.PIPELINE.begin();
    }

    @Inject(method = "compilePipeline", at = @At("RETURN"))
    private void hinexoptimize$endCompile(CallbackInfoReturnable<?> cir) {
        BenchHooks.onPipelineCompiled(BenchCounters.PIPELINE.end());
    }
}
