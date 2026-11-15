#version 330
#extension GL_ARB_separate_shader_objects : require
#include <minecraft:globals.glsl>
uniform sampler2D FrameSampler;
layout(location=0) in vec2 texCoord;
layout(location=0) out vec4 fragColor;
const int HEADER[12]=int[12](213,17,93,202,107,44,199,61,173,5,221,89);
void main(){
    fragColor=texture(FrameSampler,texCoord);
    ivec2 pixel=ivec2(gl_FragCoord.xy),size=ivec2(ScreenSize);
    int x=pixel.x-(size.x-12);
    if(x>=0&&x<12&&(pixel.y==0||pixel.y==size.y-1))fragColor.a=float(HEADER[x])/255.0;
}
