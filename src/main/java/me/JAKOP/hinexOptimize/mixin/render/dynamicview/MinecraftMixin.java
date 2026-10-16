package me.JAKOP.hinexOptimize.mixin.render.dynamicview;

import me.JAKOP.hinexOptimize.render.DynamicViewDistance;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "runTick", at = @At("HEAD"))
    private void hinexoptimize$onFrameStart(boolean advanceGameTime, CallbackInfo ci) {
        DynamicViewDistance.onFrame((Minecraft) (Object) this, System.nanoTime());
    }
}
