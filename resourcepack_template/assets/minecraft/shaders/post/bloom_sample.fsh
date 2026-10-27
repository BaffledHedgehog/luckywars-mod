#version 330
#extension GL_ARB_separate_shader_objects : require
#define GLOW_COLORS 15
#include <minecraft:globals.glsl>
uniform sampler2D DiffuseSampler;

layout(std140) uniform BlitConfig {
    vec4 ColorModulate;
};

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

void main(){
    vec4 checkColor = texture(DiffuseSampler, texCoord) * ColorModulate;
    vec4 outColor = vec4(0);
    ivec3[GLOW_COLORS] glowColorList = ivec3[](
        ivec3(0,48,255),
        ivec3(255,252,253),
        ivec3(238,50,115),
        ivec3(43,216,147),
        ivec3(255,14,14),
        ivec3(51,220,255),
        ivec3(52,55,229),
        ivec3(195,0,255),
        ivec3(147,0,237),
        ivec3(116,0,228),
        ivec3(105,218,82),
        ivec3(255,142,50),
        ivec3(235,79,255),
        ivec3(180,53,255),
        ivec3(122,42,232)
    );

    for(int i = 0; i < GLOW_COLORS; i++) {
        // Red energy is continuous. Exact RGB equality made its glow toggle
        // when movement/filtering changed a channel by a single byte.
        if (i == 4) continue;
        if (ivec3(checkColor.xyz * 255) == glowColorList[i]) {
            outColor += checkColor;
        }
    }
    vec3 redCore = vec3(255.0, 14.0, 14.0) / 255.0;
    float redDistance = length((checkColor.rgb - redCore) * vec3(1.0, 2.0, 2.0));
    float redGlow = 1.0 - smoothstep(0.005, 0.09, redDistance);
    outColor = max(outColor, checkColor * redGlow);
    fragColor = outColor;
}
