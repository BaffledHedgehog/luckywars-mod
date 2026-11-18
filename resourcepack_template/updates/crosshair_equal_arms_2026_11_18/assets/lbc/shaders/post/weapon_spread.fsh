#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D SceneSampler;
layout(location = 0) flat in ivec3 reticleGrid;
layout(location = 1) flat in ivec4 reticleReach;
layout(location = 0) out vec4 fragColor;

bool arm(int distance, int tip, int length) {
    return distance >= tip - length && distance < tip;
}

void main() {
    ivec2 pixel = ivec2(gl_FragCoord.xy);
    fragColor = texelFetch(SceneSampler, pixel, 0);

    int scale = reticleGrid.z;
    if (scale <= 0) return;
    ivec2 rel = pixel - reticleGrid.xy;
    bvec2 axial = bvec2(rel.x >= 0 && rel.x < scale, rel.y >= 0 && rel.y < scale);
    int centerWidth = max(2, scale);
    bool desired = all(greaterThanEqual(rel, ivec2(0))) && all(lessThan(rel, ivec2(centerWidth)));
    // Keep the projected tips. Trim all four strokes by the same amount
    // when any tip is too close to the center to fit four GUI cells.
    // At scale 1 the extra pixel of the 2x2 dot occupies the positive sides.
    int extra = centerWidth - scale;
    ivec4 tip = reticleReach * scale - ivec4(0, extra, 0, extra);
    int length = clamp(min(min(tip.x, tip.y), min(tip.z, tip.w)), 0, 4 * scale);
    bool nativeStroke = false;
    if (axial.y) {
        nativeStroke = rel.x >= -4 * scale && rel.x < 5 * scale;
        desired = desired || arm(-1 - rel.x, tip.x, length)
                          || arm(rel.x - centerWidth, tip.y, length);
    }
    if (axial.x) {
        nativeStroke = nativeStroke || (rel.y >= -4 * scale && rel.y < 5 * scale);
        desired = desired || arm(-1 - rel.y, tip.z, length)
                          || arm(rel.y - centerWidth, tip.w, length);
    }

    // Minecraft draws the native INVERT sprite after this pass.
    // Invert the symmetric difference so the two masks compose exactly.
    if (nativeStroke != desired) fragColor.rgb = vec3(1.0) - fragColor.rgb;
}
