#version 330
#extension GL_ARB_separate_shader_objects : require
#include <lbc:reticle_header.glsl>

uniform sampler2D SceneSampler;
layout(location = 0) flat in ivec3 reticleGrid;
layout(location = 1) flat in ivec4 reticleReach;
layout(location = 0) out vec4 fragColor;

bool arm(int pixel, int reach, int scale) {
    // Four logical pixels, shortened as an arm approaches the center.
    return pixel >= max(1, reach - 3) * scale && pixel < (reach + 1) * scale;
}

void main() {
    ivec2 pixel = ivec2(gl_FragCoord.xy);
    ivec2 size = textureSize(SceneSampler, 0);
    fragColor = texelFetch(SceneSampler, pixel, 0);
    if (lbc_reticle_offset(pixel, size) >= 0) fragColor.a = 1.0;

    int scale = reticleGrid.z;
    if (scale <= 0) return;
    ivec2 rel = pixel - reticleGrid.xy;
    bvec2 axial = bvec2(rel.x >= 0 && rel.x < scale, rel.y >= 0 && rel.y < scale);
    int centerWidth = max(2, scale);
    bool desired = all(greaterThanEqual(rel, ivec2(0))) && all(lessThan(rel, ivec2(centerWidth)));
    bool nativeStroke = false;
    if (axial.y) {
        nativeStroke = rel.x >= -4 * scale && rel.x < 5 * scale;
        desired = desired || arm(scale - 1 - rel.x, reticleReach.x, scale)
                          || arm(rel.x, reticleReach.y, scale);
    }
    if (axial.x) {
        nativeStroke = nativeStroke || (rel.y >= -4 * scale && rel.y < 5 * scale);
        desired = desired || arm(scale - 1 - rel.y, reticleReach.z, scale)
                          || arm(rel.y, reticleReach.w, scale);
    }

    // Minecraft draws the native INVERT sprite after this pass.
    // Invert the symmetric difference so the two masks compose exactly.
    if (nativeStroke != desired) fragColor.rgb = vec3(1.0) - fragColor.rgb;
}
