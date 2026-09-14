#version 330 core
in vec3 localPos;
out vec4 FragColor;
uniform float time;
uniform float alpha;
uniform float skyAngle;
uniform float starBrightness;
uniform float rainStrength;
uniform vec4 baseColor;
uniform float patternScale;
#define OCTAVES 4
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(157.7, 271.9))) * 43758.5453123);
}
float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}
float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.56;
    mat2 rot = mat2(1.7, 1.2, -1.2, 1.7);
    for (int i = 0; i < OCTAVES; i++) {
        value += amplitude * noise(p);
        p = rot * p + vec2(2.6, 7.3);
        amplitude *= 0.52;
    }
    return value;
}
float saturate(float value) {
    return clamp(value, 0.0, 1.0);
}
float ridge(float value, float sharpness) {
    return pow(saturate(1.0 - abs(value * 2.0 - 1.0)), sharpness);
}
float triFbm(vec3 p) {
    vec3 w = pow(abs(normalize(p)) + 0.0001, vec3(3.0));
    w /= (w.x + w.y + w.z);
    float yz = fbm(p.yz);
    float xz = fbm(p.xz);
    float xy = fbm(p.xy);
    return yz * w.x + xz * w.y + xy * w.z;
}
void main() {
    vec2 cacheUv = localPos.xy;
    float cacheAzimuth = cacheUv.x * 6.28318530718;
    float cacheElevation = cacheUv.y * 3.14159265359 - 1.57079632679;
    float cacheCosElevation = cos(cacheElevation);
    vec3 dir = vec3(
        cos(cacheAzimuth) * cacheCosElevation,
        sin(cacheElevation),
        sin(cacheAzimuth) * cacheCosElevation
    );
    vec3 skyDir = normalize(vec3(dir.x, abs(dir.y), dir.z));
    float t = time * 0.052;
    float height = skyDir.y * 0.5 + 0.5;
    vec3 samplePos = skyDir * vec3(1.72, 1.24, 1.72) * patternScale;
    vec3 warp = vec3(
        triFbm(samplePos * 0.44 + vec3(t * 0.02, 1.4 - t * 0.015, -1.3)),
        triFbm(samplePos * 0.42 + vec3(-1.9 + t * 0.015, t * 0.03, 2.2)),
        triFbm(samplePos * 0.46 + vec3(2.3 - t * 0.015, -1.7, t * 0.02))
    );
    vec3 domain = samplePos + (warp - 0.5) * vec3(0.82, 0.36, 0.82);
    float basin = triFbm(domain * 0.64 + vec3(t * 0.025, -t * 0.018, t * 0.016));
    float sheet = triFbm(domain * 0.96 + vec3(-t * 0.026, t * 0.018, -t * 0.03));
    float fractureSeed = triFbm(domain * 1.42 + vec3(t * 0.034, -t * 0.03, t * 0.02));
    float fractureFine = triFbm(domain * 2.08 + vec3(-t * 0.026, t * 0.02, -t * 0.016));
    float dust = triFbm(domain * 3.06 + vec3(t * 0.05, t * 0.032, -t * 0.04));
    float veinA = ridge(0.5 + 0.5 * sin((fractureSeed - basin * 0.70) * 8.4 + t * 0.56), 5.8);
    float veinB = ridge(0.5 + 0.5 * sin((fractureFine + sheet * 0.54) * 10.6 - t * 0.70), 6.2);
    float veinC = ridge(0.5 + 0.5 * sin((fractureSeed + fractureFine - basin * 0.30) * 6.8 + t * 0.42), 5.2);
    float crackWeb = smoothstep(0.52, 0.92, veinA * 0.40 + veinB * 0.52 + veinC * 0.24);
    float plasmaSheet = smoothstep(0.36, 0.80, basin * 0.62 + sheet * 0.38);
    float shardGlow = smoothstep(0.58, 0.92, fractureSeed * 0.72 + fractureFine * 0.28);
    float ember = ridge(dust * 0.66 + fractureFine * 0.24, 3.2);
    float lowerBand = smoothstep(0.06, 0.18, height) * (1.0 - smoothstep(0.78, 0.93, height));
    float upperBand = smoothstep(0.22, 0.48, height) * (1.0 - smoothstep(0.98, 0.998, height));
    float domeMask = lowerBand * 0.72 + upperBand;
    float riftMask = saturate(plasmaSheet * 0.34 + crackWeb * 0.52 + shardGlow * 0.18);
    riftMask *= domeMask;
    vec3 obsidian = vec3(0.030, 0.020, 0.060);
    vec3 indigo = vec3(0.16, 0.20, 0.44);
    vec3 violet = mix(vec3(0.54, 0.28, 0.84), baseColor.rgb * 0.94, 0.18);
    vec3 cyan = vec3(0.34, 0.86, 0.96);
    vec3 flare = vec3(0.90, 0.96, 1.00);
    vec3 color = mix(obsidian, indigo, plasmaSheet * 0.48 + shardGlow * 0.12);
    color = mix(color, violet, crackWeb * 0.28 + shardGlow * 0.16);
    color = mix(color, cyan, veinB * 0.16 + crackWeb * 0.10);
    color = mix(color, flare, ember * 0.05 + veinA * 0.04);
    color += vec3(0.04, 0.03, 0.08) * plasmaSheet * 0.18;
    float weatherFade = 1.0 - rainStrength * 0.42;
    float pulse = 0.94 + 0.06 * sin(skyAngle * 6.28318 + basin * 2.7 + skyDir.x * 0.9 - skyDir.z * 0.7);
    float finalAlpha = alpha * riftMask * (0.22 + crackWeb * 0.18 + plasmaSheet * 0.22 + ember * 0.04)
            * weatherFade * pulse * (0.80 + starBrightness * 0.34);
    FragColor = vec4(clamp(color, 0.0, 1.0), clamp(finalAlpha, 0.0, alpha));
}
