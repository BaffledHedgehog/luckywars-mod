// Separate from zone0, black-hole/nuclear1..49 and signed glowing50..57.
const int LBC_WEAPON_SPREAD_ROW=58;
bool lbc_weapon_spread_marker(vec3 color) {
    return all(equal(ivec3(round(color*255.0)),ivec3(213,118,0)));
}
bool lbc_weapon_spread_reserved(vec2 pixel,vec2 size) {
    int row=int(floor(pixel.y));
    return pixel.x<57.0 && (row==LBC_WEAPON_SPREAD_ROW || row==int(size.y)-1-LBC_WEAPON_SPREAD_ROW);
}
