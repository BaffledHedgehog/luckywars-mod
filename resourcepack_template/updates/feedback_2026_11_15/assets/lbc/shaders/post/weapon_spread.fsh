#version 330
#extension GL_ARB_separate_shader_objects : require
#include <lbc:reticle_header.glsl>
uniform sampler2D SceneSampler;
layout(location=0) flat in vec4 spreadBounds;
layout(location=1) flat in vec2 spreadStyle;
layout(location=0) out vec4 fragColor;
void main() {
    ivec2 pixel=ivec2(gl_FragCoord.xy);
    fragColor=texelFetch(SceneSampler,pixel,0);
    int n=lbc_reticle_offset(pixel,textureSize(SceneSampler,0));
    if(n<0)return;
    int value;
    if(n<4)value=LBC_RETICLE_MAGIC[n];
    else {
        int field=(n-4)/4;
        float v=field<4 ? spreadBounds[field] : spreadStyle[field-4];
        value=int((floatBitsToUint(v)>>uint(((n-4)%4)*8))&255u);
    }
    fragColor.a=float(value)/255.0;
}
