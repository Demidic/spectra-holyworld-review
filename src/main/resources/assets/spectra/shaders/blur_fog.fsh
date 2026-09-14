#version 330 core

out vec4 fragColor;
in vec2 texCoord;

uniform sampler2D uBlurTexture;
uniform sampler2D uDepthTexture;
uniform float uNearPlane;
uniform float uFarPlane;
uniform float uFogDistance;

float linearDepth(float depth) {
    float ndc = depth * 2.0 - 1.0;
    return (2.0 * uNearPlane * uFarPlane)
        / (uFarPlane + uNearPlane - ndc * (uFarPlane - uNearPlane));
}

void main() {
    float distance = linearDepth(texture(uDepthTexture, texCoord).r);
    float fog = smoothstep(2.0, uFogDistance, distance);
    vec3 blurred = texture(uBlurTexture, texCoord).rgb;
    fragColor = vec4(blurred, fog);
}
