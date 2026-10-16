package me.JAKOP.hinexOptimize.platform.fabric;

//? if fabric {
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class HinexOptimizeFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (FabricLoader.getInstance().isModLoaded("fabric-command-api-v2")) {
            FabricBenchCommands.register();
        }
    }
}
//?}
