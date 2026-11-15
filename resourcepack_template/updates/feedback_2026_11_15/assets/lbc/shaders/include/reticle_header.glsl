#ifndef LBC_RETICLE_HEADER
#define LBC_RETICLE_HEADER
// Outside TimeStop's last12 pixels. RGB is preserved: this is invisible metadata.
const int LBC_RETICLE_BYTES = 28;
const int LBC_RETICLE_MAGIC[4] = int[4](83,80,82,219);
int lbc_reticle_offset(ivec2 pixel,ivec2 size) {
    int n=size.x-20-pixel.x;
    return (pixel.y==0 || pixel.y==size.y-1) && n>=0 && n<LBC_RETICLE_BYTES ? n : -1;
}
#endif
