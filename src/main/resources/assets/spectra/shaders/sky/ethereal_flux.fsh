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
    return fract(sin(dot(p, vec2(173.3, 246.1))) * 43758.5453123);
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
    float amplitude = 0.54;
    mat2 rot = mat2(1.7, 1.1, -1.1, 1.7);
    for (int i = 0; i < OCTAVES; i++) {
        value += amplitude * noise(p);
        p = rot * p + vec2(2.3, 5.9);
        amplitude *= 0.56;
    }
    return value;
}
float saturate(float x) {
    return clamp(x, 0.0, 1.0);
}
float ridge(float x, float sharpness) {
    return pow(saturate(1.0 - abs(x * 2.0 - 1.0)), sharpness);
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
    float t = time * 0.11;
    float height = skyDir.y * 0.5 + 0.5;
    vec3 samplePos = skyDir * vec3(2.05, 1.55, 2.05) * patternScale;
    vec3 warp = vec3(
        triFbm(samplePos * 0.62 + vec3(t * 0.05, -t * 0.03, 1.7)),
        triFbm(samplePos * 0.58 + vec3(-1.2, t * 0.04, -t * 0.02)),
        triFbm(samplePos * 0.64 + vec3(2.4 - t * 0.03, -1.6, t * 0.02))
    );
    vec3 domain = samplePos + (warp - 0.5) * vec3(1.12, 0.46, 1.12);
    float mistA = triFbm(domain + vec3(t * 0.12, -t * 0.05, t * 0.08));
    float mistB = triFbm(domain * 1.36 + vec3(-t * 0.08, t * 0.05, -t * 0.04));
    float detail = triFbm(domain * 2.08 + vec3(t * 0.03, -t * 0.08, t * 0.02));
    float micro = triFbm(domain * 3.10 + vec3(-t * 0.02, t * 0.03, -t * 0.05));
    float voidNoise = triFbm(domain * 0.74 + vec3(3.4, -2.1, 1.6) + vec3(t * 0.02, 0.0, -t * 0.01));
    float currentAxisA = dot(skyDir, normalize(vec3(0.86, 0.32, -0.40)));
    float currentAxisB = dot(skyDir, normalize(vec3(-0.28, 0.24, 0.93)));
    float currentAxisC = dot(skyDir, normalize(vec3(0.14, 0.96, 0.22)));
    float currentA = ridge(fract(currentAxisA * 1.82 + mistA * 0.92 - t * 0.34), 4.0);
    float currentB = ridge(fract(currentAxisB * 2.16 - mistB * 0.86 + t * 0.26), 4.4);
    float currentC = ridge(fract(currentAxisC * 1.38 + detail * 0.74 - t * 0.18), 3.2);
    float filament = ridge(detail * 0.68 + micro * 0.32, 4.8);
    float veil = smoothstep(0.30, 0.84, mistA * 0.72 + mistB * 0.40);
    float density = currentA * 0.62 + currentB * 0.58 + currentC * 0.26 + filament * 0.20;
    float lowerBand = smoothstep(0.04, 0.18, height) * (1.0 - smoothstep(0.72, 0.90, height));
    float midBand = smoothstep(0.16, 0.42, height) * (1.0 - smoothstep(0.95, 0.995, height));
    float topBand = smoothstep(0.44, 0.74, height) * (1.0 - smoothstep(0.998, 1.0, height));
    float bandMask = lowerBand * 0.62 + midBand + topBand * 0.38;
    float softVoids = smoothstep(0.56, 0.92, voidNoise * 0.82 + (1.0 - veil) * 0.24);
    float flux = saturate(veil * 0.30 + density * 0.70);
    flux *= bandMask * (1.0 - softVoids * 0.34);
    vec3 nightBase = vec3(0.022, 0.028, 0.070);
    vec3 teal = mix(vec3(0.20, 0.88, 0.90), baseColor.rgb * 1.02, 0.18);
    vec3 gold = vec3(0.98, 0.80, 0.44);
    vec3 lilac = vec3(0.68, 0.60, 0.92);
    vec3 paleMist = vec3(0.82, 0.86, 0.96);
    vec3 color = mix(nightBase, teal, veil * 0.26 + currentA * 0.18);
    color = mix(color, lilac, currentB * 0.30 + filament * 0.10);
    color = mix(color, gold, currentA * 0.18 + currentC * 0.12);
    color = mix(color, paleMist, filament * 0.12 + currentB * 0.08 + veil * 0.08);
    color += vec3(0.05, 0.09, 0.13) * veil * 0.22;
    float fresher = 0.92 + 0.08 * cos(skyAngle * 6.28318 + skyDir.x * 1.2 - skyDir.z * 1.0);
    float weatherFade = 1.0 - rainStrength * 0.48;
    float finalAlpha = alpha * flux * (0.34 + density * 0.40 + veil * 0.12 + filament * 0.08)
            * weatherFade * fresher * (0.78 + starBrightness * 0.42);
    FragColor = vec4(clamp(color, 0.0, 1.0), clamp(finalAlpha, 0.0, alpha));
}
