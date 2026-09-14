#version 330 core

#define COLOR_MODE 0
#define TEXTURE_MODE 1
#define ROUNDED_RECTANGLE_MODE 2
#define ROUNDED_TEXTURE_MODE 3
#define BLUR 4
#define CHECKER_MODE 5
#define CIRCLE_MODE 6
#define OUTER_MASK 7
#define ALPHA_MASK 8
#define MSDF_FONT 9
#define RADIAL_ROUNDED_RECTANGLE_MODE 10
#define GLASS_PANEL_MODE 11
#define SOFT_TEXTURE_MODE 12

in vec2 meshPosition;
in vec2 meshSize;
in vec2 texCoord;
in vec4 radius;
in vec4 color;
in vec4 outlineColor;
in float thickness;
in float softness;

flat in int texIndex;
flat in int drawMode;
flat in int maskIndex;

out vec4 fragColor;

uniform sampler2D textureSampler[32];
uniform vec2 resolution;


float sdRoundedBox(in vec2 point, in vec2 size, in vec4 r) {
    r.xy = (point.x > 0.0) ? r.xy : r.zw;
    r.x = (point.y > 0.0) ? r.x : r.y;
    vec2 q = abs(point) - size + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

float median(vec3 color) {
    return max(min(color.r, color.g), min(max(color.r, color.g), color.b));
}
vec4 sampleTex(int id, vec2 uv);
ivec2 sampleTexSize(int id);

vec4 sampleTex(int id, vec2 uv) {
    if (id == 0)  return texture(textureSampler[0],  uv);
    if (id == 1)  return texture(textureSampler[1],  uv);
    if (id == 2)  return texture(textureSampler[2],  uv);
    if (id == 3)  return texture(textureSampler[3],  uv);
    if (id == 4)  return texture(textureSampler[4],  uv);
    if (id == 5)  return texture(textureSampler[5],  uv);
    if (id == 6)  return texture(textureSampler[6],  uv);
    if (id == 7)  return texture(textureSampler[7],  uv);
    if (id == 8)  return texture(textureSampler[8],  uv);
    if (id == 9)  return texture(textureSampler[9],  uv);
    if (id == 10) return texture(textureSampler[10], uv);
    if (id == 11) return texture(textureSampler[11], uv);
    if (id == 12) return texture(textureSampler[12], uv);
    if (id == 13) return texture(textureSampler[13], uv);
    if (id == 14) return texture(textureSampler[14], uv);
    if (id == 15) return texture(textureSampler[15], uv);
    if (id == 16) return texture(textureSampler[16], uv);
    if (id == 17) return texture(textureSampler[17], uv);
    if (id == 18) return texture(textureSampler[18], uv);
    if (id == 19) return texture(textureSampler[19], uv);
    if (id == 20) return texture(textureSampler[20], uv);
    if (id == 21) return texture(textureSampler[21], uv);
    if (id == 22) return texture(textureSampler[22], uv);
    if (id == 23) return texture(textureSampler[23], uv);
    if (id == 24) return texture(textureSampler[24], uv);
    if (id == 25) return texture(textureSampler[25], uv);
    if (id == 26) return texture(textureSampler[26], uv);
    if (id == 27) return texture(textureSampler[27], uv);
    if (id == 28) return texture(textureSampler[28], uv);
    if (id == 29) return texture(textureSampler[29], uv);
    if (id == 30) return texture(textureSampler[30], uv);
    if (id == 31) return texture(textureSampler[31], uv);
    return vec4(1.0, 0.0, 1.0, 1.0);
}

ivec2 sampleTexSize(int id) {
    if (id == 0)  return textureSize(textureSampler[0],  0);
    if (id == 1)  return textureSize(textureSampler[1],  0);
    if (id == 2)  return textureSize(textureSampler[2],  0);
    if (id == 3)  return textureSize(textureSampler[3],  0);
    if (id == 4)  return textureSize(textureSampler[4],  0);
    if (id == 5)  return textureSize(textureSampler[5],  0);
    if (id == 6)  return textureSize(textureSampler[6],  0);
    if (id == 7)  return textureSize(textureSampler[7],  0);
    if (id == 8)  return textureSize(textureSampler[8],  0);
    if (id == 9)  return textureSize(textureSampler[9],  0);
    if (id == 10) return textureSize(textureSampler[10], 0);
    if (id == 11) return textureSize(textureSampler[11], 0);
    if (id == 12) return textureSize(textureSampler[12], 0);
    if (id == 13) return textureSize(textureSampler[13], 0);
    if (id == 14) return textureSize(textureSampler[14], 0);
    if (id == 15) return textureSize(textureSampler[15], 0);
    if (id == 16) return textureSize(textureSampler[16], 0);
    if (id == 17) return textureSize(textureSampler[17], 0);
    if (id == 18) return textureSize(textureSampler[18], 0);
    if (id == 19) return textureSize(textureSampler[19], 0);
    if (id == 20) return textureSize(textureSampler[20], 0);
    if (id == 21) return textureSize(textureSampler[21], 0);
    if (id == 22) return textureSize(textureSampler[22], 0);
    if (id == 23) return textureSize(textureSampler[23], 0);
    if (id == 24) return textureSize(textureSampler[24], 0);
    if (id == 25) return textureSize(textureSampler[25], 0);
    if (id == 26) return textureSize(textureSampler[26], 0);
    if (id == 27) return textureSize(textureSampler[27], 0);
    if (id == 28) return textureSize(textureSampler[28], 0);
    if (id == 29) return textureSize(textureSampler[29], 0);
    if (id == 30) return textureSize(textureSampler[30], 0);
    if (id == 31) return textureSize(textureSampler[31], 0);
    return ivec2(1, 1);
}
void main() {
    switch (drawMode) {
        case COLOR_MODE:
        {
            fragColor = color;
            break;
        }
        case TEXTURE_MODE:
        {
            vec4 textureColor = sampleTex(texIndex, texCoord);

            if (textureColor.a == 0.0) {
                discard;
            }

            fragColor = textureColor * color;
            break;
        }
        case SOFT_TEXTURE_MODE:
        {
            vec2 radiusUv = softness / vec2(sampleTexSize(texIndex));
            vec2 innerUv = radiusUv * 0.5;
            vec4 blurred = sampleTex(texIndex, texCoord) * 0.20;
            blurred += sampleTex(texIndex, texCoord + vec2( innerUv.x, 0.0)) * 0.10;
            blurred += sampleTex(texIndex, texCoord + vec2(-innerUv.x, 0.0)) * 0.10;
            blurred += sampleTex(texIndex, texCoord + vec2(0.0,  innerUv.y)) * 0.10;
            blurred += sampleTex(texIndex, texCoord + vec2(0.0, -innerUv.y)) * 0.10;
            blurred += sampleTex(texIndex, texCoord + vec2( radiusUv.x, 0.0)) * 0.05;
            blurred += sampleTex(texIndex, texCoord + vec2(-radiusUv.x, 0.0)) * 0.05;
            blurred += sampleTex(texIndex, texCoord + vec2(0.0,  radiusUv.y)) * 0.05;
            blurred += sampleTex(texIndex, texCoord + vec2(0.0, -radiusUv.y)) * 0.05;
            blurred += sampleTex(texIndex, texCoord + innerUv) * 0.05;
            blurred += sampleTex(texIndex, texCoord - innerUv) * 0.05;
            blurred += sampleTex(texIndex, texCoord + vec2(innerUv.x, -innerUv.y)) * 0.05;
            blurred += sampleTex(texIndex, texCoord + vec2(-innerUv.x, innerUv.y)) * 0.05;
            if (blurred.a <= 0.001) {
                discard;
            }
            fragColor = vec4((blurred.rgb / blurred.a) * color.rgb,
                    blurred.a * color.a);
            break;
        }
        case ROUNDED_RECTANGLE_MODE:
        {
            vec2 center = gl_FragCoord.xy - meshPosition - (meshSize / 2.0);
            float dist = sdRoundedBox(center, meshSize / 2.0, radius);
            float smoothedAlpha = 1.0 - smoothstep(-1.0, thickness > 0. ? 1. : softness + 1., dist);
            float smoothedborderAlpha = (1.0 - smoothstep(-softness, softness, dist));
            float borderAlpha = 1.0 - smoothstep(thickness - 2.0, thickness, abs(dist));

            if (smoothedAlpha < 0.49 && thickness > 0.) {
                fragColor = vec4(outlineColor.rgb, outlineColor.a * smoothedborderAlpha);
            } else {
                vec4 basicColor = vec4(color.rgb, color.a * smoothedAlpha);

                fragColor = mix(vec4(color.rgb, 0.), mix(basicColor, thickness > 0. ? outlineColor : basicColor,
                outlineColor.a *  borderAlpha), smoothedAlpha);
            }
            break;
        }
        case GLASS_PANEL_MODE:
        {
            vec2 pos = gl_FragCoord.xy;
            vec2 center = pos - meshPosition - (meshSize / 2.0);
            float dist = sdRoundedBox(center, meshSize / 2.0, radius);
            float antialias = max(fwidth(dist), 0.85);
            float outerCoverage = 1.0 - smoothstep(-antialias, antialias, dist);
            if (outerCoverage <= 0.0) {
                discard;
            }

            bool hasBlur = maskIndex != 0;
            vec3 panelRgb = color.rgb;
            float animationAlpha = max(outlineColor.a, color.a);
            float tintStrength = animationAlpha > 0.0001
                    ? clamp(color.a / animationAlpha, 0.0, 1.0)
                    : 0.0;
            float panelAlpha = hasBlur ? animationAlpha : color.a;
            if (hasBlur) {
                vec3 blurredRgb = sampleTex(texIndex, pos / resolution).rgb;
                panelRgb = mix(blurredRgb, color.rgb, tintStrength);
            }

            float innerCoverage = 1.0 - smoothstep(-antialias, antialias, dist + thickness);
            vec3 surfaceRgb = mix(outlineColor.rgb, panelRgb, clamp(innerCoverage, 0.0, 1.0));
            float surfaceAlpha = mix(outlineColor.a, panelAlpha, clamp(innerCoverage, 0.0, 1.0));
            fragColor = vec4(surfaceRgb, surfaceAlpha * outerCoverage);
            break;
        }
        case ROUNDED_TEXTURE_MODE:
        {
            vec4 textureColor = sampleTex(texIndex, texCoord);
            vec4 textureColorMultipliedByInputColor = textureColor * color;

            vec2 center = gl_FragCoord.xy - meshPosition - (meshSize / 2.0);
            float dist = sdRoundedBox(center, meshSize / 2.0, radius);
            float alpha = 1.0 - smoothstep(-1.0, 1.0, dist);

            fragColor = vec4(textureColorMultipliedByInputColor.rgb, textureColorMultipliedByInputColor.a * alpha);
            break;
        }
        case BLUR:
        {
            vec2 pos = gl_FragCoord.xy;
            vec2 blurredPos = pos / resolution;
            vec4 textureColor = sampleTex(texIndex, blurredPos);
            vec3 blurredColor = textureColor.rgb;

            vec2 center = pos - meshPosition - (meshSize / 2.0);
            float dist = sdRoundedBox(center, meshSize / 2.0, radius);
            float alpha = 1.0 - smoothstep(-softness, softness, dist);

            fragColor = vec4(blurredColor.rgb * color.rgb, color.a * alpha);
            break;
        }
        case CHECKER_MODE: {
            vec2 local = gl_FragCoord.xy - meshPosition;

            float rows = 2.0;

            float cellSize = min(meshSize.x, meshSize.y) / rows;

            ivec2 cell = ivec2(floor(local / cellSize));

            bool isWhite = (cell.x + cell.y) % 2 == 0;

            vec3 checkerColor = isWhite ? color.rgb : outlineColor.rgb;
            float alpha = isWhite ? color.a : outlineColor.a;

            vec2 center = local - (meshSize * 0.5);
            float dist = sdRoundedBox(center, meshSize * 0.5, radius);
            float mask = 1.0 - smoothstep(-softness, softness, dist);

            fragColor = vec4(checkerColor, alpha * mask);
            break;
        }
        case CIRCLE_MODE:{
            vec2 center = gl_FragCoord.xy - meshPosition - (meshSize / 2.0);
            float len = length(center);
            float dist = len - radius.x;
            if (radius.y > 0.0) {
                dist = max(dist, radius.y - len);
            }
            float smoothedAlpha = 1.0 - smoothstep(-1.0, thickness > 0. ? 1. : softness + 1., dist);
            float smoothedborderAlpha = (1.0 - smoothstep(-softness, softness, dist));
            float borderAlpha = 1.0 - smoothstep(thickness - 2.0, thickness, abs(dist));

            float angle = atan(center.y, center.x);
            if (angle < 0.0) angle += 6.28318530718;
            float start = texCoord.x;
            float end = texCoord.y;
            bool insideArc = end >= start ? (angle >= start && angle <= end) : (angle >= start || angle <= end);
            if (!insideArc) discard;

            if (smoothedAlpha < 0.49 && thickness > 0.) {
                fragColor = vec4(outlineColor.rgb, outlineColor.a * smoothedborderAlpha);
            } else {
                vec4 basicColor = vec4(color.rgb, color.a * smoothedAlpha);

                fragColor = mix(vec4(color.rgb, 0.), mix(basicColor, thickness > 0. ? outlineColor : basicColor,
                outlineColor.a *  borderAlpha), smoothedAlpha);
            }
            break;
        }
        case OUTER_MASK: {
            vec4 original = sampleTex(texIndex, texCoord);
            float mask = sampleTex(maskIndex, texCoord).a;

            vec3 rgb = original.rgb * color.rgb * (1.0 - mask);
            float a  = original.a * color.a * (1.0 - mask);

            fragColor = vec4(rgb, a);
            break;
        }
        case ALPHA_MASK: {
            vec4 textureColor = sampleTex(texIndex, texCoord);
            textureColor.a *= color.a;
            fragColor = textureColor;
            break;
        }
        case MSDF_FONT: {
            // Integrate four sub-pixel samples: the >=2 range protects atlas
            // padding per sample, without narrowing the full-pixel coverage edge.
            // All bundled MSDF/MTSDF and SVG SDF atlases use a 10-texel range.
            vec2 dx = dFdx(texCoord);
            vec2 dy = dFdy(texCoord);
            vec2 h = max((abs(dx) + abs(dy)) * vec2(sampleTexSize(texIndex)), vec2(0.0001));
            float pixels = max(10.0 * (1.0 / h.x + 1.0 / h.y), 2.0);
            vec2 a = (dx + dy) * 0.25;
            vec2 b = (dx - dy) * 0.25;
            float alpha = 0.25 * (
                smoothstep(-softness, softness, (median(sampleTex(texIndex, texCoord + a).rgb) - 0.5 + thickness) * pixels) +
                smoothstep(-softness, softness, (median(sampleTex(texIndex, texCoord - a).rgb) - 0.5 + thickness) * pixels) +
                smoothstep(-softness, softness, (median(sampleTex(texIndex, texCoord + b).rgb) - 0.5 + thickness) * pixels) +
                smoothstep(-softness, softness, (median(sampleTex(texIndex, texCoord - b).rgb) - 0.5 + thickness) * pixels));
            vec4 outColor = vec4(color.rgb, color.a * alpha);
            fragColor = outColor;
            break;
        }
        case RADIAL_ROUNDED_RECTANGLE_MODE: {
            vec2 center = gl_FragCoord.xy - meshPosition - (meshSize / 2.0);
            float dist = sdRoundedBox(center, meshSize / 2.0, radius);
            float alpha = 1.0 - smoothstep(-1.0, 1.0, dist);
            vec2 normalized = center / (meshSize / 2.0);
            float radial = clamp(length(normalized), 0.0, 1.0);
            vec4 gradientColor = mix(color, outlineColor, radial);

            fragColor = vec4(gradientColor.rgb, gradientColor.a * alpha);
            break;
        }
    }
}

