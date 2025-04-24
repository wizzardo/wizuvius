#version 450

// Vertex attributes
layout (location = 0) in vec3 inPos;
layout (location = 1) in vec3 inNormal;
layout (location = 2) in vec2 inUV;

// Instanced attributes
layout (location = 3) in vec3 instancePos;
layout (location = 4) in vec3 instanceRot;
layout (location = 5) in float instanceScale;
layout (location = 6) in int instanceTexIndex;

layout (binding = 0) uniform UBO
{
	mat4 model;
	mat4 view;
	mat4 projection;
} ubo;

layout (binding = 1) uniform LightSpeed
{
	vec4 lightPos;
	float locSpeed;
	float globSpeed;
} ls;

layout (location = 0) out vec3 outNormal;
layout (location = 1) out vec3 outColor;
layout (location = 2) out vec3 outUV;
layout (location = 3) out vec3 outViewVec;
layout (location = 4) out vec3 outLightVec;

void main() 
{
	outColor = vec3(0.75);
	outUV = vec3(inUV, instanceTexIndex);

	mat3 mx, my, mz;
	
	// rotate around x
	float s = sin(instanceRot.x + ls.locSpeed);
	float c = cos(instanceRot.x + ls.locSpeed);

	mx[0] = vec3(c, s, 0.0);
	mx[1] = vec3(-s, c, 0.0);
	mx[2] = vec3(0.0, 0.0, 1.0);
	
	// rotate around y
	s = sin(instanceRot.y + ls.locSpeed);
	c = cos(instanceRot.y + ls.locSpeed);

	my[0] = vec3(c, 0.0, s);
	my[1] = vec3(0.0, 1.0, 0.0);
	my[2] = vec3(-s, 0.0, c);
	
	// rot around z
	s = sin(instanceRot.z + ls.locSpeed);
	c = cos(instanceRot.z + ls.locSpeed);
	
	mz[0] = vec3(1.0, 0.0, 0.0);
	mz[1] = vec3(0.0, c, s);
	mz[2] = vec3(0.0, -s, c);
	
	mat3 rotMat = mz * my * mx;

	mat4 gRotMat;
	s = sin(instanceRot.y + ls.globSpeed);
	c = cos(instanceRot.y + ls.globSpeed);
	gRotMat[0] = vec4(c, 0.0, s, 0.0);
	gRotMat[1] = vec4(0.0, 1.0, 0.0, 0.0);
	gRotMat[2] = vec4(-s, 0.0, c, 0.0);
	gRotMat[3] = vec4(0.0, 0.0, 0.0, 1.0);	
	
	vec4 locPos = vec4(inPos.xyz * rotMat, 1.0);
	vec4 pos = vec4((locPos.xyz * instanceScale) + instancePos, 1.0);

	mat4 modelview = ubo.view * ubo.model;
	gl_Position = ubo.projection * modelview * gRotMat * pos;
	outNormal = mat3(modelview * gRotMat) * inverse(rotMat) * inNormal;

	pos = modelview * vec4(inPos.xyz + instancePos, 1.0);
	vec3 lPos = mat3(modelview) * ls.lightPos.xyz;
	outLightVec = lPos - pos.xyz;
	outViewVec = -pos.xyz;		
}
