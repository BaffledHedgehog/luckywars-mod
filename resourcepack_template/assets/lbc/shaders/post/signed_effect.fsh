#version 330
#extension GL_ARB_separate_shader_objects : require
#include <minecraft:globals.glsl>
uniform sampler2D InSampler;
layout(std140) uniform SamplerInfo { vec2 OutSize; vec2 InSize; };
layout(std140) uniform SignedEffectConfig { float Mode; float Level; };
layout(location=0) in vec2 texCoord;
layout(location=0) out vec4 fragColor;
void main() {
    vec2 uv=texCoord;
    float strength=clamp(Level,1.0,16.0);
    if(Mode>3.5) {
        float t=GameTime*1200.0;
        float amplitude=0.006*strength;
        vec2 centered=uv-0.5;
        float turn=sin(t*1.7)*amplitude;
        uv=mat2(cos(turn),-sin(turn),sin(turn),cos(turn))*centered+0.5;
        uv+=amplitude*vec2(sin(uv.y*17.0+t*2.7),cos(uv.x*13.0+t*2.1));
    }
    vec4 original=texture(InSampler,clamp(uv,vec2(0.0),vec2(1.0)));
    vec3 color=original.rgb;
    if(Mode<0.5) color*=max(0.0,(3.0-Level)*0.35);
    else if(Mode<1.5) {
        color=(color-0.5)*(1.0+strength*0.07)+0.5;
        color*=1.0+strength*0.18;
        color+=strength*0.012;
    } else if(Mode<2.5) {
        color=vec3(1.0)-color;
        color=(color-0.5)*(1.0+strength*0.08)+0.5;
    } else if(Mode<3.5) color=(color-0.5)*(1.0+strength*0.12)+0.5;
    else {
        float t=GameTime*1200.0;
        float edge=clamp(length(texCoord-0.5)*1.4,0.0,1.0);
        float pulse=0.5+0.5*sin(t*2.3);
        color*=1.0-edge*min(0.8,strength*0.045)*(0.5+0.5*pulse);
        color.r+=edge*strength*0.008*pulse;
    }
    fragColor=vec4(clamp(color,0.0,1.0),original.a);
}
