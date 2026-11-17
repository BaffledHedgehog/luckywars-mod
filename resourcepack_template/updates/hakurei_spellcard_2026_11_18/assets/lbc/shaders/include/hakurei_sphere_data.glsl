#ifndef LBC_HAKUREI_SPHERE_DATA_GLSL
#define LBC_HAKUREI_SPHERE_DATA_GLSL

// Below these are the smoke rows 59 through 74.
const int LBC_HAKUREI_FIRST_ROW = 75;
const int LBC_HAKUREI_SLOTS = 20;
const ivec3 LBC_HAKUREI_TINT = ivec3(231, 163, 23);

bool lbc_hakurei_rows_fit(ivec2 size) {
    return size.x >= 57 && size.y >= 2 * (LBC_HAKUREI_FIRST_ROW + LBC_HAKUREI_SLOTS);
}

int lbc_hakurei_slot(ivec3 bytes) {
    return bytes.r == 231 && (bytes.g == 161 || bytes.g == 162) &&
           bytes.b >= 0 && bytes.b < LBC_HAKUREI_SLOTS ? bytes.b : -1;
}

bool lbc_hakurei_sprite(sampler2D atlas, vec2 uv) {
    ivec2 size = textureSize(atlas, 0);
    ivec2 pixel = clamp(ivec2(uv * vec2(size)), ivec2(0), size - 1);
    return all(equal(ivec4(round(texelFetch(atlas, pixel, 0) * 255.0)), ivec4(31, 97, 183, 247)));
}

bool lbc_hakurei_reserved(vec2 pixel, vec2 size) {
    if (!lbc_hakurei_rows_fit(ivec2(size)) || pixel.x >= 57.0) return false;
    int row = int(floor(pixel.y));
    int mirror = int(size.y) - 1 - row;
    return (row >= LBC_HAKUREI_FIRST_ROW && row < LBC_HAKUREI_FIRST_ROW + LBC_HAKUREI_SLOTS) ||
           (mirror >= LBC_HAKUREI_FIRST_ROW && mirror < LBC_HAKUREI_FIRST_ROW + LBC_HAKUREI_SLOTS);
}

#endif
