package com.example.instancing;

import com.example.AbstractSampleApp;
import com.wizzardo.tools.misc.Unchecked;
import com.wizzardo.vulkan.*;
import com.wizzardo.vulkan.alterations.OrbitCameraAlteration;
import com.wizzardo.vulkan.material.Uniform;
import com.wizzardo.vulkan.material.predefined.UnshadedColor;
import com.wizzardo.vulkan.material.predefined.UnshadedTexture;
import com.wizzardo.vulkan.scene.Geometry;
import com.wizzardo.vulkan.scene.Spatial;
import com.wizzardo.vulkan.scene.shape.Box;
import org.joml.*;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.lang.Math;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.vulkan.VK10.*;

public class SampleApp extends AbstractSampleApp {

    public static void main(String[] args) {
        new SampleApp().start();
    }

    class Background extends Mesh {
        public Background() {
            super(new Vertex[]{
                    new Vertex(new Vector3f(0), new Vector4f(0), new Vector2f(0, 0)),
                    new Vertex(new Vector3f(0), new Vector4f(0), new Vector2f(0, 0)),
                    new Vertex(new Vector3f(0), new Vector4f(0), new Vector2f(0, 0)),
            }, new int[1]);
        }

        @Override
        public void draw(VkCommandBuffer commandBuffer, Material material, CommandBufferTempData tempData) {
            vkCmdDraw(commandBuffer, 3, 1, 0, 0);
        }
    }

