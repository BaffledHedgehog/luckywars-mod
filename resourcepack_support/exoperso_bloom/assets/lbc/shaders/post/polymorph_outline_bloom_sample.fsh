#version 330
#extension GL_ARB_separate_shader_objects : require
#include <lbc:exoperso_bloom.glsl>
uniform sampler2D PolymorphOutlineSampler;
uniform sampler2D SceneDepthSampler;
layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;
void main() {
    vec4 outline = texture(PolymorphOutlineSampler, texCoord);
    // Empty pixels and alpha-zero transport records never emit bloom.
    if (outline.a <= 0.0) {
        fragColor = vec4(0.0);
        return;
    }
    if (lbc_exoperso_record(outline)) {
        float terrainDepth = texture(SceneDepthSampler, texCoord).r;
        fragColor = lbc_exoperso_visible(outline, terrainDepth)
            ? vec4(lbc_exoperso_rgb(outline), 1.0) : vec4(0.0);
        return;
    }
    bool sphere = all(equal(ivec3(round(outline.rgb * 255.0)), ivec3(223,20,255)));
    fragColor = sphere && outline.a > 0.0 ? vec4(outline.rgb, 1.0) : vec4(0.0);
}
