package com.wizzardo.vulkan;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.vkDestroyFramebuffer;
import static org.lwjgl.vulkan.VK10.vkDestroyRenderPass;
import static org.lwjgl.vulkan.VK10.vkFreeCommandBuffers;
import static org.lwjgl.vulkan.VK10.vkMapMemory;
import static org.lwjgl.vulkan.VK10.vkUnmapMemory;

import com.wizzardo.vulkan.scene.Node;

import org.joml.Matrix4d;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkOffset2D;

import java.util.ArrayList;
import java.util.List;

public class Viewport {

    protected Camera camera = new Camera();
    protected Node scene = new Node();
    protected long renderPass;
    protected List<Long> swapChainFramebuffers;
    protected List<VkCommandBuffer> commandBuffers;
    protected List<PreparedGeometry> preparedGeometries = new ArrayList<>();
    protected VkExtent2D extent;
    protected VkOffset2D offset = VkOffset2D.create().set(0, 0);

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

    public List<PreparedGeometry> getPreparedGeometries() {
        return preparedGeometries;
    }

    public void setPreparedGeometries(List<PreparedGeometry> preparedGeometries) {
        this.preparedGeometries = preparedGeometries;
    }

    public VkExtent2D getExtent() {
        return extent;
    }

    public void setExtent(VkExtent2D extent) {
        this.extent = extent;
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

        for (PreparedGeometry preparedGeometry : preparedGeometries) {
            preparedGeometry.cleanupSwapChainObjects(device);
        }
    }

    protected void cleanup(VkDevice device, long commandPool) {
        for (PreparedGeometry preparedGeometry : preparedGeometries) {
            preparedGeometry.cleanup(device);
        }

        vkFreeCommandBuffers(device, commandPool, Utils.asPointerBuffer(commandBuffers));
    }

    public void updateModelUniformBuffers(VulkanApplication app, int imageIndex) {
//        camera.view.setLookAt(camera.getLocation(), camera.getDirection(), Vectors.UNIT_Z);
        Camera.fromFrame(camera.getLocation(), camera.getDirection(), camera.getUp(), camera.view);

        for (int i = 0; i < preparedGeometries.size(); i++) {
            PreparedGeometry preparedGeometry = preparedGeometries.get(i);
            long uniformAddress = preparedGeometry.uniformBuffers.uniformBuffersMemory.get(imageIndex);
            updateModelUniformBuffer(app, uniformAddress, preparedGeometry.geometry.getLocalTransform());
        }
    }

    protected void updateModelUniformBuffer(VulkanApplication app, long memoryAddress, Transform transform) {
        try (MemoryStack stack = stackPush()) {
            UniformBufferObject ubo = new UniformBufferObject();

            ubo.model.translate(transform.getTranslation());
            ubo.model.scale(transform.getScale());
            ubo.model.rotate(transform.getRotation());
            ubo.view.set(camera.view);
            ubo.proj.set(camera.projection);

//            ubo.view.lookAt(2.0f, 2.0f, 2.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f);
//            ubo.proj.perspective((float) Math.toRadians(45),
//                    (float) extent.width() / (float) extent.height(), 0.1f, 10.0f);
//            ubo.proj.m11(ubo.proj.m11() * -1);

            PointerBuffer data = stack.mallocPointer(1);
            vkMapMemory(app.getDevice(), memoryAddress, 0, UniformBufferObject.SIZEOF, 0, data);
            Utils.memcpy(data.getByteBuffer(0, UniformBufferObject.SIZEOF), ubo);
            vkUnmapMemory(app.getDevice(), memoryAddress);
        }
    }
}
