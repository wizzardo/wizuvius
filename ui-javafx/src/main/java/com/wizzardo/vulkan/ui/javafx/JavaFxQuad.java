package com.wizzardo.vulkan.ui.javafx;

import com.sun.javafx.embed.AbstractEvents;
import com.wizzardo.vulkan.*;
import com.wizzardo.vulkan.input.InputsManager;
import com.wizzardo.vulkan.scene.Geometry;
import javafx.application.Platform;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.vulkan.VkDevice;

import java.awt.event.KeyEvent;
import java.util.List;

import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_FRAGMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT;

public class JavaFxQuad extends Geometry {

    protected static final Vertex[] vertices = {
            new Vertex(new Vector3f(0f, 0f, 0f), new Vector4f(1f, 0f, 0.0f,1f), new Vector2f(0.0f, 0.0f)),
            new Vertex(new Vector3f(1f, 0f, 0f), new Vector4f(1f, 0f, 0.0f,1f), new Vector2f(1.0f, 0.0f)),
            new Vertex(new Vector3f(1f, 1f, 0f), new Vector4f(1f, 0f, 0.0f,1f), new Vector2f(1.0f, 1.0f)),
            new Vertex(new Vector3f(0f, 1f, 0f), new Vector4f(1f, 0f, 0.0f,1f), new Vector2f(0.0f, 1.0f))
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
        getLocalTransform().getScale().set(bridge.textureWidth, bridge.textureHeight, 1);

        Matrix4f tempMatrix = new Matrix4f();
        Vector3f mousePosition = new Vector3f();
        boolean[] isDragging = new boolean[1];
        InputsManager inputsManager = bridge.application.getInputsManager();
        float extentScale = bridge.application.getExtentWidth() * 1f / bridge.application.getWidth();

        inputsManager.addMouseMoveListener((x, y) -> {
            Camera camera = bridge.application.getGuiViewport().getCamera();
            camera.getWorldCoordinates((float) x, (float) y, 0f, mousePosition, tempMatrix);
            mousePosition.mul(extentScale).sub(getLocalTransform().getTranslation());

            if (isDragging[0] || isMouseOver(mousePosition.x,mousePosition.y)) {
                int localX = (int) mousePosition.x;
                int localY = (int) mousePosition.y;
                Platform.runLater(() -> bridge.onMouseMove(localX, localY, localX, localY));
            }
        });

        inputsManager.addMouseButtonListener((x, y, button, pressed) -> {
            Camera camera = bridge.application.getGuiViewport().getCamera();
            camera.getWorldCoordinates((float) x, (float) y, 0f, mousePosition, tempMatrix);
            mousePosition.mul(extentScale).sub(getLocalTransform().getTranslation());

            if (isMouseOver(mousePosition.x, mousePosition.y) || (isDragging[0] && !pressed)) {

                int type;
                if (pressed) {
                    type = AbstractEvents.MOUSEEVENT_PRESSED;
                    isDragging[0] = true;
                } else {
                    type = AbstractEvents.MOUSEEVENT_RELEASED;
                    isDragging[0] = false;
                }

                int btn = 0;

                if (button == 0) {
                    btn = AbstractEvents.MOUSEEVENT_PRIMARY_BUTTON;
                } else if (button == 1) {
                    btn = AbstractEvents.MOUSEEVENT_SECONDARY_BUTTON;
                } else if (button == 2) {
                    btn = AbstractEvents.MOUSEEVENT_MIDDLE_BUTTON;
                }

                int finalBtn = btn;
                int localX = (int) mousePosition.x;
                int localY = (int) mousePosition.y;
//                System.out.println("runLater.onMouseButtonEvent " + localX + " " + localY + " " + type + " " + button);
                Platform.runLater(() -> bridge.onMouseButtonEvent(localX, localY, localX, localY, type, finalBtn));
                return false;
            }
            return true;
        });


        inputsManager.addScrollListener((x, y, scrollX, scrollY) -> {
            Camera camera = bridge.application.getGuiViewport().getCamera();
            camera.getWorldCoordinates((float) x, (float) y, 0f, mousePosition, tempMatrix);
            mousePosition.sub(getLocalTransform().getTranslation());

            if (isMouseOver(mousePosition.x, mousePosition.y)) {
                int type;
                if (scrollY != 0)
                    type = AbstractEvents.MOUSEEVENT_VERTICAL_WHEEL;
                else
                    type = AbstractEvents.MOUSEEVENT_HORIZONTAL_WHEEL;

                int localX = (int) mousePosition.x;
                int localY = (int) mousePosition.y;
//                System.out.println("onscroll " + localX + "x" + localY + ": " + ((int) scrollX) + " " + ((int) scrollY));
                Platform.runLater(() -> bridge.onMouseScrollEvent(localX, localY, localX, localY, scrollX, scrollY, type));
                return false;
            }
            return true;
        });


        inputsManager.addKeyTypedListener((codepoint, chars) -> {
            Platform.runLater(() -> bridge.onKeyTyped(KeyEvent.getExtendedKeyCodeForChar(codepoint), chars));
            return true;
        });

        inputsManager.addKeyListener((key, pressed, repeat) -> {
            Platform.runLater(() -> bridge.onKey(key, pressed, repeat));
            return true;
        });

    }

