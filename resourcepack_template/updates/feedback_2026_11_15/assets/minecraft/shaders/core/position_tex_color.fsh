#version 330
#extension GL_ARB_separate_shader_objects : require

#ifndef MINECRAFT_GLOBALS_GLSL
#define MINECRAFT_GLOBALS_GLSL

layout(std140) uniform Globals {
    ivec3 CameraBlockPos;
    float GlintAlpha;
    vec3 CameraOffset;
    float GameTime;
    vec2 ScreenSize;
    int MenuBlurRadius;
    int UseRgss;
};

#endif


// Can't moj_import in things used during startup, when resource packs don't exist.
// This is a copy of dynamicimports.glsl
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};

uniform sampler2D Sampler0;

layout(location = 0) in vec2 texCoord0;
layout(location = 1) in vec4 vertexColor;
layout(location = 2) flat in vec2 centeredCrosshairScale;

layout(location = 0) out vec4 fragColor;

#define SPEED 400.0
#define WAVES 2.0

float rand(vec2 co) {
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

vec3 hsv_to_rgb(vec3 hsv) {
    vec3 rgb = clamp(abs(fract(hsv.xxx + vec3(0.0, 2.0 / 3.0, 1.0 / 3.0)) * 6.0 - 3.0) - 1.0, 0.0, 1.0);
    rgb = mix(vec3(1.0), rgb, hsv.y);
    rgb *= hsv.z;
    return rgb;
}

vec3 getPulseColor() {
    float pulseTime = fract(GameTime * SPEED);
    float pulse = 0.5 * (1.0 + sin(pulseTime * 2.0 * 3.14159));
    vec3 darkRed = vec3(0.5, 0.0, 0.0);
    vec3 black = vec3(0.1, 0.0, 0.0);
    return mix(black, darkRed, pulse);
}

bool isColorNear(ivec3 color, ivec3 target, int tolerance) {
    ivec3 delta = abs(color - target);
    return all(lessThanEqual(delta, ivec3(tolerance)));
}

void main() {
    if(centeredCrosshairScale.x>0.0) {
        // Keep native stroke width and snap its edges to physical pixels.
        // Exact geometric centering would soften odd-width strokes on even
        // framebuffers (and vice versa); the nearest sharp center is <=0.5px away.
        vec2 pixelScale=max(round(centeredCrosshairScale),vec2(1.0));
        vec2 center=floor((ScreenSize-pixelScale)*0.5)+pixelScale*0.5;
        vec2 d=abs(gl_FragCoord.xy-center);
        bvec2 stroke=lessThan(d,pixelScale*0.5);
        bvec2 reach=lessThan(d,pixelScale*4.5);
        if(!((stroke.x&&reach.y)||(stroke.y&&reach.x)))discard;
        // INVERT blending needs fully white RGB as well as opaque alpha.
        fragColor=vec4((vertexColor*ColorModulator).rgb,251.0/255.0);
        return;
    }
    vec4 color = texture(Sampler0, texCoord0) * vertexColor;
    if (color.a == 0.0) {
        discard;
    }

    vec2 uv = gl_FragCoord.xy / ScreenSize;
    vec3 currentRGB = hsv_to_rgb(vec3(fract(uv.x * WAVES) + fract(GameTime * SPEED), 0.6, 1.0));
    ivec3 checkColor = ivec3(round(color.rgb * 255.0));

    if (isColorNear(checkColor, ivec3(78, 92, 36), 1)) {
        color = vec4(currentRGB, color.a);
    } else if (isColorNear(checkColor, ivec3(21, 25, 10), 1)) {
        color = vec4(currentRGB * 0.1, color.a);
    } else if (isColorNear(checkColor, ivec3(170, 2, 2), 1)) {
        color = vec4(getPulseColor(), color.a);
        if (rand(vec2(float(floor(texCoord0.x * 1000.0) / 1000.0) + GameTime, float(floor(texCoord0.y * 1000.0) / 1000.0) + GameTime)) <= 0.4) {
            color = vec4(0.0, 0.0, 0.0, color.a);
        }
    } else if (isColorNear(checkColor, ivec3(74, 3, 15), 1)) {
        color = vec4(getPulseColor(), color.a) * vec4(0.5, 0.5, 0.5, 1.0);
        if (rand(vec2(float(floor(texCoord0.x * 1000.0) / 1000.0) + GameTime, float(floor(texCoord0.y * 1000.0) / 1000.0) + GameTime)) <= 0.4) {
            color = vec4(0.0, 0.0, 0.0, color.a);
        }
    }

    fragColor = color * ColorModulator;
}
