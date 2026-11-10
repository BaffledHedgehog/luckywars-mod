# Exoperso rune bloom

Versioned shader support for the native Exoperso ritual. Merge `assets` into the independently maintained resource pack, then reload it with F3+T. This directory is source support, not an automatically generated or server-delivered resource pack.

The ritual marks rune ItemDisplays with outline color `#E75313`. Texture alpha 254 identifies rune geometry. The outline shader encodes a private mask with depth; the existing polymorph bloom pass reads it and rejects pixels behind opaque scene geometry. The Sobel pass excludes the private records. Ordinary yellow/orange scene colors are not bloom selectors.

These files target the author's Minecraft 26.3 resource pack layout. Merge future edits with the current pack rather than overwriting unrelated shader changes. No rune PNGs or visible item colors are changed here.

Validation: native ritual checks passed for both wand versions and rollback. GPU checks covered all ten rune textures, opaque occlusion and 99,750 block/skin color samples; the broad warm selector produced 13,001 false positives, while this fix matched the previous baseline with zero. This is shader-level validation, not a full client capture.
