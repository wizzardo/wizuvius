package com.wizzardo.vulkan;

import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32B32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_VERTEX_INPUT_RATE_VERTEX;

import org.joml.Vector2fc;
import org.joml.Vector3fc;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

public class Vertex {

    public static final int SIZEOF = (3 + 3 + 2) * Float.BYTES;
    public static final int OFFSETOF_POS = 0;
    public static final int OFFSETOF_COLOR = 3 * Float.BYTES;
    public static final int OFFSETOF_TEXTCOORDS = 6 * Float.BYTES;

    public final Vector3fc pos;
    public final Vector3fc color;
    public final Vector2fc texCoords;
    public final Vector3fc normal;

    public Vertex(Vector3fc pos, Vector3fc color, Vector2fc texCoords, Vector3fc normal) {
        this.pos = pos;
        this.color = color;
        this.texCoords = texCoords;
        this.normal = normal;
    }

    public Vertex(Vector3fc pos, Vector3fc color, Vector2fc texCoords) {
        this(pos, color, texCoords, null);
    }

    public static VkVertexInputBindingDescription.Buffer getBindingDescription(MemoryStack stack) {
        VkVertexInputBindingDescription.Buffer bindingDescription =
                VkVertexInputBindingDescription.calloc(1, stack);

        bindingDescription.binding(0);
        bindingDescription.stride(Vertex.SIZEOF);
        bindingDescription.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

        return bindingDescription;
    }

    public static VkVertexInputAttributeDescription.Buffer getAttributeDescriptions(MemoryStack stack) {
        VkVertexInputAttributeDescription.Buffer attributeDescriptions =
                VkVertexInputAttributeDescription.calloc(3, stack);

        // Position
        VkVertexInputAttributeDescription posDescription = attributeDescriptions.get(0);
        posDescription.binding(0);
        posDescription.location(0);
        posDescription.format(VK_FORMAT_R32G32B32_SFLOAT);
        posDescription.offset(OFFSETOF_POS);

        // Color
        VkVertexInputAttributeDescription colorDescription = attributeDescriptions.get(1);
        colorDescription.binding(0);
        colorDescription.location(1);
        colorDescription.format(VK_FORMAT_R32G32B32_SFLOAT);
        colorDescription.offset(OFFSETOF_COLOR);

        // Texture coordinates
        VkVertexInputAttributeDescription texCoordsDescription = attributeDescriptions.get(2);
        texCoordsDescription.binding(0);
        texCoordsDescription.location(2);
        texCoordsDescription.format(VK_FORMAT_R32G32_SFLOAT);
        texCoordsDescription.offset(OFFSETOF_TEXTCOORDS);

        return attributeDescriptions.rewind();
    }

    public static VkVertexInputBindingDescription.Buffer getBindingDescription(MemoryStack stack, Material.VertexLayout vertexLayout) {
        VkVertexInputBindingDescription.Buffer bindingDescription =
                VkVertexInputBindingDescription.calloc(1, stack);
        bindingDescription.binding(0);
        bindingDescription.stride(vertexLayout.sizeof);
        bindingDescription.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

        return bindingDescription;
    }

    public static VkVertexInputAttributeDescription.Buffer getAttributeDescriptions(MemoryStack stack, Material.VertexLayout vertexLayout) {
        VkVertexInputAttributeDescription.Buffer attributeDescriptions =
                VkVertexInputAttributeDescription.calloc(vertexLayout.locations.size(), stack);
        for (int i = 0; i < vertexLayout.locations.size(); i++) {
            Material.VertexLayout.BindingDescription bindingDescription = vertexLayout.locations.get(i);
            VkVertexInputAttributeDescription posDescription = attributeDescriptions.get(i);
            posDescription.binding(0);
            posDescription.location(i);
            if (bindingDescription.size == 3)
                posDescription.format(VK_FORMAT_R32G32B32_SFLOAT);
            else if (bindingDescription.size == 2)
                posDescription.format(VK_FORMAT_R32G32_SFLOAT);
            else
                throw new IllegalArgumentException("Unknown size: " + bindingDescription.size);

            posDescription.offset(vertexLayout.offsetOf(i));
        }
        return attributeDescriptions.rewind();
    }
}
