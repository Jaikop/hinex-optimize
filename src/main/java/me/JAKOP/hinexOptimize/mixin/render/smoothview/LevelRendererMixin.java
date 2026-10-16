package me.JAKOP.hinexOptimize.mixin.render.smoothview;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.JAKOP.hinexOptimize.render.SmoothViewDistance;
import net.minecraft.client.Camera;
import net.minecraft.client.Options;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the full geometry invalidation with a resize when the only change was the render
 * distance. Mirrors vanilla {@code invalidateCompiledGeometry}, except that the section compiler is
 * kept (nothing it depends on changed) and meshes inside both grids move to the new grid.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow
    private ViewArea viewArea;
    @Shadow
    private SectionRenderDispatcher sectionRenderDispatcher;
    @Shadow
    @Final
    private SectionOcclusionGraph sectionOcclusionGraph;
    @Shadow
    @Final
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;
    @Shadow
    @Final
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> nearbyVisibleSections;

    @Shadow
    public abstract CloudRenderer cloudRenderer();

    @Inject(method = "invalidateCompiledGeometry", at = @At("HEAD"), cancellable = true)
    private void hinexoptimize$resizeInsteadOfRebuild(ClientLevel level, Options options, Camera camera, BlockColors blockColors,
                                                     CallbackInfo ci) {
        SmoothViewDistance.Request request = SmoothViewDistance.takePending();
        ViewArea oldArea = this.viewArea;
        if (request == null || oldArea == null || this.sectionRenderDispatcher == null
            || oldArea.minSectionY() != level.getMinSectionY() || oldArea.maxSectionY() != level.getMaxSectionY()) {
            return; // first build, new level or another reason: vanilla rebuild
        }

        long[] t = new long[6];
        t[0] = System.nanoTime();
        int newDistance = options.getEffectiveRenderDistance();
        ViewArea newArea = new ViewArea(this.sectionRenderDispatcher, level.getMinY(), level.getMaxY(),
            level.getMinSectionY(), level.getMaxSectionY(), newDistance, this.sectionOcclusionGraph);
        t[1] = System.nanoTime();
        // Waits for an in-flight visibility update that may still read the old grid
        this.sectionOcclusionGraph.waitAndReset(newArea);
        // Drops a "graph ready" flag left over from the old grid, so only the new graph counts as ready
        this.sectionOcclusionGraph.consumeFrustumUpdate();
        t[2] = System.nanoTime();
        newArea.repositionCamera(SectionPos.of(camera.position()));
        t[3] = System.nanoTime();

        SmoothViewDistance.Stats stats = SmoothViewDistance.transfer(oldArea, newArea, request);
        // Keep drawing what was visible (now from the new grid) until the new graph is computed
        SmoothViewDistance.remapVisible(this.visibleSections, newArea);
        SmoothViewDistance.remapVisible(this.nearbyVisibleSections, newArea);
        SmoothViewDistance.markGraphPending();
        t[4] = System.nanoTime();

        // Releases what did not move and cancels rebuilds still in flight on the old grid
        SmoothViewDistance.releaseOld(oldArea);
        this.sectionRenderDispatcher.clearCompileQueue();
        this.cloudRenderer().markForRebuild();
        this.viewArea = newArea;
        t[5] = System.nanoTime();
        SmoothViewDistance.log(oldArea.getViewDistance(), newDistance, stats, t);
        ci.cancel();
    }
}
