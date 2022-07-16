#version 450

layout (binding = 1) uniform sampler2D samplerColor;
layout (binding = 2) uniform UBO {
    float lodBias;
} ubo;

layout (location = 0) in vec2 inUV;

layout (location = 0) out vec4 outFragColor;

void main() {
    vec4 color = texture(samplerColor, inUV, ubo.lodBias);

    outFragColor = vec4(color.rgb, 1.0);
    //	outFragColor = vec4(ubo.lodBias/10,0,0, 1.0);
}