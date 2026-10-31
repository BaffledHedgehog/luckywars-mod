#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D SceneSampler;
layout(location=0) flat in vec4 spreadBounds;
layout(location=1) flat in vec2 spreadStyle;
layout(location=0) out vec4 fragColor;
void main() {
    ivec2 pixel=ivec2(gl_FragCoord.xy);
    vec4 scene=texelFetch(SceneSampler,pixel,0);
    vec2 delta=gl_FragCoord.xy-vec2(textureSize(SceneSampler,0))*0.5;
    float thickness=spreadStyle.x,lengthPx=spreadStyle.y;
    bool horizontal=delta.y>=-thickness*0.5 && delta.y<thickness*0.5 &&
        ((delta.x<0.0 && -delta.x<=spreadBounds.x && -delta.x>=max(0.0,spreadBounds.x-lengthPx)) ||
         (delta.x>=0.0 && delta.x<=spreadBounds.y && delta.x>=max(0.0,spreadBounds.y-lengthPx)));
    bool vertical=delta.x>=-thickness*0.5 && delta.x<thickness*0.5 &&
        ((delta.y<0.0 && -delta.y<=spreadBounds.z && -delta.y>=max(0.0,spreadBounds.z-lengthPx)) ||
         (delta.y>=0.0 && delta.y<=spreadBounds.w && delta.y>=max(0.0,spreadBounds.w-lengthPx)));
    fragColor=vec4(horizontal || vertical ? vec3(1.0)-scene.rgb : scene.rgb,scene.a);
}
