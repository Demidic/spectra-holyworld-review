#version 330 core

in vec2 v_uv;
in vec3 v_surface;
out vec4 out_color;

uniform vec2 u_resolution;
uniform float u_time;
uniform vec4 u_tint;
uniform int u_mode;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash21(i), hash21(i + vec2(1.0, 0.0)), f.x),
               mix(hash21(i + vec2(0.0, 1.0)), hash21(i + 1.0), f.x), f.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 5; i++) {
        value += noise(p) * amplitude;
        p = mat2(1.6, 1.2, -1.2, 1.6) * p;
        amplitude *= 0.5;
    }
    return value;
}

void main() {
    vec2 uv = v_uv;
    float t = u_time;
    float value;

    if (u_mode == 0) {
        value = 0.5 + 0.5 * sin((uv.x * 9.0 + sin(uv.y * 7.0 + t)) * 3.0 + t * 2.0);
    } else if (u_mode == 1) {
        vec2 q = vec2(fbm(uv * 4.0 + t * 0.18), fbm(uv * 4.0 - t * 0.13 + 4.7));
        value = fbm(uv * 5.0 + q * 2.5);
    } else if (u_mode == 2) {
        vec2 grid = abs(fract(uv * 7.0 + vec2(t * 0.08, -t * 0.06)) - 0.5);
        float strands = min(grid.x, grid.y);
        float diagonal = abs(fract((uv.x + uv.y) * 5.0 + t * 0.05) - 0.5);
        value = 1.0 - smoothstep(0.02, 0.12, min(strands, diagonal));
    } else if (u_mode == 3) {
        value = 0.5 + 0.5 * sin(uv.x * 14.0 + sin(uv.y * 9.0 - t * 1.7) * 2.2 + t * 2.3);
    } else if (u_mode == 4) {
        value = smoothstep(0.25, 0.82, fbm(uv * 5.0 + vec2(t * 0.12, -t * 0.08)));
    } else {
        float distanceToCenter = length(v_surface - vec3(0.5));
        value = 0.55 + 0.45 * sin(distanceToCenter * 24.0 - t * 4.0);
    }

    vec3 accent = u_tint.rgb;
    vec3 bright = mix(accent * 0.28, min(vec3(1.0), accent * 1.65 + 0.22), value);
    float edge = 1.0 - smoothstep(0.25, 0.72, length(v_surface - vec3(0.5)));
    out_color = vec4(bright, u_tint.a * (0.58 + value * 0.42) * (0.82 + edge * 0.18));
}
