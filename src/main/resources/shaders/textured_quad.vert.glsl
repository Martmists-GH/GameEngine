#version 460 core

layout (location = 0) in vec2 aPos;
layout (location = 1) in vec2 aUVOffset;
layout (location = 2) in vec2 aUVScale;
layout (location = 3) in mat4 aInstanceMatrix;

uniform mat4 u_View;
uniform mat4 u_Proj;

out vec2 FragPos;

void main() {
    vec2 pos = (aPos * aUVScale);
    pos.y = aUVScale.y - pos.y;
    FragPos = aUVOffset + pos;
    gl_Position = u_Proj * u_View * aInstanceMatrix * vec4(aPos, 0.0, 1.0);
}
