#version 330
#extension GL_ARB_separate_shader_objects : require
#include <minecraft:globals.glsl>
#include <lbc:smoke_data.glsl>

uniform sampler2D SceneSampler;
uniform sampler2D SceneDepthSampler;
layout(location = 0) in vec2 texCoord;
layout(location = 1) flat in mat4 smokeInverseVP;
layout(location = 5) flat in vec4 smokeObjects[LBC_SMOKE_COUNT];
layout(location = 21) flat in vec4 smokeDensities[4];
layout(location = 25) flat in int smokeCount;
layout(location = 0) out vec4 fragColor;

float smokeHash(vec3 p) {
    uvec3 q = uvec3(ivec3(mod(p, 256.0)));
    uint bits = q.x * 1597334677u ^ q.y * 3812015801u ^ q.z * 2798796415u;
    bits ^= bits >> 16u; bits *= 2246822519u; bits ^= bits >> 13u;
    return float(bits & 16777215u) / 16777216.0;
}
float smokeNoise(vec3 p) {
    vec3 cell = floor(p), t = fract(p);
    t = t * t * (3.0 - 2.0 * t);
    return mix(mix(mix(smokeHash(cell), smokeHash(cell + vec3(1,0,0)), t.x),
                   mix(smokeHash(cell + vec3(0,1,0)), smokeHash(cell + vec3(1,1,0)), t.x), t.y),
               mix(mix(smokeHash(cell + vec3(0,0,1)), smokeHash(cell + vec3(1,0,1)), t.x),
                   mix(smokeHash(cell + vec3(0,1,1)), smokeHash(cell + vec3(1,1,1)), t.x), t.y), t.z);
}

// Antiderivative of (1 - r^2/R^2)^2 along a ray through a spherical volume.
// No per-pixel ray-march loop: geometry and the smooth density integral are exact.
float smokeIntegral(float u, float halfChordSquared) {
    float u2 = u * u;
    return u * (halfChordSquared * halfChordSquared -
                (2.0 / 3.0) * halfChordSquared * u2 + (1.0 / 5.0) * u2 * u2);
}

void main() {
    vec4 scene = texture(SceneSampler, texCoord);
    fragColor = scene;
    if (smokeCount == 0) return;
    vec2 ndc = texCoord * 2.0 - 1.0;
    // A perspective camera is the homogeneous image of the clip-space Z axis.
    // Unlike a fixed near depth, this also works with reverse Z/infinite far planes.
    vec4 cameraH = smokeInverseVP * vec4(0, 0, 1, 0);
    vec4 aH = smokeInverseVP * vec4(ndc, 0.25, 1);
    vec4 bH = smokeInverseVP * vec4(ndc, 0.75, 1);
    if (abs(cameraH.w) < 1e-8 || abs(aH.w) < 1e-8 || abs(bH.w) < 1e-8) return;
    vec3 camera = cameraH.xyz / cameraH.w;
    vec3 a = aH.xyz / aH.w, b = bH.xyz / bH.w;
    vec3 direction = normalize(b - a);
    if (dot(direction, a - camera) < 0.0) direction = -direction;
    float depth = texture(SceneDepthSampler, texCoord).r;
    #ifndef RENDERPEARL_DEPTH_IS_ZERO_TO_ONE
    depth = depth * 2.0 - 1.0;
    #endif
    vec4 surface = smokeInverseVP * vec4(ndc, depth, 1);
    float sceneDistance = abs(surface.w) > 1e-7 ?
        max(0.0, dot(surface.xyz / surface.w - camera, direction)) : 1e30;
    float opticalDepth = 0.0, front = 1e30;
    for (int i = 0; i < smokeCount; i++) {
        float density = smokeDensities[i / 4][i % 4];
        if(density<=0.0)continue;
        vec4 sphere = smokeObjects[i];
        vec3 relative = camera - sphere.xyz;
        float middle = -dot(relative, direction);
        float perpendicularSquared = max(0.0, dot(relative, relative) - middle * middle);
        float radiusSquared = sphere.w * sphere.w;
        float halfChordSquared = radiusSquared - perpendicularSquared;
        if (halfChordSquared <= 0.0) continue;
        float halfChord = sqrt(halfChordSquared);
        float start = max(0.0, middle - halfChord);
        float finish = min(sceneDistance, middle + halfChord);
        if (finish <= start) continue;
        float integral = (smokeIntegral(finish - middle, halfChordSquared) -
                          smokeIntegral(start - middle, halfChordSquared)) / (radiusSquared * radiusSquared);
        opticalDepth += max(0.0, integral) * 3.0 * density;
        front = min(front, start);
    }
    if (opticalDepth <= 0.0) return;
    // Anchor the billows in the world, with exact integer camera wrapping.
    // Sample before opaque depth: hidden silhouettes cannot modulate the fog color.
    vec3 point = camera + direction * (front + 0.75);
    vec3 world = mod((vec3(CameraBlockPos % ivec3(512)) + point - CameraOffset) * 0.5, 256.0);
    vec3 drift = vec3(0.08, -0.045, 0.065) * (GameTime * 1200.0);
    float noise = smokeNoise(world + drift) * 0.7 + smokeNoise(world * 2.0 - drift * 1.3) * 0.3;
    float transmission = exp(-min(opticalDepth * (0.85 + 0.3 * noise), 24.0));
    vec3 fog = mix(vec3(0.36, 0.375, 0.39), vec3(0.49, 0.505, 0.52), noise);
    // Preserve alpha transport used by the existing black-hole/nuclear passes.
    fragColor = vec4(mix(fog, scene.rgb, transmission), scene.a);
}
