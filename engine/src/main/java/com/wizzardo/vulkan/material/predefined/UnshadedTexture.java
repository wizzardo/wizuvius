package com.wizzardo.vulkan.material.predefined;

import com.wizzardo.vulkan.Material;
import com.wizzardo.vulkan.TextureImage;
import com.wizzardo.vulkan.Viewport;
import com.wizzardo.vulkan.VulkanApplication;

public class UnshadedTexture extends Material {

    public UnshadedTexture(TextureImage textureImage) {
        this();
        addTextureImage(textureImage);
    }

    public UnshadedTexture() {
        setVertexShader("shaders/unshaded_texture.vert.spv");
        setFragmentShader("shaders/unshaded_texture.frag.spv");
        vertexLayout = new VertexLayout(VertexLayout.BindingDescription.POSITION, VertexLayout.BindingDescription.TEXTURE_COORDINATES);
    }

    @Override
    public void prepare(VulkanApplication application, Viewport viewport) {
        if (textureSampler == null)
            setTextureSampler(application.createTextureSampler(getTextures().get(0).getMipLevels()));
        super.prepare(application, viewport);
    }
}
