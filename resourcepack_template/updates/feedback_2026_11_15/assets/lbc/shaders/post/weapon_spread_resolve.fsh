#version 330
#extension GL_ARB_separate_shader_objects : require
#include <lbc:reticle_header.glsl>
uniform sampler2D SceneSampler;
layout(location=0) flat in vec4 reticleBounds;
layout(location=1) flat in vec2 reticleStyle;
layout(location=2) flat in vec2 reticleCenter;
layout(location=3) flat in vec4 nativeRect;
layout(location=0) out vec4 fragColor;
void main() {
    ivec2 pixel=ivec2(gl_FragCoord.xy),size=textureSize(SceneSampler,0);
    vec4 scene=texelFetch(SceneSampler,pixel,0);
    bool nativeTag=int(round(scene.a*255.0))==251 && all(lessThan(abs(gl_FragCoord.xy-vec2(size)*.5),vec2(128)));
    bool nativeStroke=nativeTag && all(greaterThanEqual(gl_FragCoord.xy,nativeRect.xy)) && all(lessThan(gl_FragCoord.xy,nativeRect.zw));
    fragColor=scene;
    if(nativeTag || lbc_reticle_offset(pixel,size)>=0)fragColor.a=1.0;
    if(reticleStyle.x<=0)return;
    vec2 delta=gl_FragCoord.xy-reticleCenter;
    vec4 bounds=max(reticleBounds,vec4(1.0));
    float thickness=min(reticleStyle.x,min(min(bounds.x,bounds.y),min(bounds.z,bounds.w))*2.0);
    float lengthPx=reticleStyle.y;
    bool horizontal=delta.y>=-thickness*.5 && delta.y<thickness*.5 &&
        ((delta.x<0 && -delta.x<=bounds.x && -delta.x>=max(0.0,bounds.x-lengthPx)) ||
         (delta.x>=0 && delta.x<bounds.y && delta.x>=max(0.0,bounds.y-lengthPx)));
    bool vertical=delta.x>=-thickness*.5 && delta.x<thickness*.5 &&
        ((delta.y<0 && -delta.y<=bounds.z && -delta.y>=max(0.0,bounds.z-lengthPx)) ||
         (delta.y>=0 && delta.y<bounds.w && delta.y>=max(0.0,bounds.w-lengthPx)));
    bool within=delta.x>=-bounds.x && delta.x<bounds.y && delta.y>=-bounds.z && delta.y<bounds.w;
    // Exactly2x2 physical pixels even at zero spread; never a disappearing crosshair.
    vec2 dotOrigin=floor(reticleCenter)-vec2(1);
    bool centerDot=all(greaterThanEqual(gl_FragCoord.xy,dotOrigin)) &&
        all(lessThan(gl_FragCoord.xy,dotOrigin+2.0));
    bool desired=horizontal || vertical || centerDot || (nativeStroke && within);
    // The scene already contains the GUI inversion. Undo only unwanted native
    // pixels and invert only newly added pixels. Overlap is inverted just once.
    if(nativeStroke!=desired)fragColor.rgb=vec3(1.0)-scene.rgb;
}
