package com.wizzardo.vulkan;

import static org.lwjgl.vulkan.VK10.vkDestroyBuffer;
import static org.lwjgl.vulkan.VK10.vkFreeMemory;

import org.lwjgl.vulkan.VkDevice;

import java.nio.ByteBuffer;

public class BufferHolder {
    public final long buffer;
    public final long bufferMemory;
    protected ByteBuffer mappedBuffer;

    public BufferHolder(long buffer, long bufferMemory) {
        this.buffer = buffer;
        this.bufferMemory = bufferMemory;
    }

    public void cleanup(VkDevice device) {
        vkDestroyBuffer(device, buffer, null);
        vkFreeMemory(device, bufferMemory, null);
    }

    public ByteBuffer getMappedBuffer() {
        return mappedBuffer;
    }

    public void setMappedBuffer(ByteBuffer mappedBuffer) {
        this.mappedBuffer = mappedBuffer;
    }
}
