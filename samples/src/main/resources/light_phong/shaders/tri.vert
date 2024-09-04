#version 450
#extension GL_ARB_separate_shader_objects : enable

layout(binding = 0) uniform UniformBufferObject {
    mat4 model;
    mat4 view;
    mat4 proj;
} ubo;

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec2 inTexCoord;
layout(location = 2) in vec3 inNormal;

layout(location = 0) out vec3 position;
layout(location = 1) out vec3 normal;
layout(location = 2) out vec2 fragTexCoord;
layout(location = 3) out mat4 view;


void main() {

    //    vec4 mvPos = modelViewMatrix * vec4(position, 1.0);
    //    gl_Position = projectionMatrix * mvPos;
    //    outTexCoord = texCoord;
    //    mvVertexNormal = normalize(modelViewMatrix * vec4(vertexNormal, 0.0)).xyz;
    //    mvVertexPos = mvPos.xyz;

    mat4 modelViewMatrix = ubo.view * ubo.model;

    vec4 pos = modelViewMatrix * vec4(inPosition, 1.0);
    gl_Position = ubo.proj * pos;

    normal = normalize(modelViewMatrix * vec4(inNormal, 0.0)).xyz;
    position = pos.xyz;

    view = ubo.view;

    //    gl_Position = ubo.proj * ubo.view * ubo.model * vec4(inPosition, 1.0);
    fragTexCoord = inTexCoord;
}