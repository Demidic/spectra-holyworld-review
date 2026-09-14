#version 330 core

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform vec2 texSize;
uniform float time;
uniform float intensity;
uniform float speed;
uniform float length;
uniform float smoke;
uniform float activity;
uniform float persistence;
uniform float slash;
uniform float slashDir;
uniform float swingHand;
uniform vec2 camShift;
uniform vec4 glowColor;

in vec2 TexCoord;
out vec4 OutColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
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

float layeredNoise(vec2 p) {
    float low = noise(p);
    float high = noise(p * 2.07 + vec2(5.2, 1.7));
    return low * 0.68 + high * 0.32;
}

void main() {
    float maskCoverage = texture(Sampler2, vec2(0.5)).r;
    if (maskCoverage > 0.22) {
        OutColor = vec4(0.0);
        return;
    }

    vec2 px = 1.0 / max(texSize, vec2(1.0));
    vec2 flow = camShift * vec2(0.72, -0.72);
    // Reproject the slash only on the half of the screen that contains the
    // actively swinging hand. Applying this displacement to the full
    // framebuffer made smoke around the idle off-hand move as well.
    float activeHandSide = smoothstep(
        -0.08,
        0.08,
        (TexCoord.x - 0.5) * swingHand
    );
    flow.x += slashDir * swingHand * slash * 0.012 * activeHandSide;
    flow.y -= (0.0015 + 0.0045 * length) * max(speed, 0.1);

    // Two independently moving noise fields bend the old trail into small,
    // slow curls. The displacement is measured in pixels so it stays stable
    // at different resolutions and does not turn into a full-screen wobble.
    float curlX = layeredNoise(
        TexCoord * vec2(8.5, 6.0) + vec2(time * speed * 0.075, -time * speed * 0.11)
    ) - 0.5;
    float curlY = layeredNoise(
        TexCoord.yx * vec2(7.0, 9.0) + vec2(-time * speed * 0.09, time * speed * 0.065) + 13.4
    ) - 0.5;
    vec2 curl = vec2(curlX, curlY) * px * mix(1.8, 4.8, length);
    vec2 previousUv = clamp(TexCoord + flow + curl, vec2(0.0), vec2(1.0));

    // A tiny cross-filter acts as controlled diffusion. It removes the
    // staircase pattern left by temporal reprojection without washing away
    // the internal wisps.
    vec2 diffusion = px * mix(0.65, 1.35, length);
    vec4 previous = texture(Sampler0, previousUv) * 0.48;
    previous += texture(Sampler0, previousUv + vec2(diffusion.x, 0.0)) * 0.13;
    previous += texture(Sampler0, previousUv - vec2(diffusion.x, 0.0)) * 0.13;
    previous += texture(Sampler0, previousUv + vec2(0.0, diffusion.y)) * 0.13;
    previous += texture(Sampler0, previousUv - vec2(0.0, diffusion.y)) * 0.13;

    float mask = texture(Sampler1, TexCoord).r;
    float nearEdge = 0.0;
    nearEdge += texture(Sampler1, TexCoord + vec2(px.x, 0.0)).r;
    nearEdge += texture(Sampler1, TexCoord - vec2(px.x, 0.0)).r;
    nearEdge += texture(Sampler1, TexCoord + vec2(0.0, px.y)).r;
    nearEdge += texture(Sampler1, TexCoord - vec2(0.0, px.y)).r;
    nearEdge *= 0.25;

    float diagonalEdge = 0.0;
    diagonalEdge += texture(Sampler1, TexCoord + px).r;
    diagonalEdge += texture(Sampler1, TexCoord - px).r;
    diagonalEdge += texture(Sampler1, TexCoord + vec2(px.x, -px.y)).r;
    diagonalEdge += texture(Sampler1, TexCoord + vec2(-px.x, px.y)).r;
    diagonalEdge *= 0.25;
    float edge = smoothstep(0.015, 0.72, max(nearEdge, diagonalEdge) - mask * 0.58);

    float turbulence = layeredNoise(
        TexCoord * vec2(13.0, 9.0)
        + vec2(time * speed * 0.14, -time * speed * 0.23)
    );
    float filament = smoothstep(0.18, 0.86, turbulence);
    float source = edge * mix(0.38, 1.0, filament);
    float localActivity = mix(
        activity,
        activity * activeHandSide,
        smoothstep(0.02, 0.2, slash)
    );
    source += mask * (0.012 + localActivity * 0.026);
    source *= smoke * (0.72 + intensity * 0.38);

    float retained = previous.a * clamp(persistence, 0.0, 0.985);
    retained *= mix(0.972, 0.994, turbulence);
    retained *= 1.0 - smoothstep(0.42, 0.92, curlX + curlY + 0.54) * 0.035;

    // Soft union avoids the hard, plastic-looking borders produced by max().
    float alpha = clamp(1.0 - (1.0 - retained) * (1.0 - source), 0.0, 1.0);
    float freshSmoke = clamp(source * 1.55, 0.0, 1.0);
    vec3 color = mix(previous.rgb, glowColor.rgb, freshSmoke);
    color *= mix(0.78, 1.08, turbulence);
    OutColor = vec4(color, alpha);
}
