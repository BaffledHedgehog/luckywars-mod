#version 330
#extension GL_ARB_separate_shader_objects : require

// Cosmetic edge instrumentation only; flight/camera/input are native server state.
uniform sampler2D InSampler;
layout(location = 0) out vec4 fragColor;

float fpvLine(vec2 point, vec2 a, vec2 b, float width) {
    vec2 delta = b - a;
    float along = clamp(dot(point - a, delta) / max(dot(delta, delta), 0.0001), 0.0, 1.0);
    float distance = length(point - a - delta * along);
    return 1.0 - smoothstep(width * 0.5, width * 0.5 + 0.7, distance);
}

float fpvGrid(vec2 point, vec2 origin, vec2 size, float cell) {
    vec2 local = point - origin;
    if (any(lessThan(local, vec2(0.0))) || any(greaterThanEqual(local, size))) return 0.0;
    ivec2 index = ivec2(floor(local / cell));
    vec2 inCell = mod(local, cell);
    // Sparse fixed pixel clusters, not animated noise or invented flight telemetry.
    int pattern = (index.x * 3 + index.y * 5 + (index.y / 3) * 2) % 13;
    float occupied = pattern < 7 ? 1.0 : 0.0;
    float square = float(all(lessThan(inCell, vec2(cell * 0.48))));
    return occupied * square;
}

void main() {
    ivec2 pixel = ivec2(gl_FragCoord.xy);
    vec2 size = vec2(textureSize(InSampler, 0));
    vec4 scene = texelFetch(InSampler, pixel, 0);
    fragColor = scene;
    float sideX = min(gl_FragCoord.x, size.x - gl_FragCoord.x);
    // Keep the full central 60% unmodified at every height and aspect ratio.
    if (sideX >= size.x * 0.20) return;
    float scale = max(min(size.x, size.y) / 720.0, 0.5);
    // All geometry is measured in height-scaled pixels, clipped to the side band.
    vec2 point = vec2(sideX, gl_FragCoord.y) / scale;
    vec2 viewport = size / scale;
    float rail = min(22.0, viewport.x * 0.035);
    float reach = min(92.0, viewport.x * 0.165);
    float bottom = viewport.y * 0.16;
    float top = viewport.y * 0.84;
    float middle = viewport.y * 0.5;
    float frame = 0.0;

    // Broken side rails, with short chamfered caps; no full-screen border.
    frame = max(frame, fpvLine(point, vec2(rail, bottom + 16.0), vec2(rail, middle - 72.0), 1.1));
    frame = max(frame, fpvLine(point, vec2(rail, middle + 72.0), vec2(rail, top - 16.0), 1.1));
    frame = max(frame, fpvLine(point, vec2(rail, top - 16.0), vec2(rail + 16.0, top), 1.1));
    frame = max(frame, fpvLine(point, vec2(rail + 16.0, top), vec2(reach, top), 1.1));
    frame = max(frame, fpvLine(point, vec2(rail, bottom + 16.0), vec2(rail + 16.0, bottom), 1.1));
    frame = max(frame, fpvLine(point, vec2(rail + 16.0, bottom), vec2(reach, bottom), 1.1));
    // Independent tiny brackets leave the camera horizon open.
    frame = max(frame, fpvLine(point, vec2(rail - 4.0, middle - 35.0), vec2(rail - 4.0, middle - 15.0), 1.0));
    frame = max(frame, fpvLine(point, vec2(rail - 4.0, middle - 15.0), vec2(rail + 5.0, middle - 15.0), 1.0));
    frame = max(frame, fpvLine(point, vec2(rail - 4.0, middle + 15.0), vec2(rail - 4.0, middle + 35.0), 1.0));
    frame = max(frame, fpvLine(point, vec2(rail - 4.0, middle + 15.0), vec2(rail + 5.0, middle + 15.0), 1.0));

    float gridWidth = max(4.0, min(52.0, reach - rail - 6.0));
    float upperGrid = fpvGrid(point, vec2(rail + 7.0, viewport.y * 0.64), vec2(gridWidth, 72.0), 4.0);
    float lowerGrid = fpvGrid(point, vec2(rail + 7.0, viewport.y * 0.28), vec2(gridWidth * 0.7, 40.0), 4.0);
    float grid = max(upperGrid, lowerGrid);
    float ticks = 0.0;
    // A small, fixed number of etched divisions, isolated to the side rails.
    for (int index = 0; index < 4; index++) {
        float y = middle - 58.0 + float(index) * 5.0;
        ticks = max(ticks, fpvLine(point, vec2(rail, y), vec2(rail + 6.0 + float(index % 2) * 4.0, y), 1.0));
        ticks = max(ticks, fpvLine(point, vec2(rail, viewport.y - y), vec2(rail + 6.0 + float(index % 2) * 4.0, viewport.y - y), 1.0));
    }

    // The installed Magnum's cyan/steel palette, with deliberately light coverage.
    vec3 cyan = vec3(94.0, 193.0, 199.0) / 255.0;
    vec3 pale = vec3(180.0, 212.0, 225.0) / 255.0;
    vec3 color = mix(scene.rgb, cyan, frame * 0.40);
    color = mix(color, cyan, grid * 0.18);
    color = mix(color, pale, ticks * 0.27);
    // Preserve every original alpha byte, including black-hole/nuclear transport.
    fragColor = vec4(color, scene.a);
}
