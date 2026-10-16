package me.JAKOP.hinexOptimize.render;

import me.JAKOP.hinexOptimize.HinexOptimize;
import me.JAKOP.hinexOptimize.core.config.HinexConfig;
import net.minecraft.client.Minecraft;

import java.util.Locale;

/**
 * Lowers the rendered distance when the frame rate drops below the target and raises it back when
 * there is headroom (decisions in {@link ViewDistanceController}). Only
 * {@code Options.getEffectiveRenderDistance()} is capped: the saved setting and the distance requested
 * from the server stay as the player set them, so chunks stay loaded and going back up only needs
 * meshing. Works best with {@link SmoothViewDistance}, which makes each step cost one ring of
 * sections instead of a full rebuild.
 */
public final class DynamicViewDistance {
    private static final ViewDistanceController CONTROLLER = new ViewDistanceController();
    private static volatile int cap = ViewDistanceController.NO_CAP;

    private DynamicViewDistance() {
    }

    /** Called from {@code Options.getEffectiveRenderDistance()}, possibly off the render thread. */
    public static int apply(int effectiveDistance) {
        return Math.min(effectiveDistance, cap);
    }

    public static void onFrame(Minecraft mc, long now) {
        if (mc.level == null) {
            CONTROLLER.reset();
            cap = ViewDistanceController.NO_CAP;
            return;
        }
        HinexConfig config = HinexConfig.get();
        ViewDistanceController.Settings settings = new ViewDistanceController.Settings(
            config.getInt("dynamicview.target_fps"), config.getInt("dynamicview.min_distance"), config.getInt("dynamicview.max_reduction"));
        ViewDistanceController.Decision decision = CONTROLLER.onFrame(now, mc.options.renderDistance().get(),
            mc.options.framerateLimit().get(), mc.levelRenderer.hasRenderedAllSections(), settings);
        cap = CONTROLLER.cap();
        if (decision != null) {
            HinexOptimize.LOGGER.info("Dynamic render distance -> {} (setting {}), p90 frame {} ms, target {} ms",
                decision.distance(), mc.options.renderDistance().get(),
                String.format(Locale.ROOT, "%.1f", decision.p90Nanos() / 1e6), String.format(Locale.ROOT, "%.1f", decision.targetNanos() / 1e6));
        }
    }
}
