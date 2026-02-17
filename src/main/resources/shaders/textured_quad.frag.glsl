#version 460 core

in vec2 FragPos;

uniform sampler2D u_TextureAtlas;

out vec4 FragColor;

void main() {
    FragColor = texture(u_TextureAtlas, FragPos);
}
