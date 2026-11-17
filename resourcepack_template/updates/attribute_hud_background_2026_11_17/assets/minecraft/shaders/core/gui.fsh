#version 330
#extension GL_ARB_separate_shader_objects : require

// Can't moj_import in things used during startup, when resource packs don't exist.
// This is a copy of dynamicimports.glsl
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};

layout(location = 0) in vec4 vertexColor;
layout(location = 1) noperspective in vec4 sidebarRect;
layout(location = 2) flat in vec2 sidebarGuiSize;

layout(location = 0) out vec4 fragColor;

void main() {
    // Compute derivatives before any discard. Invert the quad-UV Jacobian to
    // recover GUI bounds on both triangles, without relying on vertex order
    // chosen by OpenGL/Vulkan for flat interpolation.
    vec4 dx = dFdx(sidebarRect);
    vec4 dy = dFdy(sidebarRect);
    float determinant = dx.x * dy.y - dy.x * dx.y;
    if (abs(determinant) > 1e-12) {
        vec2 edgeU = (dx.zw * dy.y - dy.zw * dx.y) / determinant;
        vec2 edgeV = (dy.zw * dx.x - dx.zw * dy.x) / determinant;
        vec2 origin = sidebarRect.zw - edgeU * sidebarRect.x - edgeV * sidebarRect.y;
        vec2 lower = origin + min(edgeU, vec2(0.0)) + min(edgeV, vec2(0.0));
        vec2 upper = origin + max(edgeU, vec2(0.0)) + max(edgeV, vec2(0.0));
        float height = upper.y - lower.y;
        float centre = floor(sidebarGuiSize.y * 0.5);
        float bodyRows = round((height - 1.0) / 9.0);
        float titleRows = round((centre - 1.0 - upper.y) / 6.0);
        bool body = bodyRows >= 0.0 && bodyRows <= 15.0 &&
                    abs(height - (9.0 * bodyRows + 1.0)) < 0.05 &&
                    abs(upper.y - (centre + 3.0 * bodyRows)) < 0.05;
        bool title = titleRows >= 0.0 && titleRows <= 15.0 &&
                     abs(height - 9.0) < 0.05 &&
                     abs(upper.y - (centre - 6.0 * titleRows - 1.0)) < 0.05;
        bool rightEdge = abs(upper.x - (sidebarGuiSize.x - 1.0)) < 0.05;
        // Server-provided text can make the sidebar cross the screen midpoint.
        // Its right edge and vertical layout stay anchored; its left edge does not.
        bool sidebarWidth = upper.x - lower.x >= 3.0;
        // StagedVertexBuffer baseVertex can rotate the quad corner phase.
        // Both edge bases describe the same axis-aligned rectangle.
        bool aligned = (abs(edgeU.y) < 0.05 && abs(edgeV.x) < 0.05) ||
                       (abs(edgeU.x) < 0.05 && abs(edgeV.y) < 0.05);
        bool black = all(lessThan(abs(vertexColor.rgb), vec3(0.001)));
        // Opacity is deliberately unrestricted: accessibility settings can
        // override both the usual 0.3 body alpha and the 0.4 title alpha.
        if (black && aligned && rightEdge && sidebarWidth && (body || title)) discard;
    }
    vec4 color = vertexColor;
    if (color.a == 0.0) {
        discard;
    }
    fragColor = color * ColorModulator;
}
