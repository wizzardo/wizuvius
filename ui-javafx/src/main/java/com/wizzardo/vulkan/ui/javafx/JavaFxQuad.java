package com.wizzardo.vulkan.ui.javafx;

import com.wizzardo.vulkan.*;
import com.wizzardo.vulkan.scene.Geometry;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.vulkan.VkDevice;

import java.util.List;

public class JavaFxQuad extends Geometry {

    protected static final Vertex[] vertices = {
            new Vertex(new Vector3f(0f, 0f, 0f), new Vector3f(1f, 0f, 0.0f), new Vector2f(0.0f, 0.0f)),
            new Vertex(new Vector3f(1f, 0f, 0f), new Vector3f(1f, 0f, 0.0f), new Vector2f(1.0f, 0.0f)),
            new Vertex(new Vector3f(1f, 1f, 0f), new Vector3f(1f, 0f, 0.0f), new Vector2f(1.0f, 1.0f)),
            new Vertex(new Vector3f(0f, 1f, 0f), new Vector3f(1f, 0f, 0.0f), new Vector2f(0.0f, 1.0f))
    };

    protected static final int[] indices = {
            2, 1, 0,
            0, 3, 2
    };

    protected JavaFxToTextureBridge bridge;
    protected JavaFxToTextureBridge.TextureHolder currentImage;

    public JavaFxQuad(JavaFxToTextureBridge bridge) {
        this.bridge = bridge;

        this.material = bridge.material;
        this.mesh = new Mesh(vertices, indices);
//        currentImage = bridge.getCurrentImage();
        getLocalTransform().getScale().set(bridge.textureWidth, bridge.textureHeight, 1);
    }

    public void update() {
        JavaFxToTextureBridge.TextureHolder currentImage = bridge.getCurrentImage();
        this.currentImage = currentImage;

        VulkanApplication application = bridge.application;
        application.getCurrentFrame().addFrameListener(() -> {
            bridge.release(currentImage);
        });

        if (currentImage.descriptorSets.isEmpty()) {
            currentImage.descriptorSets.addAll(VulkanDescriptorSets.createDescriptorSets(application.getDevice(),
                    application.getSwapChainImages(),
                    material.descriptorSetLayout,
                    application.getDescriptorPool(),
                    currentImage.textureImage.textureImageView,
                    material.getTextureSampler(),
                    uniformBuffers.uniformBuffers
            ));
        }
    }

    @Override
    public List<Long> getDescriptorSets() {
        return currentImage.descriptorSets;
    }

    @Override
    public void cleanupSwapChainObjects(VkDevice device) {
        super.cleanupSwapChainObjects(device);
        bridge.cleanupSwapChainObjects(device);
    }

    @Override
    public void prepare(VulkanApplication application) {
        if (uniformBuffers == null)
            uniformBuffers = UniformBuffers.createUniformBuffers(application.getPhysicalDevice(), application.getDevice(), application.getSwapChainImages());
    }
}
