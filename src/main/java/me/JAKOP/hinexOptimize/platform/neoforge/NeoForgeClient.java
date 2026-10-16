package me.JAKOP.hinexOptimize.platform.neoforge;

//? if neoforge {
/*import me.JAKOP.hinexOptimize.bench.BenchCommand;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

final class NeoForgeClient {
    private NeoForgeClient() {
    }

    static void init() {
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) ->
            event.getDispatcher().register(BenchCommand.<CommandSourceStack>create(CommandSourceStack::sendSystemMessage)));
    }
}
*///?}
