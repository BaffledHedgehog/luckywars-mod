#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D SceneSampler;
layout(std140) uniform BadAppleConfig { float Invert; };
layout(location=0) in vec2 texCoord;
layout(location=0) out vec4 fragColor;
void main(){
    vec4 scene=texture(SceneSampler,texCoord);
    float gray=clamp(dot(scene.rgb,vec3(0.299,0.587,0.114)),0.0,1.0);
    if(Invert>0.5)gray=1.0-gray;
    fragColor=vec4(vec3(gray),scene.a);
}
