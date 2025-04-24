package com.wizzardo.vulkan;

import org.joml.Vector2fc;
import org.joml.Vector3fc;
import org.joml.Vector4fc;
import org.joml.Vector4ic;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import static org.lwjgl.vulkan.VK10.*;

public class Vertex {

    public final Vector3fc pos;
    public final Vector4fc color;
    public final Vector2fc texCoords;
    public final Vector3fc normal;
    public final Vector3fc tangent;
    public final Vector4fc boneWeights;
    public final Vector4ic boneIndexes;

    public Vertex(Vector3fc pos, Vector4fc color, Vector2fc texCoords, Vector3fc normal) {
        this(pos, color, texCoords, normal, null, null, null);
    }

    public Vertex(Vector3fc pos, Vector4fc color, Vector2fc texCoords, Vector3fc normal, Vector3fc tangent, Vector4fc boneWeights, Vector4ic boneIndexes) {
        this.pos = pos;
        this.color = color;
        this.texCoords = texCoords;
        this.normal = normal;
        this.tangent = tangent;
        this.boneWeights = boneWeights;
        this.boneIndexes = boneIndexes;
    }

    public Vertex(Vector3fc pos, Vector4fc color, Vector2fc texCoords) {
        this(pos, color, texCoords, null);
    }

    public static VkVertexInputBindingDescription.Buffer getBindingDescription(MemoryStack stack, Material.VertexLayout vertexLayout, Material.VertexLayout instanceBindingLayout) {
        int capacity = instanceBindingLayout.sizeof == 0 ? 1 : 2;
        VkVertexInputBindingDescription.Buffer bindingDescription = VkVertexInputBindingDescription.calloc(capacity, stack);
        VkVertexInputBindingDescription bd = bindingDescription.get(0);
        bd.binding(0);
        bd.stride(vertexLayout.sizeof);
        bd.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

        if (instanceBindingLayout.sizeof != 0) {
            bd = bindingDescription.get(1);
            bd.binding(1);
            bd.stride(vertexLayout.sizeof);
            bd.inputRate(VK_VERTEX_INPUT_RATE_INSTANCE);
        }
        return bindingDescription;
    }

    public static VkVertexInputAttributeDescription.Buffer getAttributeDescriptions(MemoryStack stack, Material.VertexLayout vertexLayout, Material.VertexLayout instanceBindingLayout) {
        int capacity = vertexLayout.locations.size() + instanceBindingLayout.locations.size();
        VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.calloc(capacity, stack);
        for (int i = 0; i < vertexLayout.locations.size(); i++) {
            Material.VertexLayout.BindingDescription bindingDescription = vertexLayout.locations.get(i);
            VkVertexInputAttributeDescription posDescription = attributeDescriptions.get(i);
            posDescription.binding(0);
            posDescription.location(i);
            posDescription.format(bindingDescription.format);
            posDescription.offset(vertexLayout.offsetOf(i));
        }
        for (int i = 0; i < instanceBindingLayout.locations.size(); i++) {
            Material.VertexLayout.BindingDescription bindingDescription = instanceBindingLayout.locations.get(i);
            VkVertexInputAttributeDescription posDescription = attributeDescriptions.get(i + vertexLayout.locations.size());
            posDescription.binding(1);
            posDescription.location(i + vertexLayout.locations.size());
            posDescription.format(bindingDescription.format);
            posDescription.offset(instanceBindingLayout.offsetOf(i));
        }
        return attributeDescriptions;
    }
}
