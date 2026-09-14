#version 330 core

layout(location = 0) in vec2 a_position;
layout(location = 1) in vec2 a_uv;

out vec2 TexCoord;

void main() {
    TexCoord = a_uv;
    gl_Position = vec4(a_position, 0.0, 1.0);
}
