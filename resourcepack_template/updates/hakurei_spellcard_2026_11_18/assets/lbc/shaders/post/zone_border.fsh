#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:globals.glsl>
#include <lbc:zone_border.glsl>
#include <lbc:zone_window.glsl>
#include <lbc:hakurei_sphere_data.glsl>

uniform sampler2D SceneSampler;
uniform sampler2D SceneDepthSampler;
layout(location = 0) in vec2 texCoord;
layout(location = 1) flat in mat4 zoneInverseViewProjection;
layout(location = 5) flat in mat3 zoneInverseBasis;
layout(location = 8) flat in vec3 zoneCenter;
// Zone active, zone outside distance, sphere count, camera depth in sphere.
layout(location = 9) flat in vec4 zoneInfo;
layout(location = 10) flat in vec4 hakureiSpheres[LBC_HAKUREI_SLOTS];
layout(location = 0) out vec4 fragColor;

vec3 zoneUnproject(vec2 ndc, float depth) {
    vec4 position = zoneInverseViewProjection * vec4(ndc, depth, 1.0);
    return position.xyz / position.w;
}

bool zoneVisibleHit(float distance, vec3 origin, vec3 direction,
                    float sceneDistance, float depthUncertainty) {
    vec3 position = origin + direction * distance;
    vec3 local = zoneInverseBasis * (position - zoneCenter);
    float height = local.y;
    vec3 gradient = transpose(zoneInverseBasis) * vec3(local.x, 0.0, local.z);
    // The fallback uses 2048 inscribed chords, while the post traces a perfect
    // cylinder. Convert that radial error to ray distance, including grazing
    // angles. Otherwise the wall's own depth cuts vertical holes in its shader.
    float meshUncertainty = 0.000002 / max(abs(dot(gradient, direction)), 1e-9);
    float tolerance = 0.001 + depthUncertainty + meshUncertainty;
    return distance >= 0.0 && distance <= sceneDistance + tolerance &&
           abs(position.y) < lbc_zone_half_height(length(position.xz)) && height >= 0.0 && height <= 1.0;
}

struct ZoneRay {
    vec3 origin;
    vec3 direction;
};

ZoneRay zoneRay(vec2 ndc) {
    vec3 a = zoneUnproject(ndc, 0.25);
    vec3 b = zoneUnproject(ndc, 0.75);
    bool firstNear = dot(a, a) < dot(b, b);
    return ZoneRay(firstNear ? a : b, normalize(firstNear ? b - a : a - b));
}

vec3 zoneHitDifferential(float distance, vec3 normal, ZoneRay ray, ZoneRay neighbour) {
    vec3 delta = neighbour.origin - ray.origin + distance * (neighbour.direction - ray.direction);
    float denominator = dot(normal, ray.direction);
    denominator = (denominator < 0.0 ? -1.0 : 1.0) * max(abs(denominator), 0.001);
    return delta - ray.direction * (dot(normal, delta) / denominator);
}

#include <lbc:hakurei_sphere.glsl>

float zoneTerrainContact(vec3 position, vec3 pixelDX, vec3 pixelDY, bool finiteDepth) {
    if (!finiteDepth) return 0.0;
    vec3 local = zoneInverseBasis * (position - zoneCenter);
    vec3 wallNormal = transpose(zoneInverseBasis) * vec3(local.x, 0.0, local.z);
    vec3 surfaceNormal = cross(pixelDX, pixelDY);
    float normalProduct = dot(wallNormal, wallNormal) * dot(surfaceNormal, surfaceNormal);
    if (normalProduct < 1e-20) return 0.0;
    float alignment = abs(dot(wallNormal, surfaceNormal)) * inversesqrt(normalProduct);
    // The fallback wall can itself be present in main depth. Its normal is
    // parallel to the cylinder: this is NOT terrain intersecting the wall.
    // Smoothly reject tangent surfaces rather than illuminating the whole zone.
    return 1.0 - smoothstep(0.90, 0.98, alignment);
}

vec3 zoneSurfaceContact(vec3 color, vec3 position, vec3 pixelDX, vec3 pixelDY,
                        float maxFootprint, float terrainContact) {
    if (terrainContact <= 0.0 || lbc_zone_window(position) <= 0.0) return color;
    vec3 local = zoneInverseBasis * (position - zoneCenter);
    if (local.y < 0.0 || local.y > 1.0) return color;
    float radius = length(local.xz);
    if (radius < 0.00001) return color;
    vec3 gradient = transpose(zoneInverseBasis) * vec3(local.x / radius, 0.0, local.z / radius);
    float gradientLength = length(gradient);
    float separation = abs(radius - 1.0) / max(gradientLength, 1e-8);
    vec3 normal = gradient / max(gradientLength, 1e-8);
    // Depth jumps at foreground silhouettes must not inflate the contact halo.
    float footprint = min(abs(dot(normal, pixelDX)) + abs(dot(normal, pixelDY)), maxFootprint);
    float core = 1.0 - smoothstep(0.04, 0.16 + footprint, separation);
    float halo = exp2(-separation * separation / (0.20 + footprint * footprint));
    float window = lbc_zone_window(position) * terrainContact;
    color += vec3(0.055, 0.22, 0.48) * halo * window;
    return mix(color, vec3(0.46, 0.77, 1.0), core * 0.97 * window);
}

