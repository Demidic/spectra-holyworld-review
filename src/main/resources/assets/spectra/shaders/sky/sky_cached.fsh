#version 330 core

in vec3 localPos;
in vec2 uvCoord;

uniform sampler2D SkyTexturePrevious;
uniform sampler2D SkyTextureCurrent;
uniform float u_blend;
uniform float u_opacity;

out vec4 FragColor;

void main() {
    vec3 direction = normalize(localPos);
    float azimuth = atan(direction.z, direction.x);
    float u = fract(azimuth / 6.28318530718 + 1.0);
    float v = asin(clamp(direction.y, -1.0, 1.0)) / 3.14159265359 + 0.5;
    vec2 skyUv = vec2(u, clamp(v, 0.0, 1.0));
    vec4 previousSky = texture(SkyTexturePrevious, skyUv);
    vec4 currentSky = texture(SkyTextureCurrent, skyUv);
    vec4 sky = mix(previousSky, currentSky, clamp(u_blend, 0.0, 1.0));
    // Keep a continuous shader base over the whole dome. Individual effects
    // can still become denser, but never expose a hard empty strip.
    sky.a = max(sky.a, u_opacity * 0.35);
    FragColor = sky;
}
