#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D SceneSampler;
layout(location=0) flat in vec4 glowGeometry[8];
layout(location=8) flat in vec4 glowStyle[8];
layout(location=0) out vec4 fragColor;
void main() {
    ivec2 pixel=ivec2(gl_FragCoord.xy);
    vec4 scene=texelFetch(SceneSampler,pixel,0);
    vec3 halo=vec3(0);
    for(int slot=0;slot<8;slot++) {
        vec4 g=glowGeometry[slot];if(g.w<=0.0)continue;
        vec2 delta=max(abs(gl_FragCoord.xy-g.xy)-vec2(g.z*0.32,g.z),vec2(0));
        float weight=exp(-dot(delta,delta)/max(g.w*g.w,1.0)*3.0)*glowStyle[slot].a;
        halo+=glowStyle[slot].rgb*weight;
    }
    fragColor=vec4(vec3(1)-(vec3(1)-scene.rgb)*(vec3(1)-clamp(halo,0.0,1.0)),scene.a);
}
