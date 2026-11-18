#version 330
#extension GL_ARB_separate_shader_objects : require
#include <minecraft:globals.glsl>
#include <lbc:bloom_rules.glsl>
layout(std140) uniform BlitConfig { vec4 ColorModulate; vec2 BlurDir; };
uniform sampler2D DiffuseSampler;
uniform sampler2D BloomSampler;
layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

vec4 materialBlur(sampler2D image, vec2 coord, vec2 direction) {
    const vec2 pairs[5] = vec2[](
        vec2(1.4615384980, 0.1706784160), vec2(3.3846153360, 0.1280088120),
        vec2(5.5, 0.0984683045), vec2(7.5, 0.0437636549), vec2(9.5, 0.0131289345));
    vec2 stepUV = direction / ScreenSize;
    vec4 result = texture(image, coord) * 0.0919037562;
    for (int i = 0; i < 5; ++i) {
        vec2 offset = stepUV * pairs[i].x;
        result += (texture(image, coord - offset) + texture(image, coord + offset)) * pairs[i].y;
    }
    // No global whitening: each material owns its core treatment.
    return result;
}
void main() {
    if (BlurDir.x == 1.0) {
        fragColor = materialBlur(DiffuseSampler, texCoord, BlurDir);
    } else {
        vec4 scene = texture(DiffuseSampler, texCoord);
        bool materialPixel = !lbc_bloom_transport(ivec2(gl_FragCoord.xy), textureSize(DiffuseSampler, 0));
        int index = materialPixel && lbc_bloom_marked(scene) ? lbc_bloom_rule(scene.rgb) : -1;
        vec3 core = lbc_bloom_core(scene.rgb, index) * ColorModulate.rgb;
        vec4 halo = clamp(4.0 * materialBlur(BloomSampler, texCoord, BlurDir), 0.0, 1.0);
        vec3 combined = 1.0 - (1.0 - core) * (1.0 - halo.rgb);
        // Keep explicitly white material cores white, including under a dark halo.
        // Scene alpha is unrelated camera/TimeStop data and must remain untouched.
        if (index < 0 || LBC_BLOOM_RULES[index].core != LBC_CORE_WHITE) combined *= 1.0 - halo.a;
        // The white halo belongs to the surroundings, with a fully black lightning core.
        if (index >= 0 && LBC_BLOOM_RULES[index].core == LBC_CORE_BLACK) combined = vec3(0.0);
        fragColor = vec4(combined, scene.a);
    }
}
