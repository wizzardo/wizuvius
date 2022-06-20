package com.wizzardo.vulkan.scene;

import com.wizzardo.vulkan.*;
import org.lwjgl.vulkan.VkDevice;

import java.util.List;

public class Geometry extends Spatial {
    protected Mesh mesh;
    protected Material material;
    protected UniformBuffers uniformBuffers;
    protected List<Long> descriptorSets;
    protected boolean prepared = false;

    public Geometry() {
    }

    public Geometry(Mesh mesh, Material material) {
        this(null, mesh, material);
    }

    public Geometry(String name, Mesh mesh, Material material) {
        this.name = name;
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
        if (mesh != null)
            mesh.cleanup(device);
    }

    public void cleanupSwapChainObjects(VkDevice device) {
        try {
            if (uniformBuffers != null)
                uniformBuffers.cleanup(device);
            if (material != null)
                material.cleanupSwapChainObjects(device);
            prepared = false;
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

        if (descriptorSets == null) {
            VulkanDescriptorSets.DescriptorSetsBuilder descriptorSetsBuilder = new VulkanDescriptorSets.DescriptorSetsBuilder(material.bindings)
                    .withUniformBuffers(uniformBuffers.uniformBuffers);

            descriptorSets = descriptorSetsBuilder.build(application.getDevice(), application.getSwapChainImages(), material.descriptorSetLayout, application.getDescriptorPool());
        }
        prepared = true;
    }

    public boolean isPrepared() {
        return prepared;
    }
}
