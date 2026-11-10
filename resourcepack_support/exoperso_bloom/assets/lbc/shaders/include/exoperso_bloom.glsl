#ifndef LBC_EXOPERSO_BLOOM
#define LBC_EXOPERSO_BLOOM

// Native rune ItemDisplays exclusively own outline color #E75313.
// This is an auxiliary entity-outline record, never a scene-color classifier.
bool lbc_exoperso_outline_color(vec3 color) {
    return all(equal(ivec3(round(color * 255.0)), ivec3(231,83,19)));
}
uint lbc_exoperso_bytes(vec4 value) {
    uvec4 b = uvec4(round(clamp(value,0.0,1.0)*255.0));
    return b.r | (b.g<<8u) | (b.b<<16u) | (b.a<<24u);
}
bool lbc_exoperso_record(vec4 value) {
    // Ordinary outlines use alpha255; existing invisible transport uses alpha0.
    return (lbc_exoperso_bytes(value)>>28u)==10u;
}
uint lbc_exoperso_depth(float depth) {
    // Logarithmic reverse-Z retains relative depth accuracy at long distances.
    return uint(round(clamp(-log2(max(depth,exp2(-32.0))),0.0,32.0)*(65535.0/32.0)));
}
vec4 lbc_exoperso_pack(vec3 rgb,float depth) {
    uvec3 c=uvec3(round(clamp(rgb,0.0,1.0)*15.0));
    uint bits=0xA0000000u | (lbc_exoperso_depth(depth)<<12u) | (c.r<<8u) | (c.g<<4u) | c.b;
    return vec4(uvec4(bits,bits>>8u,bits>>16u,bits>>24u)&uvec4(255u))/255.0;
}
vec3 lbc_exoperso_rgb(vec4 value) {
    uint bits=lbc_exoperso_bytes(value);
    return vec3(float((bits>>8u)&15u),float((bits>>4u)&15u),float(bits&15u))/15.0;
}
bool lbc_exoperso_visible(vec4 value,float terrainDepth) {
    uint rune=(lbc_exoperso_bytes(value)>>12u)&65535u;
    uint scene=lbc_exoperso_depth(terrainDepth);
    // Two quantization units cover depth interpolation; opaque walls still occlude.
    return rune<=min(65535u,scene+2u);
}
#endif
