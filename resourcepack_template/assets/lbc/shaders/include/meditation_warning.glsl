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

// Existing scene-color bloom loses exact palette equality after translucent
// blending. Match chroma continuously, so darkness/fade attenuates the glow
// rather than switching it off at one byte. Scene-color matching cannot isolate
// these items over every arbitrary background: it is the existing bloom route.
vec4 lbc_meditation_warning_bloom(vec4 scene) {
    const vec3 palette[4] = vec3[](
        vec3(255.0, 14.0, 14.0) / 255.0,
        vec3(43.0, 216.0, 147.0) / 255.0,
        vec3(0.0, 48.0, 255.0) / 255.0,
        vec3(255.0, 142.0, 50.0) / 255.0
    );
    vec3 bloom = vec3(0.0);
    for (int i = 0; i < 4; ++i) {
        float strength = clamp(dot(scene.rgb, palette[i]) / dot(palette[i], palette[i]), 0.0, 1.0);
        float chromaError = length(scene.rgb - palette[i] * strength) / max(strength, 0.025);
        float match = 1.0 - smoothstep(0.025, 0.13, chromaError);
        bloom = max(bloom, palette[i] * strength * match);
    }
    return vec4(bloom, scene.a);
}
#endif
