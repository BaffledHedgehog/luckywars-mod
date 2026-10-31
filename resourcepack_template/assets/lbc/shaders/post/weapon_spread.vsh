#version 330
#extension GL_ARB_separate_shader_objects : require
#include <lbc:weapon_spread_data.glsl>
uniform sampler2D SpreadDataSampler;
// Pixel extents from the screen center: left, right, bottom, top.
layout(location=0) flat out vec4 spreadBounds;
layout(location=1) flat out vec2 spreadStyle;
const float PI=3.141592653589793;
bool header(int row) {
    return all(equal(ivec3(round(texelFetch(SpreadDataSampler,ivec2(56,row),0).rgb*255.0)),ivec3(83,80,82)));
}
float readField(int field,int row,inout bool valid) {
    ivec3 lo=ivec3(round(texelFetch(SpreadDataSampler,ivec2(field*2,row),0).rgb*255.0));
    ivec3 hi=ivec3(round(texelFetch(SpreadDataSampler,ivec2(field*2+1,row),0).rgb*255.0));
    valid=valid && lo.b==201 && hi.b==202;
    float value=uintBitsToFloat(uint(lo.r)|(uint(lo.g)<<8u)|(uint(hi.r)<<16u)|(uint(hi.g)<<24u));
    valid=valid && !isnan(value) && !isinf(value);
    return value;
}
void sampleAngle(float theta,vec3 n,vec2 d,inout vec2 bounds) {
    float denom=d.x*cos(theta)+d.y;
    // A horizon-crossing envelope covers the viewport. Never divide by zero.
    if(denom<=0.00001) {bounds=vec2(-100000,100000);return;}
    float value=(n.x*cos(theta)+n.y*sin(theta)+n.z)/denom;
    bounds=vec2(min(bounds.x,value),max(bounds.y,value));
}
void yawEnvelope(float q,float angle,vec3 h,vec3 side,vec3 forward,vec3 axis,inout vec2 bounds) {
    vec3 n=vec3(cos(q)*dot(h,axis),cos(q)*dot(side,axis),-sin(q)*axis.y);
    vec2 d=vec2(cos(q)*dot(h,forward),-sin(q)*forward.y);
    sampleAngle(-angle,n,d,bounds);sampleAngle(0,n,d,bounds);sampleAngle(angle,n,d,bounds);
    // d(N/D)/d(theta)=a*cos(theta)+b*sin(theta)+c.
    float a=n.y*d.y,b=n.z*d.x-n.x*d.y,c=n.y*d.x;
    float radius=length(vec2(a,b));
    if(radius<1e-10 || abs(c)>radius+1e-7)return;
    float phase=atan(b,a),offset=acos(clamp(-c/radius,-1.0,1.0));
    for(int signIndex=0;signIndex<2;signIndex++) {
        float theta=phase+(signIndex==0 ? offset : -offset);
        // Reduce to [-pi,pi], then retain only roots inside the yaw interval.
        theta=mod(theta+PI,2.0*PI)-PI;
        if(abs(theta)<=angle+0.000001)sampleAngle(clamp(theta,-angle,angle),n,d,bounds);
    }
}
void main() {
    vec2 uv=vec2((gl_VertexIndex<<1)&2,gl_VertexIndex&2);
    gl_Position=vec4(uv*2.0-1.0,0,1);
    spreadBounds=vec4(0);spreadStyle=vec2(0);
    ivec2 size=textureSize(SpreadDataSampler,0);
    if(size.x<57 || size.y<118)return;
    int row=LBC_WEAPON_SPREAD_ROW;
    if(!header(row))row=size.y-1-row;
    if(!header(row))return;
    bool valid=true;vec3 p[4];
    for(int i=0;i<4;i++)for(int a=0;a<3;a++)p[i][a]=readField(i*3+a,row,valid);
    vec2 projection=vec2(readField(12,row,valid),readField(13,row,valid));
    vec3 forward,right,up;
    for(int a=0;a<3;a++)forward[a]=readField(14+a,row,valid);
    for(int a=0;a<3;a++)right[a]=readField(17+a,row,valid);
    for(int a=0;a<3;a++)up[a]=readField(20+a,row,valid);
    if(!valid || any(lessThan(projection,vec2(0.00001))) || any(greaterThan(projection,vec2(10000))))return;
    float degrees=length(p[1]-p[0]),modeLength=length(p[2]-p[0]);
    if(isnan(degrees) || isinf(degrees) || degrees<=0.0 || degrees>=89.9 || modeLength<0.5 || modeLength>2.5)return;
    if(abs(length(forward)-1.0)>0.02 || abs(length(right)-1.0)>0.02 || abs(length(up)-1.0)>0.02)return;
    forward=normalize(forward);right=normalize(right);up=normalize(up);
    if(abs(dot(forward,right))>0.02 || abs(dot(forward,up))>0.02 || abs(dot(right,up))>0.02)return;
    int mode=int(round(modeLength));float angle=radians(degrees);
    vec2 x=vec2(-tan(angle),tan(angle)),y=x;
    if(mode==2) {
        vec3 h=vec3(forward.x,0,forward.z);
        // At the exact pole yaw is recovered from the client right vector.
        if(length(h)<0.00001)h=cross(vec3(0,1,0),vec3(right.x,0,right.z));
        if(length(h)<0.00001)return;
        h=normalize(h);vec3 side=cross(h,vec3(0,1,0));
        float pitch=-asin(clamp(forward.y,-1.0,1.0));
        x=vec2(100000,-100000);y=x;
        // For a fixed theta N/D is fractional-linear in tan(q), so
        // front-facing extrema in q lie on these two pitch boundaries.
        yawEnvelope(pitch-angle,angle,h,side,forward,right,x);
        yawEnvelope(pitch+angle,angle,h,side,forward,right,x);
        yawEnvelope(pitch-angle,angle,h,side,forward,up,y);
        yawEnvelope(pitch+angle,angle,h,side,forward,up,y);
    }
    vec2 halfFrame=vec2(size)*0.5;
    vec2 scale=projection*halfFrame;
    spreadBounds=clamp(vec4(-x.x,x.y,-y.x,y.y)*scale.xxyy,vec4(0),vec4(halfFrame.x,halfFrame.x,halfFrame.y,halfFrame.y)+vec4(8));
    float pixelScale=max(1.0,floor(min(float(size.x)/640.0,float(size.y)/360.0)));
    spreadStyle=vec2(pixelScale,4.0*pixelScale);
}
