package me.JAKOP.hinexOptimize.mixin.render.smoothview;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.JAKOP.hinexOptimize.render.SmoothViewDistance;
import net.minecraft.client.SectionUpdateTracker;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code extract} calls {@code allChanged()} only when the effective render distance changed; that
 * call is marked so {@link SmoothViewDistance} can resize instead of rebuilding everything.
 * Every other {@code allChanged()} keeps the vanilla full rebuild.
 */
@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {
    @Shadow
    private SectionUpdateTracker sectionUpdateTracker;

    @WrapOperation(method = "extract", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/extract/LevelExtractor;allChanged()V"))
    private void hinexoptimize$onViewDistanceChanged(LevelExtractor self, Operation<Void> original) {
        SectionUpdateTracker before = this.sectionUpdateTracker;
        SmoothViewDistance.beginViewDistanceChange();
        try {
            original.call(self);
        } finally {
            SmoothViewDistance.endViewDistanceChange(before, this.sectionUpdateTracker);
        }
    }

    @Inject(method = "allChanged", at = @At("HEAD"))
    private void hinexoptimize$onAllChanged(CallbackInfo ci) {
        SmoothViewDistance.onAllChanged();
    }

    /** Camera rotation re-culls from the (still empty) new graph; keep the remapped list instead. */
    @Inject(method = "applyFrustum", at = @At("HEAD"), cancellable = true)
    private void hinexoptimize$deferFrustumUntilGraphReady(CallbackInfo ci) {
        if (SmoothViewDistance.shouldDeferFrustum()) {
            ci.cancel();
        }
    }
}
