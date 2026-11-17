#ifndef LBC_BLOOM_RULES_GLSL
#define LBC_BLOOM_RULES_GLSL

// ARTIST SETTINGS: OPEN_EDITOR.bat or bloom_rules.json -> edit_bloom.py --write-glsl.
// Source/halo are RGB bytes, strength is 0..4; the marked block is generated.
// Only marked material pixels qualify. Ordinary matching scene RGB never does.
const int LBC_CORE_KEEP = 0;
const int LBC_CORE_WHITE = 1;
const int LBC_CORE_HALO = 2;
struct LbcBloomRule {
    ivec3 source;
    ivec3 halo;
    float strength;
    int core;
};
// BEGIN ARTIST RULES
const LbcBloomRule LBC_BLOOM_RULES[] = LbcBloomRule[](
    LbcBloomRule(ivec3(0,48,255), ivec3(0,48,255), 1.0, LBC_CORE_KEEP),
    LbcBloomRule(ivec3(255,252,253), ivec3(255,252,253), 1.0, LBC_CORE_KEEP),
    LbcBloomRule(ivec3(238,50,115), ivec3(238,50,115), 1.0, LBC_CORE_KEEP),
    LbcBloomRule(ivec3(43,216,147), ivec3(43,216,147), 1.0, LBC_CORE_KEEP),
    LbcBloomRule(ivec3(255,14,14), ivec3(255,14,14), 1.0, LBC_CORE_KEEP),
    LbcBloomRule(ivec3(51,220,255), ivec3(51,220,255), 1.0, LBC_CORE_KEEP),
    LbcBloomRule(ivec3(52,55,229), ivec3(52,55,229), 1.0, LBC_CORE_KEEP),
    LbcBloomRule(ivec3(195,0,255), ivec3(195,0,255), 1.0, LBC_CORE_KEEP),
    LbcBloomRule(ivec3(147,0,237), ivec3(147,0,237), 1.0, LBC_CORE_KEEP),
    LbcBloomRule(ivec3(116,0,228), ivec3(116,0,228), 1.0, LBC_CORE_KEEP),
    LbcBloomRule(ivec3(105,218,82), ivec3(105,218,82), 1.0, LBC_CORE_KEEP),
    LbcBloomRule(ivec3(255,142,50), ivec3(255,142,50), 1.0, LBC_CORE_KEEP),
    LbcBloomRule(ivec3(235,79,255), ivec3(235,79,255), 1.0, LBC_CORE_KEEP),
    LbcBloomRule(ivec3(180,53,255), ivec3(180,53,255), 1.0, LBC_CORE_KEEP),
    LbcBloomRule(ivec3(122,42,232), ivec3(122,42,232), 1.0, LBC_CORE_KEEP),
    LbcBloomRule(ivec3(255,3,3), ivec3(255,3,3), 1.0, LBC_CORE_WHITE),
    LbcBloomRule(ivec3(255,0,64), ivec3(255,14,14), 1.5, LBC_CORE_WHITE),
    LbcBloomRule(ivec3(29,28,32), ivec3(0,0,0), 2.0, LBC_CORE_WHITE),
    LbcBloomRule(ivec3(64,143,255), ivec3(35,140,255), 1.4, LBC_CORE_WHITE)
);
// END ARTIST RULES

// Reserved scene-alpha tag. Raw PNG alpha254 is the explicit opt-in.
// Core shaders remove this tag from ordinary terrain, skins and particles.
bool lbc_bloom_marked(vec4 value) {
    return int(round(value.a * 255.0)) == 254;
}
bool lbc_bloom_transport(ivec2 pixel, ivec2 size) {
    // Existing camera / black-hole / nuclear bytes also use scene alpha.
    // Those two171x50 strips are data, never emissive material coverage.
    return pixel.x < 171 && min(pixel.y, size.y - 1 - pixel.y) < 50;
}
int lbc_bloom_rule(vec3 rgb) {
    ivec3 source = ivec3(round(clamp(rgb, 0.0, 1.0) * 255.0));
    for (int i = 0; i < LBC_BLOOM_RULES.length(); ++i) {
        if (all(equal(source, LBC_BLOOM_RULES[i].source))) return i;
    }
    return -1;
}
// A zero-RGB halo is subtractive. The existing editor's black swatch selects it.
// Its weight travels in the separate bloom target's otherwise unused alpha.
bool lbc_bloom_dark(LbcBloomRule rule) {
    return all(equal(rule.halo, ivec3(0)));
}
vec4 lbc_bloom_unmarked(vec4 value) {
    // One alpha byte, not a color key: RGB and geometric coverage stay intact.
    if (lbc_bloom_marked(value)) value.a = 1.0;
    return value;
}
vec3 lbc_bloom_core(vec3 original, int index) {
    if (index < 0) return original;
    int mode = LBC_BLOOM_RULES[index].core;
    if (mode == LBC_CORE_WHITE) return vec3(1.0);
    if (mode == LBC_CORE_HALO) return vec3(LBC_BLOOM_RULES[index].halo) / 255.0;
    return original;
}
#endif
