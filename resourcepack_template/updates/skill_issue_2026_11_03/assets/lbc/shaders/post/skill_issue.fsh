#version 330
#extension GL_ARB_separate_shader_objects : require
#include <minecraft:globals.glsl>
uniform sampler2D InSampler;
layout(location=0) in vec2 texCoord;
layout(location=0) out vec4 fragColor;

float grain(vec2 p) {
    p=fract(p*vec2(123.34,456.21));p+=dot(p,p+45.32);
    return fract(p.x*p.y);
}
vec3 tripPalette(float h) { return 0.5+0.5*cos(6.2831853*(h+vec3(0.0,0.33,0.67))); }
void main() {
    float t=GameTime*1200.0;
    vec2 size=vec2(textureSize(InSampler,0));
    vec2 aspect=vec2(size.x/max(size.y,1.0),1.0);
    vec2 point=(texCoord-0.5)*aspect;
    float turn=0.035*sin(t*0.61);
    point=mat2(cos(turn),-sin(turn),sin(turn),cos(turn))*point;
    vec2 flowing=point+0.026*vec2(sin(point.y*9.0+t*.77),cos(point.x*8.0-t*.63));
    flowing+=0.011*vec2(sin(point.x*27.0-t*1.13),sin(point.y*31.0+t*.91));
    vec2 uv=clamp(flowing/aspect+0.5,vec2(.001),vec2(.999));
    vec2 split=vec2(1.8+1.2*sin(t*.9),1.1*cos(t*.7))/size;
    vec3 scene=vec3(texture(InSampler,clamp(uv+split,vec2(0.0),vec2(1.0))).r,
                    texture(InSampler,uv).g,
                    texture(InSampler,clamp(uv-split,vec2(0.0),vec2(1.0))).b);
    // Moving, softly merged squares recall the original noisy colored overlay without a font atlas.
    vec2 grid=flowing*vec2(44.0,44.0)+vec2(t*.24,-t*.19);
    vec2 cell=floor(grid),local=fract(grid);
    float seed=grain(cell);
    float edge=min(min(local.x,1.0-local.x),min(local.y,1.0-local.y));
    float face=smoothstep(.025,.18,edge);
    float occupied=smoothstep(.35,.6,seed+.12*sin(t*.71+seed*15.0));
    vec3 tile=tripPalette(seed*.7+t*.035+point.y*.14);
    float fine=grain(floor(texCoord*size*.5)+floor(t*9.0));
    float stipple=smoothstep(.69,.88,fine);
    vec3 inverted=1.0-scene;
    vec3 drug=mix(scene,inverted,.13*occupied);
    drug=mix(drug,tile,.27*occupied*face);
    drug+=tile*stipple*(.05+.12*occupied);
    drug*=.91+.09*face;
    fragColor=vec4(clamp(drug,0.0,1.0),texture(InSampler,texCoord).a);
}
