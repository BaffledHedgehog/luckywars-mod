# Meditation warning bloom: terrain regression correction

The author reported orange terracotta and red sandstone becoming brighter and
glowing after applying meditation warning shader edits. The actual resource pack
was inspected read-only. Its `bloom_sample.fsh` called
`lbc_meditation_warning_bloom(checkColor)` on the composited world scene. The
helper normalizes intensity and accepts approximate matches to four warning
colors, so ordinary terrain with similar chroma also entered bloom.

The template now contains the current author's bloom sampler with only that
call and its now-unused include removed. Existing exact 15-color extraction,
the earlier tolerant red-energy branch, post chain and blur are preserved.
The item shader still identifies warnings by raw RGBA texture markers, keeping
their fullbright rendering, transparency and fade. Actual RP files, server JAR,
Java code, models and user-applied/deleted templates were not modified/restored.
The README retracts the incorrect approximate scene-color bloom instructions.

Apply only `resourcepack_template/assets/minecraft/shaders/post/bloom_sample.fsh`
to the same relative path in the actual pack, then F3+T. The author can instead
remove the two specified lines manually if the pack has changed subsequently.

GPU validation on RTX 4080 SUPER sampled all 1,300 vanilla block textures
(actual pack overrides where present), at five byte-quantized brightnesses:
96,430 distinct texture/brightness samples. Previously 5,413 entered bloom;
after the correction zero did. All six orange terracotta/red sandstone textures
specifically reproduced the old false matches and now have none. All 15 legacy
palette colors retain their original outputs. Both bloom samplers and eight
current item-shader variants compile/link. Five actual RP shader/chain files
retain their initial SHA-256. Evidence and the reproducible validator are in
`build/verification/warning-bloom-fix/`.

This is standalone GPU validation, not in-game observation. Scene-only palette
extraction cannot distinguish a translucent warning from a similar terrain
pixel. Consequently warning bloom may weaken/disappear over blended backgrounds;
its fullbright geometry, opacity and fade remain. Dedicated material-only bloom
would need a separate mask and is outside this correction. Do not restore broad
scene-color matching to compensate for this limitation.
