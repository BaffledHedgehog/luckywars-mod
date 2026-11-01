#version 330
#extension GL_ARB_separate_shader_objects : require
#include <minecraft:globals.glsl>

#define RADIUS 10.0
#define GAMMA  2.0

layout(std140) uniform BlitConfig {
    vec4 ColorModulate;
    vec2 BlurDir;
};

uniform sampler2D DiffuseSampler;
uniform sampler2D BloomSampler;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

vec3 bloomToWhite(vec3 color) {
    // Rec.709 luminance
    float brightness = dot(color, vec3(0.2126, 0.7152, 0.0722));

    // Only start whitening above ~0.8 brightness
    float factor = smoothstep(0.5, 1.0, brightness);
    return mix(color, vec3(1.0), factor);
}

vec4 blur(vec2 dir, float radius, sampler2D samplerTex, vec2 coord) {
    float weight[5] = float[](0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);
    vec2 oneTexel = 1.0 / ScreenSize;
    vec4 blurred = vec4(0.0);
    float totalWeight = 0.0;

    for (float r = -radius; r <= radius; r += 1.0) {
        int idx = int(abs(r) / radius * 4.0 + 0.5);
        idx = clamp(idx, 0, 4);
        float w = weight[idx];
        blurred += texture(samplerTex, coord + oneTexel * r * dir) * w;
        totalWeight += w;
    }

    blurred /= totalWeight; // normalize

    return vec4(bloomToWhite(blurred.rgb), 1.0);
}

vec4 screen_blend(vec4 src, vec4 dst) {
    return 1.0 - (1.0 - src) * (1.0 - dst);
}

void main() {
    if (BlurDir.x == 1.0) {
        fragColor = blur(BlurDir, RADIUS, DiffuseSampler, texCoord);
    } else {
        fragColor = screen_blend(
            texture(DiffuseSampler, texCoord) * ColorModulate,
            blur(BlurDir, RADIUS, BloomSampler, texCoord)
        );
        // This pass runs before black-hole/nuclear alpha transport is consumed.
        // Screen blending alpha with an opaque blur would erase every record.
        fragColor.a = texture(DiffuseSampler, texCoord).a;
    }
}