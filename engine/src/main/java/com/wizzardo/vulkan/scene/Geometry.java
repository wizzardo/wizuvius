package com.wizzardo.vulkan.scene;

import com.wizzardo.vulkan.*;
import org.lwjgl.vulkan.VkDevice;

import java.util.List;

public class Geometry extends Spatial {
    protected Mesh mesh;
    protected Material material;
    protected UniformBuffers uniformBuffers;
    protected List<Long> descriptorSets;

    public Geometry() {
    }

    public Geometry(Mesh mesh, Material material) {
        this.mesh = mesh;
        this.material = material;
    }

    public Mesh getMesh() {
        return mesh;
    }

    public void setMesh(Mesh mesh) {
        this.mesh = mesh;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public void cleanup(VkDevice device) {
        mesh.cleanup(device);
    }

    public void cleanupSwapChainObjects(VkDevice device) {
        try {
            uniformBuffers.cleanup(device);
            material.cleanupSwapChainObjects(device);
        } finally {
            uniformBuffers = null;
            descriptorSets = null;
        }
    }

    public UniformBuffers getUniformBuffers() {
        return uniformBuffers;
    }

    public void setUniformBuffers(UniformBuffers uniformBuffers) {
        this.uniformBuffers = uniformBuffers;
    }

    public List<Long> getDescriptorSets() {
        return descriptorSets;
    }

    public long getDescriptorSet(int imageIndex) {
        return getDescriptorSets().get(imageIndex);
    }

    public void setDescriptorSets(List<Long> descriptorSets) {
        this.descriptorSets = descriptorSets;
    }

    public void prepare(VulkanApplication application) {
        if (uniformBuffers == null)
            uniformBuffers = UniformBuffers.createUniformBuffers(application.getPhysicalDevice(), application.getDevice(), application.getSwapChainImages());

        if (descriptorSets == null)
            descriptorSets = VulkanDescriptorSets.createDescriptorSets(application.getDevice(),
                    application.getSwapChainImages(),
                    material.descriptorSetLayout,
                    application.getDescriptorPool(),
                    material.getTextureImage().textureImageView,
                    material.getTextureSampler(),
                    uniformBuffers.uniformBuffers
            );
    }
}
