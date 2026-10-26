#ifndef LBC_SIGNED_GLOW_DATA
#define LBC_SIGNED_GLOW_DATA
const int LBC_SIGNED_GLOW_FIRST_ROW=50;
const int LBC_SIGNED_GLOW_COUNT=8;
int lbc_signed_glow_slot(vec3 color) {
    ivec3 b=ivec3(round(color*255.0));
    return b.r==213 && b.g==117 && b.b<LBC_SIGNED_GLOW_COUNT ? b.b : -1;
}
bool lbc_signed_glow_reserved(vec2 p,vec2 size) {
    float row=min(p.y,size.y-p.y);
    return p.x<57.0 && row>=50.0 && row<58.0;
}
#endif
