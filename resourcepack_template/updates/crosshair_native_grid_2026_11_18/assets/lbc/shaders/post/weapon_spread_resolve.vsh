#version 330
#extension GL_ARB_separate_shader_objects : require
#include <lbc:reticle_header.glsl>
#include <lbc:reticle_native.glsl>

uniform sampler2D SceneSampler;
layout(location = 0) flat out ivec3 reticleGrid;
layout(location = 1) flat out ivec4 reticleReach;

int byteAt(int n,int row,ivec2 size) {
    return int(round(texelFetch(SceneSampler,ivec2(size.x-20-n,row),0).a*255.0));
}
bool header(int row,ivec2 size) {
    for(int i=0;i<4;i++)if(byteAt(i,row,size)!=LBC_RETICLE_MAGIC[i])return false;
    return true;
}
void main() {
    vec2 uv = vec2((gl_VertexIndex << 1) & 2, gl_VertexIndex & 2);
    gl_Position = vec4(uv * 2.0 - 1.0, 0, 1);
    reticleGrid = ivec3(0);
    reticleReach = ivec4(0);

    ivec2 size = textureSize(SceneSampler, 0);
    if (size.x < 48 || size.y < 2) return;
    int row = header(0, size) ? 0 : size.y - 1;
    if (!header(row, size)) return;

    float fields[6];
    for (int i = 0; i < 6; i++) {
        uint bits = 0u;
        for (int b = 0; b < 4; b++) bits |= uint(byteAt(4 + i * 4 + b, row, size)) << uint(b * 8);
        fields[i] = uintBitsToFloat(bits);
        if (isnan(fields[i]) || isinf(fields[i]) || fields[i] < 0.0) return;
    }
    if (fields[4] <= 0 || fields[4] > 64 || fields[5] > 256) return;

    int scale = lbc_reticle_gui_scale(size);
    reticleGrid = ivec3(lbc_reticle_cell_origin(size, scale), scale);

    // Round all four arm tips on the same grid. Their angular extents
    // still come from the original cone / Euler-envelope projection.
    vec4 limit = vec4(size.x, size.x, size.y, size.y);
    vec4 bounds = min(vec4(fields[0], fields[1], fields[2], fields[3]), limit);
    reticleReach = ivec4(floor(bounds / float(scale) + 0.5));
}