    @Override
    protected void initApp() {
        getMainViewport().getCamera().setLocation(new Vector3f(8.25f, -2.775f, -27.75f));
        getMainViewport().getCamera().lookAt(Vectors.ZERO, Vectors.UNIT_Y);
        {
            Material backgroundMaterial = new Material() {
                @Override
                protected DepthStencilStateOptions createDepthStencilStateOptions() {
                    DepthStencilStateOptions options = super.createDepthStencilStateOptions();
                    options.depthWriteEnable = false;
                    return options;
                }
            };
            backgroundMaterial.setFragmentShader(folder + "/shaders/starfield.frag");
            backgroundMaterial.setVertexShader(folder + "/shaders/starfield.vert");
            Background background = new Background();
            addGeometry(new Geometry(background, backgroundMaterial), getMainViewport());
        }
        Quaternionf light = new Quaternionf(-5.0f, 0.0f, 1.0f, 0.0f);
        float[] speed = new float[]{0, 0};
        {
            TextureImage textureImage = Unchecked.call(() -> createTextureImage(folder + "/textures/lavaplanet_rgba.ktx2"));
            Material material = new Material() {
                {
                    vertexLayout = new VertexLayout(
                            VertexLayout.BindingDescription.POSITION,
                            VertexLayout.BindingDescription.NORMAL,
                            VertexLayout.BindingDescription.TEXTURE_COORDINATES
                    );
                    addTextureImage(textureImage);
                    setTextureSampler(SampleApp.this.createTextureSampler(textureImage.getMipLevels()));

                    addUniform(new Uniform.Vec4(SampleApp.this, VK_SHADER_STAGE_VERTEX_BIT, 1, light));

                    setFragmentShader(folder + "/shaders/planet.frag");
                    setVertexShader(folder + "/shaders/planet.vert");
                }
            };

            Spatial scene = Unchecked.call(() -> ModelLoader.loadModel(
                    folder + "/models/lavaplanet.gltf", aiProcess_JoinIdenticalVertices | aiProcess_PreTransformVertices, this::loadAssetAsByteBuffer
            ));

            Geometry planet = scene.geometries().findFirst().get();
            planet.setMaterial(material);

            addGeometry(planet, getMainViewport());
        }
        {

            TextureImage textureImage = Unchecked.call(() -> createTextureImage(folder + "/textures/texturearray_rocks_rgba.ktx2"));
            int numberOfLayers = 5; // todo: put this info into TextureImage

            Uniform lightSpeed = new Uniform(SampleApp.this, VK_SHADER_STAGE_VERTEX_BIT, 6 * 4, 1) {
                @Override
                protected void write(ByteBuffer byteBuffer) {
                    Material.VertexLayout.BindingDescription.F4.put(byteBuffer, light);
                    byteBuffer.putFloat(speed[0]);
                    byteBuffer.putFloat(speed[1]);
                }
            };

            Material material = new Material() {
                {
                    vertexLayout = new VertexLayout(
                            VertexLayout.BindingDescription.POSITION,
                            VertexLayout.BindingDescription.NORMAL,
                            VertexLayout.BindingDescription.TEXTURE_COORDINATES
                    );
                    instanceBindingLayout = new VertexLayout(
                            VertexLayout.BindingDescription.POSITION,
                            VertexLayout.BindingDescription.F3, // rotation
                            VertexLayout.BindingDescription.F1, // scale
                            VertexLayout.BindingDescription.I1 // texture index
                    );
                    addTextureImage(textureImage);
                    setTextureSampler(SampleApp.this.createTextureSampler(textureImage.getMipLevels()));

                    addUniform(lightSpeed);

                    setFragmentShader(folder + "/shaders/instancing.frag");
                    setVertexShader(folder + "/shaders/instancing.vert");
                }
            };

            Spatial scene = Unchecked.call(() -> ModelLoader.loadModel(
                    folder + "/models/rock01.gltf", aiProcess_JoinIdenticalVertices | aiProcess_PreTransformVertices, this::loadAssetAsByteBuffer
            ));

            Geometry rock = scene.geometries().findFirst().get();

            int n = 1024 * 8;

            var position = Material.VertexLayout.BindingDescription.F3;
            var rotation = Material.VertexLayout.BindingDescription.F3;
            var scale = Material.VertexLayout.BindingDescription.F1;
            var textureIndex = Material.VertexLayout.BindingDescription.I1;

            int instanceDataSize = position.size + rotation.size + scale.size + textureIndex.size;
            BufferHolder instanceBuffer = Utils.createVertexBuffer(physicalDevice, device, transferQueue, transferCommandPool, n, instanceDataSize, buffer -> {
                Vector2d ring0 = new Vector2d(7.0f, 11.0f);
                Vector2d ring1 = new Vector2d(14.0f, 18.0f);
                Vector3f vec3f = new Vector3f();
                ThreadLocalRandom random = ThreadLocalRandom.current();
                for (int i = 0; i < n / 2; i++) {
                    double rho, theta;
                    // Inner ring
                    rho = Math.sqrt((Math.pow(ring0.y, 2.0f) - Math.pow(ring0.x, 2.0f)) * random.nextDouble() + Math.pow(ring0.x, 2.0f));
                    theta = (float) (2.0f * Math.PI * random.nextDouble());
                    position.put(buffer, vec3f.set(rho * Math.cos(theta), random.nextDouble() * 0.5f - 0.25f, rho * Math.sin(theta)));
                    rotation.put(buffer, vec3f.set(Math.PI * random.nextDouble(), Math.PI * random.nextDouble(), Math.PI * random.nextDouble()));
                    scale.put(buffer, (float) (1.5f + random.nextDouble() - random.nextDouble()) * 0.75f);
                    textureIndex.put(buffer, random.nextInt(numberOfLayers));

                    // Outer ring
                    rho = Math.sqrt((Math.pow(ring1.y, 2.0f) - Math.pow(ring1.x, 2.0f)) * random.nextDouble() + Math.pow(ring1.x, 2.0f));
                    theta = (float) (2.0f * Math.PI * random.nextDouble());
                    position.put(buffer, vec3f.set(rho * Math.cos(theta), random.nextDouble() * 0.5f - 0.25f, rho * Math.sin(theta)));
                    rotation.put(buffer, vec3f.set(Math.PI * random.nextDouble(), Math.PI * random.nextDouble(), Math.PI * random.nextDouble()));
                    scale.put(buffer, (float) (1.5f + random.nextDouble() - random.nextDouble()) * 0.75f);
                    textureIndex.put(buffer, random.nextInt(numberOfLayers));
                }
            });
            this.addCleanupTask(instanceBuffer, instanceBuffer.createCleanupTask(device));
            rock.getMesh().setInstanceBuffer(instanceBuffer);
            rock.setMaterial(material);
            addGeometry(rock, getMainViewport());

            addAlteration(tpf -> {
                speed[0] += (float) (tpf * 0.35);
                speed[1] += (float) (tpf * 0.01);
                lightSpeed.update();
                return true;
            });
        }

        addAlteration(new OrbitCameraAlteration(this, getMainViewport().getCamera()));
    }

}
