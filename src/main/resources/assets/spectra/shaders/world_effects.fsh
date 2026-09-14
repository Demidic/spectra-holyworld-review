#version 330 core

out vec4 fragColor;
in vec2 texCoord;

#define MAX_EFFECTS 20
#define BLOOD_DROPS 9

uniform sampler2D uSource;
uniform sampler2D uBlur;
uniform sampler2D uDepth;
uniform mat4 uInverseViewProjection;
uniform mat4 uViewProjection;
uniform mat4 uViewMatrix;
uniform vec2 uResolution;
uniform float uTime;
uniform float uIntensity;
uniform float uScreenShake;
uniform int uEffectCount;
uniform vec4 uEffects[MAX_EFFECTS];
uniform vec4 uParams[MAX_EFFECTS];
uniform vec4 uDirections[MAX_EFFECTS];
uniform vec4 uAnchors[MAX_EFFECTS];

float hash11(float value) {
    return fract(sin(value * 127.1) * 43758.5453123);
}

float easeOutCubic(float value) {
    float inverse = 1.0 - clamp(value, 0.0, 1.0);
    return 1.0 - inverse * inverse * inverse;
}

float ringMask(float distanceValue, float radius, float halfWidth) {
    return 1.0 - smoothstep(halfWidth, halfWidth * 1.8,
            abs(distanceValue - radius));
}

vec3 safeDirection(vec3 value, vec3 fallback) {
    float magnitude = length(value);
    return magnitude > 0.00001 ? value / magnitude : fallback;
}

vec3 reconstructPosition(vec2 uv, float depth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 relative = uInverseViewProjection * clip;
    return relative.xyz / max(abs(relative.w), 0.000001) * sign(relative.w);
}

float raySphere(vec3 rayDirection, vec3 center, float radius) {
    vec3 originToCenter = -center;
    float projection = dot(originToCenter, rayDirection);
    float discriminant = projection * projection
            - (dot(originToCenter, originToCenter) - radius * radius);
    if (discriminant < 0.0) {
        return -1.0;
    }
    float root = sqrt(discriminant);
    float nearHit = -dot(-center, rayDirection) - root;
    float farHit = -dot(-center, rayDirection) + root;
    if (nearHit > 0.0) {
        return nearHit;
    }
    return farHit > 0.0 ? farHit : -1.0;
}

float rayEllipsoid(vec3 rayDirection, vec3 center, vec3 axis,
                   float halfLength, float radius, out vec3 worldNormal) {
    vec3 forward = safeDirection(axis, vec3(0.0, 1.0, 0.0));
    vec3 reference = abs(forward.y) < 0.88
            ? vec3(0.0, 1.0, 0.0)
            : vec3(1.0, 0.0, 0.0);
    vec3 right = safeDirection(cross(forward, reference), vec3(1.0, 0.0, 0.0));
    vec3 up = safeDirection(cross(right, forward), vec3(0.0, 0.0, 1.0));
    vec3 relativeOrigin = -center;
    vec3 localOrigin = vec3(
            dot(relativeOrigin, right) / radius,
            dot(relativeOrigin, up) / radius,
            dot(relativeOrigin, forward) / halfLength
    );
    vec3 localDirection = vec3(
            dot(rayDirection, right) / radius,
            dot(rayDirection, up) / radius,
            dot(rayDirection, forward) / halfLength
    );
    float quadraticA = dot(localDirection, localDirection);
    float quadraticB = dot(localOrigin, localDirection);
    float quadraticC = dot(localOrigin, localOrigin) - 1.0;
    float discriminant = quadraticB * quadraticB - quadraticA * quadraticC;
    if (discriminant < 0.0 || quadraticA < 0.000001) {
        worldNormal = vec3(0.0, 1.0, 0.0);
        return -1.0;
    }
    float root = sqrt(discriminant);
    float nearHit = (-quadraticB - root) / quadraticA;
    float farHit = (-quadraticB + root) / quadraticA;
    float hitDistance = nearHit > 0.0 ? nearHit : farHit;
    if (hitDistance <= 0.0) {
        worldNormal = vec3(0.0, 1.0, 0.0);
        return -1.0;
    }
    vec3 localHit = localOrigin + localDirection * hitDistance;
    worldNormal = safeDirection(
            right * (localHit.x / radius)
            + up * (localHit.y / radius)
            + forward * (localHit.z / halfLength),
            vec3(0.0, 1.0, 0.0)
    );
    return hitDistance;
}

