#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:globals.glsl>
#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:oit.glsl>
#include <lbc:zone_window.glsl>
#include <lbc:meditation_warning.glsl>

uniform sampler2D Sampler0;

#ifdef GLINT
uniform sampler2D GlintSampler;
#endif

#ifndef OIT_ALPHA_ONLY
layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
#endif
layout(location = 2) in vec4 vertexColor;
#ifndef OIT_ALPHA_ONLY
layout(location = 3) in vec4 lightMapColor;
layout(location = 4) in vec4 overlayColor;
#endif
layout(location = 5) in vec2 texCoord0;
#ifdef GLINT
layout(location = 6) in vec2 texCoordGlint;
#endif
layout(location = 9) flat in int zoneMaterial;
layout(location = 10) in vec3 zoneRelativePosition;
layout(location = 11) in float meditationWarningFade;
layout(location = 12) in float meditationWarningProgress;
layout(location = 13) in vec2 meditationWarningQuadCoord;

#ifndef OIT_ALPHA_ONLY
layout(location = 0) out vec4 fragColor;
#endif

#ifndef OIT_ALPHA_ONLY
vec4 calculateFinalColor(vec4 color, bool bypassLighting, bool bypassFog) {
    if (!bypassLighting) {
        color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
        color *= lightMapColor;

    }

    #ifdef GLINT
    vec4 glintColor = GlintAlpha * texture(GlintSampler, texCoordGlint);// Glint color modulator?
    // Matches BlendFuntion.GLINT
    color.rgb += glintColor.rgb * glintColor.rgb;
    #endif

    #ifdef OIT_ACCUMULATE
    color = sampleColorForAccumulation(color);
    vec4 fogColor = vec4(FogColor.rgb * color.a, FogColor.a);
    #else
    vec4 fogColor = FogColor;
    #endif

    if (bypassFog) {
        return color;
    }
    return apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, fogColor);
}
#endif

void main() {
    vec4 rawColor = texture(Sampler0, texCoord0);
    int alphaMarker = int(round(rawColor.a * 255.0));
    float meditationBase = lbc_meditation_warning_base_alpha(rawColor);
    bool isMeditationWarning = meditationBase >= 0.0;
    bool isZoneBorder = zoneMaterial != 0;
    bool bypassLighting = isMeditationWarning || isZoneBorder || alphaMarker == 254 || alphaMarker == 130;
    bool bypassFog = bypassLighting;
    // Only the requested 1x1, 10%-opaque red fallback is drawn here.
    // All energy detail belongs to the post effect.
    vec4 color = isMeditationWarning
        ? vec4(lbc_meditation_warning_color(rawColor, meditationWarningProgress), meditationBase * clamp(meditationWarningFade, 0.0, 1.0) * ColorModulator.a)
        : (isZoneBorder ? vec4(1.0, 0.0, 0.0, 26.0 / 255.0) : rawColor);
    if (lbc_meditation_warning_is_ring(rawColor)) {
        // Double-sided model: draw only the facing quad, also on no-cull layers.
        if (!gl_FrontFacing) discard;
        color.a *= lbc_meditation_warning_ring_coverage(meditationWarningQuadCoord);
    }
    if (isZoneBorder) {
        color.a *= lbc_zone_window(zoneRelativePosition);
        if (color.a <= 0.0) discard;
    }
    #ifdef ALPHA_CUTOUT
    if (color.a < (isMeditationWarning ? 0.00001 : (isZoneBorder ? 0.005 : ALPHA_CUTOUT))) {
        discard;
    }
    #endif

    if (isMeditationWarning) {
        if (color.a <= 0.00001) discard;
    } else if (isZoneBorder) {
        color.a *= ColorModulator.a;
    } else if (alphaMarker == 254) {
        color = vec4(rawColor.rgb, 1.0);
    } else if (alphaMarker == 130) {
        color = vec4(rawColor.rgb, 0.5);
    } else {
        color *= vertexColor * ColorModulator;
    }

    #ifdef GLINT
    color.a = max(color.a, GlintAlpha);
    #endif

    #ifdef OIT_ALPHA_ONLY
    executeAlphaOnlyPhase(gl_FragCoord.z, color.a);
    #else
    fragColor = calculateFinalColor(color, bypassLighting, bypassFog);
    #endif
}
