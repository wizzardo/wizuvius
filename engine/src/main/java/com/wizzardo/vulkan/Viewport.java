package com.wizzardo.vulkan;

import static com.wizzardo.vulkan.Matrices.EMPTY_MATRIX_4F;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.vkDestroyFramebuffer;
import static org.lwjgl.vulkan.VK10.vkDestroyRenderPass;
import static org.lwjgl.vulkan.VK10.vkFreeCommandBuffers;
import static org.lwjgl.vulkan.VK10.vkMapMemory;
import static org.lwjgl.vulkan.VK10.vkUnmapMemory;

import com.wizzardo.vulkan.scene.Geometry;
import com.wizzardo.vulkan.scene.Node;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
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
    }

    protected void cleanup(VkDevice device, long commandPool) {
        for (Geometry preparedGeometry : geometries) {
            preparedGeometry.cleanup(device);
        }

        vkFreeCommandBuffers(device, commandPool, Utils.asPointerBuffer(commandBuffers));
    }

    public void updateModelUniformBuffers(VulkanApplication app, int imageIndex) {
//        camera.view.setLookAt(camera.getLocation(), camera.getDirection(), Vectors.UNIT_Z);
        camera.updateViewMatrix();

        for (int i = 0; i < geometries.size(); i++) {
            Geometry geometry = geometries.get(i);
            UniformBuffers uniformBuffers = geometry.getUniformBuffers();
            updateModelUniformBuffer(app, uniformBuffers, imageIndex, geometry.getLocalTransform());
        }
    }

    protected void updateModelUniformBuffer(VulkanApplication app, UniformBuffers uniformBuffers, int index, Transform transform) {
        try (MemoryStack stack = stackPush()) {
            UniformBufferObject ubo = uniformBuffers.uniformBufferObject;
            ubo.model.set(EMPTY_MATRIX_4F);
            ubo.model.translate(transform.getTranslation());
            ubo.model.scale(transform.getScale());
            ubo.model.rotate(transform.getRotation());
            ubo.view.set(camera.view);
            ubo.proj.set(camera.projection);

            long memoryAddress = uniformBuffers.uniformBuffersMemory.get(index);

            // allocates 48 bytes
            PointerBuffer data = stack.mallocPointer(1); // todo reuse one buffer for all geometries in the list
            vkMapMemory(app.getDevice(), memoryAddress, 0, UniformBufferObject.SIZEOF, 0, data);

            // allocates 64 bytes
            ByteBuffer byteBuffer = data.getByteBuffer(0, UniformBufferObject.SIZEOF); // todo reuse one buffer for all geometries in the list
            Utils.memcpy(byteBuffer, ubo);
            vkUnmapMemory(app.getDevice(), memoryAddress);
        }
    }
}
