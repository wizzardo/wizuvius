package com.example.mips;

import com.example.AbstractSampleApp;
import com.wizzardo.tools.misc.Unchecked;
import com.wizzardo.vulkan.*;
import com.wizzardo.vulkan.material.Uniform;
import com.wizzardo.vulkan.scene.Geometry;
import com.wizzardo.vulkan.scene.shape.Box;
import com.wizzardo.vulkan.ui.javafx.JavaFxQuad;
import com.wizzardo.vulkan.ui.javafx.JavaFxToTextureBridge;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.joml.Vector3f;

import static org.lwjgl.vulkan.VK10.*;

public class SampleApp extends AbstractSampleApp {

    public static void main(String[] args) {
        JavaFxToTextureBridge.init();
        new SampleApp().start();
    }

    JavaFxQuad javaFxUI;

    @Override
    protected void initApp() {
        getMainViewport().getCamera().setLocation(new Vector3f(2, 2, 2));
        getMainViewport().getCamera().lookAt(Vectors.ZERO, Vectors.UNIT_Z);

        TextureImage textureImage = Unchecked.call(() -> createTextureImage(folder + "/textures/colored_glass_rgba.ktx2"));

        Uniform.Float lodBiasUniform = new Uniform.Float(this, VK_SHADER_STAGE_FRAGMENT_BIT, 2, 0);

        {
            Material material = new Material() {
                {
                    withUBO = true;
                    fragmentShader = folder + "/shaders/texture.frag";
                    vertexShader = folder + "/shaders/texture.vert";
                    vertexLayout = new VertexLayout(
                            VertexLayout.BindingDescription.POSITION,
                            VertexLayout.BindingDescription.TEXTURE_COORDINATES,
                            VertexLayout.BindingDescription.NORMAL
                    );

                    addTextureImage(textureImage);
                    setTextureSampler(createTextureSampler(textureImage.getMipLevels()));
                    addUniform(lodBiasUniform);
                }
            };

            Geometry geometry = new Box(material);
            addGeometry(geometry, getMainViewport());
        }
        {
            int uiWidth = 400;
            int uiHeight = 150;
            JavaFxToTextureBridge bridge = new JavaFxToTextureBridge(this, uiWidth, uiHeight, 2);
            Platform.runLater(() -> {
                FlowPane root = new FlowPane(Orientation.VERTICAL);
                root.setPadding(new Insets(5));
                root.setAlignment(Pos.TOP_LEFT);
                root.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));

                Label label = new Label("LOD bias");
                root.getChildren().add(label);

                Slider slider = new Slider(0, textureImage.getMipLevels(), 0);
                root.getChildren().add(slider);
                slider.setBlockIncrement(1);
                slider.setSnapToTicks(true);
                slider.setShowTickLabels(true);
                slider.setShowTickMarks(true);
                slider.setMajorTickUnit(1);
                slider.setMinorTickCount(0);
                slider.valueProperty().addListener((observable, oldValue, newValue) -> {
                    int n = newValue.intValue();
                    int o = oldValue.intValue();
                    if (o != n) {
                        lodBiasUniform.set(n);
                    }
                });

                Scene scene = new Scene(root, uiWidth, uiHeight);
//                Scene scene = new Scene(root, uiWidth, uiHeight, Color.TRANSPARENT);
                bridge.setScene(scene);
                bridge.repaint();
            });

            javaFxUI = new JavaFxQuad(bridge);
            javaFxUI.getLocalTransform().setTranslation(50, 50, 0);
            addGeometry(javaFxUI, getGuiViewport());
        }
    }

    @Override
    protected void simpleUpdate(double tpf) {
        javaFxUI.update();
    }

    @Override
    protected void cleanup() {
        JavaFxToTextureBridge.cleanup(this);
        super.cleanup();
    }

}
