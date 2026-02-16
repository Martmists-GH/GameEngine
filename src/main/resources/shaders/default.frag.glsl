#version 460 core

struct DirLight {
    vec3 direction;
    vec4 color;
    float intensity;
};

struct PointLight {
    vec3 position;
    vec4 color;
    float intensity;
};

struct SpotLight {
    vec3 position;
    vec3 direction;
    vec4 color;
    float intensity;
    float range;
};

struct Material {
    vec4 ambient;
    vec4 diffuse;
    vec4 emissive;
    vec4 specular;
    float shininess;
    float opacity;

    bool hasAmbientTexture;
    sampler2D ambientTexture;
    bool hasDiffuseTexture;
    sampler2D diffuseTexture;
    bool hasEmissiveTexture;
    sampler2D emissiveTexture;
    bool hasSpecularTexture;
    sampler2D specularTexture;
    bool hasNormalTexture;
    sampler2D normalTexture;
    bool hasDisplacementTexture;
    sampler2D displacementTexture;

    int ambientUVIndex;
    int diffuseUVIndex;
    int emissiveUVIndex;
    int specularUVIndex;
    int normalUVIndex;
    int displacementUVIndex;
};

struct DebugSettings {
    // Disable parts of the shader
    bool disableAmbient;
    bool disableDiffuse;
    bool disableEmissive;
    bool disableSpecular;
    bool disableNormal;
    bool disableDisplacement;

    // Toggle between simple and ray-marched approach
    bool useRayMarchDisplacement;

    // Render custom stuff instead
    bool viewNormals;
};

#define MAX_DIR_LIGHTS 4
#define MAX_POINT_LIGHTS 16
#define MAX_SPOT_LIGHTS 8

in vec3 FragPos;
in vec2 TexCoords0;
in vec2 TexCoords1;
in mat3 TBN;

uniform int u_NumDirLights;
uniform int u_NumPointLights;
uniform int u_NumSpotLights;

uniform DirLight u_DirLights[MAX_DIR_LIGHTS];
uniform PointLight u_PointLights[MAX_POINT_LIGHTS];
uniform SpotLight u_SpotLights[MAX_SPOT_LIGHTS];

uniform vec3 u_ViewPos;
uniform Material u_Material;
uniform DebugSettings u_DebugSettings;

out vec4 FragColor;

void main() {
    vec3 V = normalize(u_ViewPos - FragPos);
    vec3 tanV = transpose(TBN) * V;

    vec2 uv0 = TexCoords0;
    vec2 uv1 = TexCoords1;

#define UV(index) (index == 0 ? uv0 : uv1)

    // TODO: Implement as tesselation?
    if (u_Material.hasDisplacementTexture && !u_DebugSettings.disableDisplacement) {
        const float scale = 0.05;
        if (u_DebugSettings.useRayMarchDisplacement) {
            // FIXME: This looks like shit atm
            const int maxLayers = 32;
            float numLayers = mix(maxLayers, 8, abs(dot(vec3(0.0, 0.0, 1.0), tanV)));
            float layerDepth = 1.0 / numLayers;
            float currentLayerDepth = 0.0;

            if (tanV.z <= 0.0)
                discard;

            // P is the view direction in tangent space
            vec2 P = tanV.xy * scale;
            vec2 deltaUV = P / numLayers;

            vec2 currentUV = UV(u_Material.displacementUVIndex);
            float currentDepthMapValue = 1 - texture(u_Material.displacementTexture, currentUV).r;

            for(int i = 0; i < numLayers; i++) {
                if (currentLayerDepth >= currentDepthMapValue) break;
                currentUV -= deltaUV;
                currentDepthMapValue = 1 - texture(u_Material.displacementTexture, currentUV).r;
                currentLayerDepth += layerDepth;
            }

            vec2 prevUV = currentUV + deltaUV;

            float afterDepth  = currentDepthMapValue - currentLayerDepth;
            float beforeDepth = texture(u_Material.displacementTexture, prevUV).r - currentLayerDepth + layerDepth;
            float weight = afterDepth / (afterDepth + beforeDepth);

            vec2 finalUV = prevUV * weight + currentUV * (1.0 - weight);

            vec2 finalOffset = finalUV - UV(u_Material.displacementUVIndex);
            uv0 += finalOffset;
            uv1 += finalOffset;
        } else {
            vec4 displacement = texture(u_Material.displacementTexture, UV(u_Material.displacementUVIndex));
            vec2 delta = tanV.xy / tanV.z * (displacement.r - 1) * scale;
            uv0 += delta;
            uv1 += delta;
        }
    }

    vec4 ambient = (u_Material.hasAmbientTexture ? texture(u_Material.ambientTexture, UV(u_Material.ambientUVIndex)) : u_Material.ambient);
    if (u_DebugSettings.disableAmbient) {
        ambient = vec4(0.0, 0.0, 0.0, 1.0);
    }
    vec4 diffuse = (u_Material.hasDiffuseTexture ? texture(u_Material.diffuseTexture, UV(u_Material.diffuseUVIndex)) : u_Material.diffuse);
    if (u_DebugSettings.disableDiffuse) {
        diffuse = vec4(0.2, 0.2, 0.2, 1.0);
    }
    vec4 emissive = (u_Material.hasEmissiveTexture ? texture(u_Material.emissiveTexture, UV(u_Material.emissiveUVIndex)) : u_Material.emissive);
    if (u_DebugSettings.disableEmissive) {
        emissive = vec4(0.0, 0.0, 0.0, 1.0);
    }
    vec4 specular = (u_Material.hasSpecularTexture ? texture(u_Material.specularTexture, UV(u_Material.specularUVIndex)) : u_Material.specular);
    if (u_DebugSettings.disableSpecular) {
        specular = vec4(0.0, 0.0, 0.0, 1.0);
    }

    vec3 ambientColor = ambient.rgb * diffuse.rgb;

    vec3 N;
    if (u_Material.hasNormalTexture && !u_DebugSettings.disableNormal) {
        vec3 normal = texture(u_Material.normalTexture, UV(u_Material.normalUVIndex)).rgb;
        normal = normalize(normal * 2 - 1);
        N = normalize(TBN * normal);
    } else {
        N = normalize(TBN[2]);
    }


    vec3 lighting = vec3(0.0);
    for (int i = 0; i < u_NumDirLights; i++) {
        if (i >= MAX_DIR_LIGHTS) break;

        vec3 L = normalize(-u_DirLights[i].direction);
        bool isActuallyLit = dot(TBN[2], L) > 0.0;
        if (!isActuallyLit) continue;

        float diffuseIntensity = max(dot(N, L), 0.0);\
        if (diffuseIntensity == 0.0) continue;

        vec3 H = normalize(L + V);
        float specularIntensity = pow(max(dot(N, H), 0.0), u_Material.shininess);

        vec3 diffuseFactor = diffuse.rgb * diffuseIntensity;
        vec3 specularFactor = specular.rgb * specularIntensity;

        lighting += u_DirLights[i].color.rgb * (diffuseFactor + specularFactor) * u_DirLights[i].intensity;
    }

    FragColor = vec4(ambientColor + lighting + emissive.rgb, diffuse.a);

    // Convert to sRGB
    FragColor.rgb = pow(FragColor.rgb, vec3(1.0/2.2));

    if (u_DebugSettings.viewNormals) {
        FragColor.rgb = N * 0.5 + 0.5;
    }
}

