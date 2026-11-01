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

uniform sampler2D Sampler2;

layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;
layout(location = 2) out vec2 texCoord0;
layout(location = 3) out vec4 vertexColor;
layout(location = 4) flat out int customParticle;
layout(location = 5) out vec2 particleCoord;

void main() {

    vec4 pre_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    
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
