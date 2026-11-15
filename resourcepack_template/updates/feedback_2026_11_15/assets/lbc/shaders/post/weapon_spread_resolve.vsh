#version 330
#extension GL_ARB_separate_shader_objects : require
#include <lbc:reticle_header.glsl>
uniform sampler2D SceneSampler;
layout(location=0) flat out vec4 reticleBounds;
layout(location=1) flat out vec2 reticleStyle;
layout(location=2) flat out vec2 reticleCenter;
layout(location=3) flat out vec4 nativeRect;
int byteAt(int n,int row,ivec2 size) {
    return int(round(texelFetch(SceneSampler,ivec2(size.x-20-n,row),0).a*255.0));
}
bool header(int row,ivec2 size) {
    for(int i=0;i<4;i++)if(byteAt(i,row,size)!=LBC_RETICLE_MAGIC[i])return false;
    return true;
}
bool nativePixel(ivec2 p,ivec2 size) {
    return all(greaterThanEqual(p,ivec2(0))) && all(lessThan(p,size)) &&
        int(round(texelFetch(SceneSampler,p,0).a*255.0))==251;
}
void main() {
    vec2 uv=vec2((gl_VertexIndex<<1)&2,gl_VertexIndex&2);
    gl_Position=vec4(uv*2.0-1.0,0,1);
    reticleBounds=vec4(0);reticleStyle=vec2(0);nativeRect=vec4(0);
    ivec2 size=textureSize(SceneSampler,0);
    reticleCenter=vec2(size)*.5;
    if(size.x<48 || size.y<2)return;
    int row=header(0,size)?0:size.y-1;
    if(!header(row,size))return;
    float fields[6];
    for(int i=0;i<6;i++) {
        uint bits=0u;
        for(int b=0;b<4;b++)bits|=uint(byteAt(4+i*4+b,row,size))<<uint(b*8);
        fields[i]=uintBitsToFloat(bits);
        if(isnan(fields[i]) || isinf(fields[i]) || fields[i]<0)return;
    }
    if(fields[4]<=0 || fields[4]>64 || fields[5]>256)return;
    // Inspect the actual GUI stroke, including its pixel-snapped center/GUI scale.
    // With F1, a scope or a menu there is no native stroke: retain that behavior.
    ivec2 middle=size/2,found=ivec2(-1);
    for(int y=-1;y<=1;y++)for(int x=-1;x<=1;x++)
        if(nativePixel(middle+ivec2(x,y),size))found=middle+ivec2(x,y);
    if(found.x<0)return;
    ivec2 lo=found,hi=found;
    for(int i=-128;i<=128;i++)for(int d=-1;d<=1;d++) {
        ivec2 x=ivec2(middle.x+i,middle.y+d),y=ivec2(middle.x+d,middle.y+i);
        if(nativePixel(x,size)){lo.x=min(lo.x,x.x);hi.x=max(hi.x,x.x);}
        if(nativePixel(y,size)){lo.y=min(lo.y,y.y);hi.y=max(hi.y,y.y);}
    }
    nativeRect=vec4(lo,hi+1);
    reticleCenter=(vec2(lo+hi)+1.0)*.5;
    reticleBounds=vec4(fields[0],fields[1],fields[2],fields[3]);
    reticleStyle=vec2(fields[4],fields[5]);
}
