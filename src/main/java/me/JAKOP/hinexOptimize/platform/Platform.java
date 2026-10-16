package me.JAKOP.hinexOptimize.platform;

import java.nio.file.Path;

public sealed interface Platform {
    Platform INSTANCE =
        /*? if fabric {*/new FabricPlatform();
        /*?} elif neoforge *///new NeoForgePlatform();

    String loaderName();

    Path configDir();

    boolean isClient();

    boolean isModLoaded(String id);

    //? if fabric {
    final class FabricPlatform implements Platform {
        private final net.fabricmc.loader.api.FabricLoader loader = net.fabricmc.loader.api.FabricLoader.getInstance();

        @Override
        public String loaderName() {
            return "fabric";
        }

        @Override
        public Path configDir() {
            return loader.getConfigDir();
        }

        @Override
        public boolean isClient() {
            return loader.getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT;
        }

        @Override
        public boolean isModLoaded(String id) {
            return loader.isModLoaded(id);
        }
    }
    //?} elif neoforge {
    /*final class NeoForgePlatform implements Platform {
        @Override
        public String loaderName() {
            return "neoforge";
        }

        @Override
        public Path configDir() {
            return net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
        }

        @Override
        public boolean isClient() {
            return net.neoforged.fml.loading.FMLEnvironment.getDist().isClient();
        }

        @Override
        public boolean isModLoaded(String id) {
            return net.neoforged.fml.loading.FMLLoader.getCurrent().getLoadingModList().getModFileById(id) != null;
        }
    }
    *///?}
}
