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
    return fract(sin(dot(p, vec2(41.0, 289.0))) * 45758.5453);
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
    float amplitude = 0.55;
    mat2 rot = mat2(1.7, 1.1, -1.1, 1.7);
    for (int i = 0; i < OCTAVES; i++) {
        value += amplitude * noise(p);
        p = rot * p + vec2(2.3, 6.1);
        amplitude *= 0.53;
    }
    return value;
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
    float t = time * 0.18;
    vec3 samplePos = skyDir * vec3(2.3, 1.8, 2.3) * patternScale;
    float nebulaA = triFbm(samplePos + vec3(t * 0.22, -t * 0.08, t * 0.12));
    float nebulaB = triFbm(samplePos * 1.42 + vec3(-t * 0.14, t * 0.11, -t * 0.05));
    float veil = triFbm(samplePos * 0.82 + vec3(t * 0.06, t * 0.14, -t * 0.10));
    float cloud = smoothstep(0.34, 0.96, nebulaA * 0.65 + nebulaB * 0.55 + veil * 0.42);
    float glowCluster = smoothstep(0.56, 1.0, nebulaB * 0.85 + veil * 0.40);
    vec3 deepNight = vec3(0.05, 0.07, 0.18);
    vec3 nebulaBlue = mix(vec3(0.28, 0.44, 0.98), baseColor.rgb * 1.10, 0.32);
    vec3 nebulaPurple = mix(vec3(0.58, 0.33, 0.88), baseColor.rgb * 1.18, 0.42);
    vec3 nebulaPink = vec3(0.95, 0.56, 0.82);
    vec3 color = mix(deepNight, nebulaBlue, smoothstep(0.18, 0.92, nebulaA));
    color = mix(color, nebulaPurple, smoothstep(0.28, 1.0, nebulaB) * 0.82);
    color = mix(color, nebulaPink, glowCluster * 0.26);
    color += vec3(0.05, 0.08, 0.16) * veil * 0.45;
    float weatherFade = 1.0 - rainStrength * 0.58;
    float height = skyDir.y * 0.5 + 0.5;
    float poleFade = 1.0 - smoothstep(0.988, 1.0, skyDir.y);
    float cosmicSpread = smoothstep(0.04, 0.30, height) * (1.0 - smoothstep(0.985, 1.0, height));
    float anglePulse = 0.88 + 0.12 * cos(skyAngle * 6.28318 + skyDir.x * 1.1 + skyDir.z * 1.35);
    float finalAlpha = alpha * (0.16 + cloud * 0.60 + glowCluster * 0.28)
            * weatherFade * poleFade * cosmicSpread * anglePulse * (0.82 + starBrightness * 0.48);
    FragColor = vec4(color, clamp(finalAlpha, 0.0, alpha));
}
