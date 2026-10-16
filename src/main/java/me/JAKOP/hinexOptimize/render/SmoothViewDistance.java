package me.JAKOP.hinexOptimize.render;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.JAKOP.hinexOptimize.HinexOptimize;
import me.JAKOP.hinexOptimize.mixin.render.smoothview.ViewAreaAccessor;
import net.minecraft.client.SectionUpdateTracker;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;

public final class SmoothViewDistance {
    private static final long GRAPH_WAIT_NANOS = 2_000_000_000L;

    private static boolean changingViewDistance;
    private static Request pending;
    private static long graphPendingSince;

    private SmoothViewDistance() {
    }

    public record Request(SectionUpdateTracker oldTracker, SectionUpdateTracker newTracker) {
    }

    public record Stats(int moved, int keptClean, int rebuilt, int released) {
    }

    public static void beginViewDistanceChange() {
        changingViewDistance = true;
    }

    public static void endViewDistanceChange(SectionUpdateTracker before, SectionUpdateTracker after) {
        changingViewDistance = false;
        pending = before != null && after != null && before != after ? new Request(before, after) : null;
    }

    public static void onAllChanged() {
        if (!changingViewDistance) {
            pending = null;
        }
    }

    public static Request takePending() {
        Request request = pending;
        pending = null;
        return request;
    }

    public static Stats transfer(ViewArea oldArea, ViewArea newArea, Request request) {
        long now = System.nanoTime();
        int[] counts = new int[2];
        ViewAreaAccessor newAccess = (ViewAreaAccessor) newArea;
        // forEach, not for-each: the storage iterator skips the last element
        ((ViewAreaAccessor) oldArea).hinexoptimize$sections().forEach(previous -> {
            SectionMesh mesh = previous.sectionMesh.get();
            if (mesh == CompiledSectionMesh.UNCOMPILED) {
                return;
            }
            long node = previous.getSectionNode();
            SectionRenderDispatcher.RenderSection section = newAccess.hinexoptimize$getRenderSection(node);
            if (section == null || section.getSectionNode() != node) {
                return;
            }
            boolean pendingRebuild = ((SmoothSection) previous).hinexoptimize$isRebuildPending(now);
            if (!previous.sectionMesh.compareAndSet(mesh, CompiledSectionMesh.UNCOMPILED)) {
                return;
            }
            section.sectionMesh.set(mesh);
            ((SmoothSection) section).hinexoptimize$copyFadeStateFrom((SmoothSection) previous);
            counts[0]++;

            SectionUpdateTracker.SectionDirtyState oldState = request.oldTracker().getDirtyState(node);
            SectionUpdateTracker.SectionDirtyState newState = request.newTracker().getDirtyState(node);
            if (newState == null) {
                return;
            }
            if (oldState != null && !oldState.isDirty() && !pendingRebuild) {
                newState.setNotDirty();
                counts[1]++;
            } else if (oldState != null && oldState.isDirtyFromPlayer()) {
                newState.setDirty(true);
            }
        });
        return new Stats(counts[0], counts[1], newArea.size() - counts[1], Math.max(0, oldArea.size() - counts[0]));
    }

    public static void releaseOld(ViewArea oldArea) {
        long now = System.nanoTime();
        ((ViewAreaAccessor) oldArea).hinexoptimize$sections().forEach(section -> {
            if (section.sectionMesh.get() != CompiledSectionMesh.UNCOMPILED
                || section.transparencyResortingScheduled()
                || ((SmoothSection) section).hinexoptimize$isRebuildPending(now)) {
                section.reset();
            }
        });
    }

    public static void remapVisible(ObjectArrayList<SectionRenderDispatcher.RenderSection> sections, ViewArea newArea) {
        ViewAreaAccessor access = (ViewAreaAccessor) newArea;
        int kept = 0;
        for (int i = 0; i < sections.size(); i++) {
            SectionRenderDispatcher.RenderSection old = sections.get(i);
            SectionRenderDispatcher.RenderSection replacement = access.hinexoptimize$getRenderSection(old.getSectionNode());
            if (replacement != null && replacement.getSectionNode() == old.getSectionNode()) {
                sections.set(kept++, replacement);
            }
        }
        sections.size(kept);
    }

    public static void markGraphPending() {
        graphPendingSince = System.nanoTime();
    }

    public static void onFrustumUpdateConsumed(boolean graphUpdated) {
        if (graphUpdated && graphPendingSince != 0L) {
            HinexOptimize.LOGGER.debug("Visibility graph ready {} ms after the render distance change",
                (System.nanoTime() - graphPendingSince) / 1_000_000);
            graphPendingSince = 0L;
        }
    }

    public static boolean shouldDeferFrustum() {
        if (graphPendingSince == 0L) {
            return false;
        }
        if (System.nanoTime() - graphPendingSince > GRAPH_WAIT_NANOS) {
            HinexOptimize.LOGGER.warn("Visibility graph did not report back within 2 s after a render distance change");
            graphPendingSince = 0L;
            return false;
        }
        return true;
    }

    public static void log(int fromDistance, int toDistance, Stats stats, long[] t) {
        HinexOptimize.LOGGER.debug("Render distance {} -> {} without full rebuild: {} meshes kept ({} up to date), {} sections to build;"
                + " {} ms (grid {}, graph {}, place {}, move {}, release {})",
            fromDistance, toDistance, stats.moved(), stats.keptClean(), stats.rebuilt(), ms(t[5] - t[0]),
            ms(t[1] - t[0]), ms(t[2] - t[1]), ms(t[3] - t[2]), ms(t[4] - t[3]), ms(t[5] - t[4]));
    }

    private static String ms(long nanos) {
        return String.format(java.util.Locale.ROOT, "%.1f", nanos / 1e6);
    }
}