float smoothMinimum(float first, float second, float radius) {
    float blend = clamp(0.5 + 0.5 * (second - first) / radius, 0.0, 1.0);
    return mix(second, first, blend) - radius * blend * (1.0 - blend);
}

float bloodBlobDistance(vec3 point, vec3 center,
                        vec3 side, vec3 up, vec3 forward,
                        float radius, vec4 variation) {
    float coreRadius = radius * mix(0.80, 0.96, variation.x);
    float distanceValue = length(point - center) - coreRadius;

    vec3 firstCenter = center
            + side * (variation.x - 0.5) * radius * 0.82
            + forward * (variation.y - 0.5) * radius * 0.48;
    float firstRadius = radius * mix(0.56, 0.74, variation.z);
    distanceValue = smoothMinimum(distanceValue,
            length(point - firstCenter) - firstRadius, radius * 0.34);

    vec3 secondCenter = center
            + up * (variation.z - 0.5) * radius * 0.72
            - side * (variation.w - 0.5) * radius * 0.66;
    float secondRadius = radius * mix(0.50, 0.70, variation.y);
    distanceValue = smoothMinimum(distanceValue,
            length(point - secondCenter) - secondRadius, radius * 0.31);

    vec3 thirdCenter = center
            - forward * mix(0.28, 0.58, variation.w) * radius
            + side * (variation.y - 0.5) * radius * 0.46
            - up * (variation.x - 0.5) * radius * 0.34;
    float thirdRadius = radius * mix(0.42, 0.60, variation.w);
    return smoothMinimum(distanceValue,
            length(point - thirdCenter) - thirdRadius, radius * 0.27);
}

float rayBloodBlob(vec3 rayDirection, vec3 center,
                   vec3 side, vec3 up, vec3 forward,
                   float radius, vec4 variation, out vec3 worldNormal) {
    // The bounding sphere prevents a full ray march on pixels that cannot
    // possibly see this tiny drop.
    float boundRadius = radius * 1.72;
    float projectedCenter = dot(center, rayDirection);
    float discriminant = projectedCenter * projectedCenter
            - (dot(center, center) - boundRadius * boundRadius);
    if (discriminant < 0.0) {
        worldNormal = vec3(0.0, 1.0, 0.0);
        return -1.0;
    }

    float root = sqrt(discriminant);
    float startDistance = max(projectedCenter - root, 0.001);
    float endDistance = projectedCenter + root;
    if (endDistance <= startDistance) {
        worldNormal = vec3(0.0, 1.0, 0.0);
        return -1.0;
    }

    float hitDistance = startDistance;
    for (int step = 0; step < 14; step++) {
        vec3 samplePoint = rayDirection * hitDistance;
        float distanceValue = bloodBlobDistance(samplePoint, center,
                side, up, forward, radius, variation);
        if (distanceValue < max(0.0012, radius * 0.018)) {
            float epsilon = max(0.0014, radius * 0.025);
            vec3 offsetX = vec3(epsilon, 0.0, 0.0);
            vec3 offsetY = vec3(0.0, epsilon, 0.0);
            vec3 offsetZ = vec3(0.0, 0.0, epsilon);
            worldNormal = safeDirection(vec3(
                    bloodBlobDistance(samplePoint + offsetX, center,
                            side, up, forward, radius, variation)
                            - bloodBlobDistance(samplePoint - offsetX, center,
                            side, up, forward, radius, variation),
                    bloodBlobDistance(samplePoint + offsetY, center,
                            side, up, forward, radius, variation)
                            - bloodBlobDistance(samplePoint - offsetY, center,
                            side, up, forward, radius, variation),
                    bloodBlobDistance(samplePoint + offsetZ, center,
                            side, up, forward, radius, variation)
                            - bloodBlobDistance(samplePoint - offsetZ, center,
                            side, up, forward, radius, variation)
            ), vec3(0.0, 1.0, 0.0));
            return hitDistance;
        }
        hitDistance += max(distanceValue * 0.72, radius * 0.025);
        if (hitDistance > endDistance) {
            break;
        }
    }

    worldNormal = vec3(0.0, 1.0, 0.0);
    return -1.0;
}

