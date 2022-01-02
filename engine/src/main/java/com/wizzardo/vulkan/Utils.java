package com.wizzardo.vulkan;

import static com.wizzardo.vulkan.AlignmentUtils.alignas;
import static com.wizzardo.vulkan.AlignmentUtils.alignof;

import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_HEAP_DEVICE_LOCAL_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT;
import static org.lwjgl.vulkan.VK10.vkDestroyBuffer;
import static org.lwjgl.vulkan.VK10.vkFreeMemory;
import static org.lwjgl.vulkan.VK10.vkMapMemory;
import static org.lwjgl.vulkan.VK10.vkUnmapMemory;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Pointer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueue;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Collection;
import java.util.List;

public class Utils {
    static void memcpy(ByteBuffer buffer, Vertex[] vertices) {
        for (Vertex vertex : vertices) {
            buffer.putFloat(vertex.pos.x());
            buffer.putFloat(vertex.pos.y());
            buffer.putFloat(vertex.pos.z());

            buffer.putFloat(vertex.color.x());
            buffer.putFloat(vertex.color.y());
            buffer.putFloat(vertex.color.z());

            buffer.putFloat(vertex.texCoords.x());
            buffer.putFloat(vertex.texCoords.y());
        }
    }

    static void memcpy(ByteBuffer buffer, int[] indices) {
        for (int index : indices) {
            buffer.putInt(index);
        }
        buffer.rewind();
    }

    static void memcpy(ByteBuffer buffer, UniformBufferObject ubo) {
        final int mat4Size = 16 * Float.BYTES;
        ubo.model.get(0, buffer);
        ubo.view.get(alignas(mat4Size, alignof(ubo.view)), buffer);
        ubo.proj.get(alignas(mat4Size * 2, alignof(ubo.view)), buffer);
    }

    static void memcpy(ByteBuffer buffer, short[] indices) {
        for (short index : indices) {
            buffer.putShort(index);
        }
        buffer.rewind();
    }

    static PointerBuffer asPointerBuffer(Collection<String> collection) {
        MemoryStack stack = stackGet();
        PointerBuffer buffer = stack.mallocPointer(collection.size());
        collection.stream()
                .map(stack::UTF8)
                .forEach(buffer::put);
        return buffer.rewind();
    }

    static PointerBuffer asPointerBuffer(List<? extends Pointer> list) {
        MemoryStack stack = stackGet();
        PointerBuffer buffer = stack.mallocPointer(list.size());
        list.forEach(buffer::put);
        return buffer.rewind();
    }

    static BufferHolder createVertexBuffer(
            VkPhysicalDevice physicalDevice,
            VkDevice device,
            VkQueue graphicsQueue,
            long commandPool,
            Vertex[] vertices
    ) {
        try (MemoryStack stack = stackPush()) {
            long bufferSize = Vertex.SIZEOF * vertices.length;

            LongBuffer pBuffer = stack.mallocLong(1);
            LongBuffer pBufferMemory = stack.mallocLong(1);
            VulkanBuffers.createBuffer(
                    physicalDevice,
                    device,
                    bufferSize,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    pBuffer,
                    pBufferMemory);

            long stagingBuffer = pBuffer.get(0);
            long stagingBufferMemory = pBufferMemory.get(0);

            PointerBuffer data = stack.mallocPointer(1);

            vkMapMemory(device, stagingBufferMemory, 0, bufferSize, 0, data);
            memcpy(data.getByteBuffer(0, (int) bufferSize), vertices);
            vkUnmapMemory(device, stagingBufferMemory);

            VulkanBuffers.createBuffer(
                    physicalDevice,
                    device,
                    bufferSize,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                    VK_MEMORY_HEAP_DEVICE_LOCAL_BIT,
                    pBuffer,
                    pBufferMemory);

            long vertexBuffer = pBuffer.get(0);
            long vertexBufferMemory = pBufferMemory.get(0);

            VulkanBuffers.copyBuffer(device, graphicsQueue, commandPool, stagingBuffer, vertexBuffer, bufferSize);

            vkDestroyBuffer(device, stagingBuffer, null);
            vkFreeMemory(device, stagingBufferMemory, null);
            return new BufferHolder(vertexBuffer, vertexBufferMemory);
        }
    }

    static BufferHolder createIndexBuffer(
            VkPhysicalDevice physicalDevice,
            VkDevice device,
            VkQueue graphicsQueue,
            long commandPool,
            int[] indices
    ) {
        try (MemoryStack stack = stackPush()) {
            long bufferSize = Integer.BYTES * indices.length; // todo: use short if it's enough

            LongBuffer pBuffer = stack.mallocLong(1);
            LongBuffer pBufferMemory = stack.mallocLong(1);
            VulkanBuffers.createBuffer(
                    physicalDevice,
                    device,
                    bufferSize,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    pBuffer,
                    pBufferMemory);

            long stagingBuffer = pBuffer.get(0);
            long stagingBufferMemory = pBufferMemory.get(0);

            PointerBuffer data = stack.mallocPointer(1);

            vkMapMemory(device, stagingBufferMemory, 0, bufferSize, 0, data);
            memcpy(data.getByteBuffer(0, (int) bufferSize), indices);
            vkUnmapMemory(device, stagingBufferMemory);

            VulkanBuffers.createBuffer(
                    physicalDevice,
                    device,
                    bufferSize,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                    VK_MEMORY_HEAP_DEVICE_LOCAL_BIT,
                    pBuffer,
                    pBufferMemory);

            long indexBuffer = pBuffer.get(0);
            long indexBufferMemory = pBufferMemory.get(0);

            VulkanBuffers.copyBuffer(device, graphicsQueue, commandPool, stagingBuffer, indexBuffer, bufferSize);

            vkDestroyBuffer(device, stagingBuffer, null);
            vkFreeMemory(device, stagingBufferMemory, null);
            return new BufferHolder(indexBuffer, indexBufferMemory);
        }
    }
}
