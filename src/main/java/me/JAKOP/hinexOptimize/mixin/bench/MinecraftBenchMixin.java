package me.JAKOP.hinexOptimize.mixin.bench;

import me.JAKOP.hinexOptimize.bench.BenchHooks;
import me.JAKOP.hinexOptimize.bench.LoadTimes;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(Minecraft.class)
public abstract class MinecraftBenchMixin {
    private static final String RELOAD = "reloadResourcePacks(ZLnet/minecraft/client/GameLoadCookie;)Ljava/util/concurrent/CompletableFuture;";

    @Inject(method = "runTick", at = @At("HEAD"))
    private void hinexoptimize$onFrameStart(boolean advanceGameTime, CallbackInfo ci) {
        BenchHooks.onFrameStart((Minecraft) (Object) this);
    }

    @Inject(method = "onGameLoadFinished", at = @At("HEAD"))
    private void hinexoptimize$onGameLoadFinished(CallbackInfo ci) {
        LoadTimes.onGameLoadFinished();
    }

    @Inject(method = RELOAD, at = @At("HEAD"))
    private void hinexoptimize$onReloadStart(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        LoadTimes.onReloadStart();
    }

    @Inject(method = RELOAD, at = @At("RETURN"))
    private void hinexoptimize$onReloadReturned(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        LoadTimes.onReloadReturned(cir.getReturnValue());
    }
}
