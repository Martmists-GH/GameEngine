#version 460 core

#define MAX_BONES 200

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aTexCoord0;
layout (location = 3) in vec2 aTexCoord1;
layout (location = 4) in vec3 aTangent;
layout (location = 5) in vec3 aBitangent;
layout (location = 6) in ivec4 aBoneIds;
layout (location = 7) in vec4 aBoneWeights;
layout (location = 8) in vec3 aColor;
layout (location = 9) in mat4 aInstanceMatrix;

uniform mat4 u_View;
uniform mat4 u_Proj;
uniform bool u_HasBones;

out vec4 vColor;

layout (std430, binding = 0) readonly buffer BoneMatrices {
    mat4 bones[];
} u_BoneMatrices;

void main() {
    mat4 model;
    if (u_HasBones) {
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

    vColor = vec4(aColor, 1.0);
    gl_Position = u_Proj * u_View * model * vec4(aPos, 1.0);
}
