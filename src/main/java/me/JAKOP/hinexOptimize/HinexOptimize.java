package me.JAKOP.hinexOptimize;

import me.JAKOP.hinexOptimize.core.config.HinexConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HinexOptimize {
    public static final String MOD_ID = "hinexoptimize";
    public static final String VERSION = /*$ mod_version*/ "0.0.1";
    public static final String MINECRAFT = /*$ minecraft*/ "26.3";
    public static final Logger LOGGER = LoggerFactory.getLogger("HinexOptimize");

    private HinexOptimize() {
    }

    /** Called from the loader entrypoint, after mixins have been applied. */
    public static void init() {
        HinexConfig config = HinexConfig.get();
        LOGGER.info("HinexOptimize {} for Minecraft {} loaded, {} of {} options enabled",
            VERSION, MINECRAFT, config.enabledCount(), config.options().size());
    }
}
