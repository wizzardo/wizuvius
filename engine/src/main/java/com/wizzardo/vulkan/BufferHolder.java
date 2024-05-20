package com.wizzardo.vulkan;

import org.lwjgl.vulkan.VkDevice;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class BufferHolder {
    public final long buffer;
    public final long bufferMemory;
    public final long size;
    public final int sizeof;
    protected ByteBuffer mappedBuffer;

    public BufferHolder(long buffer, long bufferMemory, long size, int sizeof) {
        this.buffer = buffer;
        this.bufferMemory = bufferMemory;
        this.size = size;
        this.sizeof = sizeof;
    }

    public ByteBuffer getMappedBuffer() {
        return mappedBuffer;
    }

    public void setMappedBuffer(ByteBuffer mappedBuffer) {
        this.mappedBuffer = mappedBuffer;
    }

    public Runnable createCleanupTask(VkDevice device) {
        ByteBuffer mappedBuffer = this.mappedBuffer;
        long bufferMemory = this.bufferMemory;
        long buffer = this.buffer;

        return () -> {
            if (mappedBuffer != null) {
                vkUnmapMemory(device, bufferMemory);
            }
            vkDestroyBuffer(device, buffer, null);
            vkFreeMemory(device, bufferMemory, null);
        };
    }
}
