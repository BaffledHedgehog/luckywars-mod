#version 330
#extension GL_ARB_separate_shader_objects : require

#include <lbc:hakurei_sphere_data.glsl>

uniform sampler2D ZoneDataSampler;

layout(location = 0) out vec2 texCoord;
layout(location = 1) flat out mat4 zoneInverseViewProjection;
layout(location = 5) flat out mat3 zoneInverseBasis;
layout(location = 8) flat out vec3 zoneCenter;
// Zone active, zone outside distance, sphere count, camera depth in sphere.
layout(location = 9) flat out vec4 zoneInfo;
layout(location = 10) flat out vec4 hakureiSpheres[LBC_HAKUREI_SLOTS];

bool zoneHeader(int row) {
    return all(equal(ivec3(round(texelFetch(ZoneDataSampler, ivec2(56, row), 0).rgb * 255.0)),
                     ivec3(90, 66, 83)));
}

float zoneFloat(int field, int row, inout bool valid) {
    ivec3 low = ivec3(round(texelFetch(ZoneDataSampler, ivec2(field * 2, row), 0).rgb * 255.0));
    ivec3 high = ivec3(round(texelFetch(ZoneDataSampler, ivec2(field * 2 + 1, row), 0).rgb * 255.0));
    valid = valid && low.b == 201 && high.b == 202;
    uint bits = uint(low.r) | (uint(low.g) << 8u) | (uint(high.r) << 16u) | (uint(high.g) << 24u);
    float value = uintBitsToFloat(bits);
    valid = valid && !isnan(value) && !isinf(value);
    return value;
}

mat4 readInverseViewProjection(int row, inout bool valid) {
    mat4 invVP;
    for (int component = 0; component < 16; component++) {
        invVP[component / 4][component % 4] = zoneFloat(12 + component, row, valid);
    }
    valid = valid && abs(determinant(invVP)) >= 1e-16;
    return invVP;
}

void readZone(ivec2 size) {
    if (size.x < 57) return;
    int row = zoneHeader(0) ? 0 : size.y - 1;
    if (!zoneHeader(row)) return;
    bool valid = true;
    vec3 references[4];
    for (int point = 0; point < 4; point++) {
        for (int axis = 0; axis < 3; axis++) {
            references[point][axis] = zoneFloat(point * 3 + axis, row, valid);
        }
    }
    mat4 invVP = readInverseViewProjection(row, valid);
    mat3 basis = mat3(references[1] - references[0],
                      references[2] - references[0],
                      references[3] - references[0]);
    if (!valid || abs(determinant(basis)) < 1e-12) return;
    zoneCenter = references[0];
    zoneInverseBasis = inverse(basis);
    zoneInverseViewProjection = invVP;
    vec3 cameraLocal = zoneInverseBasis * -zoneCenter;
    float cameraRadius = length(cameraLocal.xz);
    if (cameraRadius > 1e-6) {
        vec3 radialGradient = transpose(zoneInverseBasis) *
            vec3(cameraLocal.x / cameraRadius, 0.0, cameraLocal.z / cameraRadius);
        zoneInfo.y = (cameraRadius - 1.0) / max(length(radialGradient), 1e-8);
    }
    zoneInfo.x = 1.0;
}

int hakureiPalette(int row) {
    ivec3 code = ivec3(round(texelFetch(ZoneDataSampler, ivec2(56, row), 0).rgb * 255.0));
    return code.r == 72 && code.g == 83 && (code.b == 1 || code.b == 2) ? code.b : 0;
}

void readHakureiSpheres(ivec2 size) {
    if (!lbc_hakurei_rows_fit(size)) return;
    int count = 0;
    for (int slot = 0; slot < LBC_HAKUREI_SLOTS; slot++) {
        int row = LBC_HAKUREI_FIRST_ROW + slot;
        int palette = hakureiPalette(row);
        if (palette == 0) {
            row = size.y - 1 - row;
            palette = hakureiPalette(row);
        }
        if (palette == 0) continue;

        bool valid = true;
        vec3 center, edge;
        for (int axis = 0; axis < 3; axis++) {
            center[axis] = zoneFloat(axis, row, valid);
            edge[axis] = zoneFloat(3 + axis, row, valid);
        }
        float radius = distance(center, edge);
        if (!valid || !(radius > 0.0001) || isinf(radius)) continue;
        if (zoneInfo.x == 0.0 && count == 0) {
            mat4 invVP = readInverseViewProjection(row, valid);
            if (!valid) continue;
            zoneInverseViewProjection = invVP;
        }
        hakureiSpheres[count++] = vec4(center, palette == 1 ? radius : -radius);
    }
    if (count == 0) return;
    zoneInfo.z = float(count);
    // In a perspective projection the eye is the point at clip x=y=w=0.
    vec4 eye = zoneInverseViewProjection[2];
    if (abs(eye.w) < 1e-8) return;
    for (int i = 0; i < count; i++) {
        vec4 sphere = hakureiSpheres[i];
        float inside = abs(sphere.w) - distance(eye.xyz / eye.w, sphere.xyz);
        zoneInfo.w = max(zoneInfo.w, inside);
    }
}

void main() {
    vec2 uv = vec2((gl_VertexIndex << 1) & 2, gl_VertexIndex & 2);
    gl_Position = vec4(uv * 2.0 - 1.0, 0.0, 1.0);
    texCoord = uv;
    zoneInfo.x = 0.0;
    zoneInfo.y = -100000.0;
    zoneCenter = vec3(0.0);
    zoneInverseBasis = mat3(1.0);
    zoneInverseViewProjection = mat4(1.0);
    zoneInfo.zw = vec2(0.0);
    ivec2 size = textureSize(ZoneDataSampler, 0);
    readZone(size);
    readHakureiSpheres(size);
}
