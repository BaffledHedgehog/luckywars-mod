#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D GateSampler;
layout(location=0) out vec2 texCoord;
void main(){
    vec2 uv=vec2((gl_VertexIndex<<1)&2,gl_VertexIndex&2);
    texCoord=uv;
    bool enabled=texelFetch(GateSampler,ivec2(0),0).r>0.5;
    gl_Position=enabled?vec4(uv*2.0-1.0,0,1):vec4(2,2,0,1);
}
