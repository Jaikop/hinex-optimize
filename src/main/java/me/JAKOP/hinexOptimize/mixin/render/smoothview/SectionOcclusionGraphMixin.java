package me.JAKOP.hinexOptimize.mixin.render.smoothview;

import me.JAKOP.hinexOptimize.render.SmoothViewDistance;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** {@code consumeFrustumUpdate()} returns true once a full visibility update has finished. */
@Mixin(SectionOcclusionGraph.class)
public abstract class SectionOcclusionGraphMixin {
    @Inject(method = "consumeFrustumUpdate", at = @At("RETURN"))
    private void hinexoptimize$onFrustumUpdateConsumed(CallbackInfoReturnable<Boolean> cir) {
        SmoothViewDistance.onFrustumUpdateConsumed(cir.getReturnValueZ());
    }
}
