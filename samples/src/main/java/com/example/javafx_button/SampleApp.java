package com.example.javafx_button;

import com.example.AbstractSampleApp;
import com.wizzardo.vulkan.*;
import com.wizzardo.vulkan.material.predefined.UnshadedColor;
import com.wizzardo.vulkan.scene.Geometry;
import com.wizzardo.vulkan.scene.Spatial;
import com.wizzardo.vulkan.scene.shape.Box;
import com.wizzardo.vulkan.ui.javafx.JavaFxQuad;
import com.wizzardo.vulkan.ui.javafx.JavaFxToTextureBridge;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.joml.Vector3f;

public class SampleApp extends AbstractSampleApp {

    public static void main(String[] args) throws InterruptedException {
        JavaFxToTextureBridge.init();
        new SampleApp().start();
    }

    Spatial scene;
    JavaFxQuad javaFxUI;

    @Override
    protected void initApp() {
        int cameraOffset = 3;
        getMainViewport().getCamera().setLocation(new Vector3f(cameraOffset, cameraOffset, cameraOffset));
        getMainViewport().getCamera().lookAt(Vectors.ZERO, Vectors.UNIT_Z);

        {
            Material material = new UnshadedColor(new Vector3f(1, 1, 0));
            Geometry geometry = new Box(material);
            addGeometry(geometry, getMainViewport());
            scene = geometry;
        }

        {
            int uiWidth = 400;
            int uiHeight = 200;
            JavaFxToTextureBridge bridge = new JavaFxToTextureBridge(this, uiWidth, uiHeight, 2);
            Platform.runLater(() -> {
                StackPane root = new StackPane();
                root.setAlignment(Pos.TOP_LEFT);
                root.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));
//                root.setBackground(new Background(new BackgroundFill(Color.BLUE, CornerRadii.EMPTY, Insets.EMPTY)));

                Button button = new Button("BUTTON");
                root.getChildren().add(button);
                button.setOnMouseEntered(event -> Platform.runLater(() -> {
                    button.setBackground(new Background(new BackgroundFill(Color.GREEN, CornerRadii.EMPTY, Insets.EMPTY)));
                    System.out.println("enter - GREEN");
                }));
                button.setOnMouseExited(event -> Platform.runLater(() -> {
                    button.setBackground(new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY)));
                    System.out.println("exited - RED");
                }));
                button.setOnMousePressed(event -> Platform.runLater(() -> {
                    button.setBackground(new Background(new BackgroundFill(Color.BLUE, CornerRadii.EMPTY, Insets.EMPTY)));
                    System.out.println("pressed - BLUE");
                }));
                button.setOnMouseReleased(event -> Platform.runLater(() -> {
                    button.setBackground(new Background(new BackgroundFill(Color.YELLOW, CornerRadii.EMPTY, Insets.EMPTY)));
                    System.out.println("released - YELLOW");
                }));


                Scene scene = new Scene(root, uiWidth, uiHeight, Color.GREEN);
//                Scene scene = new Scene(root, uiWidth, uiHeight, Color.TRANSPARENT);
                bridge.setScene(scene);
                bridge.repaint();
            });

            javaFxUI = new JavaFxQuad(bridge);
            javaFxUI.getLocalTransform().setTranslation(50, 50, 0);
            addGeometry(javaFxUI, getGuiViewport());
            addAlteration(tpf -> {
                javaFxUI.update();
                return true;
            });
        }
    }

    @Override
    protected void simpleUpdate(double tpf) {
        scene.getLocalTransform().getRotation().setAngleAxis(this.getTime() * Math.toRadians(90) / 10f, 0.0f, 0.0f, 1.0f);
    }

    @Override
    protected void cleanup() {
        JavaFxToTextureBridge.cleanup(this);
        super.cleanup();
    }

}
