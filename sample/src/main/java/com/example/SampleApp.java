package com.example;

import com.wizzardo.tools.io.IOTools;
import com.wizzardo.tools.misc.Unchecked;
import com.wizzardo.vulkan.*;
import com.wizzardo.vulkan.scene.Geometry;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.nio.ByteBuffer;

import static org.lwjgl.assimp.Assimp.aiProcess_FlipUVs;
import static org.lwjgl.assimp.Assimp.aiProcess_JoinIdenticalVertices;

public class SampleApp extends DesktopVulkanApplication {

    public static void main(String[] args) {
        new SampleApp().start();
    }

    Material material;

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
            Material material = new Material();
            material.setVertexShader("shaders/tri.vert.spv");
            material.setFragmentShader("shaders/tri.frag.spv");
            material.setTextureImage(textureImage);
            material.setTextureSampler(createTextureSampler(textureImage.mipLevels));

            Vertex[] vertices = {
                    new Vertex(new Vector3f(-0.5f, -0.5f, 0f), new Vector3f(1.0f, 0.0f, 0.0f), new Vector2f(1.0f, 0.0f)),
                    new Vertex(new Vector3f(0.5f, -0.5f, 0f), new Vector3f(0.0f, 1.0f, 0.0f), new Vector2f(0.0f, 0.0f)),
                    new Vertex(new Vector3f(0.5f, 0.5f, 0f), new Vector3f(0.0f, 0.0f, 0.0f), new Vector2f(0.0f, 1.0f)),
                    new Vertex(new Vector3f(-0.5f, 0.5f, 0f), new Vector3f(1.0f, 1.0f, 0.0f), new Vector2f(1.0f, 1.0f))
            };

            int[] indices = {
                    0, 1, 2,
                    2, 3, 0
            };

            Geometry geometry = new Geometry(new Mesh(vertices, indices), material);
            geometry.getLocalTransform().setScale(1f);
            geometry.getLocalTransform().setTranslation(-0.5f, -0.5f, -0.5f);
//            addGeometry(geometry, getGuiViewport());
        }
    }

    @Override
    protected void simpleUpdate(double tpf) {
        for (PreparedGeometry preparedGeometry : getMainViewport().getPreparedGeometries()) {
            preparedGeometry.geometry.getLocalTransform().getRotation().setAngleAxis(this.getTime() * Math.toRadians(90), 0.0f, 0.0f, 1.0f);
        }
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
