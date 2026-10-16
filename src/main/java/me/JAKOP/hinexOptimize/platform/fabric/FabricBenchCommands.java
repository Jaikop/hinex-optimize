package me.JAKOP.hinexOptimize.platform.fabric;

//? if fabric {
import me.JAKOP.hinexOptimize.bench.BenchCommand;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

/** Only loaded when Fabric API's client command module is present. */
final class FabricBenchCommands {
    private FabricBenchCommands() {
    }

    static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(BenchCommand.<FabricClientCommandSource>create(FabricClientCommandSource::sendFeedback)));
    }
}
//?}
