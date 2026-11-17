#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:globals.glsl>
#include <lbc:zone_damage.glsl>
#include <lbc:black_hole_transfer.glsl>
#include <lbc:nuclear_transfer.glsl>
#include <lbc:effect_camera_transfer.glsl>

uniform sampler2D SceneSampler;
uniform sampler2D HoleDataSampler;
layout(location = 0) in vec2 texCoord;
layout(location = 9) flat in vec4 zoneInfo;
layout(location = 0) out vec4 fragColor;

void main() {
    vec4 scene = texture(SceneSampler, texCoord);
    fragColor = scene;
    fragColor.a = lbc_bh_encode_alpha(HoleDataSampler, ivec2(gl_FragCoord.xy), textureSize(SceneSampler, 0), scene.a);
    fragColor.a = lbc_nuclear_encode_alpha(HoleDataSampler, ivec2(gl_FragCoord.xy), textureSize(SceneSampler, 0), fragColor.a);
    fragColor.a = lbc_effect_camera_alpha(HoleDataSampler, ivec2(gl_FragCoord.xy), textureSize(SceneSampler, 0), fragColor.a);
    // Existing nexus particles send a control marker through these four
    // corner pixels; the later end_of_frame chain must receive it unchanged.
    ivec2 pixel = ivec2(texCoord * ScreenSize);
    if (all(lessThan(pixel, ivec2(4))) &&
        all(lessThanEqual(abs(ivec3(round(scene.rgb * 255.0)) - ivec3(72, 82, 25)), ivec3(1)))) return;
    if (zoneInfo.x != 0.0) {
        fragColor.rgb = lbc_zone_damage(scene.rgb, texCoord, zoneInfo.y);
    }
}
