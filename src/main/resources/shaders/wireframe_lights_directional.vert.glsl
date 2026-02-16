#version 460 core

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aColor;
layout (location = 2) in mat4 aInstanceMatrix;

uniform mat4 u_View;
uniform mat4 u_Proj;

out vec4 vColor;

void main() {
    vColor = vec4(aColor, 1.0);
    gl_Position = u_Proj * u_View * aInstanceMatrix * vec4(aPos, 1.0);
}
