#version 330
#extension GL_ARB_separate_shader_objects : require
#include <minecraft:globals.glsl>
uniform sampler2D InSampler;
layout(std140) uniform SamplerInfo { vec2 OutSize; vec2 InSize; };
layout(std140) uniform SignedBlindnessConfig { float Level; };
layout(location=0) in vec2 texCoord;
layout(location=0) out vec4 fragColor;

void main() {
    vec4 scene=texture(InSampler,texCoord);
    float strength=clamp(Level,1.0,16.0);
    // A fixed screen-space grain avoids camera blur; its pattern changes over time.
    vec2 pixel=floor(texCoord*OutSize);
    float phase=floor(GameTime*1200.0*8.0);
    float grain=fract(sin(dot(pixel,vec2(12.9898,78.233))+phase*37.719)*43758.5453)-0.5;
    float contrast=1.0+strength*0.24;
    float noise=0.025+strength*0.012;
    vec3 color=(scene.rgb-0.5)*contrast+0.5;
    color+=grain*noise;
    // Quantization and noisy thresholds produce the requested fried/dithered look.
    float steps=max(5.0,32.0-strength*1.5);
    color=floor(color*steps+0.5+grain*(0.15+strength*0.055))/steps;
    fragColor=vec4(clamp(color,0.0,1.0),scene.a);
}
