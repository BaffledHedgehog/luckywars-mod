#ifndef LBC_MEDITATION_WARNING_GLSL
#define LBC_MEDITATION_WARNING_GLSL

// Textures have solid RGBA. Keep these markers separate from existing 254/130.
// -1 is an ordinary item; all warning materials use raw custom-model tint as fade.
float lbc_meditation_warning_base_alpha(vec4 rawColor) {
    ivec4 b = ivec4(round(rawColor * 255.0));
    bool red = all(equal(b.rgb, ivec3(255, 14, 14)));
    bool green = all(equal(b.rgb, ivec3(43, 216, 147)));
    if (b.a == 253 && (red || green)) return 0.45;
    if (b.a == 252 && green) return 0.55;
    if (b.a == 251 && red) return 0.20;
    if (b.a == 250 && all(equal(b.rgb, ivec3(0, 48, 255)))) return 0.07;
    if (b.a == 249 && all(equal(b.rgb, ivec3(255, 142, 50)))) return 0.20;
    return -1.0;
}

// Exact existing ring marker: all other item/terrain materials keep their path.
bool lbc_meditation_warning_is_ring(vec4 rawColor) {
    return all(equal(ivec4(round(rawColor * 255.0)), ivec4(43, 216, 147, 252)));
}

// A single world-space plane, outer radius .5 and inner radius 1/3.
// Screen derivatives keep its silhouette smooth at oblique angles and distance.
float lbc_meditation_warning_ring_coverage(vec2 quadCoord) {
    float radius = length(quadCoord - vec2(0.5));
    float edge = max(1.0 / 3.0 - radius, radius - 0.5);
    float antialias = max(fwidth(radius) * 0.5, 0.000001);
    return 1.0 - smoothstep(-antialias, antialias, edge);
}

// Only exact raw red-beam texture markers participate. Never sample terrain
// or the composited scene for countdown colors. Fade is carried independently.
vec3 lbc_meditation_warning_color(vec4 rawColor, float progress) {
    ivec4 b = ivec4(round(rawColor * 255.0));
    if (b.a == 253 && all(equal(b.rgb, ivec3(255, 14, 14)))) {
        return mix(vec3(96.0, 0.0, 24.0) / 255.0,
                   vec3(255.0, 14.0, 14.0) / 255.0,
                   clamp(progress, 0.0, 1.0));
    }
    return rawColor.rgb;
}

// Compatibility with packs that still include the previous post-sampler call.
// Approximate scene-color matching also lit terrain; never re-enable it here.
vec4 lbc_meditation_warning_bloom(vec4 scene) {
    return vec4(0.0);
}
#endif
