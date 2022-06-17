package com.wizzardo.vulkan.material.predefined;

import com.wizzardo.vulkan.Material;
import com.wizzardo.vulkan.TextureImage;
import com.wizzardo.vulkan.Viewport;
import com.wizzardo.vulkan.VulkanApplication;

public class UnshadedTexture extends Material {

    public UnshadedTexture(TextureImage textureImage) {
        setVertexShader("shaders/unshaded_texture.vert.spv");
        setFragmentShader("shaders/unshaded_texture.frag.spv");
        setTextureImage(textureImage);
        vertexLayout = new VertexLayout(VertexLayout.BindingDescription.POSITION, VertexLayout.BindingDescription.TEXTURE_COORDINATES);
    }

    @Override
    protected void prepare(VulkanApplication application, Viewport viewport) {
        setTextureSampler(application.createTextureSampler(getTextureImage().mipLevels));
        super.prepare(application, viewport);
    }
}
