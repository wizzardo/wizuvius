package com.example;

import com.wizzardo.tools.io.IOTools;
import com.wizzardo.tools.misc.Unchecked;
import com.wizzardo.vulkan.*;
import com.wizzardo.vulkan.scene.Geometry;
import com.wizzardo.vulkan.scene.Spatial;
import com.wizzardo.vulkan.ui.javafx.JavaFxQuad;
import com.wizzardo.vulkan.ui.javafx.JavaFxToTextureBridge;
import com.wizzardo.vulkan.ui.javafx.WebViewSample;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;

import static org.lwjgl.assimp.Assimp.aiProcess_FlipUVs;
import static org.lwjgl.assimp.Assimp.aiProcess_JoinIdenticalVertices;

public class SampleApp extends DesktopVulkanApplication {

    public SampleApp() {
        width = 1280;
        height = 720;
    }

    public static void main(String[] args) {
        System.setProperty("joml.format", "false");
        JavaFxToTextureBridge.init();
        new SampleApp().run();
    }

    Material material;
    JavaFxQuad javaFxUI;

    @Override
    protected void initApp() {
        TextureImage textureImage = Unchecked.call(() -> createTextureImage("textures/viking_room.png"));

        material = new Material();
        material.setVertexShader("shaders/tri.vert.spv");
        material.setFragmentShader("shaders/tri.frag.spv");
        material.addTextureImage(textureImage);
        material.setTextureSampler(createTextureSampler(textureImage.mipLevels));


        {
            Spatial spatial = loadMesh();
            getMainViewport().getScene().attachChild(spatial);
//            Geometry geometry = new Geometry(mesh, material);
//            geometry.getLocalTransform().setScale(0.5f);
//            geometry.getLocalTransform().setTranslation(0, 0, 0.5f);
//            addGeometry(geometry, getMainViewport());

            allocationTrackingEnabled = false;
        }

        getMainViewport().getCamera().setLocation(new Vector3f(2, 2, 2));
        getMainViewport().getCamera().lookAt(Vectors.ZERO, Vectors.UNIT_Z);

        {
//            JavaFxToTextureBridge bridge = new JavaFxToTextureBridge(this, 400, 200);
            JavaFxToTextureBridge bridge = new JavaFxToTextureBridge(this, extentWidth, extentHeight);

            Platform.runLater(() -> {


//                Button button = new Button("BUTTON");
//                Group rootNode = new Group(button);
//                Scene scene = new Scene(rootNode, 400, 200);
//                scene.setFill(Color.TRANSPARENT);
//                scene.setFill(Color.GREEN);


//                WebViewSample.WebHolder webHolder = new WebViewSample.WebHolder("http://localhost:8080/", extentWidth, extentHeight, 1.5f);
//                WebViewSample.WebHolder webHolder = new WebViewSample.WebHolder("https://wizzardo.github.io/react-ui-basics/?path=/story/scrollable--story-1", width, height, "clientId");
//                WebViewSample.WebHolder webHolder = new WebViewSample.WebHolder("https://ya.ru", width, height, 2f);
                WebViewSample.WebHolder webHolder = new WebViewSample.WebHolder("https://www.playground.ru/misc/news/krasochnaya_vyzhivalka_under_a_rock_poluchila_novyj_trejler-1218256", width, height, 2f);
//            webHolder = new WebViewSample.WebHolder("http://localhost:9009/?path=/story/scrollable--story-1", width, height, clientId);
//            webHolder = new WebViewSample.WebHolder("http://localhost:9009/iframe.html?id=scrollable--story-1", width, height, clientId);

                var group = new Group(webHolder);
                var scene = new Scene(group, extentWidth, extentHeight);
                scene.setFill(Color.TRANSPARENT);

                bridge.setScene(scene);
            });

            javaFxUI = new JavaFxQuad(bridge);
//            javaFxUI.getLocalTransform().setTranslation(10, 10, 0);
//            javaFxUI.getLocalTransform().setTranslation(width/2, height/2, 0);
//            addGeometry(javaFxUI, getGuiViewport());

            javaFxUI.getMaterial().prepare(this, getGuiViewport());
            javaFxUI.getMesh().prepare(this, javaFxUI.getMaterial().getVertexLayout());
            javaFxUI.prepare(this);
            getGuiViewport().getScene().attachChild(javaFxUI);

//            javaFxUI.getLocalTransform().getScale().x = width * 2.f;
//            javaFxUI.getLocalTransform().getScale().y = height * 2.f;

//            Thread thread = new Thread(() -> {
//                int i =0;
//                while(true){
//                    Unchecked.ignore(() -> Thread.sleep(1000));
//                    i++;
//                    int finalI = i;
//                    Platform.runLater(() -> button.setText("BUTTON " + String.format("%03d", finalI)));
////                    return;
//                }
//            });
//            thread.setDaemon(true);
//            thread.start();

//            Material material = new Material();
//            material.setVertexShader("shaders/tri.vert.spv");
//            material.setFragmentShader("shaders/tri.frag.spv");
//            material.setTextureImage(textureImage);
//            material.setTextureSampler(createTextureSampler(textureImage.mipLevels));
//
//            Vertex[] vertices = {
////                    new Vertex(new Vector3f(-0.5f, -0.5f, 0f), new Vector3f(0.5f, 0.0f, 0.0f), new Vector2f(1.0f, 0.0f)),
////                    new Vertex(new Vector3f(0.5f, -0.5f, 0f), new Vector3f(0.0f, 0.5f, 0.0f), new Vector2f(0.0f, 0.0f)),
////                    new Vertex(new Vector3f(0.5f, 0.5f, 0f), new Vector3f(0.0f, 0.0f, 0.0f), new Vector2f(0.0f, 1.0f)),
////                    new Vertex(new Vector3f(-0.5f, 0.5f, 0f), new Vector3f(0.5f, 0.5f, 0.0f), new Vector2f(1.0f, 1.0f))
//                    new Vertex(new Vector3f(0, 0, 0f), new Vector3f(1, 0.0f, 0.0f), new Vector2f(1.0f, 0.0f)),
//                    new Vertex(new Vector3f(1, 0, 0f), new Vector3f(0.0f, 1, 0.0f), new Vector2f(0.0f, 0.0f)),
//                    new Vertex(new Vector3f(1, 1, 0f), new Vector3f(0.0f, 0.0f, 0.0f), new Vector2f(0.0f, 1.0f)),
//                    new Vertex(new Vector3f(0, 1, 0f), new Vector3f(1, 1, 0.0f), new Vector2f(1.0f, 1.0f))
//            };
//
//            int[] indices = {
//                    2, 1, 0,
//                    0, 3, 2
//            };
//
//            Geometry geometry = new Geometry(new Mesh(vertices, indices), material);
//            geometry.getLocalTransform().setScale(100f);
//            geometry.getLocalTransform().setTranslation(-100, -100, 0);
//            addGeometry(geometry, getGuiViewport());
        }

//        this.initInputsManager().addMouseMoveListener((x, y) -> {
//            System.out.println("mouse: "+x+" "+y);
//        });
    }

    @Override
    protected void simpleUpdate(double tpf) {
        for (Geometry geometry : getMainViewport().getGeometries()) {
            geometry.getLocalTransform().getRotation().setAngleAxis(this.getTime() * Math.toRadians(90) / 10f, 0.0f, 0.0f, 1.0f);
        }
        javaFxUI.update();
    }

    @Override
    protected void cleanupSwapChain() {
        super.cleanupSwapChain();
        material.cleanupSwapChainObjects(getDevice());
    }

    @Override
    protected void cleanup() {
        material.cleanup(getDevice());
        super.cleanup();
        JavaFxToTextureBridge.cleanup();
    }

    private Spatial loadMesh() {
        Spatial spatial = Unchecked.call(() -> {
            byte[] bytes = IOTools.bytes(loadAsset("models/viking_room.obj"));
            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            return ModelLoader.loadModel(buffer, aiProcess_FlipUVs | aiProcess_JoinIdenticalVertices, "viking_room.obj");
        });
        return spatial;
    }
}
