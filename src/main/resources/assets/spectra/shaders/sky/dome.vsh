#version 330 core

layout(location = 0) in vec3 a_position;
layout(location = 1) in vec2 a_uv;

uniform mat4 u_modelView;
uniform mat4 u_projection;

out vec3 localPos;
out vec2 uvCoord;

void main() {
    localPos = a_position;
    uvCoord = a_uv;
    gl_Position = u_projection * u_modelView * vec4(a_position, 1.0);
}
