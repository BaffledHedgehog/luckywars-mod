# Survival mining, settings, locator, FOV, TMT equipment and Imprinted

7 November2026. Qualified full-source follow-up to `8afde086` (true legacy). The author's resource pack and unrelated working changes remain untouched.

## Changes

- `TpsMiningMixin` previously cancelled `incrementDestroyProgress` at HEAD, skipping Polymer's RETURN handler. Polymer had already disabled client prediction, so neither side completed survival mining. Server-controlled Polymer mining now reaches its existing completion handler. Block hardness/tool rules were not replaced: all three Lucky blocks and consumed matter retain the exact mushroom hand rate. Polymer retains the ordinary five progress-tick inter-block delay.
- HP display, IMBA drop chance and team prefixes use persistent `GameConfig` fields, original GUI variants/callbacks and shared startup/edit/reset publication. Source chance holders in both `swrg.math` and `lbc.math` are updated together. HP uses the native HEALTH objective, below-name/TAB display slots. Source SFW170/NSFW91 prefix components are selected once per owned match team, not per tick; mixed is their original concatenation. No new arbitrary function execution was added.
- Existing `GameController.updateTrackers` now labels ordinary compasses with the nearest live target's nickname, uses its actual dimension, and excludes BedWars allies. Unchanged name/position components are not rewritten. Native special compasses keep their separate item identities.
- `/tps` and `/skywars tps` report actual rolling1/5/15minute TPS and native average MSPT. One primitive timestamp is recorded through the existing TPS hook; no new world/entity scan or timer thread. Early readings use the available partial history.
- The unscoped FOV denominator excludes `minecraft:sprinting`: local sprint prediction and its server acknowledgment use the same denominator. Ordinary sprint widening remains native; bonus-speed attenuation and full scope strength remain. Authoritative attributes/abilities/velocity are unchanged. Native equipment/F swaps, stale snapshots, nested bundles, reset flags and asynchronous sends are tested.
- `lbc:imprinted` is owned only by `minecraft:imprinted`, through `SignedEffectVisuals` and its existing dimension/login/respawn/rewind reconciliation. No independent timer or event-specific owner. The existing author RP already contains the effect. Full900-active-tick visits, staggered arrival, TimeStop, return/reentry and both visitors' actual effect lists pass.
- Newly generated TMT items use the single equipment asset `lbc:tmt/procedural`, with an independent persisted armor seed transported through hidden `dyed_color`. The source707-range draw and following item-model draw still occur in the same RNG positions; item seed/appearance and all gameplay properties are retained. Native codecs preserve equipment/seed/color. Old saved numbered equipment remains valid.

## Settings

Both `/settings` and its `/skywars settings` alias use these names; GUI controls use the same fields.

| Parameter | Values |
| --- | --- |
| `healthDisplay` (alias `hp`) | 0 hidden, 1 below name, 2 TAB, 3 both |
| `imbaDropChance` (alias `imbadrop_chance`) | Source indices0..6: 0 / .001 / .01 / .1 / .4 / 1 / 10 percent |
| `prefixes` | 0 SFW, 1 NSFW, 2 mixed, 3 disabled, 4 original cosmetic-prefix WIP |

Other already implemented match settings are preserved. This patch does not introduce the old process-restart/server-control menu actions or turn the source's unfinished prefix-unlock mode4 into a new system.

## Resource-pack delivery

`resourcepack_template/updates/tmt_equipment_2026_11_07/README.md` describes the merge. Its six assets and evidence are also packaged in `build/verification/test-feedback-2026-11-07/tmt-equipment-pack.zip`.

Armor cannot sample the item atlas: its bound texture is its equipment layer. The patch packs1878 unique vanilla/author item sprites into one1024-square source sheet,438361PNG bytes, supplied for both humanoid layers. The shader reuses the item collage algorithm with native armor UVs and full face coverage; no random output PNGs are generated. The source builder reads the canonical item helper, and the independent item-atlas patch is retained. Keep old707 equipment assets while saved stacks still reference them.

GPU receipt `GPU_RESULTS.json`:11 entity pipeline variants compile,24 ordinary comparisons match author/vanilla originals, fixed seed repeats, different seed changes output, endpoints remain finite, and vertex-index offsets do not change the native-UV image. Existing author alpha/light/fog/glint/OIT branches are retained. Physical Minecraft rendering and peakFPS/TPS were not measured. The author pack was read only.

## Final artifacts and evidence

| Full-source artifact | SHA-256 |
| --- | --- |
| test | `c993bde7ce5c8ea83636efa4a37a795524df0e612beaa7ff9513cde1b51d8bef` |
| production | `84c4150fbe416f3e0cf648626c4d45fa3aacadfd9588b4198c5e74d5703a1578` |

`build/verification/test-feedback-2026-11-07/qualification.json`, `artifacts.json`, `package-proof.json`, final build log and three `c993bde7-*-results.json`/log pairs are the authoritative receipts. Eleven actual matches / thirteen requested suites pass on this same final test JAR:

- Seven: custom-block-mining, feedback-patch, speed-fov-transitions, tmt-properties, signed-visuals, imprinted-return, tps-compensation.
- Three: lucky-blocks, lucky-block-rules, speed-fov.
- One actual Supernova -> Balance match: supernova-chain, true-legacy-rules, balance-runtime; real stop also observes true-legacy-exit.

Common production/test gameplay classes are byte-identical; production excludes all eight map recording/deletion classes. All5932 literal loot tables match the qualified parent. All10505 frozen source/build inputs and282 protected author paths match, including three Java WIP files, HUD note, RP edits and deletions. Gradle768MiB/one worker and one1280MiB fixture run sequentially; every fixture exits normally and restores its exact snapshots.

Rejected diagnostic receipts are retained, never counted as acceptance. Mining checks originally omitted Polymer's inter-block delay; WILD preparation intentionally removes player teams; the FOV fixture's first explicit bundle also acquired initial personal-nameplate team packets (reproduced on parent4966406c), so it now publishes those before exact movement-bundle assertions. TPS stall tests use their original allowed isolated port25576; the safety guard was not weakened. These fixture corrections do not add gameplay bypasses.

The two qualified JARs remain staged in this verification directory. Main stays production299e61e8; build/libs is unchanged. No installer, Main replacement or restart was performed. Keep the standing policy of combining fixes into the next stable release and preserve the author's current flavor selection.
