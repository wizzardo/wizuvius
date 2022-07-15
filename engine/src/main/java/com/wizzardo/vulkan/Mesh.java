package com.wizzardo.vulkan;

import org.joml.Vector3f;
import org.lwjgl.vulkan.VkDevice;

public class Mesh {
    private Vertex[] vertices;
    private int[] indices;

    private BufferHolder vertexBuffer;
    private BufferHolder indexBuffer;
    private int indicesLength;
    protected BoundingBox boundingBox;

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

    public BufferHolder getIndexBuffer() {
        return indexBuffer;
    }

    public void setIndexBuffer(BufferHolder indexBuffer) {
        this.indexBuffer = indexBuffer;
    }

    public int getIndicesLength() {
        return indicesLength;
    }

    public void setIndicesLength(int indicesLength) {
        this.indicesLength = indicesLength;
    }

    public void cleanup(VkDevice device) {
        try {
            if (indexBuffer != null)
                indexBuffer.cleanup(device);
            if (vertexBuffer != null)
                vertexBuffer.cleanup(device);
        } finally {
            indexBuffer = null;
            vertexBuffer = null;
            indicesLength = 0;
        }
    }

    public void prepare(VulkanApplication app, Material.VertexLayout vertexLayout) {
        if (vertexBuffer == null || indexBuffer == null) {
            vertexBuffer = Utils.createVertexBuffer(app.physicalDevice, app.device, app.transferQueue, app.commandPool, getVertices(), vertexLayout);
            indexBuffer = Utils.createIndexBuffer(app.physicalDevice, app.device, app.transferQueue, app.commandPool, getIndices());
            setIndicesLength(getIndices().length);
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
