#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:globals.glsl>
#include <lbc:zone_data.glsl>
#include <lbc:black_hole_data.glsl>
#include <lbc:nuclear_data.glsl>
#include <lbc:signed_glow_data.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
uniform sampler2D Sampler0;

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out vec2 texCoord0;
layout(location = 2) flat out int zoneDataIndex;
layout(location = 3) out float zoneDataValue;
layout(location = 4) flat out int blackHoleKind;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    texCoord0 = UV0;
    zoneDataIndex = -1;
    zoneDataValue = 0.0;
    blackHoleKind = -1;
    int blackHoleSlot = lbc_bh_slot(Color.rgb);
    int nuclearSlot = lbc_nuclear_slot(Color.rgb);
    int signedGlowSlot=lbc_signed_glow_slot(Color.rgb);
    if (signedGlowSlot >= 0 || blackHoleSlot >= 0 || nuclearSlot >= 0 || all(equal(ivec3(round(Color.rgb * 255.0)), LBC_ZONE_GLOW))) {
        int tag = lbc_zone_tag(UV0, textureSize(Sampler0, 0));
        if (tag < LBC_ZONE_DATA_PIXELS) {
            zoneDataIndex = tag;
            if (blackHoleSlot >= 0) {
                blackHoleKind = fract(UV0.y * float(textureSize(Sampler0, 0).y)) > 0.5 ? 1 : 0;
            }
            if (nuclearSlot >= 0) blackHoleKind = lbc_nuclear_fade_carrier(Color.rgb) ? 3 : 2;
            if(signedGlowSlot>=0) blackHoleKind=4;
            int field = tag / 2;
            if (field < 12) {
                // CPU-transformed reference points already include scale,
                // translation, rotation and display interpolation.
                zoneDataValue = Position[field % 3];
            } else if (tag != 56) {
                mat4 inverseViewProjection = inverse(ProjMat * ModelViewMat);
                int component = field - 12;
                zoneDataValue = inverseViewProjection[component / 4][component % 4];
            }
            // One exact framebuffer pixel per helper. The post shader accepts
            // either edge row, covering the OpenGL/Vulkan viewport convention.
            int corner = gl_VertexIndex % 4;
            vec2 edge = vec2((corner == 0 || corner == 3) ? 0.0 : 1.0,
                             corner < 2 ? 0.0 : 1.0);
            float row = blackHoleSlot >= 0 ? float(LBC_BH_FIRST_ROW + blackHoleSlot) : 0.0;
            if (nuclearSlot >= 0) row = float((blackHoleKind == 3 ? LBC_NUCLEAR_FADE_FIRST_ROW : LBC_NUCLEAR_FIRST_ROW) + nuclearSlot);
            if(signedGlowSlot>=0) row=float(LBC_SIGNED_GLOW_FIRST_ROW+signedGlowSlot);
            vec2 pixel = vec2(float(tag), row) + edge;
            gl_Position = vec4(pixel / ScreenSize * 2.0 - 1.0, 0.0, 1.0);
        } else {
            // The analytic cylinder is drawn by the post shader, not this mask.
            gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
        }
    }
}
