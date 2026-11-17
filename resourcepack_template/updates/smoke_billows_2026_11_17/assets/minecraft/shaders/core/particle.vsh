#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

#include <minecraft:sample_lightmap.glsl>
layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;
layout(location = 3) in ivec2 UV2;

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;

layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;
layout(location = 2) out vec2 texCoord0;
layout(location = 3) out vec4 vertexColor;
layout(location = 4) flat out int customParticle;
layout(location = 5) out vec2 particleCoord;
layout(location = 6) out vec3 smokeWorldRay;

// Recover corners from this one marked sprite, not the shared VBO's vertex phase.
// Native indexed draws may start at ANY baseVertex after another vertex format.
bool lbc_smoke_corner(out vec2 uv) {
    ivec2 size=textureSize(Sampler0,0);
    vec2 boundary=UV0*vec2(size);
    ivec2 pixel=ivec2(round(boundary));
    uv=vec2(.5);
    if(any(greaterThan(abs(boundary-vec2(pixel)),vec2(.02))))return false;
    const ivec2 offsets[4]=ivec2[](ivec2(0,0),ivec2(-1,0),ivec2(-1,-1),ivec2(0,-1));
    const vec2 coords[4]=vec2[](vec2(0,0),vec2(1,0),vec2(1,1),vec2(0,1));
    for(int i=0;i<4;i++) {
        ivec2 at=pixel+offsets[i];
        if(any(lessThan(at,ivec2(0)))||any(greaterThanEqual(at,size)))continue;
        ivec4 tag=ivec4(round(texelFetch(Sampler0,at,0)*255.0));
        if(all(equal(tag,ivec4(83,77,241+i,0)))){uv=coords[i];return true;}
    }
    return false;
}

void main() {

    vec4 pre_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    smokeWorldRay=Position;
    
    vertexColor = Color * sample_lightmap(Sampler2, UV2);

    float pos_z = pre_Position.z;
    customParticle = 0;
    int corner = gl_VertexIndex % 4;
    particleCoord = vec2((corner == 0 || corner == 3) ? 0.0 : 1.0,
                         corner < 2 ? 0.0 : 1.0);
    // Decode before lightmap multiplication; ordinary particles may naturally
    // have the same final RGB as a custom effect.
    ivec3 checkColor = ivec3(round(Color.rgb * 255.0));
    
    if (checkColor.r == 0 && checkColor.b == 0) {

    switch(checkColor.g) {

        case 1:
        vertexColor = vec4(vec3(20,209,255)/255,1.0);
        customParticle = 1;
        // 26.3 uses reverse-Z (GREATER_OR_EQUAL): clip depth 1 is nearest.
        pos_z = pre_Position.w;
        break;

        case 2:
        vertexColor = vec4(vec3(255,170,0)/255,1.0);
        customParticle = 2;
        // 26.3 uses reverse-Z (GREATER_OR_EQUAL): clip depth 1 is nearest.
        pos_z = pre_Position.w;
        break;    
        
        }
    }
    
    if(checkColor.r==83 && checkColor.g==77 && checkColor.b>=1 && checkColor.b<=127) {
        vec2 nativeUV;
        if(lbc_smoke_corner(nativeUV)) {
            customParticle=4;
            particleCoord=nativeUV;
            vec2 side=vec2(nativeUV.x*2.0-1.0,1.0-nativeUV.y*2.0);
            // OverlayParticle encodes its own local age in alpha before extraction:
            // alpha=.725-age*.125. Midpoint decoding bounds quantization error.
            // No server/client clock or network-latency-dependent birth timestamp.
            float age=(.725-(round(Color.a*255.0)+.5)/255.0)*8.0;
            float nativeSize=7.1*sin((age-1.0)*.25*3.14159265359);
            float grow=clamp((age-1.0)*1.5,0.0,1.0);
            float fade=1.0-smoothstep(3.0,4.0,age);
            vec4 view=ModelViewMat*vec4(Position,1.0);
            view.xy-=side*nativeSize;
            view.xy+=side*vec2(8.0,5.2)*(float(checkColor.b)/127.0)*grow*(.8+.2*fade);
            view.xyz+=mat3(ModelViewMat)*vec3(0.0,max(0.0,age-1.0)*.07,0.0);
            smokeWorldRay=transpose(mat3(ModelViewMat))*view.xyz;
            pre_Position=ProjMat*view;
            if(age<1.0||age>=4.0)pre_Position=vec4(2.0,2.0,2.0,1.0);
            vertexColor=vec4(vec3(.46),fade);
        } else {
        // Compatibility with an older server's potion-particle smoke carrier.
        customParticle=3;
        // Native QuadParticleRenderState corners are(+,-),(+,+),(-,+),(-,-).
        // Expand in camera space, retaining native positions, drift and depth.
        vec2 side=vec2(corner<2 ? 1.0 : -1.0,corner==1||corner==2 ? 1.0 : -1.0);
        particleCoord=side*.5+.5;
        vec4 view=ModelViewMat*vec4(Position,1.0);
        view.xy+=side*(.12+2.3*float(checkColor.b)/127.0);
        pre_Position=ProjMat*view;
        vertexColor=vec4(vec3(.48),1.0);
        }
    }
    switch(customParticle){

        case 1:
        case 2:
        gl_Position = vec4(pre_Position.xy, pos_z, pre_Position.w);
        break;

        default:
        gl_Position = pre_Position;
        break;
        
    }

    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    texCoord0 = UV0;
    
}
