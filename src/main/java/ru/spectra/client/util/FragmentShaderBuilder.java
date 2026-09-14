package ru.spectra.client.util;

import ru.spectra.client.Spectra;
import ru.spectra.client.event.ClientInitEvent;
import ru.spectra.client.event.ClientListener;
import ru.spectra.client.render.ShaderProgram;
import ru.spectra.client.resource.ByteArrayResource;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.resource.ResourceRouter;
import java.nio.charset.StandardCharsets;
import org.lwjgl.opengl.GL33;

public class FragmentShaderBuilder implements ClientListener {
    public static final String SHADER_HEADER = """
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

            """;

    public static final String SHADER_FUNCTIONS = """
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

            """;

    public static final String SHADER_MAIN = """
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
                        // The transparent capture target stores premultiplied RGB.
                        // Restore straight alpha before the normal UI blend pass.
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

                        float innerCoverage = 1.0 - smoothstep(
                                -antialias, antialias, dist + thickness);
                        vec3 surfaceRgb = mix(
                                outlineColor.rgb, panelRgb, clamp(innerCoverage, 0.0, 1.0));
                        float surfaceAlpha = mix(
                                outlineColor.a, panelAlpha, clamp(innerCoverage, 0.0, 1.0));
                        fragColor = vec4(surfaceRgb, surfaceAlpha * outerCoverage);
                        break;
                    }
                    case ROUNDED_TEXTURE_MODE:
                    {
                        vec4 textureColor = sampleTex(texIndex, texCoord);
                        vec4 multipliedColor = textureColor * color;
                        vec2 center = gl_FragCoord.xy - meshPosition - (meshSize / 2.0);
                        float dist = sdRoundedBox(center, meshSize / 2.0, radius);
                        float alpha = 1.0 - smoothstep(-1.0, 1.0, dist);
                        fragColor = vec4(multipliedColor.rgb, multipliedColor.a * alpha);
                        break;
                    }
                    case BLUR:
                    {
                        vec2 pos = gl_FragCoord.xy;
                        vec2 blurredPos = pos / resolution;
                        vec4 textureColor = sampleTex(texIndex, blurredPos);
                        vec2 center = pos - meshPosition - (meshSize / 2.0);
                        float dist = sdRoundedBox(center, meshSize / 2.0, radius);
                        float alpha = 1.0 - smoothstep(-softness, softness, dist);
                        fragColor = vec4(textureColor.rgb * color.rgb, color.a * alpha);
                        break;
                    }
                    case CHECKER_MODE:
                    {
                        vec2 local = gl_FragCoord.xy - meshPosition;
                        float cellSize = min(meshSize.x, meshSize.y) / 2.0;
                        ivec2 cell = ivec2(floor(local / cellSize));
                        bool isWhite = (cell.x + cell.y) % 2 == 0;
                        vec3 checkerColor = isWhite ? color.rgb : outlineColor.rgb;
                        float checkerAlpha = isWhite ? color.a : outlineColor.a;
                        vec2 center = local - (meshSize * 0.5);
                        float dist = sdRoundedBox(center, meshSize * 0.5, radius);
                        float mask = 1.0 - smoothstep(-softness, softness, dist);
                        fragColor = vec4(checkerColor, checkerAlpha * mask);
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
                        bool insideArc = end >= start
                                ? (angle >= start && angle <= end)
                                : (angle >= start || angle <= end);
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
                    case OUTER_MASK:
                    {
                        vec4 original = sampleTex(texIndex, texCoord);
                        float mask = sampleTex(maskIndex, texCoord).a;
                        vec3 rgb = original.rgb * (1.0 - mask);
                        float a  = original.a   * (1.0 - mask);

                        fragColor = vec4(rgb, a);
                        break;
                    }
                    case ALPHA_MASK:
                    {
                        vec4 textureColor = sampleTex(texIndex, texCoord);
                        textureColor.a *= color.a;
                        fragColor = textureColor;
                        break;
                    }
                    case MSDF_FONT:
                    {
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
                        fragColor = vec4(color.rgb, color.a * alpha);
                        break;
                    }
                    case RADIAL_ROUNDED_RECTANGLE_MODE:
                    {
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
            """;

    public FragmentShaderBuilder() {
        Spectra.INSTANCE.eventDispatcher().register(ClientInitEvent.class, event -> {
            int maxTextureUnits = Math.max(1, GL33.glGetInteger(GL33.GL_MAX_TEXTURE_IMAGE_UNITS));
            ResourceRouter resources = new ResourceRouter("/", ClasspathResource::new);
            ShaderProgram shader = new ShaderProgram(
                    new ByteArrayResource(buildSource(maxTextureUnits).getBytes(StandardCharsets.UTF_8)),
                    resources.route("shaders/core.vsh")
            );
            Spectra.INSTANCE.drawEngine(new DrawEngine(shader, maxTextureUnits));
            Spectra.INSTANCE.windowController().init();
        });
    }

    public static String buildSource(int textureUnits) {
        StringBuilder source = new StringBuilder(SHADER_HEADER);
        source.append("uniform sampler2D textureSampler[")
                .append(textureUnits)
                .append("];\n")
                .append("uniform vec2 resolution;\n\n")
                .append(SHADER_FUNCTIONS);
        appendSampleTex(source, textureUnits);
        appendSampleTexSize(source, textureUnits);
        return source.append(SHADER_MAIN).toString();
    }

    public static void appendSampleTex(StringBuilder source, int textureUnits) {
        source.append("vec4 sampleTex(int id, vec2 uv) {\n");
        for (int index = 0; index < textureUnits; index++) {
            source.append("    if (id == ")
                    .append(index)
                    .append(") return texture(textureSampler[")
                    .append(index)
                    .append("], uv);\n");
        }
        source.append("    return vec4(1.0, 0.0, 1.0, 1.0);\n}\n\n");
    }

    public static void appendSampleTexSize(StringBuilder source, int textureUnits) {
        source.append("ivec2 sampleTexSize(int id) {\n");
        for (int index = 0; index < textureUnits; index++) {
            source.append("    if (id == ")
                    .append(index)
                    .append(") return textureSize(textureSampler[")
                    .append(index)
                    .append("], 0);\n");
        }
        source.append("    return ivec2(1, 1);\n}\n\n");
    }
}
