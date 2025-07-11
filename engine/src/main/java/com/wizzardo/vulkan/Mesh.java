package com.wizzardo.vulkan;

import org.joml.Vector3f;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkCmdBindIndexBuffer;

public class Mesh {
    protected Armature armature;
    protected Vertex[] vertices;
    protected int[] indices;
    protected BufferHolder vertexBuffer;
    protected BufferHolder indexBuffer;
    protected BufferHolder instanceBuffer;
    protected BoundingBox boundingBox;
    protected int indexBufferType = VK_INDEX_TYPE_UINT32;

    public Mesh(Vertex[] vertices, int[] indices) {
        this.vertices = vertices;
        this.indices = indices;
    }

    public Vertex[] getVertices() {
        return vertices;
    }

    public void setVertices(Vertex[] vertices) {
        this.vertices = vertices;
    }

    public int[] getIndices() {
        return indices;
    }

    public void setIndices(int[] indices) {
        this.indices = indices;
    }

    public BufferHolder getVertexBuffer() {
        return vertexBuffer;
    }

    public void setVertexBuffer(BufferHolder vertexBuffer) {
        this.vertexBuffer = vertexBuffer;
    }

    public BufferHolder getInstanceBuffer() {
        return instanceBuffer;
    }

    public void setInstanceBuffer(BufferHolder instanceBuffer) {
        this.instanceBuffer = instanceBuffer;
    }

    public BufferHolder getIndexBuffer() {
        return indexBuffer;
    }

    public int getIndexBufferType() {
        return indexBufferType;
    }

    public void setIndexBuffer(BufferHolder indexBuffer) {
        this.indexBuffer = indexBuffer;
    }

    public int getIndicesLength() {
        return indices.length;
    }

    public Armature getArmature() {
        return armature;
    }

    public void prepare(VulkanApplication app, Material.VertexLayout vertexLayout) {
        if (vertexBuffer == null || indexBuffer == null) {
            vertexBuffer = Utils.createVertexBuffer(app.physicalDevice, app.device, app.transferQueue, app.transferCommandPool, getVertices(), vertexLayout);
            indexBuffer = Utils.createIndexBuffer(app.physicalDevice, app.device, app.transferQueue, app.transferCommandPool, getIndices());
            app.addCleanupTask(vertexBuffer, vertexBuffer.createCleanupTask(app.device));
            app.addCleanupTask(indexBuffer, indexBuffer.createCleanupTask(app.device));
        }
    }

    public void draw(VkCommandBuffer commandBuffer, Material material, VulkanApplication.CommandBufferTempData tempData) {
        if (instanceBuffer != null) {
            int count = (int) (instanceBuffer.size / instanceBuffer.sizeof);
            vkCmdDrawIndexed(commandBuffer, getIndicesLength(), count, 0, 0, 0);
        } else {
            vkCmdDrawIndexed(commandBuffer, getIndicesLength(), 1, 0, 0, 0);
        }
    }

    public void bindBuffers(VkCommandBuffer commandBuffer, VulkanApplication.CommandBufferTempData tempData) {
        LongBuffer vertexBuffers = tempData.pLong_1.put(0, vertexBuffer.buffer);
        LongBuffer offsets = tempData.pLong_2.put(0, 0l);
        vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, offsets);
        vkCmdBindIndexBuffer(commandBuffer, indexBuffer.buffer, 0, indexBufferType);
        if (instanceBuffer != null) {
            vkCmdBindVertexBuffers(commandBuffer, 1, tempData.pLong_1.put(0, instanceBuffer.buffer), offsets);
        }
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(BoundingBox boundingBox) {
        this.boundingBox = boundingBox;
    }

    public static class BoundingBox {
        public final Vector3f min;
        public final Vector3f max;

        public BoundingBox(Vector3f min, Vector3f max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public String toString() {
            return "BoundingBox{" +
                    "min=" + min +
                    ", max=" + max +
                    '}';
        }
    }
}
