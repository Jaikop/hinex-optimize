package me.JAKOP.hinexOptimize.mixin.core;

import me.JAKOP.hinexOptimize.HinexOptimize;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Smoke test for the mixin pipeline; later becomes the startup timer for the benchmark. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void hinexoptimize$afterInit(CallbackInfo ci) {
        HinexOptimize.LOGGER.info("HinexOptimize mixins active on the client");
    }
}
