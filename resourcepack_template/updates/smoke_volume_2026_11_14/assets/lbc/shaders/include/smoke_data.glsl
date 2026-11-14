#ifndef LBC_SMOKE_DATA_GLSL
#define LBC_SMOKE_DATA_GLSL
// Zone0, black-hole/nuclear1..49, signed glow50..57, weapon spread58.
const int LBC_SMOKE_COUNT = 16;
const int LBC_SMOKE_FIRST_ROW = 59;
const int LBC_SMOKE_PIXELS = 57;

int lbc_smoke_slot(vec3 color) {
    ivec3 bytes = ivec3(round(color * 255.0));
    return bytes.r == 213 && bytes.g == 119 && bytes.b < LBC_SMOKE_COUNT ? bytes.b : -1;
}

bool lbc_smoke_reserved_pixel(vec2 pixel, vec2 size) {
    int row = int(floor(pixel.y));
    int mirror = int(size.y) - 1 - row;
    return pixel.x < float(LBC_SMOKE_PIXELS) &&
        ((row >= LBC_SMOKE_FIRST_ROW && row < LBC_SMOKE_FIRST_ROW + LBC_SMOKE_COUNT) ||
         (mirror >= LBC_SMOKE_FIRST_ROW && mirror < LBC_SMOKE_FIRST_ROW + LBC_SMOKE_COUNT));
}
#endif
