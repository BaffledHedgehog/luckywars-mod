# Procedural TMT equipped armor

Copy this update's assets into your pack, merging entity shaders if your author
hash differs from AUTHOR_BASE_SHA256.json. The author pack was read only.
Both humanoid layers use one equipment asset lbc:tmt/procedural. The armor seed
is independently persisted by the server and transported through dyed_color
0xFA0000 | seed16. Ordinary armor, glint, light, fog and author alpha effects
keep their native branches. Native armor UVs are used, never item vertex indices.

Equipment has no access to the bound item atlas: its sole Sampler0 is its own
equipment texture. This patch therefore supplies a small source sheet of unique
vanilla and author item textures (excluding old TMT outputs), and computes the
collage in the shader. No random output textures are pregenerated. See
SOURCE_ATLAS.json for exact size and sources. Rebuild with
python tools/build_tmt_equipment_patch.py after changing the source item art.

Keep the old 707 equipment assets for saved items until they have been migrated;
new TMT items use the new asset. The item-atlas patch remains independent.
Test all four equipped slots, glint, other players, armor stands and reconnect.
GPU tests do not replace a physical Minecraft rendering test.
