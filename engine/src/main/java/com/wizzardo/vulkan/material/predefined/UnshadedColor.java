package com.wizzardo.vulkan.material.predefined;

import com.wizzardo.vulkan.*;
import com.wizzardo.vulkan.material.Uniform;
import org.joml.Vector3f;
import org.lwjgl.vulkan.VkDevice;

import static org.lwjgl.vulkan.VK10.*;

public class UnshadedColor extends Material {

    protected Vector3f color;
    protected VkDevice device;

    public UnshadedColor(Vector3f color) {
        setVertexShader("shaders/unshaded_color.vert.spv");
        setFragmentShader("shaders/unshaded_color.frag.spv");
        this.color = new Vector3f(color);
        vertexLayout = new VertexLayout(VertexLayout.BindingDescription.POSITION);
    }

    public void setColor(Vector3f color) {
        this.color.set(color);
        updateUniforms();
    }

    public Vector3f getColor(){
        return color;
    }

    @Override
    public void prepare(VulkanApplication application, Viewport viewport) {
        if (uniforms.isEmpty())
            addUniform(new Uniform.Vec3(application.getPhysicalDevice(), application.getDevice(), VK_SHADER_STAGE_FRAGMENT_BIT, 1, color));

        updateUniforms();
        super.prepare(application, viewport);
    }
}
