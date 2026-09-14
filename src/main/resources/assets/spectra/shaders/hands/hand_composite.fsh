#version 330 core

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform int mode;
uniform vec2 texSize;
uniform float time;
uniform float intensity;
uniform float speed;
uniform float patternScale;
uniform float smokeAmount;
uniform float activity;
uniform vec2 motion;
uniform vec3 glowColor;
uniform vec3 smokeColor;

in vec2 TexCoord;
out vec4 OutColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
        mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0)), f.x),
        f.y
    );
}

float fbm(vec2 p) {
    float sum = 0.0;
    float amplitude = 0.52;
    for (int i = 0; i < 4; i++) {
        sum += amplitude * noise(p);
        p = p * 2.03 + vec2(4.7, 8.3);
        amplitude *= 0.49;
    }
    return sum;
}

vec3 clouds(vec2 p, float t) {
    float warp = fbm(p * 0.72 + vec2(t * 0.08, -t * 0.06));
    float density = fbm(p + vec2(warp * 1.35, -warp * 0.9) + vec2(-t * 0.11, t * 0.05));
    density = smoothstep(0.2, 0.92, density);
    vec3 shadow = glowColor * 0.2 + vec3(0.025, 0.03, 0.05);
    vec3 light = mix(glowColor, vec3(0.92, 0.95, 1.0), 0.48);
    return mix(shadow, light, density);
}

vec3 nebula(vec2 p, float t) {
    float first = fbm(p * 0.78 + vec2(t * 0.07, -t * 0.05));
    vec2 warped = p + vec2(first, fbm(p * 0.64 + 7.2)) * 1.8;
    float gas = fbm(warped * 1.16 - vec2(t * 0.09, t * 0.04));
    float filaments = 1.0 - abs(gas * 2.0 - 1.0);
    filaments = pow(clamp(filaments, 0.0, 1.0), 3.1);
    vec3 deep = mix(vec3(0.015, 0.008, 0.045), glowColor * 0.28, first);
    vec3 bright = mix(glowColor, vec3(0.58, 0.36, 1.0), 0.42);
    return deep + bright * filaments * 1.15;
}

vec3 aurora(vec2 p, float t) {
    float n = fbm(p * 0.76 + vec2(t * 0.05, -t * 0.1));
    float bandA = 1.0 - abs(sin(p.x * 1.8 + p.y * 0.58 + n * 4.2 + t * 0.7));
    float bandB = 1.0 - abs(sin(-p.x * 1.15 + p.y * 1.08 - n * 3.4 - t * 0.46));
    bandA = pow(clamp(bandA, 0.0, 1.0), 5.2);
    bandB = pow(clamp(bandB, 0.0, 1.0), 6.0);
    vec3 second = vec3(glowColor.b, min(1.0, glowColor.r + 0.25), glowColor.g);
    return glowColor * bandA + second * bandB * 0.75 + glowColor * n * 0.13;
}

void main() {
    float maskCoverage = texture(Sampler2, vec2(0.5)).r;
    if (maskCoverage > 0.22) {
        discard;
    }

    vec2 smokePixel = 1.0 / max(texSize, vec2(1.0));
    float mask = texture(Sampler0, TexCoord).r;
    vec4 smokeTexture = texture(Sampler1, TexCoord);
    float smoke = max(smokeTexture.a - mask * 0.88, 0.0) * smokeAmount;
    if (mask < 0.002 && smoke < 0.003) {
        discard;
    }

    float t = time * max(speed, 0.05);
    vec2 aspect = vec2(texSize.x / max(texSize.y, 1.0), 1.0);
    vec2 p = (TexCoord - 0.5) * aspect * (5.0 * patternScale);
    p += motion * vec2(9.0, -9.0);

    vec3 material;
    if (mode == 0) {
        material = clouds(p, t);
    } else if (mode == 1) {
        material = nebula(p, t);
    } else {
        material = aurora(p, t);
    }

    float materialAlpha = mask * clamp(0.24 + intensity * 0.42, 0.0, 0.82);

    // smoothstep(0.005, 0.58, smoke) is exactly zero here. Preserve the
    // original material while avoiding noise/gradient work with zero weight.
    if (smoke <= 0.005) {
        OutColor = vec4(clamp(material, 0.0, 1.0), materialAlpha);
        return;
    }

    // Break the blurred trail back into soft filaments instead of displaying
    // it as one uniform halo. Two scales are enough for detail without the
    // cost of a full fluid simulation.
    float smokeDetail = fbm(
        p * 0.58 + vec2(-t * 0.12, t * 0.075) + motion * vec2(4.0, -4.0)
    );
    float fineDetail = noise(p * 1.72 + vec2(t * 0.19, -t * 0.23));
    float densityShape = mix(0.62, 1.08, smokeDetail) * mix(0.84, 1.08, fineDetail);
    smoke = smoothstep(0.005, 0.58, smoke) * densityShape;

    float alphaLeft = texture(Sampler1, TexCoord - vec2(smokePixel.x, 0.0)).a;
    float alphaRight = texture(Sampler1, TexCoord + vec2(smokePixel.x, 0.0)).a;
    float alphaDown = texture(Sampler1, TexCoord - vec2(0.0, smokePixel.y)).a;
    float alphaUp = texture(Sampler1, TexCoord + vec2(0.0, smokePixel.y)).a;
    vec2 densityGradient = vec2(alphaRight - alphaLeft, alphaUp - alphaDown);
    float rimLight = clamp(length(densityGradient) * 2.8, 0.0, 1.0);

    float smokePulse = 0.9 + 0.1 * noise(p * 0.76 - vec2(t * 0.14, t * 0.22));
    float smokeAlpha = clamp(
        smoke * smokePulse * (0.5 + intensity * 0.34 + activity * 0.1),
        0.0,
        0.68
    );
    vec3 smokeShadow = smokeColor * 0.44;
    vec3 smokeLight = mix(smokeColor, vec3(1.0), 0.22);
    vec3 resolvedSmokeColor = mix(smokeShadow, smokeLight, smokeDetail * 0.58 + rimLight * 0.42);
    resolvedSmokeColor = mix(resolvedSmokeColor, smokeTexture.rgb, 0.26);
    vec3 color = mix(material, resolvedSmokeColor, smokeAlpha / max(materialAlpha + smokeAlpha, 0.001));
    OutColor = vec4(clamp(color, 0.0, 1.0), max(materialAlpha, smokeAlpha));
}
