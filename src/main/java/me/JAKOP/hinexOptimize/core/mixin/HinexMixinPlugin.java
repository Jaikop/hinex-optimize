package me.JAKOP.hinexOptimize.core.mixin;

import me.JAKOP.hinexOptimize.HinexOptimize;
import me.JAKOP.hinexOptimize.core.config.HinexConfig;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.util.List;
import java.util.Set;

/**
 * Skips mixins that are disabled in the config, and mixins whose target class does not
 * exist in this Minecraft version (renderpearl classes move between 26.x releases).
 */
public final class HinexMixinPlugin implements IMixinConfigPlugin {
    private String mixinPackage;

    @Override
    public void onLoad(String mixinPackage) {
        this.mixinPackage = mixinPackage + ".";
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.startsWith(mixinPackage)) {
            HinexOptimize.LOGGER.error("Mixin {} is outside of package {}", mixinClassName, mixinPackage);
            return false;
        }

        String relative = mixinClassName.substring(mixinPackage.length());
        int lastDot = relative.lastIndexOf('.');
        String packagePath = lastDot < 0 ? "" : relative.substring(0, lastDot);
        if (!HinexConfig.get().isMixinEnabled(packagePath)) {
            HinexOptimize.LOGGER.info("Mixin {} disabled by config", relative);
            return false;
        }

        if (!targetExists(targetClassName)) {
            HinexOptimize.LOGGER.debug("Mixin {} skipped: target {} not present in this version", relative, targetClassName);
            return false;
        }
        return true;
    }

    private static boolean targetExists(String className) {
        try {
            MixinService.getService().getBytecodeProvider().getClassNode(className.replace('.', '/'));
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Exception e) {
            // Unexpected provider failure: let Mixin decide and report it
            return true;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
