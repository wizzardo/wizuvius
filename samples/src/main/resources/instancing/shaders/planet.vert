#version 450

layout (location = 0) in vec3 inPos;
layout (location = 1) in vec3 inNormal;
layout (location = 2) in vec2 inUV;

layout (binding = 0) uniform UBO
{
	mat4 model;
	mat4 view;
	mat4 proj;
} ubo;

layout (binding = 1) uniform Light
{
	vec4 position;
} light;

layout (location = 0) out vec3 outNormal;
layout (location = 1) out vec3 outColor;
layout (location = 2) out vec2 outUV;
layout (location = 3) out vec3 outViewVec;
layout (location = 4) out vec3 outLightVec;

void main() 
{
//	outColor = inColor;
	outColor = vec3(0.75);
	outUV = inUV;
	mat4 modelview = ubo.view * ubo.model;
	gl_Position = ubo.proj * modelview * vec4(inPos, 1.0);
	
	vec4 pos = modelview * vec4(inPos, 1.0);
	outNormal = mat3(modelview) * inNormal;
	vec3 lPos = mat3(modelview) * light.position.xyz;
	outLightVec = lPos - pos.xyz;
	outViewVec = -pos.xyz;		
}