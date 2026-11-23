#version 330
#extension GL_ARB_separate_shader_objects : require

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
#include <minecraft:fog.glsl>
#include <minecraft:sample_lightmap.glsl>
#endif

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
layout(location = 3) in ivec2 UV2;
#endif

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
uniform sampler2D Sampler2;
layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;
#endif

layout(location = 2) out vec4 vertexColor;
layout(location = 3) out vec2 texCoord0;

bool isColorNear(ivec3 color, ivec3 target, int tolerance) {
    ivec3 delta = abs(color - target);
    return all(lessThanEqual(delta, ivec3(tolerance)));
}

bool isIconShadowColor(ivec3 color) {
    // Reserved icon markers after vanilla's integer RGB / 4 shadow tint.
    // Use the same table for the icon and shadow, including legacy aliases.
    return isColorNear(color, ivec3(19, 23, 9), 1) ||
           isColorNear(color, ivec3(21, 25, 10), 1) ||
           isColorNear(color, ivec3(25, 0, 0), 1);
}

void main() {
    // Private shader-off warning: clip only the reserved marker.
    if (all(equal(ivec4(round(Color * 255.0)), ivec4(254, 1, 251, 181)))) {
        gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
        return;
    }

    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    vertexColor = Color * sample_lightmap(Sampler2, UV2);
#else
    vertexColor = Color;
#endif
    texCoord0 = UV0;

    ivec3 checkColor = ivec3(round(Color.rgb * 255.0));
    // The server assigns the same exact marker to every actionbar icon.
    // Do not infer foreground markers from a range of dark text colors.
    if (isColorNear(checkColor, ivec3(78, 92, 36), 1) || isColorNear(checkColor, ivec3(170, 2, 2), 1)) {
#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
        vertexColor = sample_lightmap(Sampler2, UV2);
#else
        vertexColor = vec4(1.0);
#endif
    } else if (isIconShadowColor(checkColor) || isColorNear(checkColor, ivec3(74, 3, 15), 1)) {
        vertexColor = vec4(0.0);
    }
}
