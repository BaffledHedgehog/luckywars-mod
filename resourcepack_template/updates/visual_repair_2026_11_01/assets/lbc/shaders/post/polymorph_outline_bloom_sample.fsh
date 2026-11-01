#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D PolymorphOutlineSampler;
layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;
void main() {
    vec4 outline = texture(PolymorphOutlineSampler, texCoord);
    bool sphere = all(equal(ivec3(round(outline.rgb * 255.0)), ivec3(223,20,255)));
    fragColor = sphere && outline.a > 0.0 ? vec4(outline.rgb, 1.0) : vec4(0.0);
}
