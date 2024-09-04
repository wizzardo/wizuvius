#version 450
#extension GL_ARB_separate_shader_objects : enable

struct Attenuation {
    float constantValue;
    float linear;
    float exponent;
};

struct PointLight {
    vec3 colour;
// Light position is assumed to be in view coordinates
    vec3 position;
    float intensity;
    Attenuation att;
};

layout(binding = 1) uniform sampler2D texSampler;
layout(binding = 2) uniform Material {
    float reflectance;
} material;
layout(binding = 3) uniform PointLightUniform {
    PointLight pointLight;
} pointLightUniform;

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;
layout(location = 2) in vec2 fragTexCoord;
layout(location = 3) in mat4 view;

layout(location = 0) out vec4 outColor;

vec4 ambientC;
vec4 diffuseC;
vec4 speculrC;

vec3 ambientLight;
float specularPower;
float reflectance;

PointLight light;

//float lengthSquared(vec3 v){
//    return dot(v, v);
//}

vec4 calcPointLight(PointLight light, vec3 position, vec3 normal) {
    vec4 diffuseColour = vec4(0, 0, 0, 0);
    vec4 specColour = vec4(0, 0, 0, 0);

    // Diffuse Light
    vec3 light_direction = light.position - position;
    vec3 to_light_source  = normalize(light_direction);
    float diffuseFactor = max(dot(normal, to_light_source), 0.0);
    diffuseColour = diffuseC * vec4(light.colour, 1.0) * light.intensity * diffuseFactor;

    // Specular Light
    vec3 camera_direction = normalize(-position);
    vec3 from_light_source = -to_light_source;
    vec3 reflected_light = normalize(reflect(from_light_source, normal));
    float specularFactor = max(dot(camera_direction, reflected_light), 0.0);
    specularFactor = pow(specularFactor, specularPower);
    specColour = speculrC * specularFactor * reflectance * vec4(light.colour, 1.0);


    // Attenuation
    float distance = length(light_direction);
    float attenuationInv = light.att.constantValue + light.att.linear * distance + light.att.exponent * distance * distance;
    return (diffuseColour + specColour) / attenuationInv;
}


void main() {
    specularPower=2.0;
    reflectance=1.0;
    Attenuation att =  Attenuation(0.0, 0.0, 0.05);
    light = PointLight(
    vec3(1, 1, 1), // color
    vec3(4, 4, 4), // position
    3.0, // intensity
    att
    );


    light.position = (view*vec4(light.position, 1.0)).xyz;

//        diffuseC = texture(texSampler, fragTexCoord);
    diffuseC = vec4(0, 0.5, 0.5, 1);
    ambientC = diffuseC;
    speculrC = diffuseC;

    //    outColor = texture(texSampler, fragTexCoord);
    //   outColor = texture(texSampler, fragTexCoord * 2.0);
    //    outColor = vec4(normal, 1);
    outColor = calcPointLight(light, position, normal);
    outColor.a = diffuseC.a;

    //    vec4 diffuseColour = vec4(0, 0, 0, 0);
    //    diffuseColour = diffuseC * vec4(light.colour, 1.0) * light.intensity;// * diffuseFactor;
    //    outColor = diffuseColour;
}

