# Shield compatibility ? 21 October 2026

Every actively blocking `minecraft:shield`, including the ordinary vanilla item, must stop source `shield_blockable` projectiles; Govnomet's `shieldblockable` spelling is included explicitly (user). Existing deliberate author exceptions remain authoritative.

Source root (read only): `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`. Paths below are relative to that root. This is a bounded code/source audit, not a claim that every legacy emitter is ported or server qualified.

## Confirmed native coverage

| Exact legacy source or profile | Native path and result |
| --- | --- |
| `lbc/function/workingitems/sauvojen_staffs/terra_preloop.mcfunction`, `terra_preloop_upgraded.mcfunction`, `true_terra_sword_hit.mcfunction`; `lbcr/function/raycast_preloop13.mcfunction` | `LbcTerra.step` / true beam uses shared `LbcWands.shieldHit`, clips damage/trail at first shield contact. |
| `lbc/function/workingitems/sauvojen_staffs/electro_preloop.mcfunction`, `electro_preloop_upgrade.mcfunction`, `electro_default.mcfunction`; `workingitems/sauvojen_staffs_tiny/electro_end.mcfunction` | `LbcElectro` and `LbcWands` Electro jobs use shared segment shieldHit. |
| `lbc/function/workingitems/sauvojen_staffs/blind_preloop.mcfunction`, `blind_preloop_upgraded_default.mcfunction`, `blind_preloop_upgraded_phantom.mcfunction`, `blind_preloop_upgraded_rain_loop.mcfunction`, `blind_loop_gatling.mcfunction` | `LbcBlind` uses shieldHit before hit effects. |
| `lbc/function/workingitems/sauvojen_staffs/teleport_preloop.mcfunction`, `teleport_default.mcfunction`, `teleport_inverted.mcfunction` | `LbcTeleport` uses shieldHit before hit effects. |
| `lbc/function/workingitems/sauvojen_staffs/acid_preloop.mcfunction`, `acid_preloop_2.mcfunction`, `acid_preloop_upgraded.mcfunction`, `acid_preloop_upgraded_lib.mcfunction`; `lbcr/function/raycast_preloop11.mcfunction`, `raycast_preloop11_2.mcfunction`, `raycast_preloop11_upgraded.mcfunction`, `raycast_preloop11_upgraded_lib.mcfunction` | Native `LbcClouds.AcidProjectile`: source spelling is **shield_blockable2**, which blocks only in #minecraft:airs. Existing acidImpact already accepts ordinary vanilla shields. Added a shared shieldHit sweep over actual native movement, clipped at native collision; check precedes native onHit effects/sticking, and MISS checks occur after native movement. Gravity, drag, bubble columns, collision selection and lifetime remain native/unchanged. |
| `lbc/function/workingitems/summoner/cultists/chaos/get_terra.mcfunction`, `get_electro.mcfunction` | `LbcSummons.chaosCast` delegates to the guarded Terra / cultistElectro paths. No new summon collision engine. |
| `lbcr/function/raycast_preloop10magnum.mcfunction`, `raycast_preloop10magnumx2.mcfunction` | Magnum uses common guarded Electro/acid profiles; root owns new shooter regression coverage. |

The common helper queries blocking players in a bounded segment AABB and has no custom-item requirement. Existing owner/team/observer filters remain. Acid's preexisting endpoint check remains for stationary charges and nonplayer-owned cloud charges; shared swept guard applies where owner is ServerPlayer.

## Preserved exclusions and remaining legacy emitters

`workingitems/sauvojen_staffs/ruby_preloop.mcfunction` has **no** shield tag. `LbcRuby` ordinary/Academy drill is therefore unchanged. Tagged `lbc/function/raycast/sauvojen_ruby.mcfunction` is a distinct Tuntija emitter, not proof that player Ruby must block. Tiny Ruby's explicitly authorized line piercing and shit_katana spinning beam's explicit shield bypass are preserved. AK47 source has no shield marker and is unchanged.

Tag search also finds Tuntija `lbc/function/raycast/sauvojen_{acid,blind,electro,terra,teleport,ruby}.mcfunction`, flying Tuntija wand and meditation emitters under `items/wands/sauvojen_wand/`, Fevgalea rain/waterdrop/hit emitters, kapusta, Halloween jack_o_lantern, transformator_shoot, muravei_acid, charge_acid_ball, racer_arrow_lighting, volos_arrow_tick, imba chain lightning, awakened Erecto sphere, and `lbcr` prism / mechanism bolt variants. These were identified by source-tag search; native emitter-by-emitter coverage is **not established by this bounded audit**. Do not report those unported/deferred profiles as shield qualified. Govnomet and Helicopter Boss are owned by the root task.

## Validation boundary

No Gradle or server process was launched by this audit. Root performs build and ordinary-shield/native projectile regressions. The extra Acid guard uses the actual positions produced by `ThrowableProjectile.tick` from the cached Minecraft 26.3 source, avoiding a second gravity/drag calculation. Source data and resource pack were not edited.
