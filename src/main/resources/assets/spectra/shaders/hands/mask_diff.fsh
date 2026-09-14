#version 330 core

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec2 texSize;

in vec2 TexCoord;
out vec4 OutColor;

float handSignal(vec2 uv) {
    float beforeHand = texture(Sampler0, uv).r;
    float afterHand = texture(Sampler1, uv).r;
    float depthDifference = max(beforeHand - afterHand, 0.0);
    return smoothstep(0.00001, 0.00045, depthDifference);
}

void main() {
    // First-person geometry is always closer than the depth already stored by
    // the world. Color grading, animated skies and translucent surfaces cannot
    // affect this signal, which keeps them out of the hand mask.
    vec2 px = 1.0 / max(texSize, vec2(1.0));
    float center = handSignal(TexCoord);
    float cardinal =
        handSignal(TexCoord + vec2(px.x, 0.0)) +
        handSignal(TexCoord - vec2(px.x, 0.0)) +
        handSignal(TexCoord + vec2(0.0, px.y)) +
        handSignal(TexCoord - vec2(0.0, px.y));
    float diagonal =
        handSignal(TexCoord + px) +
        handSignal(TexCoord - px) +
        handSignal(TexCoord + vec2(px.x, -px.y)) +
        handSignal(TexCoord + vec2(-px.x, px.y));

    // A compact 3x3 reconstruction gives boundary pixels fractional coverage.
    // This is effectively a lightweight mask-only anti-aliasing pass and does
    // not blur the procedural material inside the hand.
    float coverage = (center * 4.0 + cardinal + diagonal * 0.7071) / 10.8284;
    float changed = smoothstep(0.075, 0.925, coverage);

    // Framebuffer UV starts at the bottom. First-person hands and held items
    // are rendered in the lower portion of the frame. Keeping this hard guard
    // here prevents unrelated world changes from ever becoming hand pixels.
    float firstPersonRegion = 1.0 - smoothstep(0.52, 0.78, TexCoord.y);
    float isHand = changed * firstPersonRegion;

    OutColor = vec4(isHand, isHand, isHand, isHand);
}
