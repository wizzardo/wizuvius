package com.wizzardo.vulkan.material.predefined;

import com.wizzardo.vulkan.*;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class UnshadedColor extends Material {

    protected Vector3f color;
    protected UniformBuffers uniformBuffers;
    protected VkDevice device;

    public UnshadedColor(Vector3f color) {
        setVertexShader("shaders/unshaded_color.vert.spv");
        setFragmentShader("shaders/unshaded_color.frag.spv");
        this.color = color;
    }

    public void setColor(Vector3f color) {
        this.color = color;
        if (uniformBuffers != null) {
            updateBuffers();
        }
    }

    private void updateBuffers() {
        if (uniformBuffers == null)
            return;
        if (device == null)
            return;

        try (MemoryStack stack = stackPush()) {
            PointerBuffer data = stack.mallocPointer(1);
            long memoryAddress = uniformBuffers.uniformBuffersMemory.get(0);
            vkMapMemory(device, memoryAddress, 0, uniformBuffers.size, 0, data);

            ByteBuffer buffer = data.getByteBuffer(0, uniformBuffers.size);
            buffer.putFloat(color.x);
            buffer.putFloat(color.y);
            buffer.putFloat(color.z);

            vkUnmapMemory(device, memoryAddress);
        }
    }

    @Override
    protected void prepare(VulkanApplication application, Viewport viewport) {
        if (uniformBuffers == null)
            uniformBuffers = UniformBuffers.createUniformBuffers(application.getPhysicalDevice(), application.getDevice(), 1, Float.SIZE * 3);

        this.device = application.getDevice();

        bindings = Arrays.asList(
                new VulkanDescriptorSets.DescriptorSetLayoutBindingUBO(0, VK_SHADER_STAGE_VERTEX_BIT)
                , new VulkanDescriptorSets.DescriptorSetLayoutBindingUniformBuffer(1, VK_SHADER_STAGE_FRAGMENT_BIT, uniformBuffers)
        );

        descriptorSetLayout = VulkanDescriptorSets.createDescriptorSetLayout(device, bindings);

        updateBuffers();
        super.prepare(application, viewport);
    }
}
