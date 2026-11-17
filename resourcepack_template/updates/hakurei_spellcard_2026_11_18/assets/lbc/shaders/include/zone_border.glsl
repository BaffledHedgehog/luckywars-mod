#ifndef LBC_ZONE_BORDER_GLSL
#define LBC_ZONE_BORDER_GLSL

// World-anchored energy, filtered as one continuous scalar field.
// Pixel footprints are supplied analytically by the ray tracer: no derivatives
// are evaluated inside visibility-dependent fragment branches.
float lbc_zone_hash(vec3 cell) {
    uvec3 key = uvec3(ivec3(mod(cell, 64.0)));
    uint bits = key.x * 1597334677u ^ key.y * 3812015801u ^ key.z * 2798796415u;
    bits ^= bits >> 16u;
    bits *= 2246822519u;
    bits ^= bits >> 13u;
    return float(bits & 16777215u) / 16777216.0;
}

// Value and exact gradient of quintic noise (continuous slope and curvature).
vec4 lbc_zone_noise(vec3 p) {
    vec3 cell = floor(p);
    vec3 fraction = fract(p);
    vec3 w = fraction * fraction * fraction * (10.0 + fraction * (-15.0 + 6.0 * fraction));
    vec3 dw = 30.0 * fraction * fraction * (1.0 - fraction) * (1.0 - fraction);
    float a = lbc_zone_hash(cell);
    float b = lbc_zone_hash(cell + vec3(1, 0, 0));
    float c = lbc_zone_hash(cell + vec3(0, 1, 0));
    float d = lbc_zone_hash(cell + vec3(1, 1, 0));
    float e = lbc_zone_hash(cell + vec3(0, 0, 1));
    float f = lbc_zone_hash(cell + vec3(1, 0, 1));
    float g = lbc_zone_hash(cell + vec3(0, 1, 1));
    float h = lbc_zone_hash(cell + vec3(1, 1, 1));
    float lower0 = mix(a, b, w.x);
    float lower1 = mix(c, d, w.x);
    float upper0 = mix(e, f, w.x);
    float upper1 = mix(g, h, w.x);
    float lower = mix(lower0, lower1, w.y);
    float upper = mix(upper0, upper1, w.y);
    vec3 gradient = vec3(
        mix(mix(b - a, d - c, w.y), mix(f - e, h - g, w.y), w.z) * dw.x,
        mix(lower1 - lower0, upper1 - upper0, w.z) * dw.y,
        (upper - lower) * dw.z);
    return vec4(mix(lower, upper, w.z), gradient);
}

vec3 lbc_zone_world_domain(vec3 relativePosition, float level) {
    float scale = exp2(level);
    float inverseScale = 1.0 / scale;
    vec3 cameraPart;
    if (level < 0.0) {
        // Keep integer camera coordinates exact even near the world border.
        ivec3 cells = (CameraBlockPos % ivec3(256)) * int(inverseScale);
        cameraPart = vec3(cells % ivec3(256));
    } else {
        int period = int(256.0 * scale);
        cameraPart = vec3(CameraBlockPos % ivec3(period)) * inverseScale;
    }
    return mod(cameraPart + (relativePosition - CameraOffset) * inverseScale, 256.0);
}

struct LbcZoneField {
    float plasma;
    float flow;
    vec3 gradient;
};

LbcZoneField lbc_zone_field(vec3 relativePosition, float level) {
    float scale = exp2(level);
    vec3 position = lbc_zone_world_domain(relativePosition, level);
    float time = GameTime * 1200.0;
    vec3 drift = vec3(time * 0.065, -time * 0.13, time * 0.035);
    vec4 broad = lbc_zone_noise(position * 0.25 + drift);
    vec4 detail = lbc_zone_noise(position * 0.5 - drift * 1.7);
    float fine = lbc_zone_noise(position + drift * 2.0).x;
    float plasma = broad.x * 0.6 + detail.x * 0.3 + fine * 0.1;
    const float verticalFrequency = 6.2831853 * 114.0 / 256.0;
    float phase = position.y * verticalFrequency + broad.x * 18.0 + detail.x * 4.0 - time * 1.4;
    vec3 phaseGradient = vec3(0.0, verticalFrequency, 0.0) + broad.yzw * 4.5 + detail.yzw * 2.0;
    return LbcZoneField(plasma, sin(phase), cos(phase) * phaseGradient / scale);
}

