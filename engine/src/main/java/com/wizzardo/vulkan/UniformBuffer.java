package com.wizzardo.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class UniformBuffer {
    public final long address;
    public final long memoryAddress;
    public final int size;
    protected ByteBuffer buffer;

    public UniformBuffer(long address, long memoryAddress, int size) {
        this.address = address;
        this.memoryAddress = memoryAddress;
        this.size = size;
    }

    public ByteBuffer map(VkDevice device) {
        if (buffer != null)
            return buffer;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pointer = stack.pointers(0);
            vkMapMemory(device, memoryAddress, 0, size, 0, pointer);
            buffer = pointer.getByteBuffer(0, size);
        }
        return buffer;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void unmap(VkDevice device) {
        if (buffer == null)
            return;

        vkUnmapMemory(device, memoryAddress);
        buffer = null;
    }

    public void cleanup(VkDevice device) {
        unmap(device);
        vkDestroyBuffer(device, address, null);
        vkFreeMemory(device, memoryAddress, null);
    }
}