vec2 projectPosition(vec3 position, out float clipW) {
    vec4 clip = uViewProjection * vec4(position, 1.0);
    clipW = clip.w;
    vec2 ndc = clip.xy / max(abs(clip.w), 0.000001) * sign(clip.w);
    return ndc * 0.5 + 0.5;
}

vec3 screenSpaceReflection(vec3 surfacePosition, vec3 incident,
                           vec3 surfaceNormal, vec2 fallbackUv) {
    vec3 reflectionDirection = safeDirection(
            reflect(incident, surfaceNormal),
            vec3(0.0, 1.0, 0.0));
    vec2 lastUv = fallbackUv;
    for (int step = 1; step <= 10; step++) {
        float distanceValue = 0.16 + float(step) * 0.42;
        vec3 probe = surfacePosition + vec3(0.0, 0.035, 0.0)
                + reflectionDirection * distanceValue;
        float clipW;
        vec2 probeUv = projectPosition(probe, clipW);
        if (clipW <= 0.0 || probeUv.x <= 0.002 || probeUv.x >= 0.998
                || probeUv.y <= 0.002 || probeUv.y >= 0.998) {
            break;
        }
        lastUv = probeUv;
        float probeDepth = texture(uDepth, probeUv).r;
        if (probeDepth < 0.99999) {
            vec3 depthPosition = reconstructPosition(probeUv, probeDepth);
            float thickness = 0.24 + distanceValue * 0.075;
            if (length(depthPosition - probe) < thickness) {
                return texture(uSource, probeUv).rgb;
            }
        }
    }
    return mix(texture(uSource, lastUv).rgb,
            texture(uBlur, lastUv).rgb, 0.22);
}

vec3 glassSample(vec2 uv, vec2 direction, float separation) {
    vec2 clampedUv = clamp(uv, vec2(0.001), vec2(0.999));
    vec2 spectral = direction * separation;
    return vec3(
            texture(uSource, clamp(clampedUv + spectral, vec2(0.001), vec2(0.999))).r,
            texture(uSource, clampedUv).g,
            texture(uSource, clamp(clampedUv - spectral, vec2(0.001), vec2(0.999))).b
    );
}

