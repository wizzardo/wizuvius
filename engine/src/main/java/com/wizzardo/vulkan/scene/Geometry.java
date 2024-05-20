package com.wizzardo.vulkan.scene;

import com.wizzardo.vulkan.*;
import org.lwjgl.vulkan.VkDevice;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class Geometry extends Spatial {
    protected Mesh mesh;
    protected Material material;
    protected ModelViewProjectionUniformBuffers modelViewProjectionUniformBuffers;
    protected Map<Material, List<Long>> descriptorSetsByMaterial = new IdentityHashMap<>(8);
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

    public void cleanupSwapChainObjects(VkDevice device) {
        try {
//            if (modelViewProjectionUniformBuffers != null)
//                modelViewProjectionUniformBuffers.cleanup(device);
            if (material != null)
                material.cleanupSwapChainObjects(device);
            prepared = false;
        } finally {
            modelViewProjectionUniformBuffers = null;
            descriptorSetsByMaterial.clear();
        }
    }

    public ModelViewProjectionUniformBuffers getUniformBuffers() {
        return modelViewProjectionUniformBuffers;
    }

    public void setUniformBuffers(ModelViewProjectionUniformBuffers modelViewProjectionUniformBuffers) {
        this.modelViewProjectionUniformBuffers = modelViewProjectionUniformBuffers;
    }

    public long getDescriptorSet(Material material, int imageIndex) {
        List<Long> list = descriptorSetsByMaterial.get(material);
        if (list == null) {
            return 0;
        }
        return list.get(imageIndex);
    }

    public void prepare(VulkanApplication application) {
        if (modelViewProjectionUniformBuffers == null)
            modelViewProjectionUniformBuffers = ModelViewProjectionUniformBuffers.create(application, application.getSwapChainImages());

        prepareDescriptorSets(application, material);
        prepared = true;
    }

    public List<Long> prepareDescriptorSets(VulkanApplication application, Material material) {
        List<Long> descriptorSets;
        if ((descriptorSets = descriptorSetsByMaterial.get(material)) == null) {
            VulkanDescriptorSets.DescriptorSetsBuilder descriptorSetsBuilder = new VulkanDescriptorSets.DescriptorSetsBuilder(material.bindings)
                    .withUniformBuffers(modelViewProjectionUniformBuffers.uniformBuffers);

            descriptorSets = descriptorSetsBuilder.build(application.getDevice(), application.getSwapChainImages().size(), material.descriptorSetLayout, application.getDescriptorPool());
            descriptorSetsByMaterial.put(material, descriptorSets);
        }
        return descriptorSets;
    }

    public boolean isPrepared() {
        return prepared;
    }
}
