#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:globals.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:oit.glsl>
#include <lbc:bloom_rules.glsl>

uniform sampler2D Sampler0;

layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
layout(location = 2) in vec2 texCoord0;
layout(location = 3) in vec4 vertexColor;
layout(location = 4) flat in int customParticle;
layout(location = 5) in vec2 particleCoord;
layout(location = 6) in vec3 smokeWorldRay;

#ifndef OIT_ALPHA_ONLY
layout(location = 0) out vec4 fragColor;
#endif

vec4 calculateFinalColor(vec4 color, bool bypassFog) {
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

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    bool laser = customParticle == 1 || customParticle == 2;
    if (laser) {
        // The original 8x8, 52-pixel dot, independent of sprite packing or age.
        // Never shift UV into a neighbouring (possibly transparent) sprite.
        vec2 pixel = floor(clamp(particleCoord, 0.0, 0.99999) * 8.0) - 3.5;
        if (dot(pixel, pixel) > 16.0) discard;
        color = vertexColor * ColorModulator;
    }
    if(customParticle==3) {
        // A soft2D billow, not a fullscreen fog volume or a solid square.
        vec2 p=particleCoord*2.0-1.0;
        float edge=.79+.075*sin(p.x*9.0+p.y*5.0)+.045*sin(p.y*13.0-p.x*6.0);
        float coverage=1.0-smoothstep(edge-.34,edge,length(p));
        float shade=.45+.055*sin(p.x*7.0-p.y*8.0);
        color=vec4(vec3(shade),coverage*.9)*ColorModulator;
    }
    if(customParticle==4) {
        vec2 p=particleCoord*2.0-1.0;
        float edge=.84+.05*sin(p.x*9.0+p.y*5.0)+.03*sin(p.y*13.0-p.x*6.0);
        float coverage=1.0-smoothstep(edge-.08,edge,length(p));
        // Equal, opaque interiors do not reveal seams where billboards overlap.
        // Only the outer rim and the final native tick fade into the next puff.
        // Shared world-oriented shading: every puff on this view ray has the
        // same colour, so depth order cannot draw a patchwork of visible seams.
        vec3 camera=vec3(CameraBlockPos)-CameraOffset;
        vec3 q=normalize(smokeWorldRay)*18.0+camera*.12+GameTime*24000.0*vec3(.009,.004,-.006);
        float billow=sin(dot(q,vec3(1.17,.93,1.42))+.4*sin(dot(q,vec3(-1.3,2.2,.7))));
        float shade=.46+.035*billow;
        color=vec4(vec3(shade),coverage*vertexColor.a)*ColorModulator;
    }
    if (color.a < 0.1) {
        discard;
    }
    #ifdef OIT_ALPHA_ONLY
    executeAlphaOnlyPhase(gl_FragCoord.z, color.a);
    #else
    fragColor = calculateFinalColor(color, laser);
    #ifndef OIT
    // The material tag is owned by item/entity emission, never terrain/particles.
    fragColor = lbc_bloom_unmarked(fragColor);
    #endif
    #endif
}
