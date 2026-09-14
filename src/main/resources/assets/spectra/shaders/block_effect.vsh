#version 330 core

layout (location = 0) in vec3 a_position;
layout (location = 1) in vec3 a_surface;

uniform mat4 u_modelViewProjection;

out vec2 v_uv;
out vec3 v_surface;

void main() {
    v_surface = a_surface;
    v_uv = a_surface.xy + a_surface.z * vec2(0.37, 0.61);
    gl_Position = u_modelViewProjection * vec4(a_position, 1.0);
}
