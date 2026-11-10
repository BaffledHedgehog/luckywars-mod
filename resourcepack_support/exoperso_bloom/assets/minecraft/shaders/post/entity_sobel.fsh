#version 330
#extension GL_ARB_separate_shader_objects : require

#define WIDTH 2
#include <lbc:exoperso_bloom.glsl>

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

vec4 outlineSample(vec2 uv) {
    vec4 value = texture(InSampler, uv);
    return lbc_exoperso_record(value) ? vec4(0.0) : value;
}
void main() {
    vec4 center = outlineSample(texCoord);
    if (center.a <= 0.0001) {
        fragColor = vec4(0.0);
        return;
    }

    vec2 oneTexel = 1.0 / InSize;
    float edgeStrength = 0.0;



    for (int x = -WIDTH; x <= WIDTH; x++) {
        for (int y = -WIDTH; y <= WIDTH; y++) {
            if (x == 0 && y == 0) {
                continue;
            }

            float nearAlpha = outlineSample(texCoord + vec2(float(x), float(y)) * oneTexel).a;
            edgeStrength = max(edgeStrength, abs(center.a - nearAlpha));
        }
    }

    float edgeAlpha = step(0.001, edgeStrength);
    float fillAlpha = center.a * 0.3;
    float outAlpha = max(fillAlpha, edgeAlpha);

    fragColor = vec4(center.rgb, outAlpha);
}
