#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D HistorySampler;
layout(location=0) out vec4 fragColor;
void main(){bool ready=texelFetch(HistorySampler,ivec2(0),0).r>0.5;fragColor=vec4(1,ready?0:1,0,1);}