    protected boolean isMouseOver(float x, float y) {
        Vector3f translation = getLocalTransform().getTranslation();
        if (x < 0 || x >  bridge.textureWidth )
            return false;
        if (y<0 || y >   bridge.textureHeight )
            return false;

        return bridge.isMouseOver((int) x, (int) y);
    }

    public void update() {
        JavaFxToTextureBridge.TextureHolder currentImage = bridge.getCurrentImage();
        if (currentImage == null)
            return;

        this.currentImage = currentImage;

        VulkanApplication application = bridge.application;
        application.getCurrentFrame().addFrameListener(currentImage.frameListener);

        if (currentImage.descriptorSets.isEmpty()) {

            VulkanDescriptorSets.DescriptorSetLayoutBuilder layoutBuilder = new VulkanDescriptorSets.DescriptorSetLayoutBuilder();
            layoutBuilder.append(new VulkanDescriptorSets.DescriptorSetLayoutBindingUBO(0, VK_SHADER_STAGE_VERTEX_BIT));
            layoutBuilder.append(new VulkanDescriptorSets.DescriptorSetLayoutBindingImageWithSampler(1, VK_SHADER_STAGE_FRAGMENT_BIT, currentImage.textureImage.getTextureImageView(), material.getTextureSampler()));

            VulkanDescriptorSets.DescriptorSetsBuilder descriptorSetsBuilder = new VulkanDescriptorSets.DescriptorSetsBuilder(layoutBuilder.bindings)
                    .withUniformBuffers(modelViewProjectionUniformBuffers.uniformBuffers);

            currentImage.descriptorSets.addAll(descriptorSetsBuilder.build(application.getDevice(), application.getSwapChainImages().size(), material.descriptorSetLayout, application.getDescriptorPool()));
        }
    }

    @Override
    public long getDescriptorSet(Material material, int imageIndex) {
        return currentImage.descriptorSets.get(imageIndex);
    }

    @Override
    public boolean isPrepared() {
        return prepared && currentImage != null;
    }

    @Override
    public void cleanupSwapChainObjects(VkDevice device) {
        super.cleanupSwapChainObjects(device);
        bridge.cleanupSwapChainObjects(device);
    }

    @Override
    public void prepare(VulkanApplication application) {
        if (modelViewProjectionUniformBuffers == null)
            modelViewProjectionUniformBuffers = ModelViewProjectionUniformBuffers.create(application.getPhysicalDevice(), application.getDevice(), application.getSwapChainImages());
        prepared = true;
    }
}
