#version 450
#extension GL_ARB_separate_shader_objects : enable

layout(binding = 1) uniform UniformBuffer {
    vec3 color;
} ub;

layout(location = 0) out vec4 outColor;

void main() {
    outColor = vec4(ub.color, 1);
}