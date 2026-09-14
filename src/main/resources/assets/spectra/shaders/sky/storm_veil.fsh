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
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
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
    float amplitude = 0.58;
    mat2 rot = mat2(1.7, 1.2, -1.2, 1.7);
    for (int i = 0; i < OCTAVES; i++) {
        value += amplitude * noise(p);
        p = rot * p + vec2(2.1, 6.7);
        amplitude *= 0.54;
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
float saturate(float value) {
    return clamp(value, 0.0, 1.0);
}
float ridge(float value, float sharpness) {
    return pow(saturate(1.0 - abs(value * 2.0 - 1.0)), sharpness);
}
float starCell(vec2 uv, float scale, float threshold, float seed) {
    vec2 grid = uv * scale;
    vec2 cell = floor(grid);
    vec2 local = fract(grid) - 0.5;
    float rnd = hash(cell + vec2(seed, seed * 1.73));
    // `active` is reserved by strict GLSL compilers (notably AMD's). NVIDIA
    // drivers accepted it, which hid the portability bug during development.
    float cellEnabled = step(threshold, rnd);
    float size = mix(0.04, 0.16, hash(cell + vec2(seed * 2.11, seed * 0.91)));
    float dist = length(local);
    float core = smoothstep(size, 0.0, dist);
    float glow = smoothstep(size * 3.4, 0.0, dist) * 0.35;
    return cellEnabled * (core + glow);
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
    float t = time * 0.072;
    float height = skyDir.y * 0.5 + 0.5;
    vec3 samplePos = skyDir * vec3(1.78, 1.22, 1.78) * patternScale;
    vec3 warp = vec3(
        triFbm(samplePos * 0.56 + vec3(t * 0.03, 1.4 - t * 0.02, -1.7)),
        triFbm(samplePos * 0.54 + vec3(-1.2 + t * 0.02, t * 0.03, 2.1)),
        triFbm(samplePos * 0.58 + vec3(2.2 - t * 0.02, -1.2, t * 0.01))
    );
    vec3 domain = samplePos + (warp - 0.5) * vec3(0.92, 0.38, 0.92);
    float mainMass = triFbm(domain + vec3(t * 0.09, -t * 0.03, t * 0.06));
    float secondaryMass = triFbm(domain * 1.26 + vec3(-t * 0.05, t * 0.04, -t * 0.03));
    float smoke = triFbm(domain * 1.94 + vec3(t * 0.02, -t * 0.06, t * 0.02));
    float fine = triFbm(domain * 2.82 + vec3(-t * 0.02, t * 0.03, -t * 0.04));
    float micro = triFbm(domain * 4.35 + vec3(t * 0.04, -t * 0.08, t * 0.025));
    float voids = triFbm(domain * 0.62 + vec3(3.1, 1.7, -2.4) + vec3(t * 0.02, 0.0, -t * 0.01));
    float bulk = smoothstep(0.48, 0.82, mainMass * 0.86 + secondaryMass * 0.34);
    float broadMist = smoothstep(0.38, 0.76, mainMass * 0.74 + smoke * 0.26);
    float filament = ridge(secondaryMass * 0.68 + smoke * 0.32, 3.2);
    float fineFilament = ridge(smoke * 0.58 + fine * 0.42, 4.6);
    float swirl = ridge(mainMass * 0.44 + secondaryMass * 0.56, 2.5);
    float lowerBand = smoothstep(0.02, 0.22, height) * (1.0 - smoothstep(0.64, 0.86, height));
    float midBand = smoothstep(0.12, 0.42, height) * (1.0 - smoothstep(0.90, 0.985, height));
    float upperBand = smoothstep(0.42, 0.72, height) * (1.0 - smoothstep(0.995, 1.0, height));
    float edgeDarkness = smoothstep(0.26, 0.82, voids * 0.92 + (1.0 - bulk) * 0.34);
    float microRidges = ridge(fine * 0.55 + micro * 0.45, 6.2);
    float cloudShape = bulk * (0.56 + filament * 0.32 + fineFilament * 0.18 + microRidges * 0.11);
    float wispShape = broadMist * 0.34 + filament * 0.42 + fineFilament * 0.28 + swirl * 0.14;
    float darkCut = smoothstep(0.56, 0.90, voids * 0.86 + (1.0 - broadMist) * 0.26);
    float cloudMask = saturate(cloudShape * lowerBand + wispShape * midBand + wispShape * upperBand * 0.58);
    cloudMask *= 1.0 - darkCut * (0.48 + upperBand * 0.18);
    float columnLift = smoothstep(0.02, 0.14, height) * (1.0 - smoothstep(0.48, 0.70, height));
    float horizonBloom = smoothstep(0.42, 0.88, mainMass * 0.66 + secondaryMass * 0.28) * columnLift;
    vec3 deepNight = vec3(0.020, 0.022, 0.050);
    vec3 nightBlue = vec3(0.065, 0.072, 0.145);
    vec3 mistBody = vec3(0.40, 0.42, 0.54);
    vec3 mistLilac = vec3(0.56, 0.57, 0.71);
    vec3 mistBright = vec3(0.78, 0.79, 0.88);
    vec3 silver = vec3(0.90, 0.91, 0.97);
    vec3 color = mix(deepNight, nightBlue, broadMist * 0.22 + upperBand * 0.08);
    color = mix(color, mistBody, cloudMask * 0.54);
    color = mix(color, mistLilac, filament * 0.32 + fineFilament * 0.18 + cloudMask * 0.16);
    color = mix(color, mistBright, horizonBloom * 0.56 + cloudMask * 0.18);
    color = mix(color, silver, fineFilament * 0.10 + horizonBloom * 0.12);
    color = mix(color, vec3(0.68, 0.70, 0.83), microRidges * cloudMask * 0.13);
    color = mix(color, deepNight * 1.04, edgeDarkness * 0.30);
    float azimuth = atan(skyDir.z, skyDir.x) / 6.2831853;
    vec2 rainUv = vec2(azimuth * 52.0, height * 18.0 + time * 0.34);
    float rainColumns = smoothstep(0.73, 0.98, noise(vec2(floor(rainUv.x), rainUv.y * 0.18)));
    float rainGrain = smoothstep(0.76, 1.0, noise(rainUv * vec2(1.0, 5.2)));
    float rainCurtain = rainColumns * rainGrain * lowerBand * cloudMask * (0.24 + rainStrength * 0.56);
    color = mix(color, vec3(0.31, 0.34, 0.48), rainCurtain * 0.24);
    float flashClock = time * 0.115;
    float flashSeed = hash(vec2(floor(flashClock), 83.17));
    float flashEnvelope = pow(max(0.0, sin(fract(flashClock) * 3.1415926)), 24.0)
        * step(0.91, flashSeed);
    float boltCenter = mix(-0.62, 0.62, hash(vec2(floor(flashClock), 12.4)));
    float boltWarp = (noise(vec2(height * 11.0, floor(flashClock))) - 0.5) * 0.20;
    float bolt = exp(-abs(skyDir.x - boltCenter - boltWarp) * 82.0)
        * smoothstep(0.10, 0.58, height) * (1.0 - smoothstep(0.82, 0.98, height));
    float lightning = flashEnvelope * (0.18 + bolt * 0.82) * cloudMask;
    color += vec3(0.54, 0.59, 0.86) * lightning * 0.46;
    float weatherFade = 1.0 - rainStrength * 0.24;
    float anglePulse = 0.96 + 0.04 * cos(skyAngle * 6.28318 + skyDir.x * 0.84 - skyDir.z * 0.92);
    float cloudAlpha = cloudMask * weatherFade * anglePulse * (0.76 + starBrightness * 0.34);
    cloudAlpha += horizonBloom * 0.18 * weatherFade + rainCurtain * 0.05;
    cloudAlpha = clamp(cloudAlpha, 0.0, 0.88);
    vec2 starUv = skyDir.xz / (abs(skyDir.y) + 0.34);
    float stars = 0.0;
    stars += starCell(starUv + vec2(0.11, 0.37), 52.0, 0.9935, 7.0) * 0.90;
    stars += starCell(starUv + vec2(-0.29, 0.18), 86.0, 0.9968, 19.0) * 0.70;
    stars += starCell(starUv + vec2(0.41, -0.33), 124.0, 0.9983, 37.0) * 0.45;
    float starWindow = saturate(1.0 - cloudAlpha * 1.12);
    stars *= starWindow * weatherFade * starBrightness;
    vec3 finalColor = color + vec3(0.82, 0.85, 0.96) * stars;
    float finalAlpha = clamp(alpha * cloudAlpha + stars * 0.24, 0.0, alpha);
    FragColor = vec4(finalColor, finalAlpha);
}
