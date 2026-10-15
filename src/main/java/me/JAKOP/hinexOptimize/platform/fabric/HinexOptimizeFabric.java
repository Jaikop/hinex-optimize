package me.JAKOP.hinexOptimize.platform.fabric;

//? if fabric {
import me.JAKOP.hinexOptimize.HinexOptimize;
import net.fabricmc.api.ModInitializer;

public final class HinexOptimizeFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        HinexOptimize.init();
    }
}
//?}
