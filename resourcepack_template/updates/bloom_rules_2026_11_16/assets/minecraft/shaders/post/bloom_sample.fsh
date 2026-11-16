#version 330
#extension GL_ARB_separate_shader_objects : require
#include <lbc:bloom_rules.glsl>
uniform sampler2D DiffuseSampler;
layout(std140) uniform BlitConfig { vec4 ColorModulate; };
layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;
void main() {
    vec4 scene = texture(DiffuseSampler, texCoord);
    fragColor = vec4(0.0);
    if (lbc_bloom_transport(ivec2(gl_FragCoord.xy), textureSize(DiffuseSampler, 0))) return;
    // Reject ordinary RGB before the table lookup. Never loosen this to RGB-only.
    if (!lbc_bloom_marked(scene)) return;
    int index = lbc_bloom_rule(scene.rgb);
    if (index < 0) return;
    LbcBloomRule rule = LBC_BLOOM_RULES[index];
    // RGBA8 intermediate: retain headroom for strengths up to4, restore after blur.
    fragColor = vec4(vec3(rule.halo) / 255.0 * clamp(rule.strength, 0.0, 4.0) / 4.0, 1.0);
}
