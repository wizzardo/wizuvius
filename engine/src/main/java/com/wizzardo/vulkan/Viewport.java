package com.wizzardo.vulkan;

import static org.lwjgl.vulkan.VK10.vkDestroyFramebuffer;
import static org.lwjgl.vulkan.VK10.vkDestroyRenderPass;
import static org.lwjgl.vulkan.VK10.vkFreeCommandBuffers;
import static org.lwjgl.vulkan.VK10.vkMapMemory;
import static org.lwjgl.vulkan.VK10.vkUnmapMemory;

import com.wizzardo.vulkan.scene.Geometry;
import com.wizzardo.vulkan.scene.Node;

import com.wizzardo.vulkan.scene.Spatial;
import org.joml.Matrix4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryHelpers;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkOffset2D;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Viewport {

    protected Camera camera = new Camera();
    protected Node scene = new Node();
    protected long renderPass;
    protected List<Long> swapChainFramebuffers;
    protected List<VkCommandBuffer> commandBuffers;
    protected List<Geometry> geometries = new ArrayList<>();
    protected VkExtent2D extent;
    protected VkOffset2D offset = VkOffset2D.create().set(0, 0);
    protected ByteBuffer byteBufferPointer = MemoryUtil.memByteBufferSafe(-1, 0);
    protected int colorAttachmentsCount = 1;
    protected boolean colorBlendingEnabled = true;

    public Camera getCamera() {
        return camera;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public Node getScene() {
        return scene;
    }

    public void setScene(Node scene) {
        this.scene = scene;
    }

    public int getColorAttachmentsCount() {
        return colorAttachmentsCount;
    }

    public void setColorAttachmentsCount(int colorAttachmentsCount) {
        this.colorAttachmentsCount = colorAttachmentsCount;
    }

    public boolean isColorBlendingEnabled() {
        return colorBlendingEnabled;
    }

    public void setColorBlendingEnabled(boolean colorBlendingEnabled) {
        this.colorBlendingEnabled = colorBlendingEnabled;
    }

    public long getRenderPass() {
        return renderPass;
    }

    public void setRenderPass(long renderPass) {
        this.renderPass = renderPass;
    }

    public List<Long> getSwapChainFramebuffers() {
        return swapChainFramebuffers;
    }

    public void setSwapChainFramebuffers(List<Long> swapChainFramebuffers) {
        this.swapChainFramebuffers = swapChainFramebuffers;
    }

    public List<VkCommandBuffer> getCommandBuffers() {
        return commandBuffers;
    }

    public void setCommandBuffers(List<VkCommandBuffer> commandBuffers) {
        this.commandBuffers = commandBuffers;
    }

    public List<Geometry> getGeometries() {
        return geometries;
    }

    public void setGeometries(List<Geometry> geometries) {
        this.geometries = geometries;
    }

    public VkExtent2D getExtent() {
        return extent;
    }

    public void setExtent(VkExtent2D extent) {
        this.extent = extent;
        camera.screenWidth = extent.width();
        camera.screenHeight = extent.height();
    }

    public VkOffset2D getOffset() {
        return offset;
    }

    public void setOffset(VkOffset2D offset) {
        this.offset = offset;
    }

    protected void cleanupSwapChain(VkDevice device) {
        swapChainFramebuffers.forEach(framebuffer -> vkDestroyFramebuffer(device, framebuffer, null));
        vkDestroyRenderPass(device, renderPass, null);

        for (Geometry preparedGeometry : geometries) {
            preparedGeometry.cleanupSwapChainObjects(device);
        }

        cleanupSwapChain(device, scene);
    }

    protected void cleanupSwapChain(VkDevice device, Node node) {
        List<Spatial> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Spatial spatial = children.get(i);
            if (spatial instanceof Geometry) {
                ((Geometry) spatial).cleanupSwapChainObjects(device);
            } else if (spatial instanceof Node) {
                cleanupSwapChain(device, (Node) spatial);
            }
        }
    }

    protected void cleanup(VkDevice device, long commandPool) {
        for (Geometry preparedGeometry : geometries) {
            preparedGeometry.cleanup(device);
        }

        cleanup(device, scene);

        vkFreeCommandBuffers(device, commandPool, Utils.asPointerBuffer(commandBuffers));
    }


    protected void cleanup(VkDevice device, Node node) {
        List<Spatial> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Spatial spatial = children.get(i);
            if (spatial instanceof Geometry) {
                ((Geometry) spatial).cleanup(device);
            } else if (spatial instanceof Node) {
                cleanup(device, (Node) spatial);
            }
        }
    }

    public void updateModelUniformBuffers(VulkanApplication app, int imageIndex, PointerBuffer data) {
//        if (geometries.isEmpty())
//            return;

        camera.updateViewMatrix();


        for (int i = 0; i < geometries.size(); i++) {
            Geometry geometry = geometries.get(i);
            updateModelUniformBuffer(app, imageIndex, geometry, data, byteBufferPointer);
        }

        updateModelUniformBuffers(app, imageIndex, data, scene);
    }

    protected void updateModelUniformBuffers(VulkanApplication app, int imageIndex, PointerBuffer data, Node node) {
        List<Spatial> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Spatial spatial = children.get(i);
            if (spatial instanceof Geometry) {
                updateModelUniformBuffer(app, imageIndex, (Geometry) spatial, data, byteBufferPointer);
            } else if (spatial instanceof Node) {
                updateModelUniformBuffers(app, imageIndex, data, (Node) spatial);
            }
        }
    }

    protected void updateFromParent(Spatial parent, Matrix4f model) {
        if (parent == null)
            return;

        updateFromParent(parent.getParent(), model);

        Transform transform = parent.getLocalTransform();
        model.translate(transform.getTranslation());
        model.rotate(transform.getRotation());
        model.scale(transform.getScale());

    }

    protected void updateModelUniformBuffer(
            VulkanApplication app,
            int index,
            Geometry geometry,
            PointerBuffer data,
            ByteBuffer byteBuffer
    ) {
        if (!geometry.isPrepared()) {
            geometry.getMaterial().prepare(app, this);
            geometry.getMesh().prepare(app, geometry.getMaterial().vertexLayout);
            geometry.prepare(app);
        }

        Transform transform = geometry.getLocalTransform();
        UniformBuffers uniformBuffers = geometry.getUniformBuffers();
        UniformBufferObject ubo = uniformBuffers.uniformBufferObject;
        Matrix4f model = ubo.model;
        model.identity();

        updateFromParent(geometry.getParent(), model);
        model.translate(transform.getTranslation());
        model.rotate(transform.getRotation());
        model.scale(transform.getScale());

        long memoryAddress = uniformBuffers.uniformBuffersMemory.get(index);

        vkMapMemory(app.getDevice(), memoryAddress, 0, UniformBufferObject.SIZEOF, 0, data);

        MemoryHelpers.remapByteBuffer(byteBuffer, data.get(0), UniformBufferObject.SIZEOF);
        Utils.memcpy(byteBuffer, model, camera.view, camera.projection);
        vkUnmapMemory(app.getDevice(), memoryAddress);
    }
}
