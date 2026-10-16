package me.JAKOP.hinexOptimize.mixin.render.smoothview;

import net.minecraft.client.RotatingSectionStorage;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ViewArea.class)
public interface ViewAreaAccessor {
    @Accessor("sections")
    RotatingSectionStorage<SectionRenderDispatcher.RenderSection> hinexoptimize$sections();

    @Invoker("getRenderSection")
    SectionRenderDispatcher.RenderSection hinexoptimize$getRenderSection(long sectionNode);
}
