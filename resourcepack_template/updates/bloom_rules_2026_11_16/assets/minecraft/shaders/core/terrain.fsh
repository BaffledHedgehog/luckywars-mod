#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#define LBC_ATLAS_GLITCH_FRAGMENT
#include <lbc:atlas_glitch.glsl>
#include <minecraft:globals.glsl>
#include <minecraft:texture_sampling.glsl>
#include <minecraft:oit.glsl>
#include <lbc:bloom_rules.glsl>
#include <minecraft:terrainglobals.glsl>
#ifndef MULTIDRAW_TERRAIN
    #include <minecraft:chunksection.glsl>
#endif

uniform sampler2D Sampler0;

layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
layout(location = 2) in vec4 vertexColor;
layout(location = 3) in vec2 texCoord0;
layout(location = 4) in float chunkVisibility;

#ifndef OIT_ALPHA_ONLY
layout(location = 0) out vec4 fragColor;
#endif

vec4 calculateFinalColor(vec4 color) {
    #ifdef OIT_ACCUMULATE
    color = sampleColorForAccumulation(color);
    vec4 fogColor = vec4(FogColor.rgb * color.a, FogColor.a);
    #else
    vec4 fogColor = FogColor;
    #endif
    return apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, fogColor);
}

layout(location = 5) flat in float atlasGlitchCode;

void main() {
    vec4 rawColor = UseRgss == 1 ? sampleRGSS(Sampler0, texCoord0, 1.0f / TextureSize) : sampleNearest(Sampler0, texCoord0, 1.0f / TextureSize);
    vec4 color = rawColor * vertexColor;
    // Keep native coverage, including the separate OIT alpha/depth phase.
    // Only the atlas color lookup moves; geometry and transparency stay valid.
    #ifndef OIT_ALPHA_ONLY
    if (atlasGlitchCode > 0.0) color.rgb = lbc_atlas_glitch_rgb(Sampler0, texCoord0, atlasGlitchCode, rawColor.rgb) * vertexColor.rgb;
    #endif
    #ifndef OIT_ALPHA_ONLY
    color = mix(FogColor * vec4(1, 1, 1, color.a), color, chunkVisibility);
    #endif
    #ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) {
        discard;
    }
    #endif

    #ifdef OIT_ALPHA_ONLY
    executeAlphaOnlyPhase(gl_FragCoord.z, color.a);
    #else
    fragColor = calculateFinalColor(color);
    #ifndef OIT
    // The material tag is owned by item/entity emission, never terrain/particles.
    fragColor = lbc_bloom_unmarked(fragColor);
    #endif
    #endif
}