void main() {
    vec2 shake = vec2(0.0);
    for (int index = 0; index < MAX_EFFECTS; index++) {
        if (index >= uEffectCount) {
            break;
        }
        int type = int(floor(uEffects[index].w + 0.5));
        if (type > 1) {
            continue;
        }
        float progress = clamp(uParams[index].x, 0.0, 1.0);
        float strength = uParams[index].y;
        float phase = uParams[index].z * 6.28318530718;
        float decay = pow(1.0 - progress, 2.0);
        float frequency = type == 0 ? 43.0 : 56.0;
        shake += vec2(
                sin(uTime * frequency + phase),
                cos(uTime * frequency * 0.83 + phase * 1.37)
        ) * (0.0028 * strength * decay);
    }

    vec2 baseUv = clamp(texCoord + shake * uIntensity * uScreenShake,
            vec2(0.001), vec2(0.999));
    vec3 color = texture(uSource, baseUv).rgb;
    float depth = texture(uDepth, baseUv).r;
    vec3 farPosition = reconstructPosition(baseUv, 0.99999);
    vec3 rayDirection = safeDirection(farPosition, vec3(0.0, 0.0, -1.0));
    vec3 scenePosition = reconstructPosition(baseUv, depth);
    float sceneDistance = depth >= 0.99999
            ? 1000000.0
            : max(0.0, dot(scenePosition, rayDirection));
    vec3 sceneSurfaceNormal = safeDirection(cross(
            dFdx(scenePosition), dFdy(scenePosition)), vec3(0.0, 1.0, 0.0));
    float upwardSurface = smoothstep(
            0.42, 0.78, abs(sceneSurfaceNormal.y));
    float bloodPuddleMask = 0.0;
    float bloodPuddleVariation = 0.0;

    for (int index = 0; index < MAX_EFFECTS; index++) {
        if (index >= uEffectCount) {
            break;
        }

        vec3 center = uEffects[index].xyz;
        int type = int(floor(uEffects[index].w + 0.5));
        float progress = clamp(uParams[index].x, 0.0, 1.0);
        float strength = uParams[index].y * uIntensity;
        float seed = uParams[index].z;

        if (type == 0) {
            // A real world-space refractive sphere. The depth comparison keeps
            // it behind blocks whenever the expanding shell is occluded.
            float radius = mix(0.18, 7.8, easeOutCubic(progress));
            float hitDistance = raySphere(rayDirection, center, radius);
            if (hitDistance <= 0.0 || hitDistance > sceneDistance + 0.025) {
                continue;
            }

            vec3 hitPosition = rayDirection * hitDistance;
            vec3 normal = safeDirection(hitPosition - center, vec3(0.0, 1.0, 0.0));
            vec3 viewNormal = safeDirection(mat3(uViewMatrix) * normal,
                    vec3(0.0, 0.0, 1.0));
            float fresnel = pow(1.0 - abs(dot(-rayDirection, normal)), 2.15);
            float ripple = 0.5 + 0.5 * sin(
                    dot(normal, vec3(9.3, 12.7, 7.1))
                    + progress * 31.0 + seed);
            float fade = 1.0 - smoothstep(0.68, 1.0, progress);
            float shell = clamp((0.20 + fresnel * 0.82)
                    * (0.88 + ripple * 0.12) * fade * strength, 0.0, 1.0);
            vec2 refraction = viewNormal.xy
                    * (0.009 + fresnel * 0.024) * shell;
            vec2 refractedUv = clamp(baseUv + refraction,
                    vec2(0.001), vec2(0.999));
            vec3 glass = glassSample(refractedUv, viewNormal.xy,
                    0.0025 * shell);
            vec3 blurred = texture(uBlur, refractedUv).rgb;
            color = mix(color, mix(glass, blurred, 0.72), shell * 0.90);

            float heat = exp(-progress * 8.0)
                    * (0.35 + fresnel * 0.65) * strength;
            color += vec3(1.0, 0.34, 0.055) * heat * 0.68;
            color += vec3(1.0, 0.73, 0.27) * fresnel * fade * 0.11 * strength;
        } else if (type == 1) {
            // Mace smash: a compressed world-space liquid-glass dome plus a
            // terrain-following ring. Ordinary mace hits never create it.
            float impactFade = 1.0 - smoothstep(0.70, 1.0, progress);
            float domeRadius = mix(0.14, 5.8, easeOutCubic(progress));
            float domeHeight = mix(0.12, 1.65, easeOutCubic(progress));
            vec3 domeNormal;
            float domeHit = rayEllipsoid(
                    rayDirection,
                    center + vec3(0.0, 0.20, 0.0),
                    vec3(0.0, 1.0, 0.0),
                    domeHeight,
                    domeRadius,
                    domeNormal
            );
            if (domeHit > 0.0 && domeHit < sceneDistance + 0.02) {
                vec3 viewDomeNormal = safeDirection(mat3(uViewMatrix) * domeNormal,
                        vec3(0.0, 0.0, 1.0));
                float domeFresnel = pow(
                        1.0 - abs(dot(-rayDirection, domeNormal)), 2.0);
                float domeShell = clamp((0.18 + domeFresnel * 0.86)
                        * impactFade * strength, 0.0, 1.0);
                vec2 domeUv = clamp(baseUv + viewDomeNormal.xy
                        * (0.008 + domeFresnel * 0.022) * domeShell,
                        vec2(0.001), vec2(0.999));
                vec3 domeGlass = glassSample(domeUv, viewDomeNormal.xy,
                        0.0022 * domeShell);
                vec3 domeBlur = texture(uBlur, domeUv).rgb;
                color = mix(color, mix(domeGlass, domeBlur, 0.70),
                        domeShell * 0.91);
                color += vec3(0.78, 0.86, 1.0)
                        * domeFresnel * impactFade * 0.15 * strength;
            }

            vec3 sceneDelta = scenePosition - center;
            float horizontalDistance = length(sceneDelta.xz);
            float radius = mix(0.08, 6.9, easeOutCubic(progress));
            float width = mix(0.20, 0.42, progress);
            float verticalContact = 1.0 - smoothstep(0.30, 1.65, abs(sceneDelta.y));
            float surfaceWave = ringMask(horizontalDistance, radius, width)
                    * verticalContact;

            float planeWave = 0.0;
            vec3 wavePoint = scenePosition;
            if (abs(rayDirection.y) > 0.0001) {
                float planeDistance = (center.y + 0.06) / rayDirection.y;
                vec3 planePoint = rayDirection * planeDistance;
                float planeRadius = length((planePoint - center).xz);
                float contact = 1.0 - smoothstep(0.35, 1.85, abs(sceneDelta.y));
                if (planeDistance > 0.0 && planeDistance < sceneDistance + 0.16) {
                    planeWave = ringMask(planeRadius, radius, width * 0.78) * contact;
                    if (planeWave > surfaceWave) {
                        wavePoint = planePoint;
                    }
                }
            }

            float wave = max(surfaceWave, planeWave * 0.92)
                    * pow(1.0 - progress, 1.12) * strength;
            if (wave > 0.001) {
                vec3 radial = safeDirection(
                        vec3(wavePoint.x - center.x, 0.0, wavePoint.z - center.z),
                        vec3(1.0, 0.0, 0.0));
                vec3 viewRadial = mat3(uViewMatrix) * radial;
                vec2 refractedUv = clamp(baseUv + viewRadial.xy * 0.032 * wave,
                        vec2(0.001), vec2(0.999));
                vec3 glass = glassSample(refractedUv, viewRadial.xy, 0.0020 * wave);
                vec3 blurred = texture(uBlur, refractedUv).rgb;
                color = mix(color, mix(glass, blurred, 0.68),
                        clamp(wave * 0.94, 0.0, 1.0));
                color += vec3(0.72, 0.80, 0.94) * wave * 0.20;
            }

            float impactCore = (1.0 - smoothstep(0.0, 0.68, horizontalDistance))
                    * (1.0 - smoothstep(0.0, 0.9, abs(sceneDelta.y)))
                    * exp(-progress * 11.0) * strength;
            color += vec3(0.90, 0.94, 1.0) * impactCore * 0.44;
        } else {
            // Every blob owns its trajectory, landing point and puddle
            // lifetime. Times below are seconds within the 6.5 second effect.
            vec3 arrowDirection = safeDirection(uDirections[index].xyz,
                    vec3(0.0, 0.0, 1.0));
            vec3 groundAnchor = uAnchors[index].xyz;
            float bloodTime = progress * 6.5;
            vec3 boundsCenter = (center + groundAnchor) * 0.5;
            float boundsRadius = length(center - groundAnchor) * 0.5 + 1.82;
            float boundsHit = raySphere(rayDirection, boundsCenter, boundsRadius);
            bool insideBounds = length(boundsCenter) < boundsRadius
                    || (boundsHit > 0.0 && boundsHit < sceneDistance + 0.03);
            if (!insideBounds) {
                continue;
            }

            float nearestDrop = 1000000.0;
            vec3 nearestNormal = vec3(0.0, 1.0, 0.0);
            float dropVariation = 0.0;
            float nearestDropAlpha = 0.0;

            for (int drop = 0; drop < BLOOD_DROPS; drop++) {
                float id = float(drop);
                float randomA = hash11(seed + id * 17.17);
                float randomB = hash11(seed * 1.91 + id * 31.73);
                float randomC = hash11(seed * 3.17 + id * 11.41);
                float randomD = hash11(seed * 4.73 + id * 23.27);
                float randomE = hash11(seed * 7.11 + id * 43.91);
                vec3 randomVector = safeDirection(vec3(
                        randomA * 2.0 - 1.0,
                        randomB * 2.0 - 0.68,
                        randomC * 2.0 - 1.0
                ), vec3(0.0, 1.0, 0.0));
                vec3 sprayDirection = safeDirection(
                        arrowDirection * 0.80 + randomVector * 1.12
                        + vec3(0.0, 0.30, 0.0), arrowDirection);
                vec3 horizontalSpread = vec3(
                        sprayDirection.x, 0.0, sprayDirection.z)
                        * mix(0.32, 1.28, randomB);
                vec3 landingPoint = groundAnchor + horizontalSpread;
                float flightDuration = mix(0.34, 0.64, randomC);
                float airProgress = clamp(bloodTime / flightDuration, 0.0, 1.0);
                float arcSize = mix(0.28, 0.94, randomC);
                float arcHeight = arcSize * sin(airProgress * 3.14159265359);
                vec3 dropCenter = mix(center, landingPoint, airProgress)
                        + vec3(0.0, arcHeight, 0.0);
                vec3 motion = landingPoint - center + vec3(
                        0.0,
                        arcSize * 3.14159265359
                                * cos(airProgress * 3.14159265359),
                        0.0);
                float airAlpha = 1.0 - smoothstep(0.78, 1.0, airProgress);
                if (airAlpha > 0.001) {
                    float dropRadius = mix(0.045, 0.082, randomC);
                    vec3 motionDirection = safeDirection(
                            motion, sprayDirection);
                    vec3 sideDirection = safeDirection(
                            cross(motionDirection,
                            abs(motionDirection.y) < 0.90
                                    ? vec3(0.0, 1.0, 0.0)
                                    : vec3(1.0, 0.0, 0.0)),
                            vec3(1.0, 0.0, 0.0));
                    vec3 blobUp = safeDirection(
                            cross(sideDirection, motionDirection),
                            vec3(0.0, 1.0, 0.0));
                    vec3 shapeNormal;
                    float randomF = hash11(seed * 9.37 + id * 61.13);
                    float randomG = hash11(seed * 11.71 + id * 71.57);
                    vec4 blobVariation = vec4(
                            randomA, randomB, randomF, randomG);
                    // A smooth implicit union gives every drop a compact,
                    // asymmetrical liquid silhouette. Unlike intersecting
                    // ellipsoids it cannot produce the pointed seams visible
                    // in the old implementation.
                    float shapeHit = rayBloodBlob(
                            rayDirection, dropCenter,
                            sideDirection, blobUp, motionDirection,
                            dropRadius, blobVariation, shapeNormal);

                    if (shapeHit > 0.0 && shapeHit < nearestDrop
                            && shapeHit < sceneDistance + 0.01) {
                        nearestDrop = shapeHit;
                        nearestNormal = shapeNormal;
                        dropVariation = randomA;
                        nearestDropAlpha = airAlpha;
                    }
                }

                float puddleAge = bloodTime - flightDuration;
                // Each landed blot owns all three timings. The 2-4 second hold
                // is exactly 1-2 seconds longer than before, and the longer
                // fade comfortably finishes before the 6.5 second instance
                // lifetime instead of being cut off by it.
                float holdDuration = mix(2.0, 4.0, randomD);
                float appearanceDuration = mix(0.32, 0.62, randomA);
                float fadeDuration = mix(1.0, 1.55, randomE);
                float appearance = smoothstep(
                        0.0, appearanceDuration, puddleAge);
                float disappearance = 1.0 - smoothstep(
                        holdDuration, holdDuration + fadeDuration, puddleAge);
                float puddleOpacity = appearance * disappearance;
                if (puddleOpacity > 0.001) {
                    vec3 groundDelta = scenePosition - landingPoint;
                    float puddleDistance = length(groundDelta.xz);
                    float puddleAngle = atan(groundDelta.z, groundDelta.x);
                    float edgeNoise = sin(puddleAngle * 5.0 + randomA * 8.0) * 0.10
                            + sin(puddleAngle * 9.0 - randomC * 11.0) * 0.045;
                    float growth = easeOutCubic(clamp(
                            puddleAge / appearanceDuration, 0.0, 1.0));
                    float miniRadius = mix(0.055, mix(0.16, 0.34, randomB), growth)
                            * (1.0 + edgeNoise);
                    // Follow nearby upward-facing terrain instead of clipping
                    // at the exact Y plane of the original impact. This keeps
                    // overlapping stains connected across a block edge while
                    // avoiding projection onto vertical walls.
                    float terrainContact = (1.0 - smoothstep(
                            0.18, 1.18, abs(groundDelta.y))) * upwardSurface;
                    float miniMask = clamp((1.0 - smoothstep(
                            miniRadius - 0.022,
                            miniRadius + 0.026,
                            puddleDistance))
                            * terrainContact
                            * puddleOpacity
                            * max(strength, 0.0), 0.0, 1.0);
                    // Accumulate every arrow hit into one alpha-union. The
                    // reflection is evaluated once after the effect loop, so
                    // overlapping stains become one liquid surface rather
                    // than several composited layers with visible seams.
                    float contribution = miniMask * (1.0 - bloodPuddleMask);
                    float combinedMask = bloodPuddleMask + contribution;
                    if (combinedMask > 0.0001) {
                        bloodPuddleVariation = (
                                bloodPuddleVariation * bloodPuddleMask
                                + randomC * contribution) / combinedMask;
                    }
                    bloodPuddleMask = combinedMask;
                }
            }

            if (nearestDrop < 999999.0) {
                vec3 viewNormal = safeDirection(
                        mat3(uViewMatrix) * nearestNormal,
                        vec3(0.0, 0.0, 1.0));
                float fresnel = pow(
                        1.0 - abs(dot(-rayDirection, nearestNormal)), 2.0);
                vec3 highlightDirection = safeDirection(
                        -rayDirection + vec3(0.16, 0.56, -0.10),
                        -rayDirection);
                float liquidHighlight = pow(max(0.0,
                        dot(nearestNormal, highlightDirection)), 20.0);
                float alpha = clamp((0.82 + fresnel * 0.16)
                        * nearestDropAlpha * strength, 0.0, 0.97);
                vec2 streakUv = clamp(baseUv + viewNormal.xy * 0.007 * alpha,
                        vec2(0.001), vec2(0.999));
                vec3 refracted = texture(uBlur, streakUv).rgb;
                vec3 bloodColor = mix(vec3(0.12, 0.0015, 0.0015),
                        vec3(0.42, 0.012, 0.006), dropVariation);
                bloodColor += vec3(0.28, 0.042, 0.024) * fresnel * 0.38;
                bloodColor += vec3(0.72, 0.16, 0.11)
                        * liquidHighlight * 0.38;
                vec3 dropReflection = screenSpaceReflection(
                        rayDirection * nearestDrop,
                        rayDirection,
                        nearestNormal,
                        streakUv);
                vec3 reflectedDrop = dropReflection * vec3(0.76, 0.22, 0.17);
                vec3 glossyBlood = mix(
                        bloodColor,
                        reflectedDrop,
                        0.52 + fresnel * 0.32);
                color = mix(color,
                        mix(refracted * 0.12, glossyBlood, 0.94), alpha);
            }

        }
    }

    // One shared reflection for the complete connected blood surface. This
    // is both cheaper (one SSR trace instead of up to one per arrow hit) and
    // prevents one puddle from visibly covering another at their overlap.
    if (bloodPuddleMask > 0.001) {
        vec3 reflection = screenSpaceReflection(
                scenePosition,
                rayDirection,
                vec3(0.0, 1.0, 0.0),
                baseUv);
        float viewFresnel = pow(1.0 - abs(rayDirection.y), 3.0);
        vec3 deepBlood = vec3(0.115, 0.003, 0.003);
        vec3 reflectedBlood = reflection * mix(
                vec3(0.62, 0.15, 0.13),
                vec3(0.80, 0.24, 0.19), bloodPuddleVariation);
        vec3 puddleColor = mix(deepBlood, reflectedBlood,
                0.58 + viewFresnel * 0.27);
        puddleColor += vec3(0.34, 0.07, 0.045)
                * (0.10 + viewFresnel * 0.34);
        color = mix(color, puddleColor,
                clamp(bloodPuddleMask * 0.88, 0.0, 0.94));
    }

    fragColor = vec4(max(color, vec3(0.0)), 1.0);
}