vec4 zoneEnergy(float distance, ZoneRay ray, ZoneRay rayX, ZoneRay rayY,
                float sceneDistance, float terrainContact) {
    vec3 origin = ray.origin;
    vec3 direction = ray.direction;
    vec3 relativePosition = origin + direction * distance;
    vec3 localPosition = zoneInverseBasis * (relativePosition - zoneCenter);
    vec3 normal = normalize(transpose(zoneInverseBasis) * vec3(localPosition.x, 0.0, localPosition.z));
    vec3 pixelDX = zoneHitDifferential(distance, normal, ray, rayX);
    vec3 pixelDY = zoneHitDifferential(distance, normal, ray, rayY);
    vec4 energy = lbc_zone_color(relativePosition, pixelDX, pixelDY);
    // Also brighten the wall immediately above a visible intersection.
    float contact = terrainContact *
                    (1.0 - smoothstep(0.03, 0.70, max(sceneDistance - distance, 0.0)));
    energy.rgb = mix(energy.rgb, vec3(0.46, 0.77, 1.0), contact * 0.95);
    energy.a = mix(energy.a, 1.0, contact * 0.95);
    energy.a *= lbc_zone_window(relativePosition);
    return energy;
}

vec3 renderZone(vec3 color, ZoneRay ray, ZoneRay rayX, ZoneRay rayY, vec3 surfacePosition,
                bool finiteDepth, float sceneDistance, float depthUncertainty) {
    vec3 origin = ray.origin;
    vec3 direction = ray.direction;
    // Evaluate derivatives before any per-pixel visibility branches. Contact
    // follows the main depth buffer, so hills, steps and vertical faces work.
    vec3 surfaceDX = dFdx(surfacePosition);
    vec3 surfaceDY = dFdy(surfacePosition);
    vec3 viewDX = rayX.origin - origin + sceneDistance * (rayX.direction - direction);
    vec3 viewDY = rayY.origin - origin + sceneDistance * (rayY.direction - direction);
    float maxFootprint = clamp(3.0 * max(length(viewDX), length(viewDY)), 0.01, 2.0);
    float terrainContact = zoneTerrainContact(surfacePosition, surfaceDX, surfaceDY, finiteDepth);
    color = zoneSurfaceContact(color, surfacePosition, surfaceDX, surfaceDY, maxFootprint, terrainContact);

    vec3 localOrigin = zoneInverseBasis * (origin - zoneCenter);
    vec3 localDirection = zoneInverseBasis * direction;
    float quadratic = dot(localDirection.xz, localDirection.xz);
    if (quadratic < 1e-15) return color;
    float middle = -dot(localOrigin.xz, localDirection.xz) / quadratic;
    vec2 closest = localOrigin.xz + localDirection.xz * middle;
    float remainder = 1.0 - dot(closest, closest);
    if (remainder < 0.0) return color;
    float halfDistance = sqrt(remainder / quadratic);
    vec2 hits = vec2(middle - halfDistance, middle + halfDistance);

    // Identify the fallback wall in the scene depth by both its cylinder
    // position and its surface normal. Different passes can round its depth
    // differently, particularly on high/low sections viewed at steep angles.
    // Reconcile ONLY that same surface; terrain keeps the ordinary depth test.
    vec3 localSurface = zoneInverseBasis * (surfacePosition - zoneCenter);
    float surfaceRadius = length(localSurface.xz);
    vec3 surfaceGradient = transpose(zoneInverseBasis) *
                          vec3(localSurface.x, 0.0, localSurface.z);
    float radialSeparation = abs(surfaceRadius - 1.0) * surfaceRadius /
                             max(length(surfaceGradient), 1e-8);
    bool ownWallDepth = finiteDepth && surfaceRadius > 0.5 && terrainContact < 0.001 &&
                        localSurface.y >= 0.0 && localSurface.y <= 1.0 &&
                        radialSeparation <= 0.02;
    bool ownNear = hits.x >= 0.0 && abs(sceneDistance - hits.x) <= abs(sceneDistance - hits.y);
    float nearSceneDistance = ownWallDepth && ownNear ? max(sceneDistance, hits.x) : sceneDistance;
    float farSceneDistance = ownWallDepth && !ownNear ? max(sceneDistance, hits.y) : sceneDistance;

    bool nearVisible = zoneVisibleHit(hits.x, origin, direction, nearSceneDistance, depthUncertainty);
    bool farVisible = hits.y - hits.x > 0.00001 &&
                      zoneVisibleHit(hits.y, origin, direction, farSceneDistance, depthUncertainty);
    int surfaces = (nearVisible ? 1 : 0) + (farVisible ? 1 : 0);
    if (surfaces == 0) return color;

    // Do not try to invert Minecraft's OIT blend using an assumed surface
    // count. That cannot recover the background and amplifies panel seams.
    if (farVisible) {
        vec4 energy = zoneEnergy(hits.y, ray, rayX, rayY, sceneDistance, terrainContact);
        color = mix(color, energy.rgb, energy.a);
    }
    if (nearVisible) {
        vec4 energy = zoneEnergy(hits.x, ray, rayX, rayY, sceneDistance, terrainContact);
        color = mix(color, energy.rgb, energy.a);
    }
    return color;
}

