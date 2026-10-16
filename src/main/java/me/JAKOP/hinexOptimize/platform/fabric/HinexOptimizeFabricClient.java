package me.JAKOP.hinexOptimize.platform.fabric;

//? if fabric {
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class HinexOptimizeFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Fabric API is optional; without it there is no /hinexbench, the autobench still works
        if (FabricLoader.getInstance().isModLoaded("fabric-command-api-v2")) {
            FabricBenchCommands.register();
        }
    }
}
//?}
