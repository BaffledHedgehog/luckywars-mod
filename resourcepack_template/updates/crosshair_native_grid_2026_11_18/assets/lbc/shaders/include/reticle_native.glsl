#ifndef LBC_RETICLE_NATIVE
#define LBC_RETICLE_NATIVE
#include <lbc:reticle_gui_settings.glsl>

int lbc_reticle_gui_scale(ivec2 size) {
    // Window.calculateScale: GUI scale 0 means Auto, then the
    // optional Unicode rounding runs after the framebuffer limit.
    int scale = max(1, min(size.x / 320, size.y / 240));
    if (LBC_GUI_SCALE > 0) scale = min(scale, LBC_GUI_SCALE);
    if (LBC_FORCE_UNICODE && (scale & 1) != 0) scale++;
    return scale;
}

ivec2 lbc_reticle_cell_origin(ivec2 size, int scale) {
    // Hud.extractCrosshair uses ceil(framebuffer / scale) for its anchors.
    // GuiRenderer projects to framebuffer / scale without rounding.
    ivec2 gui = (size + scale - 1) / scale;
    ivec2 sprite = ((gui - 15) / 2) * scale;
    return ivec2(sprite.x + 7 * scale, size.y - sprite.y - 8 * scale);
}
#endif