void main() {
    vec4 scene = texture(SceneSampler, texCoord);
    fragColor = scene;
    if (zoneInfo.x == 0.0 && zoneInfo.z == 0.0) return;
    // Preserve the nexus fog transport pixels before applying world effects.
    if (all(lessThan(ivec2(texCoord * ScreenSize), ivec2(4))) &&
        all(lessThanEqual(abs(ivec3(round(scene.rgb * 255.0)) - ivec3(72, 82, 25)), ivec3(1)))) return;
    vec2 ndc = texCoord * 2.0 - 1.0;
    // Two finite points on the camera ray work with both depth conventions,
    // reverse Z and infinite far planes, without assuming the player's FOV.
    ZoneRay ray = zoneRay(ndc);
    ZoneRay rayX = zoneRay(ndc + vec2(2.0 / ScreenSize.x, 0.0));
    ZoneRay rayY = zoneRay(ndc + vec2(0.0, 2.0 / ScreenSize.y));
    vec3 origin = ray.origin;
    vec3 direction = ray.direction;

    ivec2 depthSize = textureSize(SceneDepthSampler, 0);
    ivec2 depthPixel = clamp(ivec2(texCoord * vec2(depthSize)), ivec2(0), depthSize - 1);
    float deviceDepth = texelFetch(SceneDepthSampler, depthPixel, 0).r;
    float sceneDepth = deviceDepth;
    float depthScale = 1.0;
    #ifndef RENDERPEARL_DEPTH_IS_ZERO_TO_ONE
    sceneDepth = sceneDepth * 2.0 - 1.0;
    depthScale = 2.0;
    #endif
    vec4 scenePosition = zoneInverseViewProjection * vec4(ndc, sceneDepth, 1.0);
    bool finiteDepth = deviceDepth > 0.0 && abs(scenePosition.w) >= 1e-7;
    vec3 surfacePosition = scenePosition.xyz / (finiteDepth ? scenePosition.w : 1.0);
    float sceneDistance = finiteDepth ? dot(surfacePosition - origin, direction) : 1e30;
    // D32_FLOAT precision is in device depth, not metres. Propagate a few ULPs
    // through homogeneous unprojection to account for distance, FOV and both
    // normal/reverse Z. A fixed world-space epsilon fails far from the camera.
    float depthUlp = uintBitsToFloat(floatBitsToUint(deviceDepth) + 1u) - deviceDepth;
    float ndcMagnitude = abs(sceneDepth);
    float ndcUlp = uintBitsToFloat(floatBitsToUint(ndcMagnitude) + 1u) - ndcMagnitude;
    // Mapping tiny reverse-Z depth to [-1,1] can lose more bits than the
    // original depth texture. Include that conversion's precision as well.
    float ndcUncertainty = max(depthUlp * depthScale, ndcUlp);
    vec3 depthDerivative = (zoneInverseViewProjection[2].xyz -
                            surfacePosition * zoneInverseViewProjection[2].w) /
                           (finiteDepth ? scenePosition.w : 1.0);
    float depthUncertainty = finiteDepth ?
        8.0 * ndcUncertainty * abs(dot(depthDerivative, direction)) : 0.0;
    vec3 color = scene.rgb;
    // Minecraft 26.3 clears reverse-Z depth to zero. Native OIT writes
    // nearest transparent coverage into this depth texture before the pass.
    if (deviceDepth == 0.0 && zoneInfo.w > 0.0) {
        color *= mix(1.0, 0.45, smoothstep(0.0, 0.9, zoneInfo.w));
    }
    if (zoneInfo.x != 0.0) color = renderZone(color, ray, rayX, rayY, surfacePosition,
                                             finiteDepth, sceneDistance, depthUncertainty);
    if (zoneInfo.z > 0.0) color = lbc_hakurei_render(color, ray, rayX, rayY,
                                                      sceneDistance + depthUncertainty + 0.001);
    fragColor = vec4(color, scene.a);
}
