package me.JAKOP.hinexOptimize.mixin.bench.render;

import me.JAKOP.hinexOptimize.bench.BenchCounters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Counts descriptor work done by the Vulkan backend per draw. 26.2/26.3 use push descriptors,
 * 26.4 allocates and updates descriptor sets; every version creates a buffer view per texel buffer.
 * Each injector is optional because only some of these calls exist in a given version.
 */
@Mixin(targets = {"com.mojang.blaze3d.vulkan.VulkanRenderPass", "com.mojang.renderpearl.backend.vulkan.VulkanRenderPass"})
public abstract class VulkanDescriptorCounterMixin {
    @Inject(method = "pushDescriptors", require = 0, at = @At(value = "INVOKE",
        target = "Lorg/lwjgl/vulkan/KHRPushDescriptor;vkCmdPushDescriptorSetKHR(Lorg/lwjgl/vulkan/VkCommandBuffer;IJILorg/lwjgl/vulkan/VkWriteDescriptorSet$Buffer;)V"))
    private void hinexoptimize$countPushDescriptor(CallbackInfo ci) {
        BenchCounters.descriptorUpdates++;
    }

    @Inject(method = "pushDescriptors", require = 0, at = @At(value = "INVOKE",
        target = "Lorg/lwjgl/vulkan/VK12;vkUpdateDescriptorSets(Lorg/lwjgl/vulkan/VkDevice;Lorg/lwjgl/vulkan/VkWriteDescriptorSet$Buffer;Lorg/lwjgl/vulkan/VkCopyDescriptorSet$Buffer;)V"))
    private void hinexoptimize$countDescriptorSetUpdate(CallbackInfo ci) {
        BenchCounters.descriptorUpdates++;
    }

    @Inject(method = "pushDescriptors", require = 0, at = @At(value = "INVOKE",
        target = "Lorg/lwjgl/vulkan/VK12;vkCreateBufferView(Lorg/lwjgl/vulkan/VkDevice;Lorg/lwjgl/vulkan/VkBufferViewCreateInfo;Lorg/lwjgl/vulkan/VkAllocationCallbacks;Ljava/nio/LongBuffer;)I"))
    private void hinexoptimize$countBufferView(CallbackInfo ci) {
        BenchCounters.bufferViews++;
    }
}
