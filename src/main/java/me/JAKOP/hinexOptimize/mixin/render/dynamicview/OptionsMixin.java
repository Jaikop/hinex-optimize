package me.JAKOP.hinexOptimize.mixin.render.dynamicview;

import me.JAKOP.hinexOptimize.render.DynamicViewDistance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public abstract class OptionsMixin {
    @Inject(method = "getEffectiveRenderDistance", at = @At("RETURN"), cancellable = true)
    private void hinexoptimize$capRenderDistance(CallbackInfoReturnable<Integer> cir) {
        int capped = DynamicViewDistance.apply(cir.getReturnValueI());
        if (capped != cir.getReturnValueI()) {
            cir.setReturnValue(capped);
        }
    }
}
