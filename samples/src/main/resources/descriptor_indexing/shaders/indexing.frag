#version 450
#extension GL_EXT_nonuniform_qualifier : require

layout(set = 0, binding = 1) uniform TextureIndex {
    int index;
} ti;
layout (set = 1, binding = 0) uniform sampler2D textures[];

layout(location = 0) in vec2 inTexCoord;

layout(location = 0) out vec4 outColor;

void main() {
    outColor = texture(textures[nonuniformEXT(ti.index)], inTexCoord);
}