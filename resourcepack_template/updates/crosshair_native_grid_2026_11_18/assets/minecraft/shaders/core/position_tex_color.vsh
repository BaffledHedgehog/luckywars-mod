#version 330
#extension GL_ARB_separate_shader_objects : require

// Can't moj_import in things used during startup, when resource packs don't exist.
// This is a copy of dynamicimports.glsl and projection.glsl
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};
layout(std140) uniform Globals {
    ivec3 CameraBlockPos;
    float GlintAlpha;
    vec3 CameraOffset;
    float GameTime;
    vec2 ScreenSize;
    int MenuBlurRadius;
    int UseRgss;
};

uniform sampler2D Sampler0;

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;

layout(location = 0) out vec2 texCoord0;
layout(location = 1) out vec4 vertexColor;
layout(location = 2) flat out vec2 centeredCrosshairScale;

bool vanillaCrosshair(ivec2 origin) {
    // Recognize the actual 15x15 vanilla sprite inside the GUI atlas. Geometry
    // alone could also match an unrelated centered icon in an inventory/menu.
    ivec2 size=textureSize(Sampler0,0);
    if(any(lessThan(origin,ivec2(0)))||any(greaterThanEqual(origin+14,size)))return false;
    const ivec2 solid[5]=ivec2[](ivec2(7,7),ivec2(7,3),ivec2(3,7),ivec2(7,11),ivec2(11,7));
    const ivec2 clear[4]=ivec2[](ivec2(3,3),ivec2(11,11),ivec2(6,6),ivec2(8,8));
    for(int i=0;i<5;i++)if(texelFetch(Sampler0,origin+solid[i],0).a<0.99)return false;
    for(int i=0;i<4;i++)if(texelFetch(Sampler0,origin+clear[i],0).a>0.01)return false;
    return true;
}

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    texCoord0 = UV0;
    vertexColor = Color;
    centeredCrosshairScale=vec2(0.0);
    if(abs(ProjMat[3][3]-1.0)<0.0001) {
        vec2 viewport=2.0/abs(vec2(ProjMat[0][0],ProjMat[1][1]));
        vec2 guiSize=ceil(viewport-0.001);
        vec2 expectedOrigin=floor((guiSize-15.0)*0.5);
        vec2 corner = (Position.xy - expectedOrigin) / 15.0;
        if (all(lessThan(abs(corner - round(corner)), vec2(0.0001))) &&
            all(greaterThanEqual(corner, vec2(0))) && all(lessThanEqual(corner, vec2(1)))) {
            ivec2 spriteOrigin = ivec2(round(UV0 * vec2(textureSize(Sampler0, 0)) - corner * 15.0));
            if (vanillaCrosshair(spriteOrigin)) centeredCrosshairScale = ScreenSize / viewport;
        }
    }
}
