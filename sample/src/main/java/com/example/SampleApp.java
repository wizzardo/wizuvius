package com.example;

import com.wizzardo.tools.io.IOTools;
import com.wizzardo.tools.misc.Unchecked;
import com.wizzardo.vulkan.*;
import com.wizzardo.vulkan.scene.Geometry;
import com.wizzardo.vulkan.ui.javafx.JavaFxQuad;
import com.wizzardo.vulkan.ui.javafx.JavaFxToTextureBridge;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.nio.ByteBuffer;

import static org.lwjgl.assimp.Assimp.aiProcess_FlipUVs;
import static org.lwjgl.assimp.Assimp.aiProcess_JoinIdenticalVertices;

public class SampleApp extends DesktopVulkanApplication {

    public SampleApp() {
        width = 1280;
        height = 720;
    }

    public static void main(String[] args) {
        new SampleApp().start();
    }

    Material material;
    JavaFxQuad javaFxUI;

    @Override
    protected void initApp() {
        TextureImage textureImage = createTextureImage("textures/viking_room.png");

        material = new Material();
        material.setVertexShader("shaders/tri.vert.spv");
        material.setFragmentShader("shaders/tri.frag.spv");
        material.setTextureImage(textureImage);
        material.setTextureSampler(createTextureSampler(textureImage.mipLevels));

        Mesh mesh = loadMesh();

        {
            Geometry geometry = new Geometry(mesh, material);
//            geometry.getLocalTransform().setScale(0.5f);
//            geometry.getLocalTransform().setTranslation(0, 0, 0.5f);
            addGeometry(geometry, getMainViewport());
        }

        getMainViewport().getCamera().setLocation(new Vector3f(2, 2, 2));
        getMainViewport().getCamera().lookAt(Vectors.ZERO, Vectors.UNIT_Z);

        {

            JavaFxToTextureBridge bridge = new JavaFxToTextureBridge(this, 400, 200);

            Button button = new Button("BUTTON");
            Group rootNode = new Group(button);
            Scene scene = new Scene(rootNode, 400, 200);
            scene.setFill(Color.TRANSPARENT);
//            scene.setFill(Color.GREEN);

            bridge.setScene(scene);


            javaFxUI = new JavaFxQuad(bridge);
            addGeometry(javaFxUI, getGuiViewport());

            Thread thread = new Thread(() -> {
                int i =0;
                while(true){
                    Unchecked.ignore(() -> Thread.sleep(1000));
                    i++;
                    int finalI = i;
                    Platform.runLater(() -> button.setText("BUTTON " + String.format("%03d", finalI)));
//                    return;
                }
            });
            thread.setDaemon(true);
            thread.start();

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

    private Mesh loadMesh() {
        ModelLoader.Model model = Unchecked.call(() -> {
            byte[] bytes = IOTools.bytes(loadAsset("models/viking_room.obj"));
            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            return ModelLoader.loadModel(buffer, aiProcess_FlipUVs | aiProcess_JoinIdenticalVertices, "viking_room.obj");
        });

        Vertex[] vertices;
        int[] indices;
        int vertexCount = model.positions.size();
        vertices = new Vertex[vertexCount];
        Vector3fc color = new Vector3f(1.0f, 1.0f, 1.0f);

        for (int i = 0; i < vertexCount; i++) {
            vertices[i] = new Vertex(
                    model.positions.get(i),
                    color,
                    model.texCoords.get(i));
        }

        indices = new int[model.indices.size()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = model.indices.get(i);
        }

        return new Mesh(vertices, indices);
    }
}
