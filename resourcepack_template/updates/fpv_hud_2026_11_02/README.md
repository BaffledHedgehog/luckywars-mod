# FPV drone peripheral HUD — 2 November 2026

Copy this directory's `assets/` folder into the resource-pack root, merging it
with the existing `assets/` folder, then reload with
F3+T. Both files are new; existing shaders, post chains, models and translations
are not replaced. The actual installed resource pack was only read.

- `assets/lbc/post_effect/fpv_drone.json`
- `assets/lbc/shaders/post/fpv_drone.fsh`

The server enables native post effect **`lbc:fpv_drone`** only during active FPV
control and removes it on release/cleanup. This pack addition supplies the visual;
activation and flight behavior belong to the mod.

The overlay uses thin mirrored incomplete side brackets and translucent fixed
pixel grids. Cyan `(94,193,199)` and pale `(180,212,225)` reuse the installed
Magnum scope palette. The middle 60% of screen width remains exactly unchanged,
including the aiming area and horizon. Nothing blurs, zooms, dims the view, adds
a center reticle, or presents decorative marks as actual telemetry.

Geometry scales with viewport height and stays within the side bands on narrow,
wide and ultrawide screens. The shader samples each scene pixel once, exits early
over the middle60%, and has only four fixed decoration iterations on the sides.
Its main-only post chain uses legal native 26.3 targets and preserves scene alpha,
including black-hole/nuclear control data. No persistent render target or history
is required.

Validation script: `tools/check_fpv_hud_2026_11_02.py`. Offscreen GPU evidence and
PNG previews are written under `build/verification/fpv-control-2026-11-02/`.
These prove the shader/pipeline contracts, not a physical Minecraft client run.
