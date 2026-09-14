#version 330 core

uniform sampler2D Sampler0;

in vec2 TexCoord;
out vec4 OutColor;

void main() {
    OutColor = texture(Sampler0, TexCoord);
}
