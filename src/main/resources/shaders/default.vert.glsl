#version 460 core

#define MAX_BONES 200;

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aTexCoord0;
layout (location = 3) in vec2 aTexCoord1;
layout (location = 4) in vec3 aTangent;
layout (location = 5) in vec3 aBitangent;
layout (location = 6) in ivec4 aBoneIds;
layout (location = 7) in vec4 aBoneWeights;
layout (location = 8) in mat4 aInstanceMatrix;

uniform mat4 u_View;
uniform mat4 u_Proj;
uniform bool u_HasBones;

out vec3 FragPos;
out vec2 TexCoords0;
out vec2 TexCoords1;
out mat3 TBN;

layout (std430, binding = 0) readonly buffer BoneMatrices {
    mat4 bones[];
} u_BoneMatrices;

void main() {
    mat4 model;
    if (u_HasBones) {
        // Calculate the matrix for model transform
        int offset = gl_InstanceID * MAX_BONES;
        mat4 boneTransform = mat4(0.0);
        float totalWeight = 0;

        for(int i = 0; i < 4; i++) {
            if(aBoneIds[i] >= 0) {
                boneTransform += u_BoneMatrices.bones[offset + aBoneIds[i]] * aBoneWeights[i];
                totalWeight += aBoneWeights[i];
            }
        }

        if(totalWeight == 0) {
            boneTransform = mat4(1.0);
        } else {
            boneTransform /= totalWeight;
        }

        model = aInstanceMatrix * boneTransform;
    } else {
        model = aInstanceMatrix;
    }

    vec4 modelPos = model * vec4(aPos, 1.0);
    // FIXME: Apparently this operation is expensive
    // Maybe figure out how to avoid it when the model matrix doesn't scale anything?
    mat3 normalMatrix = transpose(inverse(mat3(model)));
    vec3 N = normalize(normalMatrix * aNormal);
    vec3 T = normalize(normalMatrix * aTangent);
    vec3 B = normalize(normalMatrix * aBitangent);
    TBN = mat3(T, B, N);

    FragPos = vec3(modelPos);
    TexCoords0 = aTexCoord0;
    TexCoords1 = aTexCoord1;
    gl_Position = u_Proj * u_View * modelPos;
}
