package me.JAKOP.hinexOptimize.mixin.bench.render;

import com.llamalad7.mixinextras.sugar.Local;
import me.JAKOP.hinexOptimize.bench.BenchCounters;
import org.lwjgl.PointerBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.IntBuffer;
import java.util.Collection;

@Mixin(targets = {"com.mojang.blaze3d.systems.RenderPass", "com.mojang.renderpearl.frontend.FrontendRenderPass"})
public abstract class RenderPassCounterMixin {
    @Inject(method = "setPipeline", at = @At("HEAD"))
    private void hinexoptimize$countPipelineBind(CallbackInfo ci) {
        BenchCounters.pipelineBinds++;
    }

    @Inject(method = {
        "setUniform(Ljava/lang/String;Ljava/lang/Object;)V",
        "bindTexture",
        "setUniform(Ljava/lang/String;Lcom/mojang/blaze3d/buffers/GpuBuffer;)V",
        "setUniform(Ljava/lang/String;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"
    }, at = @At("HEAD"))
    private void hinexoptimize$countUniformBind(CallbackInfo ci) {
        BenchCounters.uniformBinds++;
    }

    @Inject(method = {"draw(IIII)V", "drawIndexed(IIIII)V", "drawIndexedIndirect", "drawIndirect"}, at = @At("HEAD"))
    private void hinexoptimize$countDraw(CallbackInfo ci) {
        BenchCounters.drawCalls++;
        BenchCounters.drawItems++;
    }

    @Inject(method = "drawMultipleIndexed", at = @At("HEAD"))
    private void hinexoptimize$countDrawMultiple(CallbackInfo ci, @Local(argsOnly = true, ordinal = 0) Collection<?> draws) {
        BenchCounters.drawCalls += draws.size();
        BenchCounters.drawItems += draws.size();
    }

    @Inject(method = {"multiDrawIndexed(Ljava/nio/IntBuffer;III)V", "multiDraw(Ljava/nio/IntBuffer;III)V"}, at = @At("HEAD"))
    private void hinexoptimize$countMultiDraw(IntBuffer drawParameters, int instanceCount, int firstInstance, int drawCount, CallbackInfo ci) {
        BenchCounters.drawCalls++;
        BenchCounters.drawItems += drawCount;
    }

    @Inject(method = "multiDrawIndexed(Lorg/lwjgl/PointerBuffer;Ljava/nio/IntBuffer;Ljava/nio/IntBuffer;I)V", at = @At("HEAD"))
    private void hinexoptimize$countMultiDrawIndexedSeparate(PointerBuffer firstIndexOffsets, IntBuffer indexCounts, IntBuffer vertexOffsets,
                                                             int drawCount, CallbackInfo ci) {
        BenchCounters.drawCalls++;
        BenchCounters.drawItems += drawCount;
    }

    @Inject(method = "multiDraw(Ljava/nio/IntBuffer;Ljava/nio/IntBuffer;I)V", at = @At("HEAD"))
    private void hinexoptimize$countMultiDrawSeparate(IntBuffer firstVertices, IntBuffer vertexCounts, int drawCount, CallbackInfo ci) {
        BenchCounters.drawCalls++;
        BenchCounters.drawItems += drawCount;
    }
}
