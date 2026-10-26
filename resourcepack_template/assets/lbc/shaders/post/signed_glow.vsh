#version 330
#extension GL_ARB_separate_shader_objects : require
#include <lbc:signed_glow_data.glsl>
uniform sampler2D GlowDataSampler;
// Exactly64 varying components, including all eight independent actors.
layout(location=0) flat out vec4 glowGeometry[8];
layout(location=8) flat out vec4 glowStyle[8];
bool header(int row) {
    return all(equal(ivec3(round(texelFetch(GlowDataSampler,ivec2(56,row),0).rgb*255.0)),ivec3(71,76,87)));
}
float readField(int field,int row,inout bool valid) {
    ivec3 lo=ivec3(round(texelFetch(GlowDataSampler,ivec2(field*2,row),0).rgb*255.0));
    ivec3 hi=ivec3(round(texelFetch(GlowDataSampler,ivec2(field*2+1,row),0).rgb*255.0));
    valid=valid && lo.b==201 && hi.b==202;
    float value=uintBitsToFloat(uint(lo.r)|(uint(lo.g)<<8u)|(uint(hi.r)<<16u)|(uint(hi.g)<<24u));
    valid=valid && !isnan(value) && !isinf(value);
    return value;
}
vec3 palette(int index) {
    const vec3 colors[16]=vec3[](vec3(0),vec3(0,0,170),vec3(0,170,0),vec3(0,170,170),
        vec3(170,0,0),vec3(170,0,170),vec3(255,170,0),vec3(170),vec3(85),vec3(85,85,255),
        vec3(85,255,85),vec3(85,255,255),vec3(255,85,85),vec3(255,85,255),vec3(255,255,85),vec3(255));
    return colors[clamp(index,0,15)]/255.0;
}
void main() {
    vec2 uv=vec2((gl_VertexIndex<<1)&2,gl_VertexIndex&2);
    gl_Position=vec4(uv*2.0-1.0,0,1);
    ivec2 size=textureSize(GlowDataSampler,0);
    for(int slot=0;slot<8;slot++) {
        glowGeometry[slot]=vec4(0);glowStyle[slot]=vec4(0);
        if(size.x<57 || size.y<116) continue;
        int row=LBC_SIGNED_GLOW_FIRST_ROW+slot;
        if(!header(row)) row=size.y-1-row;
        if(!header(row)) continue;
        bool valid=true;vec3 p[4];
        for(int i=0;i<4;i++)for(int a=0;a<3;a++)p[i][a]=readField(i*3+a,row,valid);
        mat4 invVP;
        for(int i=0;i<16;i++)invVP[i/4][i%4]=readField(12+i,row,valid);
        if(!valid || abs(determinant(invVP))<1e-16)continue;
        mat4 vp=inverse(invVP);vec4 center=vp*vec4(p[0],1);vec4 top=vp*vec4(p[2],1);
        if(center.w<=0.001 || top.w<=0.001)continue;
        vec2 point=center.xy/center.w*0.5+0.5;
        float height=length((top.xy/top.w-center.xy/center.w)*vec2(size))*0.25;
        float logarithm=clamp(length(p[1]-p[0]),1.0,31.0);
        float radius=min(float(max(size.x,size.y)),max(2.0,height*(exp2(logarithm)-1.0)*0.22));
        glowGeometry[slot]=vec4(point*vec2(size),max(height,1.0),radius);
        glowStyle[slot]=vec4(palette(int(round(length(p[3]-p[0])))-1),min(0.95,logarithm*0.13));
    }
}
