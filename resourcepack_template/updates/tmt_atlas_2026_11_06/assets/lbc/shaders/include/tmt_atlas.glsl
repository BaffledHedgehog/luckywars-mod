// TMT v1: sole source is the currently bound ITEM atlas (Sampler0).
// Integer hash is stable across render context; atlas repacking may change crops.
#ifndef LBC_TMT_ATLAS_GLSL
#define LBC_TMT_ATLAS_GLSL
bool lbc_tmt_carrier(vec4 sampleColor, uint tintPrefix) {
    return tintPrefix == 250u && all(equal(ivec4(round(sampleColor * 255.0)), ivec4(31, 197, 83, 255)));
}
uint lbc_tmt_hash(uint x) {
    x ^= x >> 16u; x *= 0x7feb352du;
    x ^= x >> 15u; x *= 0x846ca68bu;
    return x ^ (x >> 16u);
}
float lbc_tmt_random(inout uint state) {
    state = lbc_tmt_hash(state + 0x9e3779b9u);
    return float(state & 0x00ffffffu) / 16777216.0;
}
// Explicit sequencing avoids implementation-dependent function-argument order.
vec2 lbc_tmt_random2(inout uint state) {
    float x = lbc_tmt_random(state);
    float y = lbc_tmt_random(state);
    return vec2(x, y);
}
vec3 lbc_tmt_random3(inout uint state) {
    float x = lbc_tmt_random(state);
    float y = lbc_tmt_random(state);
    float z = lbc_tmt_random(state);
    return vec3(x, y, z);
}
// Preserve the observed legacy PNG size weights, with one stable choice per seed.
int lbc_tmt_resolution(uint seed) {
    uint ticket = lbc_tmt_hash(seed ^ 0x93a761bdu) % 12758u;
    if (ticket < 7248u) return 16;
    if (ticket < 9508u) return 32;
    if (ticket < 11379u) return 64;
    if (ticket < 12159u) return 128;
    return 256;
}
vec4 lbc_tmt_collage(sampler2D atlas, vec2 quad, uint seed) {
    // Output quantization varies per item; patch geometry stays in 32 logical units.
    // Center samples keep rotations/reflections symmetric at every resolution.
    float resolution = float(lbc_tmt_resolution(seed));
    float density = resolution / 32.0;
    vec2 cell = clamp(floor(quad * resolution), vec2(0.0), vec2(resolution - 1.0));
    vec2 pixel = (cell + 0.5) / density;
    uint state = lbc_tmt_hash(seed + 1u);
    vec3 base = lbc_tmt_random3(state);
    vec2 centered = abs(pixel - vec2(16.0));
    float mask = max(centered.x, centered.y) < 13.0 ? 1.0 : 0.0;
    if (centered.x + centered.y > 22.0) mask = 0.0;
    vec4 result = vec4(0.18 + base * 0.45, mask);
    ivec2 size = textureSize(atlas, 0);
    // Two underlying atlas crops reduce padding-only output. A visible fallback
    // remains only where both source texels are transparent. Total bound: 2+6=8.
    for (int backing = 0; backing < 2; ++backing) {
        ivec2 crop = ivec2(lbc_tmt_random2(state) * vec2(max(size - ivec2(int(resolution)), ivec2(1))));
        ivec2 texel = clamp(crop + ivec2(cell), ivec2(0), size - ivec2(1));
        vec4 source = texelFetch(atlas, texel, 0);
        if (source.a >= 0.1 && mask > 0.0) result.rgb = source.rgb;
    }
    for (int layer = 0; layer < 6; ++layer) {
        vec2 origin = floor(lbc_tmt_random2(state) * 24.0) - 4.0;
        float width = 8.0 + floor(lbc_tmt_random(state) * 17.0);
        vec2 p = pixel - origin;
        int rotation = int(lbc_tmt_random(state) * 4.0);
        bool flip = lbc_tmt_random(state) < 0.5;
        float scale = exp2(floor(lbc_tmt_random(state) * 3.0) - 1.0);
        int shape = int(lbc_tmt_random(state) * 3.0);
        vec3 tint = 0.55 + 0.9 * lbc_tmt_random3(state);
        int extent = max(int(ceil(width * scale * density)), 1);
        ivec2 crop = ivec2(lbc_tmt_random2(state) * vec2(max(size - ivec2(extent), ivec2(1))));
        bool inside = all(greaterThanEqual(p, vec2(0.0))) && all(lessThan(p, vec2(width)));
        if (shape == 1) inside = inside && abs(p.x - width * 0.5) + abs(p.y - width * 0.5) < width * 0.65;
        if (shape == 2) inside = inside && int(p.x + p.y) % 5 != 0;
        if (!inside) continue;
        if (flip) p.x = width - p.x;
        if (rotation == 1) p = vec2(p.y, width - p.x);
        if (rotation == 2) p = vec2(width) - p;
        if (rotation == 3) p = vec2(width - p.y, p.x);
        ivec2 texel = clamp(crop + ivec2(floor(p * scale * density)), ivec2(0), size - ivec2(1));
        vec4 source = texelFetch(atlas, texel, 0);
        // Sprite padding is skipped. Source alpha markers never enter effect dispatch.
        if (source.a < 0.1) continue;
        result.rgb = clamp(source.rgb * tint, 0.0, 1.0);
        result.a = 1.0;
    }
    return result;
}
#endif
