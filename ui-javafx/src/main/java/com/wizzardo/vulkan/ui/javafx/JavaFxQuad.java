package com.wizzardo.vulkan.ui.javafx;

import com.sun.javafx.embed.AbstractEvents;
import com.wizzardo.vulkan.*;
import com.wizzardo.vulkan.input.InputsManager;
import com.wizzardo.vulkan.scene.Geometry;
import javafx.application.Platform;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.vulkan.VkDevice;

import java.awt.event.KeyEvent;
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
        getLocalTransform().getScale().set(bridge.textureWidth, bridge.textureHeight, 1);

        Vector3f mousePosition = new Vector3f();
        Vector3f local = new Vector3f();
        InputsManager inputsManager = bridge.application.getInputsManager();
        inputsManager.addMouseMoveListener((x, y) -> {
            Camera camera = bridge.application.getGuiViewport().getCamera();
            camera.getWorldCoordinates((float) x, (float) y, 0f, mousePosition);

            boolean mouseOver = isMouseOver(mousePosition);
            if (mouseOver) {
                Vector3f translation = getLocalTransform().getTranslation();
                local.set(mousePosition).sub(translation);
                Platform.runLater(() -> bridge.onMouseMove((int) local.x, (int) local.y, (int) x, (int) y));
            }
        });

        inputsManager.addMouseButtonListener((x, y, button, pressed) -> {
            Camera camera = bridge.application.getGuiViewport().getCamera();
            camera.getWorldCoordinates((float) x, (float) y, 0f, mousePosition);

            boolean mouseOver = isMouseOver(mousePosition);
            if (mouseOver) {
                Vector3f translation = getLocalTransform().getTranslation();
                local.set(mousePosition).sub(translation);

                int type;
                if (pressed) {
                    type = AbstractEvents.MOUSEEVENT_PRESSED;
                } else {
                    type = AbstractEvents.MOUSEEVENT_RELEASED;
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
                Platform.runLater(() -> bridge.onMouseButtonEvent((int) local.x, (int) local.y, (int) x, (int) y, type, finalBtn));
            }
        });


        inputsManager.addScrollListener((x, y, scrollX, scrollY) -> {
            Camera camera = bridge.application.getGuiViewport().getCamera();
            camera.getWorldCoordinates((float) x, (float) y, 0f, mousePosition);

            boolean mouseOver = isMouseOver(mousePosition);
            if (mouseOver) {
                Vector3f translation = getLocalTransform().getTranslation();
                local.set(mousePosition).sub(translation);
                int type;
                if (scrollY != 0)
                    type = AbstractEvents.MOUSEEVENT_VERTICAL_WHEEL;
                else
                    type = AbstractEvents.MOUSEEVENT_HORIZONTAL_WHEEL;

//                System.out.println("onscroll " + x + "x" + y + ": " + ((int) scrollX) + " " + ((int) scrollY));
                Platform.runLater(() -> bridge.onMouseScrollEvent((int) local.x, (int) local.y, (int) x, (int) y, scrollX, scrollY, type));
            }
        });


        inputsManager.addKeyTypedListener((codepoint, chars) -> {
            Platform.runLater(() -> bridge.onKeyTyped(KeyEvent.getExtendedKeyCodeForChar(codepoint), chars));
        });

        inputsManager.addKeyListener((key, pressed, repeat) -> {
            Platform.runLater(() -> bridge.onKey(key, pressed, repeat));
        });

    }


    protected boolean isMouseOver(Vector3f worldPosition) {
        Vector3f translation = getLocalTransform().getTranslation();
        if (worldPosition.x < translation.x || worldPosition.x > translation.x + bridge.textureWidth)
            return false;
        if (worldPosition.y < translation.y || worldPosition.y > translation.y + bridge.textureHeight)
            return false;

        return bridge.isMouseOver((int) (worldPosition.x - translation.x), (int) (worldPosition.y - translation.y));
    }

    public void update() {
        JavaFxToTextureBridge.TextureHolder currentImage = bridge.getCurrentImage();
        this.currentImage = currentImage;

        VulkanApplication application = bridge.application;
        application.getCurrentFrame().addFrameListener(() -> {
            bridge.release(currentImage);
        });

        if (currentImage.descriptorSets.isEmpty()) {
            currentImage.descriptorSets.addAll(VulkanDescriptorSets.createDescriptorSets(
                    application.getDevice(),
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
