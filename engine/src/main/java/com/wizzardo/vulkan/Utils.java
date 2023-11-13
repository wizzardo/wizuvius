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

import org.joml.Matrix4f;
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
    static void memcpy(ByteBuffer buffer, Vertex[] vertices, Material.VertexLayout vertexLayout) {
        for (int i = 0; i < vertices.length; i++) {
            Vertex vertex = vertices[i];
            List<Material.VertexLayout.BindingDescription> locations = vertexLayout.locations;
            for (int j = 0; j < locations.size(); j++) {
                Material.VertexLayout.BindingDescription location = locations.get(j);
                if (location == Material.VertexLayout.BindingDescription.POSITION) {
                    buffer.putFloat(vertex.pos.x());
                    buffer.putFloat(vertex.pos.y());
                    buffer.putFloat(vertex.pos.z());
                } else if (location == Material.VertexLayout.BindingDescription.COLOR) {
                    buffer.putFloat(vertex.color.x());
                    buffer.putFloat(vertex.color.y());
                    buffer.putFloat(vertex.color.z());
                } else if (location == Material.VertexLayout.BindingDescription.TEXTURE_COORDINATES) {
                    buffer.putFloat(vertex.texCoords.x());
                    buffer.putFloat(vertex.texCoords.y());
                } else if (location == Material.VertexLayout.BindingDescription.NORMAL) {
                    buffer.putFloat(vertex.normal.x());
                    buffer.putFloat(vertex.normal.y());
                    buffer.putFloat(vertex.normal.z());
                } else if (location == Material.VertexLayout.BindingDescription.TANGENT) {
                    buffer.putFloat(vertex.tangent.x());
                    buffer.putFloat(vertex.tangent.y());
                    buffer.putFloat(vertex.tangent.z());
                } else if (location == Material.VertexLayout.BindingDescription.BONE_INDEX) {
                    buffer.putInt(vertex.boneIndexes.x());
                    buffer.putInt(vertex.boneIndexes.y());
                    buffer.putInt(vertex.boneIndexes.z());
                    buffer.putInt(vertex.boneIndexes.w());
                } else if (location == Material.VertexLayout.BindingDescription.BONE_WEIGHT) {
                    buffer.putFloat(vertex.boneWeights.x());
                    buffer.putFloat(vertex.boneWeights.y());
                    buffer.putFloat(vertex.boneWeights.z());
                    buffer.putFloat(vertex.boneWeights.w());
                }
            }
        }
    }

    static void memcpy(ByteBuffer buffer, int[] indices) {
        for (int index : indices) {
            buffer.putInt(index);
        }
        buffer.rewind();
    }

    static void memcpy(ByteBuffer buffer, Matrix4f model, Matrix4f view, Matrix4f proj) {
        final int mat4Size = 16 * Float.BYTES;
        model.get(0, buffer);
        view.get(alignas(mat4Size, AlignmentUtils.MATRIX_4F), buffer);
        proj.get(alignas(mat4Size * 2, AlignmentUtils.MATRIX_4F), buffer);
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
            VkQueue queue,
            long commandPool,
            Vertex[] vertices,
            Material.VertexLayout vertexLayout
    ) {
        try (MemoryStack stack = stackPush()) {
            long bufferSize = (long) vertexLayout.sizeof * vertices.length;

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
            memcpy(data.getByteBuffer(0, (int) bufferSize), vertices, vertexLayout);
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

            VulkanBuffers.copyBuffer(device, queue, commandPool, stagingBuffer, vertexBuffer, bufferSize);

            vkDestroyBuffer(device, stagingBuffer, null);
            vkFreeMemory(device, stagingBufferMemory, null);
            return new BufferHolder(vertexBuffer, vertexBufferMemory, bufferSize, vertexLayout.sizeof);
        }
    }

    static BufferHolder createIndexBuffer(
            VkPhysicalDevice physicalDevice,
            VkDevice device,
            VkQueue queue,
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

            VulkanBuffers.copyBuffer(device, queue, commandPool, stagingBuffer, indexBuffer, bufferSize);

            vkDestroyBuffer(device, stagingBuffer, null);
            vkFreeMemory(device, stagingBufferMemory, null);
            return new BufferHolder(indexBuffer, indexBufferMemory, bufferSize, Integer.BYTES);
        }
    }
}
