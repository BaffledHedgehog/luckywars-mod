#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D SceneSampler;
uniform sampler2D HistorySampler;
layout(location=0) out vec4 fragColor;
const int HEADER[12]=int[12](213,17,93,202,107,44,199,61,173,5,221,89);
bool headerAt(int y){ivec2 size=textureSize(SceneSampler,0);if(size.x<12)return false;for(int i=0;i<12;i++)if(int(round(texelFetch(SceneSampler,ivec2(size.x-12+i,y),0).a*255.0))!=HEADER[i])return false;return true;}
void main(){bool active=headerAt(0)||headerAt(textureSize(SceneSampler,0).y-1);bool previous=texelFetch(HistorySampler,ivec2(0),0).r>0.5;fragColor=vec4(active?1:0,active&&!previous?1:0,0,1);}
