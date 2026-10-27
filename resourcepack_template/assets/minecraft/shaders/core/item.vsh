#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:light.glsl>
#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:sample_lightmap.glsl>
#include <lbc:zone_data.glsl>
#include <lbc:zone_window.glsl>
#include <lbc:black_hole_data.glsl>
#include <lbc:nuclear_data.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV1;
layout(location = 4) in ivec2 UV2;
#ifdef GLINT_SPECIAL
layout(location = 5) in vec2 UV3;
#endif
layout(location = 6) in vec3 Normal;

uniform sampler2D Sampler0;
#ifndef OIT_ALPHA_ONLY
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;
#endif
layout(location = 2) out vec4 vertexColor;
#ifndef OIT_ALPHA_ONLY
layout(location = 3) out vec4 lightMapColor;
layout(location = 4) out vec4 overlayColor;
#endif
layout(location = 5) out vec2 texCoord0;
#ifdef GLINT
layout(location = 6) out vec2 texCoordGlint;
#endif
layout(location = 9) flat out int zoneMaterial;
layout(location = 10) out vec3 zoneRelativePosition;
layout(location = 11) out float meditationWarningFade;
layout(location = 12) out float meditationWarningProgress;
layout(location = 13) out vec2 meditationWarningQuadCoord;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    #ifndef OIT_ALPHA_ONLY
    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    #endif
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
    meditationWarningProgress = Color.g; // independent red-beam countdown
    meditationWarningFade = Color.r; // raw grayscale tint, BEFORE directional light
    #ifndef OIT_ALPHA_ONLY
    lightMapColor = sample_lightmap(Sampler2, UV2);
    overlayColor = texelFetch(Sampler1, UV1, 0);
    #endif

    texCoord0 = UV0;
    // Native item quads retain four sequential vertex indices, even with sorted OIT indices.
    // This local coordinate is independent of atlas placement and sprite resolution.
    int warningCorner = gl_VertexIndex & 3;
    meditationWarningQuadCoord = vec2(
        (warningCorner == 2 || warningCorner == 3) ? 1.0 : 0.0,
        (warningCorner == 1 || warningCorner == 2) ? 1.0 : 0.0);
    #ifdef GLINT
    #ifdef GLINT_SPECIAL
    texCoordGlint = (TextureMat * vec4(UV3, 0.0, 1.0)).xy;
    #else
    texCoordGlint = (TextureMat * vec4(UV0, 0.0, 1.0)).xy;
    #endif
    #endif

    // Reserved constant tint in lbc:zone_border. Read raw Color, before light.
    zoneMaterial = all(equal(ivec3(round(Color.rgb * 255.0)), LBC_ZONE_TINT)) ? 1 : 0;
    zoneRelativePosition = Position;
    ivec3 itemTint = ivec3(round(Color.rgb * 255.0));
    if (all(equal(itemTint, LBC_BH_SMALL_TINT)) || all(equal(itemTint, LBC_BH_DISK_TINT)) ||
        all(equal(itemTint, LBC_NUCLEAR_TINT))) {
        // Post effects have no rasterized surface: all geometry is an invisible
        // outline data carrier. In particular, it must never enter OIT depth.
        gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
    }
    if (zoneMaterial != 0) {
        if (lbc_zone_tag(UV0, textureSize(Sampler0, 0)) != LBC_ZONE_WALL_TAG) {
            // Data quads are rendered only into the invisible outline channel.
            gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
        } else {
            // Rasterize only the nearby vertical band, even without post FX.
            vec3 bandPosition = Position;
            float halfHeight = lbc_zone_half_height(length(Position.xz));
            bandPosition.y = clamp(Position.y, -halfHeight, halfHeight);
            zoneRelativePosition = bandPosition;
            gl_Position = ProjMat * ModelViewMat * vec4(bandPosition, 1.0);
        }
    }
}
