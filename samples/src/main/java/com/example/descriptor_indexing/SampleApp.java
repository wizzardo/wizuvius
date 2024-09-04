package com.example.descriptor_indexing;

import com.example.AbstractSampleApp;
import com.wizzardo.tools.misc.Unchecked;
import com.wizzardo.vulkan.*;
import com.wizzardo.vulkan.material.Uniform;
import com.wizzardo.vulkan.material.predefined.UnshadedColor;
import com.wizzardo.vulkan.material.predefined.UnshadedTexture;
import com.wizzardo.vulkan.scene.Geometry;
import com.wizzardo.vulkan.scene.shape.Box;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class SampleApp extends AbstractSampleApp {

    public static void main(String[] args) {
        new SampleApp().start();
    }

    int texturesCount = 32;
    List<TextureImage> textures = new ArrayList<>(texturesCount);
    Uniform.Int textureIndexUniform;
    TextureSampler textureSampler;

    @Override
    protected void initApp() {
        getMainViewport().getCamera().setLocation(new Vector3f(2, 2, 2));
        getMainViewport().getCamera().lookAt(Vectors.ZERO, Vectors.UNIT_Y);

        {
            textureSampler = createTextureSampler(1);
            generateRandomTextures();

//            Material material = new UnshadedColor(new Vector3f(1, 0, 0));

            textureIndexUniform = new Uniform.Int(this, VK10.VK_SHADER_STAGE_FRAGMENT_BIT, 1, 0);
            Material material = new Material() {
                {
                    withUBO = true;
                    fragmentShader = folder + "/shaders/indexing.frag";
                    vertexShader = folder + "/shaders/indexing.vert";
                    vertexLayout = new VertexLayout(
                            VertexLayout.BindingDescription.POSITION,
                            VertexLayout.BindingDescription.TEXTURE_COORDINATES
                    );
                    addUniform(textureIndexUniform);
                }
            };

            Geometry geometry = new Box(material);
            addGeometry(geometry, getMainViewport());
        }
    }

    void generateRandomTextures() {
        for (int i = 0; i < texturesCount; i++) {
            int width = 2;
            int height = 2;
            ByteBuffer byteBuffer = MemoryUtil.memAlloc(width * height * 4);
            for (int j = 0; j < width * height; j++) {
                byteBuffer.put((byte) (Math.random() * 205 + 50));
                byteBuffer.put((byte) (Math.random() * 205 + 50));
                byteBuffer.put((byte) (Math.random() * 205 + 50));
                byteBuffer.put((byte) (255));
            }
            byteBuffer.flip();

            TextureImage textureImage = TextureLoader.createTextureImage(this, byteBuffer, width, height, byteBuffer.capacity(), VK10.VK_FORMAT_R8G8B8A8_UNORM, 1);
            bindlessTexturePool.add(textureImage, textureSampler);
            textures.add(textureImage);
        }
    }

    @Override
    protected void simpleUpdate(double tpf) {
        textureIndexUniform.set((int) (getTime() * 2) % texturesCount);
    }
}
