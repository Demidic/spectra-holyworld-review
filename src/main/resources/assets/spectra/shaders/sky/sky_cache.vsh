#version 330 core

layout(location = 0) in vec2 a_position;
layout(location = 1) in vec2 a_uv;

out vec3 localPos;
out vec2 uvCoord;

void main() {
    localPos = vec3(a_uv, 0.0);
    uvCoord = a_uv;
    gl_Position = vec4(a_position, 0.0, 1.0);
}
