#version 330
#extension GL_ARB_separate_shader_objects : require
#include <lbc:smoke_data.glsl>

uniform sampler2D SmokeDataSampler;
layout(location = 0) out vec2 texCoord;
layout(location = 1) flat out mat4 smokeInverseVP;
layout(location = 5) flat out vec4 smokeObjects[LBC_SMOKE_COUNT];
layout(location = 21) flat out vec4 smokeDensities[4];
layout(location = 25) flat out int smokeCount;

bool smokeHeader(int row) {
    return all(equal(ivec3(round(texelFetch(SmokeDataSampler, ivec2(56, row), 0).rgb * 255.0)),
                     ivec3(83, 77, 75)));
}

float smokeFloat(int field, int row, inout bool valid) {
    ivec3 low = ivec3(round(texelFetch(SmokeDataSampler, ivec2(field * 2, row), 0).rgb * 255.0));
    ivec3 high = ivec3(round(texelFetch(SmokeDataSampler, ivec2(field * 2 + 1, row), 0).rgb * 255.0));
    valid = valid && low.b == 201 && high.b == 202;
    uint bits = uint(low.r) | (uint(low.g) << 8u) | (uint(high.r) << 16u) | (uint(high.g) << 24u);
    float value = uintBitsToFloat(bits);
    valid = valid && !isnan(value) && !isinf(value);
    return value;
}

void main() {
    vec2 uv = vec2((gl_VertexIndex << 1) & 2, gl_VertexIndex & 2);
    gl_Position = vec4(uv * 2.0 - 1.0, 0.0, 1.0);
    texCoord = uv;
    smokeCount = 0;
    for (int i=0;i<4;i++) smokeDensities[i]=vec4(0);
    smokeInverseVP = mat4(1.0);
    for (int i = 0; i < LBC_SMOKE_COUNT; i++) smokeObjects[i] = vec4(0.0);
    ivec2 size = textureSize(SmokeDataSampler, 0);
    if (size.x < LBC_SMOKE_PIXELS || size.y < 2 * (LBC_SMOKE_FIRST_ROW + LBC_SMOKE_COUNT)) return;
    for (int slot = 0; slot < LBC_SMOKE_COUNT; slot++) {
        for (int replica = 0; replica < 2; replica++) {
            int row = replica == 0 ? LBC_SMOKE_FIRST_ROW + slot : size.y - 1 - LBC_SMOKE_FIRST_ROW - slot;
            if (!smokeHeader(row)) continue;
            bool valid = true;
            vec3 points[4];
            for (int p = 0; p < 4; p++) for (int axis = 0; axis < 3; axis++)
                points[p][axis] = smokeFloat(p * 3 + axis, row, valid);
            mat4 invVP;
            for (int c = 0; c < 16; c++) invVP[c / 4][c % 4] = smokeFloat(12 + c, row, valid);
            float radius = length(points[1] - points[0]);
            if (!valid || radius < 0.001 || radius > 1024.0 || abs(determinant(invVP)) < 1e-16) continue;
            smokeDensities[smokeCount / 4][smokeCount % 4] = clamp(length(points[2]-points[0])/radius,0.0,1.0);
            smokeObjects[smokeCount++] = vec4(points[0], radius);
            smokeInverseVP = invVP;
            break;
        }
    }
}