vec4 lbc_zone_color(vec3 relativePosition, vec3 pixelDX, vec3 pixelDY, int palette) {
    float footprint = max(length(pixelDX), length(pixelDY));
    float frequency = clamp(log2(max(footprint * 24.0, 0.00390625)), -8.0, 10.0);
    float centre = floor(frequency + 0.5);
    float offset = frequency - centre;
    // Quadratic B-spline: value AND slope agree when the support advances.
    // Filter the fields first, then extract ONE set of filaments. Blending
    // finished RGBA layers made two unrelated line patterns overlap and blur.
    vec3 weights = vec3(0.5 * pow(0.5 - offset, 2.0),
                        0.75 - offset * offset,
                        0.5 * pow(0.5 + offset, 2.0));
    float plasma = 0.0;
    float flow = 0.0;
    float filterWidth = 0.0;
    for (int index = 0; index < 3; index++) {
        LbcZoneField field = lbc_zone_field(relativePosition, centre + float(index - 1));
        plasma += (field.plasma - 0.5) * weights[index];
        flow += field.flow * weights[index];
        // Do not cancel opposing band gradients when estimating the filter.
        // That under-filtered tiny bright cores, especially at steep angles.
        filterWidth += weights[index] * (abs(dot(field.gradient, pixelDX)) +
                                         abs(dot(field.gradient, pixelDY)));
    }
    // Retain contrast while filtering instead of washing the material out.
    float contrast = inversesqrt(dot(weights, weights));
    plasma = 0.5 + plasma * contrast;
    flow *= contrast;
    float aa = max(filterWidth * contrast, 0.012);
    float filament = 1.0 - smoothstep(0.04 - aa, 0.22 + aa, abs(flow));
    filament *= 1.0 - smoothstep(0.5, 2.5, aa);

    // No dependence on the display's lower edge or vertical scale.
    float cloud = smoothstep(0.22, 0.82, plasma);
    float intensity = clamp(cloud * 0.85 + filament * 0.75, 0.0, 1.0);
    vec3 base = vec3(0.015, 0.09, 0.24);
    vec3 plasmaColor = vec3(0.18, 0.55, 1.0);
    vec3 filamentColor = vec3(0.58, 0.88, 1.0);
    vec3 coreColor = vec3(0.72, 0.92, 1.0);
    if (palette == 1) {
        base = vec3(0.23, 0.025, 0.055);
        plasmaColor = vec3(1.0, 0.16, 0.24);
        filamentColor = vec3(1.0, 0.58, 0.6);
        coreColor = vec3(1.0, 0.84, 0.82);
    } else if (palette == 2) {
        base = vec3(0.25, 0.15, 0.013);
        plasmaColor = vec3(1.0, 0.78, 0.10);
        filamentColor = vec3(1.0, 0.95, 0.54);
        coreColor = vec3(1.0, 0.98, 0.78);
    }
    vec3 color = mix(base, plasmaColor, intensity);
    color = mix(color, filamentColor, filament * 0.5);
    float opacity = 0.14 + cloud * 0.48 + filament * 0.30;
    float core = smoothstep(0.94, 0.995, filament);
    color = mix(color, coreColor, core);
    opacity = mix(opacity, 1.0, core);
    return vec4(color, opacity);
}


vec4 lbc_zone_color(vec3 relativePosition, vec3 pixelDX, vec3 pixelDY) {
    return lbc_zone_color(relativePosition, pixelDX, pixelDY, 0);
}

#endif
